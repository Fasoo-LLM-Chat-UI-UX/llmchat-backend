package kr.ac.kau.llmchat.domain.auth

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant

@Entity(name = "user_preferences")
class UserPreferenceEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: UserEntity,
    @Column(nullable = false, length = 255)
    var uiTheme: String,
    @Column(nullable = false, length = 255)
    var uiLanguageCode: String,
    @Column(nullable = false, length = 255)
    var speechVoice: String,
    @Column(nullable = true, length = 1000)
    var aboutModelMessage: String?,
    @Column(nullable = true, length = 1000)
    var aboutUserMessage: String?,
    @Column(nullable = false)
    var aboutMessageEnabled: Boolean,
    @Column(nullable = true, length = 255)
    var modelVersion: String?,
    @Column(nullable = false)
    @CreationTimestamp
    var createdAt: Instant = Instant.EPOCH,
    @Column(nullable = false)
    @UpdateTimestamp
    var updatedAt: Instant = Instant.EPOCH,
)
