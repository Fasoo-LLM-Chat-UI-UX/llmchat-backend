package kr.ac.kau.llmchat.controller.share

import kr.ac.kau.llmchat.domain.chat.RoleEnum
import java.time.Instant

sealed class SharedThreadDto {
    data class ShareThreadResponse(
        val sharedKey: String,
        val sharedAt: Instant,
    ) : SharedThreadDto()

    data class GetSharedThreadResponse(
        val id: Long,
        val threadId: Long,
        val threadName: String,
        val messageId: Long,
        val messageContent: String,
        val sharedKey: String,
        val sharedAt: Instant,
    )

    data class GetSharedThreadMessageResponse(
        val id: Long,
        val role: RoleEnum,
        val content: String,
        val createdAt: Instant,
        val updatedAt: Instant,
    )
}
