package kr.ac.kau.llmchat.controller.auth

sealed class AuthDto {
    data class RegisterByUsernameRequest(
        val username: String,
        val password: String,
        val email: String?,
        val mobileNumber: String?,
        val name: String,
    )

    data class LoginByUsernameRequest(
        val username: String,
        val password: String,
    )

    data class LoginResponse(
        val token: String,
    )
}