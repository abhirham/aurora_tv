# Aurora TV IPTV Data Schema

Captured from the running debug app on the connected TV and cross-checked against the Room schema and Xtream parser code.

- Debug package: `com.codexlabs.auroratv.debug`
- Device transport: `adb-58091HFAG14Z1E-sO5zq5._adb-tls-connect._tcp`
- Room database: `aurora_tv.db`
- Room schema version: `1`
- Provider credentials are stored in DataStore and are intentionally not included here.

## Source Overview

The app reads IPTV provider data through Xtream-compatible endpoints:

| Source | Endpoint/action | Stored in |
| --- | --- | --- |
| Live categories | `player_api.php?action=get_live_categories` | `categories` with `section = 'live'` |
| Movie categories | `player_api.php?action=get_vod_categories` | `categories` with `section = 'movies'` |
| Series categories | `player_api.php?action=get_series_categories` | `categories` with `section = 'series'` |
| Live streams | `player_api.php?action=get_live_streams` | `channels` |
| VOD streams | `player_api.php?action=get_vod_streams` | `movies` |
| Series list | `player_api.php?action=get_series` | `series` |
| Series detail | `player_api.php?action=get_series_info&series_id=<id>` | `episodes`, plus detail updates on `series` |
| XMLTV guide | `xmltv.php` | `epg_events` |
| Short EPG fallback | `player_api.php?action=get_short_epg&stream_id=<id>&limit=24` | `epg_events` |

The app also has app-local tables for favorites, continue-watching progress, and recent live channels. These are not fetched from the IPTV source, but they reference IPTV-backed items.

## Live Database Snapshot

Row counts observed from the running debug database:

| Table | Rows | Source |
| --- | ---: | --- |
| `categories` | 358 | IPTV categories |
| `channels` | 26956 | IPTV live streams |
| `movies` | 48350 | IPTV VOD streams |
| `series` | 6157 | IPTV series list |
| `episodes` | 0 | IPTV series detail, fetched lazily when a series is opened |
| `epg_events` | 20990 | XMLTV and short EPG |
| `favorite_items` | 0 | App-local |
| `playback_history` | 2 | App-local |
| `recent_channels` | 2 | App-local |

Category breakdown observed from `categories`:

| Section | Categories | Adult | Hidden |
| --- | ---: | ---: | ---: |
| `live` | 195 | 2 | 0 |
| `movies` | 96 | 0 | 0 |
| `series` | 67 | 0 | 0 |

Additional observed guide/library details:

| Metric | Count |
| --- | ---: |
| Channels with non-empty EPG channel id | 14983 |
| Channels with catchup enabled | 61 |
| Distinct EPG channel ids with guide rows | 909 |
| Distinct category ids referenced by channels | 196 |
| Distinct category ids referenced by movies | 97 |
| Distinct category ids referenced by series | 92 |

## SQLite Conventions

- `TEXT` maps to Kotlin `String`.
- `INTEGER` maps to Kotlin `Long`, `Int`, or `Boolean`.
- Booleans are stored as `INTEGER` values, normally `0` or `1`.
- Nullable columns are marked `NULL`; required columns are marked `NOT NULL`.
- `addedAt` fields are stored as raw Xtream numeric values from the provider. The code does not normalize these to milliseconds.
- EPG `startEpochMillis` and `endEpochMillis` are stored as epoch milliseconds.
- The Room schema does not declare foreign key constraints. Relationship notes below are logical app relationships.

## IPTV-Fed Tables

### `categories`

Stores live, movie, and series category rows from the three category endpoints.

Source fields:

| Source field | Local column | Notes |
| --- | --- | --- |
| `category_id` | `remoteId` | Provider category id as text |
| `category_name` | `name` | Falls back to `Category <id>` when blank |
| Endpoint section | `section` | One of `live`, `movies`, `series` |
| Derived | `isAdult` | True when category name looks adult |
| App state | `hidden` | Preserved across category refreshes |
| Fetch order | `sortOrder` | Zero-based order within the fetched section |

Schema:

