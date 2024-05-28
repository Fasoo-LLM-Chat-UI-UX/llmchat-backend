package kr.ac.kau.llmchat.domain.chat

import kr.ac.kau.llmchat.domain.auth.UserEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ThreadRepository : JpaRepository<ThreadEntity, Long> {
    fun findAllByUserAndDeletedAtIsNull(
        user: UserEntity,
        pageable: Pageable,
    ): Page<ThreadEntity>

    @Query("SELECT t FROM threads t WHERE t.user = :user AND t.chatName LIKE %:chatName% AND t.deletedAt IS NULL")
    fun findAllByUserAndChatNameContainsAndDeletedAtIsNull(
        user: UserEntity,
        chatName: String,
        pageable: Pageable,
    ): Page<ThreadEntity>

    fun findAllByUserAndDeletedAtIsNotNull(
        user: UserEntity,
        pageable: Pageable,
    ): Page<ThreadEntity>
}
