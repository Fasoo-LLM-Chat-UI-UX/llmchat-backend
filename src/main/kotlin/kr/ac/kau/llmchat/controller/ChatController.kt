package kr.ac.kau.llmchat.controller

import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.openai.OpenAiChatClient
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException

@RestController
class ChatController(
    private val chatClient: OpenAiChatClient,
) {
    @GetMapping("/ai/generate")
    fun generate(
        @RequestParam(value = "message", defaultValue = "Tell me a joke") message: String?,
    ): Map<*, *> {
        return mapOf("generation" to chatClient.call(message))
    }

    @GetMapping("/ai/generateStream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun generateStream(
        @RequestParam(
            value = "message",
            defaultValue = "Tell me a joke",
        ) message: String?,
    ): SseEmitter {
        val emitter = SseEmitter()
        val prompt = Prompt(UserMessage(message))
        val responseFlux = chatClient.stream(prompt)

        responseFlux.subscribe(
            { chatResponse ->
                try {
                    emitter.send(SseEmitter.event().data(chatResponse))
                } catch (e: IOException) {
                    emitter.completeWithError(e)
                }
            },
            { error ->
                emitter.completeWithError(error)
            },
            {
                emitter.complete()
            },
        )

        return emitter
    }
}
