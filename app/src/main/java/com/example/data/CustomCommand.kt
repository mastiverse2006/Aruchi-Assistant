package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_commands")
data class CustomCommand(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val commandPhrase: String,
  val alternativePhrases: String = "",
  val actionType: String, // "OPEN_APP", "OPEN_URL", "OPEN_DESTINATION", "CALL_CONTACT", "SEND_MESSAGE", "SAY_TEXT"
  val actionConfig: String,
  val requiresConfirmation: Boolean = false,
  val isEnabled: Boolean = true,
  val createdAt: Long = System.currentTimeMillis()
)
