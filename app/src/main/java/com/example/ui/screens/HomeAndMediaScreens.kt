package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.CreatorRewardCalculator
import com.example.data.LiveStream
import com.example.data.MediaPost
import com.example.data.PostComment
import com.example.data.UserFollow
import com.example.data.UserNotification
import com.example.data.UserProfile
import com.example.ui.AppDestination
import com.example.ui.components.HelloStarVibeLogoHeader
import com.example.ui.components.NeonStarHeroBanner
import com.example.ui.components.ReportContentDialog
import com.example.ui.components.SingleTapButton
import com.example.ui.theme.AccentPink
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.StarBackground
import com.example.ui.theme.StarCardBackground
import com.example.ui.theme.StarCardElevated
import com.example.ui.theme.StarOutlineBorder
import com.example.ui.theme.StarPrimaryText
import com.example.ui.theme.StarSecondaryText
import com.example.ui.theme.StarSuccessGreen
import com.example.ui.theme.StarWarningGold

private data class QuickModuleItem(
  val label: String,
  val destination: AppDestination,
  val icon: ImageVector,
  val accentColor: androidx.compose.ui.graphics.Color,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
  profile: UserProfile?,
  userEmail: String,
  posts: List<MediaPost>,
  liveStreams: List<LiveStream>,
  follows: List<UserFollow>,
  isAuthorizedAdmin: Boolean,
  onNavigate: (AppDestination) -> Unit,
  onPublishPost: (postType: String, title: String, description: String, hashtags: String, mediaCount: Long, mediaUri: String, durationSeconds: Long) -> Unit,
  onRecordView: (MediaPost) -> Unit,
  onLikePost: (String) -> Unit,
  onSharePost: (String) -> Unit,
  onOpenComments: (MediaPost) -> Unit,
  onToggleFollow: (String, String) -> Unit,
  onReportContent: (targetType: String, targetId: String, reason: String, details: String) -> Unit,
) {
  var showUploadDialog by remember { mutableStateOf(false) }
  var reportTarget by remember { mutableStateOf<Pair<String, String>?>(null) }

  val modules = remember(isAuthorizedAdmin) {
    buildList {
      add(QuickModuleItem("Reels", AppDestination.REELS, Icons.Default.PlayCircleFilled, AccentPink))
      add(QuickModuleItem("Videos", AppDestination.VIDEOS, Icons.Default.VideoLibrary, ElectricBlue))
      add(QuickModuleItem("Photos", AppDestination.PHOTOS, Icons.Default.PhotoLibrary, NeonPurple))
      add(QuickModuleItem("Live & PK", AppDestination.LIVE, Icons.Default.LiveTv, AccentPink))
      add(QuickModuleItem("Wallet & Diamonds", AppDestination.WALLET, Icons.Default.Diamond, ElectricBlue))
      add(QuickModuleItem("Creator Earnings", AppDestination.EARNINGS, Icons.Default.MonetizationOn, StarSuccessGreen))
      add(QuickModuleItem("Withdrawal", AppDestination.WITHDRAWAL, Icons.Default.AccountBalance, StarWarningGold))
      add(QuickModuleItem("VIP Levels", AppDestination.VIP, Icons.Default.Star, NeonPurple))
      add(QuickModuleItem("Search", AppDestination.SEARCH, Icons.Default.Search, ElectricBlue))
      add(QuickModuleItem("Notifications", AppDestination.NOTIFICATIONS, Icons.Default.Notifications, AccentPink))
      add(QuickModuleItem("Profile", AppDestination.PROFILE, Icons.Default.Person, NeonPurple))
      add(QuickModuleItem("Help & Support", AppDestination.SUPPORT, Icons.AutoMirrored.Filled.HelpOutline, ElectricBlue))
      if (isAuthorizedAdmin) {
        add(QuickModuleItem("Admin Dashboard", AppDestination.ADMIN, Icons.Default.AdminPanelSettings, StarWarningGold))
      }
    }
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .testTag("home_screen_list"),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        HelloStarVibeLogoHeader(
          subtitle = if (userEmail.isNotBlank()) "Gmail: $userEmail" else "Creator & Live PK Network",
          compact = true,
        )
        SingleTapButton(
          text = "+ Upload",
          onClick = { showUploadDialog = true },
          containerColor = AccentPink,
          testTag = "open_upload_dialog_button",
        )
      }
    }

    item {
      val vipLvl = profile?.vipLevel ?: 1L
      val diamonds = profile?.diamondBalance ?: 0L
      val pendingPaise = profile?.pendingEarningsPaise ?: 0L
      val realName = profile?.displayName?.takeIf { it.isNotBlank() }
        ?: userEmail.substringBefore("@").ifBlank { "Authenticated User" }
      val realHandle = profile?.username?.takeIf { it.isNotBlank() }
        ?: userEmail.substringBefore("@")
      val emailLabel = if (userEmail.isNotBlank()) "Gmail ID: $userEmail • " else ""
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        NeonStarHeroBanner(
          title = "Welcome, $realName",
          subtitle = "${emailLabel}VIP Level $vipLvl • $diamonds Diamonds • Pending: ${CreatorRewardCalculator.formatInrFromPaise(pendingPaise)}",
          badgeText = "HELLO STAR VIBE • 3D LIVE",
        )
        if (userEmail.isNotBlank()) {
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .testTag("authenticated_profile_card"),
            shape = RoundedCornerShape(14.dp),
            color = StarCardBackground,
            border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.5f)),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                  text = "$realName (@$realHandle)",
                  style = MaterialTheme.typography.titleSmall,
                  color = StarPrimaryText,
                )
                Text(
                  text = "Authenticated Gmail ID: $userEmail",
                  style = MaterialTheme.typography.bodySmall,
                  color = ElectricBlue,
                )
              }
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = StarSuccessGreen.copy(alpha = 0.18f),
                border = BorderStroke(1.dp, StarSuccessGreen),
              ) {
                Text(
                  text = "VERIFIED",
                  style = MaterialTheme.typography.labelSmall,
                  color = StarSuccessGreen,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
              }
            }
          }
        }
      }
    }

    // Compact Quick Navigation Grid
    item {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "Explore Platform",
          style = MaterialTheme.typography.titleMedium,
          color = StarPrimaryText,
        )
        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
          maxItemsInEachRow = 3,
        ) {
          modules.forEach { item ->
            val singleTapNav = com.example.ui.components.rememberSingleTap(cooldownMs = 400L) {
              onNavigate(item.destination)
            }
            Surface(
              modifier = Modifier
                .weight(1f)
                .clickable(onClick = singleTapNav)
                .testTag("nav_chip_${item.destination.name.lowercase()}"),
              shape = RoundedCornerShape(12.dp),
              color = StarCardBackground,
              border = BorderStroke(1.dp, item.accentColor.copy(alpha = 0.45f)),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Icon(
                  imageVector = item.icon,
                  contentDescription = item.label,
                  tint = item.accentColor,
                  modifier = Modifier.size(18.dp),
                )
                Text(
                  text = item.label,
                  style = MaterialTheme.typography.labelSmall,
                  color = StarPrimaryText,
                  maxLines = 1,
                )
              }
            }
          }
        }
      }
    }

    // Active Live Broadcasts & PK Battles Strip
    if (liveStreams.isNotEmpty()) {
      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Live Broadcasts & PK Battles (${liveStreams.size})",
              style = MaterialTheme.typography.titleMedium,
              color = ElectricBlue,
            )
            TextButton(onClick = { onNavigate(AppDestination.LIVE) }) {
              Text("Join Live", color = AccentPink)
            }
          }
          liveStreams.take(2).forEach { stream ->
            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigate(AppDestination.LIVE) },
              shape = RoundedCornerShape(14.dp),
              color = StarCardElevated,
              border = BorderStroke(1.dp, if (stream.isPkBattle) AccentPink else ElectricBlue),
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = if (stream.isPkBattle) "PK BATTLE: ${stream.hostName} vs ${stream.opponentName}" else "LIVE: ${stream.title}",
                    style = MaterialTheme.typography.titleSmall,
                    color = StarPrimaryText,
                  )
                  Text(
                    text = "Host: ${stream.hostName} • ${stream.viewerCount} watching",
                    style = MaterialTheme.typography.bodyMedium,
                    color = StarSecondaryText,
                  )
                }
                SingleTapButton(
                  text = "Watch",
                  onClick = { onNavigate(AppDestination.LIVE) },
                  containerColor = AccentPink,
                )
              }
            }
          }
        }
      }
    }

    // Main Feed Header
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Latest Creator Feed",
          style = MaterialTheme.typography.titleMedium,
          color = StarPrimaryText,
        )
        Text(
          text = "${posts.size} posts",
          style = MaterialTheme.typography.labelSmall,
          color = StarSecondaryText,
        )
      }
    }

    if (posts.isEmpty()) {
      item {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          color = StarCardBackground,
          border = BorderStroke(1.dp, StarOutlineBorder),
        ) {
          Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(
              imageVector = Icons.Default.AddCircle,
              contentDescription = null,
              tint = NeonPurple,
              modifier = Modifier.size(40.dp),
            )
            Text(
              text = "No Posts Uploaded Yet",
              style = MaterialTheme.typography.titleMedium,
              color = StarPrimaryText,
            )
            Text(
              text = "Be the first creator to publish a Reel, Long Video, or Multi-Photo Post on HELLO STAR VIBE!",
              style = MaterialTheme.typography.bodyMedium,
              color = StarSecondaryText,
            )
            SingleTapButton(
              text = "Upload Your First Post",
              onClick = { showUploadDialog = true },
              containerColor = NeonPurple,
            )
          }
        }
      }
    } else {
      items(posts, key = { it.id }) { post ->
        val isFollowing = follows.any { it.targetUserId == post.authorId }
        MediaPostCard(
          post = post,
          isFollowing = isFollowing,
          onRecordView = { onRecordView(post) },
          onLike = { onLikePost(post.id) },
          onComment = { onOpenComments(post) },
          onShare = { onSharePost(post.id) },
          onFollow = { onToggleFollow(post.authorId, post.authorName) },
          onReport = { reportTarget = post.postType to post.id },
        )
      }
    }
  }

  if (showUploadDialog) {
    UploadPostDialog(
      onDismiss = { showUploadDialog = false },
      onSubmit = { type, title, desc, tags, count, uri, duration ->
        onPublishPost(type, title, desc, tags, count, uri, duration)
        showUploadDialog = false
      },
    )
  }

  reportTarget?.let { (type, id) ->
    ReportContentDialog(
      targetType = type,
      targetId = id,
      targetLabel = type.replaceFirstChar { it.uppercase() },
      onDismiss = { reportTarget = null },
      onSubmitReport = { reason, details ->
        onReportContent(type, id, reason, details)
        reportTarget = null
      },
    )
  }
}

