package com.peteraraujo.cronx.features.scheduler

import com.peteraraujo.cronx.db.SchedulePresetMapTable
import com.peteraraujo.cronx.db.SchedulePresetsTable
import com.peteraraujo.cronx.db.ScheduleQueueTable
import com.peteraraujo.cronx.models.PostStatus
import com.peteraraujo.cronx.models.PresetRequest
import com.peteraraujo.cronx.models.SchedulePreset
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.random.Random

object PresetService {

    suspend fun getAllPresets(): List<SchedulePreset> = newSuspendedTransaction {
        SchedulePresetsTable.selectAll().map {
            SchedulePreset(
                id = it[SchedulePresetsTable.id].value,
                name = it[SchedulePresetsTable.name],
                baseTimeUtc = it[SchedulePresetsTable.baseTimeUtc],
                variationMinutes = it[SchedulePresetsTable.variationMinutes],
                colorLight = it[SchedulePresetsTable.colorLight],
                colorDark = it[SchedulePresetsTable.colorDark],
                nextPresetId = it[SchedulePresetsTable.nextPresetId]
            )
        }
    }

    suspend fun createPreset(req: PresetRequest): String = newSuspendedTransaction {
        val newId = UUID.randomUUID().toString()
        SchedulePresetsTable.insert {
            it[id] = newId
            it[name] = req.name
            it[baseTimeUtc] = req.baseTimeUtc
            it[variationMinutes] = req.variationMinutes
            it[colorLight] = req.colorLight
            it[colorDark] = req.colorDark
            it[nextPresetId] = req.nextPresetId
        }
        newId
    }

    suspend fun updatePreset(id: String, req: PresetRequest) = newSuspendedTransaction {
        SchedulePresetsTable.update({ SchedulePresetsTable.id eq id }) {
            it[name] = req.name
            it[baseTimeUtc] = req.baseTimeUtc
            it[variationMinutes] = req.variationMinutes
            it[colorLight] = req.colorLight
            it[colorDark] = req.colorDark
            it[nextPresetId] = req.nextPresetId
        }

        val pendingItems = (ScheduleQueueTable innerJoin SchedulePresetMapTable)
            .selectAll()
            .where { (SchedulePresetMapTable.presetId eq id) and (ScheduleQueueTable.status eq PostStatus.PENDING) }
            .map { it[ScheduleQueueTable.id].value to it[ScheduleQueueTable.scheduledTime] }

        val baseTime = LocalTime.parse(req.baseTimeUtc)

        pendingItems.forEach { (queueId, originalInstant) ->
            val originalDate = originalInstant.atZone(ZoneId.of("UTC")).toLocalDate()

            var newDateTime = originalDate.atTime(baseTime).atZone(ZoneId.of("UTC")).toInstant()

            if (req.variationMinutes > 0) {
                val offset = Random.nextLong(-req.variationMinutes.toLong(), req.variationMinutes.toLong())
                newDateTime = newDateTime.plus(offset, ChronoUnit.MINUTES)
            }

            ScheduleQueueTable.update({ ScheduleQueueTable.id eq queueId }) {
                it[scheduledTime] = newDateTime
            }
        }
    }

    suspend fun deletePreset(id: String) = newSuspendedTransaction {
        SchedulePresetsTable.deleteWhere { SchedulePresetsTable.id eq id }
    }
}
