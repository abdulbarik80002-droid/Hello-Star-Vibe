package com.example.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val username: String = "",
  val displayName: String = "",
  val bio: String = "",
  val avatarUrl: String = "",
  val vipLevel: Long = 1L,
  val vipXp: Long = 0L,
  val diamondBalance: Long = 0L,
  val followersCount: Long = 0L,
  val followingCount: Long = 0L,
  val eligibleVideoViews: Long = 0L,
  val eligibleReelViews: Long = 0L,
  val eligiblePhotoViews: Long = 0L,
  val invalidViewsCount: Long = 0L,
  val pendingEarningsPaise: Long = 0L,
  val approvedEarningsPaise: Long = 0L,
  val paidEarningsPaise: Long = 0L,
  val accountStatus: String = "active",
  val statusReason: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class MediaPost(
  @DocumentId val id: String = "",
  val authorId: String = "",
  val authorName: String = "",
  val postType: String = "reel",
  val title: String = "",
  val description: String = "",
  val hashtags: String = "",
  val mediaCount: Long = 1L,
  val mediaUri: String = "",
  val thumbnailTitle: String = "",
  val durationSeconds: Long = 0L,
  val viewsCount: Long = 0L,
  val likesCount: Long = 0L,
  val commentsCount: Long = 0L,
  val sharesCount: Long = 0L,
  val visibility: String = "public",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class PostViewRecord(
  @DocumentId val id: String = "",
  val postId: String = "",
  val viewerId: String = "",
  val postType: String = "reel",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class PostComment(
  @DocumentId val id: String = "",
  val postId: String = "",
  val authorId: String = "",
  val authorName: String = "",
  val text: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class UserFollow(
  @DocumentId val id: String = "",
  val followerId: String = "",
  val targetUserId: String = "",
  val targetName: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class LiveStream(
  @DocumentId val id: String = "",
  val hostId: String = "",
  val hostName: String = "",
  val title: String = "",
  val streamCategory: String = "General",
  val status: String = "live",
  val viewerCount: Long = 1L,
  val isPkBattle: Boolean = false,
  val opponentId: String = "",
  val opponentName: String = "",
  val hostPkScore: Long = 0L,
  val opponentPkScore: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class LiveChatMessage(
  @DocumentId val id: String = "",
  val streamId: String = "",
  val senderId: String = "",
  val senderName: String = "",
  val messageText: String = "",
  val isGiftAlert: Boolean = false,
  val giftAmountDiamonds: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class WalletTransaction(
  @DocumentId val id: String = "",
  val userId: String = "",
  val txType: String = "diamond_purchase",
  val packageId: String = "",
  val diamondsDelta: Long = 0L,
  val amountInrPaise: Long = 0L,
  val recipientId: String = "",
  val streamId: String = "",
  val verificationStatus: String = "pending_backend_verification",
  val paymentRef: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class BankAccount(
  @DocumentId val id: String = "",
  val userId: String = "",
  val accountHolderName: String = "",
  val bankName: String = "",
  val accountNumberMasked: String = "",
  val ifscCode: String = "",
  val verificationStatus: String = "pending",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class WithdrawalRequest(
  @DocumentId val id: String = "",
  val userId: String = "",
  val bankAccountId: String = "",
  val amountInrPaise: Long = 1000000L,
  val userVipLevelAtRequest: Long = 30L,
  val status: String = "pending_review",
  val scheduledPayoutDay: Long = 18L,
  val adminNote: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class ModerationReport(
  @DocumentId val id: String = "",
  val reporterId: String = "",
  val targetType: String = "reel",
  val targetId: String = "",
  val reasonCategory: String = "explicit_content",
  val details: String = "",
  val status: String = "open",
  val moderationAction: String = "",
  val appealText: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class SupportTicket(
  @DocumentId val id: String = "",
  val userId: String = "",
  val category: String = "other_issues",
  val subject: String = "",
  val message: String = "",
  val status: String = "open",
  val adminReply: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class UserNotification(
  @DocumentId val id: String = "",
  val userId: String = "",
  val title: String = "",
  val body: String = "",
  val notifType: String = "system",
  val isRead: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
)

object CreatorRewardCalculator {
  // Proposed creator reward rate:
  // 1,000 eligible views = ₹0.80 (80 paise) -> 0.08 paise per view (80 paise per 1,000 views)
  // 10,000 eligible views = ₹8 (800 paise)
  // 100,000 eligible views = ₹80 (8,000 paise)
  // 500,000 eligible views = ₹400 (40,000 paise)
  // 1,000,000 eligible views = ₹800 (80,000 paise)
  // 10,000,000 eligible views = ₹8,000 (800,000 paise)
  fun calculateProposedPaiseForViews(eligibleViews: Long): Long {
    if (eligibleViews <= 0L) return 0L
    return (eligibleViews * 80L) / 1000L
  }

  fun formatInrFromPaise(paise: Long): String {
    val rupees = paise / 100.0
    return String.format(java.util.Locale.US, "₹%,.2f", rupees)
  }

  fun xpForNextVipLevel(currentLevel: Long): Long {
    return currentLevel.coerceAtLeast(1L) * 500L
  }

  fun vipTierName(level: Long): String = when {
    level >= 40L -> "Cosmic Sovereign Star"
    level >= 30L -> "Monetized Supernova (Creator Eligible)"
    level >= 20L -> "Electric Pulsar VIP"
    level >= 10L -> "Neon Nebula VIP"
    else -> "Rising Starlight VIP"
  }
}
