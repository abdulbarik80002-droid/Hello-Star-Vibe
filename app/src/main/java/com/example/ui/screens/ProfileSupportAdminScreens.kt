package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.BankAccount
import com.example.data.CreatorRewardCalculator
import com.example.data.MediaPost
import com.example.data.ModerationReport
import com.example.data.SupportTicket
import com.example.data.UserProfile
import com.example.data.WalletTransaction
import com.example.data.WithdrawalRequest
import com.example.ui.components.ReportContentDialog
import com.example.ui.components.SingleTapButton
import com.example.ui.theme.AccentPink
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.StarCardBackground
import com.example.ui.theme.StarCardElevated
import com.example.ui.theme.StarErrorRed
import com.example.ui.theme.StarOutlineBorder
import com.example.ui.theme.StarPrimaryText
import com.example.ui.theme.StarSecondaryText
import com.example.ui.theme.StarSuccessGreen
import com.example.ui.theme.StarWarningGold

@Composable
fun UserProfileScreen(
  profile: UserProfile?,
  userEmail: String,
  myPosts: List<MediaPost>,
  myReports: List<ModerationReport>,
  onUpdateProfile: (username: String, displayName: String, bio: String) -> Unit,
  onSubmitAppeal: (reportId: String, appealText: String) -> Unit,
  onReportProfile: (reason: String, details: String) -> Unit,
  onSignOut: () -> Unit,
) {
  var username by remember(profile?.username) { mutableStateOf(profile?.username ?: "") }
  var displayName by remember(profile?.displayName) { mutableStateOf(profile?.displayName ?: "") }
  var bio by remember(profile?.bio) { mutableStateOf(profile?.bio ?: "") }
  var appealText by remember { mutableStateOf("") }
  var showReportSelfDialog by remember { mutableStateOf(false) }

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
          Text("Creator Profile & Session", style = MaterialTheme.typography.headlineMedium)
          Text(userEmail, style = MaterialTheme.typography.bodyMedium, color = ElectricBlue)
        }
        SingleTapButton(
          text = "Logout",
          onClick = onSignOut,
          containerColor = StarErrorRed,
          testTag = "logout_button",
        )
      }
    }

    // Account Moderation Status Card
    item {
      val status = profile?.accountStatus ?: "active"
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = StarCardBackground,
        border = BorderStroke(
          1.dp,
          if (status == "active") StarSuccessGreen else StarErrorRed
        ),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "Account Status: ${status.uppercase()} • VIP Level ${profile?.vipLevel ?: 1L}",
            style = MaterialTheme.typography.titleSmall,
            color = if (status == "active") StarSuccessGreen else StarErrorRed,
          )
          if (!profile?.statusReason.isNullOrBlank()) {
            Text("Moderation Reason: ${profile?.statusReason}", color = StarWarningGold)
          }
          Text(
            text = "Followers: ${profile?.followersCount ?: 0L} • Following: ${profile?.followingCount ?: 0L} • Posts: ${myPosts.size}",
            style = MaterialTheme.typography.bodyMedium,
            color = StarSecondaryText,
          )
        }
      }
    }

    // Profile Editor
    item {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = StarCardBackground,
        border = BorderStroke(1.dp, StarOutlineBorder),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Edit Public Creator Profile", style = MaterialTheme.typography.titleSmall)
          OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username Handle (3-30 chars)") },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("profile_username_input"),
          )
          OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text("Display Name") },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("profile_display_name_input"),
          )
          OutlinedTextField(
            value = bio,
            onValueChange = { bio = it },
            label = { Text("Creator Bio") },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("profile_bio_input"),
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SingleTapButton(
              text = "Save Profile",
              onClick = { onUpdateProfile(username, displayName, bio) },
              containerColor = NeonPurple,
              testTag = "save_profile_button",
            )
            OutlinedButton(onClick = { showReportSelfDialog = true }) {
              Text("Report Profile Issue")
            }
          }
        }
      }
    }

    // Moderation Reports & Appeals Section
    if (myReports.isNotEmpty()) {
      item {
        Text("Submitted Reports & Appeals (${myReports.size})", style = MaterialTheme.typography.titleSmall)
      }
      items(myReports, key = { it.id }) { report ->
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          color = StarCardBackground,
          border = BorderStroke(1.dp, StarOutlineBorder),
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              "Target: ${report.targetType} (${report.reasonCategory}) • Status: ${report.status.uppercase()}",
              style = MaterialTheme.typography.titleSmall,
              color = ElectricBlue,
            )
            Text(report.details, style = MaterialTheme.typography.bodyMedium)
            if (report.moderationAction.isNotBlank()) {
              Text("Admin Action: ${report.moderationAction}", color = StarWarningGold)
            }
            if (report.status != "appealed") {
              OutlinedTextField(
                value = appealText,
                onValueChange = { appealText = it },
                label = { Text("Submit Appeal Statement") },
                modifier = Modifier.fillMaxWidth(),
              )
              SingleTapButton(
                text = "Submit Appeal",
                enabled = appealText.isNotBlank(),
                onClick = {
                  onSubmitAppeal(report.id, appealText)
                  appealText = ""
                },
                containerColor = AccentPink,
              )
            }
          }
        }
      }
    }
  }

  if (showReportSelfDialog) {
    ReportContentDialog(
      targetType = "user_profile",
      targetId = profile?.userId ?: "profile",
      targetLabel = "User Profile",
      onDismiss = { showReportSelfDialog = false },
      onSubmitReport = { reason, details ->
        onReportProfile(reason, details)
        showReportSelfDialog = false
      },
    )
  }
}

