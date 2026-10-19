package com.example.live

import android.util.Base64
import android.util.Log
import com.example.model.LiveConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiLiveClient(
  private val scope: CoroutineScope,
  private val onAudioOutput: (ByteArray) -> Unit,
  private val onTranscript: (isUser: Boolean, text: String) -> Unit,
  private val onToolCall: (callId: String, name: String, args: Map<String, Any?>) -> Unit,
  private val onInterrupted: () -> Unit
) {

  companion object {
    private const val TAG = "GeminiLiveClient"
    private const val LIVE_ENDPOINT_BASE =
      "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent"
  }

  private val _connectionState = MutableStateFlow<LiveConnectionState>(LiveConnectionState.Disconnected)
  val connectionState: StateFlow<LiveConnectionState> = _connectionState.asStateFlow()

  private var webSocket: WebSocket? = null
  private val okHttpClient = OkHttpClient.Builder()
    .readTimeout(0, TimeUnit.MILLISECONDS)
    .writeTimeout(0, TimeUnit.MILLISECONDS)
    .pingInterval(10, TimeUnit.SECONDS)
    .build()

  fun connect(apiKey: String) {
    if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
      _connectionState.value = LiveConnectionState.Error("Please set a valid Gemini API Key in Settings.")
      return
    }

    disconnect()
    _connectionState.value = LiveConnectionState.Connecting

    val url = "$LIVE_ENDPOINT_BASE?key=$apiKey"
    val request = Request.Builder().url(url).build()

    webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
      override fun onOpen(webSocket: WebSocket, response: Response) {
        Log.i(TAG, "WebSocket connected successfully.")
        _connectionState.value = LiveConnectionState.Connected
        sendInitialSetup()
      }

      override fun onMessage(webSocket: WebSocket, text: String) {
        handleIncomingMessage(text)
      }

      override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        handleIncomingMessage(bytes.utf8())
      }

      override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        Log.i(TAG, "WebSocket closing: $code / $reason")
      }

      override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        Log.i(TAG, "WebSocket closed: $code / $reason")
        _connectionState.value = LiveConnectionState.Disconnected
      }

      override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        val errorMsg = t.localizedMessage ?: "WebSocket error"
        Log.e(TAG, "WebSocket failure: $errorMsg", t)
        _connectionState.value = LiveConnectionState.Error(errorMsg)
      }
    })
  }

  fun disconnect() {
    try {
      webSocket?.close(1000, "User disconnected")
    } catch (e: Exception) {
      Log.e(TAG, "Error closing WebSocket", e)
    }
    webSocket = null
    _connectionState.value = LiveConnectionState.Disconnected
  }

  /**
   * Streams audio data from the microphone to Gemini Live.
   */
  fun sendAudioChunk(pcmChunk: ByteArray) {
    val ws = webSocket ?: return
    if (_connectionState.value !is LiveConnectionState.Connected) return

    val base64Audio = Base64.encodeToString(pcmChunk, Base64.NO_WRAP)
    try {
      val json = JSONObject().apply {
        put("realtimeInput", JSONObject().apply {
          val mediaChunksArray = JSONArray().apply {
            put(JSONObject().apply {
              put("mimeType", "audio/pcm;rate=16000")
              put("data", base64Audio)
            })
          }
          put("mediaChunks", mediaChunksArray)
        })
      }
      ws.send(json.toString())
    } catch (e: Exception) {
      Log.e(TAG, "Error sending audio chunk", e)
    }
  }

  /**
   * Sends text prompt to Gemini Live (for quick prompts or text input).
   */
  fun sendTextPrompt(text: String) {
    val ws = webSocket ?: return
    if (_connectionState.value !is LiveConnectionState.Connected) return

    try {
      val json = JSONObject().apply {
        put("clientContent", JSONObject().apply {
          val turnsArray = JSONArray().apply {
            put(JSONObject().apply {
              put("role", "user")
              val partsArray = JSONArray().apply {
                put(JSONObject().apply {
                  put("text", text)
                })
              }
              put("parts", partsArray)
            })
          }
          put("turns", turnsArray)
          put("turnComplete", true)
        })
      }
      ws.send(json.toString())
    } catch (e: Exception) {
      Log.e(TAG, "Error sending text prompt", e)
    }
  }

  /**
   * Sends function execution response back to Gemini Live.
   */
  fun sendToolResponse(callId: String, resultMessage: String, isSuccess: Boolean) {
    val ws = webSocket ?: return
    try {
      val json = JSONObject().apply {
        put("toolResponse", JSONObject().apply {
          val responses = JSONArray().apply {
            put(JSONObject().apply {
              put("id", callId)
              put("response", JSONObject().apply {
                put("output", JSONObject().apply {
                  put("status", if (isSuccess) "success" else "error")
                  put("result", resultMessage)
                })
              })
            })
          }
          put("functionResponses", responses)
        })
      }
      ws.send(json.toString())
      Log.i(TAG, "Sent toolResponse for $callId: $resultMessage")
    } catch (e: Exception) {
      Log.e(TAG, "Error sending toolResponse", e)
    }
  }

  private fun sendInitialSetup() {
    val ws = webSocket ?: return

    val systemPrompt = """
      You are Arushi, an intelligent, warm, attentive, and helpful Indian personal AI voice assistant.
      
      ## Multi-Language Voice Support:
      - You understand and speak fluently in Hindi, English, Hinglish, Marathi, Gujarati, Bengali, Tamil, Telugu, Kannada, Malayalam, Punjabi, Urdu, and other languages supported by Gemini Live.
      - AUTOMATICALLY detect the language the user is speaking.
      - If user speaks Hindi, respond in natural, polite Hindi.
      - If user speaks English, respond in English.
      - If user speaks Hinglish, respond in natural, friendly Hinglish (e.g. "Haan zaroor!", "Main abhi call connect kar rahi hoon", "Done, open kar diya!").
      - Automatically switch languages mid-conversation if the user switches languages.
      - Never ask the user to manually select or configure a language.
      
      ## Device Actions & App Control (Function Calling):
      You MUST execute device tools when the user requests an action:
      - openWhatsApp(): Call immediately when the user asks to open WhatsApp ("WhatsApp kholo", "Open WhatsApp", "WhatsApp open karo", "Open my WhatsApp", "WhatsApp chalao").
      - openApp(appName): Call when user asks to open an app like YouTube ("Open YouTube", "YouTube chalao"), Instagram ("Open Instagram"), Chrome, Settings ("Open Settings", "Phone ki settings kholo"), Camera, etc.
      - openUrl(url): Call when user asks to open a website or URL link.
      - makeCall(phoneNumber): Call when user provides a phone number to dial ("Call 9876543210", "9876543210 par phone lagao").
      - callContact(contactName): Call when user asks to call someone by name ("Call Mom", "Mummy ko call karo", "Call Rahul", "Rahul ko phone lagao", "Call Dad").
      
      CRITICAL INSTRUCTION:
      Always trigger the tool call immediately. Never merely say "I am opening WhatsApp" without invoking openWhatsApp().
      When the tool execution response is returned to you, give a short, warm, natural spoken confirmation in the user's spoken language.
    """.trimIndent()

    try {
      val toolsArray = JSONArray().apply {
        put(JSONObject().apply {
          val functionDeclarations = JSONArray().apply {
            // openWhatsApp
            put(JSONObject().apply {
              put("name", "openWhatsApp")
              put("description", "Opens WhatsApp application on the device.")
            })
            // openApp
            put(JSONObject().apply {
              put("name", "openApp")
              put("description", "Opens an installed Android app by name, e.g. YouTube, Instagram, Settings, Chrome, Camera.")
              put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                  put("appName", JSONObject().apply {
                    put("type", "STRING")
                    put("description", "The name of the application to open, e.g. YouTube, Instagram, Settings, Chrome")
                  })
                })
                put("required", JSONArray().apply { put("appName") })
              })
            })
            // openUrl
            put(JSONObject().apply {
              put("name", "openUrl")
              put("description", "Opens a website URL in the browser.")
              put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                  put("url", JSONObject().apply {
                    put("type", "STRING")
                    put("description", "The URL to open, starting with https://")
                  })
                })
                put("required", JSONArray().apply { put("url") })
              })
            })
            // makeCall
            put(JSONObject().apply {
              put("name", "makeCall")
              put("description", "Places a phone call or opens the dialer for the given phone number.")
              put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                  put("phoneNumber", JSONObject().apply {
                    put("type", "STRING")
                    put("description", "The phone number to call, e.g. 9876543210")
                  })
                })
                put("required", JSONArray().apply { put("phoneNumber") })
              })
            })
            // callContact
            put(JSONObject().apply {
              put("name", "callContact")
              put("description", "Searches the device contacts by name and initiates a phone call.")
              put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                  put("contactName", JSONObject().apply {
                    put("type", "STRING")
                    put("description", "The name of the contact to call, e.g. Mom, Mummy, Rahul, Dad")
                  })
                })
                put("required", JSONArray().apply { put("contactName") })
              })
            })
          }
          put("functionDeclarations", functionDeclarations)
        })
      }

      val setupJson = JSONObject().apply {
        put("setup", JSONObject().apply {
          put("model", "models/gemini-2.0-flash-exp")
          put("generationConfig", JSONObject().apply {
            put("responseModalities", JSONArray().apply { put("AUDIO") })
            put("speechConfig", JSONObject().apply {
              put("voiceConfig", JSONObject().apply {
                put("prebuiltVoiceConfig", JSONObject().apply {
                  put("voiceName", "Aoede")
                })
              })
            })
          })
          put("systemInstruction", JSONObject().apply {
            put("parts", JSONArray().apply {
              put(JSONObject().apply {
                put("text", systemPrompt)
              })
            })
          })
          put("tools", toolsArray)
        })
      }

      ws.send(setupJson.toString())
      Log.i(TAG, "Sent setup packet to Gemini Live.")
    } catch (e: Exception) {
      Log.e(TAG, "Error constructing setup JSON", e)
    }
  }

  private fun handleIncomingMessage(text: String) {
    try {
      val root = JSONObject(text)

      // Check toolCall
      if (root.has("toolCall")) {
        val toolCall = root.getJSONObject("toolCall")
        val functionCalls = toolCall.optJSONArray("functionCalls")
        if (functionCalls != null) {
          for (i in 0 until functionCalls.length()) {
            val callObj = functionCalls.getJSONObject(i)
            val callId = callObj.optString("id", "call_$i")
            val name = callObj.optString("name")
            val argsObj = callObj.optJSONObject("args")
            val argsMap = mutableMapOf<String, Any?>()
            if (argsObj != null) {
              val keys = argsObj.keys()
              while (keys.hasNext()) {
                val key = keys.next()
                argsMap[key] = argsObj.opt(key)
              }
            }
            scope.launch(Dispatchers.Main) {
              onToolCall(callId, name, argsMap)
            }
          }
        }
      }

      // Check serverContent
      if (root.has("serverContent")) {
        val serverContent = root.getJSONObject("serverContent")

        // Interrupted check (user barge-in)
        if (serverContent.optBoolean("interrupted", false)) {
          Log.i(TAG, "Server flagged interruption (barge-in).")
          scope.launch(Dispatchers.Main) {
            onInterrupted()
          }
        }

        // Model turn parts
        val modelTurn = serverContent.optJSONObject("modelTurn")
        if (modelTurn != null) {
          val parts = modelTurn.optJSONArray("parts")
          if (parts != null) {
            for (i in 0 until parts.length()) {
              val part = parts.getJSONObject(i)

              // Text transcript
              if (part.has("text")) {
                val transcriptText = part.getString("text")
                if (transcriptText.isNotBlank()) {
                  scope.launch(Dispatchers.Main) {
                    onTranscript(false, transcriptText)
                  }
                }
              }

              // Audio data
              if (part.has("inlineData")) {
                val inlineData = part.getJSONObject("inlineData")
                val base64Data = inlineData.optString("data")
                if (base64Data.isNotEmpty()) {
                  val rawPcm = Base64.decode(base64Data, Base64.DEFAULT)
                  onAudioOutput(rawPcm)
                }
              }
            }
          }
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error parsing incoming Gemini Live message", e)
    }
  }
}
