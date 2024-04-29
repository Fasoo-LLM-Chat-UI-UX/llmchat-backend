package kr.ac.kau.llmchat.controller.auth

import io.swagger.v3.oas.annotations.responses.ApiResponse
import kr.ac.kau.llmchat.service.auth.AuthService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService,
) {
    @PostMapping("/register-by-username")
    @ApiResponse(responseCode = "201", description = "Created")
    fun registerByUsername(
        @RequestBody dto: AuthDto.RegisterByUsernameRequest,
    ): ResponseEntity<Unit> {
        authService.registerByUsername(dto = dto)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @PostMapping("/login-by-username")
    fun loginByUsername(
        @RequestBody dto: AuthDto.LoginByUsernameRequest,
    ): ResponseEntity<AuthDto.LoginResponse> {
        val token = authService.loginByUsername(dto = dto)
        return ResponseEntity.ok(AuthDto.LoginResponse(token = token))
    }

    @PostMapping("/login-by-google")
    fun loginByGoogle(
        @RequestBody dto: AuthDto.LoginByGoogleRequest,
    ): ResponseEntity<AuthDto.LoginResponse> {
        val token = authService.loginByGoogle(dto = dto)
        return ResponseEntity.ok(AuthDto.LoginResponse(token = token))
    }

    @PostMapping("/login-by-kakao")
    fun loginByKakao(
        @RequestBody dto: AuthDto.LoginByKakaoRequest,
    ): ResponseEntity<AuthDto.LoginResponse> {
        val token = authService.loginByKakao(dto = dto)
        return ResponseEntity.ok(AuthDto.LoginResponse(token = token))
    }

    @PostMapping("/login-by-naver")
    fun loginByNaver(
        @RequestBody dto: AuthDto.LoginByNaverRequest,
    ): ResponseEntity<AuthDto.LoginResponse> {
        val token = authService.loginByNaver(dto = dto)
        return ResponseEntity.ok(AuthDto.LoginResponse(token = token))
    }

    @PostMapping("/check-username")
    fun checkUsername(
        @RequestBody dto: AuthDto.CheckUsernameRequest,
    ): ResponseEntity<AuthDto.CheckUsernameResponse> {
        val isAvailable = authService.checkUsername(dto = dto)
        return ResponseEntity.ok(AuthDto.CheckUsernameResponse(isAvailable = isAvailable))
    }
}
