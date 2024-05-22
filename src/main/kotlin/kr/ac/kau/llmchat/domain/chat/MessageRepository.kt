package kr.ac.kau.llmchat.domain.chat

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface MessageRepository : JpaRepository<MessageEntity, Long> {
    fun findAllByThread(
        thread: ThreadEntity,
        pageable: Pageable,
    ): Page<MessageEntity>

    fun deleteAllByThread(thread: ThreadEntity)

    fun deleteAllByThreadAndIdGreaterThan(
        thread: ThreadEntity,
        messageId: Long,
    )

    @Query(
        """
        SELECT m
        FROM messages m
        WHERE m.thread = :thread
        AND m.role = kr.ac.kau.llmchat.domain.chat.RoleEnum.USER
        AND m.id < :messageId
        ORDER BY m.id DESC
        LIMIT 1
    """,
    )
    fun findUserMessage(
        thread: ThreadEntity,
        messageId: Long,
    ): MessageEntity?
}
