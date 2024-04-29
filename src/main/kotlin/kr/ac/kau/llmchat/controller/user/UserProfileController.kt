package kr.ac.kau.llmchat.controller.user

import io.swagger.v3.oas.annotations.security.SecurityRequirement
import kr.ac.kau.llmchat.domain.auth.UserEntity
import kr.ac.kau.llmchat.service.user.UserProfileService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/user")
class UserProfileController(
    private val userProfileService: UserProfileService,
) {
    @GetMapping("/profile")
    @SecurityRequirement(name = "Authorization")
    fun getProfile(): UserProfileDto.GetProfileResponse {
        val user = SecurityContextHolder.getContext().authentication.principal as UserEntity
        return UserProfileDto.GetProfileResponse(
            username = user.username,
            name = user.name,
            mobileNumber = user.mobileNumber,
            email = user.email,
            profileImage = user.profileImage,
        )
    }

    @PostMapping("/update-profile")
    @SecurityRequirement(name = "Authorization")
    fun updateProfile(
        @RequestBody dto: UserProfileDto.UpdateProfileRequest,
    ): ResponseEntity<Unit> {
        val user = SecurityContextHolder.getContext().authentication.principal as UserEntity
        userProfileService.updateProfile(user = user, dto = dto)
        return ResponseEntity.ok().build()
    }
}
