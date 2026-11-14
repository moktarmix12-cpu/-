package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.data.model.UserProfile
import com.example.ui.viewmodel.UiState
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CertificateScreen(
    userProfileState: UiState<UserProfile>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val profile = (userProfileState as? UiState.Success)?.data
    val completedCount = profile?.completedChapters?.size ?: 0
    val isEligible = completedCount >= 12

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("شهادة الإنجاز", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("certificate_back_button")) {
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
                .padding(innerPadding)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 580.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(3.dp, MaterialTheme.colorScheme.secondary),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(26.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = "وسام الشهادة",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(68.dp)
                        )
                        Text(
                            text = "شهادة شكر وتقدير",
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "تُمنح للبطل الصغير:",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = profile?.childName ?: "بطل السيرة",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = TextAlign.Center
                        )

                        if (isEligible) {
                            Text(
                                text = "لإتمامه جميع فصول كتاب «رحلتي مع سيرة الرسول ﷺ» الاثني عشر واجتياز اختباراتها بنجاح وتميّز.",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Text(
                                text = "أكملت حتى الآن $completedCount من 12 فصلًا.\nأكمل بقية الفصول والاختبارات لتستحق شهادة الإتمام الكاملة!",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.size(4.dp))
                                Text(
                                    text = "${profile?.stars ?: 0} نجمة",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Text(
                                text = "•  النقاط: ${profile?.totalScore ?: 0}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (profile != null && isEligible) {
                    Button(
                        onClick = { shareCertificatePdf(context, profile) },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("share_certificate_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("مشاركة وحفظ الشهادة (PDF)", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

private fun shareCertificatePdf(context: Context, profile: UserProfile) {
    val pdf = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(842, 595, 1).create()
    val page = pdf.startPage(pageInfo)
    val canvas = page.canvas

    val bgPaint = Paint().apply { color = Color.rgb(250, 246, 238) }
    canvas.drawRect(0f, 0f, 842f, 595f, bgPaint)

    val borderPaint = Paint().apply {
        color = Color.rgb(197, 139, 23)
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }
    canvas.drawRect(28f, 28f, 814f, 567f, borderPaint)

    val titlePaint = Paint().apply {
        color = Color.rgb(20, 83, 45)
        textSize = 36f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    val namePaint = Paint().apply {
        color = Color.rgb(197, 139, 23)
        textSize = 32f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    val bodyPaint = Paint().apply {
        color = Color.rgb(24, 36, 28)
        textSize = 22f
        textAlign = Paint.Align.CENTER
    }

    canvas.drawText("شهادة إتمام وإنجاز", 421f, 130f, titlePaint)
    canvas.drawText("رحلتي مع سيرة الرسول ﷺ", 421f, 180f, bodyPaint)
    canvas.drawText("تُمنح للبطل: ${profile.childName}", 421f, 265f, namePaint)
    canvas.drawText("لإتمامه جميع الفصول الـ 12 واختباراتها بنجاح", 421f, 335f, bodyPaint)
    canvas.drawText("مجموع النجوم: ${profile.stars}   |   مجموع النقاط: ${profile.totalScore}", 421f, 405f, bodyPaint)
    canvas.drawText("بارك الله فيك وزادك علمًا ونورًا", 421f, 480f, titlePaint)

    pdf.finishPage(page)

    val dir = File(context.cacheDir, "certificates").apply { mkdirs() }
    val outFile = File(dir, "sirah_certificate.pdf")
    FileOutputStream(outFile).use { pdf.writeTo(it) }
    pdf.close()

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "مشاركة الشهادة"))
}
