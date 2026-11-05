package com.anchor.app.feature.onboarding

import com.anchor.app.core.domain.model.AppSettings
import com.anchor.app.core.domain.model.FocusArea
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
import org.junit.Before
import org.junit.Test

/**
 * Tests for [ProfileSetupViewModel] — the 3 onboarding profile fields (docs 07-screens.md §2.3b).
 * Verifies each field is persisted to settings and that the nickname is trimmed and length-capped.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileSetupViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var settings: FakeSettingsRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        settings = FakeSettingsRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = ProfileSetupViewModel(settings, TestDispatcherProvider(dispatcher))

    @Test
    fun onNicknameChange_persistsTrimmedValue() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onNicknameChange("  Rani  ")
        advanceUntilIdle()
        assertEquals("Rani", settings.state.value.nickname)
    }

    @Test
    fun onNicknameChange_capsLength() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onNicknameChange("x".repeat(100))
        advanceUntilIdle()
        assertEquals(AppSettings.MAX_NICKNAME_LENGTH, settings.state.value.nickname.length)
    }

    @Test
    fun onSelectFocusArea_persistsArea() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onSelectFocusArea(FocusArea.CAREER)
        advanceUntilIdle()
        assertEquals(FocusArea.CAREER, settings.state.value.focusArea)
    }

    @Test
    fun onSelectDuration_persistsMinutes() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onSelectDuration(45)
        advanceUntilIdle()
        assertEquals(45, settings.state.value.defaultFocusDurationMin)
    }

    @Test
    fun onToggleDoomscroll_persistsEnabled() = runTest(dispatcher) {
        val vm = viewModel()
        assertEquals(false, settings.state.value.doomscrollAlertEnabled)
        vm.onToggleDoomscroll(true)
        advanceUntilIdle()
        assertEquals(true, settings.state.value.doomscrollAlertEnabled)
        vm.onToggleDoomscroll(false)
        advanceUntilIdle()
        assertEquals(false, settings.state.value.doomscrollAlertEnabled)
    }

    @Test
    fun onSelectDoomscrollThreshold_persistsMinutes() = runTest(dispatcher) {
        val vm = viewModel()
        assertEquals(20, settings.state.value.doomscrollThresholdMinutes)
        vm.onSelectDoomscrollThreshold(30)
        advanceUntilIdle()
        assertEquals(30, settings.state.value.doomscrollThresholdMinutes)
    }

    @Test
    fun onToggleTracking_persistsEnabled() = runTest(dispatcher) {
        val vm = viewModel()
        assertEquals(false, settings.state.value.trackingEnabled)
        vm.onToggleTracking(true)
        advanceUntilIdle()
        assertEquals(true, settings.state.value.trackingEnabled)
        vm.onToggleTracking(false)
        advanceUntilIdle()
        assertEquals(false, settings.state.value.trackingEnabled)
    }

    @Test
    fun onToggleAi_persistsEnabled() = runTest(dispatcher) {
        val vm = viewModel()
        assertEquals(false, settings.state.value.aiEnabled)
        vm.onToggleAi(true)
        advanceUntilIdle()
        assertEquals(true, settings.state.value.aiEnabled)
        vm.onToggleAi(false)
        advanceUntilIdle()
        assertEquals(false, settings.state.value.aiEnabled)
    }

    @Test
    fun onToggleFocusBlocking_persistsEnabled() = runTest(dispatcher) {
        val vm = viewModel()
        assertEquals(false, settings.state.value.focusBlockingEnabled)
        vm.onToggleFocusBlocking(true)
        advanceUntilIdle()
        assertEquals(true, settings.state.value.focusBlockingEnabled)
        vm.onToggleFocusBlocking(false)
        advanceUntilIdle()
        assertEquals(false, settings.state.value.focusBlockingEnabled)
    }

    @Test
    fun onEnableAllKeyFeatures_enablesAllThree() = runTest(dispatcher) {
        val vm = viewModel()
        assertEquals(false, settings.state.value.trackingEnabled)
        assertEquals(false, settings.state.value.aiEnabled)
        assertEquals(false, settings.state.value.focusBlockingEnabled)

        vm.onEnableAllKeyFeatures()
        advanceUntilIdle()

        assertEquals(true, settings.state.value.trackingEnabled)
        assertEquals(true, settings.state.value.aiEnabled)
        assertEquals(true, settings.state.value.focusBlockingEnabled)
    }

    @Test
    fun defaults_areCollegeAnd25Minutes() {
        // New users start on the safe defaults the onboarding chips preselect.
        assertEquals(FocusArea.COLLEGE, AppSettings().focusArea)
        assertEquals(25, AppSettings().defaultFocusDurationMin)
        assertEquals(listOf(25, 45, 60), AppSettings.FOCUS_DURATION_OPTIONS_MIN)
        assertEquals(false, AppSettings().doomscrollAlertEnabled)
        assertEquals(20, AppSettings().doomscrollThresholdMinutes)
        assertEquals(listOf(1, 10, 15, 20, 30, 45), AppSettings.DOOMSCROLL_THRESHOLD_OPTIONS_MIN)
    }
}