| Column | Type | Null | Key | Notes |
| --- | --- | --- | --- | --- |
| `section` | `TEXT` | `NOT NULL` | PK 1 | `live`, `movies`, or `series` |
| `remoteId` | `TEXT` | `NOT NULL` | PK 2 | Provider category id |
| `name` | `TEXT` | `NOT NULL` |  | Display name |
| `isAdult` | `INTEGER` | `NOT NULL` |  | Derived boolean |
| `hidden` | `INTEGER` | `NOT NULL` |  | User visibility setting |
| `sortOrder` | `INTEGER` | `NOT NULL` |  | Provider order within section |

Primary key: (`section`, `remoteId`)

```sql
CREATE TABLE `categories` (
  `section` TEXT NOT NULL,
  `remoteId` TEXT NOT NULL,
  `name` TEXT NOT NULL,
  `isAdult` INTEGER NOT NULL,
  `hidden` INTEGER NOT NULL,
  `sortOrder` INTEGER NOT NULL,
  PRIMARY KEY(`section`, `remoteId`)
);
```

### `channels`

Stores live TV streams from `get_live_streams`.

Source fields:

| Source field | Local column | Notes |
| --- | --- | --- |
| `stream_id` | `streamId` | Required; rows with missing or non-positive ids are skipped |
| `category_id` | `categoryRemoteId` | Logical reference to `categories.remoteId` where `section = 'live'` |
| `name` | `name` | Falls back to `Channel <streamId>` when blank |
| `num` | `channelNumber` | Nullable channel ordering number |
| `stream_icon` | `logoUrl` | Blank values stored as null |
| `epg_channel_id` | `epgChannelId` | Logical link to `epg_events.channelEpgId` |
| `container_extension` | `containerExtension` | Used for stream URL fallback, default `ts` |
| `direct_source` | `directSource` | Overrides generated stream URL when present |
| `custom_sid` | `customSid` | Stored for provider-specific stream metadata |
| `tv_archive` | `hasCatchup` | Coerced boolean |
| `tv_archive_duration` | `catchupDurationHours` | Coerced integer |
| `added` | `addedAt` | Raw provider timestamp/value |
| Derived | `isAdult` | Category adult flag or name adult keyword match |

Schema:

| Column | Type | Null | Key | Notes |
| --- | --- | --- | --- | --- |
| `streamId` | `INTEGER` | `NOT NULL` | PK | Provider stream id |
| `categoryRemoteId` | `TEXT` | `NOT NULL` | Indexed | Live category id |
| `name` | `TEXT` | `NOT NULL` |  | Display name |
| `channelNumber` | `INTEGER` | `NULL` |  | Optional provider number |
| `logoUrl` | `TEXT` | `NULL` |  | Channel logo URL |
| `epgChannelId` | `TEXT` | `NULL` | Indexed | EPG channel id |
| `containerExtension` | `TEXT` | `NULL` |  | Stream extension |
| `directSource` | `TEXT` | `NULL` |  | Direct stream URL |
| `customSid` | `TEXT` | `NULL` |  | Provider custom SID |
| `isAdult` | `INTEGER` | `NOT NULL` |  | Derived boolean |
| `hasCatchup` | `INTEGER` | `NOT NULL` |  | Catchup availability |
| `catchupDurationHours` | `INTEGER` | `NOT NULL` |  | Catchup window |
| `addedAt` | `INTEGER` | `NULL` |  | Raw provider value |

Indexes:

- `index_channels_categoryRemoteId` on (`categoryRemoteId`)
- `index_channels_epgChannelId` on (`epgChannelId`)

```sql
CREATE TABLE `channels` (
  `streamId` INTEGER NOT NULL,
  `categoryRemoteId` TEXT NOT NULL,
  `name` TEXT NOT NULL,
  `channelNumber` INTEGER,
  `logoUrl` TEXT,
  `epgChannelId` TEXT,
  `containerExtension` TEXT,
  `directSource` TEXT,
  `customSid` TEXT,
  `isAdult` INTEGER NOT NULL,
  `hasCatchup` INTEGER NOT NULL,
  `catchupDurationHours` INTEGER NOT NULL,
  `addedAt` INTEGER,
  PRIMARY KEY(`streamId`)
);
```

