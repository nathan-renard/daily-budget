# Daily Budget

An offline Android app that turns a monthly budget into one number: how much you can spend today.
Spend more today and tomorrow's number shrinks; spend less and it grows.

The UI is plain HTML, CSS and JavaScript running in an Android WebView, with a thin Java shell
around it. No frameworks, no dependencies, no build step for the UI and no network access. The
whole app is about 1,600 lines, so you can read all of it in an afternoon and bend it into
whatever you want.

> This repository is the proof of concept behind [PerDay](https://getperday.com).

<p align="center">
  <img src="docs/screenshots/budget-dark.png" alt="Budget tab: what's left to spend today, tomorrow's allowance and the month so far" width="200">
  <img src="docs/screenshots/expenses-dark.png" alt="Expenses tab: this month's expenses grouped by day" width="200">
  <img src="docs/screenshots/calendar-dark.png" alt="Calendar tab: green days within budget, red days over" width="200">
</p>

<!-- Demo GIF or short screen recording goes here (e.g. adding an expense and watching tomorrow's allowance change). -->

## Motivation

A monthly budget is easy to set and hard to follow. "$942 left with 13 days to go" is accurate, but
it doesn't answer the question you have at the checkout: *can I afford this right now?* So you do
the division in your head, round in your own favour, and find out weeks later whether it worked.

Daily Budget was built to test one idea: if you turn the monthly budget into a single number for
*today*, and let the following days absorb whatever you over- or under-spend, is that enough to keep
a whole month on track? No categories, no envelopes, no bank sync. You log what you spend and look
at one number. A bad day doesn't ruin the month, it just makes the next few days a little tighter.

To find out quickly, it was built as the smallest thing that could work. The interface is a static
web page, so it could be iterated on in a desktop browser, and the Java side only does what a web
page can't: colour the system bars, handle the back button and post a morning notification.

The idea held up. The app shipped on
[Google Play](https://play.google.com/store/apps/details?id=com.aimbire.dailybudget), and the idea is
now being developed further as [PerDay](https://getperday.com). This repository stays as the
original, compact implementation, and it's a reasonable starting point if you want your own take:
a different formula, weekly budgets instead of monthly ones, another platform, or just a template
for a WebView-wrapped Android app.

## Quick Start

### Run the UI in a browser

The UI is static files in [`app/src/main/assets/www`](app/src/main/assets/www), and nothing needs
compiling. Serve the folder with any static file server and reload the page after you edit something:

```sh
python -m http.server 8765 --directory app/src/main/assets/www
```

On Windows without Python, use the included server (pass `-Port` to change the port):

```powershell
powershell -ExecutionPolicy Bypass -File tools/serve.ps1
```

Then open <http://localhost:8765/>. Data is saved to the browser's `localStorage` under the key
`dailyBudget.v1`, so clear that to start over. The Android bridge doesn't exist in a browser, so the
notification settings and system-bar colours do nothing there. Everything else works the same.

### Build and run on Android

You need JDK 17 or newer and the Android SDK with platform 36. Android Studio includes both, and you
can open the repository folder in it and press Run. The app runs on Android 8.0 (API 26) and later.

From the command line, tell Gradle where the SDK is, either with `ANDROID_HOME` or a
`local.properties` file in the repository root (it's git-ignored):

```properties
sdk.dir=/path/to/Android/Sdk
```

On Windows, escape the path: `sdk.dir=C\:\\path\\to\\Android\\Sdk`. Then, with a device or emulator
connected:

```sh
./gradlew installDebug           # Windows: gradlew.bat installDebug
```

`./gradlew assembleDebug` builds the APK without installing it, at
`app/build/outputs/apk/debug/app-debug.apk`.

If the Google Play version is installed on the same device, uninstall it first, because the
signatures differ (uninstalling deletes its data). Or give your build its own application ID (see
[Make it your own](#make-it-your-own)) and keep both.

### Debugging

- `console.log` output from the WebView goes to logcat under the tag `DailyBudget`:
  `adb logcat -s DailyBudget`.
- To check the notification without waiting for the morning, turn on the reminder in Settings and
  tap **Send test notification**. That button only appears in the Android app.

## Usage

1. **Set a monthly budget.** On first launch the app asks for one. The same amount applies every
   month, and each month starts fresh.
2. **Log expenses as you spend.** Tap **+ Add expense** on the Budget tab and enter the amount and
   an optional note. Expenses are recorded for today.
3. **Check today's number.** The **Budget** tab shows what you can still spend today and how much
   of today's allowance you've used. Below that, **Tomorrow** shows the allowance you'll get if you
   spend nothing more today, with an arrow if it's higher or lower than today's, followed by what's
   left this month and how many days remain. If you go over, the headline shows by how much.
4. **Look back.** **Expenses** lists this month's expenses by day with daily totals (tap × to
   delete one). **Calendar** colours each tracked day green (within that day's allowance) or red
   (over it), counts the days on budget, and shows what you spent against the allowance when you tap
   a day.
5. **Adjust settings** with the gear icon: monthly budget, currency symbol (such as $, €, £ or R$),
   theme (dark, light or same as phone), the morning reminder and its time, and **Reset all data**.

The morning reminder is a notification with today's number, for example **Today you can spend
$73.86** with *$960.21 left this month · 13 days left* underneath.

## How it works

### The math

```text
daily allowance = (monthly budget − spent before today) / days left in the month (incl. today)
left today      = daily allowance − spent today
```

- Today's allowance is fixed at the start of the day. Spending today lowers what's *left* today, not
  the allowance itself. Any difference is spread over the remaining days, starting tomorrow.
- The "Tomorrow" line is the allowance you'd get if you spent nothing more today:
  `(budget − spent this month) / (days left − 1)`. On the last day of the month it's next month's
  first allowance.
- Once the month's budget is used up, the allowance is 0 until the next month.
- Each month starts fresh with the same budget. Nothing carries over.

For example, with $500 left at the start of the day and 5 days to go (including today), today's
allowance is $100:

| Spent today | Left this month | Tomorrow's allowance |
| ----------: | --------------: | -------------------: |
|        $100 |            $400 |   $400 / 4 = **$100** |
|        $140 |            $360 |    $360 / 4 = **$90** |
|         $60 |            $440 |   $440 / 4 = **$110** |

All of this is `Budget.compute` in [`budget.js`](app/src/main/assets/www/budget.js): one pure
function with no DOM access.

### The WebView and the bridge

[`MainActivity`](app/src/main/java/com/aimbire/dailybudget/MainActivity.java) loads
`file:///android_asset/www/index.html` into a full-screen WebView and exposes a small object to
JavaScript as `window.Android`:

| Call | When the UI calls it | What the Java side does |
| ---- | -------------------- | ----------------------- |
| `setSystemBars(bar, nav, light)` | The theme changes | Colours the status and navigation bars to match the page |
| `syncData(json)` | After every save | Keeps a copy of the budget, currency and this month's expenses for the notification |
| `setReminder(enabled, hour, minute)` | Reminder settings change | Schedules or cancels the daily alarm, and asks for notification permission on Android 13+ |
| `testReminder()` | **Send test notification** is tapped | Shows the notification immediately |

In the other direction, Java calls `window.handleBack()` when the back button is pressed (the UI
closes a sheet or settings, or switches back to the Budget tab, and returns `true`; when there's
nothing left to close it returns `false` and the app exits), and
`window.onReminderPermission(granted)` after the permission prompt. Every bridge call in `app.js`
checks that `window.Android` exists first, which is why the same files run unchanged in a desktop
browser.

### Storage

All state is a single JSON object in `localStorage` under `dailyBudget.v1`:

```js
{
  budget: 1500,
  currency: '$',
  expenses: [{ id, ts, day: '2026-09-18', amount: 12.8, note: 'Lunch' }],
  since: '2026-09-01',          // first tracked day, used by the calendar
  theme: 'dark',                // 'dark' | 'light' | 'system'
  reminder: { enabled: true, time: '08:00' },
}
```

Expenses older than 12 months are pruned at startup. The morning notification can't read the
WebView's storage without opening the app, so `syncData` also keeps this month's numbers in Android
`SharedPreferences`. [`Reminder.java`](app/src/main/java/com/aimbire/dailybudget/Reminder.java)
recomputes today's allowance from that copy (`buildMessage`), and
[`RescheduleReceiver`](app/src/main/java/com/aimbire/dailybudget/RescheduleReceiver.java) re-arms the
alarm after a reboot, an app update or a clock change.

### Project layout

```text
app/src/main/assets/www/          The UI: plain HTML, CSS and JavaScript, no build step
  index.html                      Tabs, settings page and dialogs
  budget.js                       The budget math (pure functions, no DOM)
  app.js                          Rendering, storage, settings and the bridge calls
  style.css                       Dark and light themes as CSS custom properties
app/src/main/java/com/aimbire/dailybudget/
  MainActivity.java               Full-screen WebView, system bars, back button, JS bridge
  Reminder.java                   Morning notification: scheduling and message text
  ReminderReceiver.java           Shows the notification and arms the next one
  RescheduleReceiver.java         Re-arms the alarm after a reboot, update or clock change
app/src/main/res/                 App name, launcher and notification icons, window colours
docs/                             Privacy policy, release steps and README screenshots
store/                            Google Play listing graphics and the HTML they're rendered from
tools/                            Desktop preview server and release scripts
```

## Make it your own

Some starting points if you fork it:

- **Rename it.** Change `applicationId` and `namespace` in [`app/build.gradle`](app/build.gradle),
  move the Java files out of `com/aimbire/dailybudget/` into your own package and update their
  `package` lines, and set `app_name` in
  [`res/values/strings.xml`](app/src/main/res/values/strings.xml). With its own application ID,
  your build can be installed next to the Play Store version.
- **Restyle it.** The UI colours are CSS custom properties at the top of
  [`style.css`](app/src/main/assets/www/style.css): `:root` for the dark theme and
  `:root[data-theme="light"]` for the light one. The system bars pick up `--bg` and `--surface`
  automatically. The window background shown before the page loads and the notification accent are in
  [`res/values/styles.xml`](app/src/main/res/values/styles.xml), and the icons are vector drawables
  in `res/drawable/`.
- **Change the math.** Edit `Budget.compute` in `budget.js`. The notification computes the same
  numbers in Java (`Reminder.buildMessage`), so change it there too. Ideas: weekly or payday-to-payday
  periods instead of calendar months, carrying leftovers into next month, setting aside a savings
  goal before the daily number is worked out.
- **Change what's stored.** If you change the shape of the saved data, migrate it in `load()` in
  `app.js` or bump `STORAGE_KEY`, so existing installs don't break. If the notification needs the new
  fields, add them to `syncNative()` as well.
- **Move it to another platform.** The web UI doesn't depend on Android. A different shell (an iOS
  `WKWebView`, Electron, a PWA) only has to provide the bridge calls it needs, and can skip the rest.
- **Publish your own build.** [docs/RELEASING.md](docs/RELEASING.md) covers the upload key,
  versioning and the release bundle. The store graphics in `store/` and the
  [privacy policy](docs/privacy-policy.html) belong to this app, including its contact address, so
  replace them with your own.

The helper scripts in `tools/` and `store/` are PowerShell, written for Windows. Gradle and the
browser preview work on any platform.

## Contributing

Issues and pull requests are welcome.

- **Bugs:** for a wrong number, include the monthly budget, the date and the expenses that produce
  it.
- **Keep pull requests small and focused,** and say how you tested them.
- **Keep it dependency-free.** No frameworks, packages or build step in the UI, and only the Android
  framework in Java. Staying small is the point of this repository. If your idea needs more than
  that, a fork is a great home for it.
- **Formula changes go in both places:** `budget.js` and `Reminder.buildMessage`.
- **Preview UI changes in a desktop browser.** Test on a device or emulator for anything that
  touches notifications, system bars or the back button.

## Privacy

The manifest doesn't request the `INTERNET` permission, so the app can't send data anywhere. There's
no account, analytics or ads. Everything is stored on the device, in the WebView's `localStorage`
plus the `SharedPreferences` copy used for the notification. **Reset all data** in Settings or
uninstalling the app deletes it. Android's `allowBackup` is left on, so the user's own device backup
may include it. See the full [privacy policy](docs/privacy-policy.html).

## License

No license has been chosen yet. Until one is added, default copyright applies: you can view and fork
the repository on GitHub, but no other rights to use, modify or distribute the code are granted.
