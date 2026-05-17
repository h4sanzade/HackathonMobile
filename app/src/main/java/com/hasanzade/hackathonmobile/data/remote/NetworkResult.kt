package com.hasanzade.hackathonmobile.data.remote

sealed class NetworkResult<out T> {
    data class Success<T>(val data: T)  : NetworkResult<T>()
    data class Error(
        val message: String,
        val code: Int = -1
    )                                   : NetworkResult<Nothing>()
    object Loading                      : NetworkResult<Nothing>()
}

suspend fun <T> safeApiCall(
    call: suspend () -> retrofit2.Response<T>
): NetworkResult<T> {
    return try {
        val response = call()
        when {
            response.isSuccessful && response.body() != null ->
                NetworkResult.Success(response.body()!!)

            response.code() == 401 ->
                NetworkResult.Error("Sessiyanız bitib.", 401)

            response.code() == 400 -> {
                val errorBody = response.errorBody()?.string() ?: ""
                android.util.Log.e("API_ERROR", "400: $errorBody")
                val msg = try {
                    org.json.JSONObject(errorBody)
                        .optString("error", "Yanlış məlumat")
                } catch (e: Exception) { errorBody.take(100) }
                NetworkResult.Error(msg, 400)
            }

            response.code() == 500 ->
                NetworkResult.Error("Server xətası.", 500)

            else -> {
                val errorBody = response.errorBody()?.string() ?: ""
                android.util.Log.e("API_ERROR", "${response.code()}: $errorBody")
                NetworkResult.Error("Xəta ${response.code()}", response.code())
            }
        }
    } catch (e: java.net.SocketTimeoutException) {
        NetworkResult.Error("Bağlantı vaxtı bitdi.")
    } catch (e: java.net.UnknownHostException) {
        NetworkResult.Error("İnternet bağlantısı yoxdur.")
    } catch (e: Exception) {
        NetworkResult.Error(e.localizedMessage ?: "Bilinməyən xəta.")
    }
}