package com.peteraraujo.cronx.db

import com.peteraraujo.cronx.models.PostStatus
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

open class StringIdTable(name: String, columnName: String = "id") : IdTable<String>(name) {
    override val id: Column<EntityID<String>> = varchar(columnName, 36).entityId()
    override val primaryKey = PrimaryKey(id)
}

object UserPreferencesTable : Table("user_preferences") {
    val prefKey = varchar("pref_key", 50)
    val prefValue = text("pref_value")
    override val primaryKey = PrimaryKey(prefKey)
}

object ContentLibraryTable : StringIdTable("content_library") {
    val internalName = varchar("internal_name", 100).nullable()
    val bodyText = text("body_text")
    val usageCount = integer("usage_count").default(0)
    val lastUsedAt = timestamp("last_used_at").nullable()
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
}

object TagsTable : StringIdTable("tags") {
    val name = varchar("name", 50).uniqueIndex()
}

object ContentTagsTable : Table("content_tags") {
    val contentId = reference("content_id", ContentLibraryTable)
    val tagId = reference("tag_id", TagsTable)
    override val primaryKey = PrimaryKey(contentId, tagId)
}

object SchedulePresetsTable : StringIdTable("schedule_presets") {
    val name = varchar("name", 100)
    val baseTimeUtc = varchar("base_time_utc", 8)
    val variationMinutes = integer("variation_minutes").default(0)
    val colorLight = varchar("color_light", 7).default("#000000")
    val colorDark = varchar("color_dark", 7).default("#FFFFFF")
    val nextPresetId = varchar("next_preset_id", 36).nullable()
}

object ScheduleQueueTable : StringIdTable("schedule_queue") {
    val internalName = varchar("internal_name", 100).nullable()
    val bodyText = text("body_text")
    val status = enumerationByName("status", 20, PostStatus::class).default(PostStatus.PENDING)
    val scheduledTime = timestamp("scheduled_time")
    val remotePostId = varchar("remote_post_id", 255).nullable()
    val errorLog = text("error_log").nullable()
    val retryCount = integer("retry_count").default(0)
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
}

object SchedulePresetMapTable : Table("schedule_preset_map") {
    val scheduleId = reference("schedule_id", ScheduleQueueTable)
    val presetId = reference("preset_id", SchedulePresetsTable)
    override val primaryKey = PrimaryKey(scheduleId, presetId)
}

object ApiRateLimitsTable : Table("api_rate_limits") {
    val endpoint = varchar("endpoint", 255)
    val resetTime = timestamp("reset_time")
    val remainingHits = integer("remaining_hits")
    override val primaryKey = PrimaryKey(endpoint)
}
