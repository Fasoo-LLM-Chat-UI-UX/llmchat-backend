package kr.ac.kau.llmchat.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
class SecurityConfiguration {
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests { authorizeRequests ->
                authorizeRequests
                    .requestMatchers(
                        "/api/v1/auth/register-by-email",
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

        return http.build()
    }
}
