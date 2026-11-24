# CardPulse — Native Android Credit Card Payment Reminder

A native **Kotlin + Jetpack Compose + Material 3 + Gradle** Android application featuring a high-tech obsidian dark UI, Google Pay–style bank card visuals, custom recurrence scheduling, and local Android notifications.

## Features
- **High-Tech Obsidian Dark Theme:** `#080C14` obsidian background, `#0F172A` surfaces, neon cyan `#22D3EE` accents, and monospace telemetry counters.
- **Google Pay–Style Card Visuals (1.586:1):** Built-in preset themes for **HSBC**, **Citi**, **Mox Bank**, **Standard Chartered**, **Hang Seng Bank**, **ZA Bank**, and **Custom**, complete with metallic gold EMV chip, contactless waves, holographic sheen, and Android Photo Picker support for custom card images.
- **Custom Recurrence & Month-End Math:** Configure payment intervals in **Days**, **Weeks**, or **Months** with automatic month-end handling (via `java.time.LocalDate`) and one-tap **Mark Paid** with Snackbar **Undo**.
- **Configurable Local Notifications (WorkManager + NotificationCompat):**
  - **5 days before:** *Payment coming up*
  - **2 days before:** *Payment still outstanding*
  - **On due date:** *Final payment reminder*

## Uploading to GitHub & Building
1. Unzip the contents of `cardpulse-android-kotlin.zip` directly into the **root** of your GitHub repository so `settings.gradle.kts`, `build.gradle.kts`, and `app/` sit at the top level (do not nest them inside an extra subfolder).
2. Import the repository into **Google AI Studio (Build → Android)** or open it in **Android Studio** to build and install the `.apk`.
