package kr.ac.kau.llmchat.controller.chat

import io.swagger.v3.oas.annotations.security.SecurityRequirement
import kr.ac.kau.llmchat.service.chat.ChatService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/jina")
class JinaApiController(
    private val chatService: ChatService,
) {
    @GetMapping("/search")
    @SecurityRequirement(name = "Authorization")
    fun searchJinaApi(
        @RequestParam query: String,
    ): String? {
        return chatService.fetchJinaContent(query)
    }
}
