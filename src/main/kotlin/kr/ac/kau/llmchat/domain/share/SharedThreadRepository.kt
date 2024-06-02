package kr.ac.kau.llmchat.domain.share

import kr.ac.kau.llmchat.domain.auth.UserEntity
import kr.ac.kau.llmchat.domain.chat.MessageEntity
import kr.ac.kau.llmchat.domain.chat.ThreadEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SharedThreadRepository : JpaRepository<SharedThreadEntity, Long> {
    fun findAllByUser(user: UserEntity): List<SharedThreadEntity>

    fun findBySharedKey(sharedKey: String): SharedThreadEntity?

    fun findAllByUserAndThreadAndMessage(
        user: UserEntity,
        thread: ThreadEntity,
        message: MessageEntity,
    ): SharedThreadEntity?
}
