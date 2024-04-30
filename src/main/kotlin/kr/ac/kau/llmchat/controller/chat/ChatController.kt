package kr.ac.kau.llmchat.controller.chat

import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import kr.ac.kau.llmchat.domain.auth.UserEntity
import kr.ac.kau.llmchat.service.chat.ChatService
import org.springdoc.core.converters.models.PageableAsQueryParam
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/chat")
class ChatController(
    // private val chatClient: OpenAiChatClient,
    private val chatService: ChatService,
) {
    @GetMapping("/thread")
    @SecurityRequirement(name = "Authorization")
    @PageableAsQueryParam
    fun getThreads(
        @Parameter(hidden = true)
        @PageableDefault(size = 100, sort = ["id"], direction = Sort.Direction.DESC)
        pageable: Pageable,
    ): Page<ChatDto.GetThreadResponse> {
        val user = SecurityContextHolder.getContext().authentication.principal as UserEntity
        return chatService.getThreads(user = user, pageable = pageable)
            .map { thread ->
                ChatDto.GetThreadResponse(
                    id = thread.id,
                    chatName = thread.chatName,
                    createdAt = thread.createdAt,
                    updatedAt = thread.updatedAt,
                )
            }
    }

    @PostMapping("/thread")
    @SecurityRequirement(name = "Authorization")
    fun createThread(): ChatDto.CreateThreadResponse {
        val user = SecurityContextHolder.getContext().authentication.principal as UserEntity
        val thread = chatService.createThread(user = user)
        return ChatDto.CreateThreadResponse(
            id = thread.id,
            chatName = thread.chatName,
            createdAt = thread.createdAt,
            updatedAt = thread.updatedAt,
        )
    }

    @PostMapping("/thread/{threadId}/send-message")
    @SecurityRequirement(name = "Authorization")
    fun sendMessage(
        @PathVariable threadId: Long,
        @RequestBody dto: ChatDto.SendMessageRequest,
    ) {
        val user = SecurityContextHolder.getContext().authentication.principal as UserEntity
        chatService.sendMessage(
            threadId = threadId,
            user = user,
            dto = dto,
        )
    }

    // TODO: 쓰레드 제목 자동 생성 API
    // TODO: 쓰레드 제목 수정 API
    // TODO: 쓰레드 삭제 API
    // TODO: 쓰레드 메시지 조회 API
    // TODO: 쓰레드 메시지 추가 API
    // TODO: 쓰레드 메시지 생성 API
    // TODO: 쓰레드 메시지 수정 API

    // @GetMapping("/ai/generate")
    // @SecurityRequirement(name = "Authorization")
    // fun generate(
    //     @RequestParam(value = "message", defaultValue = "Tell me a joke") message: String?,
    // ): Map<*, *> {
    //     return mapOf("generation" to chatClient.call(message))
    // }
    //
    // @GetMapping("/ai/generateStream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    // @SecurityRequirement(name = "Authorization")
    // fun generateStream(
    //     @RequestParam(
    //         value = "message",
    //         defaultValue = "Tell me a joke",
    //     ) message: String?,
    // ): SseEmitter {
    //     val emitter = SseEmitter()
    //     val prompt = Prompt(UserMessage(message))
    //     val responseFlux = chatClient.stream(prompt)
    //
    //     responseFlux.subscribe(
    //         { chatResponse ->
    //             try {
    //                 emitter.send(SseEmitter.event().data(chatResponse))
    //             } catch (e: IOException) {
    //                 emitter.completeWithError(e)
    //             }
    //         },
    //         { error ->
    //             emitter.completeWithError(error)
    //         },
    //         {
    //             emitter.complete()
    //         },
    //     )
    //
    //     return emitter
    // }
}
