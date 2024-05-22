package kr.ac.kau.llmchat.controller.auth

import io.swagger.v3.oas.annotations.Operation
import kr.ac.kau.llmchat.service.auth.AuthService
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
    @Operation(summary = "사용자 이름 회원 가입", description = "사용자 이름과 비밀번호로 회원 가입하는 API")
    fun registerByUsername(
        @RequestBody dto: AuthDto.RegisterByUsernameRequest,
    ) {
        authService.registerByUsername(dto = dto)
    }

    @PostMapping("/login-by-username")
    @Operation(summary = "사용자 이름 로그인", description = "사용자 이름과 비밀번호로 로그인하는 API")
    fun loginByUsername(
        @RequestBody dto: AuthDto.LoginByUsernameRequest,
    ): ResponseEntity<AuthDto.LoginResponse> {
        val token = authService.loginByUsername(dto = dto)
        return ResponseEntity.ok(AuthDto.LoginResponse(token = token))
    }

    @PostMapping("/login-by-google")
    @Operation(summary = "구글 로그인", description = "구글 OAuth2로 로그인하는 API")
    fun loginByGoogle(
        @RequestBody dto: AuthDto.LoginByGoogleRequest,
    ): ResponseEntity<AuthDto.LoginResponse> {
        val token = authService.loginByGoogle(dto = dto)
        return ResponseEntity.ok(AuthDto.LoginResponse(token = token))
    }

    @PostMapping("/login-by-kakao")
    @Operation(summary = "카카오 로그인", description = "카카오 OAuth2로 로그인하는 API")
    fun loginByKakao(
        @RequestBody dto: AuthDto.LoginByKakaoRequest,
    ): ResponseEntity<AuthDto.LoginResponse> {
        val token = authService.loginByKakao(dto = dto)
        return ResponseEntity.ok(AuthDto.LoginResponse(token = token))
    }

    @PostMapping("/login-by-naver")
    @Operation(summary = "네이버 로그인", description = "네이버 OAuth2로 로그인하는 API")
    fun loginByNaver(
        @RequestBody dto: AuthDto.LoginByNaverRequest,
    ): ResponseEntity<AuthDto.LoginResponse> {
        val token = authService.loginByNaver(dto = dto)
        return ResponseEntity.ok(AuthDto.LoginResponse(token = token))
    }

    @PostMapping("/check-username")
    @Operation(summary = "사용자 이름 중복 확인", description = "회원 가입 시 사용자 이름 중복을 확인하는 API")
    fun checkUsername(
        @RequestBody dto: AuthDto.CheckUsernameRequest,
    ): ResponseEntity<AuthDto.CheckUsernameResponse> {
        val isAvailable = authService.checkUsername(dto = dto)
        return ResponseEntity.ok(AuthDto.CheckUsernameResponse(isAvailable = isAvailable))
    }
}
