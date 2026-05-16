package com.hasanzade.hackathonmobile.data.remote

sealed class NetworkResult<out T> {
    data class Success<T>(val data: T)     : NetworkResult<T>()
    data class Error(
        val message: String,
        val code: Int = -1
    )                                      : NetworkResult<Nothing>()
    object Loading                         : NetworkResult<Nothing>()
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
                NetworkResult.Error("Sessiyanız bitib. Yenidən daxil olun.", 401)
            response.code() == 400 ->
                NetworkResult.Error("Yanlış məlumat göndərildi.", 400)
            response.code() == 500 ->
                NetworkResult.Error("Server xətası. Bir az sonra cəhd edin.", 500)
            else ->
                NetworkResult.Error("Xəta: ${response.code()}", response.code())
        }
    } catch (e: java.net.SocketTimeoutException) {
        NetworkResult.Error("Bağlantı vaxtı bitdi. İnternetinizi yoxlayın.")
    } catch (e: java.net.UnknownHostException) {
        NetworkResult.Error("İnternet bağlantısı yoxdur.")
    } catch (e: Exception) {
        NetworkResult.Error(e.localizedMessage ?: "Bilinməyən xəta baş verdi.")
    }
}