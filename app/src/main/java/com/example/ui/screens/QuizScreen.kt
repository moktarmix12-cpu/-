package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.LessonContent
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    lessonState: UiState<LessonContent>,
    onQuizFinished: (chapterNumber: Int, correctCount: Int, total: Int) -> Unit,
    onBack: () -> Unit
) {
    val lesson = (lessonState as? UiState.Success)?.data
    val questions = lesson?.questions.orEmpty()

    var currentIdx by remember { mutableIntStateOf(0) }
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var answerChecked by remember { mutableStateOf(false) }
    var correctCount by remember { mutableIntStateOf(0) }
    var quizCompleted by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("اختبار: ${lesson?.title ?: ""}", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("quiz_back_button")) {
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
                .padding(18.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            if (lesson == null || questions.isEmpty()) {
                Text("لا توجد أسئلة متاحة لهذا الفصل حاليًا.", style = MaterialTheme.typography.titleMedium)
            } else if (quizCompleted) {
                val total = questions.size
                val starsEarned = when {
                    correctCount == total -> 3
                    correctCount >= (total + 1) / 2 -> 2
                    else -> 1
                }
                val scoreEarned = correctCount * 10
                Card(
                    modifier = Modifier
                        .widthIn(max = 560.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "كأس الإنجاز",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(72.dp)
                        )
                        Text(
                            text = if (correctCount == total) "رائع جدًا يا بطل السيرة!" else "أحسنت المحاولة! واصل التعلم",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            repeat(starsEarned) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Text(
                            text = "أجبت على $correctCount من $total إجابات صحيحة\nحصلت على $starsEarned نجوم و $scoreEarned نقطة!",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = onBack,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("quiz_done_button")
                        ) {
                            Text("العودة إلى الفصول", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            } else {
                val q = questions[currentIdx]
                Column(
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "السؤال ${currentIdx + 1} من ${questions.size}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "الصحيحة: $correctCount",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    LinearProgressIndicator(
                        progress = { (currentIdx + 1).toFloat() / questions.size },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                    )

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = q.question,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(20.dp)
                        )
                    }

                    q.options.forEachIndexed { index, optionText ->
                        val isSelected = selectedOption == index
                        val isCorrect = index == q.correctIndex.toInt()
                        val containerColor = when {
                            answerChecked && isCorrect -> MaterialTheme.colorScheme.primaryContainer
                            answerChecked && isSelected && !isCorrect -> MaterialTheme.colorScheme.errorContainer
                            isSelected -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.surface
                        }
                        val borderColor = when {
                            answerChecked && isCorrect -> MaterialTheme.colorScheme.primary
                            isSelected -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.outline
                        }
                        Surface(
                            onClick = { if (!answerChecked) selectedOption = index },
                            color = containerColor,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.5.dp, borderColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("quiz_option_$index")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = optionText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                if (answerChecked && isCorrect) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "إجابة صحيحة",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    AnimatedVisibility(visible = answerChecked) {
                        val isRight = selectedOption == q.correctIndex.toInt()
                        Surface(
                            color = if (isRight) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isRight) {
                                    "أحسنت يا بطل! إجابة صحيحة 🌟"
                                } else {
                                    "الإجابة الصحيحة هي: ${q.options.getOrNull(q.correctIndex.toInt()).orEmpty()}"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = {
                            if (!answerChecked) {
                                if (selectedOption == q.correctIndex.toInt()) {
                                    correctCount++
                                }
                                answerChecked = true
                            } else {
                                if (currentIdx + 1 < questions.size) {
                                    currentIdx++
                                    selectedOption = null
                                    answerChecked = false
                                } else {
                                    quizCompleted = true
                                    onQuizFinished(lesson.chapterNumber.toInt(), correctCount, questions.size)
                                }
                            }
                        },
                        enabled = selectedOption != null,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("quiz_submit_button")
                    ) {
                        Text(
                            text = when {
                                !answerChecked -> "تحقق من الإجابة"
                                currentIdx + 1 < questions.size -> "السؤال التالي"
                                else -> "عرض النتيجة والحصول على النجوم"
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}
