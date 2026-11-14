package com.example.ui.screens

import android.content.Intent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.curriculum.SeerahSummaryData
import com.example.data.curriculum.SeerahSummarySection
import com.example.ui.components.ChapterIllustrationBanner
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SummaryScreen(
    hasFullAccess: Boolean,
    isAdmin: Boolean,
    onOpenChapter: (Int, Boolean) -> Unit,
    onOpenPayment: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedStageId by remember { mutableStateOf("all") }
    var textScaleSp by remember { mutableFloatStateOf(17f) }
    var ttsReady by remember { mutableStateOf(false) }
    var ttsMissingArabic by remember { mutableStateOf(false) }
    var speakingSectionId by remember { mutableStateOf<String?>(null) }
    var ttsInstance by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(context) {
        val tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val engine = ttsInstance
                if (engine != null) {
                    val result = engine.setLanguage(Locale.forLanguageTag("ar"))
                    engine.setSpeechRate(0.88f)
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        ttsMissingArabic = true
                        ttsReady = false
                    } else {
                        ttsReady = true
                        ttsMissingArabic = false
                    }
                }
            }
        }
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                speakingSectionId = null
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                speakingSectionId = null
            }
        })
        ttsInstance = tts

        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    fun speakOrStop(sectionId: String, text: String) {
        val engine = ttsInstance
        if (engine == null || !ttsReady) {
            ttsMissingArabic = true
            return
        }
        if (speakingSectionId == sectionId) {
            engine.stop()
            speakingSectionId = null
        } else {
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "summary_$sectionId")
            speakingSectionId = sectionId
        }
    }

    val visibleSections = remember(selectedStageId) {
        if (selectedStageId == "all") {
            SeerahSummaryData.sections
        } else {
            SeerahSummaryData.sections.filter { it.id == selectedStageId }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = SeerahSummaryData.mainTitle,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            ttsInstance?.stop()
                            onBack()
                        },
                        modifier = Modifier.testTag("summary_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { textScaleSp = (textScaleSp - 1.5f).coerceAtLeast(14f) },
                        modifier = Modifier.testTag("summary_font_decrease")
                    ) {
                        Icon(Icons.Default.TextDecrease, contentDescription = "تصغير الخط")
                    }
                    IconButton(
                        onClick = { textScaleSp = (textScaleSp + 1.5f).coerceAtMost(24f) },
                        modifier = Modifier.testTag("summary_font_increase")
                    ) {
                        Icon(Icons.Default.TextIncrease, contentDescription = "تكبير الخط")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 760.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                speakOrStop("full_summary", SeerahSummaryData.fullSpokenText())
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (speakingSectionId == "full_summary") {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.secondary
                                }
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("summary_tts_all_button")
                        ) {
                            Icon(
                                imageVector = if (speakingSectionId == "full_summary") {
                                    Icons.Default.Stop
                                } else {
                                    Icons.AutoMirrored.Filled.VolumeUp
                                },
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = if (speakingSectionId == "full_summary") {
                                    "إيقاف القراءة الصوتية"
                                } else {
                                    "استمع إلى السيرة المختصرة كاملة"
                                },
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        if (ttsMissingArabic) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "حزمة النطق باللغة العربية غير مثبتة على هذا الجهاز بعد. يمكنك تفعيلها مجانًا من إعدادات تحويل النص إلى كلام.",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            runCatching {
                                                context.startActivity(Intent("com.android.settings.TTS_SETTINGS"))
                                            }
                                        }
                                    ) {
                                        Text("فتح إعدادات الصوت العربي")
                                    }
                                }
                            }
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedStageId == "all",
                                    onClick = { selectedStageId = "all" },
                                    label = { Text("الكل (${SeerahSummaryData.sections.size})") }
                                )
                            }
                            items(SeerahSummaryData.sections, key = { it.id }) { stage ->
                                FilterChip(
                                    selected = selectedStageId == stage.id,
                                    onClick = { selectedStageId = stage.id },
                                    label = { Text(stage.periodLabel) }
                                )
                            }
                        }
                    }
                }

                items(visibleSections, key = { it.id }) { section ->
                    SummarySectionCard(
                        section = section,
                        textScaleSp = textScaleSp,
                        isSpeaking = speakingSectionId == section.id,
                        hasFullAccess = hasFullAccess,
                        isAdmin = isAdmin,
                        onToggleSpeak = {
                            speakOrStop(section.id, "${section.title}. ${section.body}")
                        },
                        onOpenChapter = { chNum ->
                            val unlocked = chNum == 1 || hasFullAccess || isAdmin
                            ttsInstance?.stop()
                            speakingSectionId = null
                            onOpenChapter(chNum, unlocked)
                        }
                    )
                }

                if (!hasFullAccess && !isAdmin) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "افتح الفصول التفاعلية الـ 12 والاختبارات والشهادة",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "هذا الموجز مقدمة شاملة، وعند التفعيل بدفعة واحدة (200 MRU) تُفتح جميع الدروس الـ 12 وأسئلتها وشهادة الإنجاز باسم الطفل.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Button(
                                    onClick = {
                                        ttsInstance?.stop()
                                        onOpenPayment()
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Icon(Icons.Default.LockOpen, contentDescription = null)
                                    Spacer(modifier = Modifier.size(8.dp))
                                    Text("تفعيل النسخة الكاملة (200 MRU)")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SummarySectionCard(
    section: SeerahSummarySection,
    textScaleSp: Float,
    isSpeaking: Boolean,
    hasFullAccess: Boolean,
    isAdmin: Boolean,
    onToggleSpeak: () -> Unit,
    onOpenChapter: (Int) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("summary_section_${section.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ChapterIllustrationBanner(
                illustration = section.illustration,
                contentDescription = section.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = section.periodLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                OutlinedButton(
                    onClick = onToggleSpeak,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("summary_speak_${section.id}")
                ) {
                    Icon(
                        imageVector = if (isSpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(if (isSpeaking) "إيقاف" else "استمع")
                }
            }

            Text(
                text = section.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = section.body,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = textScaleSp.sp,
                    lineHeight = (textScaleSp * 1.65f).sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            if (section.relatedChapters.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    section.relatedChapters.forEach { chNum ->
                        val unlocked = chNum == 1 || hasFullAccess || isAdmin
                        AssistChip(
                            onClick = { onOpenChapter(chNum) },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (unlocked) {
                                        Icons.AutoMirrored.Filled.MenuBook
                                    } else {
                                        Icons.Default.Lock
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = {
                                Text(if (chNum == 1) "الفصل 1 (مجاني)" else "الفصل $chNum")
                            }
                        )
                    }
                }
            }
        }
    }
}
