package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.bridge.DeviceActionManager
import com.example.model.ActionIconType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DeviceActionManagerTest {

  @Test
  fun testOpenWhatsAppFallback() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = DeviceActionManager(context)
    val result = manager.openWhatsApp()

    assertNotNull(result)
    assertEquals(ActionIconType.WHATSAPP, result.actionType)
  }

  @Test
  fun testOpenUrl() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = DeviceActionManager(context)
    val result = manager.openUrl("https://example.com")

    assertTrue(result.success)
    assertEquals(ActionIconType.URL, result.actionType)
    assertEquals("https://example.com", result.detail)
  }

  @Test
  fun testMakeCallDialerFallback() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = DeviceActionManager(context)
    val result = manager.makeCall("9876543210")

    assertNotNull(result)
    assertEquals(ActionIconType.CALL, result.actionType)
    assertTrue(result.detail.contains("9876543210"))
  }
}
