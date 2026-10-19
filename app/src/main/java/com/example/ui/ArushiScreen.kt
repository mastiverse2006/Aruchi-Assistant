package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.ActionIconType
import com.example.model.AssistantState
import com.example.model.ContactMatch
import com.example.model.ConversationItem
import com.example.model.LiveConnectionState
import com.example.viewmodel.ArushiViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArushiScreen(
  viewModel: ArushiViewModel,
  hasAudioPermission: Boolean,
  hasContactsPermission: Boolean,
  hasPhonePermission: Boolean,
  onRequestPermissions: () -> Unit,
  modifier: Modifier = Modifier
) {
  val connectionState by viewModel.connectionState.collectAsState()
  val assistantState by viewModel.assistantState.collectAsState()
  val isLiveActive by viewModel.isLiveActive.collectAsState()
  val isMuted by viewModel.isMuted.collectAsState()
  val visualizerAmplitude by viewModel.visualizerAmplitude.collectAsState()
  val conversationItems by viewModel.conversationItems.collectAsState()
  val apiKey by viewModel.apiKey.collectAsState()
  val activeDisambiguation by viewModel.activeDisambiguation.collectAsState()

  var showSettingsDialog by remember { mutableStateOf(false) }
  var typedPrompt by remember { mutableStateOf("") }
  val listState = rememberLazyListState()

  LaunchedEffect(conversationItems.size) {
    if (conversationItems.isNotEmpty()) {
      listState.animateScrollToItem(conversationItems.lastIndex)
    }
  }

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .testTag("arushi_screen"),
    topBar = {
      TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        ),
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .background(
                  color = MaterialTheme.colorScheme.primaryContainer,
                  shape = CircleShape
                ),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = "Arushi Icon",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = stringResource(R.string.assistant_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Hindi • English • Hinglish",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        },
        actions = {
          // Status indicator chip
          ConnectionStatusBadge(connectionState = connectionState)

          // Mute toggle (when live active)
          if (isLiveActive) {
            IconButton(
              onClick = { viewModel.toggleMute() },
              modifier = Modifier.testTag("mute_toggle_button")
            ) {
              Icon(
                imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = if (isMuted) "Unmute" else "Mute",
                tint = if (isMuted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
              )
            }
          }

          // Clear history button
          if (conversationItems.isNotEmpty()) {
            IconButton(
              onClick = { viewModel.clearHistory() },
              modifier = Modifier.testTag("clear_history_button")
            ) {
              Icon(
                imageVector = Icons.Default.DeleteSweep,
                contentDescription = "Clear Conversation"
              )
            }
          }

          // Settings button for API key
          IconButton(
            onClick = { showSettingsDialog = true },
            modifier = Modifier.testTag("settings_button")
          ) {
            Icon(
              imageVector = Icons.Default.Settings,
              contentDescription = "Settings"
            )
          }
        }
      )
    },
    bottomBar = {
      BottomControls(
        isLiveActive = isLiveActive,
        isMuted = isMuted,
        typedPrompt = typedPrompt,
        onTypedPromptChange = { typedPrompt = it },
        onSendPrompt = {
          if (typedPrompt.isNotBlank()) {
            viewModel.sendPrompt(typedPrompt)
            typedPrompt = ""
          }
        },
        onToggleLive = {
          if (!hasAudioPermission) {
            onRequestPermissions()
          } else {
            viewModel.toggleLiveSession()
          }
        }
      )
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Permission banner if audio or contacts permissions are missing
      if (!hasAudioPermission || !hasContactsPermission || !hasPhonePermission) {
        PermissionCard(
          hasAudio = hasAudioPermission,
          hasContacts = hasContactsPermission,
          hasPhone = hasPhonePermission,
          onRequestPermissions = onRequestPermissions
        )
      }

      // Visualizer and status section
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          ArushiVisualizer(
            state = assistantState,
            amplitude = visualizerAmplitude,
            size = 180.dp
          )

          Spacer(modifier = Modifier.height(10.dp))

          // Assistant status chip
          AssistantStatusText(
            state = assistantState,
            isLiveActive = isLiveActive,
            isMuted = isMuted
          )
        }
      }

      // Quick prompt suggestion chips
      QuickPromptsRow(
        onSelectPrompt = { prompt ->
          viewModel.sendPrompt(prompt)
        }
      )

      Spacer(modifier = Modifier.height(8.dp))

      // Feed of transcripts and executed actions
      LazyColumn(
        state = listState,
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .padding(horizontal = 16.dp)
          .testTag("conversation_feed"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
      ) {
        if (conversationItems.isEmpty()) {
          item {
            EmptyFeedPlaceholder()
          }
        }

        items(conversationItems, key = { it.id }) { item ->
          when (item) {
            is ConversationItem.UserTranscript -> {
              UserBubble(item.text)
            }
            is ConversationItem.ArushiTranscript -> {
              ArushiBubble(item.text)
            }
            is ConversationItem.ActionExecuted -> {
              ActionExecutedCard(item)
            }
            is ConversationItem.ContactDisambiguation -> {
              ContactDisambiguationCard(
                item = item,
                onSelect = { contact ->
                  viewModel.selectContactToCall(contact)
                },
                onDismiss = {
                  viewModel.dismissDisambiguation()
                }
              )
            }
          }
        }
      }
    }
  }

  // Settings / API Key Dialog
  if (showSettingsDialog) {
    ApiKeySettingsDialog(
      currentKey = apiKey,
      onSave = { newKey ->
        viewModel.saveApiKey(newKey)
        showSettingsDialog = false
      },
      onDismiss = { showSettingsDialog = false }
    )
  }
}

