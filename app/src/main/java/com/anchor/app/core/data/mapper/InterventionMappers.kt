package com.anchor.app.core.data.mapper

import com.anchor.app.core.data.entity.InterventionLogEntity
import com.anchor.app.core.domain.model.InterventionChannel
import com.anchor.app.core.domain.model.InterventionLog
import com.anchor.app.core.domain.model.InterventionStatus

/**
 * Entity ↔ domain mapping for `intervention_log` (docs 03 §5.1). Enums are persisted as
 * `String`; decoding falls back to safe defaults so a malformed row never crashes the app
 * (graceful degradation, docs 02 §6).
 */

fun InterventionLogEntity.toDomain(): InterventionLog = InterventionLog(
    id = id,
    level = level,
    channel = channel.toChannel(),
    contextPackage = contextPackage,
    relatedTaskId = relatedTaskId,
    status = status.toStatus(),
    shownAt = shownAt,
    resolvedAt = resolvedAt,
    cooldownTriggered = cooldownTriggered,
)

fun InterventionLog.toEntity(): InterventionLogEntity = InterventionLogEntity(
    id = id,
    level = level,
    channel = channel.name,
    contextPackage = contextPackage,
    relatedTaskId = relatedTaskId,
    status = status.name,
    shownAt = shownAt,
    resolvedAt = resolvedAt,
    cooldownTriggered = cooldownTriggered,
)

private fun String.toChannel(): InterventionChannel =
    InterventionChannel.entries.firstOrNull { it.name == this } ?: InterventionChannel.NOTIFICATION

private fun String.toStatus(): InterventionStatus =
    InterventionStatus.entries.firstOrNull { it.name == this } ?: InterventionStatus.EXPIRED
