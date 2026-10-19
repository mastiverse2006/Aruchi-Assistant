package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.service.DurefishanForegroundService
import com.example.ui.CustomCommandsScreen
import com.example.ui.DurefishanHomeScreen
import com.example.ui.DurefishanSettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.DurefishanViewModel

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      MyApplicationTheme(darkTheme = true) {
        val viewModel: DurefishanViewModel = viewModel()

        var currentScreen by remember { mutableStateOf("home") } // "home", "settings", "custom_commands"

        var hasAudioPermission by remember {
          mutableStateOf(
            ContextCompat.checkSelfPermission(
              this@MainActivity,
              Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
          )
        }

        var hasContactsPermission by remember {
          mutableStateOf(
            ContextCompat.checkSelfPermission(
              this@MainActivity,
              Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
          )
        }

        var hasPhonePermission by remember {
          mutableStateOf(
            ContextCompat.checkSelfPermission(
              this@MainActivity,
              Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED
          )
        }

        val permissionLauncher = rememberLauncherForActivityResult(
          contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { results ->
          hasAudioPermission = results[Manifest.permission.RECORD_AUDIO] ?: hasAudioPermission
          hasContactsPermission = results[Manifest.permission.READ_CONTACTS] ?: hasContactsPermission
          hasPhonePermission = results[Manifest.permission.CALL_PHONE] ?: hasPhonePermission
        }

        // Trigger listening if launched from notification action
        LaunchedEffect(intent) {
          if (intent?.getBooleanExtra(DurefishanForegroundService.EXTRA_START_LISTENING, false) == true) {
            viewModel.startListening()
          }
        }

        Surface(
          modifier = Modifier.fillMaxSize(),
          color = Color(0xFF040208)
        ) {
          when (currentScreen) {
            "home" -> {
              DurefishanHomeScreen(
                viewModel = viewModel,
                hasAudioPermission = hasAudioPermission,
                onRequestPermissions = {
                  val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
                  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    perms.add(Manifest.permission.POST_NOTIFICATIONS)
                  }
                  permissionLauncher.launch(perms.toTypedArray())
                },
                onNavigateToSettings = { currentScreen = "settings" }
              )
            }

            "settings" -> {
              BackHandler { currentScreen = "home" }
              DurefishanSettingsScreen(
                viewModel = viewModel,
                hasAudioPermission = hasAudioPermission,
                hasContactsPermission = hasContactsPermission,
                hasPhonePermission = hasPhonePermission,
                onRequestPermissions = {
                  val perms = mutableListOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.CALL_PHONE
                  )
                  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    perms.add(Manifest.permission.POST_NOTIFICATIONS)
                  }
                  permissionLauncher.launch(perms.toTypedArray())
                },
                onNavigateToCustomCommands = { currentScreen = "custom_commands" },
                onNavigateBack = { currentScreen = "home" }
              )
            }

            "custom_commands" -> {
              BackHandler { currentScreen = "settings" }
              CustomCommandsScreen(
                viewModel = viewModel,
                onNavigateBack = { currentScreen = "settings" }
              )
            }
          }
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
  }
}
