# Aurora TV — Code Index

Map of source files for Claude. When asked to make a change, locate the right file from this index instead of re-reading the codebase. Update this file when adding/removing files or significantly changing structure.

## Architecture Overview

Android TV IPTV client built with Jetpack Compose + Room + ExoPlayer + Xtream API.

**Layers:**
- **UI** (`ui/`) — Compose screens, dialogs, navigation
- **ViewModel** (`ui/MainViewModel.kt`) — Central state hub
- **Data** (`data/`) — Repository, Room DB, Xtream API client
- **Player** (`player/PlayerActivity.kt`) — ExoPlayer fullscreen playback
- **Settings** (`settings/SettingsRepository.kt`) — DataStore prefs
- **Sync** (`sync/SyncWorker.kt`) — WorkManager 12h periodic library refresh
- **App** (`app/`) — Application class, DI container, TV window helpers

**Entry points:**
- `MainActivity` → `AuroraTvApp` Composable
- `PlayerActivity` → fullscreen playback (launched via `PlayerActivity.createIntent`)
- `AuroraTvApplication.container: AppContainer` — lazy DI singletons

---

## Source Files

### `plan.md`
Performance review todo plan. Tracks prioritized work for lazy subtitles, lazy EPG, paged browsing, staged sync, Room indexes, indexed search, player overlay recomposition, playback launch cleanup, image caching, and benchmarking.

---

### `app/src/main/java/com/codexlabs/auroratv/ui/MainActivity.kt` (31 lines)
Launcher Activity. Sets Compose content (`AuroraTvApp` inside `AuroraTheme`). Calls `configureTvWindow()` on create and on focus regain to keep system bars hidden.

---

### `app/src/main/java/com/codexlabs/auroratv/ui/AuroraTvApp.kt` (3122 lines) ⭐ LARGEST UI FILE
Owns the entire main TV UI: screens, navigation, dialogs, tiles. **Most UI changes touch this file.**

| Composable / Function | Lines | Purpose |
|---|---|---|
| `AuroraTvApp` | 159–342 | Root state, section selection, screen router, dialog host |
| `StreamingTopNav` | 345–471 | Top nav bar (Home/Live/Movies/Series/Search/Settings, sync status) |
| `TopNavItem` | 474–517 | Focusable tab button |
| `TopNavIconOnly` | 520–556 | Icon-only nav button (Search) |
| `SetupScreen` | 559–651 | Provider connection dialog (URL/user/pass) |
| `HomeScreen` | 654–849 | Home: continue watching, top picks, featured |
| `NetflixSmallRail` | 852–888 | Horizontal carousel with title |
| `NetflixRailTile` | 891–931 | Small thumbnail tile w/ focus border |
| `NetflixPrimaryTile` | 934–1019 | Large featured tile w/ play button |
| `NetflixFeatureTile` | 1022–1102 | Large tile w/ bottom gradient |
| `NetflixPosterTile` | 1105–1145 | Poster tile for home strips |
| `HomeHero` | 1148–1222 | Hero banner (art, title, description, play) |
| `LiveTvScreen` | 1225–1322 | Live TV: categories + channels + EPG grid |
| `LiveCategoryRail` | 1325–1387 | Category sidebar |
| `LiveRailHeader` | 1390–1422 | Category rail header |
| `LiveRailItem` | 1425–1472 | Category button |
| `LiveHero` | 1475–1612 | Hero w/ current channel, now/next, play |
| `LiveEpgGrid` | 1615–1735 | EPG grid (channels × time slots) |
| `MovieScreen` | 1738–1780 | Movies: category filter + grid |
| `SeriesScreen` | 1783–1826 | Series: category filter + grid |
| `LibraryGridLayout` | 1829–1923 | Reusable category sidebar + poster grid |
| `PosterTile` | 1926–1963 | Movie/series poster |
| `SearchScreen` | 1966–2067 | Search input + grouped results |
| `SearchResultSection` | 2070–2130 | One-type result group |
| `SettingsScreen` | 2133–2390 | Settings (creds, sync, buffers, player, PIN) |
| `SettingToggleRow` | 2393–2426 | Toggle switch row |
| `SetupActionButton` | 2429–2476 | TV-friendly large button |
| `PlayerPreferenceButton` | 2479–2501 | Small preference button |
| `MovieDialog` | 2504–2585 | Fullscreen movie details + play |
| `SeriesDialog` | 2588–2787 | Fullscreen series details, season selector, episodes |

