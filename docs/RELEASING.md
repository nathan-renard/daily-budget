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
   | `pro_small`  | Small           | $0.99           |
   | `pro_medium` | Medium          | $2.99           |
   | `pro_large`  | Large           | $4.99           |

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

`store/listing/` holds the Play Store icon (512×512), the feature graphic (1024×500), the phone
screenshots (1080×1920) and the promo video's YouTube thumbnail (1280×720). The README uses plain
screenshots without captions, in
[`screenshots/`](screenshots). All of them are rendered from the HTML in `store/src/` with headless
Microsoft Edge, and the screenshots show the real app UI with sample data. To regenerate them after
UI changes:

```powershell
powershell -ExecutionPolicy Bypass -File store/render.ps1
```

The script needs Microsoft Edge installed and serves the repository on port 8766 while it runs.

## Promo video

`store/listing/promo-video-1920x1080.mp4` is the listing's promo video: about 20 seconds of the
real app UI (the same sample data as the screenshots) with captions, cut to `promo_music.mp3` in
the repository root so that every tap and screen change lands on a beat. Neither file is in git:
the music is
[Promo Promotional Showreel Music](https://pixabay.com/music/lofi-promo-promotional-showreel-music-566673/)
by APALONBeats, which the Pixabay Content License lets you use in a video but not redistribute on
its own, and the video contains it. To render the video, download the track to `promo_music.mp3`
first (the timeline in `render.js` is tuned to it), or put your own track there. Play Console only
takes a YouTube link for the promo
video, so the file goes up to YouTube as an unlisted video (see
[Uploading to YouTube](#uploading-to-youtube)).

It's rendered by [`store/promo/render.js`](../store/promo/render.js), which drives the app in
headless Chromium with a virtual clock (so every frame is exact) and needs Node, ffmpeg and
[uv](https://docs.astral.sh/uv/) (for the beat analysis in `beats.py`, via librosa) on the PATH:

```sh
cd store/promo
npm install && npx playwright install chromium   # once
node render.js
```

The captions and the tap schedule are at the top of `render.js`; the stage layout is
`composer.html`. If the music changes, check `DROP_BEAT` and the opening question's beats against
the printed beat grid (and the kick strength per beat) so the phone still lands on the drop.

### Uploading to YouTube

Upload the video as unlisted: it plays on the store listing but doesn't show up in YouTube search or
on the channel page. In YouTube Studio, choose **Create → Upload videos**:

1. **Details.**
   - **Title:** `Daily Budget for Android: know what you can spend today`
   - **Description:** the text below.
   - **Thumbnail:** upload `store/listing/youtube-thumbnail-1280x720.png` (rendered by
     `store/render.ps1` from [`store/src/thumbnail.html`](../store/src/thumbnail.html)). Custom
     thumbnails need a verified channel, so YouTube may ask for a phone number the first time.
   - **Audience:** No, it's not made for kids. No age restriction, because Play won't show an
     age-restricted video.
   - Under **Show more**: Altered content **No**, Category **Science & Technology**, and leave
     **Allow embedding** on, because Play plays the video embedded. Turning comments off saves
     moderating them.
2. **Monetization** (only shown if the channel is in the YouTube Partner Program): **Off**. Play
   doesn't show a video that has ads.
3. **Checks.** Wait for the copyright check. A Content ID claim on the music lets the rights holder
   put ads on the video even with monetization off, and then Play won't show it. The Pixabay track
   is registered with Content ID, so expect one. Download the license certificate from the track's
   Pixabay page, then in YouTube Studio open the video under **Content → Copyright → See details →
   Dispute**, choose **I have permission or a license**, and give the track's title, its Pixabay
   link and the certificate.
4. **Visibility:** **Unlisted**.

The description:

```text
Daily Budget turns your monthly budget into one number: how much you can spend today. Spend less today and tomorrow's number grows. Spend more and it shrinks, so the month still adds up.

• Log an expense in two taps
• See every expense by day, and a calendar of green and red days
• Dark, AMOLED and light themes
• A home-screen widget with Pro, for whatever you want to pay
• Free and offline, with no account and no ads

Get it on Google Play: https://play.google.com/store/apps/details?id=com.aimbire.dailybudget
```

Then add it to the listing: in Play Console, open **Grow users → Store presence → Store listings**,
open the default listing, paste the video's full link (`https://www.youtube.com/watch?v=<id>`, not a
`youtu.be`, Shorts or playlist link, and without a timestamp) into the video field and save. Play
uses the feature graphic as the video's cover, with a play button over its centre, and autoplays up
to 30 seconds muted, so the captions have to carry the video without sound.

## Privacy policy

[`privacy-policy.html`](privacy-policy.html) is the app's privacy policy. If a change affects
permissions or how data is stored, update the policy and its "Last updated" date. Keep Play
Console's Data safety form in step with it, including how Pro purchases are handled.
