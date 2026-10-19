package com.example.nlp

import com.example.data.CustomCommand
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class ParsedIntent {
  data class Greeting(val message: String) : ParsedIntent()
  data class Time(val timeText: String) : ParsedIntent()
  object StopListening : ParsedIntent()
  object Mute : ParsedIntent()
  object Unmute : ParsedIntent()

  data class OpenApp(val appName: String, val spokenConfirmation: String) : ParsedIntent()
  data class OpenInstagram(val destination: String = "home", val spokenConfirmation: String) : ParsedIntent()
  data class OpenYouTube(val destination: String = "home", val query: String = "", val spokenConfirmation: String) : ParsedIntent()
  data class OpenTikTok(val spokenConfirmation: String) : ParsedIntent()
  data class OpenSnapchat(val spokenConfirmation: String) : ParsedIntent()
  data class OpenWhatsApp(val contactQuery: String = "", val message: String = "", val spokenConfirmation: String) : ParsedIntent()

  data class CallContact(val contactName: String, val spokenConfirmation: String) : ParsedIntent()
  data class CallNumber(val phoneNumber: String, val spokenConfirmation: String) : ParsedIntent()
  data class SendSms(val contactNameOrNumber: String, val messageBody: String, val spokenConfirmation: String) : ParsedIntent()

  data class OpenUrl(val url: String, val spokenConfirmation: String) : ParsedIntent()
  data class CustomAction(val command: CustomCommand, val spokenConfirmation: String) : ParsedIntent()
  data class Unknown(val rawText: String) : ParsedIntent()
}

class DurefishanIntentParser {

