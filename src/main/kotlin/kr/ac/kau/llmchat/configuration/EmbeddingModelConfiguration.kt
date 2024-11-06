package kr.ac.kau.llmchat.configuration

import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.openai.OpenAiEmbeddingModel
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class EmbeddingModelConfiguration {
    @Bean
    fun embeddingModel(
        @Value("\${spring.ai.openai.api-key}") apiKey: String,
    ): EmbeddingModel {
        return OpenAiEmbeddingModel(OpenAiApi(apiKey))
    }
}
