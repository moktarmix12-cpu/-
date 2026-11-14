package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class SirahRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ensureUserProfile_createsUnactivatedProfile_andBlocksInvalidCode() = runBlocking {
        val uid = signInTestUser("child_${UUID.randomUUID()}@test.com")
        val repo = SirahRepository(firestore)

        val createRes = withTimeout(TIMEOUT_MS) {
            repo.ensureUserProfile(uid, "أحمد", "child@test.com")
        }
        assertTrue(createRes.isSuccess)

        val profile = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeUserProfile(uid).first { it != null }
        }
        assertNotNull(profile)
        assertEquals(false, profile!!.isActivated)

        val badActivation = withTimeout(TIMEOUT_MS) {
            repo.redeemActivationCode(uid, "wrong_code_999")
        }
        assertTrue(badActivation.isFailure)
    }

    @Test
    fun redeemPrimaryCode_unlocksUserProfile_andRecordProgressSaves() = runBlocking {
        val uid = signInTestUser("learner_${UUID.randomUUID()}@test.com")
        val repo = SirahRepository(firestore)

        withTimeout(TIMEOUT_MS) {
            repo.ensureUserProfile(uid, "فاطمة", "learner@test.com").getOrThrow()
        }

        val activateRes = withTimeout(TIMEOUT_MS) {
            repo.redeemActivationCode(uid, "xs509")
        }
        assertTrue(activateRes.isSuccess)

        val activatedProfile = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeUserProfile(uid).first { it?.isActivated == true }
        }
        assertTrue(activatedProfile!!.hasFullAccess)

        val progressRes = withTimeout(TIMEOUT_MS) {
            repo.recordChapterCompletion(
                userId = uid,
                chapterNumber = 1,
                earnedStars = 3,
                earnedScore = 30,
                existingCompleted = activatedProfile.completedChapters,
                currentStars = activatedProfile.stars,
                currentScore = activatedProfile.totalScore
            )
        }
        assertTrue(progressRes.isSuccess)

        val updated = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeUserProfile(uid).first { it?.stars == 3L }
        }
        assertEquals(30L, updated!!.totalScore)
        assertTrue(updated.completedChapters.contains(1L))
    }

    @Test
    fun submitPaymentRequest_createsPendingRequest_andUnauthenticatedAccessDenied() = runBlocking {
        val uid = signInTestUser("buyer_${UUID.randomUUID()}@test.com")
        val repo = SirahRepository(firestore)

        val reqId = withTimeout(TIMEOUT_MS) {
            repo.submitPaymentRequest(
                userId = uid,
                childName = "عمر",
                userEmail = "buyer@test.com",
                method = "Bankily",
                senderPhone = "22113344",
                transactionNote = "Ref 88",
                receiptBase64 = "iVBORw0KGgoAAAANSUhEUg=="
            ).getOrThrow()
        }
        assertTrue(reqId.isNotBlank())

        val payments = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeUserPayments(uid).first { list -> list.any { it.id == reqId } }
        }
        assertEquals("PENDING", payments.first { it.id == reqId }.status)

        // Sign out -> reading protected profile must fail with PERMISSION_DENIED
        auth.signOut()
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeUserProfile(uid).first()
            }
            fail("Expected PERMISSION_DENIED when unauthenticated")
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException, got $e", fsEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx!!.code)
        }
    }

    private companion object {
        const val TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
