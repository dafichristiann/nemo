package com.anchor.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point.
 *
 * Hilt generates the [dagger.hilt.android.HiltAndroidApp] component from here, which
 * wires the [com.anchor.app.core.di] modules. Anchor is local-first with no backend —
 * nothing is initialised here that requires network access.
 */
@HiltAndroidApp
class AnchorApp : Application()
