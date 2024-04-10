package kr.ac.kau.llmchat.domain

import kr.ac.kau.llmchat.service.HelloWorldEntity
import org.springframework.data.jpa.repository.JpaRepository

interface HelloWorldRepository : JpaRepository<HelloWorldEntity, Long>
