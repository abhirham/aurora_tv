package com.codexlabs.auroratv.app

import android.app.Application
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.codexlabs.auroratv.BuildConfig
import com.codexlabs.auroratv.data.AppDatabase
import com.codexlabs.auroratv.data.IptvRepository
import com.codexlabs.auroratv.data.XtreamApi
import com.codexlabs.auroratv.settings.SettingsRepository
import java.io.File
import java.util.concurrent.TimeUnit
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okio.Path.Companion.toOkioPath

class AppContainer(
    application: Application,
) {
    val settingsRepository: SettingsRepository = SettingsRepository(application)

    private val httpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .cache(Cache(File(application.cacheDir, "aurora_http"), 128L * 1024L * 1024L))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(120, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                },
            )
        }

        builder.build()
    }

    val imageLoader: ImageLoader by lazy {
        ImageLoader.Builder(application)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(application, 0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(application.cacheDir, "aurora_images").toOkioPath())
                    .maxSizeBytes(256L * 1024L * 1024L)
                    .build()
            }
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = httpClient))
            }
            .build()
    }

    private val database: AppDatabase by lazy {
        AppDatabase.create(application)
    }

    private val xtreamApi: XtreamApi by lazy {
        XtreamApi(httpClient)
    }

    val repository: IptvRepository by lazy {
        IptvRepository(
            database = database,
            settingsRepository = settingsRepository,
            xtreamApi = xtreamApi,
        )
    }
}
