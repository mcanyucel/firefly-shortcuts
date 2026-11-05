package com.mustafacanyucel.firefly_shortcuts

import android.app.Application
import android.content.Context
import com.mustafacanyucel.firefly_shortcuts.widget.ShortcutWidget

class FireflyApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        ShortcutWidget.keepUpdated(this)
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as FireflyApp).container
