package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.HelloStarRepository
import com.example.ui.AppDestination
import com.example.ui.HelloStarViewModel
import com.example.ui.components.HelloStarVibeLogoHeader
import com.example.ui.components.rememberSingleTap
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CreatorEarningsScreen
import com.example.ui.screens.DiamondWalletScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LiveStreamingScreen
import com.example.ui.screens.PostCommentsDialog
import com.example.ui.screens.ReelsVerticalScreen
import com.example.ui.screens.SearchAndNotificationsScreen
import com.example.ui.screens.SupportCenterScreen
import com.example.ui.screens.UserProfileScreen
import com.example.ui.screens.VideosOrPhotosScreen
import com.example.ui.screens.VipLevelsScreen
import com.example.ui.screens.WithdrawalAndBankScreen
import com.example.ui.screens.signOutUser
import com.example.ui.theme.AccentPink
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.StarBackground
import com.example.ui.theme.StarCardBackground
import com.example.ui.theme.StarCardElevated
import com.example.ui.theme.StarPrimaryText
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        HelloStarAppRoot()
      }
    }
  }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
  val listener = FirebaseAuth.AuthStateListener { auth ->
    trySend(auth.currentUser)
  }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun HelloStarAppRoot(auth: FirebaseAuth = Firebase.auth) {
  val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  val user = currentUser

  var isSessionConfirmed by remember { mutableStateOf(auth.currentUser != null) }
  var confirmedGmail by remember { mutableStateOf(auth.currentUser?.email ?: "") }
  var isNewAccountWelcome by remember { mutableStateOf(false) }

  if (!isSessionConfirmed) {
    AuthScreen(
      onAuthSuccess = { email, isNewSignup ->
        confirmedGmail = email.ifBlank { auth.currentUser?.email ?: "Verified User" }
        isNewAccountWelcome = isNewSignup
        isSessionConfirmed = true
      }
    )
  } else {
    val effectiveEmail = confirmedGmail.ifBlank { user?.email ?: "Verified User" }
    val effectiveUid = user?.uid ?: ("otp_" + effectiveEmail.replace(Regex("[^a-zA-Z0-9]"), "_").take(24))
    val effectiveDisplayName = user?.displayName?.takeIf { it.isNotBlank() }
      ?: effectiveEmail.substringBefore("@").ifBlank { "Star Creator" }

    val vm: HelloStarViewModel = viewModel(
      key = effectiveUid,
      factory = viewModelFactory {
        initializer {
          val app = checkNotNull(this[APPLICATION_KEY]) {
            "APPLICATION_KEY missing from CreationExtras"
          }
          val databaseId = app.getString(R.string.firestore_database_id)
          val db = FirebaseFirestore.getInstance(databaseId)
          HelloStarViewModel(
            repository = HelloStarRepository(db, fallbackUserId = effectiveUid),
            currentUserId = effectiveUid,
            currentUserEmail = effectiveEmail,
            currentDisplayName = effectiveDisplayName,
          )
        }
      },
    )
    LaunchedEffect(effectiveUid, isNewAccountWelcome) {
      if (effectiveEmail.isNotBlank()) {
        vm.announceAuthenticatedSession(effectiveEmail, isNewAccountWelcome)
      }
    }
    AuthenticatedMainShell(
      viewModel = vm,
      onSessionLoggedOut = {
        isSessionConfirmed = false
        confirmedGmail = ""
        isNewAccountWelcome = false
      },
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticatedMainShell(
  viewModel: HelloStarViewModel,
  onSessionLoggedOut: () -> Unit = {},
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  val currentDestination by viewModel.currentDestination.collectAsStateWithLifecycle()
  val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
  val profile by viewModel.userProfile.collectAsStateWithLifecycle()
  val posts by viewModel.publicPosts.collectAsStateWithLifecycle()
  val follows by viewModel.myFollows.collectAsStateWithLifecycle()
  val liveStreams by viewModel.activeLiveStreams.collectAsStateWithLifecycle()
  val selectedLiveStream by viewModel.selectedLiveStream.collectAsStateWithLifecycle()
  val streamMessages by viewModel.activeStreamMessages.collectAsStateWithLifecycle()
  val walletTransactions by viewModel.walletTransactions.collectAsStateWithLifecycle()
  val bankAccounts by viewModel.bankAccounts.collectAsStateWithLifecycle()
  val withdrawals by viewModel.withdrawals.collectAsStateWithLifecycle()
  val myReports by viewModel.myReports.collectAsStateWithLifecycle()
  val myTickets by viewModel.mySupportTickets.collectAsStateWithLifecycle()
  val notifications by viewModel.notifications.collectAsStateWithLifecycle()
  val selectedPostForComments by viewModel.selectedPostForComments.collectAsStateWithLifecycle()
  val activePostComments by viewModel.activePostComments.collectAsStateWithLifecycle()

  val adminUsers by viewModel.adminAllUsers.collectAsStateWithLifecycle()
  val adminReports by viewModel.adminAllReports.collectAsStateWithLifecycle()
  val adminTransactions by viewModel.adminAllTransactions.collectAsStateWithLifecycle()
  val adminBanks by viewModel.adminAllBankAccounts.collectAsStateWithLifecycle()
  val adminWithdrawals by viewModel.adminAllWithdrawals.collectAsStateWithLifecycle()
  val adminTickets by viewModel.adminAllTickets.collectAsStateWithLifecycle()

  BackHandler(enabled = currentDestination != AppDestination.HOME) {
    if (selectedLiveStream != null) {
      viewModel.selectLiveStream(null)
    } else {
      viewModel.navigateTo(AppDestination.HOME)
    }
  }

  val bottomTabs = remember {
    listOf(
      Triple(AppDestination.HOME, "Home", Icons.Default.Home),
      Triple(AppDestination.REELS, "Reels", Icons.Default.PlayCircleFilled),
      Triple(AppDestination.LIVE, "Live & PK", Icons.Default.LiveTv),
      Triple(AppDestination.WALLET, "Wallet", Icons.Default.AccountBalanceWallet),
      Triple(AppDestination.PROFILE, "Profile", Icons.Default.Person),
    )
  }

  val onSearchClick = rememberSingleTap(cooldownMs = 400L) {
    viewModel.navigateTo(AppDestination.SEARCH)
  }
  val onNotificationsClick = rememberSingleTap(cooldownMs = 400L) {
    viewModel.navigateTo(AppDestination.NOTIFICATIONS)
  }

  Scaffold(
    containerColor = StarBackground,
    topBar = {
      TopAppBar(
        title = {
          HelloStarVibeLogoHeader(
            subtitle = if (viewModel.currentUserEmail.isNotBlank()) {
              "${currentDestination.title} • ${viewModel.currentUserEmail}"
            } else {
              currentDestination.title
            },
            compact = true,
          )
        },
        actions = {
          IconButton(onClick = onSearchClick) {
            Icon(Icons.Default.Search, contentDescription = "Search", tint = ElectricBlue)
          }
          IconButton(onClick = onNotificationsClick) {
            Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = AccentPink)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = StarCardBackground,
          titleContentColor = StarPrimaryText,
        ),
      )
    },
    bottomBar = {
      NavigationBar(
        containerColor = StarCardBackground,
        contentColor = StarPrimaryText,
      ) {
        bottomTabs.forEach { (dest, label, icon) ->
          val singleTapTab = rememberSingleTap(cooldownMs = 350L) {
            if (currentDestination != dest) {
              viewModel.navigateTo(dest)
            }
          }
          NavigationBarItem(
            selected = currentDestination == dest,
            onClick = singleTapTab,
            icon = { Icon(icon, contentDescription = label) },
            label = { Text(label) },
            modifier = Modifier.testTag("bottom_nav_${dest.name.lowercase()}"),
          )
        }
      }
    },
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      statusBanner?.let { msg ->
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("status_banner_card"),
          shape = RoundedCornerShape(12.dp),
          color = StarCardElevated,
          border = BorderStroke(1.dp, ElectricBlue),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = msg,
              style = MaterialTheme.typography.bodyMedium,
              color = StarPrimaryText,
              modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { viewModel.clearBanner() }) {
              Text("Dismiss", color = ElectricBlue)
            }
          }
        }
      }

      Box(modifier = Modifier.weight(1f)) {
        when (currentDestination) {
          AppDestination.HOME -> HomeScreen(
            profile = profile,
            userEmail = viewModel.currentUserEmail,
            posts = posts,
            liveStreams = liveStreams,
            follows = follows,
            isAuthorizedAdmin = viewModel.isAuthorizedAdmin,
            onNavigate = { viewModel.navigateTo(it) },
            onPublishPost = { type, title, desc, tags, count, uri, dur ->
              viewModel.publishPost(type, title, desc, tags, count, uri, dur)
            },
            onRecordView = { viewModel.recordView(it) },
            onLikePost = { viewModel.likePost(it) },
            onSharePost = { viewModel.sharePost(it) },
            onOpenComments = { viewModel.openCommentsForPost(it) },
            onToggleFollow = { targetId, targetName -> viewModel.toggleFollow(targetId, targetName) },
            onReportContent = { type, id, reason, details ->
              viewModel.submitReport(type, id, reason, details)
            },
          )

          AppDestination.REELS -> ReelsVerticalScreen(
            reels = posts.filter { it.postType == "reel" },
            follows = follows,
            onRecordView = { viewModel.recordView(it) },
            onLikePost = { viewModel.likePost(it) },
            onSharePost = { viewModel.sharePost(it) },
            onOpenComments = { viewModel.openCommentsForPost(it) },
            onToggleFollow = { targetId, targetName -> viewModel.toggleFollow(targetId, targetName) },
            onReportContent = { type, id, reason, details ->
              viewModel.submitReport(type, id, reason, details)
            },
            onPublishReel = { title, desc, tags, uri, dur ->
              viewModel.publishPost("reel", title, desc, tags, 1L, uri, dur)
            },
          )

          AppDestination.VIDEOS -> VideosOrPhotosScreen(
            postTypeFilter = "video",
            posts = posts,
            follows = follows,
            onPublishPost = { type, title, desc, tags, count, uri, dur ->
              viewModel.publishPost(type, title, desc, tags, count, uri, dur)
            },
            onRecordView = { viewModel.recordView(it) },
            onLikePost = { viewModel.likePost(it) },
            onSharePost = { viewModel.sharePost(it) },
            onOpenComments = { viewModel.openCommentsForPost(it) },
            onToggleFollow = { targetId, targetName -> viewModel.toggleFollow(targetId, targetName) },
            onReportContent = { type, id, reason, details ->
              viewModel.submitReport(type, id, reason, details)
            },
          )

          AppDestination.PHOTOS -> VideosOrPhotosScreen(
            postTypeFilter = "photo",
            posts = posts,
            follows = follows,
            onPublishPost = { type, title, desc, tags, count, uri, dur ->
              viewModel.publishPost(type, title, desc, tags, count, uri, dur)
            },
            onRecordView = { viewModel.recordView(it) },
            onLikePost = { viewModel.likePost(it) },
            onSharePost = { viewModel.sharePost(it) },
            onOpenComments = { viewModel.openCommentsForPost(it) },
            onToggleFollow = { targetId, targetName -> viewModel.toggleFollow(targetId, targetName) },
            onReportContent = { type, id, reason, details ->
              viewModel.submitReport(type, id, reason, details)
            },
          )

          AppDestination.LIVE -> LiveStreamingScreen(
            currentUserId = viewModel.currentUserId,
            profile = profile,
            activeStreams = liveStreams,
            selectedStream = selectedLiveStream,
            streamMessages = streamMessages,
            onSelectStream = { viewModel.selectLiveStream(it) },
            onStartBroadcast = { title, category, isPk, opponent ->
              viewModel.startBroadcast(title, category, isPk, opponent)
            },
            onEndBroadcast = { viewModel.endBroadcast(it) },
            onSendChat = { streamId, text -> viewModel.sendLiveChat(streamId, text) },
            onSendVerifiedGift = { stream, giftName, cost, supportHost ->
              viewModel.sendLiveGift(stream, giftName, cost, supportHost)
            },
            onFollowHost = { hostId, hostName -> viewModel.toggleFollow(hostId, hostName) },
            onReport = { type, id, reason, details ->
              viewModel.submitReport(type, id, reason, details)
            },
          )

          AppDestination.SEARCH -> SearchAndNotificationsScreen(
            isSearchMode = true,
            posts = posts,
            liveStreams = liveStreams,
            notifications = notifications,
            onRecordView = { viewModel.recordView(it) },
            onMarkNotificationRead = { viewModel.markNotificationAsRead(it) },
            onNavigateToLive = { viewModel.navigateTo(AppDestination.LIVE) },
          )

          AppDestination.NOTIFICATIONS -> SearchAndNotificationsScreen(
            isSearchMode = false,
            posts = posts,
            liveStreams = liveStreams,
            notifications = notifications,
            onRecordView = { viewModel.recordView(it) },
            onMarkNotificationRead = { viewModel.markNotificationAsRead(it) },
            onNavigateToLive = { viewModel.navigateTo(AppDestination.LIVE) },
          )

          AppDestination.WALLET -> DiamondWalletScreen(
            profile = profile,
            transactions = walletTransactions,
            onCreatePurchaseOrder = { pkgId, diamonds, paise, ref ->
              viewModel.requestDiamondPurchaseOrder(pkgId, diamonds, paise, ref)
            },
          )

          AppDestination.EARNINGS -> CreatorEarningsScreen(
            profile = profile,
            onNavigateToWithdrawal = { viewModel.navigateTo(AppDestination.WITHDRAWAL) },
          )

          AppDestination.WITHDRAWAL -> WithdrawalAndBankScreen(
            profile = profile,
            bankAccounts = bankAccounts,
            withdrawals = withdrawals,
            onAddBankAccount = { holder, bank, acct, ifsc ->
              viewModel.addBankAccount(holder, bank, acct, ifsc)
            },
            onSubmitWithdrawal = { bankId, paise ->
              viewModel.requestWithdrawal(bankId, paise)
            },
          )

          AppDestination.VIP -> VipLevelsScreen(profile = profile)

          AppDestination.PROFILE -> UserProfileScreen(
            profile = profile,
            userEmail = viewModel.currentUserEmail,
            myPosts = posts.filter { it.authorId == viewModel.currentUserId },
            myReports = myReports,
            onUpdateProfile = { u, d, b -> viewModel.updateProfile(u, d, b) },
            onSubmitAppeal = { reportId, text -> viewModel.submitAppeal(reportId, text) },
            onReportProfile = { reason, details ->
              viewModel.submitReport("user_profile", viewModel.currentUserId, reason, details)
            },
            onSignOut = {
              signOutUser(
                context = context,
                credentialManager = credentialManager,
                onSignOutComplete = {
                  onSessionLoggedOut()
                },
                scope = scope,
              )
            },
          )

          AppDestination.SUPPORT -> SupportCenterScreen(
            tickets = myTickets,
            onCreateTicket = { cat, subj, msg ->
              viewModel.submitSupportTicket(cat, subj, msg)
            },
          )

          AppDestination.ADMIN -> AdminDashboardScreen(
            isAuthorizedAdmin = viewModel.isAuthorizedAdmin,
            users = adminUsers,
            posts = posts,
            reports = adminReports,
            transactions = adminTransactions,
            bankAccounts = adminBanks,
            withdrawals = adminWithdrawals,
            tickets = adminTickets,
            onModerateUser = { uid, status, reason -> viewModel.adminModerateUser(uid, status, reason) },
            onSetVipAndEarnings = { uid, lvl, paise -> viewModel.adminSetVipAndEarnings(uid, lvl, paise) },
            onRemovePost = { viewModel.adminRemoveViolatingPost(it) },
            onResolveReport = { rId, status, note -> viewModel.adminResolveReport(rId, status, note) },
            onVerifyDiamondPurchase = { viewModel.adminVerifyPurchase(it) },
            onVerifyBankAccount = { bId, status -> viewModel.adminVerifyBank(bId, status) },
            onProcessWithdrawal = { wd, status, note -> viewModel.adminUpdateWithdrawal(wd, status, note) },
            onReplyTicket = { tId, status, reply -> viewModel.adminReplyTicket(tId, status, reply) },
          )
        }
      }
    }
  }

  selectedPostForComments?.let { post ->
    PostCommentsDialog(
      post = post,
      comments = activePostComments,
      onDismiss = { viewModel.openCommentsForPost(null) },
      onAddComment = { text -> viewModel.addComment(post.id, text) },
    )
  }
}
