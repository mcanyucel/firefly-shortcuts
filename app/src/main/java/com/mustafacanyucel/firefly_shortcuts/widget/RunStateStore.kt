package com.mustafacanyucel.firefly_shortcuts.widget

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.edit
import com.mustafacanyucel.firefly_shortcuts.R

enum class RunState(@DrawableRes val icon: Int, @StringRes val label: Int) {
    IDLE(R.drawable.ic_widget_play, R.string.widget_run),
    RUNNING(R.drawable.ic_widget_hourglass, R.string.widget_running),
    SUCCEEDED(R.drawable.ic_widget_check, R.string.widget_run_succeeded),
    FAILED(R.drawable.ic_widget_error, R.string.widget_run_failed),
}

/**
 * Each shortcut's run state on the widget. A result shows for a few seconds,
 * then the row is idle again. A tap only counts while the row is idle, which
 * stops double runs.
 */
class RunStateStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** What the widget shows for shortcut [id] at [now]. */
    @Synchronized
    fun get(id: Long, now: Long = System.currentTimeMillis()): RunState {
        val state = prefs.getString(stateKey(id), null)
            ?.let { name -> RunState.entries.find { it.name == name } }
            ?: return RunState.IDLE
        val age = now - prefs.getLong(timeKey(id), 0)
        val shownFor = if (state == RunState.RUNNING) RUNNING_TIMEOUT_MS else RESULT_SHOWN_MS
        // A negative age means the clock moved back, so the entry can't be trusted.
        return if (age in 0L until shownFor) state else RunState.IDLE
    }

    /** Marks [id] as running if it's idle. Returns false if the tap should be ignored. */
    @Synchronized
    fun tryStart(id: Long): Boolean {
        val now = System.currentTimeMillis()
        if (get(id, now) != RunState.IDLE) return false
        set(id, RunState.RUNNING, now)
        return true
    }

    @Synchronized
    fun finish(id: Long, succeeded: Boolean) =
        set(id, if (succeeded) RunState.SUCCEEDED else RunState.FAILED, System.currentTimeMillis())

    private fun set(id: Long, state: RunState, time: Long) = prefs.edit {
        putString(stateKey(id), state.name)
        putLong(timeKey(id), time)
    }

    private fun stateKey(id: Long) = "state_$id"
    private fun timeKey(id: Long) = "time_$id"

    companion object {
        /** Left out of backups (step 9). */
        const val PREFS_NAME = "widget_run_state"

        /** A run still marked running after this is treated as lost, e.g. the process was killed. */
        const val RUNNING_TIMEOUT_MS = 5 * 60 * 1000L
        const val RESULT_SHOWN_MS = 5_000L
    }
}
