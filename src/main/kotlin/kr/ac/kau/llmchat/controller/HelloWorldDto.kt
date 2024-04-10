package kr.ac.kau.llmchat.controller

sealed class HelloWorldDto {
    data class Response(
        val id: Long,
        val name: String,
    )
}
