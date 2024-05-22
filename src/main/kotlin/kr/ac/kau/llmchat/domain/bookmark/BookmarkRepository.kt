package kr.ac.kau.llmchat.domain.bookmark

import kr.ac.kau.llmchat.domain.auth.UserEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BookmarkRepository : JpaRepository<BookmarkEntity, Long> {
    fun findAllByUser(
        user: UserEntity,
        pageable: Pageable,
    ): Page<BookmarkEntity>
}
