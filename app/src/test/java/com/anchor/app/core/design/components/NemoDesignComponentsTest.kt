package com.anchor.app.core.design.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class NemoDesignComponentsTest {

    @Test
    fun mascotExpression_has12Expressions() {
        val expressions = MascotExpression.entries
        assertEquals(12, expressions.size)
        assertNotNull(MascotExpression.valueOf("NEUTRAL"))
        assertNotNull(MascotExpression.valueOf("HAPPY"))
        assertNotNull(MascotExpression.valueOf("SAD"))
        assertNotNull(MascotExpression.valueOf("ANGRY"))
        assertNotNull(MascotExpression.valueOf("SURPRISED"))
        assertNotNull(MascotExpression.valueOf("SLEEPY"))
        assertNotNull(MascotExpression.valueOf("CONFUSED"))
        assertNotNull(MascotExpression.valueOf("LAUGHING"))
        assertNotNull(MascotExpression.valueOf("SHY"))
        assertNotNull(MascotExpression.valueOf("THINKING"))
        assertNotNull(MascotExpression.valueOf("WINK"))
        assertNotNull(MascotExpression.valueOf("TALKING"))
    }

    @Test
    fun mascotExpression_namesMatchSpecification() {
        val expected = setOf(
            "NEUTRAL", "HAPPY", "SAD", "ANGRY", "SURPRISED", "SLEEPY",
            "CONFUSED", "LAUGHING", "SHY", "THINKING", "WINK", "TALKING",
        )
        val actual = MascotExpression.entries.map { it.name }.toSet()
        assertEquals(expected, actual)
    }

    @Test
    fun buttonStyle_hasExpectedVariants() {
        val styles = NemoButtonStyle.entries
        assertEquals(5, styles.size)
        assertNotNull(NemoButtonStyle.valueOf("Primary"))
        assertNotNull(NemoButtonStyle.valueOf("Secondary"))
        assertNotNull(NemoButtonStyle.valueOf("Outline"))
        assertNotNull(NemoButtonStyle.valueOf("Danger"))
        assertNotNull(NemoButtonStyle.valueOf("Text"))
    }
}
