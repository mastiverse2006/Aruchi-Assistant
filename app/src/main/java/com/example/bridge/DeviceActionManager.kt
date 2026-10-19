package com.example.bridge

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.model.ActionIconType
import com.example.model.ActionResult
import com.example.model.ContactMatch

class DeviceActionManager(private val context: Context) {

  companion object {
    private const val TAG = "DeviceActionManager"
  }

  /**
   * Opens WhatsApp if installed, or falls back to WhatsApp Web link.
   */
  fun openWhatsApp(): ActionResult {
    val pm = context.packageManager
    val packages = listOf("com.whatsapp", "com.whatsapp.w4b")

    for (pkg in packages) {
      val intent = pm.getLaunchIntentForPackage(pkg)
      if (intent != null) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
          context.startActivity(intent)
          return ActionResult(
            success = true,
            message = "WhatsApp opened successfully.",
            actionType = ActionIconType.WHATSAPP,
            detail = "Package: $pkg"
          )
        } catch (e: Exception) {
          Log.e(TAG, "Error launching $pkg", e)
        }
      }
    }

    // Fallback to WhatsApp Web URL
    return try {
      val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/")).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(webIntent)
      ActionResult(
        success = true,
        message = "WhatsApp app is not installed. Opened WhatsApp in browser.",
        actionType = ActionIconType.WHATSAPP,
        detail = "https://wa.me/"
      )
    } catch (e: Exception) {
      Log.e(TAG, "Failed to launch WhatsApp URL", e)
      ActionResult(
        success = false,
        message = "Could not open WhatsApp on this device.",
        actionType = ActionIconType.WHATSAPP,
        detail = e.localizedMessage ?: "Unknown error"
      )
    }
  }

  /**
   * Opens an application by name or common alias.
   */
  fun openApp(appName: String): ActionResult {
    val trimmed = appName.trim().lowercase()

    // Specific well-known aliases and native intents
    when {
      trimmed.contains("setting") -> {
        return try {
          val intent = Intent(Settings.ACTION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          }
          context.startActivity(intent)
          ActionResult(
            success = true,
            message = "Settings opened.",
            actionType = ActionIconType.SETTINGS,
            detail = "Android Settings"
          )
        } catch (e: Exception) {
          ActionResult(false, "Could not open Settings: ${e.message}", ActionIconType.SETTINGS)
        }
      }

      trimmed.contains("camera") -> {
        return try {
          val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          }
          context.startActivity(intent)
          ActionResult(
            success = true,
            message = "Camera opened.",
            actionType = ActionIconType.APP,
            detail = "Camera"
          )
        } catch (e: Exception) {
          ActionResult(false, "Could not open Camera: ${e.message}", ActionIconType.APP)
        }
      }

      trimmed.contains("youtube") -> {
        val ytIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.youtube")
        if (ytIntent != null) {
          ytIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          context.startActivity(ytIntent)
          return ActionResult(true, "YouTube opened.", ActionIconType.APP, "com.google.android.youtube")
        }
        return openUrl("https://www.youtube.com")
      }

      trimmed.contains("instagram") -> {
        val igIntent = context.packageManager.getLaunchIntentForPackage("com.instagram.android")
        if (igIntent != null) {
          igIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          context.startActivity(igIntent)
          return ActionResult(true, "Instagram opened.", ActionIconType.APP, "com.instagram.android")
        }
        return openUrl("https://www.instagram.com")
      }

      trimmed.contains("chrome") -> {
        val chromeIntent = context.packageManager.getLaunchIntentForPackage("com.android.chrome")
        if (chromeIntent != null) {
          chromeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          context.startActivity(chromeIntent)
          return ActionResult(true, "Google Chrome opened.", ActionIconType.APP, "com.android.chrome")
        }
        return openUrl("https://www.google.com")
      }

      trimmed.contains("whatsapp") -> {
        return openWhatsApp()
      }
    }

    // Generic search across launcher apps
    val pm = context.packageManager
    val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
      addCategory(Intent.CATEGORY_LAUNCHER)
    }
    val resolveInfos = pm.queryIntentActivities(mainIntent, 0)

    val match = resolveInfos.firstOrNull { info ->
      val label = info.loadLabel(pm).toString().lowercase()
      label.contains(trimmed) || trimmed.contains(label)
    }

    if (match != null) {
      val pkg = match.activityInfo.packageName
      val launchIntent = pm.getLaunchIntentForPackage(pkg)
      if (launchIntent != null) {
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
        val appLabel = match.loadLabel(pm).toString()
        return ActionResult(
          success = true,
          message = "$appLabel opened.",
          actionType = ActionIconType.APP,
          detail = pkg
        )
      }
    }

    return ActionResult(
      success = false,
      message = "Could not find an installed app matching '$appName'.",
      actionType = ActionIconType.APP,
      detail = "Search query: $appName"
    )
  }

  /**
   * Opens a URL in the default browser.
   */
  fun openUrl(url: String): ActionResult {
    var formattedUrl = url.trim()
    if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
      formattedUrl = "https://$formattedUrl"
    }

    return try {
      val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intent)
      ActionResult(
        success = true,
        message = "Opened link: $formattedUrl",
        actionType = ActionIconType.URL,
        detail = formattedUrl
      )
    } catch (e: Exception) {
      Log.e(TAG, "Error opening URL: $formattedUrl", e)
      ActionResult(
        success = false,
        message = "Failed to open link: ${e.message}",
        actionType = ActionIconType.URL,
        detail = formattedUrl
      )
    }
  }

  /**
   * Initiates a phone call or opens the dialer with the phone number pre-filled.
   */
  fun makeCall(phoneNumber: String): ActionResult {
    val cleanedNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
    if (cleanedNumber.isEmpty()) {
      return ActionResult(
        success = false,
        message = "Invalid phone number provided.",
        actionType = ActionIconType.CALL
      )
    }

    val hasCallPermission = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.CALL_PHONE
    ) == PackageManager.PERMISSION_GRANTED

    return try {
      if (hasCallPermission) {
        val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$cleanedNumber")).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(callIntent)
        ActionResult(
          success = true,
          message = "Calling $cleanedNumber...",
          actionType = ActionIconType.CALL,
          detail = cleanedNumber
        )
      } else {
        // Fallback to opening phone dialer safely with number pre-filled
        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanedNumber")).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(dialIntent)
        ActionResult(
          success = true,
          message = "Opened dialer with $cleanedNumber. Tap call button to connect.",
          actionType = ActionIconType.CALL,
          detail = cleanedNumber
        )
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error making call to $cleanedNumber", e)
      ActionResult(
        success = false,
        message = "Could not place call: ${e.message}",
        actionType = ActionIconType.CALL,
        detail = cleanedNumber
      )
    }
  }

  /**
   * Searches contacts by name, handling 0, 1, or multiple matches.
   */
  fun callContact(
    contactName: String,
    onMultipleMatches: (List<ContactMatch>) -> Unit
  ): ActionResult {
    val hasContactsPermission = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.READ_CONTACTS
    ) == PackageManager.PERMISSION_GRANTED

    if (!hasContactsPermission) {
      return ActionResult(
        success = false,
        message = "Contacts permission is required to search and call contacts. Please grant permission.",
        actionType = ActionIconType.CALL,
        detail = "Permission: READ_CONTACTS"
      )
    }

    val trimmedQuery = contactName.trim()
    val matches = mutableListOf<ContactMatch>()

    val contentResolver = context.contentResolver
    val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
    val projection = arrayOf(
      ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
      ContactsContract.CommonDataKinds.Phone.NUMBER,
      ContactsContract.CommonDataKinds.Phone.TYPE
    )

    val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
    val selectionArgs = arrayOf("%$trimmedQuery%")

    try {
      contentResolver.query(
        uri,
        projection,
        selection,
        selectionArgs,
        "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
      )?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val typeIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)

        val seen = mutableSetOf<String>()
        while (cursor.moveToNext()) {
          val name = if (nameIndex >= 0) cursor.getString(nameIndex) ?: "" else ""
          val number = if (numberIndex >= 0) cursor.getString(numberIndex) ?: "" else ""
          val typeInt = if (typeIndex >= 0) cursor.getInt(typeIndex) else -1
          val typeLabel = when (typeInt) {
            ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "Home"
            ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> "Work"
            ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> "Mobile"
            else -> "Phone"
          }

          val key = "$name:$number"
          if (name.isNotEmpty() && number.isNotEmpty() && seen.add(key)) {
            matches.add(ContactMatch(name, number, typeLabel))
          }
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error querying contacts for $trimmedQuery", e)
      return ActionResult(
        success = false,
        message = "Error searching contacts: ${e.message}",
        actionType = ActionIconType.CALL
      )
    }

    return when {
      matches.isEmpty() -> {
        ActionResult(
          success = false,
          message = "No contact found with name '$trimmedQuery'. Please check the contact name.",
          actionType = ActionIconType.CALL,
          detail = "Contact not found"
        )
      }

      matches.size == 1 -> {
        val contact = matches[0]
        val callRes = makeCall(contact.number)
        ActionResult(
          success = callRes.success,
          message = "Calling ${contact.name} (${contact.number})...",
          actionType = ActionIconType.CALL,
          detail = "${contact.name} • ${contact.number}"
        )
      }

      else -> {
        // Multiple matches: Notify callback to display disambiguation UI
        onMultipleMatches(matches)
        val namesList = matches.joinToString(", ") { "${it.name} (${it.number})" }
        ActionResult(
          success = true,
          message = "Found ${matches.size} contacts matching '$trimmedQuery': $namesList. Which one would you like to call?",
          actionType = ActionIconType.CALL,
          detail = "Multiple matches (${matches.size})"
        )
      }
    }
  }
}
