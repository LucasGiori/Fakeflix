package com.fakeflix

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("video")
data class Video(
    @Id
    val id: Long? = null,
    val title: String,
    val description: String? = null,
    val url: String,
    val sizeInKb: Int? = null,
    val duration: Int? = null,
    val thumbnailUrl: String? = null,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null
)
