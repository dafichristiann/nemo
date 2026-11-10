package com.anchor.app.core.design.components

import com.anchor.app.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimatedMascotViewTest {

    @Test
    fun angryConfig_matchesSpecification() {
        val config = AnimatedMascotView.getConfig(MascotExpression.ANGRY)
        assertEquals(R.drawable.mascot_angry, config.headRes)
        assertNull(config.fxRes)
        assertEquals(2400L, config.bodyDurationMs)
        assertTrue(config.keyframes.size >= 10)
        assertEquals(0.5f, config.originX)
        assertEquals(0.88f, config.originY)
    }

    @Test
    fun thinkingConfig_matchesSpecification() {
        val config = AnimatedMascotView.getConfig(MascotExpression.THINKING)
        assertEquals(R.drawable.mascot_thinking, config.headRes)
        assertEquals(R.drawable.mascot_thinking_fx, config.fxRes)
        assertEquals(5000L, config.bodyDurationMs)
        assertEquals(2400L, config.fxDurationMs)
        assertTrue(config.keyframes.size >= 4)
    }

    @Test
    fun allExpressions_haveValidConfig() {
        for (expression in MascotExpression.entries) {
            val config = AnimatedMascotView.getConfig(expression)
            assertTrue("headRes should not be 0 for $expression", config.headRes != 0)
            assertTrue("duration must be > 0 for $expression", config.bodyDurationMs > 0L)
            assertTrue("keyframes must not be empty for $expression", config.keyframes.isNotEmpty())
        }
    }

    @Test
    fun interpolate_boundariesAndMidpoints() {
        val keyframes = listOf(
            AnimatedMascotView.Keyframe(0f, x = 0f, y = 0f, rot = 0f, sx = 1f, sy = 1f),
            AnimatedMascotView.Keyframe(0.5f, x = 10f, y = -10f, rot = 20f, sx = 1.2f, sy = 0.8f),
            AnimatedMascotView.Keyframe(1f, x = 0f, y = 0f, rot = 0f, sx = 1f, sy = 1f),
        )
        val linear = AnimatedMascotView.LinearEase

        val start = AnimatedMascotView.interpolate(keyframes, 0f, linear)
        assertEquals(0f, start.x, 0.001f)
        assertEquals(0f, start.rot, 0.001f)

        val mid = AnimatedMascotView.interpolate(keyframes, 0.5f, linear)
        assertEquals(10f, mid.x, 0.001f)
        assertEquals(-10f, mid.y, 0.001f)
        assertEquals(20f, mid.rot, 0.001f)
        assertEquals(1.2f, mid.sx, 0.001f)
        assertEquals(0.8f, mid.sy, 0.001f)

        val quarter = AnimatedMascotView.interpolate(keyframes, 0.25f, linear)
        assertEquals(5f, quarter.x, 0.001f)
        assertEquals(-5f, quarter.y, 0.001f)

        val end = AnimatedMascotView.interpolate(keyframes, 1f, linear)
        assertEquals(0f, end.x, 0.001f)
        assertEquals(0f, end.y, 0.001f)
    }
}