**Helpers in this file:** `dpadFocusRoute()`, `progressLabel()`, `launchPlayback()`, `homeMetadata()`, `homeDescription()`, `formatEpgClockRange()`, `darkTextFieldColors()`, `FocusCard()`, `TvActionButton()`, `Artwork()`, `EmptyState()`, `LoadingState()`, `StripCard` data class.

**For new screens:** add value to `LibrarySection` enum (in `DomainModels.kt`), add Composable here, route in `AuroraTvApp`.

---

### `app/src/main/java/com/codexlabs/auroratv/ui/MainViewModel.kt` (261 lines)
Central state hub (AndroidViewModel). Owns settings, library stats, search, favorites, continue-watching, playback resolution.

**StateFlows exposed:** `settings`, `libraryStats`, `isSyncing`, `syncMessage`, `searchQuery`, `searchResults`, `continueWatching`, `recentChannels`, `favoriteChannels`, `favoriteMovies`, `favoriteSeries`.

**Methods:** `syncAll(force)`, `saveProvider(...)`, `setAutoSync()`, `setAdultContent()`, `setPreferredPlayer()`, `setBufferProfile()`, `setEpgWindowHours()`, `setParentalPin()`, `toggleFavorite()`, `setCategoryHidden()`, `ensureSeriesLoaded()`, `ensureGuide()`, `updateSearchQuery()`, `resolvePlayback()`, `resolveAdjacentChannel()`, `registerRecentChannel()`, `updatePlaybackHistory()`, plus passthrough `observe*()` methods to repository.

**Behaviors:** auto-syncs on launch if 6+ hours since last; search debounced 250ms.

---

### `app/src/main/java/com/codexlabs/auroratv/ui/theme/AuroraTheme.kt` (34 lines)
Material 3 dark color scheme. Primary = Netflix Red `#FFE50914`, secondary = `#FFB000`, background `#050505`. Always dark regardless of system.

---

### `app/src/main/java/com/codexlabs/auroratv/app/AuroraTvApplication.kt` (7 lines)
`Application` subclass. Holds `container: AppContainer by lazy`.

---

### `app/src/main/java/com/codexlabs/auroratv/app/AppContainer.kt` (55 lines)
DI container. Singletons: `settingsRepository` (eager), `httpClient` (OkHttp 15s/90s/30s/120s timeouts, 128MB cache, logging), `database` (Room), `xtreamApi`, `repository`.

---

### `app/src/main/java/com/codexlabs/auroratv/app/TvWindow.kt` (28 lines)
Extension `ComponentActivity.configureTvWindow(keepScreenOn)`. Hides system bars (transient on swipe), enables fullscreen + cutout layout (P+), optional FLAG_KEEP_SCREEN_ON. Used by MainActivity and PlayerActivity.

---

### `app/src/main/java/com/codexlabs/auroratv/settings/SettingsRepository.kt` (109 lines)
DataStore-backed prefs. Keys: `providerBaseUrl/Username/Password`, `autoSyncEnabled`, `adultContentEnabled`, `preferredPlayer`, `bufferProfile`, `epgWindowHours`, `parentalPin`, `lastSyncEpochMillis`.

**Public:** `settings: Flow<AppSettings>`; suspend mutators (`saveProviderCredentials`, `setAutoSyncEnabled`, `setAdultContentEnabled`, `setPreferredPlayer`, `setBufferProfile`, `setEpgWindowHours`, `setParentalPin`, `markSyncCompleted`). Helper: `sanitizeBaseUrl()` strips trailing slash. EPG window clamped 12–96h. PIN max 4 digits.

**For new settings:** add key here + accessor; add UI in `SettingsScreen` (AuroraTvApp.kt); add field to `AppSettings` (DomainModels.kt).

---

### `app/src/main/java/com/codexlabs/auroratv/sync/SyncWorker.kt` (66 lines)
`CoroutineWorker`. Calls `repository.syncAll()`. Companion: `schedulePeriodic(context)` (12h, NETWORK_CONNECTED), `enqueueImmediate(context)` (one-shot).

