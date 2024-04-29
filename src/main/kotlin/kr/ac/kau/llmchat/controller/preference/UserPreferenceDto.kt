package kr.ac.kau.llmchat.controller.preference

import kr.ac.kau.llmchat.domain.preference.ModelVersionEnum
import kr.ac.kau.llmchat.domain.preference.SpeechVoiceEnum
import kr.ac.kau.llmchat.domain.preference.UILanguageCodeEnum
import kr.ac.kau.llmchat.domain.preference.UIThemeEnum

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
