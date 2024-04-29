package kr.ac.kau.llmchat.configuration

import kr.ac.kau.llmchat.service.auth.AuthService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
class SecurityConfiguration {
    @Bean
    fun filterChain(
        http: HttpSecurity,
        authService: AuthService,
    ): SecurityFilterChain {
        http
            .authorizeHttpRequests { authorizeRequests ->
                authorizeRequests
                    .requestMatchers(
                        "/api/v1/auth/register-by-*",
                        "/api/v1/auth/login-by-*",
                        "/api/v1/auth/check-username",
                    )
                    .permitAll()
                    .requestMatchers(
                        "/api/v1/**",
                    )
                    .authenticated()
                    .anyRequest()
                    .permitAll()
            }
            .csrf { csrf ->
                csrf
                    .disable()
            }
            .addFilterBefore(
                JwtAuthenticationFilter(authService = authService),
                UsernamePasswordAuthenticationFilter::class.java,
            )

        return http.build()
    }
}
