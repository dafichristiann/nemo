package com.anchor.app.core.domain.model

/**
 * Focus session mode (docs/superpowers/design/03-database-schema.md §4.1, PRD §10.5).
 *
 *  - [GENTLE] only reduces interruptions — never blocks anything;
 *  - [STRICT] may temporarily limit selected distraction apps;
 *  - [CUSTOM] lets the user pick which apps are limited.
 *
 * Used by [com.anchor.app.core.domain.blocking.BlockingStrategy]. Stored as `String` in Room.
 */
enum class FocusMode { GENTLE, STRICT, CUSTOM }
