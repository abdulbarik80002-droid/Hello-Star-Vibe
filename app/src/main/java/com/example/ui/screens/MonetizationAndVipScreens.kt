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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.BankAccount
import com.example.data.CreatorRewardCalculator
import com.example.data.UserProfile
import com.example.data.WalletTransaction
import com.example.data.WithdrawalRequest
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

private data class ProposedDiamondTier(
  val id: String,
  val diamonds: Long,
  val proposedInrPaise: Long,
  val label: String,
)

@Composable
fun DiamondWalletScreen(
  profile: UserProfile?,
  transactions: List<WalletTransaction>,
  onCreatePurchaseOrder: (packageId: String, diamondsAmount: Long, amountInrPaise: Long, paymentRef: String) -> Unit,
) {
  var paymentReferenceInput by remember { mutableStateOf("") }
  val packages = remember {
    listOf(
      ProposedDiamondTier("pkg_100", 100L, 9900L, "100 Diamonds (Proposed ₹99)"),
      ProposedDiamondTier("pkg_500", 500L, 44900L, "500 Diamonds (Proposed ₹449)"),
      ProposedDiamondTier("pkg_1200", 1200L, 99900L, "1,200 Diamonds (Proposed ₹999)"),
    )
  }
  var selectedPackage by remember { mutableStateOf(packages.first()) }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      Text("Diamond Wallet & Payment Ledger", style = MaterialTheme.typography.headlineMedium)
    }

    item {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = StarCardBackground,
        border = BorderStroke(1.5.dp, ElectricBlue),
      ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Diamond, contentDescription = null, tint = ElectricBlue)
            Text("Verified Wallet Balance", style = MaterialTheme.typography.titleSmall, color = StarSecondaryText)
          }
          Text(
            text = "${profile?.diamondBalance ?: 0L} Diamonds",
            style = MaterialTheme.typography.displaySmall,
            color = StarPrimaryText,
          )
          Text(
            text = "Security Rule Enforced: Client-submitted diamond orders are recorded as 'pending_backend_verification'. Diamonds are credited only after backend / administrator verification.",
            style = MaterialTheme.typography.bodyMedium,
            color = StarSecondaryText,
          )
        }
      }
    }

    item {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = StarCardBackground,
        border = BorderStroke(1.dp, StarOutlineBorder),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Submit Diamond Purchase Order (Subject to Pricing & Razorpay Webhook Approval)",
            style = MaterialTheme.typography.titleSmall,
            color = StarPrimaryText,
          )
          Text(
            text = "Note: Diamond package prices below are proposed tiers awaiting final commercial approval. Secret Razorpay API keys are never stored in the client APK.",
            style = MaterialTheme.typography.bodyMedium,
            color = StarWarningGold,
          )
          packages.forEach { pkg ->
            FilterChip(
              selected = selectedPackage.id == pkg.id,
              onClick = { selectedPackage = pkg },
              label = { Text(pkg.label) },
              modifier = Modifier.fillMaxWidth(),
            )
          }
          OutlinedTextField(
            value = paymentReferenceInput,
            onValueChange = { paymentReferenceInput = it },
            label = { Text("Razorpay Order / UPI Reference ID (for backend verification)") },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("wallet_payment_ref_input"),
          )
          SingleTapButton(
            text = "Submit Order for Backend Verification",
            onClick = {
              onCreatePurchaseOrder(
                selectedPackage.id,
                selectedPackage.diamonds,
                selectedPackage.proposedInrPaise,
                paymentReferenceInput.ifBlank { "RZP_PENDING_${System.currentTimeMillis()}" },
              )
              paymentReferenceInput = ""
            },
            containerColor = NeonPurple,
            testTag = "submit_diamond_order_button",
          )
        }
      }
    }

    item {
      Text(
        text = "Auditable Wallet & Gift Transaction Ledger (${transactions.size})",
        style = MaterialTheme.typography.titleMedium,
        color = StarPrimaryText,
      )
    }

    if (transactions.isEmpty()) {
      item {
        Text("No wallet or gift transactions recorded yet.", color = StarSecondaryText)
      }
    } else {
      items(transactions, key = { it.id }) { tx ->
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          color = StarCardBackground,
          border = BorderStroke(1.dp, StarOutlineBorder),
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "${tx.txType.uppercase()} • ${tx.packageId}",
                style = MaterialTheme.typography.titleSmall,
                color = StarPrimaryText,
              )
              Text(
                text = "Ref: ${tx.paymentRef} • Status: ${tx.verificationStatus}",
                style = MaterialTheme.typography.bodyMedium,
                color = if (tx.verificationStatus == "verified") StarSuccessGreen else StarWarningGold,
              )
            }
            Text(
              text = (if (tx.diamondsDelta > 0) "+${tx.diamondsDelta}" else "${tx.diamondsDelta}") + " 💎",
              style = MaterialTheme.typography.titleMedium,
              color = if (tx.diamondsDelta > 0) ElectricBlue else AccentPink,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }
    }
  }
}

