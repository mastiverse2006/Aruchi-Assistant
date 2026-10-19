package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.audio.AudioInputManager
import com.example.audio.AudioOutputManager
import com.example.bridge.DeviceActionManager
import com.example.live.GeminiLiveClient
import com.example.model.ActionIconType
import com.example.model.ActionResult
import com.example.model.AssistantState
import com.example.model.ContactMatch
import com.example.model.ConversationItem
import com.example.model.LiveConnectionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ArushiViewModel(application: Application) : AndroidViewModel(application) {

  companion object {
    private const val TAG = "ArushiViewModel"
    private const val PREFS_NAME = "arushi_prefs"
    private const val PREF_API_KEY = "custom_gemini_api_key"
  }

  private val prefs: SharedPreferences =
    application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  private val deviceActionManager = DeviceActionManager(application)
  private val audioOutputManager = AudioOutputManager()

  private lateinit var audioInputManager: AudioInputManager
  private lateinit var geminiLiveClient: GeminiLiveClient

  // API Key State
  private val _apiKey = MutableStateFlow(loadApiKey())
  val apiKey: StateFlow<String> = _apiKey.asStateFlow()

  // Connection & Assistant States
  val connectionState: StateFlow<LiveConnectionState> get() = geminiLiveClient.connectionState

  private val _assistantState = MutableStateFlow<AssistantState>(AssistantState.Idle)
  val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

  private val _isLiveActive = MutableStateFlow(false)
  val isLiveActive: StateFlow<Boolean> = _isLiveActive.asStateFlow()

  val isMuted: StateFlow<Boolean> get() = audioInputManager.isMuted

  // Smoothed visualization amplitude (0.0 .. 1.0)
  private val _visualizerAmplitude = MutableStateFlow(0f)
  val visualizerAmplitude: StateFlow<Float> = _visualizerAmplitude.asStateFlow()

  // Conversation & Events
  private val _conversationItems = MutableStateFlow<List<ConversationItem>>(emptyList())
  val conversationItems: StateFlow<List<ConversationItem>> = _conversationItems.asStateFlow()

  // Contact Disambiguation Dialog
  private val _activeDisambiguation = MutableStateFlow<ConversationItem.ContactDisambiguation?>(null)
  val activeDisambiguation: StateFlow<ConversationItem.ContactDisambiguation?> = _activeDisambiguation.asStateFlow()

  init {
    initClients()
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

  fun saveApiKey(newKey: String) {
    val trimmed = newKey.trim()
    prefs.edit().putString(PREF_API_KEY, trimmed).apply()
    _apiKey.value = trimmed
    if (_isLiveActive.value) {
      stopSession()
      startSession()
    }
  }

  private fun initClients() {
    geminiLiveClient = GeminiLiveClient(
      scope = viewModelScope,
      onAudioOutput = { pcmChunk ->
        _assistantState.value = AssistantState.Speaking
        audioOutputManager.writeAudioChunk(pcmChunk)
      },
      onTranscript = { isUser, text ->
        addTranscript(isUser, text)
      },
      onToolCall = { callId, name, args ->
        handleToolCall(callId, name, args)
      },
      onInterrupted = {
        // User barged in: stop audio playback immediately
        audioOutputManager.clearAndInterrupt()
        _assistantState.value = AssistantState.Listening
      }
    )

    audioInputManager = AudioInputManager(getApplication()) { pcmChunk ->
      // When mic captures user speech, send to Gemini Live
      if (_isLiveActive.value && !audioInputManager.isMuted.value) {
        if (_assistantState.value != AssistantState.Speaking && _assistantState.value !is AssistantState.ExecutingAction) {
          _assistantState.value = AssistantState.Listening
        }
        geminiLiveClient.sendAudioChunk(pcmChunk)
      }
    }
  }

  private fun observeAmplitudes() {
    viewModelScope.launch {
      audioOutputManager.outputAmplitude.collect { outAmp ->
        if (outAmp > 0.05f) {
          _visualizerAmplitude.value = outAmp
          _assistantState.value = AssistantState.Speaking
        } else if (_isLiveActive.value && !audioInputManager.isMuted.value) {
          val inAmp = audioInputManager.inputAmplitude.value
          _visualizerAmplitude.value = inAmp
          if (inAmp > 0.08f) {
            _assistantState.value = AssistantState.Listening
          }
        } else {
          _visualizerAmplitude.value = 0f
        }
      }
    }

    viewModelScope.launch {
      audioInputManager.inputAmplitude.collect { inAmp ->
        if (audioOutputManager.outputAmplitude.value <= 0.05f) {
          _visualizerAmplitude.value = inAmp
          if (_isLiveActive.value && inAmp > 0.08f) {
            _assistantState.value = AssistantState.Listening
          }
        }
      }
    }
  }

  fun toggleLiveSession() {
    if (_isLiveActive.value) {
      stopSession()
    } else {
      startSession()
    }
  }

  fun startSession() {
    val key = _apiKey.value
    if (key.isBlank() || key == "MY_GEMINI_API_KEY") {
      _conversationItems.value = _conversationItems.value + ConversationItem.ArushiTranscript(
        text = "Please set your Gemini API key in Settings to connect to Arushi Live."
      )
      return
    }

    _isLiveActive.value = true
    _assistantState.value = AssistantState.Listening
    geminiLiveClient.connect(key)
    audioInputManager.startRecording()
  }

  fun stopSession() {
    _isLiveActive.value = false
    _assistantState.value = AssistantState.Idle
    audioInputManager.stopRecording()
    audioOutputManager.clearAndInterrupt()
    geminiLiveClient.disconnect()
    _visualizerAmplitude.value = 0f
  }

  fun toggleMute(): Boolean {
    return audioInputManager.toggleMute()
  }

  fun sendPrompt(prompt: String) {
    if (prompt.isBlank()) return

    // Add user prompt to conversation
    _conversationItems.value = _conversationItems.value + ConversationItem.UserTranscript(text = prompt)

    if (!_isLiveActive.value) {
      startSession()
    }

    _assistantState.value = AssistantState.Thinking
    geminiLiveClient.sendTextPrompt(prompt)
  }

  private fun addTranscript(isUser: Boolean, text: String) {
    val current = _conversationItems.value.toMutableList()
    // Group consecutive transcripts from same speaker if within short time
    val last = current.lastOrNull()
    if (isUser) {
      if (last is ConversationItem.UserTranscript) {
        val updated = last.copy(text = "${last.text} $text".trim())
        current[current.lastIndex] = updated
      } else {
        current.add(ConversationItem.UserTranscript(text = text))
      }
    } else {
      _assistantState.value = AssistantState.Speaking
      if (last is ConversationItem.ArushiTranscript) {
        val updated = last.copy(text = "${last.text} $text".trim())
        current[current.lastIndex] = updated
      } else {
        current.add(ConversationItem.ArushiTranscript(text = text))
      }
    }
    _conversationItems.value = current
  }

  private fun handleToolCall(callId: String, name: String, args: Map<String, Any?>) {
    Log.i(TAG, "Executing tool call: $name with args $args")
    _assistantState.value = AssistantState.ExecutingAction(name)

    viewModelScope.launch(Dispatchers.IO) {
      val result: ActionResult = when (name) {
        "openWhatsApp" -> {
          deviceActionManager.openWhatsApp()
        }

        "openApp" -> {
          val appName = args["appName"]?.toString() ?: ""
          deviceActionManager.openApp(appName)
        }

        "openUrl" -> {
          val url = args["url"]?.toString() ?: ""
          deviceActionManager.openUrl(url)
        }

        "makeCall" -> {
          val number = args["phoneNumber"]?.toString() ?: ""
          deviceActionManager.makeCall(number)
        }

        "callContact" -> {
          val contactName = args["contactName"]?.toString() ?: ""
          deviceActionManager.callContact(contactName) { disambiguationList ->
            val disambiguationItem = ConversationItem.ContactDisambiguation(
              query = contactName,
              contacts = disambiguationList
            )
            _activeDisambiguation.value = disambiguationItem
            _conversationItems.value = _conversationItems.value + disambiguationItem
          }
        }

        else -> {
          ActionResult(
            success = false,
            message = "Unknown tool requested: $name",
            actionType = ActionIconType.APP
          )
        }
      }

      // Record executed action in feed
      val actionItem = ConversationItem.ActionExecuted(
        action = name,
        detail = result.detail.ifEmpty { result.message },
        success = result.success,
        iconType = result.actionType
      )
      _conversationItems.value = _conversationItems.value + actionItem

      // Send tool response back to Gemini Live
      geminiLiveClient.sendToolResponse(
        callId = callId,
        resultMessage = result.message,
        isSuccess = result.success
      )

      _assistantState.value = AssistantState.Thinking
    }
  }

  fun selectContactToCall(contact: ContactMatch) {
    _activeDisambiguation.value = null
    viewModelScope.launch(Dispatchers.IO) {
      val res = deviceActionManager.makeCall(contact.number)
      val actionItem = ConversationItem.ActionExecuted(
        action = "callContact",
        detail = "Calling ${contact.name} (${contact.number})",
        success = res.success,
        iconType = ActionIconType.CALL
      )
      _conversationItems.value = _conversationItems.value + actionItem
    }
  }

  fun dismissDisambiguation() {
    _activeDisambiguation.value = null
  }

  fun clearHistory() {
    _conversationItems.value = emptyList()
  }

  override fun onCleared() {
    super.onCleared()
    stopSession()
    audioOutputManager.release()
  }
}
