package kr.ac.kau.llmchat

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class LlmChatApplication

fun main(args: Array<String>) {
	runApplication<LlmChatApplication>(*args)
}
