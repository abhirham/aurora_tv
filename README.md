# Aurora TV

Aurora TV is a single-provider Xtream IPTV app built from scratch for Android TV and Google TV Streamer 4K.

## Included

- Live TV with internal Media3 playback
- Movies and series browsing
- Xtream login for one provider
- Full category sync into Room
- XMLTV guide sync plus short-EPG fallback
- Search across channels, movies, and series
- Favorites, recent channels, and continue watching
- Series detail loading with seasons and episodes
- Internal or external player preference
- Picture-in-picture for the internal player
- Adult-content visibility toggle
- Hidden groups per section
- Periodic background sync with WorkManager

## Performance Strategy

Your provider size is large:

- 26,317 live channels
- 48,334 movies
- 6,155 shows

The app is set up to handle that by:

- streaming Xtream JSON responses instead of loading them fully into memory
- inserting library data into Room in chunks
- storing guide data locally for faster browsing
- loading series episodes on demand instead of syncing every episode up front
- keeping playback and UI state separate from sync work

## Stack

- Android Gradle Plugin `9.2.0`
- Kotlin `2.2.20`
- Compile SDK `36`
- Min SDK `24`
- Compose BOM `2026.04.01`
- Media3 `1.10.0`
- Room `2.8.4`
- WorkManager `2.11.1`
- OkHttp `5.3.0`
- Coil `3.4.0`

## Project Layout

- `app/src/main/java/com/codexlabs/auroratv/app` application bootstrap and dependency container
- `app/src/main/java/com/codexlabs/auroratv/data` database, Xtream client, repository, and shared models
- `app/src/main/java/com/codexlabs/auroratv/player` internal TV player activity
- `app/src/main/java/com/codexlabs/auroratv/settings` DataStore-backed settings
- `app/src/main/java/com/codexlabs/auroratv/sync` periodic background sync worker
- `app/src/main/java/com/codexlabs/auroratv/ui` Compose TV screens and state

## Build Notes

This workspace did not have a local Java runtime or Gradle installed, so the project could not be compiled or the Gradle wrapper generated here.

To build locally:

1. Install JDK 17.
2. Open the project in Android Studio.
3. Let Android Studio sync the Gradle project.
4. Generate the Gradle wrapper if you want CLI builds.
5. Run on a Google TV device or Android TV emulator.

## Current Defaults

- one Xtream provider only
- cleartext traffic allowed because many IPTV providers still use HTTP
- balanced playback buffer profile by default
- background sync every 12 hours when auto-sync is enabled
- 48-hour EPG window by default
