package kr.ac.kau.llmchat

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@SpringBootApplication(exclude = [ErrorMvcAutoConfiguration::class])
@EnableJpaAuditing
class MainApplication

fun main(args: Array<String>) {
    runApplication<MainApplication>(*args)
}
