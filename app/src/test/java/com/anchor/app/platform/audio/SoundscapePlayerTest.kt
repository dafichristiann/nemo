package com.anchor.app.platform.audio

import android.media.AudioManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests verifying [AndroidSoundscapePlayer] contract and state transitions.
 * Pure JVM test using [FakeAudioEngine].
 */
class SoundscapePlayerTest {

    private lateinit var fakeEngine: FakeAudioEngine
    private lateinit var player: AndroidSoundscapePlayer

    private class FakeAudioEngine : AudioEngine {
        var lastPreparedResId: Int? = null
        var isLoopingConfigured: Boolean = true
        var isCurrentlyPlaying: Boolean = false
        var currentVolume: Float = 1.0f
        var focusRequested: Boolean = false
        var focusAbandoned: Boolean = false
        var preparedCount: Int = 0
        var pauseCount: Int = 0
        var resumeCount: Int = 0
        var stopCount: Int = 0
        var releaseCount: Int = 0
        var shouldFail: Boolean = false

        override fun prepareAndPlay(soundResId: Int, looping: Boolean): Boolean {
            if (shouldFail) return false
            lastPreparedResId = soundResId
            this.isLoopingConfigured = looping
            this.isCurrentlyPlaying = true
            preparedCount++
            return true
        }

        override fun pause(): Boolean {
            isCurrentlyPlaying = false
            pauseCount++
            return true
        }

        override fun resume(): Boolean {
            if (shouldFail) return false
            isCurrentlyPlaying = true
            resumeCount++
            return true
        }

        override fun stop(): Boolean {
            isCurrentlyPlaying = false
            stopCount++
            return true
        }

        override fun release() {
            isCurrentlyPlaying = false
            releaseCount++
        }

        override fun isPlaying(): Boolean = isCurrentlyPlaying

        override fun setVolume(volume: Float) {
            currentVolume = volume
        }

        override fun setLooping(looping: Boolean) {
            this.isLoopingConfigured = looping
        }

        override fun requestAudioFocus(): Boolean {
            focusRequested = true
            return true
        }

        override fun abandonAudioFocus() {
            focusAbandoned = true
        }
    }

    @Before
    fun setUp() {
        fakeEngine = FakeAudioEngine()
        player = AndroidSoundscapePlayer(context = null, engine = fakeEngine)
    }

    @Test
    fun initialState_isIdleAndNotPlaying() {
        assertEquals(SoundscapeState.IDLE, player.state)
        assertFalse(player.isPlaying())
    }

    @Test
    fun play_initializesAndStartsEngine_setsPlayingState() {
        val testRes = 12345
        player.play(testRes)

        assertEquals(SoundscapeState.PLAYING, player.state)
        assertTrue(player.isPlaying())
        assertTrue(fakeEngine.focusRequested)
        assertEquals(testRes, fakeEngine.lastPreparedResId)
        assertTrue(fakeEngine.isLoopingConfigured)
    }

    @Test
    fun pause_whenPlaying_setsPausedStateAndCallsEnginePause() {
        player.play(123)
        player.pause()

        assertEquals(SoundscapeState.PAUSED, player.state)
        assertFalse(player.isPlaying())
        assertEquals(1, fakeEngine.pauseCount)
    }

    @Test
    fun pause_whenNotPlaying_doesNothing() {
        player.pause()

        assertEquals(SoundscapeState.IDLE, player.state)
        assertEquals(0, fakeEngine.pauseCount)
    }

    @Test
    fun resume_whenPaused_requestsFocusAndResumesPlayback() {
        player.play(123)
        player.pause()
        fakeEngine.focusRequested = false

        player.resume()

        assertEquals(SoundscapeState.PLAYING, player.state)
        assertTrue(player.isPlaying())
        assertTrue(fakeEngine.focusRequested)
        assertEquals(1, fakeEngine.resumeCount)
    }

    @Test
    fun resume_whenStoppedOrIdle_replaysTrack() {
        player.play(123)
        player.stop()
        assertEquals(1, fakeEngine.preparedCount)

        player.resume()

        assertEquals(SoundscapeState.PLAYING, player.state)
        assertTrue(player.isPlaying())
        assertEquals(2, fakeEngine.preparedCount)
    }

    @Test
    fun stop_stopsEngine_abandonsFocus_setsStoppedState() {
        player.play(123)
        player.stop()

        assertEquals(SoundscapeState.STOPPED, player.state)
        assertFalse(player.isPlaying())
        assertTrue(fakeEngine.focusAbandoned)
        assertEquals(1, fakeEngine.stopCount)
    }

    @Test
    fun release_releasesEngine_setsReleasedState() {
        player.play(123)
        player.release()

        assertEquals(SoundscapeState.RELEASED, player.state)
        assertFalse(player.isPlaying())
        assertEquals(1, fakeEngine.releaseCount)
    }

    @Test
    fun setLooping_updatesLoopingFlagOnEngine() {
        player.setLooping(false)
        assertFalse(fakeEngine.isLoopingConfigured)

        player.play(123)
        assertFalse(fakeEngine.isLoopingConfigured)
    }

    @Test
    fun audioFocusLossTransientCanDuck_ducksVolumeAndRestoresOnGain() {
        player.play(123)
        assertEquals(1.0f, fakeEngine.currentVolume, 0.01f)

        // Notification arrived
        player.handleAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)
        assertEquals(0.2f, fakeEngine.currentVolume, 0.01f)

        // Notification ended
        player.handleAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)
        assertEquals(1.0f, fakeEngine.currentVolume, 0.01f)
    }

    @Test
    fun audioFocusLossTransient_pausesPlaybackAndResumesOnGain() {
        player.play(123)
        assertTrue(player.isPlaying())

        // Incoming call / navigation
        player.handleAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
        assertEquals(SoundscapeState.PAUSED, player.state)
        assertFalse(player.isPlaying())

        // Call ended
        player.handleAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)
        assertEquals(SoundscapeState.PLAYING, player.state)
        assertTrue(player.isPlaying())
    }

    @Test
    fun audioFocusLoss_stopsPlaybackPermanently() {
        player.play(123)
        assertTrue(player.isPlaying())

        // Another app started playback
        player.handleAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS)
        assertEquals(SoundscapeState.STOPPED, player.state)
        assertFalse(player.isPlaying())
    }

    @Test
    fun play_whenEngineFails_revertsToIdleWithoutCrashing() {
        fakeEngine.shouldFail = true
        player.play(123)

        assertEquals(SoundscapeState.IDLE, player.state)
        assertFalse(player.isPlaying())
    }
}
