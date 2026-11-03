package com.mustafacanyucel.firefly_shortcuts

import android.app.Application
import android.content.Context

class FireflyApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

val Context.appContext: AppContainer
    get() = (applicationContext as FireflyApp).container
