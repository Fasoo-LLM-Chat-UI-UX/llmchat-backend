package kr.ac.kau.llmchat.service.auth

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import kr.ac.kau.llmchat.controller.auth.AuthDto
import kr.ac.kau.llmchat.domain.auth.UserEntity
import kr.ac.kau.llmchat.domain.auth.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.util.Date
import javax.crypto.SecretKey

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    @Value("\${llmchat.auth.jwt-secret}") private val jwtSecret: String,
) {
    val key: SecretKey = Keys.hmacShaKeyFor(jwtSecret.toByteArray())

    fun registerByUsername(dto: AuthDto.RegisterByUsernameRequest) {
        val username = dto.username.lowercase()

        val existingUser = userRepository.findByUsername(username)
        if (existingUser != null) {
            throw IllegalArgumentException("User already exists with username: $username")
        }

        val user =
            UserEntity(
                username = username,
                password = passwordEncoder.encode(dto.password),
                email = dto.email,
                mobileNumber = dto.mobileNumber,
                name = dto.name,
                lastLogin = null,
            )

        userRepository.save(user)
    }

    fun loginByUsername(dto: AuthDto.LoginByUsernameRequest): String {
        val username = dto.username.lowercase()
        val password = dto.password

        val user =
            userRepository.findByUsername(username)
                ?: throw IllegalArgumentException("User not found with username: $username")

        if (!passwordEncoder.matches(password, user.password)) {
            throw IllegalArgumentException("Invalid password for username: $username")
        }

        return generateJwtToken(user)
    }

    private fun generateJwtToken(user: UserEntity): String {
        val claims = Jwts.claims().subject(user.username).build()
        val now = Date()
        val validity = Date(now.time + 1000 * 60 * 60) // 1 hour

        return Jwts.builder()
            .claims(claims)
            .issuedAt(now)
            .expiration(validity)
            .signWith(key, Jwts.SIG.HS512)
            .compact()
    }
}