@Composable
fun CreatorEarningsScreen(
  profile: UserProfile?,
  onNavigateToWithdrawal: () -> Unit,
) {
  val videoViews = profile?.eligibleVideoViews ?: 0L
  val reelViews = profile?.eligibleReelViews ?: 0L
  val photoViews = profile?.eligiblePhotoViews ?: 0L
  val invalidViews = profile?.invalidViewsCount ?: 0L
  val totalEligible = videoViews + reelViews + photoViews

  val rateTable = remember {
    listOf(
      "1,000 eligible views" to "₹0.80",
      "10,000 eligible views" to "₹8.00",
      "100,000 eligible views" to "₹80.00",
      "500,000 eligible views" to "₹400.00",
      "1,000,000 eligible views" to "₹800.00",
      "10,000,000 eligible views" to "₹8,000.00",
    )
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      Text("Creator Monetization & Earnings", style = MaterialTheme.typography.headlineMedium)
      Text(
        "Separate eligible view tracking for Videos, Reels, and Photo Posts",
        style = MaterialTheme.typography.bodyMedium,
        color = StarSecondaryText,
      )
    }

    // Balances Breakdown
    item {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = StarCardBackground,
        border = BorderStroke(1.dp, NeonPurple),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
              Text("Pending Earnings", style = MaterialTheme.typography.labelSmall, color = StarSecondaryText)
              Text(
                CreatorRewardCalculator.formatInrFromPaise(profile?.pendingEarningsPaise ?: 0L),
                style = MaterialTheme.typography.titleLarge,
                color = StarWarningGold,
              )
            }
            Column {
              Text("Approved Earnings", style = MaterialTheme.typography.labelSmall, color = StarSecondaryText)
              Text(
                CreatorRewardCalculator.formatInrFromPaise(profile?.approvedEarningsPaise ?: 0L),
                style = MaterialTheme.typography.titleLarge,
                color = StarSuccessGreen,
              )
            }
            Column {
              Text("Paid Earnings", style = MaterialTheme.typography.labelSmall, color = StarSecondaryText)
              Text(
                CreatorRewardCalculator.formatInrFromPaise(profile?.paidEarningsPaise ?: 0L),
                style = MaterialTheme.typography.titleLarge,
                color = ElectricBlue,
              )
            }
          }
          SingleTapButton(
            text = "Manage Bank Account & Withdrawals",
            onClick = onNavigateToWithdrawal,
            containerColor = NeonPurple,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }
    }

    // View Breakdown Card
    item {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = StarCardBackground,
        border = BorderStroke(1.dp, StarOutlineBorder),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Validated View Metrics (Deduplicated per User)", style = MaterialTheme.typography.titleSmall, color = ElectricBlue)
          Text("• Eligible Long-Video Views: $videoViews", style = MaterialTheme.typography.bodyMedium)
          Text("• Eligible Vertical Reel Views: $reelViews", style = MaterialTheme.typography.bodyMedium)
          Text("• Eligible Photo Post Views: $photoViews", style = MaterialTheme.typography.bodyMedium)
          Text("• Total Eligible Views: $totalEligible", style = MaterialTheme.typography.titleSmall, color = StarSuccessGreen)
          Text("• Rejected / Duplicate Views Filtered Out: $invalidViews", style = MaterialTheme.typography.bodyMedium, color = AccentPink)
        }
      }
    }

    // Proposed Platform Rate Table & Eligibility Rules
    item {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = StarCardElevated,
        border = BorderStroke(1.dp, StarOutlineBorder),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Proposed Platform Creator Reward Rates", style = MaterialTheme.typography.titleSmall, color = StarPrimaryText)
          Text(
            "These are proposed HELLO STAR VIBE platform rates, not guaranteed earnings or the official rates of other platforms. Payouts require sufficient platform revenue and a configured RazorpayX payout integration.",
            style = MaterialTheme.typography.bodyMedium,
            color = StarSecondaryText,
          )
          rateTable.forEach { (viewsLabel, rewardLabel) ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(viewsLabel, style = MaterialTheme.typography.bodyMedium, color = StarPrimaryText)
              Text(rewardLabel, style = MaterialTheme.typography.titleSmall, color = ElectricBlue)
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text("Creator Eligibility Requirements:", style = MaterialTheme.typography.titleSmall, color = AccentPink)
          Text("1. Account must reach VIP Level 30 (Current: Level ${profile?.vipLevel ?: 1L})", style = MaterialTheme.typography.bodyMedium)
          Text("2. Minimum payout threshold: ₹10,000.00", style = MaterialTheme.typography.bodyMedium)
          Text("3. Proposed monthly payout date: the 18th of each month", style = MaterialTheme.typography.bodyMedium)
          Text("4. Verified bank account destination required", style = MaterialTheme.typography.bodyMedium)
        }
      }
    }
  }
}

