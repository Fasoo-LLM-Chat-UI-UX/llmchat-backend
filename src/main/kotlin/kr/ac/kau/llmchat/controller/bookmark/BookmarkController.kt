package kr.ac.kau.llmchat.controller.bookmark

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import kr.ac.kau.llmchat.domain.auth.UserEntity
import kr.ac.kau.llmchat.service.bookmark.BookmarkService
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/bookmark")
class BookmarkController(
    private val bookmarkService: BookmarkService,
) {
    // Create
    @PostMapping
    @SecurityRequirement(name = "Authorization")
    @Operation(summary = "AI 응답으로부터 북마크 생성", description = "북마크 생성")
    fun create(
        @RequestBody dto: BookmarkDto.CreateRequest,
    ) {
        val user = SecurityContextHolder.getContext().authentication.principal as UserEntity
        bookmarkService.create(user = user, dto = dto)
    }

    // List

    // Detail

    // Delete
}
