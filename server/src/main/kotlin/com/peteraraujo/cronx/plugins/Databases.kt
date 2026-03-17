package com.peteraraujo.cronx.plugins

import com.peteraraujo.cronx.Configuration
import com.peteraraujo.cronx.db.*
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun configureDatabase() {
    val driverClass = "com.mysql.cj.jdbc.Driver"

    val url = Configuration.dbUrlVar.get()
    val port = Configuration.dbPortVar.get()
    val name = Configuration.dbNameVar.get()
    val dbUser = Configuration.dbUserVar.get()
    val dbPassword = Configuration.dbPasswordVar.get()

    val dbUrl = "jdbc:mysql://$url:$port/$name?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC"

    val hikariConfig = HikariConfig().apply {
        driverClassName = driverClass
        jdbcUrl = dbUrl

        username = dbUser
        password = dbPassword
        maximumPoolSize = 10
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        validate()
    }

    val dataSource = HikariDataSource(hikariConfig)
    Database.connect(dataSource)

    transaction {
        SchemaUtils.create(
            UserPreferencesTable,
            ContentLibraryTable,
            TagsTable,
            ContentTagsTable,
            ScheduleQueueTable,
        )
    }
}
