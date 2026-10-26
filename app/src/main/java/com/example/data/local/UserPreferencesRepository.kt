package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ultra_player_prefs")

enum class SortOption(val title: String) {
    DATE_DESC("Date (Newest)"),
    DATE_ASC("Date (Oldest)"),
    NAME_ASC("Name (A to Z)"),
    NAME_DESC("Name (Z to A)"),
    SIZE_DESC("Size (Largest)"),
    DURATION_DESC("Duration (Longest)")
}

data class UserPreferences(
    val isDarkTheme: Boolean = false, // Default is Modern White theme
    val accentColorName: String = "Blue",
    val doubleTapSeekSeconds: Int = 10,
    val autoResume: Boolean = true,
    val autoPip: Boolean = true,
    val backgroundPlayback: Boolean = false,
    val defaultAspectRatio: String = "FIT",
    val isGridView: Boolean = false,
    val sortBy: SortOption = SortOption.DATE_DESC,
    val pinLockCode: String = "",
    val isPinLockEnabled: Boolean = false,
    val autoCheckUpdates: Boolean = true,
    val githubOwner: String = "jeebanrai05-eng",
    val githubRepo: String = "Mx-player-pro"
)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val DOUBLE_TAP_SEEK = intPreferencesKey("double_tap_seek")
        val AUTO_RESUME = booleanPreferencesKey("auto_resume")
        val AUTO_PIP = booleanPreferencesKey("auto_pip")
        val BACKGROUND_PLAYBACK = booleanPreferencesKey("background_playback")
        val DEFAULT_ASPECT_RATIO = stringPreferencesKey("default_aspect_ratio")
        val IS_GRID_VIEW = booleanPreferencesKey("is_grid_view")
        val SORT_BY = stringPreferencesKey("sort_by")
        val PIN_LOCK_CODE = stringPreferencesKey("pin_lock_code")
        val IS_PIN_LOCK_ENABLED = booleanPreferencesKey("is_pin_lock_enabled")
        val AUTO_CHECK_UPDATES = booleanPreferencesKey("auto_check_updates")
        val GITHUB_OWNER = stringPreferencesKey("github_owner")
        val GITHUB_REPO = stringPreferencesKey("github_repo")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        val isDark = preferences[PreferencesKeys.IS_DARK_THEME] ?: false // Modern White Theme by default
        val accentColor = preferences[PreferencesKeys.ACCENT_COLOR] ?: "Blue"
        val doubleTap = preferences[PreferencesKeys.DOUBLE_TAP_SEEK] ?: 10
        val autoResume = preferences[PreferencesKeys.AUTO_RESUME] ?: true
        val autoPip = preferences[PreferencesKeys.AUTO_PIP] ?: true
        val bgPlayback = preferences[PreferencesKeys.BACKGROUND_PLAYBACK] ?: false
        val aspectRatio = preferences[PreferencesKeys.DEFAULT_ASPECT_RATIO] ?: "FIT"
        val isGrid = preferences[PreferencesKeys.IS_GRID_VIEW] ?: false
        val sortString = preferences[PreferencesKeys.SORT_BY] ?: SortOption.DATE_DESC.name
        val sort = try {
            SortOption.valueOf(sortString)
        } catch (e: Exception) {
            SortOption.DATE_DESC
        }
        val pin = preferences[PreferencesKeys.PIN_LOCK_CODE] ?: ""
        val isPinEnabled = preferences[PreferencesKeys.IS_PIN_LOCK_ENABLED] ?: false
        val autoCheckUpdates = preferences[PreferencesKeys.AUTO_CHECK_UPDATES] ?: true
        val owner = preferences[PreferencesKeys.GITHUB_OWNER] ?: "jeebanrai05-eng"
        val repo = preferences[PreferencesKeys.GITHUB_REPO] ?: "Mx-player-pro"

        UserPreferences(
            isDarkTheme = isDark,
            accentColorName = accentColor,
            doubleTapSeekSeconds = doubleTap,
            autoResume = autoResume,
            autoPip = autoPip,
            backgroundPlayback = bgPlayback,
            defaultAspectRatio = aspectRatio,
            isGridView = isGrid,
            sortBy = sort,
            pinLockCode = pin,
            isPinLockEnabled = isPinEnabled,
            autoCheckUpdates = autoCheckUpdates,
            githubOwner = owner,
            githubRepo = repo
        )
    }

    suspend fun updateDarkTheme(isDark: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.IS_DARK_THEME] = isDark }
    }

    suspend fun updateAccentColor(colorName: String) {
        context.dataStore.edit { it[PreferencesKeys.ACCENT_COLOR] = colorName }
    }

    suspend fun updateDoubleTapSeek(seconds: Int) {
        context.dataStore.edit { it[PreferencesKeys.DOUBLE_TAP_SEEK] = seconds }
    }

    suspend fun updateAutoResume(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.AUTO_RESUME] = enabled }
    }

    suspend fun updateAutoPip(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.AUTO_PIP] = enabled }
    }

    suspend fun updateBackgroundPlayback(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.BACKGROUND_PLAYBACK] = enabled }
    }

    suspend fun updateDefaultAspectRatio(ratio: String) {
        context.dataStore.edit { it[PreferencesKeys.DEFAULT_ASPECT_RATIO] = ratio }
    }

    suspend fun updateIsGridView(isGrid: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.IS_GRID_VIEW] = isGrid }
    }

    suspend fun updateSortBy(option: SortOption) {
        context.dataStore.edit { it[PreferencesKeys.SORT_BY] = option.name }
    }

    suspend fun updatePinLock(code: String, enabled: Boolean) {
        context.dataStore.edit {
            it[PreferencesKeys.PIN_LOCK_CODE] = code
            it[PreferencesKeys.IS_PIN_LOCK_ENABLED] = enabled
        }
    }

    suspend fun updateAutoCheckUpdates(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.AUTO_CHECK_UPDATES] = enabled }
    }

    suspend fun updateGithubRepoConfig(owner: String, repo: String) {
        context.dataStore.edit {
            it[PreferencesKeys.GITHUB_OWNER] = owner.trim()
            it[PreferencesKeys.GITHUB_REPO] = repo.trim()
        }
    }
}
