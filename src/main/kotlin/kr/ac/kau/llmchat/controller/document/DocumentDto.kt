package kr.ac.kau.llmchat.controller.document

import kr.ac.kau.llmchat.domain.document.SecurityLevelEnum
import java.time.Instant

sealed class DocumentDto {
    data class CreateRequest(
        val content: String,
        val securityLevel: SecurityLevelEnum,
    )

    data class Response(
        val id: Long,
        val content: String,
        val securityLevel: SecurityLevelEnum,
        val createdAt: Instant,
        val updatedAt: Instant,
    )
}
