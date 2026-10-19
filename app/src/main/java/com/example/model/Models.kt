package com.example.model

import java.util.UUID

/**
 * State of the Gemini Live WebSocket connection.
 */
sealed class LiveConnectionState {
  object Disconnected : LiveConnectionState()
  object Connecting : LiveConnectionState()
  object Connected : LiveConnectionState()
  data class Error(val message: String) : LiveConnectionState()
}

/**
 * High level state of Arushi assistant.
 */
sealed class AssistantState {
  object Idle : AssistantState()
  object Listening : AssistantState()
  object Thinking : AssistantState()
  object Speaking : AssistantState()
  data class ExecutingAction(val actionName: String) : AssistantState()
}

/**
 * Types of icons for device actions.
 */
enum class ActionIconType {
  WHATSAPP,
  CALL,
  APP,
  URL,
  SETTINGS
}

/**
 * Represents a contact matching a name query.
 */
data class ContactMatch(
  val name: String,
  val number: String,
  val type: String = "Mobile"
)

/**
 * Result of executing a device action.
 */
data class ActionResult(
  val success: Boolean,
  val message: String,
  val actionType: ActionIconType,
  val detail: String = ""
)

/**
 * Items displayed in the live conversation and action feed.
 */
sealed class ConversationItem {
  abstract val id: String
  abstract val timestamp: Long

  data class UserTranscript(
    override val id: String = UUID.randomUUID().toString(),
    val text: String,
    override val timestamp: Long = System.currentTimeMillis()
  ) : ConversationItem()

  data class ArushiTranscript(
    override val id: String = UUID.randomUUID().toString(),
    val text: String,
    override val timestamp: Long = System.currentTimeMillis()
  ) : ConversationItem()

  data class ActionExecuted(
    override val id: String = UUID.randomUUID().toString(),
    val action: String,
    val detail: String,
    val success: Boolean,
    val iconType: ActionIconType,
    override val timestamp: Long = System.currentTimeMillis()
  ) : ConversationItem()

  data class ContactDisambiguation(
    override val id: String = UUID.randomUUID().toString(),
    val query: String,
    val contacts: List<ContactMatch>,
    override val timestamp: Long = System.currentTimeMillis()
  ) : ConversationItem()
}
