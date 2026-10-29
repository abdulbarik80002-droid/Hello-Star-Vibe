package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HelloStarRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun createProfileAndPost_authenticatedOwner_succeedsAndDeduplicatesViews() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val repo = HelloStarRepository(firestore)

    val profileResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.ensureOrCreateProfile("alice_vibe", "Alice Star", "Neon Star Creator")
    }
    assertTrue(profileResult.isSuccess)

    val postIdResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createMediaPost(
        authorName = "Alice Star",
        postType = "reel",
        title = "Midnight Neon Dance",
        description = "Official Hello Star Vibe vertical reel",
        hashtags = "#HelloStarVibe #Neon",
        mediaCount = 1L,
        mediaUri = "content://hellostarvibe/reel/1",
        thumbnailTitle = "Midnight Neon Dance",
        durationSeconds = 30L,
      )
    }
    assertTrue(postIdResult.isSuccess)
    val postId = postIdResult.getOrThrow()

    val posts = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observePublicPosts().first { list -> list.any { it.id == postId } }
    }
    val createdPost = posts.first { it.id == postId }
    assertEquals(aliceUid, createdPost.authorId)

    // First view is recorded as valid
    val firstView = withTimeout(DEFAULT_TIMEOUT_MS) { repo.recordValidatedView(createdPost).getOrThrow() }
    assertTrue(firstView)

    // Second view by the same user is rejected as duplicate
    val duplicateView = withTimeout(DEFAULT_TIMEOUT_MS) { repo.recordValidatedView(createdPost).getOrThrow() }
    assertFalse(duplicateView)
  }

  @Test
  fun crossUserProfileAccess_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = HelloStarRepository(firestore)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.ensureOrCreateProfile("alice_vibe", "Alice Star").getOrThrow()
    }

    signInTestUser(BOB_EMAIL)
    var caught: Exception? = null
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore.collection("users").document(aliceUid).get().await()
      }
    } catch (e: Exception) {
      caught = e
    }
    assertNotNull(caught)
    val fsEx = caught?.unwrapFirestoreException()
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
  }

  @Test
  fun unauthenticatedCaller_observePublicPosts_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repo = HelloStarRepository(firestore)
    var caught: Exception? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observePublicPosts().first()
      }
    } catch (e: Exception) {
      caught = e
    }
    assertNotNull(caught)
    val fsEx = caught?.unwrapFirestoreException()
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
  }

  @Test
  fun diamondPurchaseOrder_createsPendingBackendVerificationEntry() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val repo = HelloStarRepository(firestore)

    val txId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createDiamondPurchaseOrder(
        packageId = "pkg_star_500",
        diamondsAmount = 500L,
        amountInrPaise = 49900L,
        paymentRef = "RZP_ORDER_991",
      ).getOrThrow()
    }
    assertTrue(txId.isNotEmpty())

    val txs = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeWalletTransactions(aliceUid).first { list -> list.any { it.id == txId } }
    }
    val createdTx = txs.first { it.id == txId }
    assertEquals("pending_backend_verification", createdTx.verificationStatus)
  }

  private companion object {
    const val ALICE_EMAIL = "alice@hellostarvibe.test"
    const val BOB_EMAIL = "bob@hellostarvibe.test"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
