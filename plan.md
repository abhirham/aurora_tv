# Aurora TV Performance Plan

This plan captures the performance review findings and turns them into actionable todos. No implementation has been started.

## P1 Todos

- [ ] Replace clear-and-repopulate full sync with staged or diffed sync.
  - Current hotspot: `IptvRepository.syncAll()` clears `channels`, `movies`, `series`, and `epg_events` before refilling them.
  - Goal: avoid empty UI states, large Room invalidations, list churn, and image reloads during sync.
  - Implementation direction: write incoming rows into staging tables or use diff-aware upserts, then prune stale rows only after each provider fetch succeeds.
  - Verify: syncing a large provider does not blank visible library screens, and Room observer emissions are reduced.

- [ ] Stop materializing full category result sets for browsing.
  - Current hotspot: DAO `observeChannels`, `observeMovies`, and `observeSeries` return full `List`s per category.
  - Goal: load only the visible/windowed portion of large live/movie/series categories.
  - Implementation direction: add Paging 3 or explicit limit/offset/window queries per category, then wire Compose lists/grids to paged data.
  - Verify: large categories do not allocate thousands of entities before the first frame of the screen.

- [ ] Make EPG loading lazy by default.
  - Current hotspot: `XtreamApi.streamXmlTv()` still downloads and parses the full XMLTV response, even though it filters to a time window.
  - Goal: reduce sync time, network use, parser CPU, and DB churn.
  - Implementation direction: prefer `get_short_epg` per focused/played channel, cache per `channelEpgId`, and make full XMLTV optional/background enrichment.
  - Verify: initial sync can complete without full XMLTV, and live guide data appears on demand for focused channels.

- [ ] Lazy-load subtitles/text tracks.
  - Current hotspot: `PlayerActivity.playDescriptor()` prepares the media item with default track selection, so default text tracks can be fetched immediately.
  - Goal: avoid subtitle fetch/parsing cost until the viewer explicitly turns subtitles on.
  - Implementation direction: disable `C.TRACK_TYPE_TEXT` before `player.prepare()`, then enable and override a selected subtitle track only from the language rail.
  - Verify: subtitle tracks are off by default, selectable from the rail, and do not regress audio selection or clean track labels.

- [ ] Stop the duplicate sync trigger and per-emission WorkManager re-scheduling.
  - Current hotspot: `MainViewModel.init { settings.collect { … schedulePeriodic … maybeRefreshOnLaunch } }` (`MainViewModel.kt:67-76`) plus `LaunchedEffect(settings.isConfigured) { syncAll(force = false) }` in `AuroraTvApp.kt:174-178`.
  - Goal: avoid re-enqueueing periodic work and re-checking sync cadence on every unrelated settings change (PIN, EPG hours, buffer, adult, preferred player).
  - Implementation direction: split the init collector into a one-shot first-config effect and a focused listener keyed on `(isConfigured, autoSyncEnabled)` via `distinctUntilChangedBy`; remove the redundant `LaunchedEffect` sync trigger in `AuroraTvApp`.
  - Verify: toggling any non-sync setting does not enqueue a new periodic work or call `repository.syncAll`.

- [ ] Stop writing DataStore on every keystroke for EPG hours and parental PIN.
  - Current hotspot: `OutlinedTextField` `onValueChange` calls `onEpgWindowChanged` and `onPinChanged` per character (`AuroraTvApp.kt:2284-2287, 2300-2303`).
  - Goal: avoid disk write storms and the cascading settings-emission → ViewModel collector → WorkManager re-enqueue chain.
  - Implementation direction: hold the field as local Compose state, commit to DataStore on focus loss, explicit save, or debounced ~500 ms idle.
  - Verify: typing a 4-digit PIN results in a single DataStore commit and no WorkManager re-schedule mid-typing.

## P2 Todos

