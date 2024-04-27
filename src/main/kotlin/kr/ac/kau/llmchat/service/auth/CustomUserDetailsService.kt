package kr.ac.kau.llmchat.service.auth

import kr.ac.kau.llmchat.domain.auth.UserRepository
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(
    private val userRepository: UserRepository,
) : UserDetailsService {
    override fun loadUserByUsername(username: String): UserDetails {
        val user =
            userRepository.findByUsername(username = username)
                ?: throw UsernameNotFoundException("User not found with username: $username")
        return User(user.username, user.password, emptyList())
    }
}
