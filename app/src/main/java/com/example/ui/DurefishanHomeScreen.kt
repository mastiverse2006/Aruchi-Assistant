package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AssistantState
import com.example.model.ContactMatch
import com.example.viewmodel.DurefishanViewModel

@Composable
fun DurefishanHomeScreen(
  viewModel: DurefishanViewModel,
  hasAudioPermission: Boolean,
  onRequestPermissions: () -> Unit,
  onNavigateToSettings: () -> Unit,
  modifier: Modifier = Modifier
) {
  val assistantState by viewModel.assistantState.collectAsState()
  val statusMessage by viewModel.statusMessage.collectAsState()
  val visualizerAmplitude by viewModel.visualizerAmplitude.collectAsState()
  val isMuted by viewModel.isMuted.collectAsState()
  val lastSpokenCommand by viewModel.lastSpokenCommand.collectAsState()
  val lastAssistantResponse by viewModel.lastAssistantResponse.collectAsState()
  val activeDisambiguation by viewModel.activeDisambiguation.collectAsState()

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .testTag("durefishan_home_screen"),
    containerColor = Color(0xFF040208) // Pure deep black with subtle violet depth
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .background(
          brush = Brush.radialGradient(
            colors = listOf(
              Color(0xFF170D30).copy(alpha = 0.5f),
              Color(0xFF040208)
            ),
            radius = 1200f
          )
        )
    ) {
      // 1. Minimal Header (Status Area & Settings Icon)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Durefishan Assistant",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
          )
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 2.dp)
          ) {
            Box(
              modifier = Modifier
                .size(7.dp)
                .background(
                  color = when {
                    isMuted -> Color.Gray
                    assistantState is AssistantState.Listening -> Color(0xFF00E5FF)
                    assistantState is AssistantState.Thinking -> Color(0xFFFFB300)
                    assistantState is AssistantState.Speaking -> Color(0xFFFF4081)
                    else -> Color(0xFF7C4DFF)
                  },
                  shape = CircleShape
                )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = when {
                isMuted -> "Muted"
                assistantState is AssistantState.Listening -> "Listening"
                assistantState is AssistantState.Thinking -> "Processing"
                assistantState is AssistantState.Speaking -> "Speaking"
                else -> "Ready"
              },
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFFB388FF),
              fontWeight = FontWeight.Medium
            )
          }
        }

        IconButton(
          onClick = onNavigateToSettings,
          modifier = Modifier
            .size(44.dp)
            .background(Color(0xFF141026), CircleShape)
            .testTag("settings_button")
        ) {
          Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "Settings",
            tint = Color(0xFFE1BEE7)
          )
        }
      }

      // 2. Central Voice Orb & Subtle Status Message
      Column(
        modifier = Modifier
          .align(Alignment.Center)
          .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        // The interactive 3D-style Voice Orb
        DurefishanOrb(
          state = assistantState,
          amplitude = visualizerAmplitude,
          isMuted = isMuted,
          onOrbClick = {
            if (!hasAudioPermission) {
              onRequestPermissions()
            } else {
              viewModel.onOrbClicked()
            }
          },
          size = 260.dp
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Subtle status message ("Tap to speak", "Listening...", "Processing...", "Speaking...")
        Text(
          text = statusMessage,
          style = MaterialTheme.typography.titleMedium,
          color = when {
            isMuted -> Color.Gray
            assistantState is AssistantState.Listening -> Color(0xFF00E5FF)
            assistantState is AssistantState.Thinking -> Color(0xFFFFB300)
            assistantState is AssistantState.Speaking -> Color(0xFFFF80AB)
            else -> Color(0xFFE1BEE7)
          },
          fontWeight = FontWeight.Medium,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Optional small, elegant transcript of latest command and response without clutter
        AnimatedVisibility(
          visible = lastSpokenCommand != null || lastAssistantResponse != null,
          enter = fadeIn(),
          exit = fadeOut()
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.widthIn(max = 320.dp)
          ) {
            lastSpokenCommand?.let { cmd ->
              Text(
                text = "\"$cmd\"",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                fontSize = 13.sp
              )
            }
            lastAssistantResponse?.let { resp ->
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = resp,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFB388FF),
                textAlign = TextAlign.Center,
                fontSize = 12.sp
              )
            }
          }
        }
      }

      // 3. Bottom Minimal Microphone Mute/Unmute Control
      Box(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(bottom = 36.dp)
      ) {
        Surface(
          shape = CircleShape,
          color = if (isMuted) Color(0xFF2E2640) else Color(0xFF141026),
          modifier = Modifier
            .size(56.dp)
            .clickable { viewModel.toggleMute() }
            .testTag("mute_unmute_button")
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
              contentDescription = if (isMuted) "Unmute Microphone" else "Mute Microphone",
              tint = if (isMuted) Color(0xFFFF5252) else Color(0xFF00E5FF),
              modifier = Modifier.size(24.dp)
            )
          }
        }
      }
    }
  }

  // Contact Disambiguation Dialog (Safety for Multiple Contact Matches)
  activeDisambiguation?.let { disambiguation ->
    AlertDialog(
      onDismissRequest = { viewModel.dismissDisambiguation() },
      containerColor = Color(0xFF131024),
      titleContentColor = Color.White,
      textContentColor = Color(0xFFE1BEE7),
      title = {
        Text("Multiple Contacts for '${disambiguation.query}'")
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            "Which contact would you like Durefishan to call?",
            fontSize = 13.sp,
            color = Color(0xFFB388FF)
          )
          disambiguation.contacts.forEach { contact ->
            FilledTonalButton(
              onClick = { viewModel.selectContactToCall(contact) },
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(12.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text(contact.name, color = Color.White, fontWeight = FontWeight.Bold)
                  Text("${contact.type} • ${contact.number}", fontSize = 11.sp, color = Color(0xFFE1BEE7))
                }
                Icon(Icons.Default.Call, contentDescription = null, tint = Color(0xFF00E5FF))
              }
            }
          }
        }
      },
      confirmButton = {},
      dismissButton = {
        TextButton(onClick = { viewModel.dismissDisambiguation() }) {
          Text("Cancel", color = Color.Gray)
        }
      }
    )
  }
}
