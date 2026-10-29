package com.example.ui.components

import android.os.SystemClock
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentPink
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.StarBackground
import com.example.ui.theme.StarCardBackground
import com.example.ui.theme.StarCardElevated
import com.example.ui.theme.StarPrimaryText
import com.example.ui.theme.StarSecondaryText
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Remembers a lock-free single-tap dispatcher so every button responds immediately
 * on the first tap and ignores accidental double-taps within [cooldownMs].
 */
@Composable
fun rememberSingleTap(
  cooldownMs: Long = 450L,
  onClick: () -> Unit,
): () -> Unit {
  val currentOnClick by rememberUpdatedState(onClick)
  val lastTapRef = remember { AtomicLong(-1_000_000L) }
  return remember(cooldownMs) {
    {
      val now = SystemClock.uptimeMillis()
      val prev = lastTapRef.get()
      if ((prev < 0L || now - prev >= cooldownMs) && lastTapRef.compareAndSet(prev, now)) {
        currentOnClick()
      }
    }
  }
}

/**
 * 3D Faceted Neon Star Emblem with smooth 3D perspective animation, orbiting
 * blue-purple light rings, and 3D extruded bevel geometry.
 */
@Composable
fun Animated3DNeonStarEmblem(
  size: Dp,
  modifier: Modifier = Modifier,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "star3dTransition")
  val tiltAngle by infiniteTransition.animateFloat(
    initialValue = -16f,
    targetValue = 16f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "star3dTilt",
  )
  val orbitAngle by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 7000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "star3dOrbit",
  )
  val glowPulse by infiniteTransition.animateFloat(
    initialValue = 0.78f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "star3dGlow",
  )

  Box(
    modifier = modifier
      .size(size)
      .testTag("animated_3d_star_logo")
      .graphicsLayer {
        rotationY = tiltAngle
        rotationX = tiltAngle * 0.3f
        cameraDistance = 14f * density
      },
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.matchParentSize()) {
      val cx = this.size.width / 2f
      val cy = this.size.height / 2f
      val outerR = this.size.minDimension * 0.42f
      val innerR = outerR * 0.44f

      // 1. Volumetric Blue-Purple Ambient Aura
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            NeonPurple.copy(alpha = 0.55f * glowPulse),
            ElectricBlue.copy(alpha = 0.30f * glowPulse),
            Color.Transparent,
          ),
          center = Offset(cx, cy),
          radius = this.size.minDimension * 0.50f,
        ),
        center = Offset(cx, cy),
        radius = this.size.minDimension * 0.50f,
      )

      // 2. Orbiting 3D Neon Halo Ring
      rotate(degrees = orbitAngle, pivot = Offset(cx, cy)) {
        drawCircle(
          brush = Brush.sweepGradient(
            listOf(
              ElectricBlue,
              NeonPurple,
              AccentPink,
              ElectricBlue,
            )
          ),
          radius = outerR * 1.08f,
          center = Offset(cx, cy),
          style = Stroke(width = (this.size.minDimension * 0.045f).coerceAtLeast(2f), cap = StrokeCap.Round),
        )
      }

      // Helper to build a 5-point star path with an offset for 3D extrusion
      fun buildStarPath(offsetX: Float, offsetY: Float, scale: Float = 1f): Path {
        val path = Path()
        for (i in 0 until 10) {
          val r = if (i % 2 == 0) outerR * scale else innerR * scale
          val angleRad = ((i * 36.0 - 90.0) * PI / 180.0).toFloat()
          val px = cx + offsetX + r * cos(angleRad)
          val py = cy + offsetY + r * sin(angleRad)
          if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        return path
      }

      // 3. 3D Extruded Back Depth Layer (Dark Indigo-Purple Bevel)
      val depthShiftX = (-tiltAngle * 0.25f)
      val depthShiftY = this.size.minDimension * 0.055f
      drawPath(
        path = buildStarPath(depthShiftX, depthShiftY, scale = 1.02f),
        color = Color(0xFF231354),
      )

      // 4. 3D Mid Extrusion Layer (Electric Blue / Neon Purple Rim)
      drawPath(
        path = buildStarPath(depthShiftX * 0.5f, depthShiftY * 0.5f, scale = 1.0f),
        brush = Brush.linearGradient(
          colors = listOf(ElectricBlue, NeonPurple, AccentPink),
          start = Offset(0f, 0f),
          end = Offset(this.size.width, this.size.height),
        ),
      )

      // 5. Front 3D Faceted Star Body
      val frontStar = buildStarPath(0f, 0f, scale = 0.94f)
      drawPath(
        path = frontStar,
        brush = Brush.linearGradient(
          colors = listOf(
            Color(0xFF7DD3FC),
            ElectricBlue,
            NeonPurple,
            AccentPink,
          ),
          start = Offset(cx - outerR, cy - outerR),
          end = Offset(cx + outerR, cy + outerR),
        ),
      )

      // 6. 3D Ridge Lines from Center to Star Vertices (gives true 3D crystal star look)
      for (i in 0 until 5) {
        val outerAngle = ((i * 72.0 - 90.0) * PI / 180.0).toFloat()
        val vx = cx + (outerR * 0.92f) * cos(outerAngle)
        val vy = cy + (outerR * 0.92f) * sin(outerAngle)
        drawLine(
          color = Color.White.copy(alpha = 0.55f),
          start = Offset(cx, cy),
          end = Offset(vx, vy),
          strokeWidth = (this.size.minDimension * 0.02f).coerceAtLeast(1.2f),
        )
      }

      // 7. Specular Rim Outline & Center 3D Play Emblem
      drawPath(
        path = frontStar,
        color = Color.White.copy(alpha = 0.85f),
        style = Stroke(width = (this.size.minDimension * 0.025f).coerceAtLeast(1.5f)),
      )

      val playSize = innerR * 0.55f
      val playPath = Path().apply {
        moveTo(cx - playSize * 0.45f, cy - playSize * 0.65f)
        lineTo(cx + playSize * 0.75f, cy)
        lineTo(cx - playSize * 0.45f, cy + playSize * 0.65f)
        close()
      }
      drawPath(path = playPath, color = StarBackground.copy(alpha = 0.85f))
      drawPath(
        path = playPath,
        color = Color.White,
        style = Stroke(width = (this.size.minDimension * 0.02f).coerceAtLeast(1.2f)),
      )
    }
  }
}

