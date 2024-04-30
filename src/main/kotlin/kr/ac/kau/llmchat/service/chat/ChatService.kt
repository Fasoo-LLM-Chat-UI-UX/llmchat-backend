package kr.ac.kau.llmchat.service.chat

import kr.ac.kau.llmchat.controller.chat.ChatDto
import kr.ac.kau.llmchat.domain.auth.UserEntity
import kr.ac.kau.llmchat.domain.chat.MessageEntity
import kr.ac.kau.llmchat.domain.chat.MessageRepository
import kr.ac.kau.llmchat.domain.chat.RoleEnum
import kr.ac.kau.llmchat.domain.chat.ThreadEntity
import kr.ac.kau.llmchat.domain.chat.ThreadRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class ChatService(
    private val threadRepository: ThreadRepository,
    private val messageRepository: MessageRepository,
) {
    fun getThreads(
        user: UserEntity,
        pageable: Pageable,
    ): Page<ThreadEntity> {
        return threadRepository.findAllByUser(user = user, pageable = pageable)
    }

    fun createThread(user: UserEntity): ThreadEntity {
        return threadRepository.save(
            ThreadEntity(
                user = user,
                chatName = "New chat",
            ),
        )
    }

    fun sendMessage(
        threadId: Long,
        user: UserEntity,
        dto: ChatDto.SendMessageRequest,
    ) {
        val thread = threadRepository.findByIdOrNull(threadId)
        if (thread == null || thread.user.id != user.id) {
            throw IllegalArgumentException("Thread not found")
        }
        messageRepository.save(
            MessageEntity(
                thread = thread,
                role = RoleEnum.USER,
                content = dto.content,
            ),
        )
    }
}
