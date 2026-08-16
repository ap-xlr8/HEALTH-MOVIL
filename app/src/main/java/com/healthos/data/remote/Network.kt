package com.healthos.data.remote

import com.healthos.BuildConfig
import com.healthos.security.SecureTokenStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.CertificatePinner
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthHeaderInterceptor
    @Inject
    constructor(
        private val secureTokenStore: SecureTokenStore,
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain) =
            chain.proceed(
                chain.request().newBuilder().apply {
                    secureTokenStore.accessToken()?.let { header("Authorization", "Bearer $it") }
                    header("Accept", "application/json")
                }.build(),
            )
    }

@Singleton
class TokenAuthenticator
    @Inject
    constructor(
        private val secureTokenStore: SecureTokenStore,
    ) : okhttp3.Authenticator {
        private val lock = Any()
        private val refreshClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .apply { if (!BuildConfig.DEBUG) certificatePinner(HEALTHOS_CERTIFICATE_PINNER) }
                .build()
        }
        private val moshi by lazy {
            Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()
        }

        override fun authenticate(route: okhttp3.Route?, response: okhttp3.Response): okhttp3.Request? {
            if (response.priorResponse != null && response.priorResponse?.code == 401) {
                return null // Avoid multiple retry loops
            }
            val currentToken = secureTokenStore.accessToken()
            val headerToken = response.request.header("Authorization")?.removePrefix("Bearer ")
            if (currentToken != null && currentToken != headerToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            synchronized(lock) {
                val latestToken = secureTokenStore.accessToken()
                if (latestToken != null && latestToken != headerToken) {
                    return response.request.newBuilder()
                        .header("Authorization", "Bearer $latestToken")
                        .build()
                }

                val refreshToken = secureTokenStore.refreshToken() ?: return null
                val baseUrl = if (BuildConfig.DEBUG) BuildConfig.API_BASE_URL_DEV else BuildConfig.API_BASE_URL_PROD
                val refreshUrl = "${baseUrl.ensureTrailingSlash()}v1/auth/refresh"

                val jsonBody = moshi.adapter(RefreshTokenRequestDto::class.java).toJson(RefreshTokenRequestDto(refreshToken))
                val request = okhttp3.Request.Builder()
                    .url(refreshUrl)
                    .post(jsonBody.toRequestBody("application/json".toMediaType()))
                    .header("Accept", "application/json")
                    .build()

                try {
                    val refreshResponse = refreshClient.newCall(request).execute()
                    if (refreshResponse.isSuccessful) {
                        val responseBody = refreshResponse.body?.string()
                        if (responseBody != null) {
                            val loginDto = moshi.adapter(LoginResponseDto::class.java).fromJson(responseBody)
                            if (loginDto != null) {
                                val role = loginDto.role ?: secureTokenStore.role() ?: "PATIENT"
                                val userId = secureTokenStore.userId()
                                secureTokenStore.save(loginDto.accessToken, loginDto.refreshToken, role, userId)
                                return response.request.newBuilder()
                                    .header("Authorization", "Bearer ${loginDto.accessToken}")
                                    .build()
                            }
                        }
                    } else if (refreshResponse.code == 401 || refreshResponse.code == 403) {
                        secureTokenStore.clear()
                    }
                } catch (_: Exception) {
                    // Network failure during refresh
                }
            }
            return null
        }
    }

@Singleton
class NetworkFactory
    @Inject
    constructor(
        private val authHeaderInterceptor: AuthHeaderInterceptor,
        private val tokenAuthenticator: TokenAuthenticator,
    ) {
        private val moshi: Moshi by lazy {
            Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()
        }

        fun retrofit(): Retrofit {
            val logging =
                HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.HEADERS else HttpLoggingInterceptor.Level.NONE
                    redactHeader("Authorization")
                    redactHeader("Cookie")
                }

            val clientBuilder =
                OkHttpClient.Builder()
                    .addInterceptor(authHeaderInterceptor)
                    .authenticator(tokenAuthenticator)
                    .addInterceptor(logging)
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)

            if (!BuildConfig.DEBUG) {
                clientBuilder.certificatePinner(HEALTHOS_CERTIFICATE_PINNER)
            }

            val baseUrl =
                if (BuildConfig.DEBUG) {
                    BuildConfig.API_BASE_URL_DEV
                } else {
                    BuildConfig.API_BASE_URL_PROD
                }

            return Retrofit.Builder()
                .baseUrl(baseUrl.ensureTrailingSlash())
                .client(clientBuilder.build())
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
        }
    }

private fun String.ensureTrailingSlash() = if (endsWith("/")) this else "$this/"

// Certificate pins for https://health-apis.onrender.com captured from the live
// TLS chain (leaf + intermediate + root). Keeping all three allows OkHttp to
// accept any valid chain ordering while still failing closed against MITM.
private val HEALTHOS_CERTIFICATE_PINNER: CertificatePinner =
    CertificatePinner.Builder()
        .add("health-apis.onrender.com", "sha256/BB7Exp9mdxl7TvHAZ0IRZPSyadon8vUwKSyruwUfwbE=")
        .add("health-apis.onrender.com", "sha256/oof/q3Ysxpom1IIDft9wH2U86JkCXGKn5cuIu5tBnLs=")
        .add("health-apis.onrender.com", "sha256/sIXXC5ZPGRpz5K8NVK56Dgeq/a+bcd0IYhOKtzJaJKI=")
        .build()
