package kr.ac.kau.llmchat.domain.chat

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import kr.ac.kau.llmchat.domain.auth.UserEntity
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant

@Entity(name = "threads")
class ThreadEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: UserEntity,
    @Column(nullable = false, length = 255)
    var chatName: String,
    @Column(nullable = false)
    @CreationTimestamp
    var createdAt: Instant = Instant.EPOCH,
    @Column(nullable = false)
    @UpdateTimestamp
    var updatedAt: Instant = Instant.EPOCH,
    @Column(nullable = true)
    var deletedAt: Instant? = null,
) {
    @OneToMany(mappedBy = "thread", fetch = FetchType.LAZY)
    var messages: MutableList<MessageEntity> = mutableListOf()
}
