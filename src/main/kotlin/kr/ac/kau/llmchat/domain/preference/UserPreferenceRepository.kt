package kr.ac.kau.llmchat.domain.preference

import kr.ac.kau.llmchat.domain.auth.UserEntity
import org.springframework.data.jpa.repository.JpaRepository

interface UserPreferenceRepository : JpaRepository<UserPreferenceEntity, Long> {
    fun findByUser(user: UserEntity): UserPreferenceEntity?
}
