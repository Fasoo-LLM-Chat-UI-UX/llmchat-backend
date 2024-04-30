package kr.ac.kau.llmchat.controller.chat

import java.time.Instant

sealed class ChatDto {
    data class GetThreadResponse(
        val id: Long,
        val chatName: String,
        val createdAt: Instant,
        val updatedAt: Instant,
    )

    data class CreateThreadResponse(
        val id: Long,
        val chatName: String,
        val createdAt: Instant,
        val updatedAt: Instant,
    )
}
