package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.bridge.DeviceActionManager
import com.example.data.AppDatabase
import com.example.data.CustomCommand
import com.example.model.ActionIconType
import com.example.model.ActionResult
import com.example.model.AssistantState
import com.example.model.ContactMatch
import com.example.model.ConversationItem
import com.example.nlp.DurefishanIntentParser
import com.example.nlp.ParsedIntent
import com.example.service.DurefishanForegroundService
import com.example.voice.DurefishanSpeechRecognizer
import com.example.voice.DurefishanTtsEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DurefishanViewModel(application: Application) : AndroidViewModel(application) {

  companion object {
    private const val TAG = "DurefishanViewModel"
    private const val PREFS_NAME = "durefishan_prefs"
    private const val PREF_GREETING_NAME = "greeting_name"
    private const val PREF_LANGUAGE = "preferred_language"
    private const val PREF_SPEECH_RATE = "speech_rate"
    private const val PREF_SPEECH_PITCH = "speech_pitch"
    private const val PREF_BACKGROUND_ENABLED = "background_listening"
    private const val PREF_MUTED = "is_muted"
    private const val PREF_API_KEY = "custom_gemini_api_key"
  }

  private val prefs: SharedPreferences =
    application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  private val db = AppDatabase.getDatabase(application)
  private val customCommandDao = db.customCommandDao()
  private val deviceActionManager = DeviceActionManager(application)
  private val intentParser = DurefishanIntentParser()

  // Room custom commands flow
  val customCommands: StateFlow<List<CustomCommand>> = customCommandDao.getAllCommands()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Voice Engines
  private val ttsEngine = DurefishanTtsEngine(application, viewModelScope)
  private lateinit var speechRecognizer: DurefishanSpeechRecognizer

  // Settings State
  private val _greetingName = MutableStateFlow(prefs.getString(PREF_GREETING_NAME, "Bilal") ?: "Bilal")
  val greetingName: StateFlow<String> = _greetingName.asStateFlow()

  private val _preferredLanguage = MutableStateFlow(prefs.getString(PREF_LANGUAGE, "ur") ?: "ur")
  val preferredLanguage: StateFlow<String> = _preferredLanguage.asStateFlow()

  private val _speechRate = MutableStateFlow(prefs.getFloat(PREF_SPEECH_RATE, 1.0f))
  val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

  private val _speechPitch = MutableStateFlow(prefs.getFloat(PREF_SPEECH_PITCH, 1.08f))
  val speechPitch: StateFlow<Float> = _speechPitch.asStateFlow()

  private val _isBackgroundListening = MutableStateFlow(prefs.getBoolean(PREF_BACKGROUND_ENABLED, false))
  val isBackgroundListening: StateFlow<Boolean> = _isBackgroundListening.asStateFlow()

  private val _isMuted = MutableStateFlow(prefs.getBoolean(PREF_MUTED, false))
  val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

  private val _apiKey = MutableStateFlow(loadApiKey())
  val apiKey: StateFlow<String> = _apiKey.asStateFlow()

  // Core Assistant States
  private val _assistantState = MutableStateFlow<AssistantState>(AssistantState.Idle)
  val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

  private val _statusMessage = MutableStateFlow("Tap to speak")
  val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

  private val _visualizerAmplitude = MutableStateFlow(0f)
  val visualizerAmplitude: StateFlow<Float> = _visualizerAmplitude.asStateFlow()

  private val _lastSpokenCommand = MutableStateFlow<String?>(null)
  val lastSpokenCommand: StateFlow<String?> = _lastSpokenCommand.asStateFlow()

  private val _lastAssistantResponse = MutableStateFlow<String?>(null)
  val lastAssistantResponse: StateFlow<String?> = _lastAssistantResponse.asStateFlow()

  private val _activeDisambiguation = MutableStateFlow<ConversationItem.ContactDisambiguation?>(null)
  val activeDisambiguation: StateFlow<ConversationItem.ContactDisambiguation?> = _activeDisambiguation.asStateFlow()

  init {
    initSpeechRecognizer()
    applyTtsPreferences()
    observeAmplitudes()
  }

  private fun loadApiKey(): String {
    val saved = prefs.getString(PREF_API_KEY, null)
    if (!saved.isNullOrBlank()) return saved

    val buildConfigKey = try {
      BuildConfig.GEMINI_API_KEY
    } catch (_: Exception) {
      ""
    }
    return if (buildConfigKey.isNotBlank() && buildConfigKey != "MY_GEMINI_API_KEY") {
      buildConfigKey
    } else {
      ""
    }
  }

  private fun initSpeechRecognizer() {
    speechRecognizer = DurefishanSpeechRecognizer(
      context = getApplication(),
      onSpeechResult = { text ->
        processRecognizedText(text)
      },
      onSpeechPartial = { partial ->
        _statusMessage.value = partial
      },
      onError = { errorMsg ->
        _assistantState.value = AssistantState.Idle
        _statusMessage.value = errorMsg
        _visualizerAmplitude.value = 0f
      },
      onListeningStateChanged = { isListening ->
        if (isListening) {
          _assistantState.value = AssistantState.Listening
          _statusMessage.value = "Listening..."
        } else if (_assistantState.value is AssistantState.Listening) {
          _assistantState.value = AssistantState.Idle
          _statusMessage.value = "Tap to speak"
        }
      }
    )
    speechRecognizer.language = _preferredLanguage.value
  }

  private fun applyTtsPreferences() {
    ttsEngine.preferredLanguage = _preferredLanguage.value
    ttsEngine.speechRate = _speechRate.value
    ttsEngine.speechPitch = _speechPitch.value
    ttsEngine.configureVoice()
  }

  private fun observeAmplitudes() {
    viewModelScope.launch {
      speechRecognizer.micAmplitude.collect { micAmp ->
        if (ttsEngine.isSpeaking.value) {
          _visualizerAmplitude.value = ttsEngine.speechAmplitude.value
        } else if (_assistantState.value is AssistantState.Listening && !_isMuted.value) {
          _visualizerAmplitude.value = micAmp
        } else {
          _visualizerAmplitude.value = 0f
        }
      }
    }

    viewModelScope.launch {
      ttsEngine.speechAmplitude.collect { ttsAmp ->
        if (ttsEngine.isSpeaking.value) {
          _visualizerAmplitude.value = ttsAmp
          _assistantState.value = AssistantState.Speaking
        } else if (_assistantState.value is AssistantState.Speaking) {
          _assistantState.value = AssistantState.Idle
          _statusMessage.value = "Tap to speak"
          _visualizerAmplitude.value = 0f
        }
      }
    }
  }

  /**
   * Primary interaction: Tapping the orb starts or cancels voice recognition.
   */
  fun onOrbClicked() {
    if (_isMuted.value) {
      setMuted(false)
    }

    when (_assistantState.value) {
      is AssistantState.Listening -> {
        speechRecognizer.cancel()
        _assistantState.value = AssistantState.Idle
        _statusMessage.value = "Tap to speak"
        _visualizerAmplitude.value = 0f
      }

      is AssistantState.Speaking -> {
        ttsEngine.stop()
        _assistantState.value = AssistantState.Idle
        _statusMessage.value = "Tap to speak"
        _visualizerAmplitude.value = 0f
      }

      else -> {
        startListening()
      }
    }
  }

  fun startListening() {
    if (_isMuted.value) return
    ttsEngine.stop()
    _assistantState.value = AssistantState.Listening
    _statusMessage.value = "Listening..."
    speechRecognizer.startListening()
  }

  fun stopListening() {
    speechRecognizer.stopListening()
    _assistantState.value = AssistantState.Idle
    _statusMessage.value = "Tap to speak"
    _visualizerAmplitude.value = 0f
  }

  fun toggleMute() {
    setMuted(!_isMuted.value)
  }

  fun setMuted(muted: Boolean) {
    _isMuted.value = muted
    prefs.edit().putBoolean(PREF_MUTED, muted).apply()
    if (muted) {
      stopListening()
      ttsEngine.stop()
      _statusMessage.value = "Microphone muted"
    } else {
      _statusMessage.value = "Tap to speak"
    }
  }

  fun setGreetingName(name: String) {
    val trimmed = name.trim().ifEmpty { "Bilal" }
    _greetingName.value = trimmed
    prefs.edit().putString(PREF_GREETING_NAME, trimmed).apply()
  }

  fun setPreferredLanguage(lang: String) {
    _preferredLanguage.value = lang
    prefs.edit().putString(PREF_LANGUAGE, lang).apply()
    speechRecognizer.language = lang
    applyTtsPreferences()
  }

  fun setSpeechRate(rate: Float) {
    _speechRate.value = rate
    prefs.edit().putFloat(PREF_SPEECH_RATE, rate).apply()
    ttsEngine.speechRate = rate
  }

  fun setSpeechPitch(pitch: Float) {
    _speechPitch.value = pitch
    prefs.edit().putFloat(PREF_SPEECH_PITCH, pitch).apply()
    ttsEngine.speechPitch = pitch
  }

  fun setBackgroundListening(enabled: Boolean) {
    _isBackgroundListening.value = enabled
    prefs.edit().putBoolean(PREF_BACKGROUND_ENABLED, enabled).apply()
    if (enabled) {
      DurefishanForegroundService.startService(getApplication())
    } else {
      DurefishanForegroundService.stopService(getApplication())
    }
  }

  fun saveApiKey(newKey: String) {
    val trimmed = newKey.trim()
    prefs.edit().putString(PREF_API_KEY, trimmed).apply()
    _apiKey.value = trimmed
  }

  fun testVoice() {
    val sampleText = when (_preferredLanguage.value) {
      "ur" -> "Haan ji ${_greetingName.value}, main Durefishan hoon. Main aapki kya madad kar sakti hoon?"
      "hi" -> "Namaste ${_greetingName.value}, main Durefishan hoon. Main aapki kya madad kar sakti hoon?"
      else -> "Hello ${_greetingName.value}, I am Durefishan. How can I help you today?"
    }
    speakResponse(sampleText)
  }

  fun processRecognizedText(text: String) {
    val cleanText = text.trim()
    if (cleanText.isEmpty()) return

    _lastSpokenCommand.value = cleanText
    _assistantState.value = AssistantState.Thinking
    _statusMessage.value = "Processing..."

    viewModelScope.launch(Dispatchers.IO) {
      val parsed = intentParser.parse(
        text = cleanText,
        greetingName = _greetingName.value,
        customCommands = customCommands.value
      )

      handleParsedIntent(parsed)
    }
  }

  private suspend fun handleParsedIntent(intent: ParsedIntent) {
    when (intent) {
      is ParsedIntent.Greeting -> {
        speakResponse(intent.message)
      }

      is ParsedIntent.Time -> {
        speakResponse(intent.timeText)
      }

      is ParsedIntent.StopListening -> {
        speakResponse("Theek hai, main sunna band kar rahi hoon.") {
          stopListening()
        }
      }

      is ParsedIntent.Mute -> {
        setMuted(true)
        speakResponse("Main ne khud ko mute kar liya hai.")
      }

      is ParsedIntent.Unmute -> {
        setMuted(false)
        speakResponse("Haan ji, main ab unmuted hoon.")
      }

      is ParsedIntent.OpenInstagram -> {
        deviceActionManager.openInstagram(intent.destination)
        speakResponse(intent.spokenConfirmation)
      }

      is ParsedIntent.OpenYouTube -> {
        deviceActionManager.openYouTube(intent.destination, intent.query)
        speakResponse(intent.spokenConfirmation)
      }

      is ParsedIntent.OpenTikTok -> {
        deviceActionManager.openTikTok()
        speakResponse(intent.spokenConfirmation)
      }

      is ParsedIntent.OpenSnapchat -> {
        deviceActionManager.openSnapchat()
        speakResponse(intent.spokenConfirmation)
      }

      is ParsedIntent.OpenWhatsApp -> {
        if (intent.contactQuery.isNotBlank()) {
          val contacts = deviceActionManager.searchContacts(intent.contactQuery)
          if (contacts.isNotEmpty()) {
            deviceActionManager.openWhatsAppChat(contacts[0].number, intent.message)
            speakResponse("WhatsApp par ${contacts[0].name} ki chat khol rahi hoon.")
          } else {
            deviceActionManager.openWhatsAppChat("", intent.message)
            speakResponse("Contact nahi mila, WhatsApp open kar rahi hoon.")
          }
        } else {
          deviceActionManager.openWhatsApp()
          speakResponse(intent.spokenConfirmation)
        }
      }

      is ParsedIntent.CallContact -> {
        val result = deviceActionManager.callContact(intent.contactName) { disambiguationList ->
          _activeDisambiguation.value = ConversationItem.ContactDisambiguation(
            query = intent.contactName,
            contacts = disambiguationList
          )
        }
        speakResponse(result.message)
      }

      is ParsedIntent.CallNumber -> {
        val result = deviceActionManager.makeCall(intent.phoneNumber)
        speakResponse(result.message)
      }

      is ParsedIntent.SendSms -> {
        val contacts = deviceActionManager.searchContacts(intent.contactNameOrNumber)
        val targetNumber = if (contacts.isNotEmpty()) contacts[0].number else intent.contactNameOrNumber
        val result = deviceActionManager.sendSms(targetNumber, intent.messageBody)
        speakResponse(result.message)
      }

      is ParsedIntent.OpenApp -> {
        val result = deviceActionManager.openApp(intent.appName)
        speakResponse(result.message)
      }

      is ParsedIntent.OpenUrl -> {
        val result = deviceActionManager.openUrl(intent.url)
        speakResponse(result.message)
      }

      is ParsedIntent.CustomAction -> {
        executeCustomCommand(intent.command)
        speakResponse(intent.spokenConfirmation)
      }

      is ParsedIntent.Unknown -> {
        val fallbackMsg = "Main ne suna: \"${intent.rawText}\". Yeh command samajh nahi aayi."
        speakResponse(fallbackMsg)
      }
    }
  }

  private fun executeCustomCommand(command: CustomCommand) {
    when (command.actionType) {
      "OPEN_APP" -> deviceActionManager.openApp(command.actionConfig)
      "OPEN_URL" -> deviceActionManager.openUrl(command.actionConfig)
      "OPEN_DESTINATION" -> {
        when (command.actionConfig) {
          "instagram_reels" -> deviceActionManager.openInstagram("reels")
          "instagram_dm" -> deviceActionManager.openInstagram("direct")
          "youtube_shorts" -> deviceActionManager.openYouTube("shorts")
          else -> deviceActionManager.openApp(command.actionConfig)
        }
      }
      "CALL_CONTACT" -> deviceActionManager.callContact(command.actionConfig) { matches ->
        _activeDisambiguation.value = ConversationItem.ContactDisambiguation(
          query = command.actionConfig,
          contacts = matches
        )
      }
      "SEND_MESSAGE" -> deviceActionManager.openWhatsAppChat("", command.actionConfig)
      else -> deviceActionManager.openApp(command.actionConfig)
    }
  }

  private fun speakResponse(text: String, onComplete: (() -> Unit)? = null) {
    _lastAssistantResponse.value = text
    _assistantState.value = AssistantState.Speaking
    _statusMessage.value = "Speaking..."
    ttsEngine.speak(text) {
      _assistantState.value = AssistantState.Idle
      _statusMessage.value = "Tap to speak"
      onComplete?.invoke()
    }
  }

  fun selectContactToCall(contact: ContactMatch) {
    _activeDisambiguation.value = null
    viewModelScope.launch(Dispatchers.IO) {
      val res = deviceActionManager.makeCall(contact.number)
      speakResponse("Calling ${contact.name} (${contact.number}).")
    }
  }

  fun dismissDisambiguation() {
    _activeDisambiguation.value = null
  }

  // Custom Commands Management
  fun addCustomCommand(command: CustomCommand) {
    viewModelScope.launch(Dispatchers.IO) {
      customCommandDao.insertCommand(command)
    }
  }

  fun updateCustomCommand(command: CustomCommand) {
    viewModelScope.launch(Dispatchers.IO) {
      customCommandDao.updateCommand(command)
    }
  }

  fun deleteCustomCommand(command: CustomCommand) {
    viewModelScope.launch(Dispatchers.IO) {
      customCommandDao.deleteCommand(command)
    }
  }

  fun toggleCustomCommand(command: CustomCommand) {
    viewModelScope.launch(Dispatchers.IO) {
      customCommandDao.updateCommand(command.copy(isEnabled = !command.isEnabled))
    }
  }

  fun testCustomCommand(command: CustomCommand) {
    viewModelScope.launch(Dispatchers.IO) {
      executeCustomCommand(command)
      speakResponse("Testing custom command: ${command.commandPhrase}")
    }
  }

  fun resetSettings() {
    prefs.edit().clear().apply()
    _greetingName.value = "Bilal"
    _preferredLanguage.value = "ur"
    _speechRate.value = 1.0f
    _speechPitch.value = 1.08f
    _isBackgroundListening.value = false
    _isMuted.value = false
    applyTtsPreferences()
  }

  override fun onCleared() {
    super.onCleared()
    speechRecognizer.stopListening()
    ttsEngine.shutdown()
  }
}
