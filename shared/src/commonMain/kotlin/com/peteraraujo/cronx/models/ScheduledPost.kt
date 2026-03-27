package com.peteraraujo.cronx.models

import kotlinx.serialization.Serializable

@Serializable
data class ScheduledPost(
    val id: String,
    val internalName: String? = null,
    val bodyText: String,
    val status: PostStatus,
    val scheduledTime: String,
    val errorLog: String? = null,
    val retryCount: Int = 0,
    val presetColorLight: String? = null,
    val presetColorDark: String? = null,
    val presetId: String? = null
)
