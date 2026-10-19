package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class DurefishanSpeechRecognizer(
  private val context: Context,
  private val onSpeechResult: (String) -> Unit,
  private val onSpeechPartial: (String) -> Unit,
  private val onError: (String) -> Unit,
  private val onListeningStateChanged: (Boolean) -> Unit
) : RecognitionListener {

  companion object {
    private const val TAG = "DurefishanSpeechRec"
  }

  private var speechRecognizer: SpeechRecognizer? = null

  private val _micAmplitude = MutableStateFlow(0f)
  val micAmplitude: StateFlow<Float> = _micAmplitude.asStateFlow()

  private val _isListening = MutableStateFlow(false)
  val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

  var language: String = "ur" // "ur", "en", "hi"

  fun startListening() {
    if (!SpeechRecognizer.isRecognitionAvailable(context)) {
      onError("Speech recognition service is not available on this device.")
      return
    }

    stopListening()

    try {
      speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
        setRecognitionListener(this@DurefishanSpeechRecognizer)
      }

      val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)

        val targetLocale = when (language) {
          "ur" -> "ur-PK"
          "hi" -> "hi-IN"
          else -> "en-US"
        }
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, targetLocale)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, targetLocale)
        putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
      }

      speechRecognizer?.startListening(intent)
      _isListening.value = true
      onListeningStateChanged(true)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to start speech recognition", e)
      onError("Failed to access speech recognizer: ${e.message}")
    }
  }

  fun stopListening() {
    try {
      speechRecognizer?.stopListening()
      speechRecognizer?.destroy()
    } catch (e: Exception) {
      Log.e(TAG, "Error stopping recognizer", e)
    }
    speechRecognizer = null
    _isListening.value = false
    _micAmplitude.value = 0f
    onListeningStateChanged(false)
  }

  fun cancel() {
    try {
      speechRecognizer?.cancel()
      speechRecognizer?.destroy()
    } catch (e: Exception) {
      Log.e(TAG, "Error cancelling recognizer", e)
    }
    speechRecognizer = null
    _isListening.value = false
    _micAmplitude.value = 0f
    onListeningStateChanged(false)
  }

  // RecognitionListener callbacks
  override fun onReadyForSpeech(params: Bundle?) {
    _isListening.value = true
    onListeningStateChanged(true)
  }

  override fun onBeginningOfSpeech() {
    _isListening.value = true
  }

  override fun onRmsChanged(rmsdB: Float) {
    // Convert typical -2.0 to 10.0 dB to 0.0 .. 1.0 range
    val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
    _micAmplitude.value = normalized
  }

  override fun onBufferReceived(buffer: ByteArray?) {}

  override fun onEndOfSpeech() {
    _micAmplitude.value = 0f
  }

  override fun onError(error: Int) {
    _isListening.value = false
    _micAmplitude.value = 0f
    onListeningStateChanged(false)

    val message = when (error) {
      SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
      SpeechRecognizer.ERROR_CLIENT -> "Client error occurred"
      SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
      SpeechRecognizer.ERROR_NETWORK -> "Network connection error"
      SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timed out"
      SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Please try again."
      SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer is busy"
      SpeechRecognizer.ERROR_SERVER -> "Server error occurred"
      SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
      else -> "Recognition error: $error"
    }

    Log.w(TAG, "SpeechRecognizer error: $error ($message)")
    if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
      onError(message)
    } else {
      onError("Samajh nahi aaya, baraye meherbani dobara boliye.")
    }
  }

  override fun onResults(results: Bundle?) {
    _isListening.value = false
    _micAmplitude.value = 0f
    onListeningStateChanged(false)

    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
    if (!matches.isNullOrEmpty()) {
      val text = matches[0]
      onSpeechResult(text)
    } else {
      onError("Samajh nahi aaya, baraye meherbani dobara boliye.")
    }
  }

  override fun onPartialResults(partialResults: Bundle?) {
    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
    if (!matches.isNullOrEmpty()) {
      onSpeechPartial(matches[0])
    }
  }

  override fun onEvent(eventType: Int, params: Bundle?) {}
}
