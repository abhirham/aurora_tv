# Room generated database and DAO implementations.
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Database class * { *; }
-keep class com.codexlabs.auroratv.data.AppDatabase_Impl { *; }
-keep class com.codexlabs.auroratv.data.AppDatabase_* { *; }

# Coil 3 service-loader and network fetcher integration.
-keep class coil3.network.okhttp.internal.OkHttpNetworkFetcherServiceLoaderTarget { *; }

# Media3 renderer/session classes that can be reached reflectively by ExoPlayer.
-keep class androidx.media3.exoplayer.DefaultRenderersFactory { *; }
-keep class androidx.media3.exoplayer.Renderer { *; }
-keep class androidx.media3.exoplayer.audio.** { *; }
-keep class androidx.media3.exoplayer.video.** { *; }
-keep class androidx.media3.exoplayer.text.** { *; }
-keep class androidx.media3.session.MediaSession { *; }
