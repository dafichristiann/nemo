package com.anchor.app.core.common

/** Deterministic id provider for tests (docs 11-testing-strategy.md §5). */
class FakeIdProvider(private val prefix: String = "id") : IdProvider {
    private var counter = 0
    override fun newId(): String = "$prefix-${counter++}"
}
