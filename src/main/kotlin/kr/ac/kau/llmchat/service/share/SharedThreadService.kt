package kr.ac.kau.llmchat.service.share

import kr.ac.kau.llmchat.domain.auth.UserEntity
import kr.ac.kau.llmchat.domain.chat.MessageEntity
import kr.ac.kau.llmchat.domain.chat.MessageRepository
import kr.ac.kau.llmchat.domain.chat.ThreadRepository
import kr.ac.kau.llmchat.domain.share.SharedThreadEntity
import kr.ac.kau.llmchat.domain.share.SharedThreadRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class SharedThreadService(
    private val sharedThreadRepository: SharedThreadRepository,
    private val messageRepository: MessageRepository,
    private val threadRepository: ThreadRepository,
) {
    @Transactional
    fun shareThread(
        threadId: Long,
        messageId: Long,
        user: UserEntity,
    ): SharedThreadEntity {
        val thread =
            threadRepository.findByIdOrNull(threadId)
                ?: throw IllegalArgumentException("Thread not found")
        val message =
            messageRepository.findByIdOrNull(messageId)
                ?: throw IllegalArgumentException("Message not found")
        if (message.thread.id != thread.id) {
            throw IllegalArgumentException("Message does not belong to the thread")
        }
        if (thread.user.id != user.id) {
            throw IllegalArgumentException("User does not have permission to share the thread")
        }

        val sharedKey = UUID.randomUUID().toString()
        val sharedAt = Instant.now()
        val sharedThread =
            SharedThreadEntity(
                user = user,
                thread = thread,
                message = message,
                sharedKey = sharedKey,
                sharedAt = sharedAt,
            )

        return sharedThreadRepository.save(sharedThread)
    }

    @Transactional
    fun getSharedThreads(user: UserEntity): List<SharedThreadEntity> {
        return sharedThreadRepository.findAllByUser(user)
    }

    @Transactional
    fun unshareThread(
        threadId: Long,
        messageId: Long,
        user: UserEntity,
    ) {
        val thread =
            threadRepository.findByIdOrNull(threadId)
                ?: throw IllegalArgumentException("Thread not found")
        val message =
            messageRepository.findByIdOrNull(messageId)
                ?: throw IllegalArgumentException("Message not found")
        if (message.thread.id != thread.id) {
            throw IllegalArgumentException("Message does not belong to the thread")
        }
        if (thread.user.id != user.id) {
            throw IllegalArgumentException("User does not have permission to share the thread")
        }

        val sharedThread =
            sharedThreadRepository.findAllByUserAndThreadAndMessage(user, thread, message)
                ?: throw IllegalArgumentException("Shared thread not found")

        sharedThreadRepository.delete(sharedThread)
    }

    @Transactional
    fun getSharedThread(sharedKey: String): SharedThreadEntity {
        return sharedThreadRepository.findBySharedKey(sharedKey)
            ?: throw IllegalArgumentException("Shared thread not found")
    }

    @Transactional
    fun getSharedThreadMessages(
        sharedKey: String,
        pageable: Pageable,
    ): Page<MessageEntity> {
        val sharedThread =
            sharedThreadRepository.findBySharedKey(sharedKey)
                ?: throw IllegalArgumentException("Shared thread not found")
        val messages = messageRepository.findAllByThread(sharedThread.thread, pageable)
        return messages
    }
}
