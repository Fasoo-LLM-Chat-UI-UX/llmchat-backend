package kr.ac.kau.llmchat.domain.chat

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import kr.ac.kau.llmchat.domain.auth.UserEntity
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity(name = "archived_threads")
class ArchivedThreadEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: UserEntity,
    @Column(nullable = true, length = 10)
    var emoji: String?,
    @Column(nullable = false, length = 255)
    var chatName: String,
    @Column(nullable = false, columnDefinition = "JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    var chatContent: List<ChatContent>,
    @Column(nullable = false)
    var createdAt: Instant,
    @Column(nullable = false)
    @CreationTimestamp
    var archivedAt: Instant = Instant.EPOCH,
) {
    data class ChatContent(
        val role: RoleEnum,
        val content: String,
    )
}
