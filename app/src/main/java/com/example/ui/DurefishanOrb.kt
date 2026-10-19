package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.AssistantState
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DurefishanOrb(
  state: AssistantState,
  amplitude: Float,
  isMuted: Boolean,
  onOrbClick: () -> Unit,
  modifier: Modifier = Modifier,
  size: Dp = 250.dp
) {
  val infiniteTransition = rememberInfiniteTransition(label = "orb_infinite")

  // Breathing animation when idle
  val breathScale by infiniteTransition.animateFloat(
    initialValue = 0.94f,
    targetValue = 1.05f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "breath"
  )

  // Rotating light ring angle
  val ringRotation by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(
        durationMillis = if (state is AssistantState.Thinking) 1800 else 6000,
        easing = LinearEasing
      ),
      repeatMode = RepeatMode.Restart
    ),
    label = "ring_rotation"
  )

  // Wave phase for speaking
  val wavePhase by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = (2 * Math.PI).toFloat(),
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1500, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "wave_phase"
  )

  // Smooth amplitude transition
  val smoothedAmp by animateFloatAsState(
    targetValue = if (isMuted) 0f else amplitude.coerceIn(0f, 1f),
    animationSpec = tween(100),
    label = "amp"
  )

  // Dynamic colors based on assistant state and mute status
  val primaryColor by animateColorAsState(
    targetValue = when {
      isMuted -> Color(0xFF3F3B58)
      state is AssistantState.Listening -> Color(0xFF00E5FF)
      state is AssistantState.Thinking -> Color(0xFFFFB300)
      state is AssistantState.Speaking -> Color(0xFFFF4081)
      state is AssistantState.ExecutingAction -> Color(0xFF7C4DFF)
      else -> Color(0xFF8A2BE2) // Idle violet
    },
    animationSpec = tween(350),
    label = "primary"
  )

  val secondaryColor by animateColorAsState(
    targetValue = when {
      isMuted -> Color(0xFF1E1A33)
      state is AssistantState.Listening -> Color(0xFF7C4DFF)
      state is AssistantState.Thinking -> Color(0xFFFF5252)
      state is AssistantState.Speaking -> Color(0xFF9C27B0)
      state is AssistantState.ExecutingAction -> Color(0xFF00E5FF)
      else -> Color(0xFF4A148C) // Deep indigo
    },
    animationSpec = tween(350),
    label = "secondary"
  )

  val highlightColor by animateColorAsState(
    targetValue = when {
      isMuted -> Color(0xFF7E79A8)
      state is AssistantState.Listening -> Color(0xFFE0F7FA)
      state is AssistantState.Thinking -> Color(0xFFFFF9C4)
      state is AssistantState.Speaking -> Color(0xFFFFE082)
      else -> Color(0xFFE1BEE7) // Lavender
    },
    animationSpec = tween(350),
    label = "highlight"
  )

  val interactionSource = remember { MutableInteractionSource() }

  Box(
    modifier = modifier
      .size(size)
      .testTag("durefishan_voice_orb")
      .clickable(
        interactionSource = interactionSource,
        indication = null
      ) {
        onOrbClick()
      },
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val center = Offset(this.size.width / 2f, this.size.height / 2f)
      val baseRadius = this.size.minDimension / 3.4f

      // Effective radius modulated by state & audio
      val dynamicRadius = if (isMuted) {
        baseRadius * 0.92f
      } else {
        baseRadius * breathScale * (1f + smoothedAmp * 0.4f)
      }

      // 1. Outermost Ambient Glow Aura
      val outerAuraRadius = dynamicRadius * (if (isMuted) 1.25f else 1.8f)
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            primaryColor.copy(alpha = if (isMuted) 0.08f else (0.28f + smoothedAmp * 0.35f)),
            secondaryColor.copy(alpha = if (isMuted) 0.04f else 0.12f),
            Color.Transparent
          ),
          center = center,
          radius = outerAuraRadius
        ),
        center = center,
        radius = outerAuraRadius
      )

      // 2. Concentric Ethereal Light Rings
      if (!isMuted) {
        val ring1Radius = dynamicRadius * (1.18f + smoothedAmp * 0.15f)
        val ring2Radius = dynamicRadius * (1.35f + smoothedAmp * 0.25f)

        // Ring 1
        drawCircle(
          color = primaryColor.copy(alpha = 0.25f + smoothedAmp * 0.3f),
          center = center,
          radius = ring1Radius,
          style = Stroke(width = 1.5.dp.toPx())
        )

        // Ring 2 with sweep gradient (processing spinner / ambient ring)
        drawCircle(
          brush = Brush.sweepGradient(
            colors = listOf(
              primaryColor.copy(alpha = 0.05f),
              primaryColor.copy(alpha = 0.7f),
              highlightColor.copy(alpha = 0.9f),
              secondaryColor.copy(alpha = 0.3f),
              primaryColor.copy(alpha = 0.05f)
            ),
            center = center
          ),
          center = center,
          radius = ring2Radius,
          style = Stroke(
            width = if (state is AssistantState.Thinking) 3.5.dp.toPx() else 1.5.dp.toPx()
          )
        )
      }

      // 3. 3D-Style Glowing Sphere Body
      // Radial gradient with offset center to create realistic volumetric 3D specular depth
      val lightOffset = center - Offset(dynamicRadius * 0.35f, dynamicRadius * 0.35f)
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            highlightColor.copy(alpha = if (isMuted) 0.5f else 0.95f),
            primaryColor.copy(alpha = if (isMuted) 0.6f else 0.92f),
            secondaryColor.copy(alpha = if (isMuted) 0.8f else 0.98f),
            Color(0xFF080612)
          ),
          center = lightOffset,
          radius = dynamicRadius * 1.25f
        ),
        center = center,
        radius = dynamicRadius
      )

      // 4. Subtle inner glowing rim
      drawCircle(
        brush = Brush.sweepGradient(
          colors = listOf(
            highlightColor.copy(alpha = 0.8f),
            primaryColor.copy(alpha = 0.4f),
            secondaryColor.copy(alpha = 0.6f),
            highlightColor.copy(alpha = 0.8f)
          ),
          center = center
        ),
        center = center,
        radius = dynamicRadius,
        style = Stroke(width = 2.dp.toPx())
      )

      // 5. Dynamic waveform / Particle harmonics (when active)
      if (!isMuted && (state is AssistantState.Listening || state is AssistantState.Speaking || smoothedAmp > 0.05f)) {
        val barCount = 40
        val angleStep = (2 * Math.PI / barCount).toFloat()
        val rotationRad = Math.toRadians(ringRotation.toDouble()).toFloat()

        for (i in 0 until barCount) {
          val angle = i * angleStep + rotationRad
          val waveFactor = sin(angle * 4f + wavePhase).coerceAtLeast(0f)
          val barLength = 8.dp.toPx() + (smoothedAmp * 40.dp.toPx() * (0.35f + waveFactor * 0.65f))

          val startX = center.x + dynamicRadius * cos(angle)
          val startY = center.y + dynamicRadius * sin(angle)
          val endX = center.x + (dynamicRadius + barLength) * cos(angle)
          val endY = center.y + (dynamicRadius + barLength) * sin(angle)

          drawLine(
            brush = Brush.linearGradient(
              colors = listOf(highlightColor, primaryColor)
            ),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round
          )
        }
      }
    }
  }
}
