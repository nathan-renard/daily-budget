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

## Pro (in-app products)

Pro is pay what you want: three one-time products, any of which unlocks the widget. Set them up
once in Play Console.

1. **Payments profile.** Selling anything needs a merchant account: Play Console → Settings →
   Payments profile.
2. **Upload a build with billing first.** Play Console only lets you create products after an app
   bundle that includes the Play Billing Library has been uploaded (an internal testing release is
   enough).
3. **Create the products** under Monetize with Play → Products → One-time products, with exactly
   these IDs, cheapest first:

   | Product ID   | Name in the app | Suggested price |
   | ------------ | --------------- | --------------- |
   | `pro_small`  | Small           | $1.99           |
   | `pro_medium` | Medium          | $4.99           |
   | `pro_large`  | Large           | $9.99           |

   Activate each one. Prices in other currencies can be converted automatically.
4. **Signature check.** Copy the Base64 public key from Monetize with Play → Monetization setup →
   Licensing into `PUBLIC_KEY` in
   [`Pro.java`](../app/src/main/java/com/aimbire/dailybudget/Pro.java). It isn't secret. While it's
   empty, purchases are trusted without checking their signature.
5. **Test.** Add your Google account under Settings → License testing, then install the app from the
   internal testing track. Test purchases use test cards and aren't charged. A purchase you make
   can be refunded from Order management, after which the app locks Pro again the next time it
   resumes.

After upgrading `com.android.billingclient:billing` in `app/build.gradle`, check that the merged
manifest still has no `INTERNET` permission (see the Privacy section of the README).

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
permissions or how data is stored, update the policy and its "Last updated" date. Keep Play
Console's Data safety form in step with it, including how Pro purchases are handled.
