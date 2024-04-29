package kr.ac.kau.llmchat.controller.user

sealed class UserProfileDto {
    data class GetProfileResponse(
        val username: String,
        val name: String,
        val mobileNumber: String?,
        val email: String?,
        val profileImage: String?,
    )

    data class UpdateProfileRequest(
        val password: String?,
        val name: String?,
        val mobileNumber: String?,
        val email: String?,
    )
}
