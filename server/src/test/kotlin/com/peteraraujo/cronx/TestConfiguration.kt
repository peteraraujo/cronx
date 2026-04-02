package com.peteraraujo.cronx

import com.peteraraujo.cronx.db.*
import com.peteraraujo.cronx.features.auth.configureAuthRoutes
import com.peteraraujo.cronx.features.library.configureLibraryRoutes
import com.peteraraujo.cronx.features.scheduler.configurePresetRoutes
import com.peteraraujo.cronx.features.scheduler.configureScheduleRoutes
import com.peteraraujo.cronx.features.settings.configureSettingsRoutes
import com.peteraraujo.cronx.plugins.configureSecurity
import com.peteraraujo.cronx.plugins.configureSerialization
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun configureTestDatabase() {
    val config = HikariConfig().apply {
        driverClassName = "org.h2.Driver"
        jdbcUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=MySQL"
        maximumPoolSize = 3
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        validate()
    }

    val dataSource = HikariDataSource(config)
    Database.connect(dataSource)

    transaction {
        SchemaUtils.drop(
            UserPreferencesTable,
            ContentLibraryTable,
            TagsTable,
            ContentTagsTable,
            ScheduleQueueTable,
            ApiRateLimitsTable,
            SchedulePresetMapTable
        )

        SchemaUtils.create(
            UserPreferencesTable,
            ContentLibraryTable,
            TagsTable,
            ContentTagsTable,
            ScheduleQueueTable,
            ApiRateLimitsTable,
            SchedulePresetMapTable
        )
    }
}

fun Application.configureTest() {
    configureSerialization()
    configureSecurity()
    configureTestDatabase()

    configureAuthRoutes()
    configureLibraryRoutes()
    configureScheduleRoutes()
    configureSettingsRoutes()
    configurePresetRoutes()
}
