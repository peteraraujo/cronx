package com.peteraraujo.cronx.models

import kotlinx.serialization.Serializable

@Serializable
data class ContentItem(
    val id: String,
    val internalName: String?,
    val bodyText: String,
    val tags: List<Tag> = emptyList(),
    val usageCount: Int = 0,
    val lastUsedAt: String? = null,
    val createdAt: String
)

@Serializable
data class Tag(val id: String, val name: String)

@Serializable
data class LibraryPage(
    val items: List<ContentItem>,
    val totalCount: Int,
    val currentPage: Int,
    val totalPages: Int
)
