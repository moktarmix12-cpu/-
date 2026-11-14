package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.PaymentRequest

private val ALLOWED_PAYMENT_METHODS = listOf("Bankily", "Mouv Money", "Sedad", "Click")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PaymentActivationScreen(
    userPayments: List<PaymentRequest>,
    isBusy: Boolean,
    onSubmitReceipt: (method: String, senderPhone: String, note: String, uri: Uri) -> Unit,
    onActivateCode: (String) -> Unit,
    onCopyMessage: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var selectedMethod by remember { mutableStateOf(ALLOWED_PAYMENT_METHODS.first()) }
    var senderPhone by remember { mutableStateOf("") }
    var transactionNote by remember { mutableStateOf("") }
    var pickedReceiptUri by remember { mutableStateOf<Uri?>(null) }
    var activationCode by remember { mutableStateOf("") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pickedReceiptUri = uri
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الدفع وتفعيل النسخة الكاملة", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("payment_back_button")) {
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Instructions Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "200 أوقية موريتانية جديدة (MRU)",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "دفعة واحدة فقط لفتح جميع الفصول الـ 12 مدى الحياة، بدون اشتراك شهري وبدون إعلانات.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "رقم استقبال التحويل:",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = "49719739",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        clipboard.setText(AnnotatedString("49719739"))
                                        onCopyMessage("تم نسخ الرقم 49719739")
                                    },
                                    modifier = Modifier.testTag("copy_phone_button")
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "نسخ الرقم", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text("نسخ")
                                }
                            }
                        }

                        Text(
                            text = "تنبيه مهم: إرسال الإيصال لا يفتح الدروس تلقائيًا؛ بل يراجعه صاحب التطبيق يدويًا ويتأكد من وصول المبلغ، ثم يُصدر لك كود التفعيل.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                // Latest payment request status if exists
                val latestPayment = userPayments.firstOrNull()
                if (latestPayment != null) {
                    val statusColor = when (latestPayment.status) {
                        "APPROVED" -> MaterialTheme.colorScheme.primaryContainer
                        "REJECTED" -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.secondaryContainer
                    }
                    Surface(
                        color = statusColor,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val statusText = when (latestPayment.status) {
                                "APPROVED" -> "حالة طلب الدفع: تم تأكيد الدفع ✅"
                                "REJECTED" -> "حالة طلب الدفع: مرفوض ❌"
                                else -> "حالة طلب الدفع: قيد المراجعة اليدوية ⏳"
                            }
                            Text(text = statusText, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "الوسيلة: ${latestPayment.method} | المبلغ: ${latestPayment.amountMru} MRU",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (latestPayment.adminNote.isNotBlank()) {
                                Text(
                                    text = "ملاحظة الإدارة: ${latestPayment.adminNote}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            if (latestPayment.status == "APPROVED" && latestPayment.issuedCode.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "كودك: ${latestPayment.issuedCode}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            activationCode = latestPayment.issuedCode
                                            clipboard.setText(AnnotatedString(latestPayment.issuedCode))
                                            onCopyMessage("تم نسخ الكود وتعبئته في خانة التفعيل")
                                        }
                                    ) {
                                        Text("استخدام الكود")
                                    }
                                }
                            }
                        }
                    }
                }

                // Step 1: Manual Payment Receipt Submission
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "1. إرسال إيصال التحويل اليدوي",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = "اختر وسيلة الدفع المسموح بها:",
                            style = MaterialTheme.typography.labelLarge
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ALLOWED_PAYMENT_METHODS.forEach { method ->
                                FilterChip(
                                    selected = selectedMethod == method,
                                    onClick = { selectedMethod = method },
                                    label = { Text(method) },
                                    modifier = Modifier.testTag("method_chip_$method")
                                )
                            }
                        }

                        OutlinedTextField(
                            value = senderPhone,
                            onValueChange = { if (it.length <= 24) senderPhone = it },
                            label = { Text("رقم الهاتف المحوَّل منه") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sender_phone_input")
                        )

                        OutlinedTextField(
                            value = transactionNote,
                            onValueChange = { if (it.length <= 200) transactionNote = it },
                            label = { Text("رقم العملية أو ملاحظة (اختياري)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("payment_note_input")
                        )

                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("pick_receipt_button")
                        ) {
                            Icon(
                                imageVector = if (pickedReceiptUri != null) Icons.Default.CheckCircle else Icons.Default.PhotoLibrary,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = if (pickedReceiptUri != null) {
                                    "تم اختيار صورة الإيصال ✅ (اضغط للتغيير)"
                                } else {
                                    "إرفاق صورة إيصال الدفع"
                                }
                            )
                        }

                        Button(
                            onClick = {
                                val uri = pickedReceiptUri ?: return@Button
                                onSubmitReceipt(selectedMethod, senderPhone, transactionNote, uri)
                            },
                            enabled = !isBusy && pickedReceiptUri != null && senderPhone.trim().length >= 6,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("submit_receipt_button")
                        ) {
                            if (isBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = MaterialTheme.colorScheme.onSecondary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                                Spacer(modifier = Modifier.size(8.dp))
                                Text("إرسال الإيصال للمراجعة")
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Step 2: Activation Code Entry
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "2. تفعيل التطبيق بالكود",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "بعد تأكيد التحويل من صاحب التطبيق، أدخل كود التفعيل لفتح جميع الفصول فورًا.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = activationCode,
                            onValueChange = { if (it.length <= 40) activationCode = it },
                            label = { Text("أدخل كود التفعيل") },
                            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("activation_code_input")
                        )

                        Button(
                            onClick = { onActivateCode(activationCode) },
                            enabled = !isBusy && activationCode.trim().length >= 3,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("activate_code_button")
                        ) {
                            Text("تفعيل التطبيق الآن", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
