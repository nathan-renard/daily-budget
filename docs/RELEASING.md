# Releasing

Maintainer notes for publishing Daily Budget to Google Play. You don't need any of this to build or
run the app. See the [README](../README.md) for that.

The scripts are PowerShell and run from the repository root.

## 1. Create the upload key (once)

```powershell
powershell -ExecutionPolicy Bypass -File tools/create-upload-key.ps1
```

This creates `upload-keystore.jks` (PKCS12, alias `upload`, random password) and
`keystore.properties` in the repository root. Both are git-ignored, and the script refuses to
overwrite existing files. It uses `keytool` from `JAVA_HOME` if that's set, or from `PATH`.

**Back up both files somewhere safe.** Every future update must be signed with this key.

When `keystore.properties` exists, `app/build.gradle` signs release builds with the upload key.
Without it, release builds fall back to the debug key, and Play rejects debug-signed bundles.

## 2. Bump the version

In `app/build.gradle`, increase `versionCode` for every upload, because Play rejects a bundle whose
`versionCode` was already used. Usually you'll update `versionName` too.

## 3. Build the bundle

```powershell
powershell -ExecutionPolicy Bypass -File tools/bundle.ps1
```

This runs `gradlew.bat bundleRelease` and copies the result to
`dist/DailyBudget-<versionName>-<versionCode>.aab` (`dist/` is git-ignored). Upload it in Play
Console, for example under **Testing → Internal testing → Create release**. Play App Signing
re-signs it with the app signing key that Google holds.

## Store listing graphics

`store/listing/` holds the Play Store icon (512×512), the feature graphic (1024×500) and the phone
screenshots (1080×1920). The README uses plain screenshots without captions, in
[`screenshots/`](screenshots). All of them are rendered from the HTML in `store/src/` with headless
Microsoft Edge, and the screenshots show the real app UI with sample data. To regenerate them after
UI changes:

```powershell
powershell -ExecutionPolicy Bypass -File store/render.ps1
```

The script needs Microsoft Edge installed and serves the repository on port 8766 while it runs.

## Privacy policy

[`privacy-policy.html`](privacy-policy.html) is the app's privacy policy. If a change affects
permissions or how data is stored, update the policy and its "Last updated" date.
