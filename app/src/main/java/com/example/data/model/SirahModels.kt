package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val childName: String = "",
    val email: String = "",
    @get:PropertyName("isActivated") @field:PropertyName("isActivated") val isActivated: Boolean = false,
    val revoked: Boolean = false,
    val activationMethod: String = "none",
    val redeemedCodeHash: String = "",
    val stars: Long = 0L,
    val totalScore: Long = 0L,
    val completedChapters: List<Long> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @get:Exclude
    val hasFullAccess: Boolean
        get() = isActivated && !revoked
}

@IgnoreExtraProperties
data class PaymentRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val childName: String = "",
    val userEmail: String = "",
    val method: String = "Bankily",
    val senderPhone: String = "",
    val receiverPhone: String = "49719739",
    val amountMru: Long = 200L,
    val transactionNote: String = "",
    val receiptBase64: String = "",
    val status: String = "PENDING",
    val adminNote: String = "",
    val issuedCode: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ActivationCode(
    @DocumentId val id: String = "",
    val codeDisplay: String = "",
    @get:PropertyName("isActive") @field:PropertyName("isActive") val isActive: Boolean = true,
    val maxUses: Long = 1L,
    val usedCount: Long = 0L,
    val boundUserId: String = "",
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AppConfig(
    @DocumentId val id: String = "",
    val primaryCodeHash: String = "",
    val primaryCodeDisplay: String = "",
    val primaryEnabled: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class QuizQuestion(
    val question: String = "",
    val options: List<String> = emptyList(),
    val correctIndex: Long = 0L
)

@IgnoreExtraProperties
data class ChapterMetadata(
    @DocumentId val id: String = "",
    val chapterNumber: Long = 1L,
    val title: String = "",
    val shortDescription: String = "",
    val illustration: String = "kaaba",
    @get:PropertyName("isFree") @field:PropertyName("isFree") val isFree: Boolean = false,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class LessonContent(
    @DocumentId val id: String = "",
    val chapterNumber: Long = 1L,
    val title: String = "",
    val illustration: String = "kaaba",
    @get:PropertyName("isFree") @field:PropertyName("isFree") val isFree: Boolean = false,
    val content: String = "",
    val sourceNote: String = "",
    val questions: List<QuizQuestion> = emptyList(),
    @ServerTimestamp val updatedAt: Timestamp? = null
)
