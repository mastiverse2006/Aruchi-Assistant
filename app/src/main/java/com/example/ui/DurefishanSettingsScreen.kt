package com.example.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import com.example.viewmodel.DurefishanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DurefishanSettingsScreen(
  viewModel: DurefishanViewModel,
  hasAudioPermission: Boolean,
  hasContactsPermission: Boolean,
  hasPhonePermission: Boolean,
  onRequestPermissions: () -> Unit,
  onNavigateToCustomCommands: () -> Unit,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val greetingName by viewModel.greetingName.collectAsState()
  val preferredLanguage by viewModel.preferredLanguage.collectAsState()
  val speechRate by viewModel.speechRate.collectAsState()
  val speechPitch by viewModel.speechPitch.collectAsState()
  val isBackgroundListening by viewModel.isBackgroundListening.collectAsState()
  val isMuted by viewModel.isMuted.collectAsState()
  val apiKey by viewModel.apiKey.collectAsState()

  var editingGreetingName by remember { mutableStateOf(greetingName) }
  var showGreetingDialog by remember { mutableStateOf(false) }
  var showApiKeyDialog by remember { mutableStateOf(false) }

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .testTag("settings_screen"),
    containerColor = Color(0xFF07050E),
    topBar = {
      TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = Color(0xFF07050E),
          titleContentColor = Color.White
        ),
        title = {
          Text("Settings", fontWeight = FontWeight.Bold)
        },
        navigationIcon = {
          IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.testTag("settings_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = Color.White
            )
          }
        }
      )
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

      // 1. Identity & Greeting Section
      SettingsSection(title = "Assistant & Greeting", icon = Icons.Default.Person) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { showGreetingDialog = true }
            .padding(12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text("Greeting Name", color = Color.White, fontWeight = FontWeight.SemiBold)
            Text("Responds: \"Haan ji $greetingName, boliye.\"", color = Color(0xFFB388FF), fontSize = 13.sp)
          }
          Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
        }
      }

      // 2. Language Preference
      SettingsSection(title = "Voice Language", icon = Icons.Default.Language) {
        Column(modifier = Modifier.padding(12.dp)) {
          Text(
            "Select default speaking language:",
            color = Color(0xFFE1BEE7),
            fontSize = 13.sp
          )
          Spacer(modifier = Modifier.height(8.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LanguageChip(
              label = "Roman Urdu / Urdu",
              isSelected = preferredLanguage == "ur",
              onSelect = { viewModel.setPreferredLanguage("ur") }
            )
            LanguageChip(
              label = "English",
              isSelected = preferredLanguage == "en",
              onSelect = { viewModel.setPreferredLanguage("en") }
            )
            LanguageChip(
              label = "Hinglish",
              isSelected = preferredLanguage == "hi",
              onSelect = { viewModel.setPreferredLanguage("hi") }
            )
          }
        }
      }

      // 3. Female Voice & Speech Modulation
      SettingsSection(title = "Female Voice & Speed", icon = Icons.Default.Tune) {
        Column(modifier = Modifier.padding(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Speech Rate (${String.format("%.1f", speechRate)}x)", color = Color.White, fontSize = 14.sp)
            Text("Pitch (${String.format("%.2f", speechPitch)})", color = Color(0xFFB388FF), fontSize = 13.sp)
          }

          Slider(
            value = speechRate,
            onValueChange = { viewModel.setSpeechRate(it) },
            valueRange = 0.6f..1.6f,
            colors = SliderDefaults.colors(
              thumbColor = Color(0xFF8A2BE2),
              activeTrackColor = Color(0xFF8A2BE2),
              inactiveTrackColor = Color(0xFF262040)
            )
          )

          Spacer(modifier = Modifier.height(8.dp))

          Button(
            onClick = { viewModel.testVoice() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF262040)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("test_voice_button")
          ) {
            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = Color(0xFF00E5FF))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Test Voice (\"Haan ji $greetingName...\")", color = Color.White)
          }
        }
      }

      // 4. Custom Commands Section
      SettingsSection(title = "Custom Voice Commands", icon = Icons.Default.RecordVoiceOver) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToCustomCommands() }
            .padding(12.dp)
            .testTag("navigate_custom_commands_button"),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text("Manage Custom Commands", color = Color.White, fontWeight = FontWeight.SemiBold)
            Text("Create, edit, toggle, and test your own phrases", color = Color(0xFFB388FF), fontSize = 13.sp)
          }
          Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
        }
      }

      // 5. Background Operation & OS Policy Guidance
      SettingsSection(title = "Background Wake & Service", icon = Icons.Default.NotificationsActive) {
        Column(modifier = Modifier.padding(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Background Assistant Service", color = Color.White, fontWeight = FontWeight.SemiBold)
              Text(
                "Keeps a persistent notification for 1-tap activation",
                color = Color(0xFFB388FF),
                fontSize = 12.sp
              )
            }
            Switch(
              checked = isBackgroundListening,
              onCheckedChange = { viewModel.setBackgroundListening(it) },
              colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF8A2BE2)
              ),
              modifier = Modifier.testTag("background_service_switch")
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF1E1A33),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Android OS Notice: Continuous lock-screen listening requires default Digital Assistant role in system settings. The persistent notification provides instant activation from any app.",
                color = Color(0xFFE0E0E0),
                fontSize = 11.sp,
                lineHeight = 15.sp
              )
            }
          }
        }
      }

      // 6. Connected Apps
      SettingsSection(title = "Supported Apps", icon = Icons.Default.Apps) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          ConnectedAppRow("WhatsApp", "Chats, Voice/Video Call deep links, messages")
          ConnectedAppRow("Instagram", "App, Reels, Direct/DM inbox")
          ConnectedAppRow("YouTube", "App, YouTube Shorts, voice search")
          ConnectedAppRow("TikTok", "App, profiles, and video launcher")
          ConnectedAppRow("Snapchat", "App and camera deep links")
        }
      }

      // 7. Permissions Status & Management
      SettingsSection(title = "Permissions Status", icon = Icons.Default.Security) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          PermissionStatusRow("Microphone (Audio)", hasAudioPermission)
          PermissionStatusRow("Contacts (Search & Call)", hasContactsPermission)
          PermissionStatusRow("Phone Calls (Direct dialing)", hasPhonePermission)

          if (!hasAudioPermission || !hasContactsPermission || !hasPhonePermission) {
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedButton(
              onClick = onRequestPermissions,
              colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text("Grant Missing Permissions")
            }
          }
        }
      }

      // 8. Privacy & Battery Guidance
      SettingsSection(title = "Privacy & Battery Optimization", icon = Icons.Default.BatteryAlert) {
        Column(modifier = Modifier.padding(12.dp)) {
          Text(
            text = "• Durefishan Assistant never records or uploads audio without user consent.\n" +
                "• All natural-language commands and custom rules are evaluated locally on your device for maximum speed and privacy.\n" +
                "• For seamless background responsiveness, exempt Durefishan from aggressive battery optimization in your device Settings.",
            color = Color(0xFFCCC2DC),
            fontSize = 12.sp,
            lineHeight = 18.sp
          )
        }
      }

      // 9. API Key Settings & Reset
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedButton(
          onClick = { showApiKeyDialog = true },
          modifier = Modifier.weight(1f),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
        ) {
          Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("API Key", fontSize = 12.sp)
        }

        OutlinedButton(
          onClick = { viewModel.resetSettings() },
          modifier = Modifier.weight(1f),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252))
        ) {
          Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Reset", fontSize = 12.sp)
        }
      }

      Spacer(modifier = Modifier.height(24.dp))
    }
  }

  // Edit Greeting Name Dialog
  if (showGreetingDialog) {
    AlertDialog(
      onDismissRequest = { showGreetingDialog = false },
      containerColor = Color(0xFF131024),
      title = { Text("Set Greeting Name", color = Color.White) },
      text = {
        OutlinedTextField(
          value = editingGreetingName,
          onValueChange = { editingGreetingName = it },
          label = { Text("Your Name") },
          placeholder = { Text("Bilal") },
          singleLine = true,
          modifier = Modifier.testTag("greeting_name_input")
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            viewModel.setGreetingName(editingGreetingName)
            showGreetingDialog = false
          }
        ) {
          Text("Save", color = Color(0xFF00E5FF))
        }
      },
      dismissButton = {
        TextButton(onClick = { showGreetingDialog = false }) {
          Text("Cancel", color = Color.Gray)
        }
      }
    )
  }

  // API Key Dialog
  if (showApiKeyDialog) {
    var keyInput by remember { mutableStateOf(apiKey) }
    AlertDialog(
      onDismissRequest = { showApiKeyDialog = false },
      containerColor = Color(0xFF131024),
      title = { Text("Gemini Live API Key", color = Color.White) },
      text = {
        Column {
          Text("Optional API key for extended live cloud responses:", color = Color(0xFFB388FF), fontSize = 12.sp)
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = keyInput,
            onValueChange = { keyInput = it },
            label = { Text("API Key") },
            singleLine = true
          )
        }
      },
      confirmButton = {
        TextButton(onClick = {
          viewModel.saveApiKey(keyInput)
          showApiKeyDialog = false
        }) {
          Text("Save", color = Color(0xFF00E5FF))
        }
      },
      dismissButton = {
        TextButton(onClick = { showApiKeyDialog = false }) {
          Text("Cancel", color = Color.Gray)
        }
      }
    )
  }
}

