package com.healthos.data.remote

import com.healthos.BuildConfig
import com.healthos.security.SecureTokenStore
import okhttp3.CertificatePinner
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
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
class NetworkFactory
    @Inject
    constructor(
        private val authHeaderInterceptor: AuthHeaderInterceptor,
    ) {
        fun retrofit(): Retrofit {
            val logging =
                HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
                }
            val pinner =
                CertificatePinner.Builder()
                    .add("api.healthos.app", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
                    .add("staging.api.healthos.app", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
                    .build()
            val client =
                OkHttpClient.Builder()
                    .addInterceptor(authHeaderInterceptor)
                    .addInterceptor(logging)
                    .certificatePinner(pinner)
                    .build()
            return Retrofit.Builder()
                .baseUrl(BuildConfig.API_BASE_URL_DEV.ensureTrailingSlash())
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
        }
    }

private fun String.ensureTrailingSlash() = if (endsWith("/")) this else "$this/"
