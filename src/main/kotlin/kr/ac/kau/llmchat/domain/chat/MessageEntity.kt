package kr.ac.kau.llmchat.domain.chat

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Lob
import jakarta.persistence.ManyToOne
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant

@Entity(name = "messages")
class MessageEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id", nullable = false)
    var thread: ThreadEntity,
    @Column(nullable = false, length = 255, columnDefinition = "VARCHAR(255)")
    @Enumerated(value = EnumType.STRING)
    var role: RoleEnum,
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,
    @Column(nullable = true, length = 255, columnDefinition = "VARCHAR(255)")
    @Enumerated(value = EnumType.STRING)
    var rating: RatingEnum?,
    @Column(nullable = false)
    @CreationTimestamp
    var createdAt: Instant = Instant.EPOCH,
    @Column(nullable = false)
    @UpdateTimestamp
    var updatedAt: Instant = Instant.EPOCH,
)