@Composable
fun WithdrawalAndBankScreen(
  profile: UserProfile?,
  bankAccounts: List<BankAccount>,
  withdrawals: List<WithdrawalRequest>,
  onAddBankAccount: (holderName: String, bankName: String, accountNumber: String, ifscCode: String) -> Unit,
  onSubmitWithdrawal: (bankAccountId: String, amountInrPaise: Long) -> Unit,
) {
  var holderName by remember { mutableStateOf("") }
  var bankName by remember { mutableStateOf("") }
  var accountNumber by remember { mutableStateOf("") }
  var ifscCode by remember { mutableStateOf("") }
  var withdrawalRupeesInput by remember { mutableStateOf("10000") }

  val verifiedBanks = remember(bankAccounts) {
    bankAccounts.filter { it.verificationStatus == "verified" }
  }
  val vipLevel = profile?.vipLevel ?: 1L
  val approvedPaise = profile?.approvedEarningsPaise ?: 0L
  val isLevelEligible = vipLevel >= 30L
  val isBalanceEligible = approvedPaise >= 1000000L

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      Text("Withdrawals & Verified Bank Accounts", style = MaterialTheme.typography.headlineMedium)
    }

    // Eligibility Check Card
    item {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = StarCardBackground,
        border = BorderStroke(1.dp, if (isLevelEligible && isBalanceEligible && verifiedBanks.isNotEmpty()) StarSuccessGreen else StarWarningGold),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("Backend-Enforced Withdrawal Eligibility", style = MaterialTheme.typography.titleSmall, color = ElectricBlue)
          Text(
            text = "${if (isLevelEligible) "✅" else "⏳"} VIP Level 30+ Required (Current: Level $vipLevel)",
            style = MaterialTheme.typography.bodyMedium,
            color = if (isLevelEligible) StarSuccessGreen else StarWarningGold,
          )
          Text(
            text = "${if (isBalanceEligible) "✅" else "⏳"} Minimum Approved Balance ₹10,000.00 (Current: ${CreatorRewardCalculator.formatInrFromPaise(approvedPaise)})",
            style = MaterialTheme.typography.bodyMedium,
            color = if (isBalanceEligible) StarSuccessGreen else StarWarningGold,
          )
          Text(
            text = "${if (verifiedBanks.isNotEmpty()) "✅" else "⏳"} Verified Bank Destination (${verifiedBanks.size} verified)",
            style = MaterialTheme.typography.bodyMedium,
            color = if (verifiedBanks.isNotEmpty()) StarSuccessGreen else StarWarningGold,
          )
          Text(
            text = "Scheduled Monthly Payout Date: 18th • Withdrawals are never marked 'paid' until confirmed by the payment provider or administrator.",
            style = MaterialTheme.typography.labelSmall,
            color = StarSecondaryText,
          )
        }
      }
    }

    // Add Bank Account Form
    item {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = StarCardBackground,
        border = BorderStroke(1.dp, StarOutlineBorder),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Add Creator Bank Account", style = MaterialTheme.typography.titleSmall)
          OutlinedTextField(
            value = holderName,
            onValueChange = { holderName = it },
            label = { Text("Account Holder Full Name") },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("bank_holder_input"),
          )
          OutlinedTextField(
            value = bankName,
            onValueChange = { bankName = it },
            label = { Text("Bank Name") },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("bank_name_input"),
          )
          OutlinedTextField(
            value = accountNumber,
            onValueChange = { accountNumber = it },
            label = { Text("Account Number") },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("bank_account_number_input"),
          )
          OutlinedTextField(
            value = ifscCode,
            onValueChange = { ifscCode = it },
            label = { Text("IFSC Code") },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("bank_ifsc_input"),
          )
          SingleTapButton(
            text = "Submit Bank Account for Verification",
            enabled = holderName.length >= 2 && bankName.length >= 2 && accountNumber.length >= 4 && ifscCode.length >= 4,
            onClick = {
              onAddBankAccount(holderName, bankName, accountNumber, ifscCode)
              holderName = ""
              bankName = ""
              accountNumber = ""
              ifscCode = ""
            },
            containerColor = NeonPurple,
            testTag = "submit_bank_account_button",
          )
        }
      }
    }

    // Saved Bank Accounts
    if (bankAccounts.isNotEmpty()) {
      item {
        Text("Your Bank Accounts (${bankAccounts.size})", style = MaterialTheme.typography.titleSmall)
      }
      items(bankAccounts, key = { it.id }) { bank ->
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          color = StarCardBackground,
          border = BorderStroke(1.dp, StarOutlineBorder),
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column {
              Text("${bank.bankName} (${bank.accountNumberMasked})", style = MaterialTheme.typography.titleSmall)
              Text("Holder: ${bank.accountHolderName} • IFSC: ${bank.ifscCode}", style = MaterialTheme.typography.bodyMedium, color = StarSecondaryText)
            }
            Text(
              text = bank.verificationStatus.uppercase(),
              style = MaterialTheme.typography.labelLarge,
              color = if (bank.verificationStatus == "verified") StarSuccessGreen else StarWarningGold,
            )
          }
        }
      }
    }

    // Request Withdrawal Form
    item {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = StarCardBackground,
        border = BorderStroke(1.dp, StarOutlineBorder),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Request Monthly Payout (18th Cycle)", style = MaterialTheme.typography.titleSmall)
          OutlinedTextField(
            value = withdrawalRupeesInput,
            onValueChange = { withdrawalRupeesInput = it.filter { ch -> ch.isDigit() } },
            label = { Text("Withdrawal Amount in INR (Minimum ₹10,000)") },
            modifier = Modifier.fillMaxWidth(),
          )
          val requestedPaise = (withdrawalRupeesInput.toLongOrNull() ?: 0L) * 100L
          val targetBankId = verifiedBanks.firstOrNull()?.id ?: ""
          SingleTapButton(
            text = "Submit Withdrawal Request",
            enabled = isLevelEligible && verifiedBanks.isNotEmpty() && requestedPaise >= 1000000L && approvedPaise >= requestedPaise,
            onClick = {
              onSubmitWithdrawal(targetBankId, requestedPaise)
            },
            containerColor = StarSuccessGreen,
          )
        }
      }
    }

    // Withdrawal History
    item {
      Text("Withdrawal & Payout History (${withdrawals.size})", style = MaterialTheme.typography.titleSmall)
    }
    items(withdrawals, key = { it.id }) { wd ->
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = StarCardBackground,
        border = BorderStroke(1.dp, StarOutlineBorder),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
              text = CreatorRewardCalculator.formatInrFromPaise(wd.amountInrPaise),
              style = MaterialTheme.typography.titleMedium,
              color = ElectricBlue,
            )
            Text(
              text = wd.status.uppercase(),
              style = MaterialTheme.typography.labelLarge,
              color = if (wd.status == "paid") StarSuccessGreen else StarWarningGold,
            )
          }
          Text(
            text = "VIP Level at Request: ${wd.userVipLevelAtRequest} • Scheduled Day: ${wd.scheduledPayoutDay}th",
            style = MaterialTheme.typography.bodyMedium,
            color = StarSecondaryText,
          )
          if (wd.adminNote.isNotBlank()) {
            Text("Admin Note: ${wd.adminNote}", style = MaterialTheme.typography.labelSmall, color = AccentPink)
          }
        }
      }
    }
  }
}