/**
 * 3D Extruded "HELLO STAR VIBE" Brand Title with multi-layer depth shadow,
 * blue-purple neon lighting, and animated specular shimmer.
 */
@Composable
fun Extruded3DBrandTitle(
  fontSizeSp: Int,
  modifier: Modifier = Modifier,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "brandText3d")
  val shimmerShift by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 600f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 3200, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "textShimmer",
  )

  val baseStyle = TextStyle(
    fontFamily = OrbitronFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = fontSizeSp.sp,
    letterSpacing = 1.6.sp,
  )

  Box(
    modifier = modifier.testTag("hello_star_vibe_3d_text"),
    contentAlignment = Alignment.Center,
  ) {
    // Layer 1: Deep 3D Extrusion Shadow
    Text(
      text = "HELLO STAR VIBE",
      style = baseStyle,
      color = Color(0xFF1B0E42),
      modifier = Modifier
        .offset(x = 2.5.dp, y = 3.dp)
        .clearAndSetSemantics {},
    )
    // Layer 2: Mid 3D Neon Purple-Blue Bevel
    Text(
      text = "HELLO STAR VIBE",
      style = baseStyle,
      color = NeonPurple.copy(alpha = 0.75f),
      modifier = Modifier
        .offset(x = 1.2.dp, y = 1.5.dp)
        .clearAndSetSemantics {},
    )
    // Layer 3: Front Lit Metallic Blue-Purple-White 3D Face
    Text(
      text = "HELLO STAR VIBE",
      style = baseStyle.copy(
        brush = Brush.linearGradient(
          colors = listOf(
            ElectricBlue,
            Color.White,
            NeonPurple,
            AccentPink,
            ElectricBlue,
          ),
          start = Offset(shimmerShift - 200f, 0f),
          end = Offset(shimmerShift + 200f, 80f),
        )
      ),
    )
  }
}

/**
 * Official HELLO STAR VIBE 3D Animated Logo Header.
 * Used across Login/Signup and Home screens.
 */
@Composable
fun HelloStarVibeLogoHeader(
  subtitle: String? = null,
  iconSize: Dp = 56.dp,
  compact: Boolean = false,
  modifier: Modifier = Modifier,
) {
  if (compact) {
    Row(
      modifier = modifier,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Animated3DNeonStarEmblem(size = 38.dp)
      Spacer(modifier = Modifier.width(10.dp))
      Column {
        Extruded3DBrandTitle(fontSizeSp = 15)
        if (!subtitle.isNullOrBlank()) {
          Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = StarSecondaryText,
            maxLines = 1,
          )
        }
      }
    }
  } else {
    Column(
      modifier = modifier,
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Box(
        modifier = Modifier
          .size(iconSize + 28.dp)
          .clip(CircleShape)
          .background(
            Brush.radialGradient(
              colors = listOf(
                NeonPurple.copy(alpha = 0.42f),
                ElectricBlue.copy(alpha = 0.22f),
                Color.Transparent,
              )
            )
          ),
        contentAlignment = Alignment.Center,
      ) {
        Animated3DNeonStarEmblem(size = iconSize + 10.dp)
      }
      Spacer(modifier = Modifier.height(10.dp))
      Extruded3DBrandTitle(fontSizeSp = 22)
      if (!subtitle.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = StarSecondaryText,
        )
      }
    }
  }
}

