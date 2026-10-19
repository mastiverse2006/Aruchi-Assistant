# Durefishan Assistant

**Durefishan Assistant** is a premium, minimal Android AI voice assistant tailored for natural Roman Urdu, Urdu, Hinglish, and English voice interactions. It features a glowing 3D voice orb, offline-first natural language understanding, native app integrations (WhatsApp, Instagram Reels/DM, YouTube Shorts/Search, TikTok, Snapchat, Phone Calls, SMS), and customizable voice commands backed by local Room database storage.

---

## 🚀 How to Download the APK (Without Android Studio)

You do **not** need Android Studio or a high-spec PC to build or install Durefishan Assistant. The APK is built automatically in the cloud using **GitHub Actions**.

### Step 1: Push Changes to GitHub
Push your completed code to your repository:
```bash
git add .
git commit -m "Configure Durefishan Assistant and automated APK build"
git push origin main
```

### Step 2: Open GitHub Actions
1. Navigate to your repository in your browser:  
   `https://github.com/mastiverse2006/Aruchi-Assistant`
2. Click on the **Actions** tab at the top of the repository.
3. You will see a workflow named **"Build Durefishan Assistant APK"** running automatically (or you can trigger it manually by clicking **Run workflow**).

### Step 3: Download the APK Artifact
1. Click on the completed workflow run (marked with a green checkmark `✔`).
2. Scroll down to the **Artifacts** section at the bottom of the summary page.
3. Click on **`Durefishan-Assistant-debug-apk`** to download the ZIP file.

### Step 4: Install on your Android Phone
1. Unzip the downloaded file on your computer or phone to get `app-debug.apk`.
2. Transfer `app-debug.apk` to your phone (via USB, Google Drive, WhatsApp, or Telegram).
3. Tap on the file on your phone to install.
   - *Note*: If Android shows "Install unknown apps", tap **Settings** and allow your file manager/browser to install apps.

---

## 🔐 Optional: Configure Gemini API Key

Durefishan Assistant works **100% offline** for voice commands, app launching, phone calls, and custom commands.

If you wish to enable the optional Gemini Live cloud features:
1. Open your GitHub repository settings:  
   **Settings** $\rightarrow$ **Secrets and variables** $\rightarrow$ **Actions**.
2. Click **New repository secret**.
3. Name: `GEMINI_API_KEY`  
   Value: Your Google Gemini API key.
4. Click **Add secret**.
5. The next build will automatically inject your key into the build environment. Alternatively, you can enter or change your key directly within the app under **Settings $\rightarrow$ API Key**.

---

## 📱 Features

- **Volumetric 3D Voice Orb**: Real-time breathing, listening (amplitude wave), thinking, speaking, and muted states.
- **Natural Language Voice Control**:
  - *"Hey Durefishan"* / *"Suno Durefishan"* $\rightarrow$ *"Haan ji Bilal, boliye. Main sun rahi hoon."*
  - *"Instagram kholo"*, *"Insta Reels chalao"*, *"Instagram par Bilal ko DM bhejo"*
  - *"YouTube kholo"*, *"YouTube Shorts chalao"*, *"YouTube par [query] search karo"*
  - *"WhatsApp kholo"*, *"WhatsApp par Bilal ko message karo: main aa raha hoon"*
  - *"TikTok chalao"*, *"Snapchat kholo"*
  - *"Bilal ko call lagao"*, *"Call Mum"*, *"Call 9876543210"*
  - *"Bilal ko SMS bhejo: main baad mein call karunga"*
  - *"Kitne baj rahe hain?"* (Current time inquiry)
  - *"Mute yourself"*, *"Unmute karo"*, *"Sunna band karo"*
- **Custom Voice Commands**: Create, toggle, search, and test your own personalized voice triggers (stored locally in Room database).
- **Background Service**: Android foreground notification service for instant access from any app.
- **Customizable Persona**: Configurable greeting name (default: "Bilal"), voice pitch, speech speed, and language selector.

---

## 🛠 Local Build (Optional)

If you have a Linux, macOS, or Windows terminal with JDK 17 installed:

```bash
# Verify unit tests
./gradlew testDebugUnitTest

# Assemble Debug APK
./gradlew assembleDebug

# Output APK location:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## ❓ Troubleshooting

1. **"Install Blocked by Play Protect" on Phone**:  
   Tap **More details** $\rightarrow$ **Install anyway**. This warning appears for all debug APKs that are self-signed during development.
2. **Missing Permissions**:  
   Open app Settings to grant Microphone, Contacts, and Phone permissions. Each permission has an explanatory rationale and a 1-tap grant button.
3. **Contacts Search**:  
   Make sure Contacts permission is granted. If multiple contacts match a name (e.g. two "Rahuls"), Durefishan will ask which one to call and display contact selection buttons.
