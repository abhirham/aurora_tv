package com.codexlabs.auroratv.data

enum class LibrarySection(val rawValue: String) {
    LIVE("live"),
    MOVIES("movies"),
    SERIES("series"),
    HOME("home"),
    SEARCH("search"),
    SETTINGS("settings"),
}

enum class TargetType(val rawValue: String) {
    CHANNEL("channel"),
    MOVIE("movie"),
    SERIES("series"),
    EPISODE("episode");

    companion object {
        fun from(rawValue: String): TargetType {
            return entries.firstOrNull { it.rawValue == rawValue } ?: CHANNEL
        }
    }
}

enum class PreferredPlayer {
    INTERNAL,
    EXTERNAL;

    companion object {
        fun from(rawValue: String?): PreferredPlayer {
            return entries.firstOrNull { it.name == rawValue } ?: INTERNAL
        }
    }
}

enum class BufferProfile {
    LOW_LATENCY,
    BALANCED,
    STABLE;

    companion object {
        fun from(rawValue: String?): BufferProfile {
            return entries.firstOrNull { it.name == rawValue } ?: BALANCED
        }
    }
}

data class ProviderCredentials(
    val baseUrl: String,
    val username: String,
    val password: String,
)

data class AppSettings(
    val providerBaseUrl: String = "",
    val providerUsername: String = "",
    val providerPassword: String = "",
    val autoSyncEnabled: Boolean = true,
    val adultContentEnabled: Boolean = false,
    val preferredPlayer: PreferredPlayer = PreferredPlayer.INTERNAL,
    val bufferProfile: BufferProfile = BufferProfile.BALANCED,
    val epgWindowHours: Int = 48,
    val parentalPin: String = "",
    val lastSyncEpochMillis: Long? = null,
) {
    val isConfigured: Boolean
        get() = providerBaseUrl.isNotBlank() &&
            providerUsername.isNotBlank() &&
            providerPassword.isNotBlank()

    fun credentialsOrNull(): ProviderCredentials? {
        if (!isConfigured) return null
        return ProviderCredentials(
            baseUrl = providerBaseUrl,
            username = providerUsername,
            password = providerPassword,
        )
    }
}

data class PlaybackDescriptor(
    val targetType: TargetType,
    val targetId: String,
    val title: String,
    val subtitle: String?,
    val artworkUrl: String?,
    val mediaUrl: String,
    val isLive: Boolean,
    val categoryId: String? = null,
    val epgChannelId: String? = null,
)

data class LibraryStats(
    val liveChannels: Int = 0,
    val movies: Int = 0,
    val series: Int = 0,
    val episodes: Int = 0,
    val favorites: Int = 0,
)

data class SearchResultItem(
    val targetType: TargetType,
    val targetId: String,
    val title: String,
    val subtitle: String?,
    val artworkUrl: String?,
)

data class SearchResults(
    val channels: List<SearchResultItem> = emptyList(),
    val movies: List<SearchResultItem> = emptyList(),
    val series: List<SearchResultItem> = emptyList(),
)
