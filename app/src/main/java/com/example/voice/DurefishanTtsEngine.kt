package com.example.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.sin

class DurefishanTtsEngine(
  private val context: Context,
  private val scope: CoroutineScope
) : TextToSpeech.OnInitListener {

  companion object {
    private const val TAG = "DurefishanTtsEngine"
  }

  private var tts: TextToSpeech? = null
  private var isInitialized = false

  private val _isSpeaking = MutableStateFlow(false)
  val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

  private val _speechAmplitude = MutableStateFlow(0f)
  val speechAmplitude: StateFlow<Float> = _speechAmplitude.asStateFlow()

  var speechRate: Float = 1.0f
    set(value) {
      field = value
      tts?.setSpeechRate(value)
    }

  var speechPitch: Float = 1.08f
    set(value) {
      field = value
      tts?.setPitch(value)
    }

  var preferredLanguage: String = "ur" // "ur", "en", "hi"

  private var amplitudeJob: Job? = null

  init {
    tts = TextToSpeech(context.applicationContext, this)
  }

  override fun onInit(status: Int) {
    if (status == TextToSpeech.SUCCESS) {
      isInitialized = true
      configureVoice()
      setupUtteranceListener()
    } else {
      Log.e(TAG, "Failed to initialize Android TextToSpeech.")
    }
  }

  fun configureVoice() {
    val t = tts ?: return
    t.setSpeechRate(speechRate)
    t.setPitch(speechPitch)

    // Try setting appropriate female voice locale
    val targetLocale = when (preferredLanguage) {
      "ur" -> Locale.forLanguageTag("ur-PK")
      "hi" -> Locale.forLanguageTag("hi-IN")
      else -> Locale.forLanguageTag("en-US")
    }

    val res = t.setLanguage(targetLocale)
    if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
      // Fallback to English (India or US) if Urdu TTS data is not installed on device
      t.setLanguage(Locale.forLanguageTag("en-IN"))
    }

    // Attempt to pick a female voice if available on the engine
    try {
      val voices = t.voices
      if (voices != null) {
        val femaleVoice = voices.firstOrNull { voice ->
          val name = voice.name.lowercase()
          (name.contains("female") || name.contains("fem") || name.contains("#female")) &&
              !voice.isNetworkConnectionRequired
        } ?: voices.firstOrNull { voice ->
          voice.name.lowercase().contains("female")
        }

        if (femaleVoice != null) {
          t.voice = femaleVoice
          Log.i(TAG, "Selected female voice: ${femaleVoice.name}")
        }
      }
    } catch (e: Exception) {
      Log.w(TAG, "Could not select custom voice: ${e.message}")
    }
  }

  private fun setupUtteranceListener() {
    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
      override fun onStart(utteranceId: String?) {
        _isSpeaking.value = true
        startAmplitudeSimulation()
      }

      override fun onDone(utteranceId: String?) {
        _isSpeaking.value = false
        stopAmplitudeSimulation()
      }

      @Deprecated("Deprecated in Java")
      override fun onError(utteranceId: String?) {
        _isSpeaking.value = false
        stopAmplitudeSimulation()
      }
    })
  }

  fun speak(text: String, onComplete: (() -> Unit)? = null) {
    if (text.isBlank()) return

    if (!isInitialized) {
      Log.w(TAG, "TTS not initialized yet. Skipping speech.")
      onComplete?.invoke()
      return
    }

    stop()
    val utteranceId = "durefishan_${System.currentTimeMillis()}"
    _isSpeaking.value = true
    startAmplitudeSimulation()

    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
  }

  fun stop() {
    tts?.stop()
    _isSpeaking.value = false
    stopAmplitudeSimulation()
  }

  private fun startAmplitudeSimulation() {
    amplitudeJob?.cancel()
    amplitudeJob = scope.launch(Dispatchers.Default) {
      var step = 0.0
      while (isActive && _isSpeaking.value) {
        // Voice-like natural modulation
        val amp = ((sin(step * 0.4) * 0.3 + sin(step * 0.8) * 0.3 + 0.4)).toFloat().coerceIn(0.15f, 0.95f)
        _speechAmplitude.value = amp
        step += 0.5
        delay(60)
      }
      _speechAmplitude.value = 0f
    }
  }

  private fun stopAmplitudeSimulation() {
    amplitudeJob?.cancel()
    amplitudeJob = null
    _speechAmplitude.value = 0f
  }

  fun shutdown() {
    stop()
    tts?.shutdown()
    tts = null
  }
}