- [ ] Add composite Room indexes for hot queries.
  - Current hotspot: existing indexes mostly cover category ids only.
  - Goal: speed category browsing, guide lookup, history/favorites, and channel zapping.
  - Implementation direction: add indexes for category/adult/sort patterns, `epg_events(channelEpgId, endEpochMillis, startEpochMillis)`, favorites/history recency, and adjacent-channel navigation.
  - Verify: `EXPLAIN QUERY PLAN` shows index usage for browse, guide, and adjacent-channel queries.

- [ ] Replace leading-wildcard `LIKE` search with an indexed search path.
  - Current hotspot: `searchChannels`, `searchMovies`, and `searchSeries` use `%query%` scans.
  - Goal: keep search responsive on catalogs with tens of thousands of rows.
  - Implementation direction: add FTS tables or a normalized search index with section/type filters and stable ranking.
  - Verify: search latency stays low on the captured provider-sized catalog.

- [ ] Reduce player overlay progress recomposition frequency.
  - Current hotspot: `PlayerScreen` updates position/duration state every 100 ms.
  - Goal: reduce Compose work while preserving smooth enough TV progress feedback.
  - Implementation direction: poll only while overlay is visible, use a 250-500 ms interval, or isolate progress state to the smallest composable that needs it.
  - Verify: overlay remains responsive and progress display updates cleanly without whole-screen churn.

- [ ] Avoid resolving playback twice for internal player launches.
  - Current hotspot: `launchPlayback()` resolves a descriptor before starting `PlayerActivity`, then `PlayerActivity` resolves it again from intent ids.
  - Goal: reduce playback startup latency and duplicate recent-channel writes.
  - Implementation direction: either pass the resolved descriptor/media URL to `PlayerActivity`, or only resolve inside the destination activity and keep external-player resolution separate.
  - Verify: internal playback launch does one descriptor resolution and still supports resume position, live category hints, and external player mode.

- [ ] Drop the hidden-category `NOT IN` subquery from observed and search queries.
  - Current hotspot: `MediaDao.observeChannels`/`observeMovies`/`observeSeries` and `searchChannels`/`searchMovies`/`searchSeries` (`AppDatabase.kt:226-228, 242-244, 255-257, 325-327, 338-340, 352-354`) include `categoryRemoteId NOT IN (SELECT remoteId FROM categories WHERE section = 'X' AND hidden = 1)`. The subquery defeats `index_*_categoryRemoteId` and runs on every observe.
  - Goal: let the optimizer use the existing category index instead of falling back to a hash join against `categories`.
  - Implementation direction: denormalize a `categoryHidden` boolean onto each entity, updated when `setCategoryHidden` runs and on category sync; filter with `WHERE categoryHidden = 0`.
  - Verify: `EXPLAIN QUERY PLAN observeChannels` shows index seek on `index_channels_categoryRemoteId` with no subquery scan.

- [ ] Run sync network calls in parallel where independent.
  - Current hotspot: `IptvRepository.syncAll` runs categories → channels → movies → series → EPG sequentially (`IptvRepository.kt:84-185`).
  - Goal: cut sync wall-clock by overlapping HTTP downloads while serializing only DB writes.
  - Implementation direction: structured concurrency with `coroutineScope { launch { syncChannels } ; launch { syncMovies } ; launch { syncSeries } ; launch { syncEpg } }`; gate Room writes behind a Mutex if necessary.
  - Verify: a full sync on a representative provider completes faster than the sequential baseline; per-section progress messages still surface.

- [ ] Stop wiring the OkHttp logging interceptor in release builds.
  - Current hotspot: `AppContainer.kt:21-37` always adds the interceptor; only the level differs in release.
  - Goal: remove per-request interceptor overhead in release.
  - Implementation direction: `if (BuildConfig.DEBUG) builder.addInterceptor(loggingInterceptor)`.
  - Verify: release `OkHttpClient.interceptors()` does not contain the logging interceptor.