---

### `app/src/main/java/com/codexlabs/auroratv/data/DomainModels.kt` (115 lines)
Domain types (no Room/HTTP deps).

**Enums:** `LibrarySection` (LIVE/MOVIES/SERIES/HOME/SEARCH/SETTINGS), `TargetType` (CHANNEL/MOVIE/SERIES/EPISODE, with `from(rawValue)`), `PreferredPlayer` (INTERNAL/EXTERNAL), `BufferProfile` (LOW_LATENCY/BALANCED/STABLE).

**Data classes:** `ProviderCredentials`, `AppSettings` (with `isConfigured` and `credentialsOrNull()`), `PlaybackDescriptor` (resolved playback target), `LibraryStats`, `SearchResultItem`, `SearchResults`.

---

### `app/src/main/java/com/codexlabs/auroratv/data/AppDatabase.kt` (498 lines)
Room schema + DAO + database class. Version 1, exportSchema=true, explicit destructive fallback only for unmigrated versions.

**Entities (lines 16–149):**
| Entity | PK | Notable fields |
|---|---|---|
| `CategoryEntity` | (section, remoteId) | name, isAdult, hidden, sortOrder |
| `ChannelEntity` | streamId | categoryRemoteId, channelNumber, logoUrl, epgChannelId, hasCatchup |
| `MovieEntity` | streamId | categoryRemoteId, artworkUrl, plot, rating, releaseYear |
| `SeriesEntity` | seriesId | categoryRemoteId, artworkUrl, plot, rating, releaseYear |
| `EpisodeEntity` | episodeId | seriesId, seasonNumber, episodeNumber, durationSeconds |
| `EpgEventEntity` | (channelEpgId, startEpochMillis) | endEpochMillis, title, description |
| `FavoriteItemEntity` | (targetType, targetId) | title, subtitle, artworkUrl, addedAt |
| `PlaybackHistoryEntity` | id (`"type:id"`) | positionMs, durationMs, lastPlayedAt |
| `RecentChannelEntity` | channelId | title, artworkUrl, categoryId, lastPlayedAt |

**MediaDao (lines 152–482):** upserts for each entity, clears (`clearChannels/Movies/Series/EpisodesForSeries/Epg/Categories`), Flow-returning observers (`observeVisibleCategories`, `observeChannels`, `observeMovies`, `observeSeries`, `observeEpisodes`, `observeGuide`, `observeFavoriteItems`, `observeContinueWatching`, `observeRecentChannels`, count observers), single-row lookups (`channelById`, `movieById`, `seriesById`, `episodeById`, `nextEpisodeAfter`), adjacent-channel lookups (`nextChannelInCategory`, `previousChannelInCategory`), search (`searchChannels`, `searchMovies`, `searchSeries`), favorites (`isFavorite`, `deleteFavorite`), `hiddenCategoryIds`, `setCategoryHidden`, `updateSeriesDetails`.

Soft-delete via `hidden` flag on categories. EPG queries filter by `endEpochMillis >= fromEpochMillis`.

---

### `app/src/main/java/com/codexlabs/auroratv/data/XtreamApi.kt` (876 lines)
HTTP client for Xtream provider API + XMLTV parsing.

**Payloads (lines 30–109):** `CategoryPayload`, `ChannelPayload`, `MoviePayload`, `SeriesPayload`, `EpisodePayload`, `SeriesDetailPayload`, `EpgProgramPayload`.

**Public API:**
| Method | Purpose |
|---|---|
| `fetchCategories(creds, section)` | Categories for a section |
| `streamChannels/Movies/Series(creds, adultMap, onChunk)` | Stream items in 400-row chunks |
| `fetchSeriesInfo(creds, seriesId)` | Series details + episodes |
| `streamXmlTv(creds, windowHours, onChunk)` | Stream XMLTV EPG |
| `fetchShortEpg(creds, streamId, channelEpgId)` | 24-event short EPG |
| `liveStreamUrl/movieStreamUrl/episodeStreamUrl(creds, item)` | Build playable URL (or use `directSource`) |

