package com.anchor.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anchor.app.core.common.DispatcherProvider
import com.anchor.app.core.domain.model.AppSettings
import com.anchor.app.core.domain.repository.FocusRepository
import com.anchor.app.core.domain.repository.InsightRepository
import com.anchor.app.core.domain.repository.InterventionRepository
import com.anchor.app.core.domain.repository.SettingsRepository
import com.anchor.app.core.domain.repository.TaskDefinitionRepository
import com.anchor.app.core.domain.repository.TaskRepository
import com.anchor.app.core.domain.repository.UsageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Settings screen state (docs 07-screens.md §2.13). */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val taskRepository: TaskRepository,
    private val definitionRepository: TaskDefinitionRepository,
    private val usageRepository: UsageRepository,
    private val focusRepository: FocusRepository,
    private val interventionRepository: InterventionRepository,
    private val insightRepository: InsightRepository,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun setAiEnabled(enabled: Boolean) =
        viewModelScope.launch(dispatchers.io) { settingsRepository.setAiEnabled(enabled) }

    fun setTrackingEnabled(enabled: Boolean) =
        viewModelScope.launch(dispatchers.io) { settingsRepository.setTrackingEnabled(enabled) }

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
