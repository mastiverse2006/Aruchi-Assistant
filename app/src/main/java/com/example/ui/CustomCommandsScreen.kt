package com.example.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CustomCommand
import com.example.viewmodel.DurefishanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomCommandsScreen(
  viewModel: DurefishanViewModel,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val commands by viewModel.customCommands.collectAsState()
  var searchQuery by remember { mutableStateOf("") }
  var showEditorDialog by remember { mutableStateOf(false) }
  var commandToEdit by remember { mutableStateOf<CustomCommand?>(null) }

  val filteredCommands = remember(commands, searchQuery) {
    if (searchQuery.isBlank()) {
      commands
    } else {
      commands.filter { cmd ->
        cmd.commandPhrase.contains(searchQuery, ignoreCase = true) ||
            cmd.alternativePhrases.contains(searchQuery, ignoreCase = true) ||
            cmd.actionConfig.contains(searchQuery, ignoreCase = true)
      }
    }
  }

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .testTag("custom_commands_screen"),
    containerColor = Color(0xFF07050E),
    topBar = {
      TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = Color(0xFF07050E),
          titleContentColor = Color.White
        ),
        title = {
          Text("Custom Commands", fontWeight = FontWeight.Bold)
        },
        navigationIcon = {
          IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.testTag("custom_commands_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = Color.White
            )
          }
        }
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = {
          commandToEdit = null
          showEditorDialog = true
        },
        containerColor = Color(0xFF8A2BE2),
        contentColor = Color.White,
        modifier = Modifier.testTag("add_custom_command_fab")
      ) {
        Icon(Icons.Default.Add, contentDescription = "Add Command")
      }
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(horizontal = 16.dp)
    ) {
      // Search Bar
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Search custom commands...", color = Color.Gray) },
        leadingIcon = {
          Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFFB388FF))
        },
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 8.dp)
          .testTag("search_commands_input"),
        shape = RoundedCornerShape(20.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = Color.White,
          unfocusedTextColor = Color.White,
          focusedBorderColor = Color(0xFF8A2BE2),
          unfocusedBorderColor = Color(0xFF262040)
        ),
        singleLine = true
      )

      if (filteredCommands.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = if (searchQuery.isBlank()) "No custom commands yet.\nTap '+' to create your first voice command." else "No commands match '$searchQuery'",
            color = Color.Gray,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
          )
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(filteredCommands, key = { it.id }) { cmd ->
            CustomCommandCard(
              command = cmd,
              onToggle = { viewModel.toggleCustomCommand(cmd) },
              onEdit = {
                commandToEdit = cmd
                showEditorDialog = true
              },
              onDelete = { viewModel.deleteCustomCommand(cmd) },
              onTest = { viewModel.testCustomCommand(cmd) }
            )
          }
          item {
            Spacer(modifier = Modifier.height(72.dp))
          }
        }
      }
    }
  }

  if (showEditorDialog) {
    CommandEditorDialog(
      initialCommand = commandToEdit,
      onSave = { newCmd ->
        if (commandToEdit == null) {
          viewModel.addCustomCommand(newCmd)
        } else {
          viewModel.updateCustomCommand(newCmd.copy(id = commandToEdit!!.id))
        }
        showEditorDialog = false
      },
      onDismiss = { showEditorDialog = false }
    )
  }
}

