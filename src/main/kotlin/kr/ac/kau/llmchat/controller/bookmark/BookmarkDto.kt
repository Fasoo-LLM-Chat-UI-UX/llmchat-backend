package kr.ac.kau.llmchat.controller.bookmark

sealed class BookmarkDto {
    data class CreateRequest(
        val messageId: Long,
    )
}
