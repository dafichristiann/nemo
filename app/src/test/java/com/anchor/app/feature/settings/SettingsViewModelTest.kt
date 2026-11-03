package com.anchor.app.feature.settings

import com.anchor.app.core.domain.model.AppSettings
import com.anchor.app.testutil.FakeSettingsRepository
import com.anchor.app.testutil.TestDispatcherProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var settingsRepository: FakeSettingsRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        settingsRepository = FakeSettingsRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = SettingsViewModel(
        settingsRepository = settingsRepository,
        dispatchers = TestDispatcherProvider(dispatcher),
    )

    @Test
    fun initialState_reflectsDoomscrollDefaults() = runTest(dispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        assertFalse(vm.settings.value.doomscrollAlertEnabled)
        assertEquals(
            AppSettings.DEFAULT_DOOMSCROLL_THRESHOLD_MIN,
            vm.settings.value.doomscrollThresholdMinutes,
        )
    }

    @Test
    fun setDoomscrollAlertEnabled_updatesSetting() = runTest(dispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.setDoomscrollAlertEnabled(true)
        advanceUntilIdle()
        assertTrue(settingsRepository.state.value.doomscrollAlertEnabled)

        vm.setDoomscrollAlertEnabled(false)
        advanceUntilIdle()
        assertFalse(settingsRepository.state.value.doomscrollAlertEnabled)
    }

    @Test
    fun setDoomscrollThresholdMinutes_updatesSettingForAllowedValues() = runTest(dispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        for (minutes in AppSettings.DOOMSCROLL_THRESHOLD_OPTIONS_MIN) {
            vm.setDoomscrollThresholdMinutes(minutes)
            advanceUntilIdle()
            assertEquals(minutes, settingsRepository.state.value.doomscrollThresholdMinutes)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun setDoomscrollThresholdMinutes_rejectsInvalidValue() {
        val vm = createViewModel()
        vm.setDoomscrollThresholdMinutes(25)
    }
}
