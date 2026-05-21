package com.mustafacanyucel.firefly_shortcuts

  import android.content.Intent
  import android.widget.RemoteViewsService

  class ShortcutListWidgetService : RemoteViewsService() {
      override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
          ShortcutListRemoteViewsFactory(applicationContext)
  }