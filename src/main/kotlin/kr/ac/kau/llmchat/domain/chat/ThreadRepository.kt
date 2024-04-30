package kr.ac.kau.llmchat.domain.chat

import kr.ac.kau.llmchat.domain.auth.UserEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface ThreadRepository : JpaRepository<ThreadEntity, Long> {
    fun findAllByUser(
        user: UserEntity,
        pageable: Pageable,
    ): Page<ThreadEntity>
}