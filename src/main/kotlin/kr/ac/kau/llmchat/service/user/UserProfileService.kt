package kr.ac.kau.llmchat.service.user

import jakarta.transaction.Transactional
import kr.ac.kau.llmchat.controller.user.UserProfileDto
import kr.ac.kau.llmchat.domain.auth.UserEntity
import kr.ac.kau.llmchat.domain.auth.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class UserProfileService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    @Transactional
    fun updateProfile(
        user: UserEntity,
        dto: UserProfileDto.UpdateProfileRequest,
    ) {
        dto.password?.let { user.password = passwordEncoder.encode(it) }
        dto.name?.let { user.name = it }
        dto.mobileNumber?.let { user.mobileNumber = it }
        dto.email?.let { user.email = it }

        userRepository.save(user)
    }
}
