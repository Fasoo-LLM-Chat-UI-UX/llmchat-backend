package kr.ac.kau.llmchat.service.auth

import kr.ac.kau.llmchat.controller.auth.AuthDto
import kr.ac.kau.llmchat.domain.auth.UserEntity
import kr.ac.kau.llmchat.domain.auth.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    fun registerByEmail(dto: AuthDto.RegisterByEmailRequest) {
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
}
