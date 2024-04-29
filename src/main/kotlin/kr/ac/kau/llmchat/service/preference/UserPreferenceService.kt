package kr.ac.kau.llmchat.service.preference

import kr.ac.kau.llmchat.controller.preference.UserPreferenceDto
import kr.ac.kau.llmchat.domain.auth.UserEntity
import kr.ac.kau.llmchat.domain.preference.ModelVersionEnum
import kr.ac.kau.llmchat.domain.preference.SpeechVoiceEnum
import kr.ac.kau.llmchat.domain.preference.UILanguageCodeEnum
import kr.ac.kau.llmchat.domain.preference.UIThemeEnum
import kr.ac.kau.llmchat.domain.preference.UserPreferenceEntity
import kr.ac.kau.llmchat.domain.preference.UserPreferenceRepository
import org.springframework.stereotype.Service

@Service
class UserPreferenceService(
    private val userPreferenceRepository: UserPreferenceRepository,
) {
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
            userPreferenceRepository.save(userPreference)

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
