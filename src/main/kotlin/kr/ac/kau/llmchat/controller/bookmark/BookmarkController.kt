package kr.ac.kau.llmchat.controller.bookmark

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import kr.ac.kau.llmchat.domain.auth.UserEntity
import kr.ac.kau.llmchat.service.bookmark.BookmarkService
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

@RestController
@RequestMapping("/api/v1/bookmark")
class BookmarkController(
    private val bookmarkService: BookmarkService,
) {
    @PostMapping
    @SecurityRequirement(name = "Authorization")
    @Operation(summary = "AI 응답으로부터 북마크 생성", description = "북마크 생성")
    fun create(
        @RequestBody dto: BookmarkDto.CreateRequest,
    ): BookmarkDto.BookmarkResponse {
        val user = SecurityContextHolder.getContext().authentication.principal as UserEntity
        return bookmarkService.create(user = user, dto = dto)
            .let {
                BookmarkDto.BookmarkResponse(
                    id = it.id,
                    title = it.title,
                    emoji = it.emoji,
                    userMessage = it.userMessage,
                    assistantMessage = it.assistantMessage,
                    createdAt = it.createdAt,
                )
            }
    }

    @GetMapping
    @SecurityRequirement(name = "Authorization")
    @Operation(summary = "북마크 목록 조회", description = "북마크 목록 조회")
    @PageableAsQueryParam
    fun getAllBookmarks(
        @Parameter(hidden = true)
        @PageableDefault(size = 100, sort = ["id"], direction = Sort.Direction.DESC)
        pageable: Pageable,
    ): Page<BookmarkDto.BookmarkResponse> {
        val user = SecurityContextHolder.getContext().authentication.principal as UserEntity
        return bookmarkService.getAllBookmarks(user = user, pageable = pageable)
            .map {
                BookmarkDto.BookmarkResponse(
                    id = it.id,
                    title = it.title,
                    emoji = it.emoji,
                    userMessage = it.userMessage,
                    assistantMessage = it.assistantMessage,
                    createdAt = it.createdAt,
                )
            }
    }

    @GetMapping("/{id}")
    @SecurityRequirement(name = "Authorization")
    @Operation(summary = "북마크 상세 조회", description = "북마크 상세 조회")
    fun getBookmark(
        @PathVariable id: Long,
    ): BookmarkDto.BookmarkResponse {
        val user = SecurityContextHolder.getContext().authentication.principal as UserEntity
        return bookmarkService.getBookmark(user = user, id = id)
            .let {
                BookmarkDto.BookmarkResponse(
                    id = it.id,
                    title = it.title,
                    emoji = it.emoji,
                    userMessage = it.userMessage,
                    assistantMessage = it.assistantMessage,
                    createdAt = it.createdAt,
                )
            }
    }

    @PutMapping("/{id}")
    @SecurityRequirement(name = "Authorization")
    @Operation(summary = "북마크 수정", description = "북마크 수정")
    fun updateBookmark(
        @PathVariable id: Long,
        @RequestBody dto: BookmarkDto.UpdateRequest,
    ): BookmarkDto.BookmarkResponse {
        val user = SecurityContextHolder.getContext().authentication.principal as UserEntity
        return bookmarkService.updateBookmark(user = user, id = id, dto = dto)
            .let {
                BookmarkDto.BookmarkResponse(
                    id = it.id,
                    title = it.title,
                    emoji = it.emoji,
                    userMessage = it.userMessage,
                    assistantMessage = it.assistantMessage,
                    createdAt = it.createdAt,
                )
            }
    }

    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "Authorization")
    @Operation(summary = "북마크 삭제", description = "북마크 삭제")
    fun deleteBookmark(
        @PathVariable id: Long,
    ) {
        val user = SecurityContextHolder.getContext().authentication.principal as UserEntity
        bookmarkService.deleteBookmark(user = user, id = id)
    }
}
