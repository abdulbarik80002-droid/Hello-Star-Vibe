package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class HelloStarRepository(
  private val db: FirebaseFirestore,
  private val fallbackUserId: String? = null,
) {

  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private val auth
    get() = Firebase.auth

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: fallbackUserId?.takeIf { it.isNotBlank() }
      ?: throw IllegalStateException("User must be signed in before accessing Firestore.")
  }

  // --- USER PROFILE & SESSION ---

  fun observeUserProfile(userId: String): Flow<UserProfile?> {
    val path = "users/$userId"
    return db.collection("users").document(userId)
      .snapshots()
      .map { snapshot ->
        if (snapshot.exists()) {
          snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } else {
          null
        }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
        throw error
      }
  }

  suspend fun ensureOrCreateProfile(
    username: String,
    displayName: String,
    bio: String = "Creator on HELLO STAR VIBE",
  ): Result<UserProfile> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    try {
      val existing = docRef.get().await()
      if (existing.exists()) {
        return@runCatching existing.toObject(
          UserProfile::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        ) ?: UserProfile(id = uid, userId = uid, username = username, displayName = displayName)
      }
      val safeUsername = username.trim().take(30).let { if (it.length < 3) "star_${uid.take(6)}" else it }
      val safeDisplay = displayName.trim().take(60).ifEmpty { "Star Creator" }
      val payload = mapOf(
        "userId" to uid,
        "username" to safeUsername,
        "displayName" to safeDisplay,
        "bio" to bio.take(250),
        "avatarUrl" to "",
        "vipLevel" to 1L,
        "vipXp" to 100L,
        "diamondBalance" to 0L,
        "followersCount" to 0L,
        "followingCount" to 0L,
        "eligibleVideoViews" to 0L,
        "eligibleReelViews" to 0L,
        "eligiblePhotoViews" to 0L,
        "invalidViewsCount" to 0L,
        "pendingEarningsPaise" to 0L,
        "approvedEarningsPaise" to 0L,
        "paidEarningsPaise" to 0L,
        "accountStatus" to "active",
        "statusReason" to "",
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      docRef.set(payload).await()
      UserProfile(
        id = uid,
        userId = uid,
        username = safeUsername,
        displayName = safeDisplay,
        bio = bio.take(250),
        vipLevel = 1L,
        vipXp = 100L,
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      throw e
    }
  }

  suspend fun updateProfileDetails(
    username: String,
    displayName: String,
    bio: String,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    try {
      docRef.update(
        mapOf(
          "username" to username.trim().take(30).let { if (it.length < 3) "star_${uid.take(6)}" else it },
          "displayName" to displayName.trim().take(60).ifEmpty { "Star Creator" },
          "bio" to bio.trim().take(250),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  // --- VIDEOS, REELS & PHOTO POSTS ---

  fun observePublicPosts(): Flow<List<MediaPost>> {
    val path = "posts"
    return db.collection(path)
      .whereEqualTo("visibility", "public")
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(MediaPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  suspend fun createMediaPost(
    authorName: String,
    postType: String,
    title: String,
    description: String,
    hashtags: String,
    mediaCount: Long,
    mediaUri: String,
    thumbnailTitle: String,
    durationSeconds: Long,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("posts").document()
    try {
      val payload = mapOf(
        "authorId" to uid,
        "authorName" to authorName.trim().take(60).ifEmpty { "Star Creator" },
        "postType" to postType,
        "title" to title.trim().take(120).ifEmpty { "Untitled Vibe" },
        "description" to description.trim().take(1000),
        "hashtags" to hashtags.trim().take(200),
        "mediaCount" to mediaCount.coerceIn(1L, 10L),
        "mediaUri" to mediaUri.take(500),
        "thumbnailTitle" to thumbnailTitle.take(120),
        "durationSeconds" to durationSeconds.coerceIn(0L, 7200L),
        "viewsCount" to 0L,
        "likesCount" to 0L,
        "commentsCount" to 0L,
        "sharesCount" to 0L,
        "visibility" to "public",
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      docRef.set(payload).await()
      // Grant VIP XP for publishing content
      val userRef = db.collection("users").document(uid)
      val userSnap = userRef.get().await()
      if (userSnap.exists()) {
        val currentXp = userSnap.getLong("vipXp") ?: 0L
        val currentLevel = userSnap.getLong("vipLevel") ?: 1L
        val newXp = currentXp + 150L
        val nextLevelThreshold = CreatorRewardCalculator.xpForNextVipLevel(currentLevel)
        val newLevel = if (newXp >= nextLevelThreshold && currentLevel < 50L) currentLevel + 1L else currentLevel
        userRef.update(
          mapOf(
            "vipXp" to newXp,
            "vipLevel" to newLevel,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      }
      docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  /**
   * Anti-fraud unique view recording:
   * Checks if `/posts/{postId}/views/{viewerId}` already exists.
   * If not, creates the immutable view record and increments the post's `viewsCount`
   * plus the creator's eligible view counter (if the viewer is the creator or updating own stats).
   * If already viewed, increments the user's `invalidViewsCount` to transparently log duplicate view prevention.
   */
  suspend fun recordValidatedView(post: MediaPost): Result<Boolean> = runCatching {
    val uid = requireUserId()
    if (post.id.isBlank()) return@runCatching false
    val viewRef = db.collection("posts").document(post.id).collection("views").document(uid)
    val postRef = db.collection("posts").document(post.id)
    val userRef = db.collection("users").document(uid)
    try {
      val existingView = viewRef.get().await()
      if (existingView.exists()) {
        // Duplicate view attempt blocked; record in user's invalidViewsCount
        val userSnap = userRef.get().await()
        if (userSnap.exists()) {
          userRef.update(
            mapOf(
              "invalidViewsCount" to FieldValue.increment(1L),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          ).await()
        }
        return@runCatching false
      }
      // Record unique view
      viewRef.set(
        mapOf(
          "postId" to post.id,
          "viewerId" to uid,
          "postType" to post.postType,
          "createdAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      postRef.update(
        mapOf(
          "viewsCount" to FieldValue.increment(1L),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      // Update creator eligible view stats if viewing own post or tracking creator metrics
      if (post.authorId == uid) {
        val userSnap = userRef.get().await()
        if (userSnap.exists()) {
          val videoViews = (userSnap.getLong("eligibleVideoViews") ?: 0L) + if (post.postType == "video") 1L else 0L
          val reelViews = (userSnap.getLong("eligibleReelViews") ?: 0L) + if (post.postType == "reel") 1L else 0L
          val photoViews = (userSnap.getLong("eligiblePhotoViews") ?: 0L) + if (post.postType == "photo") 1L else 0L
          val totalEligible = videoViews + reelViews + photoViews
          val recalculatedPendingPaise = CreatorRewardCalculator.calculateProposedPaiseForViews(totalEligible)
          userRef.update(
            mapOf(
              "eligibleVideoViews" to videoViews,
              "eligibleReelViews" to reelViews,
              "eligiblePhotoViews" to photoViews,
              "pendingEarningsPaise" to recalculatedPendingPaise,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          ).await()
        }
      }
      true
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, viewRef.path)
      throw e
    }
  }

  suspend fun likePost(postId: String): Result<Unit> = runCatching {
    requireUserId()
    val postRef = db.collection("posts").document(postId)
    try {
      postRef.update(
        mapOf(
          "likesCount" to FieldValue.increment(1L),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, postRef.path)
      throw e
    }
  }

  suspend fun sharePost(postId: String): Result<Unit> = runCatching {
    requireUserId()
    val postRef = db.collection("posts").document(postId)
    try {
      postRef.update(
        mapOf(
          "sharesCount" to FieldValue.increment(1L),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, postRef.path)
      throw e
    }
  }

  fun observePostComments(postId: String): Flow<List<PostComment>> {
    val path = "posts/$postId/comments"
    return db.collection("posts").document(postId).collection("comments")
      .whereEqualTo("postId", postId)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(PostComment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedBy { it.createdAt?.seconds ?: 0L }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  suspend fun addPostComment(postId: String, authorName: String, text: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    val commentRef = db.collection("posts").document(postId).collection("comments").document()
    val postRef = db.collection("posts").document(postId)
    try {
      commentRef.set(
        mapOf(
          "postId" to postId,
          "authorId" to uid,
          "authorName" to authorName.trim().take(60).ifEmpty { "Star User" },
          "text" to text.trim().take(500),
          "createdAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      postRef.update(
        mapOf(
          "commentsCount" to FieldValue.increment(1L),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, commentRef.path)
      throw e
    }
  }

  // --- FOLLOWS ---

  fun observeMyFollows(userId: String): Flow<List<UserFollow>> {
    val path = "follows"
    return db.collection(path)
      .whereEqualTo("followerId", userId)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(UserFollow::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  suspend fun toggleFollowCreator(targetUserId: String, targetName: String): Result<Boolean> = runCatching {
    val uid = requireUserId()
    require(uid != targetUserId) { "Cannot follow your own profile." }
    val followDocId = "${uid}_$targetUserId"
    val followRef = db.collection("follows").document(followDocId)
    val myUserRef = db.collection("users").document(uid)
    try {
      val existing = followRef.get().await()
      if (existing.exists()) {
        followRef.delete().await()
        myUserRef.update(
          mapOf(
            "followingCount" to FieldValue.increment(-1L),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
        false
      } else {
        followRef.set(
          mapOf(
            "followerId" to uid,
            "targetUserId" to targetUserId,
            "targetName" to targetName.take(60).ifEmpty { "Creator" },
            "createdAt" to FieldValue.serverTimestamp(),
          )
        ).await()
        myUserRef.update(
          mapOf(
            "followingCount" to FieldValue.increment(1L),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
        true
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, followRef.path)
      throw e
    }
  }

  // --- LIVE STREAMING, CHAT & TWO-PERSON PK BATTLES ---

  fun observeActiveLiveStreams(): Flow<List<LiveStream>> {
    val path = "live_streams"
    return db.collection(path)
      .whereIn("status", listOf("live", "pk_battle"))
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(LiveStream::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  suspend fun startLiveStream(
    hostName: String,
    title: String,
    streamCategory: String,
    isPkBattle: Boolean,
    opponentId: String = "",
    opponentName: String = "",
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("live_streams").document()
    try {
      docRef.set(
        mapOf(
          "hostId" to uid,
          "hostName" to hostName.trim().take(60).ifEmpty { "Star Host" },
          "title" to title.trim().take(120).ifEmpty { "Live Star Broadcast" },
          "streamCategory" to streamCategory.trim().take(50).ifEmpty { "Talent & Music" },
          "status" to if (isPkBattle) "pk_battle" else "live",
          "viewerCount" to 1L,
          "isPkBattle" to isPkBattle,
          "opponentId" to opponentId.take(128),
          "opponentName" to opponentName.take(60),
          "hostPkScore" to 0L,
          "opponentPkScore" to 0L,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun endLiveStream(streamId: String): Result<Unit> = runCatching {
    requireUserId()
    val docRef = db.collection("live_streams").document(streamId)
    try {
      docRef.update(
        mapOf(
          "status" to "ended",
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  fun observeStreamMessages(streamId: String): Flow<List<LiveChatMessage>> {
    val path = "live_streams/$streamId/messages"
    return db.collection("live_streams").document(streamId).collection("messages")
      .whereEqualTo("streamId", streamId)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(LiveChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedBy { it.createdAt?.seconds ?: 0L }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  suspend fun sendStreamChatMessage(
    streamId: String,
    senderName: String,
    messageText: String,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val msgRef = db.collection("live_streams").document(streamId).collection("messages").document()
    try {
      msgRef.set(
        mapOf(
          "streamId" to streamId,
          "senderId" to uid,
          "senderName" to senderName.trim().take(60).ifEmpty { "Viewer" },
          "messageText" to messageText.trim().take(300),
          "isGiftAlert" to false,
          "giftAmountDiamonds" to 0L,
          "createdAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, msgRef.path)
      throw e
    }
  }

  /**
   * Sends a verified diamond gift in a live stream or PK battle.
   * Validates the sender's current diamond balance, records an auditable WalletTransaction,
   * deducts diamonds from the sender, increments the PK Battle score on the stream,
   * and posts a verified gift alert in the live chat.
   */
  suspend fun sendVerifiedLiveGift(
    stream: LiveStream,
    senderName: String,
    giftName: String,
    diamondCost: Long,
    supportHostSide: Boolean,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    require(diamondCost > 0L) { "Diamond gift amount must be positive." }
    val userRef = db.collection("users").document(uid)
    val txRef = db.collection("wallet_transactions").document()
    val streamRef = db.collection("live_streams").document(stream.id)
    val msgRef = streamRef.collection("messages").document()
    try {
      val userSnap = userRef.get().await()
      val currentBalance = userSnap.getLong("diamondBalance") ?: 0L
      require(currentBalance >= diamondCost) {
        "Insufficient diamond balance ($currentBalance available, $diamondCost required)."
      }
      val newBalance = currentBalance - diamondCost
      val currentXp = userSnap.getLong("vipXp") ?: 0L
      val currentLevel = userSnap.getLong("vipLevel") ?: 1L
      val newXp = currentXp + (diamondCost * 5L)
      val newLevel = if (newXp >= CreatorRewardCalculator.xpForNextVipLevel(currentLevel) && currentLevel < 50L) {
        currentLevel + 1L
      } else {
        currentLevel
      }

      // 1. Deduct diamonds & award VIP XP
      userRef.update(
        mapOf(
          "diamondBalance" to newBalance,
          "vipXp" to newXp,
          "vipLevel" to newLevel,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()

      // 2. Write auditable gift transaction ledger entry
      txRef.set(
        mapOf(
          "userId" to uid,
          "txType" to if (stream.isPkBattle) "pk_battle_gift" else "gift_sent",
          "packageId" to giftName.take(60),
          "diamondsDelta" to -diamondCost,
          "amountInrPaise" to 0L,
          "recipientId" to if (supportHostSide) stream.hostId else stream.opponentId.ifEmpty { stream.hostId },
          "streamId" to stream.id,
          "verificationStatus" to "verified",
          "paymentRef" to "GIFT_${System.currentTimeMillis()}",
          "createdAt" to FieldValue.serverTimestamp(),
        )
      ).await()

      // 3. Increment PK battle score based on verified gift transaction
      val scoreField = if (supportHostSide) "hostPkScore" else "opponentPkScore"
      streamRef.update(
        mapOf(
          scoreField to FieldValue.increment(diamondCost * 10L),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()

      // 4. Broadcast gift alert in live chat
      val targetLabel = if (supportHostSide) stream.hostName else stream.opponentName.ifEmpty { "Challenger" }
      msgRef.set(
        mapOf(
          "streamId" to stream.id,
          "senderId" to uid,
          "senderName" to senderName.take(60).ifEmpty { "Star Supporter" },
          "messageText" to "Sent $giftName ($diamondCost Diamonds) to $targetLabel!",
          "isGiftAlert" to true,
          "giftAmountDiamonds" to diamondCost,
          "createdAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, txRef.path)
      throw e
    }
  }

  // --- DIAMOND WALLET & AUDITED PAYMENT TRANSACTIONS ---

  fun observeWalletTransactions(userId: String): Flow<List<WalletTransaction>> {
    val path = "wallet_transactions"
    return db.collection(path)
      .whereEqualTo("userId", userId)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(WalletTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  /**
   * Creates a diamond purchase order in the ledger with `pending_backend_verification`.
   * Client code never trusts client-side payment success alone; only an admin / backend
   * webhook verification can mark the transaction `verified` and credit the diamonds.
   */
  suspend fun createDiamondPurchaseOrder(
    packageId: String,
    diamondsAmount: Long,
    amountInrPaise: Long,
    paymentRef: String,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    require(diamondsAmount > 0L) { "Diamonds amount must be positive." }
    val txRef = db.collection("wallet_transactions").document()
    try {
      txRef.set(
        mapOf(
          "userId" to uid,
          "txType" to "diamond_purchase",
          "packageId" to packageId.take(60),
          "diamondsDelta" to diamondsAmount,
          "amountInrPaise" to amountInrPaise,
          "recipientId" to "",
          "streamId" to "",
          "verificationStatus" to "pending_backend_verification",
          "paymentRef" to paymentRef.trim().take(120).ifEmpty { "ORDER_${System.currentTimeMillis()}" },
          "createdAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      txRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, txRef.path)
      throw e
    }
  }

  // --- BANK ACCOUNTS & CREATOR WITHDRAWALS ---

  fun observeBankAccounts(userId: String): Flow<List<BankAccount>> {
    val path = "bank_accounts"
    return db.collection(path)
      .whereEqualTo("userId", userId)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(BankAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  suspend fun addBankAccount(
    accountHolderName: String,
    bankName: String,
    accountNumber: String,
    ifscCode: String,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("bank_accounts").document()
    val masked = if (accountNumber.length > 4) {
      "XXXX" + accountNumber.takeLast(4)
    } else {
      accountNumber
    }
    try {
      docRef.set(
        mapOf(
          "userId" to uid,
          "accountHolderName" to accountHolderName.trim().take(80),
          "bankName" to bankName.trim().take(80),
          "accountNumberMasked" to masked.take(30),
          "ifscCode" to ifscCode.trim().uppercase().take(20),
          "verificationStatus" to "pending",
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  fun observeWithdrawals(userId: String): Flow<List<WithdrawalRequest>> {
    val path = "withdrawals"
    return db.collection(path)
      .whereEqualTo("userId", userId)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(WithdrawalRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  suspend fun submitWithdrawalRequest(
    bankAccountId: String,
    amountInrPaise: Long,
    userVipLevel: Long,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    require(userVipLevel >= 30L) { "Account must reach VIP Level 30 before requesting a creator payout." }
    require(amountInrPaise >= 1000000L) { "Minimum payout threshold is ₹10,000.00." }
    val docRef = db.collection("withdrawals").document()
    try {
      docRef.set(
        mapOf(
          "userId" to uid,
          "bankAccountId" to bankAccountId,
          "amountInrPaise" to amountInrPaise,
          "userVipLevelAtRequest" to userVipLevel,
          "status" to "pending_review",
          "scheduledPayoutDay" to 18L,
          "adminNote" to "",
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  // --- COMMUNITY GUIDELINES REPORTING & APPEALS ---

  fun observeMyReports(userId: String): Flow<List<ModerationReport>> {
    val path = "reports"
    return db.collection(path)
      .whereEqualTo("reporterId", userId)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(ModerationReport::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  suspend fun submitModerationReport(
    targetType: String,
    targetId: String,
    reasonCategory: String,
    details: String,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("reports").document()
    try {
      docRef.set(
        mapOf(
          "reporterId" to uid,
          "targetType" to targetType,
          "targetId" to targetId.take(128).ifEmpty { "general_target" },
          "reasonCategory" to reasonCategory,
          "details" to details.trim().take(500).ifEmpty { "Reported for community guideline review" },
          "status" to "open",
          "moderationAction" to "",
          "appealText" to "",
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun submitReportAppeal(reportId: String, appealText: String): Result<Unit> = runCatching {
    requireUserId()
    val docRef = db.collection("reports").document(reportId)
    try {
      docRef.update(
        mapOf(
          "status" to "appealed",
          "appealText" to appealText.trim().take(500),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  // --- CUSTOMER SUPPORT TICKETS ---

  fun observeMySupportTickets(userId: String): Flow<List<SupportTicket>> {
    val path = "support_tickets"
    return db.collection(path)
      .whereEqualTo("userId", userId)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(SupportTicket::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  suspend fun createSupportTicket(
    category: String,
    subject: String,
    message: String,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("support_tickets").document()
    try {
      docRef.set(
        mapOf(
          "userId" to uid,
          "category" to category,
          "subject" to subject.trim().take(120),
          "message" to message.trim().take(1000),
          "status" to "open",
          "adminReply" to "",
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  // --- NOTIFICATIONS ---

  fun observeNotifications(userId: String): Flow<List<UserNotification>> {
    val path = "notifications"
    return db.collection(path)
      .whereEqualTo("userId", userId)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(UserNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedByDescending { it.createdAt?.seconds ?: 0L }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  suspend fun createNotification(
    targetUserId: String,
    title: String,
    body: String,
    notifType: String,
  ): Result<Unit> = runCatching {
    requireUserId()
    val docRef = db.collection("notifications").document()
    try {
      docRef.set(
        mapOf(
          "userId" to targetUserId,
          "title" to title.trim().take(120),
          "body" to body.trim().take(400),
          "notifType" to notifType,
          "isRead" to false,
          "createdAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun markNotificationRead(notificationId: String): Result<Unit> = runCatching {
    requireUserId()
    val docRef = db.collection("notifications").document(notificationId)
    try {
      docRef.update("isRead", true).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  // --- ADMIN DASHBOARD OPERATIONS (Protected by server-side isAdmin() rules) ---

  fun observeAllUsersForAdmin(): Flow<List<UserProfile>> =
    db.collection("users").snapshots().map {
      it.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
    }

  fun observeAllReportsForAdmin(): Flow<List<ModerationReport>> =
    db.collection("reports").snapshots().map {
      it.toObjects(ModerationReport::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        .sortedByDescending { r -> r.createdAt?.seconds ?: 0L }
    }

  fun observeAllTransactionsForAdmin(): Flow<List<WalletTransaction>> =
    db.collection("wallet_transactions").snapshots().map {
      it.toObjects(WalletTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        .sortedByDescending { tx -> tx.createdAt?.seconds ?: 0L }
    }

  fun observeAllBankAccountsForAdmin(): Flow<List<BankAccount>> =
    db.collection("bank_accounts").snapshots().map {
      it.toObjects(BankAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
    }

  fun observeAllWithdrawalsForAdmin(): Flow<List<WithdrawalRequest>> =
    db.collection("withdrawals").snapshots().map {
      it.toObjects(WithdrawalRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        .sortedByDescending { w -> w.createdAt?.seconds ?: 0L }
    }

  fun observeAllSupportTicketsForAdmin(): Flow<List<SupportTicket>> =
    db.collection("support_tickets").snapshots().map {
      it.toObjects(SupportTicket::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        .sortedByDescending { t -> t.createdAt?.seconds ?: 0L }
    }

  suspend fun adminVerifyDiamondPurchase(tx: WalletTransaction): Result<Unit> = runCatching {
    requireUserId()
    val txRef = db.collection("wallet_transactions").document(tx.id)
    val userRef = db.collection("users").document(tx.userId)
    try {
      txRef.update("verificationStatus", "verified").await()
      userRef.update(
        mapOf(
          "diamondBalance" to FieldValue.increment(tx.diamondsDelta),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, txRef.path)
      throw e
    }
  }

  suspend fun adminModerateUserAccount(
    targetUserId: String,
    newStatus: String,
    reason: String,
  ): Result<Unit> = runCatching {
    requireUserId()
    val userRef = db.collection("users").document(targetUserId)
    try {
      userRef.update(
        mapOf(
          "accountStatus" to newStatus,
          "statusReason" to reason.trim().take(300),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, userRef.path)
      throw e
    }
  }

  suspend fun adminUpdateUserVipAndEarnings(
    targetUserId: String,
    vipLevel: Long,
    approvedEarningsPaise: Long,
  ): Result<Unit> = runCatching {
    requireUserId()
    val userRef = db.collection("users").document(targetUserId)
    try {
      userRef.update(
        mapOf(
          "vipLevel" to vipLevel.coerceIn(1L, 50L),
          "approvedEarningsPaise" to approvedEarningsPaise.coerceAtLeast(0L),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, userRef.path)
      throw e
    }
  }

  suspend fun adminVerifyBankAccount(accountId: String, status: String): Result<Unit> = runCatching {
    requireUserId()
    val ref = db.collection("bank_accounts").document(accountId)
    try {
      ref.update(
        mapOf(
          "verificationStatus" to status,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, ref.path)
      throw e
    }
  }

  suspend fun adminProcessWithdrawal(
    withdrawal: WithdrawalRequest,
    newStatus: String,
    adminNote: String,
  ): Result<Unit> = runCatching {
    requireUserId()
    val ref = db.collection("withdrawals").document(withdrawal.id)
    val userRef = db.collection("users").document(withdrawal.userId)
    try {
      ref.update(
        mapOf(
          "status" to newStatus,
          "adminNote" to adminNote.trim().take(300),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      if (newStatus == "paid") {
        userRef.update(
          mapOf(
            "paidEarningsPaise" to FieldValue.increment(withdrawal.amountInrPaise),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, ref.path)
      throw e
    }
  }

  suspend fun adminResolveReport(
    reportId: String,
    newStatus: String,
    actionNote: String,
  ): Result<Unit> = runCatching {
    requireUserId()
    val ref = db.collection("reports").document(reportId)
    try {
      ref.update(
        mapOf(
          "status" to newStatus,
          "moderationAction" to actionNote.trim().take(100),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, ref.path)
      throw e
    }
  }

  suspend fun adminRemovePost(postId: String): Result<Unit> = runCatching {
    requireUserId()
    val ref = db.collection("posts").document(postId)
    try {
      ref.update(
        mapOf(
          "visibility" to "removed",
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, ref.path)
      throw e
    }
  }

  suspend fun adminReplySupportTicket(
    ticketId: String,
    newStatus: String,
    replyText: String,
  ): Result<Unit> = runCatching {
    requireUserId()
    val ref = db.collection("support_tickets").document(ticketId)
    try {
      ref.update(
        mapOf(
          "status" to newStatus,
          "adminReply" to replyText.trim().take(1000),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, ref.path)
      throw e
    }
  }
}
