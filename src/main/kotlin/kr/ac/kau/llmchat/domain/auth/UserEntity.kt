package kr.ac.kau.llmchat.domain.auth

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant

@Entity(name = "users")
class UserEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,
    @Column(nullable = false, unique = true, length = 255)
    var username: String,
    @Column(nullable = true, length = 255)
    var password: String?,
    @Column(nullable = true, length = 255)
    var email: String?,
    @Column(nullable = true, length = 255)
    var mobileNumber: String?,
    @Column(nullable = false, length = 255)
    var name: String,
    @Column(nullable = true, length = 255)
    var profileImage: String?,
    @Column(nullable = false)
    @CreationTimestamp
    var dateJoined: Instant = Instant.EPOCH,
    @Column(nullable = true)
    var lastLogin: Instant?,
)
