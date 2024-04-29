package kr.ac.kau.llmchat.controller.user

import io.swagger.v3.oas.annotations.security.SecurityRequirement
import kr.ac.kau.llmchat.domain.auth.UserEntity
import kr.ac.kau.llmchat.service.user.UserService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/user")
class UserController(
    private val userService: UserService,
) {
    @GetMapping("/profile")
    @SecurityRequirement(name = "Authorization")
    fun getProfile(): UserDto.GetProfileResponse {
        val user = SecurityContextHolder.getContext().authentication.principal as UserEntity
        return UserDto.GetProfileResponse(
            username = user.username,
            name = user.name,
            mobileNumber = user.mobileNumber,
            email = user.email,
            profileImage = user.profileImage,
        )
    }

    @PostMapping("/profile")
    @SecurityRequirement(name = "Authorization")
    fun updateProfile(
        @RequestBody dto: UserDto.UpdateProfileRequest,
    ): ResponseEntity<Unit> {
        val user = SecurityContextHolder.getContext().authentication.principal as UserEntity
        userService.updateProfile(user = user, dto = dto)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/preference")
    @SecurityRequirement(name = "Authorization")
    fun getPreference(): UserDto.GetPreferenceResponse {
        val user = SecurityContextHolder.getContext().authentication.principal as UserEntity
        return userService
            .getPreference(user = user)
            .let {
                UserDto.GetPreferenceResponse(
                    uiTheme = it.uiTheme,
                    uiLanguageCode = it.uiLanguageCode,
                    speechVoice = it.speechVoice,
                    aboutModelMessage = it.aboutModelMessage,
                    aboutUserMessage = it.aboutUserMessage,
                    aboutMessageEnabled = it.aboutMessageEnabled,
                    modelVersion = it.modelVersion,
                )
            }
    }

    @PostMapping("/preference")
    @SecurityRequirement(name = "Authorization")
    fun updatePreference(
        @RequestBody dto: UserDto.UpdatePreferenceRequest,
    ) {
        val user = SecurityContextHolder.getContext().authentication.principal as UserEntity
        userService.updatePreference(user = user, dto = dto)
    }
}