@Composable
fun CustomCommandCard(
  command: CustomCommand,
  onToggle: () -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
  onTest: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = Color(0xFF131024)
    ),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("command_card_${command.id}")
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "\"${command.commandPhrase}\"",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
          )
          if (command.alternativePhrases.isNotBlank()) {
            Text(
              text = "Also: ${command.alternativePhrases}",
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFFB388FF),
              maxLines = 1
            )
          }
        }
        Switch(
          checked = command.isEnabled,
          onCheckedChange = { onToggle() },
          colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = Color(0xFF8A2BE2),
            uncheckedThumbColor = Color.Gray,
            uncheckedTrackColor = Color(0xFF262040)
          ),
          modifier = Modifier.testTag("toggle_command_${command.id}")
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = Color(0xFF262040)
        ) {
          Text(
            text = "${command.actionType}: ${command.actionConfig}",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFE1BEE7),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            maxLines = 1
          )
        }

        Row {
          IconButton(onClick = onTest, modifier = Modifier.size(32.dp)) {
            Icon(
              Icons.Default.PlayArrow,
              contentDescription = "Test Command",
              tint = Color(0xFF00E5FF),
              modifier = Modifier.size(20.dp)
            )
          }
          IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
            Icon(
              Icons.Default.Edit,
              contentDescription = "Edit Command",
              tint = Color.White,
              modifier = Modifier.size(18.dp)
            )
          }
          IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(
              Icons.Default.Delete,
              contentDescription = "Delete Command",
              tint = Color(0xFFFF5252),
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandEditorDialog(
  initialCommand: CustomCommand?,
  onSave: (CustomCommand) -> Unit,
  onDismiss: () -> Unit
) {
  var phrase by remember { mutableStateOf(initialCommand?.commandPhrase ?: "") }
  var altPhrases by remember { mutableStateOf(initialCommand?.alternativePhrases ?: "") }
  var actionType by remember { mutableStateOf(initialCommand?.actionType ?: "OPEN_DESTINATION") }
  var actionConfig by remember { mutableStateOf(initialCommand?.actionConfig ?: "instagram_reels") }
  var requiresConfirmation by remember { mutableStateOf(initialCommand?.requiresConfirmation ?: false) }

  val actionTypes = listOf(
    "OPEN_DESTINATION" to "Open App Destination (e.g. Reels, Shorts)",
    "OPEN_APP" to "Open App",
    "OPEN_URL" to "Open Web URL",
    "CALL_CONTACT" to "Call Contact",
    "SEND_MESSAGE" to "Send Message"
  )
  var expandedTypeDropdown by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = Color(0xFF131024),
    titleContentColor = Color.White,
    textContentColor = Color(0xFFE1BEE7),
    title = {
      Text(if (initialCommand == null) "New Custom Command" else "Edit Custom Command")
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = phrase,
          onValueChange = { phrase = it },
          label = { Text("Command Phrase") },
          placeholder = { Text("e.g. Open my favourite reels") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )

        OutlinedTextField(
          value = altPhrases,
          onValueChange = { altPhrases = it },
          label = { Text("Alternative Phrases (comma separated)") },
          placeholder = { Text("e.g. favourite reels, meri reels") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )

        ExposedDropdownMenuBox(
          expanded = expandedTypeDropdown,
          onExpandedChange = { expandedTypeDropdown = it }
        ) {
          OutlinedTextField(
            value = actionType,
            onValueChange = {},
            readOnly = true,
            label = { Text("Action Type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTypeDropdown) },
            modifier = Modifier
              .fillMaxWidth()
              .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable)
          )
          ExposedDropdownMenu(
            expanded = expandedTypeDropdown,
            onDismissRequest = { expandedTypeDropdown = false }
          ) {
            actionTypes.forEach { (typeKey, typeLabel) ->
              DropdownMenuItem(
                text = { Text(typeLabel) },
                onClick = {
                  actionType = typeKey
                  if (typeKey == "OPEN_DESTINATION" && actionConfig.isBlank()) {
                    actionConfig = "instagram_reels"
                  }
                  expandedTypeDropdown = false
                }
              )
            }
          }
        }

        OutlinedTextField(
          value = actionConfig,
          onValueChange = { actionConfig = it },
          label = { Text("Action Configuration") },
          placeholder = { Text("e.g. instagram_reels or YouTube") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          if (phrase.isNotBlank()) {
            onSave(
              CustomCommand(
                id = initialCommand?.id ?: 0,
                commandPhrase = phrase.trim(),
                alternativePhrases = altPhrases.trim(),
                actionType = actionType,
                actionConfig = actionConfig.trim(),
                requiresConfirmation = requiresConfirmation,
                isEnabled = true
              )
            )
          }
        },
        modifier = Modifier.testTag("save_custom_command_button")
      ) {
        Text("Save", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = Color.Gray)
      }
    }
  )
}
