package kr.ac.kau.llmchat.configuration

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import kr.ac.kau.llmchat.service.auth.AuthService
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.GenericFilterBean

class JwtAuthenticationFilter(private val authService: AuthService) : GenericFilterBean() {
    override fun doFilter(
        servletRequest: ServletRequest,
        servletResponse: ServletResponse,
        filterChain: FilterChain,
    ) {
        try {
            val httpServetRequest = servletRequest as HttpServletRequest
            val bearerToken = httpServetRequest.getHeader("Authorization") ?: return
            if (!bearerToken.startsWith("Bearer ")) return
            val token = bearerToken.substring(7)
            val authentication = authService.getAuthentication(token) ?: return
            SecurityContextHolder.getContext().authentication = authentication
        } finally {
            filterChain.doFilter(servletRequest, servletResponse)
        }
    }
}
