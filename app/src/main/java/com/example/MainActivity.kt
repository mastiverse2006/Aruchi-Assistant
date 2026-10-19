package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.ArushiScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.ArushiViewModel

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      MyApplicationTheme {
        val viewModel: ArushiViewModel = viewModel()

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

        LaunchedEffect(Unit) {
          val toRequest = mutableListOf<String>()
          if (!hasAudioPermission) toRequest.add(Manifest.permission.RECORD_AUDIO)
          if (!hasContactsPermission) toRequest.add(Manifest.permission.READ_CONTACTS)
          if (!hasPhonePermission) toRequest.add(Manifest.permission.CALL_PHONE)

          if (toRequest.isNotEmpty()) {
            permissionLauncher.launch(toRequest.toTypedArray())
          }
        }

        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          ArushiScreen(
            viewModel = viewModel,
            hasAudioPermission = hasAudioPermission,
            hasContactsPermission = hasContactsPermission,
            hasPhonePermission = hasPhonePermission,
            onRequestPermissions = {
              permissionLauncher.launch(
                arrayOf(
                  Manifest.permission.RECORD_AUDIO,
                  Manifest.permission.READ_CONTACTS,
                  Manifest.permission.CALL_PHONE
                )
              )
            }
          )
        }
      }
    }
  }
}