  fun parse(text: String, greetingName: String, customCommands: List<CustomCommand>): ParsedIntent {
    val clean = text.trim()
    val lower = clean.lowercase()

    // 1. Check custom commands from Room database first
    for (custom in customCommands) {
      if (!custom.isEnabled) continue
      val phrases = listOf(custom.commandPhrase) + custom.alternativePhrases.split(",").map { it.trim() }
      for (phrase in phrases) {
        if (phrase.isNotBlank() && (lower == phrase.lowercase() || lower.contains(phrase.lowercase()))) {
          return ParsedIntent.CustomAction(
            command = custom,
            spokenConfirmation = "Executing ${custom.commandPhrase}."
          )
        }
      }
    }

    // 2. Greeting / Wake phrase
    if (lower.contains("hey durefishan") ||
        lower.contains("durefishan") ||
        lower.contains("suno durefishan") ||
        lower.contains("assalam") ||
        lower == "hi" ||
        lower == "hello"
    ) {
      if (lower.replace(Regex("hey|durefishan|suno|assalam|alaikum|hi|hello|ji|g"), "").trim().isEmpty()) {
        return ParsedIntent.Greeting("Haan ji $greetingName, boliye. Main sun rahi hoon.")
      }
    }

    // 3. Time inquiries
    if (lower.contains("time") || lower.contains("waqt") || lower.contains("baj rahe") || lower.contains("kitne baje")) {
      val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
      val currentTime = sdf.format(Date())
      return ParsedIntent.Time("Abhi waqt $currentTime hai.")
    }

    // 4. Voice controls
    if (lower.contains("stop listening") || lower.contains("sunna band karo") || lower.contains("band karo")) {
      return ParsedIntent.StopListening
    }
    if (lower.contains("mute yourself") || lower.contains("mute ho jao") || lower.contains("mute karo") || lower == "mute") {
      return ParsedIntent.Mute
    }
    if (lower.contains("unmute yourself") || lower.contains("unmute karo") || lower == "unmute" || lower.contains("awaz kholo")) {
      return ParsedIntent.Unmute
    }

    // 5. Instagram & Reels & DM
    if (lower.contains("instagram") || lower.contains("insta")) {
      return when {
        lower.contains("reels") || lower.contains("reel") -> {
          ParsedIntent.OpenInstagram("reels", "Instagram Reels open kar rahi hoon.")
        }
        lower.contains("dm") || lower.contains("direct") || lower.contains("message") -> {
          ParsedIntent.OpenInstagram("direct", "Instagram DM open kar rahi hoon.")
        }
        else -> {
          ParsedIntent.OpenInstagram("home", "Instagram open kar rahi hoon.")
        }
      }
    }
    if (lower.contains("reels kholo") || lower.contains("reels chalao")) {
      return ParsedIntent.OpenInstagram("reels", "Instagram Reels open kar rahi hoon.")
    }

    // 6. YouTube & Shorts & Search
    if (lower.contains("youtube")) {
      return when {
        lower.contains("shorts") || lower.contains("short") -> {
          ParsedIntent.OpenYouTube("shorts", "", "YouTube Shorts open kar rahi hoon.")
        }
        lower.contains("search") || lower.contains("dhoondo") -> {
          val query = clean.replace(Regex("(?i)open|youtube|par|search|karo|dhoondo"), "").trim()
          ParsedIntent.OpenYouTube("search", query, "YouTube par $query search kar rahi hoon.")
        }
        else -> {
          ParsedIntent.OpenYouTube("home", "", "YouTube open kar rahi hoon.")
        }
      }
    }
    if (lower.contains("shorts kholo") || lower.contains("shorts chalao")) {
      return ParsedIntent.OpenYouTube("shorts", "", "YouTube Shorts open kar rahi hoon.")
    }

    // 7. TikTok & Snapchat
    if (lower.contains("tiktok")) {
      return ParsedIntent.OpenTikTok("TikTok open kar rahi hoon.")
    }
    if (lower.contains("snapchat") || lower.contains("snap")) {
      return ParsedIntent.OpenSnapchat("Snapchat open kar rahi hoon.")
    }

    // 8. WhatsApp & Chats
    if (lower.contains("whatsapp")) {
      // Check if message instruction is present
      val hasMessage = lower.contains("message") || lower.contains("msg") || lower.contains("bhejo") || lower.contains("karo")
      if (hasMessage) {
        // e.g. "WhatsApp par Bilal ko message karo: main aa raha hoon"
        val colonSplit = clean.split(":")
        val msgBody = if (colonSplit.size > 1) colonSplit[1].trim() else ""

        // Extract contact name between "par" or "whatsapp" and "ko"
        val nameMatch = Regex("(?i)(?:whatsapp\\s+par|whatsapp)\\s+([a-zA-Z0-9]+)\\s+ko").find(clean)
        val contactName = nameMatch?.groupValues?.get(1) ?: ""

        return ParsedIntent.OpenWhatsApp(
          contactQuery = contactName,
          message = msgBody,
          spokenConfirmation = if (contactName.isNotEmpty()) "WhatsApp par $contactName ki chat khol rahi hoon." else "WhatsApp open kar rahi hoon."
        )
      }
      return ParsedIntent.OpenWhatsApp("", "", "WhatsApp open kar rahi hoon.")
    }

    // 9. Phone Calling
    val callPattern = Regex("(?i)(?:call|phone karo|phone lagao|ko call lagao|ko call karo|ko phone karo)")
    if (callPattern.containsMatchIn(lower)) {
      // Check if phone number
      val digitsMatch = Regex("[0-9+]{6,}").find(clean)
      if (digitsMatch != null) {
        val number = digitsMatch.value
        return ParsedIntent.CallNumber(number, "Number $number par call connect kar rahi hoon.")
      }

      // Extract contact name (e.g. "Call Mum", "Bilal ko call lagao", "Mummy ko call karo")
      var name = clean.replace(Regex("(?i)durefishan|hey|please|ko call lagao|ko call karo|ko phone lagao|ko phone karo|call karo|call|phone"), "").trim()
      if (name.isEmpty()) name = "Mom"
      return ParsedIntent.CallContact(name, "$name ko call mila rahi hoon.")
    }

    // 10. SMS
    if (lower.contains("sms") || lower.contains("text message")) {
      val colonSplit = clean.split(":")
      val msgBody = if (colonSplit.size > 1) colonSplit[1].trim() else "Salam"
      val nameMatch = Regex("(?i)([a-zA-Z0-9]+)\\s+ko\\s+sms").find(clean)
      val contactName = nameMatch?.groupValues?.get(1) ?: "Contact"
      return ParsedIntent.SendSms(contactName, msgBody, "$contactName ko SMS ready kar rahi hoon.")
    }

    // 11. Generic App Opening & System Settings
    if (lower.contains("settings") || lower.contains("setting")) {
      return ParsedIntent.OpenApp("settings", "Phone ki Settings open kar rahi hoon.")
    }
    if (lower.contains("camera")) {
      return ParsedIntent.OpenApp("camera", "Camera open kar rahi hoon.")
    }
    if (lower.contains("chrome") || lower.contains("browser")) {
      return ParsedIntent.OpenApp("chrome", "Google Chrome open kar rahi hoon.")
    }
    if (lower.startsWith("open ") || lower.endsWith("kholo") || lower.endsWith("chalao")) {
      val app = clean.replace(Regex("(?i)open|kholo|chalao|app|application"), "").trim()
      if (app.isNotEmpty()) {
        return ParsedIntent.OpenApp(app, "$app open kar rahi hoon.")
      }
    }

    return ParsedIntent.Unknown(clean)
  }
}
