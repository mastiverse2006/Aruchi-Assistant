package com.example.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sqrt

class AudioInputManager(
  private val context: Context,
  private val onAudioChunk: (ByteArray) -> Unit
) {

  companion object {
    private const val TAG = "AudioInputManager"
    const val SAMPLE_RATE = 16000
    const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    private const val CHUNK_SIZE = 2048
  }

  private val _inputAmplitude = MutableStateFlow(0f)
  val inputAmplitude: StateFlow<Float> = _inputAmplitude.asStateFlow()

  private val _isRecording = MutableStateFlow(false)
  val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

  private val _isMuted = MutableStateFlow(false)
  val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

  private var audioRecord: AudioRecord? = null
  private val isRunning = AtomicBoolean(false)
  private var recordThread: Thread? = null

  @SuppressLint("MissingPermission")
  fun startRecording(): Boolean {
    if (isRunning.get()) return true

    val hasMicPermission = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    if (!hasMicPermission) {
      Log.w(TAG, "RECORD_AUDIO permission not granted.")
      return false
    }

    try {
      val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
      val bufferSize = minBufferSize.coerceAtLeast(CHUNK_SIZE * 2)

      audioRecord = AudioRecord(
        MediaRecorder.AudioSource.VOICE_RECOGNITION,
        SAMPLE_RATE,
        CHANNEL_CONFIG,
        AUDIO_FORMAT,
        bufferSize
      )

      if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
        Log.e(TAG, "AudioRecord could not be initialized.")
        audioRecord?.release()
        audioRecord = null
        return false
      }

      audioRecord?.startRecording()
      isRunning.set(true)
      _isRecording.value = true

      recordThread = Thread({
        val buffer = ByteArray(CHUNK_SIZE)
        while (isRunning.get()) {
          val readBytes = audioRecord?.read(buffer, 0, buffer.size) ?: -1
          if (readBytes > 0) {
            val actualData = if (readBytes == buffer.size) buffer.copyOf() else buffer.copyOf(readBytes)
            if (_isMuted.value) {
              _inputAmplitude.value = 0f
            } else {
              calculateAndEmitAmplitude(actualData)
              onAudioChunk(actualData)
            }
          }
        }
      }, "ArushiAudioRecord").apply {
        priority = Thread.MAX_PRIORITY
        start()
      }

      return true
    } catch (e: Exception) {
      Log.e(TAG, "Failed to start recording", e)
      stopRecording()
      return false
    }
  }

  fun setMuted(muted: Boolean) {
    _isMuted.value = muted
    if (muted) {
      _inputAmplitude.value = 0f
    }
  }

  fun toggleMute(): Boolean {
    val newState = !_isMuted.value
    setMuted(newState)
    return newState
  }

  fun stopRecording() {
    isRunning.set(false)
    recordThread?.interrupt()
    recordThread = null
    try {
      audioRecord?.stop()
      audioRecord?.release()
    } catch (e: Exception) {
      Log.e(TAG, "Error stopping AudioRecord", e)
    }
    audioRecord = null
    _isRecording.value = false
    _inputAmplitude.value = 0f
  }

  private fun calculateAndEmitAmplitude(pcmData: ByteArray) {
    if (pcmData.size < 2) return
    var sum = 0.0
    val sampleCount = pcmData.size / 2
    for (i in 0 until sampleCount) {
      val byteL = pcmData[i * 2].toInt() and 0xFF
      val byteH = pcmData[i * 2 + 1].toInt()
      val sample = (byteH shl 8) or byteL
      sum += sample * sample
    }
    val rms = sqrt(sum / sampleCount)
    val normalized = (rms / 6000.0).toFloat().coerceIn(0f, 1f)
    _inputAmplitude.value = normalized
  }
}
