package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.LiveChatMessage
import com.example.data.LiveStream
import com.example.data.UserProfile
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
import com.example.ui.theme.StarWarningGold

@Composable
fun LiveStreamingScreen(
  currentUserId: String,
  profile: UserProfile?,
  activeStreams: List<LiveStream>,
  selectedStream: LiveStream?,
  streamMessages: List<LiveChatMessage>,
  onSelectStream: (LiveStream?) -> Unit,
  onStartBroadcast: (title: String, category: String, isPkBattle: Boolean, opponentName: String) -> Unit,
  onEndBroadcast: (String) -> Unit,
  onSendChat: (streamId: String, text: String) -> Unit,
  onSendVerifiedGift: (stream: LiveStream, giftName: String, diamondCost: Long, supportHostSide: Boolean) -> Unit,
  onFollowHost: (String, String) -> Unit,
  onReport: (targetType: String, targetId: String, reason: String, details: String) -> Unit,
) {
  var showStartModal by remember { mutableStateOf(false) }
  var chatInput by remember { mutableStateOf("") }
  var supportHostInPk by remember { mutableStateOf(true) }
  var reportTarget by remember { mutableStateOf<Pair<String, String>?>(null) }

  val currentStream = remember(activeStreams, selectedStream) {
    if (selectedStream == null) null
    else activeStreams.find { it.id == selectedStream.id } ?: selectedStream
  }

  if (currentStream != null) {
    // Active Live Room / Two-Person PK Battle Stage + Real-time Firestore Chat
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedButton(onClick = { onSelectStream(null) }) {
          Text("← All Broadcasts")
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          OutlinedButton(onClick = { onFollowHost(currentStream.hostId, currentStream.hostName) }) {
            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Follow Host")
          }
          IconButton(onClick = { reportTarget = "live_stream" to currentStream.id }) {
            Icon(Icons.Default.Flag, contentDescription = "Report Stream", tint = AccentPink)
          }
          if (currentStream.hostId == currentUserId) {
            IconButton(onClick = { onEndBroadcast(currentStream.id) }) {
              Icon(Icons.Default.StopCircle, contentDescription = "End Stream", tint = StarErrorRed)
            }
          }
        }
      }

      // Live Broadcast or Two-Person PK Split Stage
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = StarCardBackground,
        border = BorderStroke(
          1.5.dp,
          Brush.horizontalGradient(listOf(NeonPurple, ElectricBlue, AccentPink))
        ),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = if (currentStream.isPkBattle) "TWO-PERSON PK BATTLE" else "LIVE BROADCAST",
              style = MaterialTheme.typography.labelLarge,
              color = if (currentStream.isPkBattle) AccentPink else ElectricBlue,
            )
            Text(
              text = "${currentStream.viewerCount} Viewers • ${currentStream.streamCategory}",
              style = MaterialTheme.typography.labelSmall,
              color = StarSecondaryText,
            )
          }

          Text(
            text = currentStream.title,
            style = MaterialTheme.typography.titleLarge,
            color = StarPrimaryText,
          )

          if (currentStream.isPkBattle) {
            val totalScore = (currentStream.hostPkScore + currentStream.opponentPkScore).coerceAtLeast(1L)
            val hostRatio = if (currentStream.hostPkScore == 0L && currentStream.opponentPkScore == 0L) {
              0.5f
            } else {
              currentStream.hostPkScore.toFloat() / totalScore.toFloat()
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Box(
                modifier = Modifier
                  .weight(1f)
                  .height(110.dp)
                  .clip(RoundedCornerShape(14.dp))
                  .background(NeonPurple.copy(alpha = 0.28f))
                  .padding(12.dp),
                contentAlignment = Alignment.Center,
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text("HOST", style = MaterialTheme.typography.labelSmall, color = ElectricBlue)
                  Text(currentStream.hostName, style = MaterialTheme.typography.titleMedium, color = StarPrimaryText)
                  Text("${currentStream.hostPkScore} PK Pts", style = MaterialTheme.typography.titleSmall, color = StarWarningGold)
                }
              }
              Box(
                modifier = Modifier
                  .weight(1f)
                  .height(110.dp)
                  .clip(RoundedCornerShape(14.dp))
                  .background(AccentPink.copy(alpha = 0.28f))
                  .padding(12.dp),
                contentAlignment = Alignment.Center,
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text("CHALLENGER", style = MaterialTheme.typography.labelSmall, color = AccentPink)
                  Text(currentStream.opponentName.ifBlank { "PK Challenger" }, style = MaterialTheme.typography.titleMedium, color = StarPrimaryText)
                  Text("${currentStream.opponentPkScore} PK Pts", style = MaterialTheme.typography.titleSmall, color = StarWarningGold)
                }
              }
            }

            LinearProgressIndicator(
              progress = { hostRatio },
              modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(50)),
              color = ElectricBlue,
              trackColor = AccentPink,
            )
            Text(
              text = "PK Battle scores are calculated strictly from verified Diamond gift transactions.",
              style = MaterialTheme.typography.labelSmall,
              color = StarSecondaryText,
            )
          } else {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(StarCardElevated, NeonPurple.copy(alpha = 0.35f)))),
              contentAlignment = Alignment.Center,
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.LiveTv, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(40.dp))
                Text("Host: @${currentStream.hostName}", style = MaterialTheme.typography.titleMedium)
                Text("Real-time Firestore signaling & chat active", style = MaterialTheme.typography.labelSmall, color = StarSecondaryText)
              }
            }
          }

          // Verified Gift Bar
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "Send Verified Gift (Wallet: ${profile?.diamondBalance ?: 0L} Diamonds)",
                style = MaterialTheme.typography.labelLarge,
                color = StarWarningGold,
              )
              if (currentStream.isPkBattle) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  FilterChip(
                    selected = supportHostInPk,
                    onClick = { supportHostInPk = true },
                    label = { Text("Boost Host") },
                  )
                  FilterChip(
                    selected = !supportHostInPk,
                    onClick = { supportHostInPk = false },
                    label = { Text("Boost Challenger") },
                  )
                }
              }
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              listOf(
                Triple("Neon Rose", 10L, ElectricBlue),
                Triple("Star Crown", 50L, NeonPurple),
                Triple("Galaxy Rocket", 200L, AccentPink),
              ).forEach { (giftName, cost, color) ->
                SingleTapButton(
                  text = "$giftName ($cost 💎)",
                  onClick = { onSendVerifiedGift(currentStream, giftName, cost, supportHostInPk) },
                  containerColor = color,
                  modifier = Modifier.weight(1f),
                )
              }
            }
          }
        }
      }

      // Real-time Live Chat List
      Text("Live Room Chat (${streamMessages.size})", style = MaterialTheme.typography.titleSmall)
      LazyColumn(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        items(streamMessages, key = { it.id }) { msg ->
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = if (msg.isGiftAlert) NeonPurple.copy(alpha = 0.28f) else StarCardBackground,
            border = BorderStroke(1.dp, if (msg.isGiftAlert) StarWarningGold else StarOutlineBorder),
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = if (msg.isGiftAlert) "🎁 GIFT ALERT • @${msg.senderName}" else "@${msg.senderName}",
                  style = MaterialTheme.typography.labelSmall,
                  color = if (msg.isGiftAlert) StarWarningGold else ElectricBlue,
                  fontWeight = FontWeight.Bold,
                )
                Text(msg.messageText, style = MaterialTheme.typography.bodyMedium, color = StarPrimaryText)
              }
              IconButton(onClick = { reportTarget = "chat_message" to msg.id }) {
                Icon(Icons.Default.Flag, contentDescription = "Report Chat Message", tint = StarSecondaryText, modifier = Modifier.size(16.dp))
              }
            }
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedTextField(
          value = chatInput,
          onValueChange = { chatInput = it },
          placeholder = { Text("Send real-time live chat...") },
          modifier = Modifier
            .weight(1f)
            .testTag("live_chat_input"),
        )
        IconButton(
          onClick = {
            if (chatInput.isNotBlank()) {
              onSendChat(currentStream.id, chatInput)
              chatInput = ""
            }
          }
        ) {
          Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Chat", tint = ElectricBlue)
        }
      }
    }
  } else {
    // Live Broadcast Lobby
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
          Column(modifier = Modifier.weight(1f)) {
            Text("Live Streaming & PK Battles", style = MaterialTheme.typography.headlineMedium)
            Text(
              "Real-time Firestore broadcast rooms, live audience chat & verified gift PK battles",
              style = MaterialTheme.typography.bodyMedium,
              color = StarSecondaryText,
            )
          }
          SingleTapButton(
            text = "Go Live / PK",
            onClick = { showStartModal = true },
            containerColor = AccentPink,
            testTag = "open_go_live_modal_button",
          )
        }
      }

      item {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          color = StarCardElevated,
          border = BorderStroke(1.dp, StarOutlineBorder),
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              "Live Broadcast & PK Architecture",
              style = MaterialTheme.typography.titleSmall,
              color = ElectricBlue,
            )
            Text(
              "Live room metadata, real-time audience chat, viewer counts, and Two-Person PK Battle gift scores are synchronized live via Cloud Firestore. External WebRTC/RTMP camera ingest can be connected via your streaming provider credentials without simulated video feeds.",
              style = MaterialTheme.typography.bodyMedium,
              color = StarSecondaryText,
            )
          }
        }
      }

      if (activeStreams.isEmpty()) {
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
              Icon(Icons.Default.LiveTv, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(44.dp))
              Text("No Active Broadcasts Right Now", style = MaterialTheme.typography.titleMedium)
              SingleTapButton(
                text = "Start a Live Room or PK Battle",
                onClick = { showStartModal = true },
                containerColor = NeonPurple,
              )
            }
          }
        }
      } else {
        items(activeStreams, key = { it.id }) { stream ->
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onSelectStream(stream) },
            shape = RoundedCornerShape(16.dp),
            color = StarCardBackground,
            border = BorderStroke(1.dp, if (stream.isPkBattle) AccentPink else ElectricBlue),
          ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = if (stream.isPkBattle) "⚔️ TWO-PERSON PK BATTLE" else "🔴 LIVE ROOM",
                  style = MaterialTheme.typography.labelLarge,
                  color = if (stream.isPkBattle) AccentPink else ElectricBlue,
                )
                Text("${stream.viewerCount} watching", style = MaterialTheme.typography.labelSmall, color = StarSecondaryText)
              }
              Text(stream.title, style = MaterialTheme.typography.titleMedium, color = StarPrimaryText)
              Text(
                text = if (stream.isPkBattle) {
                  "Host: @${stream.hostName} (${stream.hostPkScore} pts) vs @${stream.opponentName} (${stream.opponentPkScore} pts)"
                } else {
                  "Host: @${stream.hostName} • Category: ${stream.streamCategory}"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = StarSecondaryText,
              )
              SingleTapButton(
                text = "Enter Live Room & Chat",
                onClick = { onSelectStream(stream) },
                containerColor = if (stream.isPkBattle) AccentPink else NeonPurple,
              )
            }
          }
        }
      }
    }
  }

  if (showStartModal) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Music & Talent") }
    var isPk by remember { mutableStateOf(true) }
    var opponentName by remember { mutableStateOf("StarRival_Official") }

    AlertDialog(
      onDismissRequest = { showStartModal = false },
      containerColor = StarCardBackground,
      titleContentColor = StarPrimaryText,
      title = { Text("Start Live Broadcast or PK Battle", style = MaterialTheme.typography.titleMedium) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Broadcast Title") },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("live_stream_title_input"),
          )
          OutlinedTextField(
            value = category,
            onValueChange = { category = it },
            label = { Text("Category") },
            modifier = Modifier.fillMaxWidth(),
          )
          Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = isPk, onCheckedChange = { isPk = it })
            Text("Enable Two-Person PK Battle Mode", color = StarPrimaryText)
          }
          if (isPk) {
            OutlinedTextField(
              value = opponentName,
              onValueChange = { opponentName = it },
              label = { Text("PK Opponent Creator Handle") },
              modifier = Modifier.fillMaxWidth(),
            )
          }
        }
      },
      confirmButton = {
        SingleTapButton(
          text = "Launch Live Room",
          enabled = title.isNotBlank(),
          onClick = {
            onStartBroadcast(title, category, isPk, opponentName)
            showStartModal = false
          },
          testTag = "confirm_go_live_button",
        )
      },
      dismissButton = {
        TextButton(onClick = { showStartModal = false }) {
          Text("Cancel", color = StarSecondaryText)
        }
      },
    )
  }

  reportTarget?.let { (type, id) ->
    ReportContentDialog(
      targetType = type,
      targetId = id,
      targetLabel = type.replace('_', ' '),
      onDismiss = { reportTarget = null },
      onSubmitReport = { reason, details ->
        onReport(type, id, reason, details)
        reportTarget = null
      },
    )
  }
}
