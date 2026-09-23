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
- `app/src/main/java/com/dailybudget/app/`
  - `MainActivity.java` – full-screen WebView, system bars, back button, JS bridge
  - `Reminder.java`, `ReminderReceiver.java`, `RescheduleReceiver.java` – morning notification

## Build the APK

Requires JDK 17 and the Android SDK (create `local.properties` with `sdk.dir=...`).

```
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`. It is signed with the debug key so
it installs directly (enable "Install unknown apps" on the phone). Use your own keystore
before publishing to the Play Store.

## Preview on desktop

```
powershell -ExecutionPolicy Bypass -File tools/serve.ps1
```

Then open http://localhost:8765/. Notifications and system-bar colours only work in the Android app.
