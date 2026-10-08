package com.anchor.app.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anchor.app.core.common.DispatcherProvider
import com.anchor.app.core.domain.model.AppCategory
import com.anchor.app.core.domain.model.AppSettings
import com.anchor.app.core.domain.repository.FocusRepository
import com.anchor.app.core.domain.repository.InsightRepository
import com.anchor.app.core.domain.repository.InterventionRepository
import com.anchor.app.core.domain.repository.SettingsRepository
import com.anchor.app.core.domain.repository.TaskDefinitionRepository
import com.anchor.app.core.domain.repository.TaskRepository
import com.anchor.app.core.domain.repository.UsageRepository
import com.anchor.app.platform.apps.InstalledApp
import com.anchor.app.platform.apps.InstalledAppsSource
import com.anchor.app.platform.accessibility.AccessibilityStatus
import com.anchor.app.platform.notification.NotificationPermissions
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
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val taskRepository: TaskRepository,
    private val definitionRepository: TaskDefinitionRepository,
    private val usageRepository: UsageRepository,
    private val focusRepository: FocusRepository,
    private val interventionRepository: InterventionRepository,
    private val insightRepository: InsightRepository,
    private val usageStats: UsageStatsSource,
    private val notificationPermissions: NotificationPermissions,
    private val appCatalog: InstalledAppsSource,
    private val accessibilityStatus: AccessibilityStatus,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    /** Live permission status for the two optional capabilities (FR-S1). */
    private val _permissions = MutableStateFlow(PermissionStatus())
    val permissions: StateFlow<PermissionStatus> = _permissions.asStateFlow()

    /** Detected app categories the user can override (FR-S3). */
    val categories: StateFlow<List<AppCategory>> = usageRepository.observeAppCategories()
        .map { it.sortedBy { c -> c.packageName } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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
            _installedApps.value = appCatalog.launchableApps()
        }
    }

    fun setAppQuery(query: String) { _appQuery.value = query }

    fun setCategoryQuery(query: String) { _categoryQuery.value = query }

    /** Human label for a package (from the installed-apps catalog, else package name). */
    fun labelFor(packageName: String): String =
        _installedApps.value.firstOrNull { it.packageName == packageName }?.label
            ?: appCatalog.labelFor(packageName)

    /** App icon for a package, or null. */
    fun iconFor(packageName: String): android.graphics.drawable.Drawable? =
        _installedApps.value.firstOrNull { it.packageName == packageName }?.icon
            ?: appCatalog.iconFor(packageName)

    /** Re-reads OS permission state; call on resume after the user returns from Settings. */
    fun refreshPermissions() {
        _permissions.value = PermissionStatus(
            usageAccessGranted = usageStats.hasPermission(),
            notificationsGranted = notificationPermissions.canPostNotifications(),
            focusBlockingGranted = accessibilityStatus.isEnabled(context),
        )
    }

    fun setAiEnabled(enabled: Boolean) =
        viewModelScope.launch(dispatchers.io) { settingsRepository.setAiEnabled(enabled) }

    fun setTrackingEnabled(enabled: Boolean) =
        viewModelScope.launch(dispatchers.io) { settingsRepository.setTrackingEnabled(enabled) }

    /** FR-F4/F8: opt-in toggle for Strict/Custom blocking (PRD v0.4 §10.5). */
    fun setFocusBlockingEnabled(enabled: Boolean) =
        viewModelScope.launch(dispatchers.io) { settingsRepository.setFocusBlockingEnabled(enabled) }

    /** Opens the OS Accessibility settings so the user can grant the service (docs 08 §4). */
    fun onOpenAccessibilitySettings() = accessibilityStatus.open(context)

    /** FR-S3: set/override the category label for a package. */
    fun onSetCategory(packageName: String, category: String) = viewModelScope.launch(dispatchers.io) {
        val trimmed = category.trim()
        if (trimmed.isEmpty()) return@launch
        usageRepository.upsertAppCategory(
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
        usageRepository.upsertAppCategory(
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
        taskRepository.clear()
        definitionRepository.clear()
        usageRepository.clearAll()
        focusRepository.clear()
        interventionRepository.clear()
        insightRepository.clear()
        settingsRepository.resetAll()
    }
}

/** Live OS permission availability shown on the Settings screen (FR-S1). */
data class PermissionStatus(
    val usageAccessGranted: Boolean = false,
    val notificationsGranted: Boolean = false,
    val focusBlockingGranted: Boolean = false,
)
