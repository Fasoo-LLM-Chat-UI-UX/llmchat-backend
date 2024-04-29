package kr.ac.kau.llmchat.service.user

import kr.ac.kau.llmchat.controller.user.UserPreferenceDto
import kr.ac.kau.llmchat.domain.auth.UserEntity
import kr.ac.kau.llmchat.domain.user.ModelVersionEnum
import kr.ac.kau.llmchat.domain.user.SpeechVoiceEnum
import kr.ac.kau.llmchat.domain.user.UILanguageCodeEnum
import kr.ac.kau.llmchat.domain.user.UIThemeEnum
import kr.ac.kau.llmchat.domain.user.UserPreferenceEntity
import kr.ac.kau.llmchat.domain.user.UserPreferenceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserPreferenceService(
    private val userPreferenceRepository: UserPreferenceRepository,
) {
    @Transactional
    fun getPreference(user: UserEntity): UserPreferenceEntity {
        val userPreference = userPreferenceRepository.findByUser(user = user)
        if (userPreference != null) {
            return userPreference
        }

        val newUserPreference =
            UserPreferenceEntity(
                user = user,
                uiTheme = UIThemeEnum.LIGHT,
                uiLanguageCode = UILanguageCodeEnum.ko,
                speechVoice = SpeechVoiceEnum.MALE,
                aboutModelMessage = null,
                aboutUserMessage = null,
                aboutMessageEnabled = true,
                modelVersion = ModelVersionEnum.GPT_3_5,
            )
        userPreferenceRepository.save(newUserPreference)
        return newUserPreference
    }

    @Transactional
    fun updatePreference(
        user: UserEntity,
        dto: UserPreferenceDto.UpdatePreferenceRequest,
    ) {
        val userPreference = userPreferenceRepository.findByUser(user = user)
        if (userPreference != null) {
            userPreference.uiTheme = dto.uiTheme
            userPreference.uiLanguageCode = dto.uiLanguageCode
            userPreference.speechVoice = dto.speechVoice
            userPreference.aboutModelMessage = dto.aboutModelMessage
            userPreference.aboutUserMessage = dto.aboutUserMessage
            userPreference.aboutMessageEnabled = dto.aboutMessageEnabled
            userPreference.modelVersion = dto.modelVersion

            return
        }

        val newUserPreference =
            UserPreferenceEntity(
                user = user,
                uiTheme = dto.uiTheme,
                uiLanguageCode = dto.uiLanguageCode,
                speechVoice = dto.speechVoice,
                aboutModelMessage = dto.aboutModelMessage,
                aboutUserMessage = dto.aboutUserMessage,
                aboutMessageEnabled = dto.aboutMessageEnabled,
                modelVersion = dto.modelVersion,
            )
        userPreferenceRepository.save(newUserPreference)
    }
}
