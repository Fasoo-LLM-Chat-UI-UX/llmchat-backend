package kr.ac.kau.llmchat.service.document

import kr.ac.kau.llmchat.domain.auth.UserEntity
import kr.ac.kau.llmchat.domain.document.DocumentEntity
import kr.ac.kau.llmchat.domain.document.DocumentRepository
import kr.ac.kau.llmchat.domain.document.SecurityLevelEnum
import kr.ac.kau.llmchat.domain.user.UserPreferenceRepository
import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DocumentService(
    private val documentRepository: DocumentRepository,
    private val vectorStore: VectorStore,
    private val userPreferenceRepository: UserPreferenceRepository,
) {
    @Transactional
    fun createDocument(
        user: UserEntity,
        content: String,
        securityLevel: SecurityLevelEnum,
    ): DocumentEntity {
        val document =
            DocumentEntity(
                user = user,
                content = content,
                securityLevel = securityLevel,
            )

        // Save to DB
        documentRepository.save(document)

        // Save to vector store
        vectorStore.add(
            listOf(
                Document(
                    content,
                    mapOf(
                        "documentId" to document.id.toString(),
                        "userId" to user.id.toString(),
                        "securityLevel" to securityLevel.name,
                    ),
                ),
            ),
        )

        return document
    }

    fun searchSimilarDocuments(
        user: UserEntity,
        query: String,
        topK: Int = 5,
    ): List<Document> {
        val userPreference = userPreferenceRepository.findByUser(user = user)

        return vectorStore.similaritySearch(
            SearchRequest.defaults()
                .withQuery(query)
                .withTopK(topK)
                .withFilterExpression(
                    when (userPreference?.securityLevel) {
                        SecurityLevelEnum.HIGH -> "securityLevel in ['HIGH', 'MID', 'LOW']"
                        SecurityLevelEnum.MID -> "securityLevel in ['MID', 'LOW']"
                        SecurityLevelEnum.LOW -> "securityLevel == 'LOW'"
                        null -> "securityLevel == 'LOW'"
                    },
                ),
        )
    }
}
