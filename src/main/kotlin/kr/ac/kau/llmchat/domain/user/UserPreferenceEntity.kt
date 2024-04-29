package kr.ac.kau.llmchat.domain.user

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import kr.ac.kau.llmchat.domain.auth.UserEntity
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant

@Entity(name = "user_preferences")
class UserPreferenceEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: UserEntity,
    @Column(nullable = false, length = 255, columnDefinition = "VARCHAR(255)")
    @Enumerated(value = EnumType.STRING)
    var uiTheme: UIThemeEnum,
    @Column(nullable = false, length = 255, columnDefinition = "VARCHAR(255)")
    @Enumerated(value = EnumType.STRING)
    var uiLanguageCode: UILanguageCodeEnum,
    @Column(nullable = false, length = 255, columnDefinition = "VARCHAR(255)")
    @Enumerated(value = EnumType.STRING)
    var speechVoice: SpeechVoiceEnum,
    @Column(nullable = true, length = 1000)
    var aboutModelMessage: String?,
    @Column(nullable = true, length = 1000)
    var aboutUserMessage: String?,
    @Column(nullable = false)
    var aboutMessageEnabled: Boolean,
    @Column(nullable = false, length = 255, columnDefinition = "VARCHAR(255)")
    @Enumerated(value = EnumType.STRING)
    var modelVersion: ModelVersionEnum,
    @Column(nullable = false)
    @CreationTimestamp
    var createdAt: Instant = Instant.EPOCH,
    @Column(nullable = false)
    @UpdateTimestamp
    var updatedAt: Instant = Instant.EPOCH,
)
