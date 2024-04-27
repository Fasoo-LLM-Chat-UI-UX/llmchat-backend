package kr.ac.kau.llmchat

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@SpringBootApplication
@EnableJpaAuditing
class MainApplication

fun main(args: Array<String>) {
    runApplication<MainApplication>(*args)
}
