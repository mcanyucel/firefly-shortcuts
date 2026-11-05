package com.mustafacanyucel.firefly_shortcuts

import android.app.Application
import android.content.Context
import com.mustafacanyucel.firefly_shortcuts.upgrade.LegacyDataCleanup
import com.mustafacanyucel.firefly_shortcuts.widget.ShortcutWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FireflyApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        container.appScope.launch(Dispatchers.IO) { LegacyDataCleanup.run(this@FireflyApp) }
        ShortcutWidget.keepUpdated(this)
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as FireflyApp).container
