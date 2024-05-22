package kr.ac.kau.llmchat.domain.bookmark

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Lob
import jakarta.persistence.ManyToOne
import kr.ac.kau.llmchat.domain.auth.UserEntity
import kr.ac.kau.llmchat.domain.chat.MessageEntity
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant

@Entity(name = "bookmarks")
class BookmarkEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: UserEntity,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    var message: MessageEntity,
    @Column(nullable = false, length = 255)
    var title: String,
    @Column(nullable = false, length = 255)
    var emoji: String,
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    var userMessage: String,
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    var assistantMessage: String,
    @Column(nullable = false)
    @CreationTimestamp
    var createdAt: Instant = Instant.EPOCH,
    @Column(nullable = false)
    @UpdateTimestamp
    var updatedAt: Instant = Instant.EPOCH,
)
