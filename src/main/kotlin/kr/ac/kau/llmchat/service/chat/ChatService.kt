package kr.ac.kau.llmchat.service.chat

import kr.ac.kau.llmchat.domain.auth.UserEntity
import kr.ac.kau.llmchat.domain.chat.ThreadEntity
import kr.ac.kau.llmchat.domain.chat.ThreadRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class ChatService(
    private val threadRepository: ThreadRepository,
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
}