@Composable
fun VipLevelsScreen(profile: UserProfile?) {
  val currentLevel = profile?.vipLevel ?: 1L
  val currentXp = profile?.vipXp ?: 0L
  val nextXp = CreatorRewardCalculator.xpForNextVipLevel(currentLevel)
  val progress = (currentXp.toFloat() / nextXp.toFloat()).coerceIn(0f, 1f)

  val milestones = remember {
    listOf(
      Triple(1L, "Rising Starlight VIP", "Neon profile badge & live chat participation"),
      Triple(10L, "Neon Nebula VIP", "Highlighted comments in Reels & priority live chat badge"),
      Triple(20L, "Electric Pulsar VIP", "Two-Person PK Battle host badge & custom room banner"),
      Triple(30L, "Monetized Supernova (Creator Eligible)", "Unlocks Creator Earnings Withdrawal eligibility (subject to ₹10,000 minimum threshold & verification)"),
      Triple(40L, "Cosmic Sovereign Star", "Top spotlight placement & sovereign neon star frame"),
    )
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      Text("VIP Progression System", style = MaterialTheme.typography.headlineMedium)
      Text(
        "Earn VIP XP by publishing Reels/Videos/Photos and sending verified gifts",
        style = MaterialTheme.typography.bodyMedium,
        color = StarSecondaryText,
      )
    }

    item {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = StarCardBackground,
        border = BorderStroke(1.5.dp, NeonPurple),
      ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Star, contentDescription = null, tint = StarWarningGold)
            Text(
              text = "Current Level: VIP $currentLevel — ${CreatorRewardCalculator.vipTierName(currentLevel)}",
              style = MaterialTheme.typography.titleMedium,
              color = StarPrimaryText,
            )
          }
          LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
              .fillMaxWidth()
              .height(10.dp)
              .clip(RoundedCornerShape(50)),
            color = NeonPurple,
            trackColor = StarCardElevated,
          )
          Text(
            text = "Progress to Level ${(currentLevel + 1).coerceAtMost(50L)}: $currentXp / $nextXp XP",
            style = MaterialTheme.typography.bodyMedium,
            color = ElectricBlue,
          )
        }
      }
    }

    item {
      Text("Approved VIP Level Milestones & Benefits", style = MaterialTheme.typography.titleMedium)
    }

    items(milestones, key = { it.first }) { (lvl, title, benefit) ->
      val unlocked = currentLevel >= lvl
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (unlocked) StarCardElevated else StarCardBackground,
        border = BorderStroke(1.dp, if (unlocked) StarSuccessGreen else StarOutlineBorder),
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = "Level $lvl • $title",
              style = MaterialTheme.typography.titleSmall,
              color = if (unlocked) ElectricBlue else StarPrimaryText,
            )
            Text(benefit, style = MaterialTheme.typography.bodyMedium, color = StarSecondaryText)
          }
          Text(
            text = if (unlocked) "UNLOCKED" else "LOCKED",
            style = MaterialTheme.typography.labelSmall,
            color = if (unlocked) StarSuccessGreen else StarSecondaryText,
          )
        }
      }
    }
  }
}