**Internals:** manual JsonReader parsing (no Gson/Moshi), XmlPullParser for XMLTV, robust coercion (`nextStringCoerced`, `nextLongCoerced`, etc.), date parsing (XMLTV format, epoch, ISO 8601), base64 EPG fallback, `looksAdult()` keyword detection.

---

### `app/src/main/java/com/codexlabs/auroratv/data/IptvRepository.kt` (444 lines)
Bridges DAO + API. Sync orchestrator + playback resolver.

**Flows:** `settings`, `libraryStats: StateFlow<LibraryStats>`, all `observe*` (passthrough to DAO), `observeFavoriteItems`, `observeContinueWatching`, `observeRecentChannels`.

**Methods:** `syncAll(onProgress)` (categories → channels → movies → series → EPG; preserves hidden state), `ensureSeriesLoaded(seriesId)` (lazy), `ensureGuide(streamId)` (lazy), `search(query, includeAdult)`, `resolvePlaybackDescriptor(targetType, targetId, categoryIdHint)`, `resolveAdjacentChannel(currentId, categoryId, offset, includeAdult)` (bounded next/previous DAO query), `toggleFavorite()`, `setCategoryHidden()`, `updatePlaybackHistory()`, `registerRecentChannel()`.

EPG sync is best-effort (doesn't fail full sync). Search limits: 24 results per type. History only for non-live.

---

### `app/src/main/java/com/codexlabs/auroratv/player/PlayerActivity.kt` (1436 lines) ⭐ PLAYBACK
Fullscreen ExoPlayer player + overlay controls + track selection + PiP.

**Activity (lines 155–454):** `onCreate` (build player, MediaSession, load descriptor), `onWindowFocusChanged` (re-immersive), `dispatchKeyEvent` (media keys, channel switch, overlay), `onStop` (persist position unless PiP), `onDestroy` (release).

**Intent:** `PlayerActivity.createIntent(context, targetType, targetId, categoryId, resumePositionMs)`.

**Control helpers:** `seekBy(deltaMs)`, `seekFromOverlay(deltaMs)`, `startOver()`, `playNextEpisode()`, `switchChannel(direction)` (live only, ±1 in category), `persistProgress()`, `enterPictureInPictureIfSupported()`.

**Buffer config:** `buildLoadControl(profile)` — LOW_LATENCY (2/8/0.5/1s), BALANCED (5/20/1.5/2.5s), STABLE (8/30/2.5/4s).

**Composables:**
| Composable | Lines | Purpose |
|---|---|---|
| `PlayerScreen` | 456–625 | Root: video view + overlay + sidebar + error banner |
| `PlayerOverlay` | 627–821 | HUD: title, EPG, play/seek, language pills, top actions |
| `LanguageOptionsSidebar` | 823–887 | Right-side audio/subtitle panel |
| `TrackSectionTitle` | 889–897 | Sidebar section header |
| `TrackSelectionRow` | 899–948 | Audio/subtitle row w/ checkbox |
| `PlaybackProgressBar` | 950–992 | Animated progress (red when live) |
| `TopPlayerAction` | 994–1038 | Back / Replay / Next Episode |
| `LargeRoundPlayButton` | 1040–1126 | Center play/pause w/ seek-hold repeat |
| `PlayerPill` | 1128–1169 | Bottom audio/sub pill |
| `SmallCircleControl` | 1171–1197 | Reusable circular button |

**Track utils (1199–1418):** `audioTrackOptions`, `subtitleTrackOptions`, `selectAudioTrack`, `selectSubtitleTrack`, `audioTrackLabels`, `subtitleTrackLabel`, `trackLanguageName`, `displayLanguageName`, `isLikelyLanguageTag` (regex), `extractLanguageName` (40+ known names), `audioChannelLabel` (Mono/Stereo/5.1/7.1), `audioChannelLabelFromRawText`, `rawTrackText`, `originalAudioOption`, `audioPillLabel`/`audioDrawerLabel` (adds `[Original]`), `isSameTrack`/`isSameTrackGroup`, `formatGuideTime`, `formatProgressTime`.

**Key bindings:** D-pad center / media play-pause = toggle; D-pad ←/→ = ±10s w/ repeat-on-hold (500ms initial / 100ms repeat); Back = hide overlay → exit; overlay auto-hides after 4.5s.

**Position polling:** `produceState` 100ms loop. PiP aspect ratio 16:9.

---

## Build / Manifest / Resources

### `app/build.gradle.kts` (103 lines)
- compileSdk 36, minSdk 24, targetSdk 36
- App ID `com.codexlabs.auroratv` (`.debug` suffix in debug)
- Version 0.1.0 (versionCode 1)
- JVM 17, Compose enabled, KSP for Room
- Release: minify on, debug signing
- **Deps:** Compose BOM `2026.04.01`, Lifecycle `2.9.4`, DataStore `1.2.1`, Room `2.8.4`, WorkManager `2.11.1`, Media3 `1.10.0` (exoplayer/ui/session), OkHttp `5.3.0` + logging, Coil `3.4.0`, kotlinx-coroutines `1.10.2`

### `build.gradle.kts` (root)
Plugins (apply false): AGP `8.13.0`, Kotlin `2.2.20`, Compose plugin `2.2.20`, KSP `2.2.20-2.0.3`.

### `settings.gradle.kts`
Foojay toolchain resolver, FAIL_ON_PROJECT_REPOS, includes `:app`. Root project `AuroraTV`.

### `app/src/main/AndroidManifest.xml`
Permissions: INTERNET, ACCESS_NETWORK_STATE, WAKE_LOCK, FOREGROUND_SERVICE_MEDIA_PLAYBACK. Features: `android.software.leanback` (required), touchscreen optional. Application: `AuroraTvApplication`, theme `Theme.AuroraTV`, `usesCleartextTraffic=true`. Activities: `MainActivity` (LEANBACK_LAUNCHER, landscape), `PlayerActivity` (singleTask, supportsPictureInPicture, resizeable).

### `app/src/main/res/values/strings.xml`
App name "Aurora TV", provider hints, section labels, settings strings, action labels, sync labels.

### `app/src/main/res/values/colors.xml`
`aurora_ink` `#07111E`, `aurora_surface` `#101C2B`, `aurora_surface_alt` `#16273B`, `aurora_teal` `#4BE1C3`, `aurora_copper` `#FF8A3D`, `aurora_sand` `#F4E5CF`, `aurora_red` `#FF6471`, `launcher_icon_background` `#000000`. (Note: live UI primary is Netflix-red, defined in `AuroraTheme.kt` not here.)

### `app/src/main/res/values/themes.xml`
`Theme.AuroraTV` extends `Material.NoActionBar`. Transparent status bar, black nav bar, ink background, fullscreen, no title, cutout SHORT_EDGES (P+), `forceDarkAllowed=false`.

---

## Quick Change Guide

| Change | File(s) |
|---|---|
| New screen / section | `LibrarySection` enum (DomainModels.kt) + Composable + route in `AuroraTvApp.kt` |
| New tile / card style | `AuroraTvApp.kt` (NetflixRailTile / NetflixPrimaryTile / PosterTile area) |
| Modify EPG grid | `LiveEpgGrid` in `AuroraTvApp.kt` (1615–1735) |
| Settings UI tweak | `SettingsScreen` in `AuroraTvApp.kt` (2133–2390) |
| New persisted setting | `SettingsRepository.kt` + `AppSettings` in `DomainModels.kt` + UI in SettingsScreen + `MainViewModel` setter |
| New player feature | `PlayerActivity.kt` (composable + key dispatch + ExoPlayer config) |
| Buffer tuning | `buildLoadControl` in `PlayerActivity.kt` |
| Track selection logic | Utils area in `PlayerActivity.kt` (1199–1418) |
| New DB table | Entity + DAO methods in `AppDatabase.kt`; bump `version` + handle migration; wire into `IptvRepository` |
| API field / new endpoint | Payload class + parser in `XtreamApi.kt`; map in `IptvRepository` sync |
| Sync schedule / behavior | `SyncWorker.kt` + `IptvRepository.syncAll()` |
| Theme colors | `AuroraTheme.kt` (Compose colors) and/or `colors.xml` (XML resources for splash/launcher) |
| Permissions / TV declaration | `AndroidManifest.xml` |
| Dependency bump | `app/build.gradle.kts` (deps) and root `build.gradle.kts` (plugins) |