### `movies`

Stores VOD items from `get_vod_streams`.

Source fields:

| Source field | Local column | Notes |
| --- | --- | --- |
| `stream_id` | `streamId` | Required; rows with missing or non-positive ids are skipped |
| `category_id` | `categoryRemoteId` | Logical reference to `categories.remoteId` where `section = 'movies'` |
| `name` | `name` | Falls back to `Movie <streamId>` when blank |
| `stream_icon` | `artworkUrl` | Blank values stored as null |
| `plot` | `plot` | Nullable |
| `rating` | `rating` | Nullable |
| `year` or `releaseDate` | `releaseYear` | Nullable text |
| `container_extension` | `containerExtension` | Used for stream URL fallback, default `mp4` |
| `direct_source` | `directSource` | Overrides generated stream URL when present |
| `added` | `addedAt` | Raw provider timestamp/value |
| Derived | `isAdult` | Category adult flag or name adult keyword match |

Schema:

| Column | Type | Null | Key | Notes |
| --- | --- | --- | --- | --- |
| `streamId` | `INTEGER` | `NOT NULL` | PK | Provider VOD stream id |
| `categoryRemoteId` | `TEXT` | `NOT NULL` | Indexed | Movie category id |
| `name` | `TEXT` | `NOT NULL` |  | Display name |
| `artworkUrl` | `TEXT` | `NULL` |  | Poster/art URL |
| `plot` | `TEXT` | `NULL` |  | Description |
| `rating` | `TEXT` | `NULL` |  | Provider rating text |
| `releaseYear` | `TEXT` | `NULL` |  | Year or release date text |
| `containerExtension` | `TEXT` | `NULL` |  | Stream extension |
| `directSource` | `TEXT` | `NULL` |  | Direct stream URL |
| `isAdult` | `INTEGER` | `NOT NULL` |  | Derived boolean |
| `addedAt` | `INTEGER` | `NULL` |  | Raw provider value |

Index:

- `index_movies_categoryRemoteId` on (`categoryRemoteId`)

```sql
CREATE TABLE `movies` (
  `streamId` INTEGER NOT NULL,
  `categoryRemoteId` TEXT NOT NULL,
  `name` TEXT NOT NULL,
  `artworkUrl` TEXT,
  `plot` TEXT,
  `rating` TEXT,
  `releaseYear` TEXT,
  `containerExtension` TEXT,
  `directSource` TEXT,
  `isAdult` INTEGER NOT NULL,
  `addedAt` INTEGER,
  PRIMARY KEY(`streamId`)
);
```

### `series`

Stores series rows from `get_series`. Series detail calls can later update the detail columns.

Source fields:

| Source field | Local column | Notes |
| --- | --- | --- |
| `series_id` | `seriesId` | Required; rows with missing or non-positive ids are skipped |
| `category_id` | `categoryRemoteId` | Logical reference to `categories.remoteId` where `section = 'series'` |
| `name` | `name` | Falls back to `Series <seriesId>` when blank |
| `cover` or `cover_big` | `artworkUrl` | Blank values stored as null |
| `plot` | `plot` | Nullable |
| `rating` | `rating` | Nullable |
| `year` or `releaseDate` | `releaseYear` | Nullable text |
| `last_modified` or `added` | `addedAt` | Raw provider timestamp/value |
| Derived | `isAdult` | Category adult flag or name adult keyword match |

Series detail update fields from `get_series_info.info`:

| Source field | Local column |
| --- | --- |
| `cover`, `cover_big`, or `movie_image` | `artworkUrl` |
| `plot` | `plot` |
| `rating` | `rating` |
| `year` or `releaseDate` | `releaseYear` |

Schema:

