package com.anchor.app.platform.accessibility

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the framework-free service-list matcher behind [AccessibilityStatus.isEnabled].
 * Covers the OEM/short-form spellings that made blocking look "not detected" on real devices.
 */
class AccessibilityStatusListTest {

    private val full = "com.anchor.app/com.anchor.app.platform.accessibility.NemoAccessibilityService"
    private val short = "com.anchor.app/.platform.accessibility.NemoAccessibilityService"

    @Test
    fun fullForm_present_isListed() {
        assertTrue(
            AccessibilityStatus.isServiceListed(
                enabledServices = "com.other/com.other.Svc:$full",
                expectedFull = full,
                expectedShort = short,
            ),
        )
    }

    @Test
    fun shortForm_present_matchesFullExpectation() {
        // The OS stored the short form; expectation is the full form — still a match.
        assertTrue(
            AccessibilityStatus.isServiceListed(
                enabledServices = "com.other/com.other.Svc:$short",
                expectedFull = full,
                expectedShort = short,
            ),
        )
    }

    @Test
    fun onlySingleEntry_isListed() {
        assertTrue(
            AccessibilityStatus.isServiceListed(
                enabledServices = full,
                expectedFull = full,
                expectedShort = short,
            ),
        )
    }

    @Test
    fun otherServicesOnly_isNotListed() {
        assertFalse(
            AccessibilityStatus.isServiceListed(
                enabledServices = "com.other/com.other.Svc:com.two/com.two.Svc",
                expectedFull = full,
                expectedShort = short,
            ),
        )
    }

    @Test
    fun blank_isNotListed() {
        assertFalse(
            AccessibilityStatus.isServiceListed(
                enabledServices = "",
                expectedFull = full,
                expectedShort = short,
            ),
        )
    }
}
