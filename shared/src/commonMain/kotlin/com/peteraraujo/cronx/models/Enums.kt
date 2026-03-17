package com.peteraraujo.cronx.models

import kotlinx.serialization.Serializable

@Serializable
enum class PostStatus {
    DRAFT,
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
}

enum class TagMode {
    OR,
    AND,
    NOT
}
