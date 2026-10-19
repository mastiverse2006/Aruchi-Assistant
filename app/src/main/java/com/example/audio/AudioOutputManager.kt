package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sqrt

class AudioOutputManager {

  companion object {
    private const val TAG = "AudioOutputManager"
    const val SAMPLE_RATE = 24000
    const val CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO
    const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
  }

  private val _outputAmplitude = MutableStateFlow(0f)
  val outputAmplitude: StateFlow<Float> = _outputAmplitude.asStateFlow()

  private val _isPlaying = MutableStateFlow(false)
  val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

  private var audioTrack: AudioTrack? = null
  private val audioQueue = LinkedBlockingQueue<ByteArray>()
  private val isRunning = AtomicBoolean(false)
  private var playbackThread: Thread? = null

  init {
    initAudioTrack()
    startPlaybackLoop()
  }

  private fun initAudioTrack() {
    try {
      val minBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
      val bufferSize = (minBufferSize * 4).coerceAtLeast(8192)

      audioTrack = AudioTrack.Builder()
        .setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        )
        .setAudioFormat(
          AudioFormat.Builder()
            .setEncoding(AUDIO_FORMAT)
            .setSampleRate(SAMPLE_RATE)
            .setChannelMask(CHANNEL_CONFIG)
            .build()
        )
        .setBufferSizeInBytes(bufferSize)
        .setTransferMode(AudioTrack.MODE_STREAM)
        .build()

      audioTrack?.play()
    } catch (e: Exception) {
      Log.e(TAG, "Failed to initialize AudioTrack", e)
    }
  }

  private fun startPlaybackLoop() {
    isRunning.set(true)
    playbackThread = Thread({
      while (isRunning.get()) {
        try {
          val chunk = audioQueue.take()
          if (chunk.isNotEmpty()) {
            _isPlaying.value = true
            calculateAndEmitAmplitude(chunk)

            val track = audioTrack
            if (track != null && track.playState == AudioTrack.PLAYSTATE_PLAYING) {
              track.write(chunk, 0, chunk.size)
            }
          }
          if (audioQueue.isEmpty()) {
            _isPlaying.value = false
            _outputAmplitude.value = 0f
          }
        } catch (_: InterruptedException) {
          // Thread interrupted for shutdown or flush
        } catch (e: Exception) {
          Log.e(TAG, "Error in audio playback loop", e)
        }
      }
    }, "ArushiAudioPlayback").apply {
      priority = Thread.MAX_PRIORITY
      start()
    }
  }

  /**
   * Enqueues incoming PCM 24kHz audio from Gemini Live.
   */
  fun writeAudioChunk(pcmChunk: ByteArray) {
    if (isRunning.get() && pcmChunk.isNotEmpty()) {
      audioQueue.offer(pcmChunk)
    }
  }

  /**
   * Flushes queue and AudioTrack immediately when user barges in or server flags interruption.
   */
  fun clearAndInterrupt() {
    audioQueue.clear()
    try {
      audioTrack?.let { track ->
        if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
          track.pause()
          track.flush()
          track.play()
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error flushing AudioTrack", e)
    }
    _isPlaying.value = false
    _outputAmplitude.value = 0f
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
    // Normalize roughly to 0.0 .. 1.0 range
    val normalized = (rms / 8000.0).toFloat().coerceIn(0f, 1f)
    _outputAmplitude.value = normalized
  }

  fun release() {
    isRunning.set(false)
    playbackThread?.interrupt()
    playbackThread = null
    audioQueue.clear()
    try {
      audioTrack?.stop()
      audioTrack?.release()
    } catch (e: Exception) {
      Log.e(TAG, "Error releasing AudioTrack", e)
    }
    audioTrack = null
    _isPlaying.value = false
    _outputAmplitude.value = 0f
  }
}