- [ ] Gate `PlayerActivity` settings collection on lifecycle state.
  - Current hotspot: `PlayerActivity.kt:218-222` runs `lifecycleScope.launch { viewModel.settings.collect { … } }` without a state filter.
  - Goal: stop collecting settings while the player activity is stopped (PiP, off-screen).
  - Implementation direction: wrap with `repeatOnLifecycle(Lifecycle.State.STARTED)` or use `flowWithLifecycle`.
  - Verify: settings flow is paused while `PlayerActivity` is in `Lifecycle.State.CREATED`.

- [ ] Hoist per-recompose / per-call allocations (Brushes, Formatters, Regexes, factories).
  - Current hotspots:
    - `Artwork` builds a `Brush.linearGradient` per recompose (`AuroraTvApp.kt:2944-2954`).
    - `formatGuideTime` / `formatEpgClockRange` / `formatProgressTime` build `SimpleDateFormat` and `String.format` per call (`PlayerActivity.kt:1483, 1487`, `AuroraTvApp.kt:3030`).
    - `parseXmlTvDate` and `parseFlexibleTime` rebuild `DateTimeFormatter` instances per call (`XtreamApi.kt:818-822, 836-839`).
    - `audioChannelLabelFromRawText` and `extractLanguageName` compile fresh `Regex` per call (`PlayerActivity.kt:1407-1413, 1428-1431`).
    - `XmlPullParserFactory.newInstance()` constructed per sync (`XtreamApi.kt:584`).
  - Goal: cut steady-state allocations and CPU during scroll, sync, and playback.
  - Implementation direction: hoist to top-level `private val`s; cache compiled `Regex` and `DateTimeFormatter` instances; reuse the `XmlPullParserFactory`.
  - Verify: allocation profiler shows no per-recompose `Brush`/`Regex`/`SimpleDateFormat` allocations on Movies grid scroll.

- [ ] Memoize `HomeScreen` mapped strip lists.
  - Current hotspot: `AuroraTvApp.kt:668-718` rebuilds five `StripCard` lists from Flow data on every recompose; downstream `remember(...)` keys depend on these new instances.
  - Goal: drop wasted allocations and let `remember` cache the combined lists across non-data recompositions.
  - Implementation direction: wrap each `.map { StripCard(...) }` in `remember(<source list>)` so the mapping is keyed on the source flow output.
  - Verify: focusing/blurring tiles on Home no longer triggers re-mapping in the allocation profiler.

- [ ] Debounce / cache the live guide Flow on focus changes.
  - Current hotspot: `AuroraTvApp.kt:1264-1267` re-creates a new `observeGuide` Flow every time the highlighted channel changes; rapid focus traversal cancels and re-collects per row.
  - Goal: avoid Flow churn while the user scans the live channel list.
  - Implementation direction: debounce the keying value (e.g., `snapshotFlow { highlightedChannel?.streamId }.debounce(150)`) before reattaching the guide Flow, or maintain a short-lived guide cache keyed by stream id.
  - Verify: scrolling 30 channels triggers ≤10 guide observations.

- [ ] Stop auto-scrolling `LiveCategoryRail` during sync emissions.
  - Current hotspot: `AuroraTvApp.kt:1339-1343` calls `listState.scrollToItem(selectedIndex)` whenever `categories.size` changes; during a chunked sync this jumps repeatedly.
  - Goal: keep user scroll position stable when the categories list re-emits with the same selection.
  - Implementation direction: gate the scroll on selection actually changing (`LaunchedEffect(selectedCategoryId)`), or skip when `listState.isScrollInProgress`.
  - Verify: a sync that re-emits categories does not move the rail's scroll position.

## Follow-Up Performance Work

- [ ] Add in-flight de-dupe for `ensureGuide()` and `ensureSeriesLoaded()` so rapid focus/dialog changes cannot trigger duplicate provider calls.
- [ ] Configure a shared Coil `ImageLoader` strategy with explicit memory/disk cache sizing and thumbnail-sized requests for rails, grids, logos, and dialog art.
- [ ] Add Macrobenchmark coverage for cold start, home load, live category navigation, movie/series grid browsing, search, player launch, and channel zapping.
- [ ] Add Baseline Profile generation for the main TV navigation and player startup paths.
- [ ] Add provider-sized test fixtures or seeded debug data so paging, search, and sync changes can be measured against realistic catalog volume.