@Composable
fun MediaPostCard(
  post: MediaPost,
  isFollowing: Boolean,
  onRecordView: () -> Unit,
  onLike: () -> Unit,
  onComment: () -> Unit,
  onShare: () -> Unit,
  onFollow: () -> Unit,
  onReport: () -> Unit,
) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("post_card_${post.id}"),
    shape = RoundedCornerShape(18.dp),
    color = StarCardBackground,
    border = BorderStroke(1.dp, StarOutlineBorder),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Box(
            modifier = Modifier
              .size(38.dp)
              .clip(CircleShape)
              .background(Brush.linearGradient(listOf(NeonPurple, ElectricBlue))),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = post.authorName.take(1).uppercase(),
              style = MaterialTheme.typography.titleSmall,
              color = StarPrimaryText,
            )
          }
          Column {
            Text(
              text = post.authorName,
              style = MaterialTheme.typography.titleSmall,
              color = StarPrimaryText,
            )
            Text(
              text = post.postType.uppercase() + if (post.postType == "photo" && post.mediaCount > 1) " (${post.mediaCount} photos)" else "",
              style = MaterialTheme.typography.labelSmall,
              color = ElectricBlue,
            )
          }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          OutlinedButton(
            onClick = onFollow,
            shape = RoundedCornerShape(50),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
          ) {
            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(if (isFollowing) "Following" else "Follow", style = MaterialTheme.typography.labelSmall)
          }
          IconButton(onClick = onReport) {
            Icon(Icons.Default.Flag, contentDescription = "Report Post", tint = StarSecondaryText)
          }
        }
      }

      // Visual Media Preview Banner
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(170.dp)
          .clip(RoundedCornerShape(14.dp))
          .background(
            Brush.linearGradient(
              colors = when (post.postType) {
                "reel" -> listOf(NeonPurple.copy(alpha = 0.45f), AccentPink.copy(alpha = 0.35f))
                "video" -> listOf(ElectricBlue.copy(alpha = 0.4f), NeonPurple.copy(alpha = 0.35f))
                else -> listOf(StarCardElevated, NeonPurple.copy(alpha = 0.3f))
              }
            )
          )
          .clickable { onRecordView() }
          .padding(16.dp),
        contentAlignment = Alignment.Center,
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            imageVector = if (post.postType == "photo") Icons.Default.PhotoLibrary else Icons.Default.PlayCircleFilled,
            contentDescription = "Watch or View Content",
            tint = StarPrimaryText,
            modifier = Modifier.size(44.dp),
          )
          Text(
            text = post.title,
            style = MaterialTheme.typography.titleMedium,
            color = StarPrimaryText,
          )
          Text(
            text = if (post.postType == "photo") {
              "Tap to view ${post.mediaCount} photo(s) • Verified Photo Engagement"
            } else {
              "Tap to play (${post.durationSeconds}s) • Verified View Counter"
            },
            style = MaterialTheme.typography.labelSmall,
            color = ElectricBlue,
          )
        }
      }

      if (post.description.isNotBlank()) {
        Text(
          text = post.description,
          style = MaterialTheme.typography.bodyMedium,
          color = StarPrimaryText,
        )
      }
      if (post.hashtags.isNotBlank()) {
        Text(
          text = post.hashtags,
          style = MaterialTheme.typography.labelSmall,
          color = AccentPink,
        )
      }

      // Engagement Bar
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        TextButton(onClick = onRecordView) {
          Icon(Icons.Default.Visibility, contentDescription = "Views", tint = ElectricBlue, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("${post.viewsCount} Views", color = StarPrimaryText)
        }
        TextButton(onClick = onLike) {
          Icon(Icons.Default.ThumbUp, contentDescription = "Likes", tint = AccentPink, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("${post.likesCount}", color = StarPrimaryText)
        }
        TextButton(onClick = onComment) {
          Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = "Comments", tint = NeonPurple, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("${post.commentsCount}", color = StarPrimaryText)
        }
        TextButton(onClick = onShare) {
          Icon(Icons.Default.Share, contentDescription = "Shares", tint = StarSecondaryText, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("${post.sharesCount}", color = StarPrimaryText)
        }
      }
    }
  }
}

