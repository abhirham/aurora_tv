package com.codexlabs.auroratv.app

import android.app.Application

class AuroraTvApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