- [ ] Eliminate `delay(50)` focus shims across screens.
  - Hotspots: `StreamingTopNav` (`AuroraTvApp.kt:366`), `SetupScreen` (569), `MovieDialog` (2512), `SeriesDialog` (2605), `LanguageOptionsSidebar` and `PlayerOverlay` (`PlayerActivity.kt:694, 919`), `SettingsScreen` (2162) — among others.
  - Goal: stable, non-shifting focus per Aurora focus-navigation guidance.
  - Implementation direction: combine `Modifier.focusRestorer()` with `LaunchedEffect` driven by `snapshotFlow { focusRequester.attached }.first { it }`; or migrate to `androidx.tv:tv-foundation` lazy lists which handle initial focus internally.
  - Verify: no perceptible focus flicker on tab change, dialog open, or section switch.

- [ ] Memoize player track-option recompute on `onTracksChanged`.
  - Current hotspot: `PlayerActivity.kt:547-550` rebuilds full audio/subtitle option lists on every tracks change; track parsing recompiles regexes and queries `Locale` display names per track.
  - Goal: cut work during HLS segment-format transitions and stream switches.
  - Implementation direction: cache by `(language, label, channelCount, roleFlags)` keys; reuse last result when keys are unchanged.
  - Verify: rapid `onTracksChanged` events on a live HLS stream do not allocate new `AudioTrackOption` instances when content is the same.

- [ ] Replace empty ProGuard/R8 rule file with explicit project rules and verify shrinking.
  - Current hotspot: `app/proguard-rules.pro` is one comment line.
  - Goal: explicit safety for Room generated DAOs, Coil 3 service-loader factories, and Media3 reflective lookups so we can run R8 full mode safely.
  - Implementation direction: add `-keep` rules for Room, Coil ImageLoader factories, and Media3 renderer factories; spot-check `usage.txt` and `mapping.txt`.
  - Verify: a release APK installs and runs end-to-end (sync, playback, track switch) with no crash.

- [ ] Configure a real release signing config (or document the debug-signed release as intentional).
  - Current hotspot: `app/build.gradle.kts:28` ties release to `signingConfigs.getByName("debug")`.
  - Goal: enable real release builds without breaking sideload upgrades.
  - Implementation direction: add a `release` signing config sourced from environment variables or a keystore file; keep a fallback for local sideload.
  - Verify: `./gradlew :app:assembleRelease` produces a release-signed APK on CI; debug-signed builds still possible locally.

- [ ] Replace blanket `usesCleartextTraffic="true"` with a scoped network security config.
  - Current hotspot: manifest grants global cleartext.
  - Goal: keep IPTV provider compatibility while limiting cleartext to known hosts.
  - Implementation direction: drive cleartext from the saved provider hostname via a runtime-installed network security config, or restrict cleartext to specific configured hosts.
  - Verify: HTTPS-only providers cannot fall back to plaintext if their host changes.

- [ ] Adopt `androidx.tv:tv-foundation` (and possibly `tv-material`) for lazy lists/grids and focus.
  - Current hotspot: foundation `LazyRow`/`LazyColumn`/`LazyVerticalGrid` plus manual `dpadFocusRoute` plumbing across `AuroraTvApp.kt`.
  - Goal: native TV focus traversal, initial focus, and remote-key acceleration.
  - Implementation direction: migrate one screen at a time (Home → Live → Movies → Series → Search) to `TvLazyRow`/`TvLazyColumn`; keep the existing `FocusCard` look.
  - Verify: D-pad navigation feels snappier with no focus shims.

