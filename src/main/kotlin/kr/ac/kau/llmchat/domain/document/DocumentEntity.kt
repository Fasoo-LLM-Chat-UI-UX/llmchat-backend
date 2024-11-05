package kr.ac.kau.llmchat.domain.document

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import kr.ac.kau.llmchat.domain.auth.UserEntity
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant

@Entity(name = "documents")
class DocumentEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: UserEntity,
    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,
    @Column(nullable = false, length = 255, columnDefinition = "VARCHAR(255)")
    @Enumerated(EnumType.STRING)
    var securityLevel: SecurityLevelEnum,
    @Column(nullable = false)
    @CreationTimestamp
    var createdAt: Instant = Instant.EPOCH,
    @Column(nullable = false)
    @UpdateTimestamp
    var updatedAt: Instant = Instant.EPOCH,
)
