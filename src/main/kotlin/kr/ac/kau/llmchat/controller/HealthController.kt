package kr.ac.kau.llmchat.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HealthController {
    @GetMapping("/healthz")
    fun health(): String {
        return "ok"
    }
}
