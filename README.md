# Kokpit

**An open-source home screen for Android car head units.** Built for the cheap MediaTek
(MT8227L, Android 8.1) units sold as "Android CarPlay radios", whose stock launchers look
dated and hide system settings.

[Türkçe README](README.tr.md)

![Home screen at 1024×600](docs/screenshots/home-1024x600.png)

## What it does

- **Home screen** – large clock and date, weather (Open-Meteo, no API key), your next
  calendar event, what's playing with play/pause/skip, GPS speed, heading and trip average
  speed, and six big buttons: CarPlay, Bluetooth, Phone, Radio, Maps, Apps.
- **Hidden settings back** – screen lock (change or remove), all Android settings,
  developer options, default apps, date and time, language, and an activity explorer for
  the unit's own hidden screens.
- **On wake-up** (ignition on) – turns Wi-Fi on and joins your phone's hotspot; can open
  CarPlay when an iPhone is plugged in; plays your own intro video.
- **Diagnostics** – one screen with the unit's real Android version, screen size, Wi-Fi
  bands and vendor packages, to copy into a bug report.

Kokpit is a launcher, not a firmware. Radio, Bluetooth calls, the reversing camera,
steering-wheel keys and CarPlay stay in the unit's own apps; Kokpit opens them. Nothing
here needs root, and nothing touches the system or MCU firmware.

## Supported units

| | |
|---|---|
| Tested | MT8227L, Android 8.1 (sold as "Android 11"), 1024×600 |
| Best effort | Other landscape Android 8.1–13 head units, 800×480 up to 1920×720 |

On Android 10 and later, apps can no longer switch Wi-Fi on by themselves: Kokpit adds your
hotspot as a suggested network and shows a notification when Wi-Fi is off.

## Install with a USB stick

1. Download `kokpit-<version>.apk` from [Releases](https://github.com/yorulmazsinan/kokpit/releases)
   and copy it to a USB stick.
2. Plug the stick into the head unit, open its file manager and tap the APK. Allow
   "unknown sources" when asked.
3. Open Kokpit once and allow location and calendar.

### Make it the home screen — and undo it

- Press the home key and choose **Kokpit → Always**, or go to Android settings → Apps →
  Default apps → Home app.
- To go back: Kokpit → long-press **Apps** → Settings → System Tools →
  **Return to Stock Home Screen**, then pick the stock launcher as default the same way.
- Some units hard-wire the home key to the stock launcher. If yours does, open
  Settings → Diagnostics, save the report and open an issue with it.

## Setting things up

- **Remove the screen lock:** Settings → System Tools → *Change or Remove Screen Lock*,
  enter your current PIN, choose *None*.
- **Music on the home screen:** tap the music card and grant notification access to Kokpit
  (Android only shares what's playing with apps that have it).
- **iCloud calendar:** create an app-specific password at account.apple.com, install
  [DAVx⁵](https://www.davx5.com/), add an account with `https://caldav.icloud.com`, your
  Apple ID email and that password, then pick your calendars. Google Calendar works without
  extra steps.
- **iPhone hotspot:** turn on *Allow Others to Join* and *Maximise Compatibility* (most of
  these units only see 2.4 GHz), then enter the hotspot name and password under
  Settings → Connection.
- **Intro video:** Settings → Intro Video → *Add Videos from USB*. Videos are copied to the
  unit, so the stick can come out. MP4 (H.264), up to 20 MB; 1024×600 and 3–8 seconds look
  best. Choose one, play them in turn or at random, or turn it off.
- **Assistant button:** for now it opens any app you give it (for example ChatGPT). A
  built-in voice assistant is planned.

## Privacy

Kokpit sends one kind of request to the internet: your approximate position to
[Open-Meteo](https://open-meteo.com/) for the forecast. No analytics, no ads, no accounts.
The hotspot password is encrypted with the Android Keystore.

`QUERY_ALL_PACKAGES` is requested because a home screen has to list and launch every app.

## Building

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

JDK 17–21. Release builds are signed when `keystore.properties` (or
`~/.config/kokpit/keystore.properties`) exists.

## License

[Apache License 2.0](LICENSE)
