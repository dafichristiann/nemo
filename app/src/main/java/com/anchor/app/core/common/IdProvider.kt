package com.anchor.app.core.common

import java.util.UUID

/**
 * Injectable id factory so tests can produce deterministic ids
 * (docs/superpowers/design/11-testing-strategy.md §5). Domain code must never call
 * [UUID.randomUUID] directly.
 */
interface IdProvider {
    fun newId(): String
}

/** Production implementation backed by [UUID.randomUUID]. */
class DefaultIdProvider : IdProvider {
    override fun newId(): String = UUID.randomUUID().toString()
}
