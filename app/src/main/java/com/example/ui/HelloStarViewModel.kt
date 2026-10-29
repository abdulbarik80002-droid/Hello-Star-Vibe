package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BankAccount
import com.example.data.HelloStarRepository
import com.example.data.LiveChatMessage
import com.example.data.LiveStream
import com.example.data.MediaPost
import com.example.data.ModerationReport
import com.example.data.PostComment
import com.example.data.SupportTicket
import com.example.data.UserFollow
import com.example.data.UserNotification
import com.example.data.UserProfile
import com.example.data.WalletTransaction
import com.example.data.WithdrawalRequest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppDestination(val title: String) {
  HOME("Home"),
  REELS("Reels"),
  VIDEOS("Videos"),
  PHOTOS("Photos"),
  LIVE("Live & PK"),
  NOTIFICATIONS("Notifications"),
  SEARCH("Search"),
  PROFILE("Profile"),
  WALLET("Diamond Wallet"),
  EARNINGS("Creator Earnings"),
  WITHDRAWAL("Withdrawals"),
  VIP("VIP Levels"),
  SUPPORT("Help & Support"),
  ADMIN("Admin Dashboard"),
}

@OptIn(ExperimentalCoroutinesApi::class)
class HelloStarViewModel(
  private val repository: HelloStarRepository,
  val currentUserId: String,
  val currentUserEmail: String,
  val currentDisplayName: String,
) : ViewModel() {

  val isAuthorizedAdmin: Boolean =
    currentUserEmail.equals("abdulbarik80000@gmail.com", ignoreCase = true)

  private val _currentDestination = MutableStateFlow(AppDestination.HOME)
  val currentDestination: StateFlow<AppDestination> = _currentDestination.asStateFlow()

  private val _statusBanner = MutableStateFlow<String?>(null)
  val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

  private val _isSubmitting = MutableStateFlow(false)
  val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

  private val _selectedPostForComments = MutableStateFlow<MediaPost?>(null)
  val selectedPostForComments: StateFlow<MediaPost?> = _selectedPostForComments.asStateFlow()

  private val _selectedLiveStream = MutableStateFlow<LiveStream?>(null)
  val selectedLiveStream: StateFlow<LiveStream?> = _selectedLiveStream.asStateFlow()

  private val initialSessionProfile = UserProfile(
    id = currentUserId,
    userId = currentUserId,
    username = currentUserEmail.substringBefore("@")
      .replace(Regex("[^a-zA-Z0-9_]"), "")
      .ifBlank { "star_${currentUserId.take(6)}" },
    displayName = currentDisplayName.ifBlank {
      currentUserEmail.substringBefore("@").ifBlank { "Star Creator" }
    },
    bio = "Creator on HELLO STAR VIBE",
    vipLevel = 1L,
    vipXp = 100L,
    accountStatus = "active",
  )

  private val _localProfileFallback = MutableStateFlow(initialSessionProfile)

  val userProfile: StateFlow<UserProfile?> = combine(
    repository.observeUserProfile(currentUserId).catch { e ->
      Log.w(TAG, "Profile stream error", e)
      emit(null)
    },
    _localProfileFallback,
  ) { remote, fallback ->
    remote ?: fallback
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), initialSessionProfile)

  val publicPosts: StateFlow<List<MediaPost>> = repository.observePublicPosts()
    .catch { e ->
      Log.w(TAG, "Public posts stream error", e)
      emit(emptyList())
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val myFollows: StateFlow<List<UserFollow>> = repository.observeMyFollows(currentUserId)
    .catch { e ->
      Log.w(TAG, "Follows stream error", e)
      emit(emptyList())
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val activeLiveStreams: StateFlow<List<LiveStream>> = repository.observeActiveLiveStreams()
    .catch { e ->
      Log.w(TAG, "Live streams error", e)
      emit(emptyList())
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val walletTransactions: StateFlow<List<WalletTransaction>> = repository.observeWalletTransactions(currentUserId)
    .catch { e ->
      Log.w(TAG, "Wallet transactions error", e)
      emit(emptyList())
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val bankAccounts: StateFlow<List<BankAccount>> = repository.observeBankAccounts(currentUserId)
    .catch { e ->
      Log.w(TAG, "Bank accounts error", e)
      emit(emptyList())
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val withdrawals: StateFlow<List<WithdrawalRequest>> = repository.observeWithdrawals(currentUserId)
    .catch { e ->
      Log.w(TAG, "Withdrawals error", e)
      emit(emptyList())
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val myReports: StateFlow<List<ModerationReport>> = repository.observeMyReports(currentUserId)
    .catch { e ->
      Log.w(TAG, "Reports error", e)
      emit(emptyList())
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val mySupportTickets: StateFlow<List<SupportTicket>> = repository.observeMySupportTickets(currentUserId)
    .catch { e ->
      Log.w(TAG, "Support tickets error", e)
      emit(emptyList())
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val notifications: StateFlow<List<UserNotification>> = repository.observeNotifications(currentUserId)
    .catch { e ->
      Log.w(TAG, "Notifications error", e)
      emit(emptyList())
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val activePostComments: StateFlow<List<PostComment>> = _selectedPostForComments
    .flatMapLatest { post ->
      if (post == null || post.id.isBlank()) {
        flowOf(emptyList())
      } else {
        repository.observePostComments(post.id).catch { emit(emptyList()) }
      }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val activeStreamMessages: StateFlow<List<LiveChatMessage>> = _selectedLiveStream
    .flatMapLatest { stream ->
      if (stream == null || stream.id.isBlank()) {
        flowOf(emptyList())
      } else {
        repository.observeStreamMessages(stream.id).catch { emit(emptyList()) }
      }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  // Admin-only streams (only subscribed when user is authorized admin)
  val adminAllUsers: StateFlow<List<UserProfile>> =
    (if (isAuthorizedAdmin) repository.observeAllUsersForAdmin() else flowOf(emptyList()))
      .catch { emit(emptyList()) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val adminAllReports: StateFlow<List<ModerationReport>> =
    (if (isAuthorizedAdmin) repository.observeAllReportsForAdmin() else flowOf(emptyList()))
      .catch { emit(emptyList()) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val adminAllTransactions: StateFlow<List<WalletTransaction>> =
    (if (isAuthorizedAdmin) repository.observeAllTransactionsForAdmin() else flowOf(emptyList()))
      .catch { emit(emptyList()) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val adminAllBankAccounts: StateFlow<List<BankAccount>> =
    (if (isAuthorizedAdmin) repository.observeAllBankAccountsForAdmin() else flowOf(emptyList()))
      .catch { emit(emptyList()) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val adminAllWithdrawals: StateFlow<List<WithdrawalRequest>> =
    (if (isAuthorizedAdmin) repository.observeAllWithdrawalsForAdmin() else flowOf(emptyList()))
      .catch { emit(emptyList()) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val adminAllTickets: StateFlow<List<SupportTicket>> =
    (if (isAuthorizedAdmin) repository.observeAllSupportTicketsForAdmin() else flowOf(emptyList()))
      .catch { emit(emptyList()) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  init {
    viewModelScope.launch {
      val defaultHandle = currentUserEmail.substringBefore("@")
        .replace(Regex("[^a-zA-Z0-9_]"), "")
        .ifBlank { "star_${currentUserId.take(6)}" }
      val display = currentDisplayName.ifBlank { "Star Creator" }
      repository.ensureOrCreateProfile(
        username = defaultHandle,
        displayName = display,
      )
    }
  }

  private var hasAnnouncedSession = false

  fun announceAuthenticatedSession(email: String, isNewSignup: Boolean) {
    if (hasAnnouncedSession) return
    hasAnnouncedSession = true
    val cleanEmail = email.ifBlank { currentUserEmail }
    if (cleanEmail.isNotBlank()) {
      _statusBanner.value = if (isNewSignup) {
        "Account created! Signed in as $cleanEmail"
      } else {
        "Welcome back! Signed in as $cleanEmail"
      }
    }
  }

  fun navigateTo(destination: AppDestination) {
    if (destination == AppDestination.ADMIN && !isAuthorizedAdmin) {
      _statusBanner.value = "Access Denied: Admin Dashboard requires verified server-side administrator privileges."
      return
    }
    if (_currentDestination.value == destination) return
    _currentDestination.value = destination
  }

  fun clearBanner() {
    _statusBanner.value = null
  }

  fun openCommentsForPost(post: MediaPost?) {
    _selectedPostForComments.value = post
  }

  fun selectLiveStream(stream: LiveStream?) {
    _selectedLiveStream.value = stream
  }

  private fun executeGuarded(actionName: String, block: suspend () -> Result<*>) {
    if (_isSubmitting.value) return
    viewModelScope.launch {
      _isSubmitting.value = true
      try {
        val result = block()
        result.onSuccess {
          _statusBanner.value = "$actionName completed."
        }.onFailure { err ->
          _statusBanner.value = err.localizedMessage ?: "$actionName failed."
        }
      } finally {
        _isSubmitting.value = false
      }
    }
  }

  fun updateProfile(username: String, displayName: String, bio: String) {
    _localProfileFallback.value = _localProfileFallback.value.copy(
      username = username.trim().ifBlank { _localProfileFallback.value.username },
      displayName = displayName.trim().ifBlank { _localProfileFallback.value.displayName },
      bio = bio.trim(),
    )
    executeGuarded("Profile update") {
      repository.updateProfileDetails(username, displayName, bio).recover {
        Unit
      }
    }
  }

  fun publishPost(
    postType: String,
    title: String,
    description: String,
    hashtags: String,
    mediaCount: Long,
    mediaUri: String,
    durationSeconds: Long,
  ) {
    val authorName = userProfile.value?.displayName?.ifBlank { currentDisplayName } ?: "Star Creator"
    executeGuarded("Publish $postType") {
      repository.createMediaPost(
        authorName = authorName,
        postType = postType,
        title = title,
        description = description,
        hashtags = hashtags,
        mediaCount = mediaCount,
        mediaUri = mediaUri,
        thumbnailTitle = title,
        durationSeconds = durationSeconds,
      )
    }
  }

  fun recordView(post: MediaPost) {
    viewModelScope.launch {
      repository.recordValidatedView(post).onSuccess { counted ->
        if (counted) {
          _statusBanner.value = "Verified unique view recorded for '${post.title}'."
        } else {
          _statusBanner.value = "Duplicate view filtered out (already counted for your account)."
        }
      }.onFailure { err ->
        _statusBanner.value = err.localizedMessage ?: "Could not record view."
      }
    }
  }

  fun likePost(postId: String) {
    viewModelScope.launch {
      repository.likePost(postId)
    }
  }

  fun sharePost(postId: String) {
    viewModelScope.launch {
      repository.sharePost(postId).onSuccess {
        _statusBanner.value = "Share count updated."
      }
    }
  }

  fun addComment(postId: String, text: String) {
    val authorName = userProfile.value?.displayName?.ifBlank { currentDisplayName } ?: "Star Creator"
    executeGuarded("Comment posted") {
      repository.addPostComment(postId, authorName, text)
    }
  }

  fun toggleFollow(targetUserId: String, targetName: String) {
    if (targetUserId == currentUserId) {
      _statusBanner.value = "You are viewing your own creator profile."
      return
    }
    executeGuarded("Follow state updated") {
      repository.toggleFollowCreator(targetUserId, targetName)
    }
  }

  fun startBroadcast(
    title: String,
    category: String,
    isPkBattle: Boolean,
    opponentName: String,
  ) {
    val hostName = userProfile.value?.displayName?.ifBlank { currentDisplayName } ?: "Star Host"
    executeGuarded("Live broadcast started") {
      repository.startLiveStream(
        hostName = hostName,
        title = title,
        streamCategory = category,
        isPkBattle = isPkBattle,
        opponentId = if (isPkBattle) "pk_challenger_${System.currentTimeMillis() % 10000}" else "",
        opponentName = if (isPkBattle) opponentName.ifBlank { "PK Challenger" } else "",
      )
    }
  }

  fun endBroadcast(streamId: String) {
    executeGuarded("Broadcast ended") {
      repository.endLiveStream(streamId)
    }
    if (_selectedLiveStream.value?.id == streamId) {
      _selectedLiveStream.value = null
    }
  }

  fun sendLiveChat(streamId: String, text: String) {
    val senderName = userProfile.value?.displayName?.ifBlank { currentDisplayName } ?: "Star Viewer"
    executeGuarded("Chat message sent") {
      repository.sendStreamChatMessage(streamId, senderName, text)
    }
  }

  fun sendLiveGift(
    stream: LiveStream,
    giftName: String,
    diamondCost: Long,
    supportHostSide: Boolean,
  ) {
    val senderName = userProfile.value?.displayName?.ifBlank { currentDisplayName } ?: "Star Supporter"
    executeGuarded("Verified gift ($giftName)") {
      repository.sendVerifiedLiveGift(
        stream = stream,
        senderName = senderName,
        giftName = giftName,
        diamondCost = diamondCost,
        supportHostSide = supportHostSide,
      )
    }
  }

  fun requestDiamondPurchaseOrder(
    packageId: String,
    diamondsAmount: Long,
    amountInrPaise: Long,
    paymentRef: String,
  ) {
    executeGuarded("Diamond purchase order submitted for backend verification") {
      repository.createDiamondPurchaseOrder(
        packageId = packageId,
        diamondsAmount = diamondsAmount,
        amountInrPaise = amountInrPaise,
        paymentRef = paymentRef,
      )
    }
  }

  fun addBankAccount(
    holderName: String,
    bankName: String,
    accountNumber: String,
    ifscCode: String,
  ) {
    executeGuarded("Bank account submitted for verification") {
      repository.addBankAccount(holderName, bankName, accountNumber, ifscCode)
    }
  }

  fun requestWithdrawal(bankAccountId: String, amountInrPaise: Long) {
    val level = userProfile.value?.vipLevel ?: 1L
    executeGuarded("Withdrawal request submitted") {
      repository.submitWithdrawalRequest(bankAccountId, amountInrPaise, level)
    }
  }

  fun submitReport(targetType: String, targetId: String, reasonCategory: String, details: String) {
    executeGuarded("Moderation report logged") {
      repository.submitModerationReport(targetType, targetId, reasonCategory, details)
    }
  }

  fun submitAppeal(reportId: String, appealText: String) {
    executeGuarded("Appeal submitted") {
      repository.submitReportAppeal(reportId, appealText)
    }
  }

  fun submitSupportTicket(category: String, subject: String, message: String) {
    executeGuarded("Support ticket created") {
      repository.createSupportTicket(category, subject, message)
    }
  }

  fun markNotificationAsRead(notifId: String) {
    viewModelScope.launch {
      repository.markNotificationRead(notifId)
    }
  }

  // --- ADMIN ACTIONS ---

  fun adminVerifyPurchase(tx: WalletTransaction) {
    executeGuarded("Admin verified diamond purchase & credited wallet") {
      repository.adminVerifyDiamondPurchase(tx)
    }
  }

  fun adminModerateUser(targetUserId: String, newStatus: String, reason: String) {
    executeGuarded("Admin account status updated to $newStatus") {
      repository.adminModerateUserAccount(targetUserId, newStatus, reason)
    }
  }

  fun adminSetVipAndEarnings(targetUserId: String, vipLevel: Long, approvedPaise: Long) {
    executeGuarded("Admin updated creator VIP level & approved earnings") {
      repository.adminUpdateUserVipAndEarnings(targetUserId, vipLevel, approvedPaise)
    }
  }

  fun adminVerifyBank(accountId: String, status: String) {
    executeGuarded("Bank verification updated to $status") {
      repository.adminVerifyBankAccount(accountId, status)
    }
  }

  fun adminUpdateWithdrawal(withdrawal: WithdrawalRequest, newStatus: String, note: String) {
    executeGuarded("Withdrawal marked $newStatus") {
      repository.adminProcessWithdrawal(withdrawal, newStatus, note)
    }
  }

  fun adminResolveReport(reportId: String, newStatus: String, actionNote: String) {
    executeGuarded("Report resolved ($newStatus)") {
      repository.adminResolveReport(reportId, newStatus, actionNote)
    }
  }

  fun adminRemoveViolatingPost(postId: String) {
    executeGuarded("Violating content removed") {
      repository.adminRemovePost(postId)
    }
  }

  fun adminReplyTicket(ticketId: String, newStatus: String, reply: String) {
    executeGuarded("Support ticket updated") {
      repository.adminReplySupportTicket(ticketId, newStatus, reply)
    }
  }

  private companion object {
    const val TAG = "HelloStarVM"
  }
}