- [ ] Add channel up/down and media FF/REW remote key handling.
  - Current hotspot: `PlayerActivity.dispatchKeyEvent` only handles `MEDIA_PLAY_PAUSE` (`PlayerActivity.kt:295`).
  - Goal: support full TV remote semantics.
  - Implementation direction: handle `KEYCODE_CHANNEL_UP`/`DOWN` by calling `switchChannel(±1)` on live; handle `KEYCODE_MEDIA_FAST_FORWARD`/`REWIND` by calling `seekBy(±SEEK_STEP_MS)` on VOD.
  - Verify: Shield-style remotes can FF/REW VOD and zap channels live.

- [ ] Pre-warm adjacent live channels for fast zapping.
  - Current hotspot: `switchChannel` builds a fresh `MediaItem` and calls `prepare()` per swap; the first segment must download.
  - Goal: near-instant channel changes.
  - Implementation direction: when the user dwells on a focused channel in `LiveEpgGrid`, kick off a low-priority `MediaSource.load()` for ±1 channels using `DefaultMediaSourceFactory`.
  - Verify: typical channel zap feels closer to a TV box than current.

- [ ] Tie `MediaItem.LiveConfiguration.targetOffsetMs` to `BufferProfile`.
  - Current hotspot: `playDescriptor` builds `LiveConfiguration.Builder().build()` with defaults (`PlayerActivity.kt:365-368`).
  - Goal: let the buffer slider influence how close to the live edge users sit.
  - Implementation direction: map LOW_LATENCY/BALANCED/STABLE to target offsets (e.g., 1.5 s / 6 s / 12 s) and set on the LiveConfiguration.
  - Verify: switching profiles visibly changes live latency.

- [ ] Implement a real `MediaSessionService` (or drop the `FOREGROUND_SERVICE_MEDIA_PLAYBACK` permission).
  - Current hotspot: `AndroidManifest.xml` grants the permission but no service is declared and `onStop` pauses playback (`PlayerActivity.kt:328-331`).
  - Goal: align the manifest with actual playback behavior; optionally enable Now Playing tiles.
  - Implementation direction: either add a `MediaSessionService` for background continuation or remove the unused permission.
  - Verify: lint passes; manifest matches behavior.

- [ ] Add a cold-start splash composition before the Home screen renders.
  - Current hotspot: `MainActivity.kt` immediately sets `AuroraTvApp` content; first paint can show empty black until Flows emit.
  - Goal: smoother cold start with a branded loading state.
  - Implementation direction: show `LoadingState` until the first `settings` emission and at least one home Flow has emitted.
  - Verify: cold-start timeline shows a non-blank first frame.

- [ ] Add Room migrations and remove `fallbackToDestructiveMigration` outside debug.
  - Current hotspot: `AppDatabase.kt:495` falls back destructively for unmigrated versions.
  - Goal: avoid wiping a user's library on every schema change post-1.0.
  - Implementation direction: introduce a versioning helper, write per-version migration objects, and only allow destructive fallback in `BuildConfig.DEBUG`.
  - Verify: bumping schema with a stub migration preserves user data on upgrade.

- [ ] Add `@RewriteQueriesToDropUnusedColumns` and project narrower list-item entities for grids.
  - Current hotspot: `MediaDao.observeChannels`/`observeMovies`/`observeSeries` return all entity columns; the UI uses 4–5 of them.
  - Goal: reduce row width and Compose recomposition cost on large grids.
  - Implementation direction: annotate the DAO; add `MovieListItem`/`ChannelListItem`/`SeriesListItem` projections for grid screens.
  - Verify: row width on `observeMovies` drops; grid scroll allocations reduce.

- [ ] Split `AuroraTvApp.kt` into per-screen and per-component files.
  - Current hotspot: 3 122-line single file holding all screens, dialogs, tiles, helpers.
  - Goal: faster incremental builds, tighter Compose stability inference, easier review.
  - Implementation direction: one file per screen (`HomeScreen.kt`, `LiveTvScreen.kt`, `MoviesScreen.kt`, `SeriesScreen.kt`, `SearchScreen.kt`, `SettingsScreen.kt`), one per dialog, shared components under `ui/components/`.
  - Verify: incremental rebuild after a UI change is meaningfully faster.