@Composable
fun NeonStarHeroBanner(
  title: String,
  subtitle: String,
  badgeText: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(18.dp),
    color = StarCardBackground,
    border = BorderStroke(
      1.dp,
      Brush.horizontalGradient(listOf(NeonPurple, ElectricBlue, AccentPink))
    ),
  ) {
    Box(modifier = Modifier.fillMaxWidth()) {
      Canvas(modifier = Modifier.matchParentSize()) {
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(NeonPurple.copy(alpha = 0.32f), Color.Transparent),
            center = Offset(size.width * 0.85f, size.height * 0.3f),
            radius = size.width * 0.55f,
          ),
          center = Offset(size.width * 0.85f, size.height * 0.3f),
          radius = size.width * 0.55f,
        )
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(ElectricBlue.copy(alpha = 0.25f), Color.Transparent),
            center = Offset(size.width * 0.15f, size.height * 0.8f),
            radius = size.width * 0.45f,
          ),
          center = Offset(size.width * 0.15f, size.height * 0.8f),
          radius = size.width * 0.45f,
        )
      }
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Surface(
            color = NeonPurple.copy(alpha = 0.22f),
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.5f)),
          ) {
            Text(
              text = badgeText,
              style = MaterialTheme.typography.labelSmall,
              color = ElectricBlue,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
          }
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = StarPrimaryText,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = StarSecondaryText,
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Animated3DNeonStarEmblem(size = 58.dp)
      }
    }
  }
}

/**
 * Single-tap button that executes immediately on the first tap and blocks
 * duplicate rapid taps using lock-free uptimeMillis tracking.
 */
@Composable
fun SingleTapButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  containerColor: Color = NeonPurple,
  contentColor: Color = StarPrimaryText,
  testTag: String? = null,
) {
  val guardedClick = rememberSingleTap(cooldownMs = 450L, onClick = onClick)
  val taggedModifier = if (testTag != null) modifier.testTag(testTag) else modifier

  Button(
    onClick = guardedClick,
    enabled = enabled,
    modifier = taggedModifier,
    shape = RoundedCornerShape(12.dp),
    colors = ButtonDefaults.buttonColors(
      containerColor = containerColor,
      contentColor = contentColor,
      disabledContainerColor = StarCardElevated,
      disabledContentColor = StarSecondaryText,
    ),
  ) {
    Text(text = text, style = MaterialTheme.typography.labelLarge)
  }
}

@Composable
fun ReportContentDialog(
  targetType: String,
  targetId: String,
  targetLabel: String,
  onDismiss: () -> Unit,
  onSubmitReport: (reasonCategory: String, details: String) -> Unit,
) {
  val categories = listOf(
    "explicit_content" to "Explicit / Adult Content",
    "illegal_content" to "Illegal Content",
    "harassment" to "Harassment & Abuse",
    "threats_danger" to "Threats & Danger",
    "impersonation_scam" to "Impersonation / Scam",
    "copyright" to "Copyright Infringement",
  )
  var selectedCategory by remember { mutableStateOf(categories.first().first) }
  var details by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = StarCardBackground,
    titleContentColor = StarPrimaryText,
    textContentColor = StarSecondaryText,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Flag, contentDescription = null, tint = AccentPink)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Report $targetLabel", style = MaterialTheme.typography.titleMedium)
      }
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "Select the Community Guideline violation category:",
          style = MaterialTheme.typography.bodyMedium,
          color = StarSecondaryText,
        )
        categories.forEach { (key, label) ->
          FilterChip(
            selected = selectedCategory == key,
            onClick = { selectedCategory = key },
            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
            modifier = Modifier.fillMaxWidth(),
          )
        }
        OutlinedTextField(
          value = details,
          onValueChange = { details = it },
          label = { Text("Additional details (required for audit log)") },
          singleLine = false,
          maxLines = 3,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("report_details_input"),
        )
      }
    },
    confirmButton = {
      SingleTapButton(
        text = "Submit Report",
        onClick = {
          onSubmitReport(
            selectedCategory,
            details.ifBlank { "Reported $targetType ($targetId) for $selectedCategory" },
          )
        },
        containerColor = AccentPink,
        testTag = "confirm_report_button",
      )
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = StarSecondaryText)
      }
    },
  )
}
