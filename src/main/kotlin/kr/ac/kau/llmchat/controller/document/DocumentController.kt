package kr.ac.kau.llmchat.controller.document

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import kr.ac.kau.llmchat.domain.auth.UserEntity
import kr.ac.kau.llmchat.service.document.DocumentService
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/document")
class DocumentController(
    private val documentService: DocumentService,
) {
    @PostMapping
    @SecurityRequirement(name = "Authorization")
    @Operation(summary = "문서 생성", description = "새로운 문서를 생성하는 API")
    fun createDocument(
        @RequestBody request: DocumentDto.CreateRequest,
    ): DocumentDto.Response {
        val user = SecurityContextHolder.getContext().authentication.principal as UserEntity
        return documentService.createDocument(
            user = user,
            content = request.content,
            securityLevel = request.securityLevel,
        ).let {
            DocumentDto.Response(
                id = it.id,
                content = it.content,
                securityLevel = it.securityLevel,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt,
            )
        }
    }
}