/**
 * Vertical scrolling Reels and Shorts screen using Jetpack Compose VerticalPager.
 */
@Composable
fun ReelsVerticalScreen(
  reels: List<MediaPost>,
  follows: List<UserFollow>,
  onRecordView: (MediaPost) -> Unit,
  onLikePost: (String) -> Unit,
  onSharePost: (String) -> Unit,
  onOpenComments: (MediaPost) -> Unit,
  onToggleFollow: (String, String) -> Unit,
  onReportContent: (targetType: String, targetId: String, reason: String, details: String) -> Unit,
  onPublishReel: (title: String, desc: String, hashtags: String, uri: String, duration: Long) -> Unit,
) {
  var showCreateReel by remember { mutableStateOf(false) }
  var reportPost by remember { mutableStateOf<MediaPost?>(null) }

  Box(modifier = Modifier.fillMaxSize()) {
    if (reels.isEmpty()) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        Icon(Icons.Default.PlayCircleFilled, contentDescription = null, tint = AccentPink, modifier = Modifier.size(56.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text("No Vertical Reels Yet", style = MaterialTheme.typography.headlineMedium, color = StarPrimaryText)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          "Upload a short vertical Reel to start earning eligible Reel views.",
          style = MaterialTheme.typography.bodyMedium,
          color = StarSecondaryText,
        )
        Spacer(modifier = Modifier.height(16.dp))
        SingleTapButton(
          text = "Upload Reel Now",
          onClick = { showCreateReel = true },
          containerColor = AccentPink,
        )
      }
    } else {
      val pagerState = rememberPagerState(pageCount = { reels.size })
      VerticalPager(
        state = pagerState,
        modifier = Modifier
          .fillMaxSize()
          .testTag("reels_vertical_pager"),
      ) { page ->
        val reel = reels[page]
        val isFollowing = follows.any { it.targetUserId == reel.authorId }
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
              Brush.verticalGradient(
                listOf(StarCardElevated, StarBackground, NeonPurple.copy(alpha = 0.38f))
              )
            )
            .padding(20.dp),
        ) {
          Column(
            modifier = Modifier.align(Alignment.TopStart),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Surface(
              color = AccentPink.copy(alpha = 0.25f),
              shape = RoundedCornerShape(50),
            ) {
              Text(
                text = "REEL #${page + 1} of ${reels.size} • SWIPE UP/DOWN",
                style = MaterialTheme.typography.labelSmall,
                color = ElectricBlue,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
              )
            }
          }

          // Center Play / Verified View Trigger
          Column(
            modifier = Modifier
              .align(Alignment.Center)
              .clickable { onRecordView(reel) },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(
              imageVector = Icons.Default.PlayCircleFilled,
              contentDescription = "Play Reel",
              tint = ElectricBlue,
              modifier = Modifier.size(72.dp),
            )
            Text(
              text = reel.title,
              style = MaterialTheme.typography.headlineMedium,
              color = StarPrimaryText,
            )
            Text(
              text = "Tap to watch & count verified unique view (${reel.durationSeconds}s)",
              style = MaterialTheme.typography.bodyMedium,
              color = StarSecondaryText,
            )
          }

          // Bottom Creator Info + Right Action Rail
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
          ) {
            Column(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = "@${reel.authorName}",
                  style = MaterialTheme.typography.titleMedium,
                  color = StarPrimaryText,
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                  onClick = { onToggleFollow(reel.authorId, reel.authorName) },
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                ) {
                  Text(if (isFollowing) "Following" else "Follow", style = MaterialTheme.typography.labelSmall)
                }
              }
              Text(
                text = reel.description,
                style = MaterialTheme.typography.bodyMedium,
                color = StarSecondaryText,
              )
              if (reel.hashtags.isNotBlank()) {
                Text(
                  text = reel.hashtags,
                  style = MaterialTheme.typography.labelSmall,
                  color = ElectricBlue,
                )
              }
            }

            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              IconButton(onClick = { onRecordView(reel) }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Icon(Icons.Default.Visibility, contentDescription = "Views", tint = ElectricBlue)
                  Text("${reel.viewsCount}", style = MaterialTheme.typography.labelSmall)
                }
              }
              IconButton(onClick = { onLikePost(reel.id) }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Icon(Icons.Default.ThumbUp, contentDescription = "Like", tint = AccentPink)
                  Text("${reel.likesCount}", style = MaterialTheme.typography.labelSmall)
                }
              }
              IconButton(onClick = { onOpenComments(reel) }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = "Comment", tint = NeonPurple)
                  Text("${reel.commentsCount}", style = MaterialTheme.typography.labelSmall)
                }
              }
              IconButton(onClick = { onSharePost(reel.id) }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Icon(Icons.Default.Share, contentDescription = "Share", tint = StarPrimaryText)
                  Text("${reel.sharesCount}", style = MaterialTheme.typography.labelSmall)
                }
              }
              IconButton(onClick = { reportPost = reel }) {
                Icon(Icons.Default.Flag, contentDescription = "Report Reel", tint = StarSecondaryText)
              }
            }
          }
        }
      }
    }

    SingleTapButton(
      text = "+ Reel",
      onClick = { showCreateReel = true },
      containerColor = AccentPink,
      modifier = Modifier
        .align(Alignment.TopEnd)
        .padding(24.dp),
    )
  }

  if (showCreateReel) {
    UploadPostDialog(
      defaultType = "reel",
      onDismiss = { showCreateReel = false },
      onSubmit = { _, title, desc, tags, _, uri, duration ->
        onPublishReel(title, desc, tags, uri, duration)
        showCreateReel = false
      },
    )
  }

  reportPost?.let { reel ->
    ReportContentDialog(
      targetType = "reel",
      targetId = reel.id,
      targetLabel = "Reel '${reel.title}'",
      onDismiss = { reportPost = null },
      onSubmitReport = { reason, details ->
        onReportContent("reel", reel.id, reason, details)
        reportPost = null
      },
    )
  }
}

