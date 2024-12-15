package kr.ac.kau.llmchat.controller.bookmark

import java.time.Instant

sealed class BookmarkDto {
    data class CreateRequest(
        val messageId: Long,
    )

    data class BookmarkResponse(
        val id: Long,
        val title: String,
        val emoji: String,
        val userMessage: String,
        val assistantMessage: String,
        val createdAt: Instant,
    )

    data class UpdateRequest(
        val title: String?,
        val emoji: String?,
    )
}
