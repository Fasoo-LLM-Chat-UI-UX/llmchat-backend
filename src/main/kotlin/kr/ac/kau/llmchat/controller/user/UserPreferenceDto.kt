package kr.ac.kau.llmchat.controller.user

import kr.ac.kau.llmchat.domain.user.ModelVersionEnum
import kr.ac.kau.llmchat.domain.user.SpeechVoiceEnum
import kr.ac.kau.llmchat.domain.user.UILanguageCodeEnum
import kr.ac.kau.llmchat.domain.user.UIThemeEnum

sealed class UserPreferenceDto {
    data class GetPreferenceResponse(
        var uiTheme: UIThemeEnum,
        var uiLanguageCode: UILanguageCodeEnum,
        var speechVoice: SpeechVoiceEnum,
        var aboutModelMessage: String?,
        var aboutUserMessage: String?,
        var aboutMessageEnabled: Boolean,
        var modelVersion: ModelVersionEnum,
    )

    data class UpdatePreferenceRequest(
        var uiTheme: UIThemeEnum,
        var uiLanguageCode: UILanguageCodeEnum,
        var speechVoice: SpeechVoiceEnum,
        var aboutModelMessage: String?,
        var aboutUserMessage: String?,
        var aboutMessageEnabled: Boolean,
        var modelVersion: ModelVersionEnum,
    )
}