@Composable
fun VideosOrPhotosScreen(
  postTypeFilter: String, // "video" or "photo"
  posts: List<MediaPost>,
  follows: List<UserFollow>,
  onPublishPost: (postType: String, title: String, description: String, hashtags: String, mediaCount: Long, mediaUri: String, durationSeconds: Long) -> Unit,
  onRecordView: (MediaPost) -> Unit,
  onLikePost: (String) -> Unit,
  onSharePost: (String) -> Unit,
  onOpenComments: (MediaPost) -> Unit,
  onToggleFollow: (String, String) -> Unit,
  onReportContent: (targetType: String, targetId: String, reason: String, details: String) -> Unit,
) {
  var showUpload by remember { mutableStateOf(false) }
  var reportTarget by remember { mutableStateOf<MediaPost?>(null) }
  val filtered = remember(posts, postTypeFilter) {
    posts.filter { it.postType == postTypeFilter }
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = if (postTypeFilter == "photo") "Photo Posts & Galleries" else "Long-Form Creator Videos",
            style = MaterialTheme.typography.headlineMedium,
            color = StarPrimaryText,
          )
          Text(
            text = if (postTypeFilter == "photo") {
              "Single & multi-photo uploads tracked separately for Creator Rewards"
            } else {
              "Full-length videos with anti-fraud view validation"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = StarSecondaryText,
          )
        }
        SingleTapButton(
          text = if (postTypeFilter == "photo") "+ Photo Post" else "+ Video",
          onClick = { showUpload = true },
          containerColor = NeonPurple,
        )
      }
    }

    if (filtered.isEmpty()) {
      item {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          color = StarCardBackground,
          border = BorderStroke(1.dp, StarOutlineBorder),
        ) {
          Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = "No ${postTypeFilter.replaceFirstChar { it.uppercase() }} Posts Yet",
              style = MaterialTheme.typography.titleMedium,
            )
            SingleTapButton(
              text = "Upload First ${postTypeFilter.replaceFirstChar { it.uppercase() }}",
              onClick = { showUpload = true },
            )
          }
        }
      }
    } else {
      items(filtered, key = { it.id }) { post ->
        val isFollowing = follows.any { it.targetUserId == post.authorId }
        MediaPostCard(
          post = post,
          isFollowing = isFollowing,
          onRecordView = { onRecordView(post) },
          onLike = { onLikePost(post.id) },
          onComment = { onOpenComments(post) },
          onShare = { onSharePost(post.id) },
          onFollow = { onToggleFollow(post.authorId, post.authorName) },
          onReport = { reportTarget = post },
        )
      }
    }
  }

  if (showUpload) {
    UploadPostDialog(
      defaultType = postTypeFilter,
      onDismiss = { showUpload = false },
      onSubmit = { type, title, desc, tags, count, uri, duration ->
        onPublishPost(type, title, desc, tags, count, uri, duration)
        showUpload = false
      },
    )
  }

  reportTarget?.let { post ->
    ReportContentDialog(
      targetType = post.postType,
      targetId = post.id,
      targetLabel = post.title,
      onDismiss = { reportTarget = null },
      onSubmitReport = { reason, details ->
        onReportContent(post.postType, post.id, reason, details)
        reportTarget = null
      },
    )
  }
}

