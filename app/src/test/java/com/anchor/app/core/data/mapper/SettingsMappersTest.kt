package com.anchor.app.core.data.mapper

import com.anchor.app.core.data.entity.AppSettingsEntity
import com.anchor.app.core.domain.model.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsMappersTest {

    @Test
    fun entityToDomain_mapsDoomscrollAlertFieldsCorrectly() {
        val entity = AppSettingsEntity(
            doomscrollAlertEnabled = true,
            doomscrollThresholdMinutes = 30,
        )

        val domain = entity.toDomain()

        assertTrue(domain.doomscrollAlertEnabled)
        assertEquals(30, domain.doomscrollThresholdMinutes)
    }

    @Test
    fun domainToEntity_mapsDoomscrollAlertFieldsCorrectly() {
        val domain = AppSettings(
            doomscrollAlertEnabled = true,
            doomscrollThresholdMinutes = 45,
        )

        val entity = domain.toEntity()

        assertTrue(entity.doomscrollAlertEnabled)
        assertEquals(45, entity.doomscrollThresholdMinutes)
    }

    @Test
    fun defaultValues_alignBetweenDomainAndEntity() {
        val defaultEntity = AppSettingsEntity()
        val defaultDomain = AppSettings()

        assertFalse(defaultEntity.doomscrollAlertEnabled)
        assertFalse(defaultDomain.doomscrollAlertEnabled)
        assertEquals(20, defaultEntity.doomscrollThresholdMinutes)
        assertEquals(20, defaultDomain.doomscrollThresholdMinutes)
    }
}
