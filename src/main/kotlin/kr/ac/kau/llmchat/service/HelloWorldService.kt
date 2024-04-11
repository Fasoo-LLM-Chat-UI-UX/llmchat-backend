package kr.ac.kau.llmchat.service

import kr.ac.kau.llmchat.domain.HelloWorldEntity
import kr.ac.kau.llmchat.domain.HelloWorldRepository
import org.springframework.stereotype.Service

@Service
class HelloWorldService(
    private val helloWorldRepository: HelloWorldRepository,
) {
    fun hello(): List<HelloWorldEntity> {
        return helloWorldRepository.findAll()
    }
}
