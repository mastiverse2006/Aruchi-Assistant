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
   * Opens WhatsApp app or deep links.
   */
  fun openWhatsApp(): ActionResult {
    val pm = context.packageManager
    val packages = listOf("com.whatsapp", "com.whatsapp.w4b")

    for (pkg in packages) {
      val intent = pm.getLaunchIntentForPackage(pkg)
      if (intent != null) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
          context.startActivity(intent)
          ActionResult(
            success = true,
            message = "WhatsApp opened.",
            actionType = ActionIconType.WHATSAPP,
            detail = pkg
          )
        } catch (e: Exception) {
          Log.e(TAG, "Error opening WhatsApp package", e)
          ActionResult(false, "WhatsApp launch failed: ${e.message}", ActionIconType.WHATSAPP)
        }
      }
    }

    val fallback = openUrl("https://wa.me/")
    return fallback.copy(
      message = "WhatsApp app is not installed. Opened WhatsApp in browser.",
      actionType = ActionIconType.WHATSAPP
    )
  }

  /**
   * Opens WhatsApp chat with a contact phone number, optionally with pre-filled message.
   */
  fun openWhatsAppChat(phoneNumber: String, messageText: String = ""): ActionResult {
    val cleaned = phoneNumber.replace(Regex("[^0-9+]"), "")
    val encodedMsg = Uri.encode(messageText)
    val url = if (cleaned.isNotEmpty()) {
      if (messageText.isNotEmpty()) "https://api.whatsapp.com/send?phone=$cleaned&text=$encodedMsg"
      else "https://api.whatsapp.com/send?phone=$cleaned"
    } else {
      "https://wa.me/"
    }

    return try {
      val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        setPackage("com.whatsapp")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intent)
      ActionResult(
        success = true,
        message = if (cleaned.isNotEmpty()) "Opening WhatsApp chat with $cleaned." else "Opening WhatsApp.",
        actionType = ActionIconType.WHATSAPP,
        detail = url
      )
    } catch (_: Exception) {
      // Fallback to generic browser / app chooser
      openUrl(url)
    }
  }

  /**
   * Opens Instagram, Reels, or Direct.
   */
  fun openInstagram(destination: String = "home"): ActionResult {
    val pm = context.packageManager
    val igPkg = "com.instagram.android"
    val isInstalled = pm.getLaunchIntentForPackage(igPkg) != null

    return when (destination.lowercase()) {
      "reels" -> {
        try {
          val uri = Uri.parse("https://www.instagram.com/reels/")
          val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            if (isInstalled) setPackage(igPkg)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          }
          context.startActivity(intent)
          ActionResult(true, "Opening Instagram Reels.", ActionIconType.APP, "Instagram Reels")
        } catch (e: Exception) {
          openUrl("https://www.instagram.com/reels/")
        }
      }

      "direct", "dm" -> {
        try {
          val uri = Uri.parse("https://www.instagram.com/direct/inbox/")
          val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            if (isInstalled) setPackage(igPkg)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          }
          context.startActivity(intent)
          ActionResult(true, "Opening Instagram Direct Messages.", ActionIconType.APP, "Instagram DM")
        } catch (e: Exception) {
          openUrl("https://www.instagram.com/direct/inbox/")
        }
      }

      else -> {
        if (isInstalled) {
          val intent = pm.getLaunchIntentForPackage(igPkg)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          }
          if (intent != null) {
            context.startActivity(intent)
            return ActionResult(true, "Instagram opened.", ActionIconType.APP, igPkg)
          }
        }
        openUrl("https://www.instagram.com")
      }
    }
  }

  /**
   * Opens YouTube, YouTube Shorts, or searches YouTube.
   */
  fun openYouTube(destination: String = "home", query: String = ""): ActionResult {
    val pm = context.packageManager
    val ytPkg = "com.google.android.youtube"
    val isInstalled = pm.getLaunchIntentForPackage(ytPkg) != null

    return when {
      destination.equals("shorts", ignoreCase = true) -> {
        try {
          val uri = Uri.parse("https://www.youtube.com/shorts")
          val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            if (isInstalled) setPackage(ytPkg)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          }
          context.startActivity(intent)
          ActionResult(true, "Opening YouTube Shorts.", ActionIconType.APP, "YouTube Shorts")
        } catch (e: Exception) {
          openUrl("https://www.youtube.com/shorts")
        }
      }

      query.isNotBlank() -> {
        try {
          val uri = Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")
          val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            if (isInstalled) setPackage(ytPkg)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          }
          context.startActivity(intent)
          ActionResult(true, "Searching YouTube for '$query'.", ActionIconType.APP, "Search: $query")
        } catch (e: Exception) {
          openUrl("https://www.youtube.com/results?search_query=${Uri.encode(query)}")
        }
      }

      else -> {
        if (isInstalled) {
          val intent = pm.getLaunchIntentForPackage(ytPkg)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          }
          if (intent != null) {
            context.startActivity(intent)
            return ActionResult(true, "YouTube opened.", ActionIconType.APP, ytPkg)
          }
        }
        openUrl("https://www.youtube.com")
      }
    }
  }

  /**
   * Opens TikTok app or web.
   */
  fun openTikTok(): ActionResult {
    val pm = context.packageManager
    val tiktokPackages = listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill")
    for (pkg in tiktokPackages) {
      val intent = pm.getLaunchIntentForPackage(pkg)
      if (intent != null) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
          context.startActivity(intent)
          ActionResult(true, "TikTok opened.", ActionIconType.APP, pkg)
        } catch (e: Exception) {
          Log.e(TAG, "Error opening TikTok", e)
          ActionResult(false, "Could not launch TikTok: ${e.message}", ActionIconType.APP)
        }
      }
    }
    return openUrl("https://www.tiktok.com")
  }

  /**
   * Opens Snapchat app or web.
   */
  fun openSnapchat(): ActionResult {
    val pm = context.packageManager
    val snapPkg = "com.snapchat.android"
    val intent = pm.getLaunchIntentForPackage(snapPkg)
    if (intent != null) {
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      return try {
        context.startActivity(intent)
        ActionResult(true, "Snapchat opened.", ActionIconType.APP, snapPkg)
      } catch (e: Exception) {
        ActionResult(false, "Could not open Snapchat: ${e.message}", ActionIconType.APP)
      }
    }
    return openUrl("https://www.snapchat.com")
  }

  /**
   * Opens an application by name or common alias.
   */
  fun openApp(appName: String): ActionResult {
    val trimmed = appName.trim().lowercase()

    return when {
      trimmed.contains("reels") -> openInstagram("reels")
      trimmed.contains("insta") -> openInstagram("home")
      trimmed.contains("youtube") && trimmed.contains("short") -> openYouTube("shorts")
      trimmed.contains("youtube") -> openYouTube("home")
      trimmed.contains("tiktok") -> openTikTok()
      trimmed.contains("snapchat") || trimmed.contains("snap") -> openSnapchat()
      trimmed.contains("whatsapp") -> openWhatsApp()
      trimmed.contains("setting") -> openSettings()
      trimmed.contains("camera") -> openCamera()
      trimmed.contains("chrome") -> openChrome()
      else -> launchGenericApp(appName)
    }
  }

  fun openSettings(): ActionResult {
    return try {
      val intent = Intent(Settings.ACTION_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intent)
      ActionResult(true, "Settings opened.", ActionIconType.SETTINGS, "Android Settings")
    } catch (e: Exception) {
      ActionResult(false, "Could not open Settings: ${e.message}", ActionIconType.SETTINGS)
    }
  }

  fun openCamera(): ActionResult {
    return try {
      val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intent)
      ActionResult(true, "Camera opened.", ActionIconType.APP, "Camera")
    } catch (e: Exception) {
      ActionResult(false, "Could not open Camera: ${e.message}", ActionIconType.APP)
    }
  }

  fun openChrome(): ActionResult {
    val pm = context.packageManager
    val chromePkg = "com.android.chrome"
    val intent = pm.getLaunchIntentForPackage(chromePkg)
    if (intent != null) {
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(intent)
      return ActionResult(true, "Google Chrome opened.", ActionIconType.APP, chromePkg)
    }
    return openUrl("https://www.google.com")
  }

  private fun launchGenericApp(appName: String): ActionResult {
    val pm = context.packageManager
    val trimmed = appName.trim().lowercase()
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
        return ActionResult(true, "$appLabel opened.", ActionIconType.APP, pkg)
      }
    }

    return ActionResult(
      success = false,
      message = "Could not find an installed app matching '$appName'.",
      actionType = ActionIconType.APP,
      detail = "Not installed"
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
      ActionResult(true, "Opened link: $formattedUrl", ActionIconType.URL, formattedUrl)
    } catch (e: Exception) {
      Log.e(TAG, "Error opening URL: $formattedUrl", e)
      ActionResult(false, "Failed to open link: ${e.message}", ActionIconType.URL, formattedUrl)
    }
  }

  /**
   * Prepares or sends an SMS message.
   */
  fun sendSms(phoneNumber: String, messageBody: String): ActionResult {
    val cleaned = phoneNumber.replace(Regex("[^0-9+]"), "")
    return try {
      val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$cleaned")).apply {
        putExtra("sms_body", messageBody)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intent)
      ActionResult(
        success = true,
        message = "SMS ready for $cleaned: '$messageBody'",
        actionType = ActionIconType.APP,
        detail = "smsto:$cleaned"
      )
    } catch (e: Exception) {
      ActionResult(false, "Failed to open SMS: ${e.message}", ActionIconType.APP)
    }
  }

  /**
   * Initiates a phone call or opens dialer.
   */
  fun makeCall(phoneNumber: String): ActionResult {
    val cleanedNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
    if (cleanedNumber.isEmpty()) {
      return ActionResult(false, "Invalid phone number provided.", ActionIconType.CALL)
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
        ActionResult(true, "Calling $cleanedNumber...", ActionIconType.CALL, cleanedNumber)
      } else {
        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanedNumber")).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(dialIntent)
        ActionResult(true, "Opened dialer with $cleanedNumber.", ActionIconType.CALL, cleanedNumber)
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error making call to $cleanedNumber", e)
      ActionResult(false, "Could not place call: ${e.message}", ActionIconType.CALL, cleanedNumber)
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
        message = "Contacts permission is required to search contacts. Please grant Contacts permission.",
        actionType = ActionIconType.CALL,
        detail = "Permission: READ_CONTACTS"
      )
    }

    val trimmedQuery = contactName.trim()
    val matches = searchContacts(trimmedQuery)

    return when {
      matches.isEmpty() -> {
        ActionResult(
          success = false,
          message = "No contact found matching '$trimmedQuery'.",
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

  fun searchContacts(query: String): List<ContactMatch> {
    val matches = mutableListOf<ContactMatch>()
    val contentResolver = context.contentResolver
    val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
    val projection = arrayOf(
      ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
      ContactsContract.CommonDataKinds.Phone.NUMBER,
      ContactsContract.CommonDataKinds.Phone.TYPE
    )

    val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
    val selectionArgs = arrayOf("%$query%")

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
      Log.e(TAG, "Error querying contacts for $query", e)
    }
    return matches
  }
}
