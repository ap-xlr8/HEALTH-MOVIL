package com.healthos

import com.healthos.data.remote.AuthApiService
import com.healthos.data.remote.ForgotPasswordRequestDto
import com.healthos.data.remote.HealthProfileRequestDto
import com.healthos.data.remote.LoginRequestDto
import com.healthos.data.remote.LoginResponseDto
import com.healthos.data.remote.LoginResponseWrapperDto
import com.healthos.data.remote.RegisterRequestDto
import com.healthos.data.remote.RegisterResponseDto
import com.healthos.data.remote.RegisterResponseWrapperDto
import com.healthos.data.remote.VerifyEmailRequestDto
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

class FakeTokenStore : SecureTokenStore {
    var savedAccess: String? = null
    var savedRefresh: String? = null
    var savedRole: String? = null

    override fun save(accessToken: String, refreshToken: String, role: String) {
        savedAccess = accessToken
        savedRefresh = refreshToken
        savedRole = role
    }

    override fun accessToken(): String? = savedAccess
    override fun refreshToken(): String? = savedRefresh
    override fun role(): String? = savedRole
    override fun clear() {
        savedAccess = null
        savedRefresh = null
        savedRole = null
    }
}

class FakeAuthApiService : AuthApiService {
    var shouldFailLogin = false
    var shouldFailRegister = false

    override suspend fun register(request: RegisterRequestDto): Response<RegisterResponseWrapperDto> {
        if (shouldFailRegister) {
            val errorBody = "{\"error\": \"User exists\"}".toResponseBody("application/json".toMediaTypeOrNull())
            return Response.error(400, errorBody)
        }
        return Response.success(
            RegisterResponseWrapperDto(
                data = RegisterResponseDto(
                    id = "USR-001",
                    email = request.email,
                    firstName = request.firstName,
                    lastName = request.lastName,
                )
            )
        )
    }

    override suspend fun login(request: LoginRequestDto): Response<LoginResponseWrapperDto> {
        if (shouldFailLogin) {
            val errorBody = "{\"error\": \"Unauthorized\"}".toResponseBody("application/json".toMediaTypeOrNull())
            return Response.error(401, errorBody)
        }
        return Response.success(
            LoginResponseWrapperDto(
                data = LoginResponseDto(
                    accessToken = "access_token_123",
                    refreshToken = "refresh_token_123",
                    role = if (request.email.contains("cuidador")) "caregiver" else "patient",
                )
            )
        )
    }

    override suspend fun verifyEmail(request: VerifyEmailRequestDto): Response<Map<String, Any>> =
        Response.success(mapOf("success" to true))

    override suspend fun forgotPassword(request: ForgotPasswordRequestDto): Response<Map<String, Any>> =
        Response.success(mapOf("success" to true))

    override suspend fun saveHealthProfile(request: HealthProfileRequestDto): Response<Map<String, Any>> =
        Response.success(mapOf("success" to true))
}

class AuthRepositoryTest {
    @Test
    fun login_successful_returnsValidSessionAndPersistsToken() = runBlocking {
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
    fun login_unauthorized_throwsIllegalArgumentException() = runBlocking {
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
            assertTrue(e.message?.contains("8 caracteres") == true)
        }
    }
}
