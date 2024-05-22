package kr.ac.kau.llmchat.controller.chat

import kr.ac.kau.llmchat.domain.chat.RoleEnum
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

    data class ManualRenameThreadRequest(
        val chatName: String,
    )

    data class SendMessageRequest(
        val content: String,
    )

    data class GetMessageResponse(
        val id: Long,
        val role: RoleEnum,
        val content: String,
        val createdAt: Instant,
        val updatedAt: Instant,
    )

    data class SseMessageResponse(
        val messageId: Long,
        val content: String,
    )
}
