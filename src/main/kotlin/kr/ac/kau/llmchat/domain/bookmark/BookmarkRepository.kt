package kr.ac.kau.llmchat.domain.bookmark

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BookmarkRepository : JpaRepository<BookmarkEntity, Long>
