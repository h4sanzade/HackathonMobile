package com.hasanzade.hackathonmobile.data.remote

import com.hasanzade.hackathonmobile.data.local.TokenDataStore
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class LoginRequest(val userId: String, val password: String)

data class LoginResponse(
    val accessToken: String,
    val expiresInSeconds: Long,
    val role: String,
    val displayName: String,
    val filial: String,
    val department: String?,
    val allDepartments: Boolean
)

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val code: Int = -1) : Result<Nothing>()
}

class AuthRepository(private val tokenDataStore: TokenDataStore) {

    private val BASE_URL = "https://irrefragably-overcured-clyde.ngrok-free.dev"

    suspend fun login(userId: String, password: String): Result<LoginResponse> {
        return try {
            val url = URL("$BASE_URL/api/auth/login")
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("ngrok-skip-browser-warning", "true")
                setRequestProperty("User-Agent", "HackathonMobile/1.0")
                doOutput = true
                connectTimeout = 15_000
                readTimeout = 15_000
            }

            val body = JSONObject().apply {
                put("userId", userId)
                put("password", password)
            }.toString()

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use {
                it.write(body)
                it.flush()
            }

            val code = connection.responseCode
            val responseText = if (code == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader(Charsets.UTF_8).readText()
            } else {
                connection.errorStream?.bufferedReader(Charsets.UTF_8)?.readText() ?: ""
            }

            android.util.Log.d("AUTH", "code=$code body=$responseText")

            if (code == HttpURLConnection.HTTP_OK) {
                val json = JSONObject(responseText)
                val response = LoginResponse(
                    accessToken    = json.getString("accessToken"),
                    expiresInSeconds = json.getLong("expiresInSeconds"),
                    role           = json.getString("role"),
                    displayName    = json.getString("displayName"),
                    filial         = json.getString("filial"),
                    department     = json.optString("department")
                        .takeIf { it.isNotEmpty() && it != "null" },
                    allDepartments = json.getBoolean("allDepartments")
                )

                // Token-i DataStore-a saxla
                tokenDataStore.saveSession(
                    token       = response.accessToken,
                    role        = response.role,
                    userId      = userId,
                    displayName = response.displayName,
                    filial      = response.filial,
                    department  = response.department
                )

                Result.Success(response)
            } else {
                Result.Error("HTTP $code: $responseText", code)
            }
        } catch (e: Exception) {
            android.util.Log.e("AUTH", "exception", e)
            Result.Error(e.localizedMessage ?: "Unknown error")
        }
    }

    suspend fun logout() {
        tokenDataStore.clear()
    }
}