package com.codexlabs.auroratv.app

import android.app.Application
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

class AppContainer(
    application: Application,
) {
    val settingsRepository: SettingsRepository = SettingsRepository(application)

    private val httpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        OkHttpClient.Builder()
            .cache(Cache(File(application.cacheDir, "aurora_http"), 128L * 1024L * 1024L))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(120, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(loggingInterceptor)
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
