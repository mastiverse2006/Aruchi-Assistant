package com.example

import com.example.data.CustomCommand
import com.example.nlp.DurefishanIntentParser
import com.example.nlp.ParsedIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DurefishanIntentParserTest {

  private val parser = DurefishanIntentParser()
  private val greetingName = "Bilal"

  @Test
  fun testWakeGreeting() {
    val result = parser.parse("Hey Durefishan", greetingName, emptyList())
    assertTrue(result is ParsedIntent.Greeting)
    val greeting = result as ParsedIntent.Greeting
    assertTrue(greeting.message.contains("Bilal"))
    assertTrue(greeting.message.contains("Haan ji"))
  }

  @Test
  fun testInstagramReels() {
    val result1 = parser.parse("Open Instagram Reels", greetingName, emptyList())
    assertTrue(result1 is ParsedIntent.OpenInstagram)
    assertEquals("reels", (result1 as ParsedIntent.OpenInstagram).destination)

    val result2 = parser.parse("Insta Reels chalao", greetingName, emptyList())
    assertTrue(result2 is ParsedIntent.OpenInstagram)
    assertEquals("reels", (result2 as ParsedIntent.OpenInstagram).destination)
  }

  @Test
  fun testYouTubeShorts() {
    val result = parser.parse("YouTube Shorts kholo", greetingName, emptyList())
    assertTrue(result is ParsedIntent.OpenYouTube)
    assertEquals("shorts", (result as ParsedIntent.OpenYouTube).destination)
  }

  @Test
  fun testCallContact() {
    val result = parser.parse("Bilal ko call lagao", greetingName, emptyList())
    assertTrue(result is ParsedIntent.CallContact)
    assertTrue((result as ParsedIntent.CallContact).contactName.contains("Bilal"))
  }

  @Test
  fun testCallNumber() {
    val result = parser.parse("Call 9876543210", greetingName, emptyList())
    assertTrue(result is ParsedIntent.CallNumber)
    assertEquals("9876543210", (result as ParsedIntent.CallNumber).phoneNumber)
  }

  @Test
  fun testCustomCommandMatching() {
    val customCommand = CustomCommand(
      id = 1,
      commandPhrase = "Open my favourite reels",
      alternativePhrases = "favourite reels, meri reels",
      actionType = "OPEN_DESTINATION",
      actionConfig = "instagram_reels",
      isEnabled = true
    )
    val result = parser.parse("Open my favourite reels", greetingName, listOf(customCommand))
    assertTrue(result is ParsedIntent.CustomAction)
    assertEquals(customCommand, (result as ParsedIntent.CustomAction).command)
  }

  @Test
  fun testTimeInquiry() {
    val result = parser.parse("Kitne baj rahe hain?", greetingName, emptyList())
    assertTrue(result is ParsedIntent.Time)
  }

  @Test
  fun testVoiceControls() {
    val muteResult = parser.parse("Mute yourself", greetingName, emptyList())
    assertTrue(muteResult is ParsedIntent.Mute)

    val stopResult = parser.parse("Sunna band karo", greetingName, emptyList())
    assertTrue(stopResult is ParsedIntent.StopListening)
  }
}