@Composable
fun SettingsSection(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable () -> Unit) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF131024)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFF1C1733))
          .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(icon, contentDescription = null, tint = Color(0xFFB388FF), modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
      }
      content()
    }
  }
}

@Composable
fun LanguageChip(label: String, isSelected: Boolean, onSelect: () -> Unit) {
  FilterChip(
    selected = isSelected,
    onClick = onSelect,
    label = { Text(label, fontSize = 12.sp) },
    colors = FilterChipDefaults.filterChipColors(
      selectedContainerColor = Color(0xFF8A2BE2),
      selectedLabelColor = Color.White,
      containerColor = Color(0xFF1E1A33),
      labelColor = Color(0xFFCCC2DC)
    )
  )
}

@Composable
fun ConnectedAppRow(appName: String, capabilities: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column {
      Text(appName, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
      Text(capabilities, color = Color(0xFFB388FF), fontSize = 11.sp)
    }
    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00E676), modifier = Modifier.size(16.dp))
  }
}

@Composable
fun PermissionStatusRow(name: String, isGranted: Boolean) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(name, color = Color.White, fontSize = 13.sp)
    Surface(
      shape = RoundedCornerShape(8.dp),
      color = if (isGranted) Color(0xFF00E676).copy(alpha = 0.15f) else Color(0xFFFF5252).copy(alpha = 0.15f)
    ) {
      Text(
        text = if (isGranted) "Granted" else "Missing",
        color = if (isGranted) Color(0xFF00E676) else Color(0xFFFF5252),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
      )
    }
  }
}
