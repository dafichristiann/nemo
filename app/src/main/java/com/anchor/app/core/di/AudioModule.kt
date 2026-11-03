package com.anchor.app.core.di

import com.anchor.app.platform.audio.AndroidSoundscapePlayer
import com.anchor.app.platform.audio.SoundscapePlayer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AudioModule {

    @Binds
    @Singleton
    abstract fun bindSoundscapePlayer(impl: AndroidSoundscapePlayer): SoundscapePlayer
}
