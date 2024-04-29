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

    data class LoginByGoogleRequest(
        val accessToken: String,
    )

    data class LoginByKakaoRequest(
        val code: String,
    )

    data class LoginByNaverRequest(
        val code: String,
    )

    data class LoginResponse(
        val token: String,
    )

    data class CheckUsernameRequest(
        val username: String,
    )

    data class CheckUsernameResponse(
        val isAvailable: Boolean,
    )
}
