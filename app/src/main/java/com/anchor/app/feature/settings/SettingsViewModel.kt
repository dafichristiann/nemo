package com.anchor.app.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anchor.app.core.common.DispatcherProvider
import com.anchor.app.core.data.NemoDatabase
import com.anchor.app.core.design.theme.AppThemeMode
import com.anchor.app.core.design.theme.ThemePreferences
import com.anchor.app.core.domain.model.AppCategory
import com.anchor.app.core.domain.model.AppSettings
import com.anchor.app.core.domain.model.FocusArea
import com.anchor.app.core.domain.repository.SettingsRepository
import com.anchor.app.core.domain.repository.UsageRepository
import com.anchor.app.platform.apps.InstalledApp
import com.anchor.app.platform.apps.InstalledAppsSource
import com.anchor.app.platform.accessibility.AccessibilityStatus
import com.anchor.app.platform.ai.AiPreferences
import com.anchor.app.platform.notification.NotificationPermissions
import com.anchor.app.platform.service.FocusService
import com.anchor.app.platform.usagestats.UsageStatsSource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Settings screen state (docs 07-screens.md §2.13): persisted toggles plus live permission
 * status (FR-S1) and the managed app categories (FR-S3) / blocked apps (FR-S4).
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context? = null,
    private val database: NemoDatabase? = null,
    private val settingsRepository: SettingsRepository,
    private val usageRepository: UsageRepository? = null,
    private val usageStats: UsageStatsSource? = null,
    private val notificationPermissions: NotificationPermissions? = null,
    private val appCatalog: InstalledAppsSource? = null,
    private val accessibilityStatus: AccessibilityStatus? = null,
    private val dispatchers: DispatcherProvider,
    private val themePreferences: ThemePreferences? = null,
    private val aiPreferences: AiPreferences? = null,
) : ViewModel() {

    val themeMode: StateFlow<AppThemeMode> = themePreferences?.themeMode
        ?: MutableStateFlow(AppThemeMode.SYSTEM)

    fun toggleTheme(isCurrentlyDark: Boolean) {
        themePreferences?.toggleTheme(isCurrentlyDark)
    }

    fun setThemeMode(mode: AppThemeMode) {
        themePreferences?.setThemeMode(mode)
    }

    val customApiKey: StateFlow<String> = aiPreferences?.customApiKey
        ?: MutableStateFlow("")

    private val _apiKeyTestStatus = MutableStateFlow<ApiKeyTestStatus>(ApiKeyTestStatus.Idle)
    val apiKeyTestStatus: StateFlow<ApiKeyTestStatus> = _apiKeyTestStatus.asStateFlow()

    fun setCustomApiKey(key: String) {
        aiPreferences?.setCustomApiKey(key)
        _apiKeyTestStatus.value = ApiKeyTestStatus.Idle
    }

    fun testApiKey(keyToTest: String? = null) {
        val key = (keyToTest ?: aiPreferences?.getEffectiveApiKey()).orEmpty().trim()
        if (key.isBlank()) {
            _apiKeyTestStatus.value = ApiKeyTestStatus.Error("API key is blank")
            return
        }
        viewModelScope.launch(dispatchers.io) {
            _apiKeyTestStatus.value = ApiKeyTestStatus.Testing
            try {
                val url = java.net.URL("https://generativelanguage.googleapis.com/v1beta/models")
                val connection = (url.openConnection() as java.net.HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8_000
                    readTimeout = 8_000
                    setRequestProperty("x-goog-api-key", key)
                }
                val code = connection.responseCode
                connection.disconnect()
                _apiKeyTestStatus.value = if (code in 200..299) {
                    ApiKeyTestStatus.Success
                } else {
                    ApiKeyTestStatus.Error("HTTP $code")
                }
            } catch (e: Exception) {
                _apiKeyTestStatus.value = ApiKeyTestStatus.Error(e.message ?: "Network error")
            }
        }
    }

    fun clearApiKeyTestStatus() {
        _apiKeyTestStatus.value = ApiKeyTestStatus.Idle
    }

    fun hasCustomApiKey(): Boolean = aiPreferences?.hasCustomApiKey() ?: false

    fun hasEffectiveApiKey(): Boolean = aiPreferences?.hasEffectiveApiKey() ?: false

    val settings: StateFlow<AppSettings> = settingsRepository.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    /** Persisted master switch for task reminders & interventions. */
    val notificationsEnabled: StateFlow<Boolean> = settingsRepository.observe()
        .map { it.notificationsEnabled }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    /** Live permission status for the two optional capabilities (FR-S1). */
    private val _permissions = MutableStateFlow(PermissionStatus())
    val permissions: StateFlow<PermissionStatus> = _permissions.asStateFlow()

    /** Detected app categories the user can override (FR-S3). */
    val categories: StateFlow<List<AppCategory>> = usageRepository?.observeAppCategories()
        ?.map { it.sortedBy { c -> c.packageName } }
        ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        ?: MutableStateFlow(emptyList())

    /** Launchable apps on the device, for the blocked-apps picker (FR-S4). */
    private val _installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val installedApps: StateFlow<List<InstalledApp>> = _installedApps.asStateFlow()

    /** Current search text for the blocked-apps picker. */
    private val _appQuery = MutableStateFlow("")
    val appQuery: StateFlow<String> = _appQuery.asStateFlow()

    /** Installed apps filtered by [appQuery] (case-insensitive label/package match). */
    val filteredApps: StateFlow<List<InstalledApp>> = combine(
        _installedApps, _appQuery,
    ) { apps, query ->
        if (query.isBlank()) apps
        else apps.filter {
            it.label.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Current search text for the app-categories screen. */
    private val _categoryQuery = MutableStateFlow("")
    val categoryQuery: StateFlow<String> = _categoryQuery.asStateFlow()

    /** Categories filtered by [categoryQuery] (matches package or category label). */
    val filteredCategories: StateFlow<List<AppCategory>> = combine(
        categories, _categoryQuery,
    ) { cats, query ->
        if (query.isBlank()) cats
        else cats.filter {
            it.packageName.contains(query, ignoreCase = true) || it.category.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        refreshPermissions()
        loadInstalledApps()
    }

    /** Loads the launchable apps off the main thread (FR-S4). */
    private fun loadInstalledApps() {
        viewModelScope.launch(dispatchers.io) {
            _installedApps.value = appCatalog?.launchableApps() ?: emptyList()
        }
    }

    fun setAppQuery(query: String) { _appQuery.value = query }

    fun setCategoryQuery(query: String) { _categoryQuery.value = query }

    /** Human label for a package (from the installed-apps catalog, else package name). */
    fun labelFor(packageName: String): String =
        _installedApps.value.firstOrNull { it.packageName == packageName }?.label
            ?: appCatalog?.labelFor(packageName) ?: packageName

    /** App icon for a package, or null. */
    fun iconFor(packageName: String): android.graphics.drawable.Drawable? =
        _installedApps.value.firstOrNull { it.packageName == packageName }?.icon
            ?: appCatalog?.iconFor(packageName)

    /** Re-reads OS permission state; call on resume after the user returns from Settings. */
    fun refreshPermissions() {
        _permissions.value = PermissionStatus(
            usageAccessGranted = usageStats?.hasPermission() ?: false,
            notificationsGranted = notificationPermissions?.canPostNotifications() ?: false,
            focusBlockingGranted = if (context != null && accessibilityStatus != null) accessibilityStatus.isEnabled(context) else false,
        )
    }

    fun setAiEnabled(enabled: Boolean) =
        viewModelScope.launch(dispatchers.io) { settingsRepository.setAiEnabled(enabled) }

    fun setTrackingEnabled(enabled: Boolean) =
        viewModelScope.launch(dispatchers.io) { settingsRepository.setTrackingEnabled(enabled) }

    fun setNotificationsEnabled(enabled: Boolean) =
        viewModelScope.launch(dispatchers.io) { settingsRepository.setNotificationsEnabled(enabled) }

    fun toggleNotifications(enabled: Boolean) = setNotificationsEnabled(enabled)

    /** FR-F4/F8: opt-in toggle for Strict/Custom blocking (PRD v0.4 §10.5). */
    fun setFocusBlockingEnabled(enabled: Boolean) =
        viewModelScope.launch(dispatchers.io) { settingsRepository.setFocusBlockingEnabled(enabled) }

    /** Toggle the continuous doomscroll alert feature (specs 2026-11-01). */
    fun setDoomscrollAlertEnabled(enabled: Boolean) =
        viewModelScope.launch(dispatchers.io) { settingsRepository.setDoomscrollAlertEnabled(enabled) }

    /** Update continuous scrolling alert threshold in minutes (10/15/20/30/45). */
    fun setDoomscrollThresholdMinutes(minutes: Int) {
        require(minutes in AppSettings.DOOMSCROLL_THRESHOLD_OPTIONS_MIN) {
            "Invalid duration: $minutes min"
        }
        viewModelScope.launch(dispatchers.io) {
            settingsRepository.setDoomscrollThresholdMinutes(minutes)
        }
    }

    /** Profile: update the mascot's preferred name (blank clears it). */
    fun setNickname(nickname: String) =
        viewModelScope.launch(dispatchers.io) { settingsRepository.setNickname(nickname) }

    /** Profile: update the user's main focus area. */
    fun setFocusArea(area: FocusArea) =
        viewModelScope.launch(dispatchers.io) { settingsRepository.setFocusArea(area) }

    /** Profile: update the default focus-session length (25/45/60). */
    fun setDefaultFocusDurationMin(minutes: Int) =
        viewModelScope.launch(dispatchers.io) { settingsRepository.setDefaultFocusDurationMin(minutes) }

    fun setLocale(locale: String) {
        val normalized = if (locale.startsWith("id", true) || locale.startsWith("in", true)) "id" else "en"
        viewModelScope.launch(dispatchers.io) {
            settingsRepository.setLocale(normalized)
        }
        if (context != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            try {
                val localeManager = context.getSystemService(android.app.LocaleManager::class.java)
                val target = if (normalized == "id") java.util.Locale("in", "ID") else java.util.Locale.ENGLISH
                localeManager?.applicationLocales = android.os.LocaleList(target)
            } catch (_: Exception) {}
        }
    }

    /** Opens the OS Accessibility settings so the user can grant the service (docs 08 §4). */
    fun onOpenAccessibilitySettings() {
        if (context != null) accessibilityStatus?.open(context)
    }

    /** FR-S3: set/override the category label for a package. */
    fun onSetCategory(packageName: String, category: String) = viewModelScope.launch(dispatchers.io) {
        val trimmed = category.trim()
        if (trimmed.isEmpty()) return@launch
        usageRepository?.upsertAppCategory(
            AppCategory(
                packageName = packageName,
                category = trimmed,
                isDistraction = categories.value.firstOrNull { it.packageName == packageName }?.isDistraction ?: false,
                overrideSource = AppCategory.SOURCE_USER,
            ),
        )
    }

    /** FR-S3: toggle whether an app counts as a distraction. */
    fun onToggleDistraction(category: AppCategory) = viewModelScope.launch(dispatchers.io) {
        usageRepository?.upsertAppCategory(
            category.copy(isDistraction = !category.isDistraction, overrideSource = AppCategory.SOURCE_USER),
        )
    }

    /** FR-S4: add a package to the blocked set. */
    fun onAddBlockedApp(packageName: String) = viewModelScope.launch(dispatchers.io) {
        val trimmed = packageName.trim()
        if (trimmed.isEmpty()) return@launch
        settingsRepository.setBlockedApps(settings.value.blockedApps + trimmed)
    }

    /** FR-S4: remove a package from the blocked set. */
    fun onRemoveBlockedApp(packageName: String) = viewModelScope.launch(dispatchers.io) {
        settingsRepository.setBlockedApps(settings.value.blockedApps - packageName)
    }

    /** "Delete all data" — erase every user table then reset settings (PRD §10.10). */
    fun onDeleteAllData() = viewModelScope.launch(dispatchers.io) {
        context?.let { FocusService.stop(it) }
        database?.clearAllTables()
        settingsRepository.resetAll()
        aiPreferences?.clear()
    }
}

/** Live OS permission availability shown on the Settings screen (FR-S1). */
data class PermissionStatus(
    val usageAccessGranted: Boolean = false,
    val notificationsGranted: Boolean = false,
    val focusBlockingGranted: Boolean = false,
)

sealed interface ApiKeyTestStatus {
    object Idle : ApiKeyTestStatus
    object Testing : ApiKeyTestStatus
    object Success : ApiKeyTestStatus
    data class Error(val message: String) : ApiKeyTestStatus
}
