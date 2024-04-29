package kr.ac.kau.llmchat.service.auth

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.transaction.Transactional
import kr.ac.kau.llmchat.controller.auth.AuthDto
import kr.ac.kau.llmchat.domain.auth.ProviderEnum
import kr.ac.kau.llmchat.domain.auth.SocialAccountEntity
import kr.ac.kau.llmchat.domain.auth.SocialAccountRepository
import kr.ac.kau.llmchat.domain.auth.UserEntity
import kr.ac.kau.llmchat.domain.auth.UserRepository
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.time.Instant
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.crypto.SecretKey

interface AuthApi {
    @GET("https://www.googleapis.com/oauth2/v3/userinfo")
    fun getGoogleUserInfo(
        @Query("access_token")
        accessToken: String,
    ): Call<GoogleUserInfo>

    @GET("https://kauth.kakao.com/oauth/token")
    fun getKakaoAccessToken(
        @Query("client_id")
        clientId: String,
        @Query("grant_type")
        grantType: String = "authorization_code",
        @Query("code")
        code: String,
    ): Call<KakaoAccessToken>

    @GET("https://kapi.kakao.com/v2/user/me")
    fun getKakaoUserInfo(
        @Query("access_token")
        accessToken: String,
    ): Call<KakaoUserInfo>

    @GET("https://nid.naver.com/oauth2.0/token")
    fun getNaverAccessToken(
        @Query("grant_type")
        grantType: String = "authorization_code",
        @Query("client_id")
        clientId: String,
        @Query("client_secret")
        clientSecret: String,
        @Query("code")
        code: String,
    ): Call<NaverAccessToken>

