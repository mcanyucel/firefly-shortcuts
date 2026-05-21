package com.mustafacanyucel.firefly_shortcuts

import android.content.Context
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            "firefly_shortcuts/widget"
        ).setMethodCallHandler { call, result ->
            if (call.method == "updateWidgetData") {
                val prefs = getSharedPreferences(
                    ShortcutWidgetProvider.PREFS_NAME, Context.MODE_PRIVATE
                )
                prefs.edit()
                    .putString("shortcuts", call.argument("shortcuts") ?: "[]")
                    .putString("server_url", call.argument("server_url") ?: "")
                    .putString("access_token", call.argument("access_token") ?: "")
                    .apply()
                ShortcutWidgetProvider.updateAllWidgets(this)
                result.success(null)
            } else {
                result.notImplemented()
            }
        }
    }
}
