package kr.ac.kau.llmchat.domain.auth

import org.springframework.data.jpa.repository.JpaRepository

interface SocialAccountRepository : JpaRepository<SocialAccountEntity, Long> {
    fun findByUidAndProvider(
        uid: String,
        provider: ProviderEnum,
    ): SocialAccountEntity?
}