@Composable
fun SupportCenterScreen(
  tickets: List<SupportTicket>,
  onCreateTicket: (category: String, subject: String, message: String) -> Unit,
) {
  val categories = remember {
    listOf(
      "login_problems" to "Login problems",
      "video_upload_problems" to "Video upload problems",
      "live_streaming_problems" to "Live streaming problems",
      "diamond_payment_problems" to "Diamond and payment problems",
      "earnings_withdrawal" to "Earnings and withdrawal",
      "account_bans_appeals" to "Account bans and appeals",
      "other_issues" to "Other issues",
    )
  }
  var selectedCategory by remember { mutableStateOf(categories.first().first) }
  var subject by remember { mutableStateOf("") }
  var message by remember { mutableStateOf("") }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      Text("Customer Support & Help Center", style = MaterialTheme.typography.headlineMedium)
      Text(
        "Submit a support ticket across official complaint categories and track admin responses.",
        style = MaterialTheme.typography.bodyMedium,
        color = StarSecondaryText,
      )
    }

    item {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = StarCardBackground,
        border = BorderStroke(1.dp, StarOutlineBorder),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Select Complaint Category", style = MaterialTheme.typography.titleSmall, color = ElectricBlue)
          categories.forEach { (key, label) ->
            FilterChip(
              selected = selectedCategory == key,
              onClick = { selectedCategory = key },
              label = { Text(label) },
              modifier = Modifier.fillMaxWidth(),
            )
          }
          OutlinedTextField(
            value = subject,
            onValueChange = { subject = it },
            label = { Text("Ticket Subject (min 3 chars)") },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("support_subject_input"),
          )
          OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            label = { Text("Describe your issue in detail (min 5 chars)") },
            minLines = 3,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("support_message_input"),
          )
          SingleTapButton(
            text = "Submit Support Ticket",
            enabled = subject.trim().length >= 3 && message.trim().length >= 5,
            onClick = {
              onCreateTicket(selectedCategory, subject, message)
              subject = ""
              message = ""
            },
            containerColor = NeonPurple,
            testTag = "submit_support_ticket_button",
          )
        }
      }
    }

    item {
      Text("Your Support Ticket History (${tickets.size})", style = MaterialTheme.typography.titleMedium)
    }

    if (tickets.isEmpty()) {
      item {
        Text("No support tickets submitted yet.", color = StarSecondaryText)
      }
    } else {
      items(tickets, key = { it.id }) { ticket ->
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          color = StarCardBackground,
          border = BorderStroke(1.dp, StarOutlineBorder),
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text(ticket.subject, style = MaterialTheme.typography.titleSmall, color = StarPrimaryText)
              Text(ticket.status.uppercase(), style = MaterialTheme.typography.labelSmall, color = ElectricBlue)
            }
            Text("Category: ${ticket.category}", style = MaterialTheme.typography.labelSmall, color = AccentPink)
            Text(ticket.message, style = MaterialTheme.typography.bodyMedium, color = StarSecondaryText)
            if (ticket.adminReply.isNotBlank()) {
              Surface(
                color = StarCardElevated,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Text(
                  text = "Admin Reply: ${ticket.adminReply}",
                  style = MaterialTheme.typography.bodyMedium,
                  color = StarSuccessGreen,
                  modifier = Modifier.padding(10.dp),
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun AdminDashboardScreen(
  isAuthorizedAdmin: Boolean,
  users: List<UserProfile>,
  posts: List<MediaPost>,
  reports: List<ModerationReport>,
  transactions: List<WalletTransaction>,
  bankAccounts: List<BankAccount>,
  withdrawals: List<WithdrawalRequest>,
  tickets: List<SupportTicket>,
  onModerateUser: (userId: String, newStatus: String, reason: String) -> Unit,
  onSetVipAndEarnings: (userId: String, vipLevel: Long, approvedPaise: Long) -> Unit,
  onRemovePost: (postId: String) -> Unit,
  onResolveReport: (reportId: String, status: String, actionNote: String) -> Unit,
  onVerifyDiamondPurchase: (WalletTransaction) -> Unit,
  onVerifyBankAccount: (accountId: String, status: String) -> Unit,
  onProcessWithdrawal: (WithdrawalRequest, newStatus: String, note: String) -> Unit,
  onReplyTicket: (ticketId: String, newStatus: String, reply: String) -> Unit,
) {
  if (!isAuthorizedAdmin) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Text("403 • Administrator Access Required", style = MaterialTheme.typography.headlineMedium, color = StarErrorRed)
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        "Ordinary users cannot access admin actions. All admin mutations are enforced server-side in Firestore Security Rules via isAdmin().",
        style = MaterialTheme.typography.bodyMedium,
        color = StarSecondaryText,
      )
    }
    return
  }

  var activeTab by remember { mutableStateOf("stats") }
  var moderationReason by remember { mutableStateOf("Violation of Community Guidelines") }
  var adminReplyText by remember { mutableStateOf("Issue investigated and resolved by HELLO STAR VIBE Support.") }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      Text("Administrator Security & Moderation Dashboard", style = MaterialTheme.typography.headlineMedium)
      Text(
        "Server-verified role: isAdmin() active • Audit log & RBAC enforced",
        style = MaterialTheme.typography.labelSmall,
        color = StarSuccessGreen,
      )
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        listOf(
          "stats" to "Stats",
          "users" to "Users (${users.size})",
          "reports" to "Moderation (${reports.size})",
          "finance" to "Finance & Payouts",
          "tickets" to "Support (${tickets.size})",
        ).forEach { (key, label) ->
          FilterChip(
            selected = activeTab == key,
            onClick = { activeTab = key },
            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
          )
        }
      }
    }

    when (activeTab) {
      "stats" -> {
        item {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = StarCardBackground,
            border = BorderStroke(1.dp, ElectricBlue),
          ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text("Platform Statistics", style = MaterialTheme.typography.titleMedium, color = ElectricBlue)
              Text("• Total Registered Creators: ${users.size}")
              Text("• Public Videos / Reels / Photos: ${posts.size}")
              Text("• Open Moderation Reports & Appeals: ${reports.count { it.status == "open" || it.status == "appealed" }}")
              Text("• Pending Diamond Purchase Orders: ${transactions.count { it.verificationStatus == "pending_backend_verification" }}")
              Text("• Pending Bank Verifications: ${bankAccounts.count { it.verificationStatus == "pending" }}")
              Text("• Pending Withdrawal Requests: ${withdrawals.count { it.status == "pending_review" }}")
              Text("• Open Customer Support Tickets: ${tickets.count { it.status == "open" }}")
            }
          }
        }
      }

      "users" -> {
        item {
          OutlinedTextField(
            value = moderationReason,
            onValueChange = { moderationReason = it },
            label = { Text("Mandatory Audit Log Reason for Account Action") },
            modifier = Modifier.fillMaxWidth(),
          )
        }
        items(users, key = { it.id }) { u ->
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = StarCardBackground,
            border = BorderStroke(1.dp, StarOutlineBorder),
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text("@${u.username} (${u.displayName}) • Status: ${u.accountStatus.uppercase()}", style = MaterialTheme.typography.titleSmall)
              Text(
                "VIP Level: ${u.vipLevel} • Pending: ${CreatorRewardCalculator.formatInrFromPaise(u.pendingEarningsPaise)} • Approved: ${CreatorRewardCalculator.formatInrFromPaise(u.approvedEarningsPaise)}",
                style = MaterialTheme.typography.bodyMedium,
                color = StarSecondaryText,
              )
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SingleTapButton(
                  text = "Warn",
                  onClick = { onModerateUser(u.userId, "warned", moderationReason) },
                  containerColor = StarWarningGold,
                )
                SingleTapButton(
                  text = "Suspend",
                  onClick = { onModerateUser(u.userId, "suspended", moderationReason) },
                  containerColor = AccentPink,
                )
                SingleTapButton(
                  text = "Ban",
                  onClick = { onModerateUser(u.userId, "banned", moderationReason) },
                  containerColor = StarErrorRed,
                )
                SingleTapButton(
                  text = "Restore",
                  onClick = { onModerateUser(u.userId, "active", "Restored after admin review") },
                  containerColor = StarSuccessGreen,
                )
              }
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                  onClick = { onSetVipAndEarnings(u.userId, 30L, u.pendingEarningsPaise.coerceAtLeast(1000000L)) }
                ) {
                  Text("Approve VIP 30 & ₹10k+ Earnings", style = MaterialTheme.typography.labelSmall)
                }
              }
            }
          }
        }
      }

      "reports" -> {
        items(reports, key = { it.id }) { r ->
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = StarCardBackground,
            border = BorderStroke(1.dp, StarOutlineBorder),
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text("Report [${r.status.uppercase()}] • ${r.targetType} (${r.reasonCategory})", style = MaterialTheme.typography.titleSmall, color = AccentPink)
              Text("Details: ${r.details}", style = MaterialTheme.typography.bodyMedium)
              if (r.appealText.isNotBlank()) {
                Text("Creator Appeal: ${r.appealText}", style = MaterialTheme.typography.bodyMedium, color = ElectricBlue)
              }
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SingleTapButton(
                  text = "Action & Log",
                  onClick = { onResolveReport(r.id, "actioned", "Removed violating content: $moderationReason") },
                  containerColor = StarErrorRed,
                )
                SingleTapButton(
                  text = "Dismiss",
                  onClick = { onResolveReport(r.id, "dismissed", "Reviewed; no violation found") },
                  containerColor = StarSuccessGreen,
                )
                if (r.targetType in listOf("reel", "video", "photo")) {
                  OutlinedButton(onClick = { onRemovePost(r.targetId) }) {
                    Text("Remove Post")
                  }
                }
              }
            }
          }
        }
      }

      "finance" -> {
        item {
          Text("Pending Diamond Orders", style = MaterialTheme.typography.titleSmall, color = ElectricBlue)
        }
        items(transactions.filter { it.verificationStatus == "pending_backend_verification" }, key = { it.id }) { tx ->
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = StarCardBackground,
            border = BorderStroke(1.dp, StarOutlineBorder),
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text("${tx.packageId} (+${tx.diamondsDelta} Diamonds)", style = MaterialTheme.typography.titleSmall)
                Text("Ref: ${tx.paymentRef}", style = MaterialTheme.typography.bodyMedium, color = StarSecondaryText)
              }
              SingleTapButton(
                text = "Verify & Credit",
                onClick = { onVerifyDiamondPurchase(tx) },
                containerColor = StarSuccessGreen,
              )
            }
          }
        }

        item {
          Text("Bank Account Verification Queue", style = MaterialTheme.typography.titleSmall, color = ElectricBlue)
        }
        items(bankAccounts, key = { it.id }) { bank ->
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = StarCardBackground,
            border = BorderStroke(1.dp, StarOutlineBorder),
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text("${bank.bankName} • ${bank.accountNumberMasked}", style = MaterialTheme.typography.titleSmall)
                Text("${bank.accountHolderName} (${bank.ifscCode}) • ${bank.verificationStatus}", style = MaterialTheme.typography.bodyMedium, color = StarSecondaryText)
              }
              if (bank.verificationStatus != "verified") {
                SingleTapButton(
                  text = "Verify Bank",
                  onClick = { onVerifyBankAccount(bank.id, "verified") },
                  containerColor = StarSuccessGreen,
                )
              }
            }
          }
        }

        item {
          Text("Creator Withdrawal Requests", style = MaterialTheme.typography.titleSmall, color = ElectricBlue)
        }
        items(withdrawals, key = { it.id }) { wd ->
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = StarCardBackground,
            border = BorderStroke(1.dp, StarOutlineBorder),
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(
                "${CreatorRewardCalculator.formatInrFromPaise(wd.amountInrPaise)} • Status: ${wd.status.uppercase()}",
                style = MaterialTheme.typography.titleSmall,
              )
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SingleTapButton(
                  text = "Approve",
                  onClick = { onProcessWithdrawal(wd, "approved", "Approved for 18th payout cycle") },
                  containerColor = ElectricBlue,
                )
                SingleTapButton(
                  text = "Confirm Paid",
                  onClick = { onProcessWithdrawal(wd, "paid", "Confirmed settled via bank payout") },
                  containerColor = StarSuccessGreen,
                )
                SingleTapButton(
                  text = "Reject",
                  onClick = { onProcessWithdrawal(wd, "rejected", "Rejected by compliance review") },
                  containerColor = StarErrorRed,
                )
              }
            }
          }
        }
      }

      "tickets" -> {
        item {
          OutlinedTextField(
            value = adminReplyText,
            onValueChange = { adminReplyText = it },
            label = { Text("Official Admin Response Text") },
            modifier = Modifier.fillMaxWidth(),
          )
        }
        items(tickets, key = { it.id }) { t ->
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = StarCardBackground,
            border = BorderStroke(1.dp, StarOutlineBorder),
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text("${t.subject} [${t.status.uppercase()}]", style = MaterialTheme.typography.titleSmall)
              Text("Category: ${t.category} • ${t.message}", style = MaterialTheme.typography.bodyMedium, color = StarSecondaryText)
              SingleTapButton(
                text = "Send Reply & Resolve",
                onClick = { onReplyTicket(t.id, "resolved", adminReplyText) },
                containerColor = NeonPurple,
              )
            }
          }
        }
      }
    }
  }
}