@Composable
fun SearchAndNotificationsScreen(
  isSearchMode: Boolean,
  posts: List<MediaPost>,
  liveStreams: List<LiveStream>,
  notifications: List<UserNotification>,
  onRecordView: (MediaPost) -> Unit,
  onMarkNotificationRead: (String) -> Unit,
  onNavigateToLive: () -> Unit,
) {
  var query by remember { mutableStateOf("") }

  if (isSearchMode) {
    val matchingPosts = remember(posts, query) {
      if (query.isBlank()) posts else posts.filter {
        it.title.contains(query, ignoreCase = true) ||
          it.authorName.contains(query, ignoreCase = true) ||
          it.hashtags.contains(query, ignoreCase = true)
      }
    }
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        Text("Search Creators, Reels, Videos & Hashtags", style = MaterialTheme.typography.headlineMedium)
      }
      item {
        OutlinedTextField(
          value = query,
          onValueChange = { query = it },
          label = { Text("Search by title, @creator, or #hashtag") },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("search_query_input"),
        )
      }
      item {
        Text(
          text = "Results (${matchingPosts.size} posts, ${liveStreams.size} active streams)",
          style = MaterialTheme.typography.labelLarge,
          color = ElectricBlue,
        )
      }
      items(matchingPosts, key = { it.id }) { post ->
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onRecordView(post) },
          shape = RoundedCornerShape(14.dp),
          color = StarCardBackground,
          border = BorderStroke(1.dp, StarOutlineBorder),
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(post.title, style = MaterialTheme.typography.titleSmall, color = StarPrimaryText)
              Text(
                "${post.postType.uppercase()} by @${post.authorName} • ${post.viewsCount} views",
                style = MaterialTheme.typography.bodyMedium,
                color = StarSecondaryText,
              )
            }
            Text(post.hashtags, style = MaterialTheme.typography.labelSmall, color = AccentPink)
          }
        }
      }
    }
  } else {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        Text("Notifications & Alerts", style = MaterialTheme.typography.headlineMedium)
      }
      if (notifications.isEmpty()) {
        item {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = StarCardBackground,
            border = BorderStroke(1.dp, StarOutlineBorder),
          ) {
            Column(modifier = Modifier.padding(20.dp)) {
              Text("You're all caught up!", style = MaterialTheme.typography.titleMedium)
              Text(
                "Live broadcast alerts, new followers, diamond gift notifications, and moderation updates appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = StarSecondaryText,
              )
            }
          }
        }
      } else {
        items(notifications, key = { it.id }) { notif ->
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onMarkNotificationRead(notif.id) },
            shape = RoundedCornerShape(14.dp),
            color = if (notif.isRead) StarCardBackground else StarCardElevated,
            border = BorderStroke(1.dp, if (notif.isRead) StarOutlineBorder else ElectricBlue),
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(notif.title, style = MaterialTheme.typography.titleSmall, color = StarPrimaryText)
              Text(notif.body, style = MaterialTheme.typography.bodyMedium, color = StarSecondaryText)
            }
          }
        }
      }
    }
  }
}