| Column | Type | Null | Key | Notes |
| --- | --- | --- | --- | --- |
| `seriesId` | `INTEGER` | `NOT NULL` | PK | Provider series id |
| `categoryRemoteId` | `TEXT` | `NOT NULL` | Indexed | Series category id |
| `name` | `TEXT` | `NOT NULL` |  | Display name |
| `artworkUrl` | `TEXT` | `NULL` |  | Poster/art URL |
| `plot` | `TEXT` | `NULL` |  | Description |
| `rating` | `TEXT` | `NULL` |  | Provider rating text |
| `releaseYear` | `TEXT` | `NULL` |  | Year or release date text |
| `isAdult` | `INTEGER` | `NOT NULL` |  | Derived boolean |
| `addedAt` | `INTEGER` | `NULL` |  | Raw provider value |

Index:

- `index_series_categoryRemoteId` on (`categoryRemoteId`)

```sql
CREATE TABLE `series` (
  `seriesId` INTEGER NOT NULL,
  `categoryRemoteId` TEXT NOT NULL,
  `name` TEXT NOT NULL,
  `artworkUrl` TEXT,
  `plot` TEXT,
  `rating` TEXT,
  `releaseYear` TEXT,
  `isAdult` INTEGER NOT NULL,
  `addedAt` INTEGER,
  PRIMARY KEY(`seriesId`)
);
```

### `episodes`

Stores episode rows from `get_series_info`. Episodes are loaded lazily per series, not during the main full sync.

Source shape:

- Response object has an `episodes` object.
- Each key in `episodes` is a season number.
- Each season value is an array of episode objects.

Source fields:

| Source field | Local column | Notes |
| --- | --- | --- |
| Episode object `id` | `episodeId` | Required; rows with missing or non-positive ids are skipped |
| Requested `series_id` | `seriesId` | Logical reference to `series.seriesId` |
| Season object key | `seasonNumber` | Parsed from the season key; non-numeric keys become `0` |
| `episode_num` | `episodeNumber` | Coerced integer |
| `title` | `title` | Falls back to `Episode <episodeId>` when blank |
| `info.movie_image` or `info.cover_big` | `artworkUrl` | Nullable |
| `info.plot` | `plot` | Nullable |
| `info.duration_secs` | `durationSeconds` | Nullable seconds |
| `container_extension` | `containerExtension` | Used for stream URL fallback, default `mp4` |
| `direct_source` | `directSource` | Overrides generated stream URL when present |
| `added` | `addedAt` | Raw provider timestamp/value |

Schema:

| Column | Type | Null | Key | Notes |
| --- | --- | --- | --- | --- |
| `episodeId` | `INTEGER` | `NOT NULL` | PK | Provider episode id |
| `seriesId` | `INTEGER` | `NOT NULL` | Indexed | Parent series id |
| `seasonNumber` | `INTEGER` | `NOT NULL` | Indexed | Season order |
| `episodeNumber` | `INTEGER` | `NOT NULL` |  | Episode order |
| `title` | `TEXT` | `NOT NULL` |  | Display title |
| `artworkUrl` | `TEXT` | `NULL` |  | Episode art URL |
| `plot` | `TEXT` | `NULL` |  | Description |
| `durationSeconds` | `INTEGER` | `NULL` |  | Duration in seconds |
| `containerExtension` | `TEXT` | `NULL` |  | Stream extension |
| `directSource` | `TEXT` | `NULL` |  | Direct stream URL |
| `addedAt` | `INTEGER` | `NULL` |  | Raw provider value |

Indexes:

- `index_episodes_seriesId` on (`seriesId`)
- `index_episodes_seasonNumber` on (`seasonNumber`)

```sql
CREATE TABLE `episodes` (
  `episodeId` INTEGER NOT NULL,
  `seriesId` INTEGER NOT NULL,
  `seasonNumber` INTEGER NOT NULL,
  `episodeNumber` INTEGER NOT NULL,
  `title` TEXT NOT NULL,
  `artworkUrl` TEXT,
  `plot` TEXT,
  `durationSeconds` INTEGER,
  `containerExtension` TEXT,
  `directSource` TEXT,
  `addedAt` INTEGER,
  PRIMARY KEY(`episodeId`)
);
```

