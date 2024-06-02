package kr.ac.kau.llmchat.controller.share

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import kr.ac.kau.llmchat.domain.auth.UserEntity
import kr.ac.kau.llmchat.service.share.SharedThreadService
import org.springdoc.core.converters.models.PageableAsQueryParam
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/share")
class SharedThreadController(
    private val sharedThreadService: SharedThreadService,
) {
    @PostMapping("/thread/{threadId}/message/{messageId}/share")
    @SecurityRequirement(name = "Authorization")
    @Operation(summary = "쓰레드 공유", description = "쓰레드를 공유하는 API")
    fun shareThread(
        @PathVariable threadId: Long,
        @PathVariable messageId: Long,
    ): SharedThreadDto.ShareThreadResponse {
        val user = SecurityContextHolder.getContext().authentication.principal as UserEntity
        return sharedThreadService.shareThread(threadId = threadId, messageId = messageId, user = user)
            .let { sharedThread ->
                SharedThreadDto.ShareThreadResponse(
                    sharedKey = sharedThread.sharedKey,
                    sharedAt = sharedThread.sharedAt,
                )
            }
    }

    @PostMapping("/thread/{threadId}/message/{messageId}/unshare")
    @SecurityRequirement(name = "Authorization")
    @Operation(summary = "쓰레드 공유 취소", description = "쓰레드 공유를 취소하는 API")
    fun unshareThread(
        @PathVariable threadId: Long,
        @PathVariable messageId: Long,
    ) {
        val user = SecurityContextHolder.getContext().authentication.principal as UserEntity
        sharedThreadService.unshareThread(threadId = threadId, messageId = messageId, user = user)
    }

    @GetMapping("/thread")
    @SecurityRequirement(name = "Authorization")
    @Operation(summary = "쓰레드 공유 목록 조회", description = "사용자의 쓰레드 공유 목록을 조회하는 API")
    @Transactional(readOnly = true)
    fun getSharedThreads(): List<SharedThreadDto.GetSharedThreadResponse> {
        val user = SecurityContextHolder.getContext().authentication.principal as UserEntity
        return sharedThreadService.getSharedThreads(user = user)
            .map { sharedThread ->
                SharedThreadDto.GetSharedThreadResponse(
                    id = sharedThread.id,
                    threadId = sharedThread.thread.id,
                    threadName = sharedThread.thread.chatName,
                    messageId = sharedThread.message.id,
                    messageContent = sharedThread.message.content,
                    sharedKey = sharedThread.sharedKey,
                    sharedAt = sharedThread.sharedAt,
                )
            }
    }

    @GetMapping("/shared-thread/{sharedKey}")
    @Operation(summary = "쓰레드 정보 조회 (인증 없음)", description = "sharedKey로 쓰레드 정보를 조회하는 API")
    @Transactional(readOnly = true)
    fun getSharedThread(
        @PathVariable sharedKey: String,
    ): SharedThreadDto.GetSharedThreadResponse {
        return sharedThreadService.getSharedThread(sharedKey = sharedKey)
            .let { sharedThread ->
                SharedThreadDto.GetSharedThreadResponse(
                    id = sharedThread.id,
                    threadId = sharedThread.thread.id,
                    threadName = sharedThread.thread.chatName,
                    messageId = sharedThread.message.id,
                    messageContent = sharedThread.message.content,
                    sharedKey = sharedThread.sharedKey,
                    sharedAt = sharedThread.sharedAt,
                )
            }
    }

    @GetMapping("/shared-thread/{sharedKey}/message")
    @Operation(summary = "쓰레드 메시지 목록 조회 (인증 없음)", description = "sharedKey로 쓰레드 메시지 목록을 조회하는 API")
    @PageableAsQueryParam
    fun getSharedThreadMessages(
        @PathVariable sharedKey: String,
        @Parameter(hidden = true)
        @PageableDefault(size = 100, sort = ["id"], direction = Sort.Direction.DESC)
        pageable: Pageable,
    ): Page<SharedThreadDto.GetSharedThreadMessageResponse> {
        return sharedThreadService.getSharedThreadMessages(sharedKey = sharedKey, pageable = pageable)
            .map { message ->
                SharedThreadDto.GetSharedThreadMessageResponse(
                    id = message.id,
                    role = message.role,
                    content = message.content,
                    createdAt = message.createdAt,
                    updatedAt = message.updatedAt,
                )
            }
    }
}
