package com.peteraraujo.cronx.features.scheduler

import com.peteraraujo.cronx.db.ApiRateLimitsTable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.time.temporal.ChronoUnit

object RateLimitService {

    suspend fun canPost(endpoint: String, limit: Int, windowMinutes: Int): Boolean = newSuspendedTransaction {
        val now = Instant.now()

        val row = ApiRateLimitsTable.selectAll().where { ApiRateLimitsTable.endpoint eq endpoint }
            .forUpdate()
            .singleOrNull()

        if (row == null) {
            ApiRateLimitsTable.insert {
                it[this.endpoint] = endpoint
                it[this.resetTime] = now.plus(windowMinutes.toLong(), ChronoUnit.MINUTES)
                it[this.remainingHits] = limit - 1
            }
            true
        } else {
            val resetTime = row[ApiRateLimitsTable.resetTime]
            val remaining = row[ApiRateLimitsTable.remainingHits]

            if (now.isAfter(resetTime)) {
                ApiRateLimitsTable.update({ ApiRateLimitsTable.endpoint eq endpoint }) {
                    it[this.resetTime] = now.plus(windowMinutes.toLong(), ChronoUnit.MINUTES)
                    it[this.remainingHits] = limit - 1
                }
                true
            } else {
                if (remaining > 0) {
                    ApiRateLimitsTable.update({ ApiRateLimitsTable.endpoint eq endpoint }) {
                        it[this.remainingHits] = remaining - 1
                    }
                    true
                } else {
                    false
                }
            }
        }
    }
}