@Composable
fun ConnectionStatusBadge(connectionState: LiveConnectionState) {
  val (label, color) = when (connectionState) {
    is LiveConnectionState.Connected -> "Live" to Color(0xFF00E676)
    is LiveConnectionState.Connecting -> "Connecting" to Color(0xFFFFB300)
    is LiveConnectionState.Error -> "Offline" to MaterialTheme.colorScheme.error
    is LiveConnectionState.Disconnected -> "Ready" to MaterialTheme.colorScheme.outline
  }

  Surface(
    shape = RoundedCornerShape(12.dp),
    color = color.copy(alpha = 0.15f),
    modifier = Modifier.padding(end = 4.dp)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(8.dp)
          .background(color, CircleShape)
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontWeight = FontWeight.SemiBold
      )
    }
  }
}

@Composable
fun AssistantStatusText(
  state: AssistantState,
  isLiveActive: Boolean,
  isMuted: Boolean
) {
  val text = when {
    !isLiveActive -> "Tap mic to start speaking with Arushi"
    isMuted -> "Microphone muted"
    state is AssistantState.Listening -> "Listening in Hindi, English, Hinglish..."
    state is AssistantState.Thinking -> "Thinking..."
    state is AssistantState.Speaking -> "Arushi is speaking..."
    state is AssistantState.ExecutingAction -> "Executing action: ${state.actionName}..."
    else -> "Listening..."
  }

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    modifier = Modifier.padding(horizontal = 16.dp)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = Icons.Default.Language,
        contentDescription = null,
        modifier = Modifier.size(16.dp),
        tint = MaterialTheme.colorScheme.primary
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
fun QuickPromptsRow(onSelectPrompt: (String) -> Unit) {
  val prompts = listOf(
    "WhatsApp kholo",
    "Mummy ko call karo",
    "Call 9876543210",
    "Open YouTube",
    "Hindi mein baat karo",
    "Talk in English",
    "Hinglish mein bolo",
    "Open Settings"
  )

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .horizontalScroll(rememberScrollState())
      .padding(horizontal = 12.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    for (prompt in prompts) {
      SuggestionChip(
        onClick = { onSelectPrompt(prompt) },
        label = { Text(prompt, fontSize = 12.sp) },
        colors = SuggestionChipDefaults.suggestionChipColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.testTag("quick_prompt_${prompt.replace(" ", "_")}")
      )
    }
  }
}

@Composable
fun EmptyFeedPlaceholder() {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .padding(24.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Icon(
        imageVector = Icons.Default.SmartToy,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.size(48.dp)
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "Say 'Hello Arushi', 'WhatsApp kholo', or 'Call Mom'",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )
    }
  }
}

@Composable
fun UserBubble(text: String) {
  Box(
    modifier = Modifier.fillMaxWidth(),
    contentAlignment = Alignment.CenterEnd
  ) {
    Surface(
      shape = RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp),
      color = MaterialTheme.colorScheme.primary,
      contentColor = MaterialTheme.colorScheme.onPrimary,
      modifier = Modifier.widthIn(max = 280.dp)
    ) {
      Text(
        text = text,
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        style = MaterialTheme.typography.bodyMedium
      )
    }
  }
}

@Composable
fun ArushiBubble(text: String) {
  Box(
    modifier = Modifier.fillMaxWidth(),
    contentAlignment = Alignment.CenterStart
  ) {
    Surface(
      shape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp),
      color = MaterialTheme.colorScheme.secondaryContainer,
      contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
      modifier = Modifier.widthIn(max = 300.dp)
    ) {
      Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
        Text(
          text = text,
          style = MaterialTheme.typography.bodyMedium
        )
      }
    }
  }
}

