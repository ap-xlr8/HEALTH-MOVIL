package com.healthos

import com.healthos.data.remote.ApiResponse
import com.healthos.data.remote.AuthApiService
import com.healthos.data.remote.ForgotPasswordRequestDto
import com.healthos.data.remote.HealthProfileRequestDto
import com.healthos.data.remote.LoginRequestDto
import com.healthos.data.remote.LoginResponseDto
import com.healthos.data.remote.RefreshTokenRequestDto
import com.healthos.data.remote.RegisterRequestDto
import com.healthos.data.remote.RegisterResponseDataDto
import com.healthos.data.remote.TwoFactorResendRequestDto
import com.healthos.data.remote.TwoFactorVerifyRequestDto
import com.healthos.data.remote.VerifyEmailResponseDataDto
import com.healthos.data.remote.VerifyEmailTokenDto
import com.healthos.data.repository.AuthRepositoryImpl
import com.healthos.domain.model.Role
import com.healthos.security.SecureTokenStore
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.Response

class FakeTokenStore : SecureTokenStore() {
    var savedAccess: String? = null
    var savedRefresh: String? = null
    var savedRole: String? = null
    var savedUserId: String? = null

    override fun save(
        accessToken: String,
        refreshToken: String,
        role: String,
        userId: String?,
    ) {
        savedAccess = accessToken
        savedRefresh = refreshToken
        savedRole = role
        savedUserId = userId
    }

    override fun accessToken(): String? = savedAccess

    override fun refreshToken(): String? = savedRefresh

    override fun role(): String? = savedRole

    override fun userId(): String? = savedUserId

    override fun clear() {
        savedAccess = null
        savedRefresh = null
        savedRole = null
        savedUserId = null
    }
}

class FakeAuthApiService : AuthApiService {
    var shouldFailLogin = false
    var shouldFailRegister = false

    override suspend fun register(request: RegisterRequestDto): Response<ApiResponse<RegisterResponseDataDto>> {
        if (shouldFailRegister) {
            val errorBody = "{\"error\": \"User exists\"}".toResponseBody("application/json".toMediaTypeOrNull())
            return Response.error(400, errorBody)
        }
        return Response.success(
            ApiResponse(
                status = "success",
                data =
                    RegisterResponseDataDto(
                        userId = "USR-001",
                        message = "User registered successfully",
                    ),
            ),
        )
    }

    override suspend fun login(request: LoginRequestDto): Response<LoginResponseDto> {
        if (shouldFailLogin) {
            val errorBody = "{\"error\": \"Unauthorized\"}".toResponseBody("application/json".toMediaTypeOrNull())
            return Response.error(401, errorBody)
        }
        return Response.success(
            LoginResponseDto(
                accessToken = "access_token_123",
                refreshToken = "refresh_token_123",
                role = if (request.email.contains("cuidador")) "caregiver" else "patient",
            ),
        )
    }

    override suspend fun refresh(request: RefreshTokenRequestDto): Response<LoginResponseDto> =
        Response.success(
            LoginResponseDto(
                accessToken = "new_access_token",
                refreshToken = request.refreshToken,
                role = "patient",
            ),
        )

    override suspend fun verifyEmail(request: VerifyEmailTokenDto): Response<ApiResponse<VerifyEmailResponseDataDto>> =
        Response.success(ApiResponse(status = "success", data = VerifyEmailResponseDataDto(userId = "USR-001", message = "Verified")))

    override suspend fun verify2FA(request: TwoFactorVerifyRequestDto): Response<LoginResponseDto> =
        Response.success(LoginResponseDto(accessToken = "2fa_token", refreshToken = "2fa_refresh", role = "patient"))

    override suspend fun resend2FA(request: TwoFactorResendRequestDto): Response<ApiResponse<Map<String, Any>>> =
        Response.success(ApiResponse(status = "success", data = mapOf("success" to true)))

    override suspend fun logout(): Response<Map<String, String>> = Response.success(mapOf("status" to "ok"))

    override suspend fun forgotPassword(request: ForgotPasswordRequestDto): Response<ApiResponse<Map<String, Any>>> =
        Response.success(ApiResponse(status = "success", data = mapOf("success" to true)))

    override suspend fun saveHealthProfile(request: HealthProfileRequestDto): Response<ApiResponse<Map<String, Any>>> =
        Response.success(ApiResponse(status = "success", data = mapOf("success" to true)))
}

class AuthRepositoryTest {
    @Test
    fun login_successful_returnsValidSessionAndPersistsToken() =
        runBlocking {
            val fakeApi = FakeAuthApiService()
            val fakeStore = FakeTokenStore()
            val repository = AuthRepositoryImpl(fakeApi, fakeStore)

            val session = repository.login("user@healthos.app", "Password123!")
            assertNotNull(session)
            assertEquals("access_token_123", session.accessToken)
            assertEquals(Role.PATIENT, session.role)
            assertEquals("access_token_123", fakeStore.accessToken())
        }

    @Test
    fun login_unauthorized_throwsIllegalArgumentException() =
        runBlocking {
            val fakeApi = FakeAuthApiService().apply { shouldFailLogin = true }
            val fakeStore = FakeTokenStore()
            val repository = AuthRepositoryImpl(fakeApi, fakeStore)

            try {
                repository.login("user@healthos.app", "BadPassword")
                fail("Expected exception on auth failure")
            } catch (e: IllegalArgumentException) {
                assertTrue(e.message?.contains("Credenciales inválidas") == true)
            }
        }

    @Test
    fun register_shortPassword_throwsIllegalArgumentException() {
        val fakeApi = FakeAuthApiService()
        val fakeStore = FakeTokenStore()
        val repository = AuthRepositoryImpl(fakeApi, fakeStore)

        try {
            runBlocking {
                repository.register("user@healthos.app", "123", Role.PATIENT, "Alex", "Dev")
            }
            fail("Expected exception for short password")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("9 y 128 caracteres") == true)
        }
    }
}
