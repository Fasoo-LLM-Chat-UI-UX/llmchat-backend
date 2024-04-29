package kr.ac.kau.llmchat.controller.user

import io.swagger.v3.oas.annotations.security.SecurityRequirement
import kr.ac.kau.llmchat.domain.auth.UserEntity
import kr.ac.kau.llmchat.service.user.UserPreferenceService
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/preference")
class UserPreferenceController(
    private val userPreferenceService: UserPreferenceService,
) {
    @GetMapping
    @SecurityRequirement(name = "Authorization")
    fun getPreference(): UserPreferenceDto.GetPreferenceResponse {
        val user = SecurityContextHolder.getContext().authentication.principal as UserEntity
        return userPreferenceService
            .getPreference(user = user)
            .let {
                UserPreferenceDto.GetPreferenceResponse(
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

    @PostMapping
    @SecurityRequirement(name = "Authorization")
    fun updatePreference(
        @RequestBody dto: UserPreferenceDto.UpdatePreferenceRequest,
    ) {
        val user = SecurityContextHolder.getContext().authentication.principal as UserEntity
        userPreferenceService.updatePreference(user = user, dto = dto)
    }
}
