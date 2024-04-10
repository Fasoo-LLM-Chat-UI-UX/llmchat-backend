package kr.ac.kau.llmchat.controller

import kr.ac.kau.llmchat.service.HelloWorldService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HelloWorldController(
    private val helloWorldService: HelloWorldService,
) {
    @GetMapping("/hello")
    fun hello(): List<HelloWorldDto.Response> {
        return helloWorldService.hello()
            .map { HelloWorldDto.Response(it.id, it.name) }
    }
}
