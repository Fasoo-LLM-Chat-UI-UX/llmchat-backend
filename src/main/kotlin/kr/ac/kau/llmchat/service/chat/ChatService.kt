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
import kr.ac.kau.llmchat.service.document.DocumentService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.document.Document
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Service
class ChatService(
    private val chatModel: OpenAiChatModel,
    private val threadRepository: ThreadRepository,
    private val messageRepository: MessageRepository,
    private val userPreferenceRepository: UserPreferenceRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val documentService: DocumentService,
    private val threadPoolTaskExecutor: ThreadPoolTaskExecutor,
) {
    private val logger: Logger = LoggerFactory.getLogger(ChatService::class.java)

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
        val responseFlux = chatModel.stream(prompt)

        val chatMessage = StringBuilder()

        responseFlux.subscribe(
            { chatResponse ->
                try {
                    val content: String? = chatResponse.result.output.content
                    if (content?.isNotEmpty() == true) {
                        chatMessage.append(content)
                        emitter.send(
                            SseEmitter.event().data(
                                ChatDto.SseMessageResponse(
                                    messageId = -1,
                                    role = RoleEnum.ASSISTANT,
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
        val thread = validateThread(threadId, user)

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

    private fun validateThread(
        threadId: Long,
        user: UserEntity,
    ): ThreadEntity {
        val thread = threadRepository.findByIdOrNull(threadId)
        if (thread == null || thread.user.id != user.id) {
            throw IllegalArgumentException("Thread not found")
        }
        if (thread.deletedAt != null) {
            throw IllegalArgumentException("Thread is deleted")
        }
        return thread
    }

    private fun createUserMessage(
        thread: ThreadEntity,
        content: String,
    ): MessageEntity {
        val userMessage =
            MessageEntity(
                thread = thread,
                role = RoleEnum.USER,
                content = content,
                rating = null,
            )
        return messageRepository.save(userMessage)
    }

    private fun getSystemMessage(user: UserEntity): String? {
        val preference = userPreferenceRepository.findByUser(user)
        if (preference != null) {
            val aboutUser =
                if (preference.aboutMessageEnabled && preference.aboutUserMessage?.isNotBlank() == true) {
                    "Message about user: ${preference.aboutUserMessage}\n\n"
                } else {
                    ""
                }

            val aboutModel =
                if (preference.aboutMessageEnabled && preference.aboutModelMessage?.isNotBlank() == true) {
                    "Message to model: ${preference.aboutModelMessage}"
                } else {
                    ""
                }

            return (aboutUser + aboutModel).takeIf { it.isNotBlank() }
        }
        return null
    }

    private fun createAssistantMessage(thread: ThreadEntity): MessageEntity {
        val assistantMessage =
            MessageEntity(
                thread = thread,
                role = RoleEnum.ASSISTANT,
                content = "",
                rating = null,
            )
        return messageRepository.save(assistantMessage)
    }

    private fun getInitialMessages(
        thread: ThreadEntity,
        systemMessage: String?,
    ): MutableList<Message> {
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
        return messages
    }

    private fun shouldPerformWebSearch(
        question: String,
        relevantDocs: List<Document>,
    ): Boolean {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.of("Asia/Seoul"))
        val todayDate = formatter.format(Instant.now())

        // 문서 내용 요약
        val docSummary =
            relevantDocs.joinToString(",\n") {
                "<Document start>\n${it.content.take(500)}...\n<Document end>"
            }

        val checkSearchPrompt =
            Prompt(
                listOf(
                    SystemMessage(
                        """
                        You are an AI assistant tasked with deciding if a web search is needed to answer the user's question.
                        Today's date is $todayDate. Consider:
                        1. Your knowledge is up to date as of October 2023.
                        2. Does the question require up-to-date information?
                        3. Could general knowledge or facts online provide a sufficient answer?
                        4. Is the question specific enough to yield useful search results?
                        Relevant documents are:
                        ${if (relevantDocs.isEmpty()) "(No relevant documents found)" else docSummary}
                        
                        Respond with 'true' if a web search is needed, otherwise 'false'.
                        """.trimIndent(),
                    ),
                    UserMessage(question),
                ),
            )

        return try {
            chatModel.call(checkSearchPrompt).result.output.content.trim().equals("true", ignoreCase = true)
        } catch (e: Exception) {
            logger.error("Error while determining web search need", e)
            false
        }
    }

    private fun searchRelevantDocuments(
        emitter: SseEmitter,
        assistantMessage: MessageEntity,
        user: UserEntity,
        question: String,
        messages: MutableList<Message>,
    ): List<Document> {
        val relevantDocs =
            documentService.searchSimilarDocuments(
                user = user,
                query = question,
                threshold = 0.5,
                topK = 5,
            )

        if (relevantDocs.isNotEmpty()) {
            val context =
                buildString {
                    appendLine("Here's relevant information from our knowledge base:")
                    appendLine()
                    relevantDocs.forEachIndexed { index: Int, doc: Document ->
                        appendLine("<Document start>")
                        appendLine(doc.content)
                        appendLine("<Document end>")
                        appendLine()
                    }
                    appendLine(
                        "Please refer to this information to assist in answering the question, and if referenced, " +
                            "include the specific details at the end of your response. For example:",
                    )
                    appendLine()
                    appendLine("위 답변은 다음의 내부 문서를 참고했습니다:\n- \"참고한 문장 1.\"\n- \"참고한 문장 2.\"")
                }
            messages.add(SystemMessage(context))
        }
        return relevantDocs
    }

    private fun performWebSearch(
        emitter: SseEmitter,
        assistantMessage: MessageEntity,
        question: String,
        messages: MutableList<Message>,
    ) {
        // TODO: Implement web search
    }

    private fun handleChatResponse(
        emitter: SseEmitter,
        assistantMessage: MessageEntity,
        chatMessage: StringBuilder,
        chatResponse: ChatResponse,
    ) {
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
    }

    fun sendMessage(
        threadId: Long,
        user: UserEntity,
        question: String,
    ): SseEmitter {
        val thread: ThreadEntity = validateThread(threadId, user)
        val userMessage: MessageEntity = createUserMessage(thread, question)
        val systemMessage: String? = getSystemMessage(user)

        val emitter = SseEmitter()
        val messages: MutableList<Message> = getInitialMessages(thread, systemMessage)
        val assistantMessage: MessageEntity = createAssistantMessage(thread)
        val chatMessage = StringBuilder()

        threadPoolTaskExecutor.run {
            try {
                emitter.send(
                    SseEmitter.event().data(
                        ChatDto.SseMessageResponse(messageId = userMessage.id, role = userMessage.role, content = question),
                    ),
                )

                // Search for relevant documents and perform web search if needed
                val relevantDocs = searchRelevantDocuments(emitter, assistantMessage, user, question, messages)
                if (shouldPerformWebSearch(question, relevantDocs)) {
                    performWebSearch(emitter, assistantMessage, question, messages)
                }

                val prompt = Prompt(messages)
                val responseFlux = chatModel.stream(prompt)

                responseFlux.subscribe(
                    { chatResponse ->
                        handleChatResponse(emitter, assistantMessage, chatMessage, chatResponse)
                    },
                    { error ->
                        logger.error("채팅 응답 처리 중 오류 발생", error)
                        emitter.send(
                            SseEmitter.event().data(
                                ChatDto.SseMessageResponse(
                                    messageId = assistantMessage.id,
                                    role = assistantMessage.role,
                                    content = "죄송합니다. 응답을 생성하는 중에 오류가 발생했습니다.",
                                ),
                            ),
                        )
                        emitter.completeWithError(error)
                    },
                    {
                        emitter.complete()
                        assistantMessage.content = chatMessage.toString()
                        messageRepository.save(assistantMessage)
                    },
                )
            } catch (e: Exception) {
                logger.error("메시지 처리 중 오류 발생", e)
                emitter.completeWithError(e)
                throw e
            }
        }

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
        val thread = validateThread(threadId, user)
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
        val responseFlux = chatModel.stream(prompt)

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
        val threads = threadRepository.findAllByUserAndDeletedAtIsNull(user = user, pageable = Pageable.unpaged())
        return threads.mapNotNull { thread ->
            var matchHighlight: String? = null
            var messageId: Long? = null
            var messageIdIndex: Long? = null

            // 쓰레드 이름에서 쿼리 일치 여부 확인
            val chatNameIndex = thread.chatName.indexOf(query, ignoreCase = true)
            if (chatNameIndex != -1) {
                val start = maxOf(0, chatNameIndex - 50)
                val end = minOf(thread.chatName.length, chatNameIndex + query.length + 50)
                matchHighlight = thread.chatName.substring(start, end)
                messageId = null
            }

            // 메시지에서 쿼리 일치 여부 확인
            if (matchHighlight == null) {
                var i = 0L
                for (message in thread.messages.sortedBy { -it.id }) {
                    val messageContentIndex = message.content.indexOf(query, ignoreCase = true)
                    if (messageContentIndex != -1) {
                        val start = maxOf(0, messageContentIndex - 50)
                        val end = minOf(message.content.length, messageContentIndex + query.length + 50)
                        matchHighlight = message.content.substring(start, end)
                        messageId = message.id
                        messageIdIndex = i
                        break
                    }
                    i++
                }
            }

            // 매칭되는 하이라이트가 있는 경우 응답 생성
            if (matchHighlight != null) {
                ChatDto.SearchThreadResponse(
                    id = thread.id,
                    chatName = thread.chatName,
                    matchHighlight = matchHighlight,
                    messageId = messageId,
                    messageIdIndex = messageIdIndex,
                    createdAt = thread.createdAt,
                    updatedAt = thread.updatedAt,
                )
            } else {
                null
            }
        }
    }
}
