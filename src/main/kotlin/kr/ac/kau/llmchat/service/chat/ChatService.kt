package kr.ac.kau.llmchat.service.chat

import kr.ac.kau.llmchat.controller.chat.ChatDto
import kr.ac.kau.llmchat.domain.auth.UserEntity
import kr.ac.kau.llmchat.domain.bookmark.BookmarkRepository
import kr.ac.kau.llmchat.domain.chat.MessageEntity
import kr.ac.kau.llmchat.domain.chat.MessageRepository
import kr.ac.kau.llmchat.domain.chat.RoleEnum
import kr.ac.kau.llmchat.domain.chat.ThreadEntity
import kr.ac.kau.llmchat.domain.chat.ThreadRepository
import kr.ac.kau.llmchat.domain.user.UserPreferenceRepository
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
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException
import java.time.Instant

@Service
class ChatService(
    private val chatClient: OpenAiChatClient,
    private val threadRepository: ThreadRepository,
    private val messageRepository: MessageRepository,
    private val userPreferenceRepository: UserPreferenceRepository,
    private val bookmarkRepository: BookmarkRepository,
) {
    fun getThreads(
        user: UserEntity,
        pageable: Pageable,
        query: String?,
    ): Page<ThreadEntity> {
        return if (query == null) {
            threadRepository.findAllByUserAndDeletedAtIsNull(
                user = user,
                pageable = pageable,
            )
        } else {
            threadRepository.findAllByUserAndChatNameContainsAndDeletedAtIsNull(
                user = user,
                chatName = query,
                pageable = pageable,
            )
        }
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
                    val content: String? = chatResponse.result.output.content
                    if (content?.isNotEmpty() == true) {
                        chatMessage.append(content)
                        emitter.send(
                            SseEmitter.event().data(
                                ChatDto.SseMessageResponse(messageId = -1, role = RoleEnum.ASSISTANT, content = content),
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
    ): Page<ChatDto.GetMessageResponse> {
        val thread = threadRepository.findByIdOrNull(threadId)
        if (thread == null || thread.user.id != user.id) {
            throw IllegalArgumentException("Thread not found")
        }
        val messages = messageRepository.findAllByThread(thread = thread, pageable = pageable)
        val bookmarks =
            bookmarkRepository.findAllById(messages.map { it.id })
                .map { it.message.id }
                .toSet()

        return messages.map { message ->
            ChatDto.GetMessageResponse(
                id = message.id,
                role = message.role,
                content = message.content,
                isBookmarked = message.id in bookmarks,
                createdAt = message.createdAt,
                updatedAt = message.updatedAt,
            )
        }
    }

    fun sendMessage(
        threadId: Long,
        user: UserEntity,
        question: String,
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
                content = question,
                rating = null,
            )
        messageRepository.save(userMessage)

        var systemMessage: String? = null

        val preference = userPreferenceRepository.findByUser(user)
        if (preference != null) {
            if (preference.aboutMessageEnabled && preference.aboutUserMessage?.isNotBlank() == true) {
                systemMessage = "Message about user: ${preference.aboutUserMessage}\n\n"
            }
            if (preference.aboutMessageEnabled && preference.aboutModelMessage?.isNotBlank() == true) {
                systemMessage = "Message to model: ${preference.aboutModelMessage}"
            }
        }

        val emitter = SseEmitter()
        emitter.send(
            SseEmitter.event().data(
                ChatDto.SseMessageResponse(messageId = userMessage.id, role = userMessage.role, content = question),
            ),
        )
        val messages: MutableList<Message> =
            messageRepository
                .findAllByThread(thread = thread, pageable = Pageable.unpaged())
                .map {
                    if (it.role == RoleEnum.USER) {
                        UserMessage(it.content)
                    } else {
                        AssistantMessage(it.content)
                    }
                }
                .toMutableList()
        if (systemMessage != null) {
            messages.add(0, SystemMessage(systemMessage))
        }
        val prompt = Prompt(messages)
        val responseFlux = chatClient.stream(prompt)

        val chatMessage = StringBuilder()
        val assistantMessage =
            MessageEntity(
                thread = thread,
                role = RoleEnum.ASSISTANT,
                content = "",
                rating = null,
            )
        messageRepository.save(assistantMessage)

        responseFlux.subscribe(
            { chatResponse ->
                try {
                    val content: String? = chatResponse.result.output.content
                    if (content?.isNotEmpty() == true) {
                        chatMessage.append(content)
                        emitter.send(
                            SseEmitter.event().data(
                                ChatDto.SseMessageResponse(
                                    messageId = assistantMessage.id,
                                    role = assistantMessage.role,
                                    content = content,
                                ),
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

    @Transactional
    fun softDeleteAllThread(user: UserEntity) {
        val threads = threadRepository.findAllByUserAndDeletedAtIsNull(user = user, pageable = Pageable.unpaged())
        threads.forEach {
            it.deletedAt = Instant.now()
        }
        threadRepository.saveAll(threads)
    }

    fun restoreThread(
        threadId: Long,
        user: UserEntity,
    ) {
        val thread = threadRepository.findByIdOrNull(threadId)
        if (thread == null || thread.user.id != user.id) {
            throw IllegalArgumentException("Thread not found")
        }
        if (thread.deletedAt == null) {
            throw IllegalArgumentException("Thread is not deleted yet")
        }

        thread.deletedAt = null
        threadRepository.save(thread)
    }

    @Transactional
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

    @Transactional
    fun editMessage(
        threadId: Long,
        messageId: Long,
        user: UserEntity,
        question: String,
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
        userMessage.content = question
        messageRepository.save(userMessage)

        var systemMessage: String? = null

        val preference = userPreferenceRepository.findByUser(user)
        if (preference != null) {
            if (preference.aboutMessageEnabled && preference.aboutUserMessage?.isNotBlank() == true) {
                systemMessage = "Message about user: ${preference.aboutUserMessage}\n\n"
            }
            if (preference.aboutMessageEnabled && preference.aboutModelMessage?.isNotBlank() == true) {
                systemMessage = "Message to model: ${preference.aboutModelMessage}"
            }
        }

        // 남아있는 메시지들로 대화 재생성
        val emitter = SseEmitter()
        emitter.send(
            SseEmitter.event().data(
                ChatDto.SseMessageResponse(messageId = userMessage.id, role = userMessage.role, content = question),
            ),
        )
        val messages: MutableList<Message> =
            messageRepository
                .findAllByThread(thread = thread, pageable = Pageable.unpaged())
                .map {
                    if (it.role == RoleEnum.USER) {
                        UserMessage(it.content)
                    } else {
                        AssistantMessage(it.content)
                    }
                }
                .toMutableList()
        if (systemMessage != null) {
            messages.add(0, SystemMessage(systemMessage))
        }
        val prompt = Prompt(messages)
        val responseFlux = chatClient.stream(prompt)

        val chatMessage = StringBuilder()
        val assistantMessage =
            MessageEntity(
                thread = thread,
                role = RoleEnum.ASSISTANT,
                content = "",
                rating = null,
            )
        messageRepository.save(assistantMessage)

        responseFlux.subscribe(
            { chatResponse ->
                try {
                    val content: String? = chatResponse.result.output.content
                    if (content?.isNotEmpty() == true) {
                        chatMessage.append(content)
                        emitter.send(
                            SseEmitter.event().data(
                                ChatDto.SseMessageResponse(
                                    messageId = assistantMessage.id,
                                    role = assistantMessage.role,
                                    content = content,
                                ),
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

    fun voteMessage(
        threadId: Long,
        messageId: Long,
        user: UserEntity,
        dto: ChatDto.VoteMessageRequest,
    ) {
        val thread = threadRepository.findByIdOrNull(threadId)
        if (thread == null || thread.user.id != user.id) {
            throw IllegalArgumentException("Thread not found")
        }
        if (thread.deletedAt != null) {
            throw IllegalArgumentException("Thread is deleted")
        }

        val message = messageRepository.findByIdOrNull(messageId)
        if (message == null || message.thread.id != threadId) {
            throw IllegalArgumentException("Message not found")
        }

        if (message.role != RoleEnum.ASSISTANT) {
            throw IllegalArgumentException("Only assistant message can be rated")
        }

        message.rating = dto.rating
        messageRepository.save(message)
    }

    @Transactional
    fun searchThreads(
        user: UserEntity,
        query: String,
    ): List<ChatDto.SearchThreadResponse> {
        val threads = threadRepository.findAllByUserAndDeletedAtIsNotNull(user = user, pageable = Pageable.unpaged())
        return threads.mapNotNull { thread ->
            val searchContent = StringBuilder()

            searchContent.append(thread.chatName)
            searchContent.append(" ")

            thread.messages.forEach { message ->
                searchContent.append(message.content)
                searchContent.append(" ")
            }

            val contentString = searchContent.toString()
            val queryIndex = contentString.indexOf(query, ignoreCase = true)

            if (queryIndex != -1) {
                val start = maxOf(0, queryIndex - 50)
                val end = minOf(contentString.length, queryIndex + query.length + 50)

                // 하이라이트 부분 추출
                val highlightSnippet =
                    contentString
                        .substring(start, end)
                        .replace(query, "<b>$query</b>", ignoreCase = true)

                ChatDto.SearchThreadResponse(
                    id = thread.id,
                    chatName = thread.chatName,
                    matchHighlight = highlightSnippet,
                    createdAt = thread.createdAt,
                    updatedAt = thread.updatedAt,
                )
            } else {
                null
            }
        }
    }
}