### `epg_events`

Stores guide rows from XMLTV full sync and short EPG fallback.

XMLTV source fields:

| XMLTV source | Local column | Notes |
| --- | --- | --- |
| `<programme channel="">` | `channelEpgId` | Logical match to `channels.epgChannelId` |
| `<programme start="">` | `startEpochMillis` | Parsed as `yyyyMMddHHmmss Z`, stored in milliseconds |
| `<programme stop="">` | `endEpochMillis` | Parsed as `yyyyMMddHHmmss Z`, stored in milliseconds |
| `<title>` text | `title` | Falls back to `Live Event` when blank |
| `<desc>` text | `description` | Nullable |

Short EPG source fields:

| Source field | Local column | Notes |
| --- | --- | --- |
| Channel's `epgChannelId` | `channelEpgId` | Passed from the channel row |
| `start` or `start_timestamp` | `startEpochMillis` | Numeric seconds/millis or parsed date string |
| `end` or `stop_timestamp` | `endEpochMillis` | Numeric seconds/millis or parsed date string |
| `title` | `title` | Base64-decoded when possible |
| `description` | `description` | Base64-decoded when possible |

Schema:

| Column | Type | Null | Key | Notes |
| --- | --- | --- | --- | --- |
| `channelEpgId` | `TEXT` | `NOT NULL` | PK 1 | EPG channel id |
| `startEpochMillis` | `INTEGER` | `NOT NULL` | PK 2 | Program start time |
| `endEpochMillis` | `INTEGER` | `NOT NULL` |  | Program end time |
| `title` | `TEXT` | `NOT NULL` |  | Program title |
| `description` | `TEXT` | `NULL` |  | Program description |

Primary key: (`channelEpgId`, `startEpochMillis`)

```sql
CREATE TABLE `epg_events` (
  `channelEpgId` TEXT NOT NULL,
  `startEpochMillis` INTEGER NOT NULL,
  `endEpochMillis` INTEGER NOT NULL,
  `title` TEXT NOT NULL,
  `description` TEXT,
  PRIMARY KEY(`channelEpgId`, `startEpochMillis`)
);
```

## App-Local Tables

### `favorite_items`

Stores user favorites. These rows are not fetched from the IPTV source.

| Column | Type | Null | Key | Notes |
| --- | --- | --- | --- | --- |
| `targetType` | `TEXT` | `NOT NULL` | PK 1 | `channel`, `movie`, `series`, or `episode` |
| `targetId` | `TEXT` | `NOT NULL` | PK 2 | Referenced item id as text |
| `title` | `TEXT` | `NOT NULL` |  | Snapshot display title |
| `subtitle` | `TEXT` | `NULL` |  | Snapshot subtitle |
| `artworkUrl` | `TEXT` | `NULL` |  | Snapshot art URL |
| `addedAt` | `INTEGER` | `NOT NULL` |  | App timestamp in epoch milliseconds |

```sql
CREATE TABLE `favorite_items` (
  `targetType` TEXT NOT NULL,
  `targetId` TEXT NOT NULL,
  `title` TEXT NOT NULL,
  `subtitle` TEXT,
  `artworkUrl` TEXT,
  `addedAt` INTEGER NOT NULL,
  PRIMARY KEY(`targetType`, `targetId`)
);
```

### `playback_history`

Stores continue-watching progress for non-live playback. Live channels are intentionally not written here.

| Column | Type | Null | Key | Notes |
| --- | --- | --- | --- | --- |
| `id` | `TEXT` | `NOT NULL` | PK | `<targetType>:<targetId>` |
| `targetType` | `TEXT` | `NOT NULL` |  | `movie` or `episode` in normal use |
| `targetId` | `TEXT` | `NOT NULL` |  | Referenced item id as text |
| `title` | `TEXT` | `NOT NULL` |  | Snapshot display title |
| `subtitle` | `TEXT` | `NULL` |  | Snapshot subtitle |
| `artworkUrl` | `TEXT` | `NULL` |  | Snapshot art URL |
| `positionMs` | `INTEGER` | `NOT NULL` |  | Playback position in milliseconds |
| `durationMs` | `INTEGER` | `NOT NULL` |  | Media duration in milliseconds |
| `lastPlayedAt` | `INTEGER` | `NOT NULL` |  | App timestamp in epoch milliseconds |

