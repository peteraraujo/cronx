package com.peteraraujo.cronx.features.library

import com.peteraraujo.cronx.db.ContentLibraryTable
import com.peteraraujo.cronx.db.ContentTagsTable
import com.peteraraujo.cronx.db.TagsTable
import com.peteraraujo.cronx.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.notInList
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.*
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

object LibraryService {

    suspend fun getLibraryPage(
        page: Int,
        pageSize: Int,
        query: String?,
        tags: List<String>?,
        tagMode: TagMode?,
        sort: String?
    ): LibraryPage = newSuspendedTransaction {
        var querySet = ContentLibraryTable.selectAll()

        if (!tags.isNullOrEmpty()) {
            val mode = tagMode ?: TagMode.OR
            val uniqueTags = tags.distinct()

            val matchingIds = if (mode == TagMode.AND) {
                (ContentTagsTable innerJoin TagsTable)
                    .slice(ContentTagsTable.contentId)
                    .selectAll().where { TagsTable.name inList uniqueTags }
                    .groupBy(ContentTagsTable.contentId)
                    .having { ContentTagsTable.contentId.count() eq uniqueTags.size.toLong() }
                    .map { it[ContentTagsTable.contentId] }
            } else {
                (ContentTagsTable innerJoin TagsTable)
                    .selectAll().where { TagsTable.name inList uniqueTags }
                    .map { it[ContentTagsTable.contentId] }
            }

            querySet = if (mode == TagMode.NOT) {
                querySet.adjustWhere { ContentLibraryTable.id notInList matchingIds }
            } else {
                querySet.adjustWhere { ContentLibraryTable.id inList matchingIds }
            }
        }

        val allCandidateRows = querySet.map { row ->
            mapRowToContentItem(row, row[ContentLibraryTable.id].value)
        }

        var filteredItems = allCandidateRows

        if (!query.isNullOrBlank()) {
            val lowerQuery = query.lowercase()
            filteredItems = filteredItems.mapNotNull { item ->
                val nameScore = calculateSimilarity(item.internalName?.lowercase() ?: "", lowerQuery)
                val bodyScore = calculateSimilarity(item.bodyText.lowercase(), lowerQuery)
                val tagScore = item.tags.maxOfOrNull { calculateSimilarity(it.name.lowercase(), lowerQuery) } ?: 0.0

                val totalScore = (nameScore * 2.0) + bodyScore + (tagScore * 1.5)

                if (totalScore > 0.2) item to totalScore else null
            }
                .sortedByDescending { it.second }
                .map { it.first }
        } else {
            filteredItems = when (sort) {
                "OLDEST" -> filteredItems.sortedBy { it.createdAt }
                "MOST_USED" -> filteredItems.sortedByDescending { it.usageCount }
                "LEAST_USED" -> filteredItems.sortedBy { it.usageCount }
                else -> filteredItems.sortedByDescending { it.createdAt }
            }
        }

        val totalCount = filteredItems.size
        val totalPages = if (totalCount == 0) 1 else ceil(totalCount.toDouble() / pageSize).toInt()
        val safePage = max(1, min(page, totalPages))

        val fromIndex = (safePage - 1) * pageSize
        val toIndex = min(fromIndex + pageSize, totalCount)

        val pagedItems = if (fromIndex < totalCount) filteredItems.subList(fromIndex, toIndex) else emptyList()

        LibraryPage(
            items = pagedItems,
            totalCount = totalCount,
            currentPage = safePage,
            totalPages = totalPages
        )
    }

    suspend fun getAllTags(): List<String> = newSuspendedTransaction {
        TagsTable.selectAll().orderBy(TagsTable.name to SortOrder.ASC).map { it[TagsTable.name] }
    }

    private fun calculateSimilarity(s1: String, s2: String): Double {
        if (s1.contains(s2)) return 1.0
        val dist = levenshtein(s1, s2)
        val maxLen = max(s1.length, s2.length)
        return 1.0 - (dist.toDouble() / maxLen)
    }

    private fun levenshtein(lhs: CharSequence, rhs: CharSequence): Int {
        val lhsLength = lhs.length
        val rhsLength = rhs.length
        var cost = Array(lhsLength + 1) { it }
        var newCost = Array(lhsLength + 1) { 0 }

        for (i in 1..rhsLength) {
            newCost[0] = i
            for (j in 1..lhsLength) {
                val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1
                val costReplace = cost[j - 1] + match
                val costInsert = cost[j] + 1
                val costDelete = newCost[j - 1] + 1
                newCost[j] = min(min(costInsert, costDelete), costReplace)
            }
            val swap = cost
            cost = newCost
            newCost = swap
        }
        return cost[lhsLength]
    }

    suspend fun createContent(request: ContentRequest): String = newSuspendedTransaction {
        val newId = UUID.randomUUID().toString()

        ContentLibraryTable.insert {
            it[id] = newId
            it[internalName] = request.internalName
            it[bodyText] = request.bodyText
        }

        updateTags(newId, request.tagIds)
        newId
    }

    suspend fun updateContent(id: String, request: ContentRequest) = newSuspendedTransaction {
        ContentLibraryTable.update({ ContentLibraryTable.id eq id }) {
            it[internalName] = request.internalName
            it[bodyText] = request.bodyText
        }

        ContentTagsTable.deleteWhere { contentId eq id }
        updateTags(id, request.tagIds)
    }

    suspend fun deleteContent(id: String) = newSuspendedTransaction {
        ContentTagsTable.deleteWhere { contentId eq id }
        ContentLibraryTable.deleteWhere { ContentLibraryTable.id eq id }
    }

    private fun updateTags(contentId: String, tags: List<String>) {
        tags.forEach { tagName ->
            val existingTagRow = TagsTable
                .selectAll().where { TagsTable.name eq tagName }
                .singleOrNull()

            val tagUuid = if (existingTagRow != null) {
                existingTagRow[TagsTable.id].value
            } else {
                val newTagId = UUID.randomUUID().toString()
                TagsTable.insert {
                    it[id] = newTagId
                    it[name] = tagName
                }
                newTagId
            }

            ContentTagsTable.insert {
                it[this.contentId] = contentId
                it[tagId] = tagUuid
            }
        }
    }

    private fun mapRowToContentItem(row: ResultRow, contentId: String): ContentItem {
        val tags = (ContentTagsTable innerJoin TagsTable)
            .selectAll().where { ContentTagsTable.contentId eq contentId }
            .map {
                Tag(it[TagsTable.id].value, it[TagsTable.name])
            }

        return ContentItem(
            id = contentId,
            internalName = row[ContentLibraryTable.internalName],
            bodyText = row[ContentLibraryTable.bodyText],
            tags = tags,
            usageCount = row[ContentLibraryTable.usageCount],
            lastUsedAt = row[ContentLibraryTable.lastUsedAt]?.toString(),
            createdAt = row[ContentLibraryTable.createdAt].toString()
        )
    }
}
