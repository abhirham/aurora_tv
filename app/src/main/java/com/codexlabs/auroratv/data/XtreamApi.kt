package com.codexlabs.auroratv.data

import android.util.Base64
import android.util.JsonReader
import android.util.JsonToken
import java.io.IOException
import java.io.InputStream
import java.io.Reader
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

private val XmlTvDateFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss Z", Locale.US)
private val FlexibleTimeFormatters = listOf(
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.US),
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss", Locale.US),
)
private val XmlTvParserFactory: XmlPullParserFactory by lazy {
    XmlPullParserFactory.newInstance()
}

class XtreamApi(
    private val client: OkHttpClient,
) {
    data class CategoryPayload(
        val remoteId: String,
        val section: LibrarySection,
        val name: String,
        val isAdult: Boolean,
        val sortOrder: Int,
    )

    data class ChannelPayload(
        val streamId: Long,
        val categoryRemoteId: String,
        val name: String,
        val channelNumber: Int?,
        val logoUrl: String?,
        val epgChannelId: String?,
        val containerExtension: String?,
        val directSource: String?,
        val customSid: String?,
        val isAdult: Boolean,
        val hasCatchup: Boolean,
        val catchupDurationHours: Int,
        val addedAt: Long?,
    )

    data class MoviePayload(
        val streamId: Long,
        val categoryRemoteId: String,
        val name: String,
        val artworkUrl: String?,
        val plot: String?,
        val rating: String?,
        val releaseYear: String?,
        val containerExtension: String?,
        val directSource: String?,
        val isAdult: Boolean,
        val addedAt: Long?,
    )

    data class SeriesPayload(
        val seriesId: Long,
        val categoryRemoteId: String,
        val name: String,
        val artworkUrl: String?,
        val plot: String?,
        val rating: String?,
        val releaseYear: String?,
        val isAdult: Boolean,
        val addedAt: Long?,
    )

    data class EpisodePayload(
        val episodeId: Long,
        val seriesId: Long,
        val seasonNumber: Int,
        val episodeNumber: Int,
        val title: String,
        val artworkUrl: String?,
        val plot: String?,
        val durationSeconds: Long?,
        val containerExtension: String?,
        val directSource: String?,
        val addedAt: Long?,
    )

    data class SeriesDetailPayload(
        val seriesId: Long,
        val artworkUrl: String?,
        val plot: String?,
        val rating: String?,
        val releaseYear: String?,
        val episodes: List<EpisodePayload>,
    )

    data class EpgProgramPayload(
        val channelEpgId: String,
        val startEpochMillis: Long,
        val endEpochMillis: Long,
        val title: String,
        val description: String?,
    )

    suspend fun fetchCategories(
        credentials: ProviderCredentials,
        section: LibrarySection,
    ): List<CategoryPayload> = withContext(Dispatchers.IO) {
        val action = when (section) {
            LibrarySection.LIVE -> "get_live_categories"
            LibrarySection.MOVIES -> "get_vod_categories"
            LibrarySection.SERIES -> "get_series_categories"
            else -> error("Unsupported section $section")
        }
        execute(playerApiUrl(credentials, action)).use { response ->
            response.body?.charStream()?.use { parseCategories(it, section) } ?: emptyList()
        }
    }

    suspend fun streamChannels(
        credentials: ProviderCredentials,
        categoryAdultMap: Map<String, Boolean>,
        onChunk: suspend (List<ChannelPayload>) -> Unit,
    ) {
        streamArray(
            url = playerApiUrl(credentials, "get_live_streams"),
            parser = { parseChannel(categoryAdultMap) },
            onChunk = onChunk,
        )
    }

    suspend fun streamMovies(
        credentials: ProviderCredentials,
        categoryAdultMap: Map<String, Boolean>,
        onChunk: suspend (List<MoviePayload>) -> Unit,
    ) {
        streamArray(
            url = playerApiUrl(credentials, "get_vod_streams"),
            parser = { parseMovie(categoryAdultMap) },
            onChunk = onChunk,
        )
    }

    suspend fun streamSeries(
        credentials: ProviderCredentials,
        categoryAdultMap: Map<String, Boolean>,
        onChunk: suspend (List<SeriesPayload>) -> Unit,
    ) {
        streamArray(
            url = playerApiUrl(credentials, "get_series"),
            parser = { parseSeries(categoryAdultMap) },
            onChunk = onChunk,
        )
    }

    suspend fun fetchSeriesInfo(
        credentials: ProviderCredentials,
        seriesId: Long,
    ): SeriesDetailPayload = withContext(Dispatchers.IO) {
        execute(
            playerApiUrl(
                credentials = credentials,
                action = "get_series_info",
                extra = mapOf("series_id" to seriesId.toString()),
            ),
        ).use { response ->
            response.body?.charStream()?.use { reader ->
                parseSeriesInfo(reader, seriesId)
            } ?: SeriesDetailPayload(seriesId, null, null, null, null, emptyList())
        }
    }

    suspend fun streamXmlTv(
        credentials: ProviderCredentials,
        windowHours: Int,
        onChunk: suspend (List<EpgProgramPayload>) -> Unit,
    ) = withContext(Dispatchers.IO) {
        execute(xmlTvUrl(credentials)).use { response ->
            response.body?.byteStream()?.use { input ->
                parseXmlTv(input, windowHours, onChunk)
            }
        }
    }

    suspend fun fetchShortEpg(
        credentials: ProviderCredentials,
        streamId: Long,
        channelEpgId: String,
    ): List<EpgProgramPayload> = withContext(Dispatchers.IO) {
        execute(
            playerApiUrl(
                credentials = credentials,
                action = "get_short_epg",
                extra = mapOf(
                    "stream_id" to streamId.toString(),
                    "limit" to "24",
                ),
            ),
        ).use { response ->
            response.body?.charStream()?.use { reader ->
                parseShortEpg(reader, channelEpgId)
            } ?: emptyList()
        }
    }

    fun liveStreamUrl(credentials: ProviderCredentials, channel: ChannelEntity): String {
        return buildStreamUrl(
            credentials = credentials,
            type = "live",
            streamId = channel.streamId,
            extension = channel.containerExtension ?: "ts",
            directSource = channel.directSource,
        )
    }

    fun movieStreamUrl(credentials: ProviderCredentials, movie: MovieEntity): String {
        return buildStreamUrl(
            credentials = credentials,
            type = "movie",
            streamId = movie.streamId,
            extension = movie.containerExtension ?: "mp4",
            directSource = movie.directSource,
        )
    }

    fun episodeStreamUrl(credentials: ProviderCredentials, episode: EpisodeEntity): String {
        return buildStreamUrl(
            credentials = credentials,
            type = "series",
            streamId = episode.episodeId,
            extension = episode.containerExtension ?: "mp4",
            directSource = episode.directSource,
        )
    }

    private suspend fun <T> streamArray(
        url: HttpUrl,
        parser: JsonReader.() -> T?,
        onChunk: suspend (List<T>) -> Unit,
        chunkSize: Int = 400,
    ) = withContext(Dispatchers.IO) {
        execute(url).use { response ->
            response.body?.charStream()?.use { input ->
                val reader = JsonReader(input)
                val chunk = ArrayList<T>(chunkSize)
                if (reader.peek() == JsonToken.BEGIN_ARRAY) {
                    reader.beginArray()
                    while (reader.hasNext()) {
                        val parsed = parser(reader)
                        if (parsed != null) {
                            chunk += parsed
                            if (chunk.size >= chunkSize) {
                                onChunk(chunk.toList())
                                chunk.clear()
                            }
                        }
                    }
                    reader.endArray()
                } else {
                    reader.skipValue()
                }
                if (chunk.isNotEmpty()) {
                    onChunk(chunk.toList())
                }
                reader.close()
            }
        }
    }

    private fun parseCategories(
        reader: Reader,
        section: LibrarySection,
    ): List<CategoryPayload> {
        val jsonReader = JsonReader(reader)
        val items = mutableListOf<CategoryPayload>()
        if (jsonReader.peek() != JsonToken.BEGIN_ARRAY) {
            jsonReader.close()
            return emptyList()
        }

        jsonReader.beginArray()
        while (jsonReader.hasNext()) {
            var categoryId = ""
            var categoryName = ""
            jsonReader.beginObject()
            while (jsonReader.hasNext()) {
                when (jsonReader.nextName()) {
                    "category_id" -> categoryId = jsonReader.nextStringCoerced().orEmpty()
                    "category_name" -> categoryName = jsonReader.nextStringCoerced().orEmpty()
                    else -> jsonReader.skipValue()
                }
            }
            jsonReader.endObject()
            if (categoryId.isNotBlank()) {
                items += CategoryPayload(
                    remoteId = categoryId,
                    section = section,
                    name = categoryName.ifBlank { "Category $categoryId" },
                    isAdult = looksAdult(categoryName),
                    sortOrder = items.size,
                )
            }
        }
        jsonReader.endArray()
        jsonReader.close()
        return items
    }

    private fun JsonReader.parseChannel(categoryAdultMap: Map<String, Boolean>): ChannelPayload? {
        var streamId = 0L
        var categoryId = ""
        var name = ""
        var number: Int? = null
        var logoUrl: String? = null
        var epgChannelId: String? = null
        var containerExtension: String? = null
        var directSource: String? = null
        var customSid: String? = null
        var hasCatchup = false
        var catchupDuration = 0
        var addedAt: Long? = null

        beginObject()
        while (hasNext()) {
            when (nextName()) {
                "stream_id" -> streamId = nextLongCoerced() ?: 0L
                "category_id" -> categoryId = nextStringCoerced().orEmpty()
                "name" -> name = nextStringCoerced().orEmpty()
                "num" -> number = nextIntCoerced()
                "stream_icon" -> logoUrl = nextStringCoerced()
                "epg_channel_id" -> epgChannelId = nextStringCoerced()
                "container_extension" -> containerExtension = nextStringCoerced()
                "direct_source" -> directSource = nextStringCoerced()
                "custom_sid" -> customSid = nextStringCoerced()
                "tv_archive" -> hasCatchup = nextBooleanCoerced() ?: false
                "tv_archive_duration" -> catchupDuration = nextIntCoerced() ?: 0
                "added" -> addedAt = nextLongCoerced()
                else -> skipValue()
            }
        }
        endObject()

        if (streamId <= 0L) return null
        return ChannelPayload(
            streamId = streamId,
            categoryRemoteId = categoryId,
            name = name.ifBlank { "Channel $streamId" },
            channelNumber = number,
            logoUrl = logoUrl?.takeIf { it.isNotBlank() },
            epgChannelId = epgChannelId?.takeIf { it.isNotBlank() },
            containerExtension = containerExtension?.takeIf { it.isNotBlank() },
            directSource = directSource?.takeIf { it.isNotBlank() },
            customSid = customSid?.takeIf { it.isNotBlank() },
            isAdult = categoryAdultMap[categoryId] == true || looksAdult(name),
            hasCatchup = hasCatchup,
            catchupDurationHours = catchupDuration,
            addedAt = addedAt,
        )
    }

    private fun JsonReader.parseMovie(categoryAdultMap: Map<String, Boolean>): MoviePayload? {
        var streamId = 0L
        var categoryId = ""
        var name = ""
        var artworkUrl: String? = null
        var plot: String? = null
        var rating: String? = null
        var releaseYear: String? = null
        var containerExtension: String? = null
        var directSource: String? = null
        var addedAt: Long? = null

        beginObject()
        while (hasNext()) {
            when (nextName()) {
                "stream_id" -> streamId = nextLongCoerced() ?: 0L
                "category_id" -> categoryId = nextStringCoerced().orEmpty()
                "name" -> name = nextStringCoerced().orEmpty()
                "stream_icon" -> artworkUrl = nextStringCoerced()
                "plot" -> plot = nextStringCoerced()
                "rating" -> rating = nextStringCoerced()
                "year", "releaseDate" -> releaseYear = nextStringCoerced()
                "container_extension" -> containerExtension = nextStringCoerced()
                "direct_source" -> directSource = nextStringCoerced()
                "added" -> addedAt = nextLongCoerced()
                else -> skipValue()
            }
        }
        endObject()

        if (streamId <= 0L) return null
        return MoviePayload(
            streamId = streamId,
            categoryRemoteId = categoryId,
            name = name.ifBlank { "Movie $streamId" },
            artworkUrl = artworkUrl?.takeIf { it.isNotBlank() },
            plot = plot,
            rating = rating,
            releaseYear = releaseYear,
            containerExtension = containerExtension?.takeIf { it.isNotBlank() },
            directSource = directSource?.takeIf { it.isNotBlank() },
            isAdult = categoryAdultMap[categoryId] == true || looksAdult(name),
            addedAt = addedAt,
        )
    }

    private fun JsonReader.parseSeries(categoryAdultMap: Map<String, Boolean>): SeriesPayload? {
        var seriesId = 0L
        var categoryId = ""
        var name = ""
        var artworkUrl: String? = null
        var plot: String? = null
        var rating: String? = null
        var releaseYear: String? = null
        var addedAt: Long? = null

        beginObject()
        while (hasNext()) {
            when (nextName()) {
                "series_id" -> seriesId = nextLongCoerced() ?: 0L
                "category_id" -> categoryId = nextStringCoerced().orEmpty()
                "name" -> name = nextStringCoerced().orEmpty()
                "cover", "cover_big" -> artworkUrl = nextStringCoerced()
                "plot" -> plot = nextStringCoerced()
                "rating" -> rating = nextStringCoerced()
                "year", "releaseDate" -> releaseYear = nextStringCoerced()
                "last_modified", "added" -> addedAt = nextLongCoerced()
                else -> skipValue()
            }
        }
        endObject()

        if (seriesId <= 0L) return null
        return SeriesPayload(
            seriesId = seriesId,
            categoryRemoteId = categoryId,
            name = name.ifBlank { "Series $seriesId" },
            artworkUrl = artworkUrl?.takeIf { it.isNotBlank() },
            plot = plot,
            rating = rating,
            releaseYear = releaseYear,
            isAdult = categoryAdultMap[categoryId] == true || looksAdult(name),
            addedAt = addedAt,
        )
    }

    private fun parseSeriesInfo(reader: Reader, seriesId: Long): SeriesDetailPayload {
        val jsonReader = JsonReader(reader)
        var artworkUrl: String? = null
        var plot: String? = null
        var rating: String? = null
        var releaseYear: String? = null
        val episodes = mutableListOf<EpisodePayload>()

        if (jsonReader.peek() != JsonToken.BEGIN_OBJECT) {
            jsonReader.close()
            return SeriesDetailPayload(seriesId, null, null, null, null, emptyList())
        }

        jsonReader.beginObject()
        while (jsonReader.hasNext()) {
            when (jsonReader.nextName()) {
                "info" -> {
                    jsonReader.beginObject()
                    while (jsonReader.hasNext()) {
                        when (jsonReader.nextName()) {
                            "cover", "cover_big", "movie_image" -> artworkUrl = jsonReader.nextStringCoerced()
                            "plot" -> plot = jsonReader.nextStringCoerced()
                            "rating" -> rating = jsonReader.nextStringCoerced()
                            "year", "releaseDate" -> releaseYear = jsonReader.nextStringCoerced()
                            else -> jsonReader.skipValue()
                        }
                    }
                    jsonReader.endObject()
                }
                "episodes" -> parseEpisodesObject(jsonReader, seriesId, episodes)
                else -> jsonReader.skipValue()
            }
        }
        jsonReader.endObject()
        jsonReader.close()
        return SeriesDetailPayload(
            seriesId = seriesId,
            artworkUrl = artworkUrl,
            plot = plot,
            rating = rating,
            releaseYear = releaseYear,
            episodes = episodes.sortedWith(compareBy(EpisodePayload::seasonNumber, EpisodePayload::episodeNumber)),
        )
    }

    private fun parseEpisodesObject(
        reader: JsonReader,
        seriesId: Long,
        output: MutableList<EpisodePayload>,
    ) {
        if (reader.peek() != JsonToken.BEGIN_OBJECT) {
            reader.skipValue()
            return
        }

        reader.beginObject()
        while (reader.hasNext()) {
            val seasonKey = reader.nextName()
            val seasonNumber = seasonKey.toIntOrNull() ?: 0
            if (reader.peek() != JsonToken.BEGIN_ARRAY) {
                reader.skipValue()
                continue
            }
            reader.beginArray()
            while (reader.hasNext()) {
                var episodeId = 0L
                var episodeNumber = 0
                var title = ""
                var artworkUrl: String? = null
                var plot: String? = null
                var durationSeconds: Long? = null
                var containerExtension: String? = null
                var directSource: String? = null
                var addedAt: Long? = null

                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "id" -> episodeId = reader.nextLongCoerced() ?: 0L
                        "episode_num" -> episodeNumber = reader.nextIntCoerced() ?: 0
                        "title" -> title = reader.nextStringCoerced().orEmpty()
                        "container_extension" -> containerExtension = reader.nextStringCoerced()
                        "direct_source" -> directSource = reader.nextStringCoerced()
                        "added" -> addedAt = reader.nextLongCoerced()
                        "info" -> {
                            if (reader.peek() == JsonToken.BEGIN_OBJECT) {
                                reader.beginObject()
                                while (reader.hasNext()) {
                                    when (reader.nextName()) {
                                        "movie_image", "cover_big" -> artworkUrl = reader.nextStringCoerced()
                                        "plot" -> plot = reader.nextStringCoerced()
                                        "duration_secs" -> durationSeconds = reader.nextLongCoerced()
                                        else -> reader.skipValue()
                                    }
                                }
                                reader.endObject()
                            } else {
                                reader.skipValue()
                            }
                        }
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()

                if (episodeId > 0L) {
                    output += EpisodePayload(
                        episodeId = episodeId,
                        seriesId = seriesId,
                        seasonNumber = seasonNumber,
                        episodeNumber = episodeNumber,
                        title = title.ifBlank { "Episode $episodeId" },
                        artworkUrl = artworkUrl,
                        plot = plot,
                        durationSeconds = durationSeconds,
                        containerExtension = containerExtension,
                        directSource = directSource,
                        addedAt = addedAt,
                    )
                }
            }
            reader.endArray()
        }
        reader.endObject()
    }

    private suspend fun parseXmlTv(
        inputStream: InputStream,
        windowHours: Int,
        onChunk: suspend (List<EpgProgramPayload>) -> Unit,
    ) {
        val parser = XmlTvParserFactory.newPullParser().apply {
            setInput(inputStream, null)
        }

        val now = System.currentTimeMillis()
        val lowerBound = now
        val upperBound = now + windowHours.coerceIn(12, 96) * 60L * 60L * 1000L

        var eventType = parser.eventType
        val chunk = ArrayList<EpgProgramPayload>(500)
        var currentChannel = ""
        var currentStart = 0L
        var currentEnd = 0L
        var currentTitle = ""
        var currentDescription: String? = null
        var currentTag: String? = null

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name
                    if (parser.name == "programme") {
                        currentChannel = parser.getAttributeValue(null, "channel").orEmpty()
                        currentStart = parseXmlTvDate(parser.getAttributeValue(null, "start")) ?: 0L
                        currentEnd = parseXmlTvDate(parser.getAttributeValue(null, "stop")) ?: 0L
                        currentTitle = ""
                        currentDescription = null
                    }
                }
                XmlPullParser.TEXT -> {
                    when (currentTag) {
                        "title" -> currentTitle = parser.text.orEmpty().trim()
                        "desc" -> currentDescription = parser.text?.trim()
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "programme") {
                        if (
                            currentChannel.isNotBlank() &&
                            currentStart in lowerBound..upperBound &&
                            currentEnd > currentStart
                        ) {
                            chunk += EpgProgramPayload(
                                channelEpgId = currentChannel,
                                startEpochMillis = currentStart,
                                endEpochMillis = currentEnd,
                                title = currentTitle.ifBlank { "Live Event" },
                                description = currentDescription,
                            )
                            if (chunk.size >= 500) {
                                onChunk(chunk.toList())
                                chunk.clear()
                            }
                        }
                    }
                    currentTag = null
                }
            }
            eventType = parser.next()
        }

        if (chunk.isNotEmpty()) {
            onChunk(chunk.toList())
        }
    }

    private fun parseShortEpg(reader: Reader, channelEpgId: String): List<EpgProgramPayload> {
        val jsonReader = JsonReader(reader)
        val items = mutableListOf<EpgProgramPayload>()
        if (jsonReader.peek() != JsonToken.BEGIN_OBJECT) {
            jsonReader.close()
            return emptyList()
        }

        jsonReader.beginObject()
        while (jsonReader.hasNext()) {
            when (jsonReader.nextName()) {
                "epg_listings" -> {
                    if (jsonReader.peek() == JsonToken.BEGIN_ARRAY) {
                        jsonReader.beginArray()
                        while (jsonReader.hasNext()) {
                            var title: String? = null
                            var description: String? = null
                            var start: Long? = null
                            var end: Long? = null
                            jsonReader.beginObject()
                            while (jsonReader.hasNext()) {
                                when (jsonReader.nextName()) {
                                    "title" -> title = decodeBase64IfPossible(jsonReader.nextStringCoerced())
                                    "description" -> description = decodeBase64IfPossible(jsonReader.nextStringCoerced())
                                    "start", "start_timestamp" -> start = parseFlexibleTime(jsonReader.nextStringCoerced())
                                    "end", "stop_timestamp" -> end = parseFlexibleTime(jsonReader.nextStringCoerced())
                                    else -> jsonReader.skipValue()
                                }
                            }
                            jsonReader.endObject()
                            if (start != null && end != null && end > start) {
                                items += EpgProgramPayload(
                                    channelEpgId = channelEpgId,
                                    startEpochMillis = start,
                                    endEpochMillis = end,
                                    title = title.orEmpty().ifBlank { "Live Event" },
                                    description = description,
                                )
                            }
                        }
                        jsonReader.endArray()
                    } else {
                        jsonReader.skipValue()
                    }
                }
                else -> jsonReader.skipValue()
            }
        }
        jsonReader.endObject()
        jsonReader.close()
        return items
    }

    private fun playerApiUrl(
        credentials: ProviderCredentials,
        action: String,
        extra: Map<String, String> = emptyMap(),
    ): HttpUrl {
        val base = credentials.baseUrl.toHttpUrlOrNull()
            ?: throw IOException("Invalid provider URL: ${credentials.baseUrl}")

        return base.newBuilder()
            .addPathSegment("player_api.php")
            .addQueryParameter("username", credentials.username)
            .addQueryParameter("password", credentials.password)
            .addQueryParameter("action", action)
            .apply {
                extra.forEach { (key, value) -> addQueryParameter(key, value) }
            }
            .build()
    }

    private fun xmlTvUrl(credentials: ProviderCredentials): HttpUrl {
        val base = credentials.baseUrl.toHttpUrlOrNull()
            ?: throw IOException("Invalid provider URL: ${credentials.baseUrl}")
        return base.newBuilder()
            .addPathSegment("xmltv.php")
            .addQueryParameter("username", credentials.username)
            .addQueryParameter("password", credentials.password)
            .build()
    }

    private fun buildStreamUrl(
        credentials: ProviderCredentials,
        type: String,
        streamId: Long,
        extension: String,
        directSource: String?,
    ): String {
        directSource?.takeIf { it.isNotBlank() }?.let { return it }
        return buildString {
            append(credentials.baseUrl)
            append('/')
            append(type)
            append('/')
            append(credentials.username)
            append('/')
            append(credentials.password)
            append('/')
            append(streamId)
            append('.')
            append(extension.ifBlank { "mp4" })
        }
    }

    private fun execute(url: HttpUrl): Response {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "AuroraTV/0.1")
            .build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IOException("Xtream request failed with ${response.code}")
        }
        return response
    }

    private fun JsonReader.nextStringCoerced(): String? {
        return when (peek()) {
            JsonToken.NULL -> {
                nextNull()
                null
            }
            JsonToken.STRING -> nextString()
            JsonToken.NUMBER -> nextString()
            JsonToken.BOOLEAN -> nextBoolean().toString()
            else -> {
                skipValue()
                null
            }
        }
    }

    private fun JsonReader.nextLongCoerced(): Long? {
        val value = nextStringCoerced() ?: return null
        return value.toLongOrNull() ?: value.toDoubleOrNull()?.toLong()
    }

    private fun JsonReader.nextIntCoerced(): Int? {
        return nextStringCoerced()?.toDoubleOrNull()?.toInt()
    }

    private fun JsonReader.nextBooleanCoerced(): Boolean? {
        return when (peek()) {
            JsonToken.NULL -> {
                nextNull()
                null
            }
            JsonToken.BOOLEAN -> nextBoolean()
            JsonToken.NUMBER,
            JsonToken.STRING,
            -> {
                nextStringCoerced()?.lowercase(Locale.US)?.let { value ->
                    value == "1" || value == "true" || value == "yes"
                }
            }
            else -> {
                skipValue()
                null
            }
        }
    }

    private fun parseXmlTvDate(value: String?): Long? {
        val trimmed = value?.trim().orEmpty()
        if (trimmed.isBlank()) return null
        val normalized = trimmed.split(' ')
        val datePart = normalized.firstOrNull()?.take(14) ?: return null
        val offsetPart = normalized.getOrNull(1) ?: "+0000"
        return try {
            ZonedDateTime.parse(
                "$datePart $offsetPart",
                XmlTvDateFormatter,
            ).toInstant().toEpochMilli()
        } catch (_: DateTimeParseException) {
            null
        }
    }

    private fun parseFlexibleTime(value: String?): Long? {
        val raw = value?.trim().orEmpty()
        if (raw.isBlank()) return null

        raw.toLongOrNull()?.let { numeric ->
            return if (raw.length <= 10) numeric * 1000 else numeric
        }

        FlexibleTimeFormatters.forEach { formatter ->
            try {
                return LocalDateTime.parse(raw, formatter)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            } catch (_: DateTimeParseException) {
            }
        }

        return try {
            Instant.parse(raw).toEpochMilli()
        } catch (_: DateTimeParseException) {
            try {
                OffsetDateTime.parse(raw).toInstant().toEpochMilli()
            } catch (_: DateTimeParseException) {
                null
            }
        }
    }

    private fun decodeBase64IfPossible(value: String?): String? {
        val source = value?.trim().orEmpty()
        if (source.isBlank()) return null
        return runCatching {
            val decoded = Base64.decode(source, Base64.DEFAULT)
            String(decoded).trim().ifBlank { source }
        }.getOrElse { source }
    }

    private fun looksAdult(value: String?): Boolean {
        val normalized = value.orEmpty().lowercase(Locale.US)
        if (normalized.isBlank()) return false
        val keywords = listOf("adult", "xxx", "18+", "porn", "sex", "hustler")
        return keywords.any { normalized.contains(it) }
    }
}
