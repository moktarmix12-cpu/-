package com.example.ui.screens

import android.content.Intent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.LessonContent
import com.example.ui.components.ChapterIllustrationBanner
import com.example.ui.viewmodel.UiState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonScreen(
    lessonState: UiState<LessonContent>,
    onBack: () -> Unit,
    onStartQuiz: (Int) -> Unit,
    onOpenPayment: () -> Unit
) {
    val context = LocalContext.current
    var ttsReady by remember { mutableStateOf(false) }
    var ttsMissingArabic by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
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
            override fun onStart(utteranceId: String?) {
                isSpeaking = true
            }

            override fun onDone(utteranceId: String?) {
                isSpeaking = false
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                isSpeaking = false
            }
        })
        ttsInstance = tts

        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = (lessonState as? UiState.Success)?.data?.let {
                        "الفصل ${it.chapterNumber}: ${it.title}"
                    } ?: "درس السيرة"
                    Text(title, style = MaterialTheme.typography.titleLarge)
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            ttsInstance?.stop()
                            onBack()
                        },
                        modifier = Modifier.testTag("lesson_back_button")
                    ) {
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
            when (lessonState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is UiState.Error -> {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 520.dp)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = lessonState.message,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = onOpenPayment) {
                            Text("الذهاب إلى التفعيل")
                        }
                    }
                }

                is UiState.Success -> {
                    val lesson = lessonState.data
                    Column(
                        modifier = Modifier
                            .widthIn(max = 720.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ChapterIllustrationBanner(
                            illustration = lesson.illustration,
                            contentDescription = lesson.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )

                        Text(
                            text = lesson.title,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Audio narration button (Arabic TTS)
                        Button(
                            onClick = {
                                val engine = ttsInstance
                                if (engine == null || !ttsReady) {
                                    ttsMissingArabic = true
                                    return@Button
                                }
                                if (isSpeaking) {
                                    engine.stop()
                                    isSpeaking = false
                                } else {
                                    val spokenText = "${lesson.title}. ${lesson.content}"
                                    engine.speak(
                                        spokenText,
                                        TextToSpeech.QUEUE_FLUSH,
                                        null,
                                        "sirah_lesson_${lesson.chapterNumber}"
                                    )
                                    isSpeaking = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSpeaking) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.secondary
                                }
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("tts_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = if (isSpeaking) "إيقاف القراءة الصوتية" else "استمع للدرس بصوت عربي واضح",
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

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = lesson.content,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(20.dp)
                            )
                        }

                        if (lesson.sourceNote.isNotBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "المصدر الموثوق: ${lesson.sourceNote}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        if (lesson.questions.isNotEmpty()) {
                            Button(
                                onClick = {
                                    ttsInstance?.stop()
                                    isSpeaking = false
                                    onStartQuiz(lesson.chapterNumber.toInt())
                                },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .testTag("start_quiz_button")
                            ) {
                                Icon(imageVector = Icons.Default.Quiz, contentDescription = null)
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(
                                    text = "ابدأ اختبار الفصل (${lesson.questions.size} أسئلة)",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}
