package kr.ac.kau.llmchat.domain.auth

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
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant

@Entity(name = "social_accounts")
class SocialAccountEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: UserEntity,
    @Column(nullable = false, length = 255, columnDefinition = "VARCHAR(255)")
    @Enumerated(value = EnumType.STRING)
    var provider: ProviderEnum,
    @Column(nullable = false, length = 255)
    var uid: String,
    @Column(nullable = false)
    @CreationTimestamp
    var dateJoined: Instant = Instant.EPOCH,
    @Column(nullable = true)
    var lastLogin: Instant?,
    @Column(nullable = false, length = 255)
    var token: String,
    @Column(nullable = false)
    var tokenExpires: Instant,
)
