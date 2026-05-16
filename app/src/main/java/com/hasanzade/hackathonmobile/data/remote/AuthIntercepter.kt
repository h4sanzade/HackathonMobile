package com.hasanzade.hackathonmobile.data.remote

import com.hasanzade.hackathonmobile.data.local.TokenDataStore
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val tokenDataStore: TokenDataStore) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { tokenDataStore.getToken() }

        val request = chain.request().newBuilder().apply {
            if (token != null) {
                addHeader("Authorization", "Bearer $token")
            }
            addHeader("ngrok-skip-browser-warning", "true")
            addHeader("User-Agent", "HackathonMobile/1.0")
            addHeader("Content-Type", "application/json")
            addHeader("Accept", "application/json")
        }.build()

        return chain.proceed(request)
    }
}