```sql
CREATE TABLE `playback_history` (
  `id` TEXT NOT NULL,
  `targetType` TEXT NOT NULL,
  `targetId` TEXT NOT NULL,
  `title` TEXT NOT NULL,
  `subtitle` TEXT,
  `artworkUrl` TEXT,
  `positionMs` INTEGER NOT NULL,
  `durationMs` INTEGER NOT NULL,
  `lastPlayedAt` INTEGER NOT NULL,
  PRIMARY KEY(`id`)
);
```

### `recent_channels`

Stores recently played live channels. These rows are not fetched from the IPTV source.

| Column | Type | Null | Key | Notes |
| --- | --- | --- | --- | --- |
| `channelId` | `INTEGER` | `NOT NULL` | PK | Referenced `channels.streamId` |
| `title` | `TEXT` | `NOT NULL` |  | Snapshot channel title |
| `artworkUrl` | `TEXT` | `NULL` |  | Snapshot logo/art URL |
| `categoryId` | `TEXT` | `NULL` |  | Referenced live category id |
| `lastPlayedAt` | `INTEGER` | `NOT NULL` |  | App timestamp in epoch milliseconds |

```sql
CREATE TABLE `recent_channels` (
  `channelId` INTEGER NOT NULL,
  `title` TEXT NOT NULL,
  `artworkUrl` TEXT,
  `categoryId` TEXT,
  `lastPlayedAt` INTEGER NOT NULL,
  PRIMARY KEY(`channelId`)
);
```

## System Tables

The database also contains Room/Android support tables that are not application data:

| Table | Purpose |
| --- | --- |
| `android_metadata` | Android SQLite locale metadata |
| `room_master_table` | Room identity hash metadata |

## Logical Relationships

These are enforced by app queries, not by SQLite foreign keys:

| From | To | Notes |
| --- | --- | --- |
| `channels.categoryRemoteId` | `categories.remoteId` | Only meaningful when `categories.section = 'live'` |
| `movies.categoryRemoteId` | `categories.remoteId` | Only meaningful when `categories.section = 'movies'` |
| `series.categoryRemoteId` | `categories.remoteId` | Only meaningful when `categories.section = 'series'` |
| `episodes.seriesId` | `series.seriesId` | Episodes are cleared and refreshed per series |
| `epg_events.channelEpgId` | `channels.epgChannelId` | Used to show guide rows for live channels |
| `favorite_items.targetId` | IPTV item primary keys | Interpreted by `targetType` |
| `playback_history.targetId` | Movie or episode primary keys | Interpreted by `targetType` |
| `recent_channels.channelId` | `channels.streamId` | Recent live channel history |

## Sync Behavior

Full sync behavior:

1. Fetch live, movie, and series categories.
2. Replace categories for each section while preserving hidden category ids.
3. Clear and repopulate `channels`.
4. Clear and repopulate `movies`.
5. Clear and repopulate `series`.
6. Clear and repopulate `epg_events` from XMLTV when that fetch succeeds.
7. Store `last_sync_epoch_millis` in DataStore.

Lazy fetch behavior:

- `episodes` is populated only when `ensureSeriesLoaded(seriesId)` runs for a series.
- `epg_events` can also be populated by short EPG when a channel guide has no future events.

Provider stream URL behavior:

- Live playback defaults to `<baseUrl>/live/<username>/<password>/<streamId>.<extension>`.
- Movie playback defaults to `<baseUrl>/movie/<username>/<password>/<streamId>.<extension>`.
- Episode playback defaults to `<baseUrl>/series/<username>/<password>/<episodeId>.<extension>`.
- `directSource` overrides generated playback URLs when it is non-blank.
