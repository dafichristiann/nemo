package com.anchor.app.platform.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import androidx.annotation.RawRes
import com.anchor.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Contract for background audio soundscapes during focus sessions (FR-F7).
 */
interface SoundscapePlayer {
    /** Plays the audio resource. Loops indefinitely by default. */
    fun play(@RawRes soundResId: Int = R.raw.nemo_lofi_focus)

    /** Pauses active playback without releasing resources. */
    fun pause()

    /** Resumes playback if previously paused, or starts playback if stopped. */
    fun resume()

    /** Stops playback and resets playback position. */
    fun stop()

    /** Releases all underlying audio resources and abandons audio focus. */
    fun release()

    /** Returns whether audio is currently playing. */
    fun isPlaying(): Boolean

    /** Configures whether playback should loop seamlessly when completed. */
    fun setLooping(looping: Boolean)
}

enum class SoundscapeState {
    IDLE,
    PLAYING,
    PAUSED,
    STOPPED,
    RELEASED
}

/**
 * Decoupled engine interface for low-level media player and audio focus controls,
 * allowing safe JVM unit testing without Android runtime stubs.
 */
internal interface AudioEngine {
    fun prepareAndPlay(soundResId: Int, looping: Boolean): Boolean
    fun pause(): Boolean
    fun resume(): Boolean
    fun stop(): Boolean
    fun release()
    fun isPlaying(): Boolean
    fun setVolume(volume: Float)
    fun setLooping(looping: Boolean)
    fun requestAudioFocus(): Boolean
    fun abandonAudioFocus()
}

/**
 * Standard Android implementation of [SoundscapePlayer] handling [MediaPlayer]
 * lifecycle and [AudioManager] audio focus (gain, transient loss, ducking).
 */
@Singleton
class AndroidSoundscapePlayer internal constructor(
    context: Context?,
    engine: AudioEngine?,
) : SoundscapePlayer {

    @Inject
    constructor(@ApplicationContext context: Context) : this(context, null)

    private val actualEngine: AudioEngine = engine ?: DefaultAudioEngine(
        context = checkNotNull(context) { "Context is required when AudioEngine is not supplied" },
        onAudioFocusChange = ::handleAudioFocusChange,
    )

    private var currentState: SoundscapeState = SoundscapeState.IDLE
    private var isLoopingEnabled: Boolean = true
    private var currentResId: Int = R.raw.nemo_lofi_focus
    private var wasPlayingBeforeTransientLoss: Boolean = false
    private var isDucked: Boolean = false

    val state: SoundscapeState
        get() = currentState

    override fun play(@RawRes soundResId: Int) {
        currentResId = soundResId
        actualEngine.requestAudioFocus()
        val success = actualEngine.prepareAndPlay(soundResId, isLoopingEnabled)
        currentState = if (success) {
            SoundscapeState.PLAYING
        } else {
            SoundscapeState.IDLE
        }
    }

    override fun pause() {
        if (currentState == SoundscapeState.PLAYING) {
            actualEngine.pause()
            currentState = SoundscapeState.PAUSED
        }
    }

    override fun resume() {
        if (currentState == SoundscapeState.PAUSED) {
            actualEngine.requestAudioFocus()
            val success = actualEngine.resume()
            currentState = if (success) {
                SoundscapeState.PLAYING
            } else {
                play(currentResId)
                currentState
            }
        } else if (currentState == SoundscapeState.STOPPED || currentState == SoundscapeState.IDLE) {
            play(currentResId)
        }
    }

    override fun stop() {
        actualEngine.stop()
        actualEngine.abandonAudioFocus()
        currentState = SoundscapeState.STOPPED
        wasPlayingBeforeTransientLoss = false
        isDucked = false
    }

    override fun release() {
        actualEngine.release()
        currentState = SoundscapeState.RELEASED
        wasPlayingBeforeTransientLoss = false
        isDucked = false
    }

    override fun isPlaying(): Boolean =
        currentState == SoundscapeState.PLAYING && actualEngine.isPlaying()

    override fun setLooping(looping: Boolean) {
        isLoopingEnabled = looping
        actualEngine.setLooping(looping)
    }

    internal fun handleAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (isDucked) {
                    isDucked = false
                    actualEngine.setVolume(1.0f)
                }
                if (wasPlayingBeforeTransientLoss) {
                    wasPlayingBeforeTransientLoss = false
                    resume()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (currentState == SoundscapeState.PLAYING) {
                    isDucked = true
                    actualEngine.setVolume(0.2f)
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (currentState == SoundscapeState.PLAYING) {
                    wasPlayingBeforeTransientLoss = true
                    pause()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                wasPlayingBeforeTransientLoss = false
                stop()
            }
        }
    }

    private class DefaultAudioEngine(
        private val context: Context,
        private val onAudioFocusChange: (Int) -> Unit,
    ) : AudioEngine {

        private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        private var mediaPlayer: MediaPlayer? = null
        private var focusRequest: AudioFocusRequest? = null

        private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
            onAudioFocusChange(focusChange)
        }

        override fun requestAudioFocus(): Boolean = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val attributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()

                val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(attributes)
                    .setAcceptsDelayedFocusGain(false)
                    .setOnAudioFocusChangeListener(audioFocusChangeListener)
                    .build()

                focusRequest = request
                audioManager?.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            } else {
                @Suppress("DEPRECATION")
                audioManager?.requestAudioFocus(
                    audioFocusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN,
                ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            }
        }.getOrDefault(false)

        override fun abandonAudioFocus() {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    focusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
                    focusRequest = null
                } else {
                    @Suppress("DEPRECATION")
                    audioManager?.abandonAudioFocus(audioFocusChangeListener)
                }
            }
        }

        override fun prepareAndPlay(soundResId: Int, looping: Boolean): Boolean = runCatching {
            release()
            val player = MediaPlayer.create(context, soundResId) ?: return false
            player.isLooping = looping
            player.setVolume(1.0f, 1.0f)
            player.start()
            mediaPlayer = player
            true
        }.getOrDefault(false)

        override fun pause(): Boolean = runCatching {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.pause()
                    true
                } else false
            } ?: false
        }.getOrDefault(false)

        override fun resume(): Boolean = runCatching {
            mediaPlayer?.let { player ->
                player.start()
                true
            } ?: false
        }.getOrDefault(false)

        override fun stop(): Boolean = runCatching {
            mediaPlayer?.let { player ->
                player.stop()
                player.release()
                mediaPlayer = null
                true
            } ?: false
        }.getOrDefault(false)

        override fun release() {
            runCatching {
                mediaPlayer?.release()
                mediaPlayer = null
            }
            abandonAudioFocus()
        }

        override fun isPlaying(): Boolean = runCatching {
            mediaPlayer?.isPlaying == true
        }.getOrDefault(false)

        override fun setVolume(volume: Float) {
            runCatching {
                mediaPlayer?.setVolume(volume, volume)
            }
        }

        override fun setLooping(looping: Boolean) {
            runCatching {
                mediaPlayer?.isLooping = looping
            }
        }
    }
}
