package kr.ac.kau.llmchat.controller.document

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import kr.ac.kau.llmchat.domain.auth.UserEntity
import kr.ac.kau.llmchat.domain.auth.UserRepository
import kr.ac.kau.llmchat.domain.document.SecurityLevelEnum
import kr.ac.kau.llmchat.service.document.DocumentService
import kr.ac.kau.llmchat.service.document.PdfUploadService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/v1/document")
class DocumentController(
    private val documentService: DocumentService,
    // PdfUploadService 의존성 추가
    private val pdfUploadService: PdfUploadService,
    private val userRepository: UserRepository,
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

    @PostMapping("/upload/pdf")
    fun uploadPdf(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("userId") userId: Long,
        @RequestParam("securityLevel") securityLevel: SecurityLevelEnum,
        @RequestParam("text") text: String,
    ): ResponseEntity<Any> {
        // 파일 타입 검증
        if (!file.contentType.equals("application/pdf", ignoreCase = true)) {
            throw ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only PDF files are allowed.")
        }

        // 사용자 조회
        val user =
            userRepository.findById(userId)
                .orElseThrow { ResponseStatusException(HttpStatus.BAD_REQUEST, "User with ID $userId not found") }

        // PdfUploadService 호출
        val document = pdfUploadService.uploadPdf(user, file, securityLevel, text)

        // 응답 반환
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(
                mapOf(
                    "documentId" to document.id,
                    "fileName" to file.originalFilename,
                    "fileSize" to file.size,
                ),
            )
    }
}
