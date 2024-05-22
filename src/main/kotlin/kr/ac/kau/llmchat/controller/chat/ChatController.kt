package kr.ac.kau.llmchat.controller.chat

import io.swagger.v3.oas.annotations.Operation
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
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RestController
@RequestMapping("/api/v1/chat")
class ChatController(
    private val chatService: ChatService,
) {
    @GetMapping("/thread")
    @SecurityRequirement(name = "Authorization")
    @Operation(summary = "쓰레드 목록 조회", description = "사용자의 쓰레드 목록을 조회하는 API")
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
    @Operation(summary = "쓰레드 생성", description = "새로운 쓰레드를 생성하는 API")
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

    @PutMapping("/thread/{threadId}/auto-rename")
    @SecurityRequirement(name = "Authorization")
    @Operation(summary = "쓰레드 자동 이름 변경", description = "대화 내역을 요약해 쓰레드의 이름을 지어 주는 API")
    fun autoRenameThread(
        @PathVariable threadId: Long,
    ): SseEmitter {
        val user = SecurityContextHolder.getContext().authentication.principal as UserEntity
        val sseEmitter = chatService.autoRenameThread(threadId = threadId, user = user)
        return sseEmitter
    }

    @PutMapping("/thread/{threadId}/manual-rename")
    @SecurityRequirement(name = "Authorization")
    @Operation(summary = "쓰레드 수동 이름 변경", description = "사용자가 직접 쓰레드의 이름을 변경하는 API")
    fun manualRenameThread(
        @PathVariable threadId: Long,
        @RequestBody dto: ChatDto.ManualRenameThreadRequest,
    ) {
        val user = SecurityContextHolder.getContext().authentication.principal as UserEntity
        chatService.manualRenameThread(threadId = threadId, user = user, dto = dto)
    }

    @GetMapping("/thread/deleted")
    @SecurityRequirement(name = "Authorization")
    @Operation(summary = "삭제된 쓰레드 목록 조회", description = "소프트 삭제된 쓰레드 목록을 조회하는 API")
    @PageableAsQueryParam
    fun getDeletedThreads(
        @Parameter(hidden = true)
        @PageableDefault(size = 100, sort = ["id"], direction = Sort.Direction.DESC)
        pageable: Pageable,
    ): Page<ChatDto.GetThreadResponse> {
        val user = SecurityContextHolder.getContext().authentication.principal as UserEntity
        return chatService.getDeletedThreads(user = user, pageable = pageable)
            .map { thread ->
                ChatDto.GetThreadResponse(
                    id = thread.id,
                    chatName = thread.chatName,
                    createdAt = thread.createdAt,
                    updatedAt = thread.updatedAt,
                )
            }
    }

    @DeleteMapping("/thread/{threadId}/soft-delete")
    @SecurityRequirement(name = "Authorization")
    @Operation(summary = "쓰레드 소프트 삭제", description = "쓰레드를 소프트 삭제하는 API")
    fun softDeleteThread(
        @PathVariable threadId: Long,
    ) {
        val user = SecurityContextHolder.getContext().authentication.principal as UserEntity
        chatService.softDeleteThread(threadId = threadId, user = user)
    }

    @DeleteMapping("/thread/soft-delete-all")
    @SecurityRequirement(name = "Authorization")
    @Operation(summary = "쓰레드 소프트 삭제", description = "모든 쓰레드를 소프트 삭제하는 API")
    fun softDeleteAllThread() {
        val user = SecurityContextHolder.getContext().authentication.principal as UserEntity
        chatService.softDeleteAllThread(user = user)
    }

    @PostMapping("/thread/{threadId}/restore")
    @SecurityRequirement(name = "Authorization")
    @Operation(summary = "소프트 삭제된 쓰레드 복구", description = "소프트 삭제된 쓰레드를 복구하는 API")
    fun restoreThread(
        @PathVariable threadId: Long,
    ) {
        val user = SecurityContextHolder.getContext().authentication.principal as UserEntity
        chatService.restoreThread(threadId = threadId, user = user)
    }

    @DeleteMapping("/thread/{threadId}/hard-delete")
    @SecurityRequirement(name = "Authorization")
    @Operation(summary = "쓰레드 하드 삭제", description = "쓰레드를 하드 삭제하는 API. 소프트 삭제된 쓰레드만 가능하며, 복구 불가.")
    fun hardDeleteThread(
        @PathVariable threadId: Long,
    ) {
        val user = SecurityContextHolder.getContext().authentication.principal as UserEntity
        chatService.hardDeleteThread(threadId = threadId, user = user)
    }

    @GetMapping("/thread/{threadId}/message")
    @SecurityRequirement(name = "Authorization")
    @Operation(summary = "메시지 목록 조회", description = "쓰레드의 메시지 목록을 조회하는 API")
    @PageableAsQueryParam
    fun getMessages(
        @PathVariable threadId: Long,
        @Parameter(hidden = true)
        @PageableDefault(size = 100, sort = ["id"], direction = Sort.Direction.DESC)
        pageable: Pageable,
    ): Page<ChatDto.GetMessageResponse> {
        val user = SecurityContextHolder.getContext().authentication.principal as UserEntity
        return chatService.getMessages(threadId = threadId, user = user, pageable = pageable)
            .map { message ->
                ChatDto.GetMessageResponse(
                    id = message.id,
                    role = message.role,
                    content = message.content,
                    createdAt = message.createdAt,
                    updatedAt = message.updatedAt,
                )
            }
    }

    @PostMapping("/thread/{threadId}/send-message")
    @SecurityRequirement(name = "Authorization")
    @Operation(summary = "메시지 전송", description = "쓰레드에 메시지를 전송하고 AI의 응답을 받는 API")
    fun sendMessage(
        @PathVariable threadId: Long,
        @RequestBody dto: ChatDto.SendMessageRequest,
    ): SseEmitter {
        val user = SecurityContextHolder.getContext().authentication.principal as UserEntity
        val sseEmitter =
            chatService.sendMessage(
                threadId = threadId,
                user = user,
                dto = dto,
            )
        return sseEmitter
    }

    @PutMapping("/thread/{threadId}/message/{messageId}/edit")
    @SecurityRequirement(name = "Authorization")
    @Operation(summary = "메시지 수정", description = "메시지를 수정하고 AI의 응답을 받는 API. 사용자가 보낸 메시지만 수정 가능.")
    fun editMessage(
        @PathVariable threadId: Long,
        @PathVariable messageId: Long,
        @RequestBody dto: ChatDto.SendMessageRequest,
    ): SseEmitter {
        val user = SecurityContextHolder.getContext().authentication.principal as UserEntity
        val sseEmitter = chatService.editMessage(threadId = threadId, messageId = messageId, user = user, dto = dto)
        return sseEmitter
    }
}