- [ ] Negative-cache `fetchShortEpg` for channels with no provider EPG mapping.
  - Current hotspot: `IptvRepository.ensureGuide` (`IptvRepository.kt:365-385`) calls `fetchShortEpg` whenever `futureGuideCount = 0`; channels without EPG re-fetch on every focus dwell.
  - Goal: avoid redundant network calls for channels that will never have EPG.
  - Implementation direction: in-memory negative cache keyed on `epgChannelId` with TTL (e.g., 1 h); optionally a sentinel row in `epg_events`.
  - Verify: focusing 10 channels with no EPG triggers at most 10 network calls per TTL.

- [ ] Make the EPG lower bound user-configurable (or drop the hardcoded 4 h look-back).
  - Current hotspot: `XtreamApi.kt:588` hardcodes `lowerBound = now - 4h` while only `windowHours` (upper) is user-configurable.
  - Goal: predictable, explicit EPG window in both directions.
  - Implementation direction: add an `epgLookbackHours` setting (or default to 0) and surface in Settings if exposed.
  - Verify: EPG storage size matches the configured window in both directions.

- [ ] Remove `room.expandProjection = true` if not strictly needed.
  - Current hotspot: `app/build.gradle.kts:60`.
  - Goal: avoid known compile-time pitfalls in newer Room versions.
  - Implementation direction: drop the flag; verify schema and queries compile and behave the same.
  - Verify: `:app:kspDebugKotlin` succeeds; runtime queries unchanged.

- [ ] Add Compose stability/recomposition reporting to CI.
  - Goal: catch unstable parameters and recomposition regressions early.
  - Implementation direction: run `./gradlew :app:assembleRelease -PenableComposeCompilerReports=true` in CI; archive reports as build artifacts.
  - Verify: reports surface in CI artifacts; regressions noticeable in PR review.

- [ ] Remove dead code in `StreamingTopNav` (zero-sized placeholder Box).
  - Current hotspot: `AuroraTvApp.kt:396-401` — `Box(Modifier.width(0.dp).height(0.dp))`.
  - Goal: cleanliness; avoid composing an invisible no-op.
  - Implementation direction: delete.
  - Verify: layout unchanged.

## Suggested Implementation Order

1. Lazy subtitles in `PlayerActivity.kt`.
2. EPG lazy-first path and in-flight guide de-dupe.
3. Room indexes and query-plan validation.
4. Paging/windowed browse queries for live, movies, and series.
5. Staged/diffed sync to stop full-library invalidation.
6. FTS or normalized indexed search.
7. Player overlay polling reduction.
8. Playback launch descriptor cleanup.
9. Coil cache/request sizing.
10. Macrobenchmark and Baseline Profile coverage.
11. Stop duplicate sync triggers and per-keystroke DataStore writes (Stage 1 quick wins).
12. Hoist per-recompose / per-call allocations (Brushes, Formatters, Regexes).
13. Drop hidden-category subquery; denormalize `categoryHidden`.
14. Parallelize sync network calls; strip release logging interceptor.
15. Memoize HomeScreen mapped lists; debounce live guide flow on focus; stop sync-time rail auto-scroll.
16. Eliminate `delay(50)` focus shims; adopt `androidx.tv:tv-foundation`.
17. Channel up/down + media FF/REW handling; pre-warm adjacent live channels; tie LiveConfiguration to buffer profile.
18. Add Room migrations; `@RewriteQueriesToDropUnusedColumns`; narrow grid projections.
19. Split `AuroraTvApp.kt` into per-screen files.
20. Manifest hygiene (signing, network security config, MediaSessionService or permission removal); ProGuard rules; cold-start splash.