@Composable
fun UploadPostDialog(
  defaultType: String = "reel",
  onDismiss: () -> Unit,
  onSubmit: (postType: String, title: String, description: String, hashtags: String, mediaCount: Long, mediaUri: String, durationSeconds: Long) -> Unit,
) {
  var postType by remember { mutableStateOf(defaultType) }
  var title by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var hashtags by remember { mutableStateOf("#HelloStarVibe #Creator") }
  var selectedMediaUris by remember { mutableStateOf<List<String>>(emptyList()) }
  var durationInput by remember { mutableStateOf(if (defaultType == "photo") "0" else "30") }

  val multiMediaPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
  ) { uris ->
    if (uris.isNotEmpty()) {
      selectedMediaUris = uris.map { it.toString() }
    }
  }

  val singleMediaPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      selectedMediaUris = listOf(uri.toString())
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = StarCardBackground,
    titleContentColor = StarPrimaryText,
    title = { Text("Upload Creator Content", style = MaterialTheme.typography.titleMedium) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf("reel" to "Reel", "video" to "Video", "photo" to "Photo(s)").forEach { (key, label) ->
            FilterChip(
              selected = postType == key,
              onClick = {
                postType = key
                durationInput = if (key == "photo") "0" else "45"
              },
              label = { Text(label) },
            )
          }
        }
        OutlinedButton(
          onClick = {
            if (postType == "photo") {
              multiMediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            } else {
              singleMediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
            }
          },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            if (selectedMediaUris.isEmpty()) {
              if (postType == "photo") "Select 1–10 Photos (Android Photo Picker)" else "Select Video File (Photo Picker)"
            } else {
              "Selected ${selectedMediaUris.size} media item(s)"
            }
          )
        }
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Title / Caption Header") },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("upload_title_input"),
        )
        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Description / Photo Caption") },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("upload_description_input"),
        )
        OutlinedTextField(
          value = hashtags,
          onValueChange = { hashtags = it },
          label = { Text("Hashtags") },
          modifier = Modifier.fillMaxWidth(),
        )
        if (postType != "photo") {
          OutlinedTextField(
            value = durationInput,
            onValueChange = { durationInput = it.filter { ch -> ch.isDigit() } },
            label = { Text("Duration (seconds)") },
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }
    },
    confirmButton = {
      SingleTapButton(
        text = "Publish Now",
        enabled = title.isNotBlank(),
        onClick = {
          val mediaCount = selectedMediaUris.size.coerceAtLeast(1).toLong()
          val uri = selectedMediaUris.firstOrNull() ?: "local://${postType}/${System.currentTimeMillis()}"
          val duration = if (postType == "photo") 0L else (durationInput.toLongOrNull() ?: 30L)
          onSubmit(postType, title, description, hashtags, mediaCount, uri, duration)
        },
        testTag = "confirm_publish_post_button",
      )
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = StarSecondaryText)
      }
    },
  )
}

