package kr.ac.kau.llmchat.controller.auth

sealed class AuthDto {
    data class RegisterByEmailRequest(
        val username: String,
        val password: String,
        val email: String?,
        val mobileNumber: String?,
        val name: String,
    )
}
