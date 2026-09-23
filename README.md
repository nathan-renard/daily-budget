# Daily Budget

A simple Android app to control a daily inflow of budgets: set a recurring monthly budget,
and the main screen tells you how much you can spend **today** without overflowing the month.

```
daily allowance = (monthly budget − spent before today) / days left in month (incl. today)
left today      = daily allowance − spent today
```

Example: $500 left with 5 days to go → $100/day. Overspend today and tomorrow's
allowance shrinks; underspend and it grows. Each new month starts fresh with the same budget.

## Features

- **Budget** – today's allowance front and centre, tomorrow's projection, month progress.
- **Expenses** – this month's expenses grouped by day.
- **Calendar** – green days stayed within that day's allowance, red days went over.
- **Settings** – budget, currency, dark / light / system theme, morning reminder notification.

All data stays on the device (`localStorage` in the WebView, plus a small copy in
SharedPreferences for the notification).

## Layout

- `app/src/main/assets/www/` – the UI (plain HTML/CSS/JS)
  - `budget.js` – the budget math
  - `app.js` – UI, storage, settings
- `app/src/main/java/com/aimbire/dailybudget/`
  - `MainActivity.java` – full-screen WebView, system bars, back button, JS bridge
  - `Reminder.java`, `ReminderReceiver.java`, `RescheduleReceiver.java` – morning notification

## Build the APK

Requires JDK 17 and the Android SDK (create `local.properties` with `sdk.dir=...`).

```
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`. Without an upload key it is signed
with the debug key so it installs directly (enable "Install unknown apps" on the phone).

## Publish to Google Play

1. Create the upload key once. This writes `upload-keystore.jks` and `keystore.properties`, both
   git-ignored. **Back them up**: every future update must be signed with this key.

   ```
   powershell -ExecutionPolicy Bypass -File tools/create-upload-key.ps1
   ```

2. Build the signed bundle:

   ```
   powershell -ExecutionPolicy Bypass -File tools/bundle.ps1
   ```

   Output: `dist/DailyBudget-<versionName>-<versionCode>.aab`. Upload it in Play Console
   (Testing → Internal testing → Create release). Play App Signing re-signs it with the
   app signing key that Google holds.

3. For every new upload, bump `versionCode` (and usually `versionName`) in `app/build.gradle`.
   Play rejects a bundle whose `versionCode` was already used.

## Preview on desktop

```
powershell -ExecutionPolicy Bypass -File tools/serve.ps1
```

Then open http://localhost:8765/. Notifications and system-bar colours only work in the Android app.

## Store listing graphics

`store/listing/` holds the Play Store icon (512×512), feature graphic (1024×500) and phone
screenshots (1080×1920). They are rendered from `store/src/` with headless Microsoft Edge;
the screenshots show the real app UI with sample data. To regenerate after UI changes:

```
powershell -ExecutionPolicy Bypass -File store/render.ps1
```