@Composable
fun PostCommentsDialog(
  post: MediaPost,
  comments: List<PostComment>,
  onDismiss: () -> Unit,
  onAddComment: (String) -> Unit,
) {
  var text by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = StarCardBackground,
    titleContentColor = StarPrimaryText,
    title = { Text("Comments on '${post.title}' (${comments.size})", style = MaterialTheme.typography.titleMedium) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (comments.isEmpty()) {
          Text("No comments yet. Start the conversation!", color = StarSecondaryText)
        } else {
          comments.takeLast(6).forEach { c ->
            Surface(
              color = StarCardElevated,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(modifier = Modifier.padding(8.dp)) {
                Text("@${c.authorName}", style = MaterialTheme.typography.labelSmall, color = ElectricBlue, fontWeight = FontWeight.Bold)
                Text(c.text, style = MaterialTheme.typography.bodyMedium, color = StarPrimaryText)
              }
            }
          }
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("Write a comment...") },
            modifier = Modifier
              .weight(1f)
              .testTag("comment_text_input"),
          )
          IconButton(
            onClick = {
              if (text.isNotBlank()) {
                onAddComment(text)
                text = ""
              }
            }
          ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Comment", tint = ElectricBlue)
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Close", color = ElectricBlue)
      }
    },
  )
}
