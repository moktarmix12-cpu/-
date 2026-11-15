package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.ui.theme.AppBackgroundTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R

@Composable
fun ChapterIllustrationBanner(
    illustration: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val resId = when (illustration) {
        "kaaba" -> R.drawable.img_kaaba_makkah
        "madinah" -> R.drawable.img_madinah_mosque
        "hira" -> R.drawable.img_hira_desert
        "aqsa" -> R.drawable.img_aqsa_jerusalem
        else -> R.drawable.img_desert_journey
    }
    Box(modifier = modifier.clip(RoundedCornerShape(18.dp))) {
        Image(
            painter = painterResource(id = resId),
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.25f))
                    )
                )
        )
    }
}

@Composable
fun IslamicArchBackground(
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 2.dp.toPx())
        val cx = size.width * 0.15f
        val cy = size.height * 0.45f
        for (i in 1..3) {
            drawCircle(
                color = accentColor.copy(alpha = 0.08f),
                radius = (34f * i).dp.toPx(),
                center = Offset(cx, cy),
                style = stroke
            )
        }
        val archPath = Path().apply {
            val w = size.width
            val h = size.height
            moveTo(w * 0.72f, h)
            lineTo(w * 0.72f, h * 0.4f)
            quadraticTo(w * 0.85f, h * 0.05f, w * 0.98f, h * 0.4f)
            lineTo(w * 0.98f, h)
        }
        drawPath(archPath, color = accentColor.copy(alpha = 0.09f), style = stroke)
    }
}

@Composable
fun StatBadge(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(50)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
fun EditChildNameDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var nameInput by remember(initialName) { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("اسم البطل الصغير", style = MaterialTheme.typography.titleLarge) },
        text = {
            OutlinedTextField(
                value = nameInput,
                onValueChange = { if (it.length <= 40) nameInput = it },
                label = { Text("اكتب اسم الطفل ليظهر على الشهادة") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("child_name_input")
            )
        },
        confirmButton = {
            Button(
                onClick = { onSave(nameInput) },
                enabled = nameInput.trim().length >= 2,
                modifier = Modifier.testTag("save_child_name_button")
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun ThemePickerDialog(
    currentTheme: AppBackgroundTheme,
    onSelectTheme: (AppBackgroundTheme) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("اختر لون الخلفية", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppBackgroundTheme.entries.forEach { theme ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onSelectTheme(theme)
                                onDismiss()
                            }
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                            .testTag("theme_option_${theme.name}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(theme.previewSwatchHex))
                                .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        )
                        Text(
                            text = theme.displayNameAr,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        RadioButton(
                            selected = theme == currentTheme,
                            onClick = {
                                onSelectTheme(theme)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}

