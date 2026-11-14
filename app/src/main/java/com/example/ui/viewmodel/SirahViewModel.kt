package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.curriculum.SeerahCurriculum
import com.example.data.firebase.compressReceiptToBase64
import com.example.data.model.ActivationCode
import com.example.data.model.AppConfig
import com.example.data.model.ChapterMetadata
import com.example.data.model.LessonContent
import com.example.data.model.PaymentRequest
import com.example.data.model.UserProfile
import com.example.data.repository.SirahRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class SirahViewModel(
    private val repository: SirahRepository,
    val currentUserId: String,
    private val userEmail: String,
    private val displayName: String
) : ViewModel() {

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _lessonState = MutableStateFlow<UiState<LessonContent>>(UiState.Loading)
    val lessonState: StateFlow<UiState<LessonContent>> = _lessonState.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    val userProfileState: StateFlow<UiState<UserProfile>> = repository.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile>> { profile ->
            if (profile != null) UiState.Success(profile) else UiState.Loading
        }
        .catch { error ->
            val fsErr = unwrapFirestoreError(error)
            if (fsErr == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing user profile", fsErr ?: error)
            emit(UiState.Error((fsErr ?: error).localizedMessage ?: "تعذر تحميل بيانات الطالب"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val chaptersCatalog: StateFlow<List<ChapterMetadata>> = repository.observeChaptersCatalog()
        .map { remote ->
            if (remote.isNotEmpty()) remote else SeerahCurriculum.catalog
        }
        .catch { error ->
            val fsErr = unwrapFirestoreError(error)
            if (fsErr == null && error is CancellationException) throw error
            Log.w(TAG, "Falling back to built-in catalog", fsErr ?: error)
            emit(SeerahCurriculum.catalog)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), SeerahCurriculum.catalog)

    val userPayments: StateFlow<List<PaymentRequest>> = repository.observeUserPayments(currentUserId)
        .catch { error ->
            val fsErr = unwrapFirestoreError(error)
            if (fsErr == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing user payments", fsErr ?: error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Admin streams (collected only when admin screen is opened & isAdmin == true)
    private val _adminPayments = MutableStateFlow<List<PaymentRequest>>(emptyList())
    val adminPayments: StateFlow<List<PaymentRequest>> = _adminPayments.asStateFlow()

    private val _adminUsers = MutableStateFlow<List<UserProfile>>(emptyList())
    val adminUsers: StateFlow<List<UserProfile>> = _adminUsers.asStateFlow()

    private val _adminCodes = MutableStateFlow<List<ActivationCode>>(emptyList())
    val adminCodes: StateFlow<List<ActivationCode>> = _adminCodes.asStateFlow()

    private val _adminAppConfig = MutableStateFlow<AppConfig?>(null)
    val adminAppConfig: StateFlow<AppConfig?> = _adminAppConfig.asStateFlow()

    private var adminObserversStarted = false

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(
                userId = currentUserId,
                defaultName = displayName.ifBlank { "بطل السيرة" },
                email = userEmail
            )
            _isAdmin.value = repository.checkIsAdmin(currentUserId, userEmail)
        }
    }

    fun clearMessage() {
        _actionMessage.value = null
    }

    fun updateChildName(newName: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.updateChildName(currentUserId, newName)
            _isBusy.value = false
            _actionMessage.value = if (res.isSuccess) {
                "تم تحديث اسم البطل بنجاح!"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "تعذر تحديث الاسم"
            }
        }
    }

    fun loadLesson(chapterNumber: Int) {
        val currentProfile = (userProfileState.value as? UiState.Success)?.data
        val unlocked = chapterNumber == 1 || (currentProfile?.hasFullAccess == true) || _isAdmin.value
        if (!unlocked) {
            _lessonState.value = UiState.Error("هذا الفصل مدفوع. يرجى تفعيل النسخة الكاملة لفتحه.")
            return
        }

        _lessonState.value = UiState.Loading
        viewModelScope.launch {
            val res = repository.getLessonContent(chapterNumber)
            val remote = res.getOrNull()
            val lesson = remote ?: SeerahCurriculum.getBuiltInLesson(chapterNumber)
            _lessonState.value = if (lesson != null) {
                UiState.Success(lesson)
            } else {
                UiState.Error("تعذر تحميل محتوى الفصل")
            }
        }
    }

    fun recordQuizCompletion(chapterNumber: Int, correctCount: Int, totalQuestions: Int) {
        val profile = (userProfileState.value as? UiState.Success)?.data ?: return
        val earnedStars = when {
            totalQuestions > 0 && correctCount == totalQuestions -> 3
            correctCount >= (totalQuestions + 1) / 2 -> 2
            else -> 1
        }
        val earnedScore = correctCount * 10
        viewModelScope.launch {
            repository.recordChapterCompletion(
                userId = currentUserId,
                chapterNumber = chapterNumber,
                earnedStars = earnedStars,
                earnedScore = earnedScore,
                existingCompleted = profile.completedChapters,
                currentStars = profile.stars,
                currentScore = profile.totalScore
            )
        }
    }

    fun submitPaymentReceipt(
        context: Context,
        method: String,
        senderPhone: String,
        note: String,
        imageUri: Uri
    ) {
        if (senderPhone.trim().length < 6) {
            _actionMessage.value = "يرجى إدخال رقم الهاتف المحوَّل منه بشكل صحيح"
            return
        }
        val childName = (userProfileState.value as? UiState.Success)?.data?.childName ?: displayName.ifBlank { "طالب" }
        viewModelScope.launch {
            _isBusy.value = true
            val base64 = withContext(Dispatchers.IO) {
                compressReceiptToBase64(context, imageUri)
            }
            if (base64 == null) {
                _isBusy.value = false
                _actionMessage.value = "تعذر قراءة صورة الإيصال، يرجى اختيار صورة أخرى"
                return@launch
            }
            val res = repository.submitPaymentRequest(
                userId = currentUserId,
                childName = childName,
                userEmail = userEmail,
                method = method,
                senderPhone = senderPhone,
                transactionNote = note,
                receiptBase64 = base64
            )
            _isBusy.value = false
            _actionMessage.value = if (res.isSuccess) {
                "تم إرسال إيصال الدفع للمراجعة اليدوية. سيظهر كود التفعيل هنا أو يصلك بعد تأكيد وصول 200 أوقية جديدة."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "فشل إرسال الإيصال"
            }
        }
    }

    fun activateWithCode(code: String, onActivated: () -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.redeemActivationCode(currentUserId, code)
            _isBusy.value = false
            if (res.isSuccess) {
                _actionMessage.value = res.getOrNull()
                onActivated()
            } else {
                _actionMessage.value = res.exceptionOrNull()?.localizedMessage ?: "كود التفعيل غير صحيح أو ملغى"
            }
        }
    }

    // ==================== ADMIN FUNCTIONS ====================

    fun startAdminObservers() {
        if (!_isAdmin.value || adminObserversStarted) return
        adminObserversStarted = true

        viewModelScope.launch {
            repository.observeAllPayments()
                .catch { Log.w(TAG, "Admin payments error", it) }
                .collect { _adminPayments.value = it }
        }
        viewModelScope.launch {
            repository.observeAllUsers()
                .catch { Log.w(TAG, "Admin users error", it) }
                .collect { _adminUsers.value = it }
        }
        viewModelScope.launch {
            repository.observeAllActivationCodes()
                .catch { Log.w(TAG, "Admin codes error", it) }
                .collect { _adminCodes.value = it }
        }
        viewModelScope.launch {
            repository.observePrimaryActivationConfig()
                .catch { Log.w(TAG, "Admin config error", it) }
                .collect { _adminAppConfig.value = it }
        }
    }

    fun adminReviewPayment(
        requestId: String,
        approve: Boolean,
        adminNote: String,
        generateUniqueCode: Boolean
    ) {
        if (!_isAdmin.value) return
        val primaryCodeDisplay = _adminAppConfig.value?.primaryCodeDisplay.orEmpty()
        if (approve && !generateUniqueCode && primaryCodeDisplay.isBlank()) {
            _actionMessage.value = "يرجى تعيين الكود الأساسي أولًا في تبويب الأكواد، أو اختر توليد كود فريد"
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.reviewPaymentRequest(
                requestId = requestId,
                approve = approve,
                adminNote = adminNote,
                generateUniqueCode = generateUniqueCode,
                currentPrimaryCode = primaryCodeDisplay
            )
            _isBusy.value = false
            _actionMessage.value = if (res.isSuccess) {
                val issued = res.getOrNull().orEmpty()
                if (approve) "تم اعتماد الدفع! الكود الصادر: $issued" else "تم رفض الطلب"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "فشل مراجعة الطلب"
            }
        }
    }

    fun adminUpdatePrimaryCode(newCode: String, enabled: Boolean) {
        if (!_isAdmin.value) return
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.updatePrimaryActivationCode(newCode, enabled)
            _isBusy.value = false
            _actionMessage.value = if (res.isSuccess) {
                "تم تحديث كود التفعيل الأساسي بنجاح"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "تعذر حفظ الكود"
            }
        }
    }

    fun adminCreateUniqueCode(rawCode: String, maxUses: Long, note: String) {
        if (!_isAdmin.value) return
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.createActivationCode(rawCode, maxUses, note)
            _isBusy.value = false
            _actionMessage.value = if (res.isSuccess) {
                "تم إنشاء كود التفعيل: ${res.getOrNull()}"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "تعذر إنشاء الكود"
            }
        }
    }

    fun adminToggleCode(codeHash: String, currentlyActive: Boolean) {
        if (!_isAdmin.value) return
        viewModelScope.launch {
            repository.toggleActivationCode(codeHash, currentlyActive)
        }
    }

    fun adminToggleUserRevoke(userId: String, currentlyRevoked: Boolean) {
        if (!_isAdmin.value) return
        viewModelScope.launch {
            repository.toggleUserRevocation(userId, currentlyRevoked)
        }
    }

    fun adminActivateUser(userId: String) {
        if (!_isAdmin.value) return
        viewModelScope.launch {
            val res = repository.adminActivateUser(userId)
            _actionMessage.value = if (res.isSuccess) "تم تفعيل المستخدم بنجاح" else "تعذر تفعيل المستخدم"
        }
    }

    fun adminSeedCurriculum() {
        if (!_isAdmin.value) return
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.seedCurriculumToFirestore()
            _isBusy.value = false
            _actionMessage.value = if (res.isSuccess) {
                "تم مزامنة جميع الفصول والاختبارات الـ 12 إلى قاعدة البيانات بنجاح"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "تعذر مزامنة الدروس"
            }
        }
    }

    fun adminLoadLessonForEdit(chapterNumber: Int, onLoaded: (LessonContent) -> Unit) {
        viewModelScope.launch {
            val remote = repository.getLessonContent(chapterNumber).getOrNull()
            val lesson = remote ?: SeerahCurriculum.getBuiltInLesson(chapterNumber)
            if (lesson != null) onLoaded(lesson)
        }
    }

    fun adminSaveLesson(lesson: LessonContent) {
        if (!_isAdmin.value) return
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.saveChapterAndLesson(lesson)
            _isBusy.value = false
            _actionMessage.value = if (res.isSuccess) {
                "تم حفظ تعديلات الفصل والاختبار بنجاح"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "تعذر حفظ التعديلات"
            }
        }
    }

    private fun unwrapFirestoreError(error: Throwable): FirebaseFirestoreException? {
        return generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
    }

    companion object {
        private const val TAG = "SirahViewModel"

        fun factory(userId: String, email: String, displayName: String) = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                SirahViewModel(
                    repository = SirahRepository(db),
                    currentUserId = userId,
                    userEmail = email,
                    displayName = displayName
                )
            }
        }
    }
}
