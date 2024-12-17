package kr.ac.kau.llmchat.service.document

import kr.ac.kau.llmchat.domain.auth.UserEntity
import kr.ac.kau.llmchat.domain.document.DocumentEntity
import kr.ac.kau.llmchat.domain.document.DocumentRepository
import kr.ac.kau.llmchat.domain.document.SecurityLevelEnum
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Service
class PdfUploadService(
    private val documentRepository: DocumentRepository,
) {
    @Transactional
    fun uploadPdf(
        user: UserEntity,
        file: MultipartFile,
        securityLevel: SecurityLevelEnum,
        text: String,
    ): DocumentEntity {
        // 1. PDF 내용 추출 (텍스트로 변환)
        val pdfContent = extractTextFromPdf(file)

        // 2. 데이터베이스에 저장 (추출된 텍스트만 content 컬럼에 저장)
        val document =
            DocumentEntity(
                user = user,
                // content - 추가 텍스트와 PDF 내용 결합
                content = "$text\n$pdfContent",
                securityLevel = securityLevel,
            )

        return documentRepository.save(document)
    }

    private fun extractTextFromPdf(file: MultipartFile): String {
        PDDocument.load(file.inputStream).use { document ->
            val pdfStripper = PDFTextStripper()
            // PDF 내용을 텍스트로 추출
            return pdfStripper.getText(document)
        }
    }
}
