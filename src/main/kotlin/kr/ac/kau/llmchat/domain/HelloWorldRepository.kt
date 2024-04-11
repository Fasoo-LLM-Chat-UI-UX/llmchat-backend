package kr.ac.kau.llmchat.domain

import org.springframework.data.jpa.repository.JpaRepository

interface HelloWorldRepository : JpaRepository<HelloWorldEntity, Long>
