package kr.ac.kau.llmchat.service.chat

import kr.ac.kau.llmchat.controller.chat.ChatDto
import kr.ac.kau.llmchat.domain.auth.UserEntity
import kr.ac.kau.llmchat.domain.chat.MessageEntity
import kr.ac.kau.llmchat.domain.chat.MessageRepository
import kr.ac.kau.llmchat.domain.chat.RoleEnum
import kr.ac.kau.llmchat.domain.chat.ThreadEntity
import kr.ac.kau.llmchat.domain.chat.ThreadRepository
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.openai.OpenAiChatClient
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException
import java.time.Instant

@Service
class ChatService(
    private val chatClient: OpenAiChatClient,
    private val threadRepository: ThreadRepository,
    private val messageRepository: MessageRepository,
) {
    fun getThreads(
        user: UserEntity,
        pageable: Pageable,
    ): Page<ThreadEntity> {
        return threadRepository.findAllByUserAndDeletedAtIsNull(user = user, pageable = pageable)
    }

    fun getDeletedThreads(
        user: UserEntity,
        pageable: Pageable,
    ): Page<ThreadEntity> {
        return threadRepository.findAllByUserAndDeletedAtIsNotNull(user = user, pageable = pageable)
    }

    fun createThread(user: UserEntity): ThreadEntity {
        return threadRepository.save(
            ThreadEntity(
                user = user,
                chatName = "New chat",
            ),
        )
    }

    fun autoRenameThread(
        threadId: Long,
        user: UserEntity,
    ): SseEmitter {
        val thread = threadRepository.findByIdOrNull(threadId)
        if (thread == null || thread.user.id != user.id) {
            throw IllegalArgumentException("Thread not found")
        }
        if (thread.deletedAt != null) {
            throw IllegalArgumentException("Thread is deleted")
        }

        val emitter = SseEmitter()

        val systemMessage = "다음의 대화를 10자 내외로 요약해."

        val messages: List<Message> =
            listOf(SystemMessage(systemMessage)) +
                messageRepository
                    .findAllByThread(thread = thread, pageable = Pageable.unpaged())
                    .map {
                        if (it.role == RoleEnum.USER) {
                            UserMessage(it.content)
                        } else {
                            AssistantMessage(it.content)
                        }
                    }
                    .toList()
        val prompt = Prompt(messages)
        val responseFlux = chatClient.stream(prompt)

        val chatMessage = StringBuilder()

        responseFlux.subscribe(
            { chatResponse ->
                try {
                    chatResponse.result.output.content?.let {
                        chatMessage.append(it)
                        emitter.send(
                            SseEmitter.event().data(
                                ChatDto.SseMessageResponse(messageId = -1, role = RoleEnum.ASSISTANT, content = it),
                            ),
                        )
                    }
                } catch (e: IOException) {
                    emitter.completeWithError(e)
                }
            },
            { error ->
                emitter.completeWithError(error)
            },
            {
                emitter.complete()
                thread.chatName = chatMessage.toString()
                threadRepository.save(thread)
            },
        )

        return emitter
    }

    fun manualRenameThread(
        threadId: Long,
        user: UserEntity,
        dto: ChatDto.ManualRenameThreadRequest,
    ) {
        val thread = threadRepository.findByIdOrNull(threadId)
        if (thread == null || thread.user.id != user.id) {
            throw IllegalArgumentException("Thread not found")
        }
        if (thread.deletedAt != null) {
            throw IllegalArgumentException("Thread is deleted")
        }

        thread.chatName = dto.chatName
        threadRepository.save(thread)
    }

    fun getMessages(
        threadId: Long,
        user: UserEntity,
        pageable: Pageable,
    ): Page<MessageEntity> {
        val thread = threadRepository.findByIdOrNull(threadId)
        if (thread == null || thread.user.id != user.id) {
            throw IllegalArgumentException("Thread not found")
        }
        return messageRepository.findAllByThread(thread = thread, pageable = pageable)
    }

    fun sendMessage(
        threadId: Long,
        user: UserEntity,
        dto: ChatDto.SendMessageRequest,
    ): SseEmitter {
        val thread = threadRepository.findByIdOrNull(threadId)
        if (thread == null || thread.user.id != user.id) {
            throw IllegalArgumentException("Thread not found")
        }
        if (thread.deletedAt != null) {
            throw IllegalArgumentException("Thread is deleted")
        }
        val userMessage =
            MessageEntity(
                thread = thread,
                role = RoleEnum.USER,
                content = dto.content,
            )
        messageRepository.save(userMessage)

        val emitter = SseEmitter()
        emitter.send(
            SseEmitter.event().data(
                ChatDto.SseMessageResponse(messageId = userMessage.id, role = userMessage.role, content = dto.content),
            ),
        )
        val messages: List<Message> =
            messageRepository
                .findAllByThread(thread = thread, pageable = Pageable.unpaged())
                .map {
                    if (it.role == RoleEnum.USER) {
                        UserMessage(it.content)
                    } else {
                        AssistantMessage(it.content)
                    }
                }
                .toList()
        val prompt = Prompt(messages)
        val responseFlux = chatClient.stream(prompt)

        val chatMessage = StringBuilder()
        val assistantMessage =
            MessageEntity(
                thread = thread,
                role = RoleEnum.ASSISTANT,
                content = "",
            )
        messageRepository.save(assistantMessage)

        responseFlux.subscribe(
            { chatResponse ->
                try {
                    chatResponse.result.output.content?.let {
                        chatMessage.append(it)
                        emitter.send(
                            SseEmitter.event().data(
                                ChatDto.SseMessageResponse(messageId = assistantMessage.id, role = assistantMessage.role, content = it),
                            ),
                        )
                    }
                } catch (e: IOException) {
                    emitter.completeWithError(e)
                }
            },
            { error ->
                emitter.completeWithError(error)
            },
            {
                emitter.complete()
                assistantMessage.content = chatMessage.toString()
                messageRepository.save(assistantMessage)
            },
        )

        return emitter
    }

    fun softDeleteThread(
        threadId: Long,
        user: UserEntity,
    ) {
        val thread = threadRepository.findByIdOrNull(threadId)
        if (thread == null || thread.user.id != user.id) {
            throw IllegalArgumentException("Thread not found")
        }
        if (thread.deletedAt != null) {
            throw IllegalArgumentException("Thread is already deleted")
        }

        thread.deletedAt = Instant.now()
        threadRepository.save(thread)
    }

    fun hardDeleteThread(
        threadId: Long,
        user: UserEntity,
    ) {
        val thread = threadRepository.findByIdOrNull(threadId)
        if (thread == null || thread.user.id != user.id) {
            throw IllegalArgumentException("Thread not found")
        }
        if (thread.deletedAt == null) {
            throw IllegalArgumentException("Thread is not soft-deleted yet. Soft-delete first.")
        }

        messageRepository.deleteAllByThread(thread)
        threadRepository.delete(thread)
    }

    fun editMessage(
        threadId: Long,
        messageId: Long,
        user: UserEntity,
        dto: ChatDto.SendMessageRequest,
    ): SseEmitter {
        val thread = threadRepository.findByIdOrNull(threadId)
        if (thread == null || thread.user.id != user.id) {
            throw IllegalArgumentException("Thread not found")
        }
        if (thread.deletedAt != null) {
            throw IllegalArgumentException("Thread is deleted")
        }

        val userMessage = messageRepository.findByIdOrNull(messageId)
        if (userMessage == null || userMessage.thread.id != threadId || userMessage.role != RoleEnum.USER) {
            throw IllegalArgumentException("Message not found or not editable")
        }

        // 현재 메시지 ID보다 큰 메시지들을 모두 삭제
        messageRepository.deleteAllByThreadAndIdGreaterThan(thread, messageId)

        // 메시지 내용 업데이트 및 저장
        userMessage.content = dto.content
        messageRepository.save(userMessage)

        // 남아있는 메시지들로 대화 재생성
        val emitter = SseEmitter()
        emitter.send(
            SseEmitter.event().data(
                ChatDto.SseMessageResponse(messageId = userMessage.id, role = userMessage.role, content = dto.content),
            ),
        )
        val messages: List<Message> =
            messageRepository
                .findAllByThread(thread = thread, pageable = Pageable.unpaged())
                .map {
                    if (it.role == RoleEnum.USER) {
                        UserMessage(it.content)
                    } else {
                        AssistantMessage(it.content)
                    }
                }
                .toList()
        val prompt = Prompt(messages)
        val responseFlux = chatClient.stream(prompt)

        val chatMessage = StringBuilder()
        val assistantMessage =
            MessageEntity(
                thread = thread,
                role = RoleEnum.ASSISTANT,
                content = "",
            )
        messageRepository.save(assistantMessage)

        responseFlux.subscribe(
            { chatResponse ->
                try {
                    chatResponse.result.output.content?.let {
                        chatMessage.append(it)
                        emitter.send(
                            SseEmitter.event().data(
                                ChatDto.SseMessageResponse(messageId = assistantMessage.id, role = assistantMessage.role, content = it),
                            ),
                        )
                    }
                } catch (e: IOException) {
                    emitter.completeWithError(e)
                }
            },
            { error ->
                emitter.completeWithError(error)
            },
            {
                emitter.complete()
                assistantMessage.content = chatMessage.toString()
                messageRepository.save(assistantMessage)
            },
        )

        return emitter
    }
}
