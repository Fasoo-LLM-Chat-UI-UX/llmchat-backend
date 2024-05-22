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

    @PutMapping("/thread/{threadId}/auto-rename")
    @SecurityRequirement(name = "Authorization")
    fun autoRenameThread(
        @PathVariable threadId: Long,
    ): SseEmitter {
        val user = SecurityContextHolder.getContext().authentication.principal as UserEntity
        val sseEmitter = chatService.autoRenameThread(threadId = threadId, user = user)
        return sseEmitter
    }

    @GetMapping("/thread/deleted")
    @SecurityRequirement(name = "Authorization")
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
    fun softDeleteThread(
        @PathVariable threadId: Long,
    ) {
        val user = SecurityContextHolder.getContext().authentication.principal as UserEntity
        chatService.softDeleteThread(threadId = threadId, user = user)
    }

    @PostMapping("/thread/{threadId}/restore")
    @SecurityRequirement(name = "Authorization")
    fun restoreThread(
        @PathVariable threadId: Long,
    ) {
        val user = SecurityContextHolder.getContext().authentication.principal as UserEntity
        chatService.restoreThread(threadId = threadId, user = user)
    }

    @DeleteMapping("/thread/{threadId}/hard-delete")
    @SecurityRequirement(name = "Authorization")
    fun hardDeleteThread(
        @PathVariable threadId: Long,
    ) {
        val user = SecurityContextHolder.getContext().authentication.principal as UserEntity
        chatService.hardDeleteThread(threadId = threadId, user = user)
    }

    @PutMapping("/thread/{threadId}/manual-rename")
    @SecurityRequirement(name = "Authorization")
    fun manualRenameThread(
        @PathVariable threadId: Long,
        @RequestBody dto: ChatDto.ManualRenameThreadRequest,
    ) {
        val user = SecurityContextHolder.getContext().authentication.principal as UserEntity
        chatService.manualRenameThread(threadId = threadId, user = user, dto = dto)
    }

    @GetMapping("/thread/{threadId}/message")
    @SecurityRequirement(name = "Authorization")
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
