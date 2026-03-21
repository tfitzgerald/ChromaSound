# ChromaSound 🎨🎵
### Sound → FFT → Color · Android App

---

## Build entirely on GitHub — no local tools required

This project is designed to be built 100% in the cloud using GitHub Actions.
You do not need Android Studio, Gradle, or the Android SDK on your computer.

### What you need
- A **GitHub account** (free) — https://github.com
- A **web browser** — that's it

---

## How to get your APK in 4 steps

### Step 1 — Create a GitHub repository

1. Log into GitHub.
2. Click **+** (top right) → **New repository**.
3. Name it `chromasound`, leave everything else default.
4. Click **Create repository**.

### Step 2 — Upload the project files

On the empty repository page:

1. Click **uploading an existing file** (the link in the middle of the page).
2. Drag-and-drop ALL files from the ChromaSound project folder into the browser window.
   - Include the hidden `.github/` folder and `.gitignore` file.
   - On Windows: in File Explorer, show hidden items first (View → Hidden items).
   - On macOS: press Cmd+Shift+. to reveal hidden files in Finder.
3. Scroll down, type a commit message like `Initial commit`.
4. Click **Commit changes**.

### Step 3 — Watch GitHub build the APK

1. Click the **Actions** tab in your repository.
2. You will see a workflow run called **Build ChromaSound APK** already running.
3. Click it → click the **build** job → watch the live log scroll.
4. The first build takes 8–12 minutes (downloading Android SDK).
   Repeat builds take 3–5 minutes (cached).
5. When it shows a green ✅, scroll down to **Artifacts**.
6. Click **ChromaSound-debug-...** to download a ZIP.
7. Unzip it — inside is `app-debug.apk`.

### Step 4 — Install on your phone

1. Transfer `app-debug.apk` to your Android phone (email, Google Drive, USB).
2. On the phone: **Settings → Security → Install unknown apps** → enable for your file manager.
3. Open the APK file and tap **Install**.
4. Open **ChromaSound**, tap **TAP TO LISTEN**, grant microphone permission, make noise.

---

## For a signed release APK

A signed APK is needed for wider distribution. This requires a signing key
stored as GitHub Secrets.

### Generate a keystore (one-time, on any computer with Java installed)

```bash
keytool -genkeypair -v \
  -keystore chromasound.keystore \
  -alias chromasound \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass YOUR_STORE_PASSWORD \
  -keypass  YOUR_KEY_PASSWORD \
  -dname "CN=Your Name, O=Your Org, C=CA"

# Encode it to base64 (Linux/macOS):
base64 -w 0 chromasound.keystore

# On macOS:
base64 -i chromasound.keystore -o -
```

### Add 4 GitHub Secrets

Go to your repo → **Settings → Secrets and variables → Actions → New repository secret**:

| Secret name | Value |
|---|---|
| `KEYSTORE_BASE64` | The base64 string from the command above |
| `KEY_STORE_PASSWORD` | Your keystore password |
| `KEY_ALIAS` | `chromasound` |
| `KEY_PASSWORD` | Your key password |

### Publish a release

1. Go to your repo → **Releases** → **Create a new release**.
2. Tag it `v1.0.0`, publish it.
3. GitHub Actions builds a signed APK and attaches it to the release page automatically.

---

## Project structure

```
ChromaSound/
├── .github/workflows/build.yml      ← GitHub Actions (the build brain)
├── .gitignore
├── build.gradle.kts                 ← Root Gradle config
├── settings.gradle.kts
├── gradle/libs.versions.toml        ← Dependency versions
└── app/
    ├── build.gradle.kts             ← App Gradle config
    └── src/main/
        ├── AndroidManifest.xml
        └── java/com/chromasound/app/
            ├── MainActivity.kt
            ├── audio/AudioCaptureEngine.kt
            ├── fft/FFTEngine.kt
            ├── fft/FrequencyColorMapper.kt
            ├── model/Models.kt
            ├── ui/ChromaSoundViewModel.kt
            ├── ui/ChromaSoundScreen.kt
            └── ui/theme/Theme.kt
```

---

## How the build works (for the curious)

The workflow at `.github/workflows/build.yml` runs on GitHub's free Ubuntu servers inside a
`gradle:8.6-jdk17` Docker container that has Java and Gradle pre-installed — no wrapper jar needed.
It then downloads the Android SDK, accepts licenses, installs API 34 + build tools,
and runs `gradle assembleDebug`. The resulting APK is uploaded as a downloadable artifact.
