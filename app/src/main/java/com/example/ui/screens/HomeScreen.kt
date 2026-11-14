package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ChapterMetadata
import com.example.data.model.UserProfile
import com.example.ui.components.ChapterIllustrationBanner
import com.example.ui.components.EditChildNameDialog
import com.example.ui.components.IslamicArchBackground
import com.example.ui.components.StatBadge
import com.example.ui.viewmodel.UiState

@Composable
fun HomeScreen(
    userProfileState: UiState<UserProfile>,
    chapters: List<ChapterMetadata>,
    isAdmin: Boolean,
    onChapterClick: (ChapterMetadata, Boolean) -> Unit,
    onOpenSummary: () -> Unit,
    onOpenPayment: () -> Unit,
    onOpenCertificate: () -> Unit,
    onOpenAdmin: () -> Unit,
    onUpdateChildName: (String) -> Unit,
    onSignOut: () -> Unit
) {
    var showEditName by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        when (userProfileState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator()
                        Text("جارٍ تجهيز رحلة السيرة...", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userProfileState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            is UiState.Success -> {
                val profile = userProfileState.data
                if (showEditName) {
                    EditChildNameDialog(
                        initialName = profile.childName,
                        onDismiss = { showEditName = false },
                        onSave = {
                            onUpdateChildName(it)
                            showEditName = false
                        }
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = innerPadding.calculateBottomPadding()),
                    contentAlignment = Alignment.TopCenter
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 760.dp),
                        contentPadding = PaddingValues(bottom = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            HomeHeader(
                                profile = profile,
                                isAdmin = isAdmin,
                                topPadding = innerPadding.calculateTopPadding(),
                                onEditName = { showEditName = true },
                                onOpenCertificate = onOpenCertificate,
                                onOpenAdmin = onOpenAdmin,
                                onSignOut = onSignOut
                            )
                        }

                        if (!profile.hasFullAccess) {
                            item {
                                ActivationPromoCard(
                                    isRevoked = profile.revoked,
                                    onOpenPayment = onOpenPayment,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }

                        item {
                            SeerahSummaryBannerCard(
                                onClick = onOpenSummary,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }

                        item {
                            Text(
                                text = "فصول الكتاب التفاعلي (12 فصلًا)",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }

                        items(chapters, key = { it.chapterNumber }) { chapter ->
                            val isUnlocked = chapter.isFree || profile.hasFullAccess || isAdmin
                            val isCompleted = profile.completedChapters.contains(chapter.chapterNumber)
                            ChapterCard(
                                chapter = chapter,
                                isUnlocked = isUnlocked,
                                isCompleted = isCompleted,
                                onClick = { onChapterClick(chapter, isUnlocked) },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(
    profile: UserProfile,
    isAdmin: Boolean,
    topPadding: androidx.compose.ui.unit.Dp,
    onEditName: () -> Unit,
    onOpenCertificate: () -> Unit,
    onOpenAdmin: () -> Unit,
    onSignOut: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary
                    )
                )
            )
    ) {
        IslamicArchBackground(
            accentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.matchParentSize()
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topPadding + 14.dp, start = 18.dp, end = 18.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onEditName)
                            .padding(vertical = 4.dp)
                            .testTag("edit_child_name_button")
                    ) {
                        Text(
                            text = "مرحبًا بالبطل: ${profile.childName}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.92f)
                        )
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "تعديل اسم الطفل",
                            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isAdmin) {
                        IconButton(
                            onClick = onOpenAdmin,
                            modifier = Modifier.testTag("open_admin_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "لوحة الإدارة",
                                tint = MaterialTheme.colorScheme.secondaryContainer
                            )
                        }
                    }
                    IconButton(
                        onClick = onSignOut,
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "تسجيل الخروج",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatBadge(
                    icon = Icons.Default.Star,
                    label = "${profile.stars} نجمة",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
                StatBadge(
                    icon = Icons.Default.EmojiEvents,
                    label = "${profile.totalScore} نقطة",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    onClick = onOpenCertificate,
                    color = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.testTag("open_certificate_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = "شهادة الإنجاز",
                            modifier = Modifier.size(18.dp)
                        )
                        Text("شهادتي", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            val completedCount = profile.completedChapters.size.coerceIn(0, 12)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "التقدّم في الرحلة",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.88f)
                    )
                    Text(
                        text = "$completedCount / 12 فصول مكتملة",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                LinearProgressIndicator(
                    progress = { completedCount / 12f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50)),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f)
                )
            }
        }
    }
}

@Composable
private fun SeerahSummaryBannerCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("open_summary_button"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "السيرة النبوية كاملة مختصرة",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "استعراض موجز وشامل للسيرة الشريفة من المولد إلى الوفاة ﷺ مع الاستماع الصوتي",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "اقرأ",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun ActivationPromoCard(
    isRevoked: Boolean,
    onOpenPayment: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRevoked) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (isRevoked) {
                    "تم إيقاف التفعيل لهذا الحساب من الإدارة"
                } else {
                    "افتح جميع الفصول الـ 12 مدى الحياة"
                },
                style = MaterialTheme.typography.titleMedium,
                color = if (isRevoked) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = "السعر: 200 أوقية موريتانية جديدة (MRU) — دفعة واحدة فقط، بدون اشتراك وبدون إعلانات. الفصل الأول مجاني للتجربة.",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isRevoked) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
            )
            Button(
                onClick = onOpenPayment,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("unlock_full_version_button")
            ) {
                Icon(
                    imageVector = Icons.Default.LockOpen,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("الدفع وتفعيل التطبيق (200 MRU)")
            }
        }
    }
}

@Composable
private fun ChapterCard(
    chapter: ChapterMetadata,
    isUnlocked: Boolean,
    isCompleted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("chapter_card_${chapter.chapterNumber}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isUnlocked) 2.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.size(72.dp)) {
                ChapterIllustrationBanner(
                    illustration = chapter.illustration,
                    contentDescription = chapter.title,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .size(26.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = chapter.chapterNumber.toString(),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = chapter.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = chapter.shortDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val badgeText = when {
                        isCompleted -> "مكتمل ⭐"
                        chapter.isFree -> "فصل مجاني"
                        isUnlocked -> "مفتوح للقراءة"
                        else -> "يتطلب التفعيل"
                    }
                    Surface(
                        color = when {
                            isCompleted -> MaterialTheme.colorScheme.primaryContainer
                            chapter.isFree || isUnlocked -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Icon(
                imageVector = when {
                    isCompleted -> Icons.Default.CheckCircle
                    isUnlocked -> Icons.AutoMirrored.Filled.MenuBook
                    else -> Icons.Default.Lock
                },
                contentDescription = if (isUnlocked) "متاح" else "مقفل",
                tint = when {
                    isCompleted -> MaterialTheme.colorScheme.primary
                    isUnlocked -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(26.dp)
            )
        }
    }
}
