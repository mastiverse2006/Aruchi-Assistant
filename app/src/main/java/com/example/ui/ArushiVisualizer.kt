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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
fun ArushiVisualizer(
  state: AssistantState,
  amplitude: Float,
  modifier: Modifier = Modifier,
  size: Dp = 200.dp
) {
  val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")

  val pulseProgress by infiniteTransition.animateFloat(
    initialValue = 0.95f,
    targetValue = 1.08f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse"
  )

  val rotationAngle by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 8000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "rotation"
  )

  // Target colors based on state
  val primaryColor by animateColorAsState(
    targetValue = when (state) {
      is AssistantState.Listening -> Color(0xFF00E5FF)
      is AssistantState.Speaking -> Color(0xFFFF4081)
      is AssistantState.Thinking -> Color(0xFFFFB300)
      is AssistantState.ExecutingAction -> Color(0xFF00E676)
      is AssistantState.Idle -> Color(0xFF7C4DFF)
    },
    animationSpec = tween(400),
    label = "primary_color"
  )

  val secondaryColor by animateColorAsState(
    targetValue = when (state) {
      is AssistantState.Listening -> Color(0xFF7C4DFF)
      is AssistantState.Speaking -> Color(0xFF7C4DFF)
      is AssistantState.Thinking -> Color(0xFFFF3D00)
      is AssistantState.ExecutingAction -> Color(0xFF00B0FF)
      is AssistantState.Idle -> Color(0xFF3F51B5)
    },
    animationSpec = tween(400),
    label = "secondary_color"
  )

  val smoothedAmp by animateFloatAsState(
    targetValue = amplitude.coerceIn(0f, 1f),
    animationSpec = tween(120),
    label = "amp"
  )

  Box(
    modifier = modifier
      .size(size)
      .testTag("arushi_visualizer"),
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val center = Offset(this.size.width / 2f, this.size.height / 2f)
      val baseRadius = this.size.minDimension / 3.2f
      val dynamicRadius = baseRadius * pulseProgress * (1f + smoothedAmp * 0.45f)

      // Outer diffused ambient aura
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            primaryColor.copy(alpha = 0.28f + smoothedAmp * 0.35f),
            secondaryColor.copy(alpha = 0.12f),
            Color.Transparent
          ),
          center = center,
          radius = dynamicRadius * 1.55f
        ),
        center = center,
        radius = dynamicRadius * 1.55f
      )

      // Middle animated ring
      drawCircle(
        brush = Brush.sweepGradient(
          colors = listOf(primaryColor, secondaryColor, primaryColor),
          center = center
        ),
        center = center,
        radius = dynamicRadius * 1.12f,
        style = Stroke(
          width = 3.dp.toPx() * (1f + smoothedAmp * 2f),
          cap = StrokeCap.Round
        )
      )

      // Inner glowing core
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            Color.White.copy(alpha = 0.95f),
            primaryColor.copy(alpha = 0.85f),
            secondaryColor.copy(alpha = 0.95f)
          ),
          center = center,
          radius = dynamicRadius
        ),
        center = center,
        radius = dynamicRadius
      )

      // Audio waveform bars around perimeter
      val barCount = 32
      val angleStep = (2 * Math.PI / barCount).toFloat()
      val rad = Math.toRadians(rotationAngle.toDouble()).toFloat()

      for (i in 0 until barCount) {
        val angle = i * angleStep + rad
        val waveFactor = sin(angle * 3f + (smoothedAmp * 10f)).coerceAtLeast(0f)
        val barLength = (12.dp.toPx() + smoothedAmp * 45.dp.toPx() * (0.4f + waveFactor * 0.6f))

        val startX = center.x + dynamicRadius * cos(angle)
        val startY = center.y + dynamicRadius * sin(angle)
        val endX = center.x + (dynamicRadius + barLength) * cos(angle)
        val endY = center.y + (dynamicRadius + barLength) * sin(angle)

        drawLine(
          brush = Brush.linearGradient(
            colors = listOf(primaryColor, secondaryColor)
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
