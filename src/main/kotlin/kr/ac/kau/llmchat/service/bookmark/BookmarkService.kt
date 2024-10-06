package kr.ac.kau.llmchat.service.bookmark

import jakarta.transaction.Transactional
import kr.ac.kau.llmchat.controller.bookmark.BookmarkDto
import kr.ac.kau.llmchat.domain.auth.UserEntity
import kr.ac.kau.llmchat.domain.bookmark.BookmarkEntity
import kr.ac.kau.llmchat.domain.bookmark.BookmarkRepository
import kr.ac.kau.llmchat.domain.chat.MessageEntity
import kr.ac.kau.llmchat.domain.chat.MessageRepository
import kr.ac.kau.llmchat.domain.chat.RoleEnum
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class BookmarkService(
    private val chatModel: OpenAiChatModel,
    private val bookmarkRepository: BookmarkRepository,
    private val messageRepository: MessageRepository,
) {
    @Async
    @Transactional
    fun create(
        user: UserEntity,
        dto: BookmarkDto.CreateRequest,
    ): BookmarkEntity {
        val assistantMessage = messageRepository.findByIdOrNull(dto.messageId)
        if (assistantMessage == null || assistantMessage.thread.user.id != user.id) {
            throw IllegalArgumentException("Invalid message")
        }
        if (assistantMessage.role != RoleEnum.ASSISTANT) {
            throw IllegalArgumentException("Only assistant message can be bookmarked")
        }

        val alreadyExists = bookmarkRepository.existsByMessage(assistantMessage)
        if (alreadyExists) {
            throw IllegalArgumentException("Bookmark already exists")
        }

        val userMessage =
            messageRepository.findUserMessage(
                thread = assistantMessage.thread,
                messageId = assistantMessage.id,
            ) ?: throw IllegalArgumentException("User message not found")

        val bookmarkEntity =
            BookmarkEntity(
                user = user,
                message = assistantMessage,
                userMessage = userMessage.content,
                assistantMessage = assistantMessage.content,
                title = summarize(userMessage, assistantMessage),
                emoji = emoji(userMessage, assistantMessage),
            )
        bookmarkRepository.save(bookmarkEntity)
        return bookmarkEntity
    }

    private fun summarize(
        userMessage: MessageEntity,
        assistantMessage: MessageEntity,
    ): String {
        val messages: List<Message> =
            listOf(
                SystemMessage("다음의 대화를 30자 내외로 요약해."),
                UserMessage(userMessage.content),
                AssistantMessage(assistantMessage.content),
            )
        val prompt = Prompt(messages)
        val response = chatModel.call(prompt)
        return response.result.output.content
    }

    private fun emoji(
        userMessage: MessageEntity,
        assistantMessage: MessageEntity,
    ): String {
        val messages: List<Message> =
            listOf(
                SystemMessage("다음의 대화에 어울리는 이모지를 1개만 출력해."),
                UserMessage(userMessage.content),
                AssistantMessage(assistantMessage.content),
            )
        val prompt = Prompt(messages)
        val response = chatModel.call(prompt)
        return response.result.output.content
    }

    fun getAllBookmarks(
        user: UserEntity,
        pageable: Pageable,
    ): Page<BookmarkEntity> {
        return bookmarkRepository.findAllByUser(user, pageable)
    }

    fun getBookmark(
        user: UserEntity,
        id: Long,
    ): BookmarkEntity {
        val bookmark = bookmarkRepository.findByIdOrNull(id)

        if (bookmark == null || bookmark.user.id != user.id) {
            throw IllegalArgumentException("Bookmark not found")
        }

        return bookmark
    }

    fun updateBookmark(
        user: UserEntity,
        id: Long,
        dto: BookmarkDto.UpdateRequest,
    ): BookmarkEntity {
        val bookmark = bookmarkRepository.findByIdOrNull(id)

        if (bookmark == null || bookmark.user.id != user.id) {
            throw IllegalArgumentException("Bookmark not found")
        }

        if (dto.title != null) {
            bookmark.title = dto.title
        }

        if (dto.emoji != null) {
            bookmark.emoji = dto.emoji
        }

        bookmarkRepository.save(bookmark)
        return bookmark
    }

    fun deleteBookmark(
        user: UserEntity,
        id: Long,
    ) {
        val bookmark = bookmarkRepository.findByIdOrNull(id)

        if (bookmark == null || bookmark.user.id != user.id) {
            throw IllegalArgumentException("Bookmark not found")
        }

        bookmarkRepository.delete(bookmark)
    }
}
