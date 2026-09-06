package com.guitarvault.app.ui.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** User-selectable theme mode. */
enum class ThemeMode(val displayName: String) {
    SYSTEM("Follow system"),
    LIGHT("Light"),
    DARK("Dark")
}

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_prefs")

/**
 * Holds the user's theme choice, persisted via DataStore.
 * Initialized once at app start; UI reads [mode] as state.
 */
object ThemePrefs {
    private val KEY_MODE = stringPreferencesKey("theme_mode")

    private val _mode = MutableStateFlow(ThemeMode.SYSTEM)
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    private var appContext: Context? = null

    /** Call from Application.onCreate — loads the saved preference. */
    fun init(context: Context) {
        appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            context.applicationContext.themeDataStore.data.collect { prefs ->
                val saved = prefs[KEY_MODE]
                if (saved != null) {
                    _mode.value = runCatching { ThemeMode.valueOf(saved) }.getOrDefault(ThemeMode.SYSTEM)
                }
            }
        }
    }

    fun setMode(mode: ThemeMode) {
        _mode.value = mode
        val ctx = appContext ?: return
        CoroutineScope(Dispatchers.IO).launch {
            ctx.themeDataStore.edit { it[KEY_MODE] = mode.name }
        }
    }

    /** Convenience for cycling System -> Light -> Dark -> System. */
    fun nextMode() {
        val modes = ThemeMode.entries
        setMode(modes[(modes.indexOf(_mode.value) + 1) % modes.size])
    }
}
