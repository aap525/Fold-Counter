# Fold Tracker

An Android app for Samsung Galaxy Z Fold devices that counts how many times you unfold your phone, with daily/total/average/7-day/streak stats and three home-screen widgets.

## How detection works

Android has no public "fold/unfold" broadcast on every device. This app uses the official **Jetpack WindowManager** library (`androidx.window`), which reports `FoldingFeature` state (`FLAT`, `HALF_OPENED`, or folded/closed) — this is Google and Samsung's supported way to observe hinge posture. A foreground service watches this continuously, so it works even when the app itself isn't open, as long as the service is alive (see Battery notes below).

Every transition from folded → open is counted once, with debouncing so hinge jitter doesn't double-count.

## Features

- **Stats dashboard**: today's count, all-time total, daily average, last 7 days (bar chart), current streak, best day, days tracked
- **Daily goal** with progress bar (toggle in Settings)
- **Streak tracking** (toggle in Settings)
- **Daily summary notification** at 9 PM (toggle in Settings)
- **Three home-screen widgets**, each in light and dark mode automatically:
  - Daily count only
  - Total count only
  - Combined (both side by side)
- Widgets update **instantly** the moment an unfold is detected — no waiting on Android's default 30-minute refresh
- Settings screen to pause tracking entirely, disable any extra feature, request battery-optimization exemption, or reset all data

## Building the APK

### Option A — GitHub Actions (recommended, no local setup)

1. Create a new **public or private GitHub repository**.
2. Push this entire project folder to it:
   ```bash
   cd foldtracker
   git init
   git add .
   git commit -m "Initial commit"
   git branch -M main
   git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPO.git
   git push -u origin main
   ```
3. Go to the **Actions** tab on your GitHub repo. A workflow called "Build APK" will run automatically (it's also triggered on every push to `main`).
4. When it finishes (green check, ~2-3 minutes), click into the run, scroll to **Artifacts**, and download `FoldTracker-debug-apk`. Unzip it to get `app-debug.apk`.
5. Transfer that APK to your phone (email it to yourself, use Google Drive, ADB, etc.) and open it to install.
   - You'll need to allow "Install unknown apps" for whichever app you use to open the APK file, since it's outside the Play Store.

You can also trigger a build manually anytime from the Actions tab via "Run workflow" (workflow_dispatch).

### Option B — Android Studio (local build)

1. Install [Android Studio](https://developer.android.com/studio).
2. Open this project folder directly (`File > Open`).
3. Let Gradle sync (it will download the SDK/build tools automatically).
4. `Build > Build Bundle(s) / APK(s) > Build APK(s)`.
5. Find the APK under `app/build/outputs/apk/debug/app-debug.apk`.

## After installing

1. Open the app once — it'll ask for notification permission (needed for the ongoing low-priority tracking notification and the optional daily summary).
2. You'll see a banner prompting you to **disable battery optimization** for the app. Tap it and allow — this is important on Samsung devices, which are aggressive about killing background services (Device Care / battery). Without this, tracking may silently stop after a while.
3. Long-press your home screen → Widgets → **Fold Tracker** → drag out whichever of the three widgets you want (Daily, Total, or Combined).
4. Everything after that is automatic — fold/unfold your phone and the counts update live, both in the app and on any widgets you've placed.

## Notes & limitations

- Detection relies on the device correctly reporting a `FoldingFeature` through Jetpack WindowManager. This is standard on Samsung Z Fold devices running a reasonably current One UI, but if Samsung changes their implementation in a future OS update, detection could need adjustment.
- The persistent tracking notification is required by Android for any foreground service — there's no way to hide it entirely, but it's set to minimum priority so it stays out of the way.
- All data is stored locally on-device (Room/SQLite) — nothing leaves your phone.
