package kr.ac.kau.llmchat.service.chat

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
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
import okhttp3.OkHttpClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.prompt.ChatOptionsBuilder
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.document.Document
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.IOException
import java.time.Instant
import java.util.concurrent.TimeUnit

// Jina API Retrofit 인터페이스 정의
interface JinaApi {
    @GET("search")
    fun search(
        @Query("query") query: String,
    ): Call<JinaContent>
}

// Jina API 응답 데이터 클래스
data class JinaResponse(
    val title: String,
    val url: String,
    val description: String,
    val markdown: String,
)

data class JinaContent(
    val results: List<JinaResponse>,
)

@Service
class ChatService(
    private val chatModel: OpenAiChatModel,
    private val threadRepository: ThreadRepository,
    private val messageRepository: MessageRepository,
    private val userPreferenceRepository: UserPreferenceRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val documentService: DocumentService,
) {
    private val logger: Logger = LoggerFactory.getLogger(ChatService::class.java)

    // Retrofit 기반 JinaApi 설정
    private val jinaApi: JinaApi =
        Retrofit.Builder()
            .baseUrl("https://s.jina.ai/")
            .addConverterFactory(JacksonConverterFactory.create(jacksonObjectMapper()))
            .client(
                OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        val request =
                            chain.request().newBuilder()
                                .addHeader(
                                    "Authorization",
                                    "Bearer jina_66f08655f19e40ffa3e87f93c348a322d0vrvOBi2j_lkIR0n7PCSbsrN8H-",
                                ) // 인증 토큰 추가
                                .build()
                        chain.proceed(request)
                    }
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .readTimeout(5, TimeUnit.SECONDS)
                    .build(),
            )
            .build()
            .create(JinaApi::class.java)

    // Jina API 호출 함수
    internal fun fetchJinaContent(query: String): String? {
        return try {
            val response = jinaApi.search(query).execute()
            if (response.isSuccessful) {
                logger.info("Jina API 호출 성공")
                response.body()?.results?.joinToString(separator = "\n") { result ->
                    """
                    Title: ${result.title}
                    URL: ${result.url}
                    Description: ${result.description}
                    Markdown: ${result.markdown}
                    """.trimIndent()
                }
            } else {
                logger.error("Jina API 호출 실패: ${response.errorBody()?.string()}")
                null
            }
        } catch (e: Exception) {
            logger.error("Jina API 호출 중 예외 발생", e)
            null
        }
    }

    fun getThreads(
        user: UserEntity,
        pageable: Pageable,
        query: String?,
    ): Page<ThreadEntity> {
        val enhancedQuery =
            query?.let {
                val jinaContent = fetchJinaContent(it)
                if (!jinaContent.isNullOrEmpty()) {
                    """
                    $query

                    Jina API Results:
                    $jinaContent
                    """.trimIndent()
                } else {
                    query
                }
            }
        return if (query == null) {
            threadRepository.findAllByUserAndDeletedAtIsNull(user = user, pageable = pageable)
        } else {
            threadRepository.findAllByUserAndChatNameContainsAndDeletedAtIsNull(
                user = user,
                chatName = enhancedQuery ?: query,
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

        val chatMessage = StringBuilder()
        val assistantMessage =
            MessageEntity(
                thread = thread,
                role = RoleEnum.ASSISTANT,
                content = "",
                rating = null,
            )
        messageRepository.save(assistantMessage)

        val checkRagNeededPrompt =
            Prompt(
                listOf(
                    SystemMessage(
                        """
                        **System Prompt: External Information Check**

                        **Objective:** Identify if external information retrieval is necessary.
                        
                        **Instructions:**
                        
                        1. **Understand the Request:**
                           - Carefully read the user's question to determine what information they are asking for.
                        
                        2. **Check Your Knowledge:**
                           - Consider your knowledge base, which includes information up to October 2023.
                        
                        3. **Make a Decision:**
                           - If the answer is within the information you know, respond with 'YES'.
                           - If the answer requires knowledge beyond your data or needs external sources, respond with 'NO'.
                        
                        **Response Format:**
                        - Provide a single response: either 'YES' or 'NO'.
                        
                        **Important Note:**
                        - Remember, your knowledge includes data only up to October 2023. Use this to guide your decision.
                        """.trimIndent(),
                    ),
                    UserMessage(question),
                ),
                ChatOptionsBuilder.builder()
                    .withModel("gpt-4o-mini")
                    .withMaxTokens(10)
                    .build(),
            )
        val isRagNeeded = chatModel.call(checkRagNeededPrompt).result.output.content?.contains("NO") == true

        if (isRagNeeded) {
            // Retrieve relevant documents
            val relevantDocs =
                documentService.searchSimilarDocuments(
                    user = user,
                    query = question,
                    topK = 3,
                )

            if (relevantDocs.isNotEmpty()) {
                // Build context from retrieved documents
                val context =
                    buildString {
                        appendLine("Here's relevant information from our knowledge base:")
                        appendLine()
                        relevantDocs.forEachIndexed { index: Int, doc: Document ->
                            appendLine("Document ${index + 1}:")
                            appendLine(doc.content)
                            appendLine()

                            emitter.send(
                                SseEmitter.event().data(
                                    ChatDto.SseMessageResponse(
                                        messageId = assistantMessage.id,
                                        role = assistantMessage.role,
                                        content = "참고한 문서 ${index + 1} (보안 등급: ${doc.metadata["securityLevel"]}):\n${doc.content}\n---\n\n",
                                    ),
                                ),
                            )
                        }
                        appendLine("Please use this information to help answer the question.")
                    }

                // Add context to system message
                messages.add(0, SystemMessage(context))
            }
        }

        val prompt = Prompt(messages)
        val responseFlux = chatModel.stream(prompt)

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
