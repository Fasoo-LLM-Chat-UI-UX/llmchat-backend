package kr.ac.kau.llmchat.controller.user

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.transaction.Transactional
import kr.ac.kau.llmchat.domain.auth.ProviderEnum
import kr.ac.kau.llmchat.domain.auth.SocialAccountRepository
import kr.ac.kau.llmchat.domain.auth.UserEntity
import kr.ac.kau.llmchat.service.user.UserService
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

enum class LoginType {
    USERNAME,
    GOOGLE,
    KAKAO,
    NAVER,
}

@RestController
@RequestMapping("/api/v1/user")
class UserController(
    private val userService: UserService,
    private val socialAccountRepository: SocialAccountRepository,
) {
    @GetMapping("/profile")
    @SecurityRequirement(name = "Authorization")
    @Operation(summary = "프로필 조회", description = "사용자의 프로필을 조회하는 API")
    @Transactional
    fun getProfile(): UserDto.GetProfileResponse {
        val user = SecurityContextHolder.getContext().authentication.principal as UserEntity
        val socialAccount = socialAccountRepository.findByUser(user).firstOrNull()
        return UserDto.GetProfileResponse(
            loginType =
                when (socialAccount?.provider) {
                    ProviderEnum.GOOGLE -> LoginType.GOOGLE
                    ProviderEnum.KAKAO -> LoginType.KAKAO
                    ProviderEnum.NAVER -> LoginType.NAVER
                    null -> LoginType.USERNAME
                },
            username = user.username,
            name = user.name,
            mobileNumber = user.mobileNumber,
            email = user.email,
            profileImage = user.profileImage,
        )
    }

    @PostMapping("/profile")
    @SecurityRequirement(name = "Authorization")
    @Operation(summary = "프로필 수정", description = "사용자의 프로필을 수정하는 API")
    fun updateProfile(
        @RequestBody dto: UserDto.UpdateProfileRequest,
    ) {
        val user = SecurityContextHolder.getContext().authentication.principal as UserEntity
        userService.updateProfile(user = user, dto = dto)
    }

    @GetMapping("/preference")
    @SecurityRequirement(name = "Authorization")
    @Operation(summary = "환경 설정 조회", description = "사용자의 환경 설정을 조회하는 API")
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
                    securityLevel = it.securityLevel,
                )
            }
    }

    @PostMapping("/preference")
    @SecurityRequirement(name = "Authorization")
    @Operation(summary = "환경 설정 수정", description = "사용자의 환경 설정을 수정하는 API")
    fun updatePreference(
        @RequestBody dto: UserDto.UpdatePreferenceRequest,
    ) {
        val user = SecurityContextHolder.getContext().authentication.principal as UserEntity
        userService.updatePreference(user = user, dto = dto)
    }
}