@Composable
fun ActionExecutedCard(action: ConversationItem.ActionExecuted) {
  val (icon, iconColor) = when (action.iconType) {
    ActionIconType.WHATSAPP -> Icons.AutoMirrored.Filled.Send to Color(0xFF25D366)
    ActionIconType.CALL -> Icons.Default.Phone to Color(0xFF2196F3)
    ActionIconType.APP -> Icons.Default.OpenInBrowser to Color(0xFFFF9800)
    ActionIconType.SETTINGS -> Icons.Default.Settings to Color(0xFF9C27B0)
    ActionIconType.URL -> Icons.Default.OpenInBrowser to Color(0xFF00BCD4)
  }

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    ),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("action_executed_${action.action}")
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .background(iconColor.copy(alpha = 0.15f), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = iconColor,
          modifier = Modifier.size(22.dp)
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = formatActionTitle(action.action),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.width(6.dp))
          Icon(
            imageVector = if (action.success) Icons.Default.CheckCircle else Icons.Default.Error,
            contentDescription = null,
            tint = if (action.success) Color(0xFF00E676) else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(16.dp)
          )
        }
        Text(
          text = action.detail,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

private fun formatActionTitle(actionName: String): String {
  return when (actionName) {
    "openWhatsApp" -> "WhatsApp Action"
    "openApp" -> "App Launcher"
    "makeCall" -> "Phone Call"
    "callContact" -> "Contact Call"
    "openUrl" -> "Browser Link"
    else -> actionName
  }
}

@Composable
fun ContactDisambiguationCard(
  item: ConversationItem.ContactDisambiguation,
  onSelect: (ContactMatch) -> Unit,
  onDismiss: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
    ),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("contact_disambiguation_card")
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Multiple contacts found for '${item.query}'",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
          Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(18.dp))
        }
      }

      Text(
        text = "Which one would you like Arushi to call?",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onTertiaryContainer
      )

      Spacer(modifier = Modifier.height(10.dp))

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (contact in item.contacts) {
          FilledTonalButton(
            onClick = { onSelect(contact) },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("contact_pick_${contact.name}"),
            shape = RoundedCornerShape(12.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(contact.name, fontWeight = FontWeight.SemiBold)
                Text(
                  "${contact.type} • ${contact.number}",
                  style = MaterialTheme.typography.bodySmall
                )
              }
              Icon(Icons.Default.Call, contentDescription = "Call")
            }
          }
        }
      }
    }
  }
}

@Composable
fun PermissionCard(
  hasAudio: Boolean,
  hasContacts: Boolean,
  hasPhone: Boolean,
  onRequestPermissions: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 6.dp)
      .testTag("permission_card")
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = Icons.Default.Warning,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.error,
        modifier = Modifier.size(24.dp)
      )
      Spacer(modifier = Modifier.width(10.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Permissions Required",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold
        )
        val missing = buildList {
          if (!hasAudio) add("Microphone")
          if (!hasContacts) add("Contacts")
          if (!hasPhone) add("Phone")
        }.joinToString(", ")
        Text(
          text = "Grant $missing permissions for Arushi voice & phone features.",
          style = MaterialTheme.typography.bodySmall
        )
      }
      FilledTonalButton(
        onClick = onRequestPermissions,
        modifier = Modifier.testTag("grant_permission_button")
      ) {
        Text("Grant")
      }
    }
  }
}

@Composable
fun BottomControls(
  isLiveActive: Boolean,
  isMuted: Boolean,
  typedPrompt: String,
  onTypedPromptChange: (String) -> Unit,
  onSendPrompt: () -> Unit,
  onToggleLive: () -> Unit
) {
  Surface(
    tonalElevation = 6.dp,
    color = MaterialTheme.colorScheme.surface
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Text input field
        OutlinedTextField(
          value = typedPrompt,
          onValueChange = onTypedPromptChange,
          placeholder = { Text("Ask or command Arushi...", fontSize = 14.sp) },
          modifier = Modifier
            .weight(1f)
            .testTag("typed_prompt_input"),
          shape = RoundedCornerShape(24.dp),
          colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
          ),
          singleLine = true,
          trailingIcon = {
            if (typedPrompt.isNotBlank()) {
              IconButton(
                onClick = onSendPrompt,
                modifier = Modifier.testTag("send_prompt_button")
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.Send,
                  contentDescription = "Send",
                  tint = MaterialTheme.colorScheme.primary
                )
              }
            }
          }
        )

        Spacer(modifier = Modifier.width(10.dp))

        // Main Live Voice Action Button
        val buttonColor by animateColorAsState(
          targetValue = if (isLiveActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
          label = "button_color"
        )

        Surface(
          shape = CircleShape,
          color = buttonColor,
          modifier = Modifier
            .size(54.dp)
            .clip(CircleShape)
            .clickable { onToggleLive() }
            .testTag("main_voice_toggle_button")
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = if (isLiveActive) Icons.Rounded.CallEnd else Icons.Default.Mic,
              contentDescription = if (isLiveActive) "End Session" else "Start Live Voice",
              tint = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.size(26.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
fun ApiKeySettingsDialog(
  currentKey: String,
  onSave: (String) -> Unit,
  onDismiss: () -> Unit
) {
  var keyInput by remember { mutableStateOf(currentKey) }

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = { Icon(Icons.Default.VpnKey, contentDescription = null) },
    title = { Text("Gemini API Key Settings") },
    text = {
      Column {
        Text(
          text = "Arushi connects to Gemini Live Multimodal WebSocket. Enter your Gemini API key below if not set automatically via secrets:",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
          value = keyInput,
          onValueChange = { keyInput = it },
          label = { Text("Gemini API Key") },
          placeholder = { Text("AIzaSy...") },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("api_key_input"),
          singleLine = true
        )
      }
    },
    confirmButton = {
      TextButton(
        onClick = { onSave(keyInput) },
        modifier = Modifier.testTag("save_api_key_button")
      ) {
        Text("Save")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
