package kr.ac.kau.llmchat.controller.chat

import kr.ac.kau.llmchat.domain.chat.RatingEnum
import kr.ac.kau.llmchat.domain.chat.RoleEnum
import java.time.Instant

sealed class ChatDto {
    data class GetThreadResponse(
        val id: Long,
        val chatName: String,
        val createdAt: Instant,
        val updatedAt: Instant,
    )

    data class SearchThreadResponse(
        val id: Long,
        val chatName: String,
        val matchHighlight: String,
        val messageId: Long?,
        val messageIdIndex: Long?,
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

    data class GetMessageResponse(
        val id: Long,
        val role: RoleEnum,
        val content: String,
        val isBookmarked: Boolean,
        val createdAt: Instant,
        val updatedAt: Instant,
    )

    data class SseMessageResponse(
        val messageId: Long,
        val role: RoleEnum,
        val content: String,
    )

    data class VoteMessageRequest(
        val rating: RatingEnum,
    )
}