    @GET("https://openapi.naver.com/v1/nid/me")
    fun getNaverUserInfo(
        @Query("access_token")
        accessToken: String,
    ): Call<NaverUserInfo>
}

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class GoogleUserInfo(
    val sub: String,
    val name: String,
    val givenName: String,
    val familyName: String,
    val picture: String,
    val email: String,
    val emailVerified: Boolean,
    val locale: String,
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class KakaoAccessToken(
    val accessToken: String,
    // "bearer"
    val tokenType: String,
    val refreshToken: String,
    // 7199
    val expiresIn: Long,
    // "account_email profile_image profile_nickname"
    val scope: String,
    // 5183999
    val refreshTokenExpiresIn: Long,
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class KakaoUserInfo(
    val id: Long,
    val connectedAt: Instant,
    val properties: Properties,
    val kakaoAccount: KakaoAccount,
) {
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    data class Properties(
        val nickname: String,
        val profileImage: String,
        val thumbnailImage: String,
    )

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    data class KakaoAccount(
        val profileNicknameNeedsAgreement: Boolean,
        val profileImageNeedsAgreement: Boolean,
        val profile: Profile,
        val hasEmail: Boolean,
        val emailNeedsAgreement: Boolean,
        val isEmailValid: Boolean,
        val isEmailVerified: Boolean,
        val email: String,
    )

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    data class Profile(
        val nickname: String,
        val thumbnailImageUrl: String,
        val profileImageUrl: String,
        val isDefaultImage: Boolean,
        val isDefaultNickname: Boolean,
    )
}

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class NaverAccessToken(
    val accessToken: String,
    val refreshToken: String,
    // "bearer"
    val tokenType: String,
    // "3600"
    val expiresIn: String,
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class NaverUserInfo(
    // "00"
    val resultcode: String,
    // "success"
    val message: String,
    val response: Response,
) {
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    data class Response(
        val id: String,
        val profileImage: String,
        val email: String,
        val mobile: String,
        val mobileE164: String,
        val name: String,
    )
}

@Service
class AuthService(
    objectMapper: ObjectMapper,
    @Value("\${llmchat.auth.jwt-secret}") private val jwtSecret: String,
    @Value("\${llmchat.auth.kakao.client-id}") private val kakaoClientId: String,
    @Value("\${llmchat.auth.naver.client-id}") private val naverClientId: String,
    @Value("\${llmchat.auth.naver.client-secret}") private val naverClientSecret: String,
    private val userRepository: UserRepository,
    private val socialAccountRepository: SocialAccountRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    val key: SecretKey = Keys.hmacShaKeyFor(jwtSecret.toByteArray())

    private val apiClient =
        Retrofit.Builder()
            .baseUrl("http://localhost/") // Required but not used
            .addConverterFactory(JacksonConverterFactory.create(objectMapper))
            .client(
                OkHttpClient
                    .Builder()
                    .connectionPool(ConnectionPool(5, 1, TimeUnit.MINUTES))
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .readTimeout(5, TimeUnit.SECONDS)
                    .addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BASIC
                        },
                    )
                    .build(),
            )
            .build()
            .create(AuthApi::class.java)

    fun registerByUsername(dto: AuthDto.RegisterByUsernameRequest) {
        val username = dto.username.lowercase()

        val existingUser = userRepository.findByUsername(username)
        if (existingUser != null) {
            throw IllegalArgumentException("User already exists with username: $username")
        }

        val user =
            UserEntity(
                username = username,
                password = passwordEncoder.encode(dto.password),
                email = dto.email,
                mobileNumber = dto.mobileNumber,
                name = dto.name,
                profileImage = null,
                lastLogin = null,
            )

        userRepository.save(user)
    }

    @Transactional
    fun loginByUsername(dto: AuthDto.LoginByUsernameRequest): String {
        val username = dto.username.lowercase()
        val password = dto.password

        val user =
            userRepository.findByUsername(username)
                ?: throw IllegalArgumentException("User not found with username: $username")

        user.lastLogin = Instant.now()

        if (!passwordEncoder.matches(password, user.password)) {
            throw IllegalArgumentException("Invalid password for username: $username")
        }

        return generateJwtToken(user)
    }

    @Transactional
    fun loginByGoogle(dto: AuthDto.LoginByGoogleRequest): String {
        val response = apiClient.getGoogleUserInfo(dto.accessToken).execute()
        if (!response.isSuccessful) {
            throw IllegalArgumentException("The given access token is either invalid or expired")
        }
        val responseBody = response.body()!!

        val socialAccount = socialAccountRepository.findByUidAndProvider(responseBody.sub, ProviderEnum.GOOGLE)
        if (socialAccount != null) {
            socialAccount.lastLogin = Instant.now()
            socialAccount.user.lastLogin = Instant.now()
            socialAccount.token = dto.accessToken
            socialAccount.tokenExpires = Instant.now().plusSeconds(3599)

            return generateJwtToken(socialAccount.user)
        } else {
            val newUser =
                UserEntity(
                    username = "google_${responseBody.sub}",
                    password = null,
                    email = responseBody.email,
                    mobileNumber = null,
                    name = response.body()!!.name,
                    lastLogin = Instant.now(),
                    profileImage = responseBody.picture,
                )
            val newSocialAccount =
                SocialAccountEntity(
                    user = newUser,
                    provider = ProviderEnum.GOOGLE,
                    uid = responseBody.sub,
                    lastLogin = Instant.now(),
                    token = dto.accessToken,
                    tokenExpires = Instant.now().plusSeconds(3599),
                )

            userRepository.save(newUser)
            socialAccountRepository.save(newSocialAccount)

            return generateJwtToken(newSocialAccount.user)
        }
    }

    @Transactional
    fun loginByKakao(dto: AuthDto.LoginByKakaoRequest): String {
        val accessToken = apiClient.getKakaoAccessToken(clientId = kakaoClientId, code = dto.code).execute()
        if (!accessToken.isSuccessful) {
            throw IllegalArgumentException("The given code is either invalid or expired")
        }
        val accessTokenBody = accessToken.body()!!

        val userInfo = apiClient.getKakaoUserInfo(accessTokenBody.accessToken).execute()
        if (!userInfo.isSuccessful) {
            throw IllegalArgumentException("The given access token is either invalid or expired")
        }
        val responseBody = userInfo.body()!!

        val socialAccount = socialAccountRepository.findByUidAndProvider(responseBody.id.toString(), ProviderEnum.KAKAO)
        if (socialAccount != null) {
            socialAccount.lastLogin = Instant.now()
            socialAccount.user.lastLogin = Instant.now()
            socialAccount.token = accessTokenBody.accessToken
            socialAccount.tokenExpires = Instant.now().plusSeconds(accessTokenBody.expiresIn)

            return generateJwtToken(socialAccount.user)
        } else {
            val newUser =
                UserEntity(
                    username = "kakao_${responseBody.id}",
                    password = null,
                    email = responseBody.kakaoAccount.email,
                    mobileNumber = null,
                    name = responseBody.properties.nickname,
                    profileImage = responseBody.properties.profileImage,
                    lastLogin = Instant.now(),
                )
            val newSocialAccount =
                SocialAccountEntity(
                    user = newUser,
                    provider = ProviderEnum.KAKAO,
                    uid = responseBody.id.toString(),
                    lastLogin = Instant.now(),
                    token = accessTokenBody.accessToken,
                    tokenExpires = Instant.now().plusSeconds(accessTokenBody.expiresIn),
                )

            userRepository.save(newUser)
            socialAccountRepository.save(newSocialAccount)

            return generateJwtToken(newSocialAccount.user)
        }
    }

    @Transactional
    fun loginByNaver(dto: AuthDto.LoginByNaverRequest): String {
        val accessToken =
            try {
                val accessToken =
                    apiClient.getNaverAccessToken(
                        clientId = naverClientId,
                        clientSecret = naverClientSecret,
                        code = dto.code,
                    ).execute()
                if (!accessToken.isSuccessful) {
                    throw IllegalArgumentException("The given code is either invalid or expired")
                }

                accessToken
            } catch (e: Exception) {
                // Naver API returns 200 even if the code is invalid
                throw IllegalArgumentException("The given code is either invalid or expired")
            }
        val accessTokenBody = accessToken.body()!!

        val userInfo = apiClient.getNaverUserInfo(accessTokenBody.accessToken).execute()
        if (!userInfo.isSuccessful) {
            throw IllegalArgumentException("The given access token is either invalid or expired")
        }
        val responseBody = userInfo.body()!!

        val socialAccount = socialAccountRepository.findByUidAndProvider(responseBody.response.id, ProviderEnum.NAVER)
        if (socialAccount != null) {
            socialAccount.lastLogin = Instant.now()
            socialAccount.user.lastLogin = Instant.now()
            socialAccount.token = accessTokenBody.accessToken
            socialAccount.tokenExpires = Instant.now().plusSeconds(accessTokenBody.expiresIn.toLong())

            return generateJwtToken(socialAccount.user)
        } else {
            val newUser =
                UserEntity(
                    username = "naver_${responseBody.response.id}",
                    password = null,
                    email = responseBody.response.email,
                    mobileNumber = responseBody.response.mobile,
                    name = responseBody.response.name,
                    profileImage = responseBody.response.profileImage,
                    lastLogin = Instant.now(),
                )
            val newSocialAccount =
                SocialAccountEntity(
                    user = newUser,
                    provider = ProviderEnum.NAVER,
                    uid = responseBody.response.id,
                    lastLogin = Instant.now(),
                    token = accessTokenBody.accessToken,
                    tokenExpires = Instant.now().plusSeconds(accessTokenBody.expiresIn.toLong()),
                )

            userRepository.save(newUser)
            socialAccountRepository.save(newSocialAccount)

            return generateJwtToken(newSocialAccount.user)
        }
    }

    fun generateJwtToken(user: UserEntity): String {
        val claims = Jwts.claims().subject(user.username).build()
        val now = Date()
        val validity = Date(now.time + 1000 * 60 * 60 * 24) // 1 day

        return Jwts.builder()
            .claims(claims)
            .issuedAt(now)
            .expiration(validity)
            .signWith(key, Jwts.SIG.HS512)
            .compact()
    }

    fun getAuthentication(token: String): Authentication? {
        val jws =
            try {
                Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
            } catch (e: Exception) {
                return null
            }
        val username = jws.payload.subject
        val user = userRepository.findByUsername(username) ?: return null
        return UsernamePasswordAuthenticationToken(user, token, emptyList())
    }
}
