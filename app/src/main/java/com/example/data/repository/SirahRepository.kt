package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.curriculum.SeerahCurriculum
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.firebase.sha256Hex
import com.example.data.model.ActivationCode
import com.example.data.model.AppConfig
import com.example.data.model.ChapterMetadata
import com.example.data.model.LessonContent
import com.example.data.model.PaymentRequest
import com.example.data.model.UserProfile
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom

class SirahRepository(
    private val db: FirebaseFirestore
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    // ==================== USER PROFILE & PROGRESS ====================

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureUserProfile(
        userId: String,
        defaultName: String,
        email: String
    ): Result<Unit> {
        val docRef = db.collection("users").document(userId)
        return try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val safeName = defaultName.trim().ifBlank { "بطل السيرة" }.take(60)
                val payload = mutableMapOf<String, Any>(
                    "userId" to userId,
                    "childName" to safeName,
                    "isActivated" to false,
                    "revoked" to false,
                    "activationMethod" to "none",
                    "stars" to 0L,
                    "totalScore" to 0L,
                    "completedChapters" to emptyList<Long>(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (email.isNotBlank()) {
                    payload["email"] = email.take(120)
                }
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateChildName(userId: String, childName: String): Result<Unit> {
        val safeName = childName.trim().take(60)
        if (safeName.isBlank()) return Result.failure(IllegalArgumentException("يرجى إدخال اسم صحيح"))
        val docRef = db.collection("users").document(userId)
        return try {
            docRef.update(
                mapOf(
                    "childName" to safeName,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun recordChapterCompletion(
        userId: String,
        chapterNumber: Int,
        earnedStars: Int,
        earnedScore: Int,
        existingCompleted: List<Long>,
        currentStars: Long,
        currentScore: Long
    ): Result<Unit> {
        val docRef = db.collection("users").document(userId)
        return try {
            val updatedList = (existingCompleted + chapterNumber.toLong()).distinct().sorted().take(12)
            val alreadyCompleted = existingCompleted.contains(chapterNumber.toLong())
            val newStars = if (alreadyCompleted) currentStars else (currentStars + earnedStars).coerceAtMost(1000L)
            val newScore = if (alreadyCompleted) currentScore else (currentScore + earnedScore).coerceAtMost(10000L)
            docRef.update(
                mapOf(
                    "completedChapters" to updatedList,
                    "stars" to newStars,
                    "totalScore" to newScore,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    // ==================== CHAPTERS & LESSONS ====================

    fun observeChaptersCatalog(): Flow<List<ChapterMetadata>> = flow {
        val path = "chapters"
        emitAll(
            db.collection(path)
                .orderBy("chapterNumber")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ChapterMetadata::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun getLessonContent(chapterNumber: Int): Result<LessonContent?> {
        val docRef = db.collection("lessonContents").document(chapterNumber.toString())
        return try {
            val snap = docRef.get().await()
            val lesson = if (snap.exists()) {
                snap.toObject(
                    LessonContent::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            } else {
                null
            }
            Result.success(lesson)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    // ==================== PAYMENT & ACTIVATION ====================

    fun observeUserPayments(userId: String): Flow<List<PaymentRequest>> = flow {
        val path = "paymentRequests"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PaymentRequest::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun submitPaymentRequest(
        userId: String,
        childName: String,
        userEmail: String,
        method: String,
        senderPhone: String,
        transactionNote: String,
        receiptBase64: String
    ): Result<String> {
        val docRef = db.collection("paymentRequests").document()
        return try {
            val payload = mutableMapOf<String, Any>(
                "userId" to userId,
                "childName" to childName.trim().ifBlank { "طفل" }.take(60),
                "method" to method,
                "senderPhone" to senderPhone.trim().take(30),
                "receiverPhone" to "49719739",
                "amountMru" to 200L,
                "receiptBase64" to receiptBase64,
                "status" to "PENDING",
                "adminNote" to "",
                "issuedCode" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (userEmail.isNotBlank()) payload["userEmail"] = userEmail.take(120)
            if (transactionNote.isNotBlank()) payload["transactionNote"] = transactionNote.trim().take(300)

            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun redeemActivationCode(userId: String, rawCode: String): Result<String> {
        val cleanCode = rawCode.trim()
        if (cleanCode.length < 3) {
            return Result.failure(IllegalArgumentException("يرجى إدخال كود تفعيل صحيح"))
        }
        val codeHash = sha256Hex(cleanCode)
        val userRef = db.collection("users").document(userId)
        val uniqueCodeRef = db.collection("activationCodes").document(codeHash)

        // 1. Try unique single/multi-use activation code atomically
        try {
            val batch = db.batch()
            batch.update(
                userRef,
                mapOf(
                    "isActivated" to true,
                    "activationMethod" to "unique_code",
                    "redeemedCodeHash" to codeHash,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                uniqueCodeRef,
                mapOf(
                    "usedCount" to FieldValue.increment(1L),
                    "boundUserId" to userId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            return Result.success("تم تفعيل التطبيق بنجاح بكود التفعيل الفريد!")
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Fall through to check server-side primary code
        }

        // 2. Try primary activation code (verified purely on Firestore rules side)
        return try {
            userRef.update(
                mapOf(
                    "isActivated" to true,
                    "activationMethod" to "primary_code",
                    "redeemedCodeHash" to codeHash,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success("تم تفعيل النسخة الكاملة بنجاح! فُتحت جميع الفصول.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userRef.path)
            Result.failure(IllegalStateException("كود التفعيل غير صحيح أو ملغى أو مستخدم مسبقًا"))
        }
    }

    // ==================== ADMIN OPERATIONS ====================

    suspend fun checkIsAdmin(userId: String, email: String?): Boolean {
        if (email.equals("moktar.mix12@gmail.com", ignoreCase = true)) return true
        return try {
            val doc = db.collection("admins").document(userId).get().await()
            doc.exists()
        } catch (_: Exception) {
            false
        }
    }

    fun observeAllPayments(): Flow<List<PaymentRequest>> = flow {
        val path = "paymentRequests"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        PaymentRequest::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeAllUsers(): Flow<List<UserProfile>> = flow {
        val path = "users"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeAllActivationCodes(): Flow<List<ActivationCode>> = flow {
        val path = "activationCodes"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        ActivationCode::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observePrimaryActivationConfig(): Flow<AppConfig?> = flow {
        val path = "appConfig/activation"
        emitAll(
            db.collection("appConfig").document("activation")
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(
                            AppConfig::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    throw error
                }
        )
    }

    suspend fun reviewPaymentRequest(
        requestId: String,
        approve: Boolean,
        adminNote: String,
        generateUniqueCode: Boolean,
        currentPrimaryCode: String
    ): Result<String> {
        val reqRef = db.collection("paymentRequests").document(requestId)
        return try {
            if (!approve) {
                reqRef.update(
                    mapOf(
                        "status" to "REJECTED",
                        "adminNote" to adminNote.trim().ifBlank { "لم يتأكد وصول التحويل أو الإيصال غير واضح" }.take(300),
                        "issuedCode" to "",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                return Result.success("")
            }

            val codeToIssue = if (generateUniqueCode) {
                val created = createActivationCode("", 1L, "طلب دفع رقم $requestId").getOrThrow()
                created
            } else {
                currentPrimaryCode.trim()
            }

            reqRef.update(
                mapOf(
                    "status" to "APPROVED",
                    "adminNote" to adminNote.trim().ifBlank { "تم التحقق من وصول 200 MRU" }.take(300),
                    "issuedCode" to codeToIssue,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(codeToIssue)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, reqRef.path)
            Result.failure(e)
        }
    }

    suspend fun updatePrimaryActivationCode(newCode: String, enabled: Boolean): Result<Unit> {
        val clean = newCode.trim()
        if (clean.length < 3 || clean.length > 40) {
            return Result.failure(IllegalArgumentException("يجب أن يكون الكود بين 3 و 40 حرفًا"))
        }
        val docRef = db.collection("appConfig").document("activation")
        return try {
            docRef.set(
                mapOf(
                    "primaryCodeHash" to sha256Hex(clean),
                    "primaryCodeDisplay" to clean,
                    "primaryEnabled" to enabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun createActivationCode(rawCode: String, maxUses: Long, note: String): Result<String> {
        val codeText = rawCode.trim().ifBlank {
            val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
            val rnd = SecureRandom()
            "SR-" + (1..6).map { chars[rnd.nextInt(chars.length)] }.joinToString("")
        }
        if (codeText.length < 3 || codeText.length > 40) {
            return Result.failure(IllegalArgumentException("طول الكود يجب أن يكون بين 3 و 40 حرفًا"))
        }
        val codeHash = sha256Hex(codeText)
        val docRef = db.collection("activationCodes").document(codeHash)
        return try {
            docRef.set(
                mapOf(
                    "codeDisplay" to codeText,
                    "isActive" to true,
                    "maxUses" to maxUses.coerceIn(1L, 10000L),
                    "usedCount" to 0L,
                    "boundUserId" to "",
                    "note" to note.trim().take(200),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(codeText)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleActivationCode(codeHash: String, currentlyActive: Boolean): Result<Unit> {
        val docRef = db.collection("activationCodes").document(codeHash)
        return try {
            docRef.update(
                mapOf(
                    "isActive" to !currentlyActive,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleUserRevocation(userId: String, currentlyRevoked: Boolean): Result<Unit> {
        val docRef = db.collection("users").document(userId)
        return try {
            val newRevoked = !currentlyRevoked
            val updates = mutableMapOf<String, Any>(
                "revoked" to newRevoked,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (newRevoked) {
                updates["isActivated"] = false
            }
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun adminActivateUser(userId: String): Result<Unit> {
        val docRef = db.collection("users").document(userId)
        return try {
            docRef.update(
                mapOf(
                    "isActivated" to true,
                    "revoked" to false,
                    "activationMethod" to "admin",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun seedCurriculumToFirestore(): Result<Unit> {
        return try {
            val batch = db.batch()
            for (lesson in SeerahCurriculum.chapters) {
                val chapterRef = db.collection("chapters").document(lesson.id)
                val lessonRef = db.collection("lessonContents").document(lesson.id)
                batch.set(
                    chapterRef,
                    mapOf(
                        "chapterNumber" to lesson.chapterNumber,
                        "title" to lesson.title,
                        "shortDescription" to (lesson.content.lineSequence().firstOrNull()?.take(80) ?: ""),
                        "illustration" to lesson.illustration,
                        "isFree" to lesson.isFree,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.set(
                    lessonRef,
                    mapOf(
                        "chapterNumber" to lesson.chapterNumber,
                        "title" to lesson.title,
                        "illustration" to lesson.illustration,
                        "isFree" to lesson.isFree,
                        "content" to lesson.content,
                        "sourceNote" to lesson.sourceNote,
                        "questions" to lesson.questions.map { q ->
                            mapOf(
                                "question" to q.question,
                                "options" to q.options,
                                "correctIndex" to q.correctIndex
                            )
                        },
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "chapters")
            Result.failure(e)
        }
    }

    suspend fun saveChapterAndLesson(lesson: LessonContent): Result<Unit> {
        return try {
            val batch = db.batch()
            val chapterRef = db.collection("chapters").document(lesson.chapterNumber.toString())
            val lessonRef = db.collection("lessonContents").document(lesson.chapterNumber.toString())
            batch.set(
                chapterRef,
                mapOf(
                    "chapterNumber" to lesson.chapterNumber,
                    "title" to lesson.title.trim().take(120),
                    "shortDescription" to (lesson.content.lineSequence().firstOrNull()?.take(80) ?: ""),
                    "illustration" to lesson.illustration,
                    "isFree" to lesson.isFree,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                lessonRef,
                mapOf(
                    "chapterNumber" to lesson.chapterNumber,
                    "title" to lesson.title.trim().take(120),
                    "illustration" to lesson.illustration,
                    "isFree" to lesson.isFree,
                    "content" to lesson.content.trim().take(15000),
                    "sourceNote" to lesson.sourceNote.trim().take(500),
                    "questions" to lesson.questions.take(20).map { q ->
                        mapOf(
                            "question" to q.question.trim(),
                            "options" to q.options.map { it.trim() },
                            "correctIndex" to q.correctIndex
                        )
                    },
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "lessonContents/${lesson.chapterNumber}")
            Result.failure(e)
        }
    }
}
