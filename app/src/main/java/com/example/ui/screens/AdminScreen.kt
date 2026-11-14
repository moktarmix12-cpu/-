package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.firebase.decodeBase64Bitmap
import com.example.data.model.ActivationCode
import com.example.data.model.AppConfig
import com.example.data.model.ChapterMetadata
import com.example.data.model.LessonContent
import com.example.data.model.PaymentRequest
import com.example.data.model.QuizQuestion
import com.example.data.model.UserProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    payments: List<PaymentRequest>,
    users: List<UserProfile>,
    codes: List<ActivationCode>,
    appConfig: AppConfig?,
    chapters: List<ChapterMetadata>,
    isBusy: Boolean,
    onStartObservers: () -> Unit,
    onReviewPayment: (requestId: String, approve: Boolean, adminNote: String, generateUniqueCode: Boolean) -> Unit,
    onUpdatePrimaryCode: (newCode: String, enabled: Boolean) -> Unit,
    onCreateUniqueCode: (rawCode: String, maxUses: Long, note: String) -> Unit,
    onToggleCode: (codeHash: String, currentlyActive: Boolean) -> Unit,
    onToggleUserRevoke: (userId: String, currentlyRevoked: Boolean) -> Unit,
    onAdminActivateUser: (userId: String) -> Unit,
    onSeedCurriculum: () -> Unit,
    onLoadLessonForEdit: (Int, (LessonContent) -> Unit) -> Unit,
    onSaveLesson: (LessonContent) -> Unit,
    onBack: () -> Unit
) {
    LaunchedEffect(Unit) { onStartObservers() }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("طلبات الدفع", "رموز التفعيل", "المستخدمون", "الدروس")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("لوحة إدارة التطبيق", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("admin_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, style = MaterialTheme.typography.labelLarge) },
                        modifier = Modifier.testTag("admin_tab_$index")
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                when (selectedTab) {
                    0 -> AdminPaymentsTab(
                        payments = payments,
                        isBusy = isBusy,
                        onReviewPayment = onReviewPayment
                    )

                    1 -> AdminCodesTab(
                        codes = codes,
                        appConfig = appConfig,
                        isBusy = isBusy,
                        onUpdatePrimaryCode = onUpdatePrimaryCode,
                        onCreateUniqueCode = onCreateUniqueCode,
                        onToggleCode = onToggleCode
                    )

                    2 -> AdminUsersTab(
                        users = users,
                        onToggleUserRevoke = onToggleUserRevoke,
                        onAdminActivateUser = onAdminActivateUser
                    )

                    3 -> AdminLessonsTab(
                        chapters = chapters,
                        isBusy = isBusy,
                        onSeedCurriculum = onSeedCurriculum,
                        onLoadLessonForEdit = onLoadLessonForEdit,
                        onSaveLesson = onSaveLesson
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminPaymentsTab(
    payments: List<PaymentRequest>,
    isBusy: Boolean,
    onReviewPayment: (String, Boolean, String, Boolean) -> Unit
) {
    var receiptPreviewBase64 by remember { mutableStateOf<String?>(null) }
    var noteForReview by remember { mutableStateOf("") }

    if (receiptPreviewBase64 != null) {
        val bmp = remember(receiptPreviewBase64) { decodeBase64Bitmap(receiptPreviewBase64!!) }
        AlertDialog(
            onDismissRequest = { receiptPreviewBase64 = null },
            title = { Text("صورة إيصال التحويل") },
            text = {
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "إيصال الدفع",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text("تعذر عرض الصورة")
                }
            },
            confirmButton = {
                TextButton(onClick = { receiptPreviewBase64 = null }) { Text("إغلاق") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.widthIn(max = 760.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (payments.isEmpty()) {
            item {
                Text("لا توجد طلبات دفع حتى الآن.", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            item {
                OutlinedTextField(
                    value = noteForReview,
                    onValueChange = { noteForReview = it },
                    label = { Text("ملاحظة المراجعة (تُرسل عند الاعتماد أو الرفض)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            items(payments, key = { it.id }) { req ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${req.childName} (${req.method})",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = req.status,
                                style = MaterialTheme.typography.labelLarge,
                                color = when (req.status) {
                                    "APPROVED" -> MaterialTheme.colorScheme.primary
                                    "REJECTED" -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.secondary
                                }
                            )
                        }
                        Text(
                            text = "رقم المرسل: ${req.senderPhone} | المبلغ: ${req.amountMru} MRU",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (req.userEmail.isNotBlank()) {
                            Text("البريد: ${req.userEmail}", style = MaterialTheme.typography.bodyMedium)
                        }
                        if (req.transactionNote.isNotBlank()) {
                            Text("ملاحظة المشتري: ${req.transactionNote}", style = MaterialTheme.typography.bodyMedium)
                        }
                        if (req.issuedCode.isNotBlank()) {
                            Text(
                                text = "الكود الصادر: ${req.issuedCode}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { receiptPreviewBase64 = req.receiptBase64 },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.size(4.dp))
                                Text("الإيصال")
                            }
                            Button(
                                onClick = { onReviewPayment(req.id, true, noteForReview, true) },
                                enabled = !isBusy,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.size(4.dp))
                                Text("اعتماد + كود")
                            }
                            Button(
                                onClick = { onReviewPayment(req.id, false, noteForReview, false) },
                                enabled = !isBusy,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.size(4.dp))
                                Text("رفض")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminCodesTab(
    codes: List<ActivationCode>,
    appConfig: AppConfig?,
    isBusy: Boolean,
    onUpdatePrimaryCode: (String, Boolean) -> Unit,
    onCreateUniqueCode: (String, Long, String) -> Unit,
    onToggleCode: (String, Boolean) -> Unit
) {
    var primaryInput by remember(appConfig?.primaryCodeDisplay) {
        mutableStateOf(appConfig?.primaryCodeDisplay ?: "")
    }
    var primaryEnabled by remember(appConfig?.primaryEnabled) {
        mutableStateOf(appConfig?.primaryEnabled ?: true)
    }

    var newCodeInput by remember { mutableStateOf("") }
    var maxUsesInput by remember { mutableStateOf("1") }
    var noteInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.widthIn(max = 760.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "إدارة كود التفعيل الأساسي",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (appConfig == null) {
                            "النظام يعمل حاليًا بالكود الافتراضي على الخادم. اكتب الكود الجديد هنا لتحديثه أو تغييره في أي وقت."
                        } else {
                            "الكود الأساسي النشط حاليًا: ${appConfig.primaryCodeDisplay}"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = primaryInput,
                        onValueChange = { primaryInput = it },
                        label = { Text("الكود الأساسي الجديد") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_primary_code_input")
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("السماح باستخدام الكود الأساسي", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = primaryEnabled, onCheckedChange = { primaryEnabled = it })
                    }
                    Button(
                        onClick = { onUpdatePrimaryCode(primaryInput, primaryEnabled) },
                        enabled = !isBusy && primaryInput.trim().length >= 3,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("حفظ إعدادات الكود الأساسي")
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "إنشاء كود تفعيل فريد جديد",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedTextField(
                        value = newCodeInput,
                        onValueChange = { newCodeInput = it },
                        label = { Text("الكود (اتركه فارغًا للتوليد التلقائي)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = maxUsesInput,
                            onValueChange = { maxUsesInput = it.filter { ch -> ch.isDigit() } },
                            label = { Text("أقصى استخدام") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = noteInput,
                            onValueChange = { noteInput = it },
                            label = { Text("ملاحظة") },
                            singleLine = true,
                            modifier = Modifier.weight(2f)
                        )
                    }
                    Button(
                        onClick = {
                            val maxUses = maxUsesInput.toLongOrNull() ?: 1L
                            onCreateUniqueCode(newCodeInput, maxUses, noteInput)
                            newCodeInput = ""
                            noteInput = ""
                        },
                        enabled = !isBusy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("إنشاء كود تفعيل")
                    }
                }
            }
        }

        items(codes, key = { it.id }) { code ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(code.codeDisplay, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "الاستخدام: ${code.usedCount} / ${code.maxUses} • ${if (code.isActive) "نشط" else "ملغى"}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (code.note.isNotBlank()) {
                            Text(code.note, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Button(
                        onClick = { onToggleCode(code.id, code.isActive) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (code.isActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(if (code.isActive) "إلغاء" else "تفعيل")
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminUsersTab(
    users: List<UserProfile>,
    onToggleUserRevoke: (String, Boolean) -> Unit,
    onAdminActivateUser: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.widthIn(max = 760.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(users, key = { it.userId }) { user ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(user.childName, style = MaterialTheme.typography.titleMedium)
                    if (user.email.isNotBlank()) {
                        Text(user.email, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(
                        text = "التفعيل: ${if (user.hasFullAccess) "مفعّل (${user.activationMethod})" else if (user.revoked) "محظور/ملغى" else "نسخة مجانية"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "النجوم: ${user.stars} ⭐ | النقاط: ${user.totalScore} | المكتمل: ${user.completedChapters.size}/12",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!user.hasFullAccess) {
                            Button(onClick = { onAdminActivateUser(user.userId) }) {
                                Text("تفعيل يدوي")
                            }
                        }
                        OutlinedButton(
                            onClick = { onToggleUserRevoke(user.userId, user.revoked) }
                        ) {
                            Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(if (user.revoked) "فك الإيقاف" else "إيقاف الوصول")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminLessonsTab(
    chapters: List<ChapterMetadata>,
    isBusy: Boolean,
    onSeedCurriculum: () -> Unit,
    onLoadLessonForEdit: (Int, (LessonContent) -> Unit) -> Unit,
    onSaveLesson: (LessonContent) -> Unit
) {
    var selectedChapterNum by remember { mutableIntStateOf(1) }
    var editingLesson by remember { mutableStateOf<LessonContent?>(null) }

    var titleField by remember { mutableStateOf("") }
    var contentField by remember { mutableStateOf("") }
    var sourceField by remember { mutableStateOf("") }
    var isFreeField by remember { mutableStateOf(false) }
    var questionsList by remember { mutableStateOf<List<QuizQuestion>>(emptyList()) }

    var newQText by remember { mutableStateOf("") }
    var newOpt0 by remember { mutableStateOf("") }
    var newOpt1 by remember { mutableStateOf("") }
    var newOpt2 by remember { mutableStateOf("") }
    var newOpt3 by remember { mutableStateOf("") }
    var newCorrectIdx by remember { mutableIntStateOf(0) }

    LaunchedEffect(selectedChapterNum) {
        onLoadLessonForEdit(selectedChapterNum) { loaded ->
            editingLesson = loaded
            titleField = loaded.title
            contentField = loaded.content
            sourceField = loaded.sourceNote
            isFreeField = loaded.isFree
            questionsList = loaded.questions
        }
    }

    Column(
        modifier = Modifier
            .widthIn(max = 760.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onSeedCurriculum,
            enabled = !isBusy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Sync, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text("نشر / مزامنة الفصول الـ 12 الافتراضية لقاعدة البيانات")
        }

        Text("اختر الفصل لتعديله:", style = MaterialTheme.typography.titleMedium)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            (1..6).forEach { num ->
                FilterChip(
                    selected = selectedChapterNum == num,
                    onClick = { selectedChapterNum = num },
                    label = { Text("$num") }
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            (7..12).forEach { num ->
                FilterChip(
                    selected = selectedChapterNum == num,
                    onClick = { selectedChapterNum = num },
                    label = { Text("$num") }
                )
            }
        }

        if (editingLesson != null) {
            OutlinedTextField(
                value = titleField,
                onValueChange = { titleField = it },
                label = { Text("عنوان الفصل") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("فصل مجاني للمعاينة")
                Switch(checked = isFreeField, onCheckedChange = { isFreeField = it })
            }
            OutlinedTextField(
                value = contentField,
                onValueChange = { contentField = it },
                label = { Text("نص الدرس للأطفال") },
                minLines = 5,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = sourceField,
                onValueChange = { sourceField = it },
                label = { Text("المصدر الموثوق") },
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider()
            Text("أسئلة الفصل (${questionsList.size})", style = MaterialTheme.typography.titleMedium)

            questionsList.forEachIndexed { idx, q ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${idx + 1}. ${q.question}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            questionsList = questionsList.toMutableList().apply { removeAt(idx) }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف السؤال", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            Text("إضافة سؤال جديد للفصل:", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = newQText,
                onValueChange = { newQText = it },
                label = { Text("نص السؤال") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(value = newOpt0, onValueChange = { newOpt0 = it }, label = { Text("الخيار 1") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = newOpt1, onValueChange = { newOpt1 = it }, label = { Text("الخيار 2") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = newOpt2, onValueChange = { newOpt2 = it }, label = { Text("الخيار 3") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = newOpt3, onValueChange = { newOpt3 = it }, label = { Text("الخيار 4") }, modifier = Modifier.fillMaxWidth())

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (0..3).forEach { idx ->
                    FilterChip(
                        selected = newCorrectIdx == idx,
                        onClick = { newCorrectIdx = idx },
                        label = { Text("صحيح: ${idx + 1}") }
                    )
                }
            }

            OutlinedButton(
                onClick = {
                    val opts = listOf(newOpt0, newOpt1, newOpt2, newOpt3).map { it.trim() }.filter { it.isNotEmpty() }
                    if (newQText.isNotBlank() && opts.size >= 2) {
                        val safeIdx = newCorrectIdx.coerceIn(0, opts.lastIndex).toLong()
                        questionsList = questionsList + QuizQuestion(newQText.trim(), opts, safeIdx)
                        newQText = ""; newOpt0 = ""; newOpt1 = ""; newOpt2 = ""; newOpt3 = ""
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("إضافة السؤال للقائمة")
            }

            Button(
                onClick = {
                    val base = editingLesson!!
                    onSaveLesson(
                        base.copy(
                            title = titleField,
                            content = contentField,
                            sourceNote = sourceField,
                            isFree = isFreeField,
                            questions = questionsList
                        )
                    )
                },
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("حفظ الفصل والاختبارات في قاعدة البيانات")
            }
        }
    }
}
