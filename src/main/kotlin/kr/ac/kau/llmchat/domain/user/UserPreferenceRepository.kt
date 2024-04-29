package kr.ac.kau.llmchat.domain.user

import kr.ac.kau.llmchat.domain.auth.UserEntity
import org.springframework.data.jpa.repository.JpaRepository

interface UserPreferenceRepository : JpaRepository<UserPreferenceEntity, Long> {
    fun findByUser(user: UserEntity): UserPreferenceEntity?
}
