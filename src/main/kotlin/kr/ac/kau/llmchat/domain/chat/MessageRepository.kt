package kr.ac.kau.llmchat.domain.chat

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MessageRepository : JpaRepository<MessageEntity, Long> {
    fun findAllByThread(
        thread: ThreadEntity,
        pageable: Pageable,
    ): Page<MessageEntity>

    fun deleteAllByThread(thread: ThreadEntity)
}
