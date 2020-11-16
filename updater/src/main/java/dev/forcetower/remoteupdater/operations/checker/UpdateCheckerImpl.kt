package dev.forcetower.remoteupdater.operations.checker

import com.google.gson.Gson
import dev.forcetower.remoteupdater.extensions.executeSuspend
import dev.forcetower.remoteupdater.operations.Result
import dev.forcetower.remoteupdater.model.UpdateVersion
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * Returns whether the library should update or not
 * Network errors will be treated as a "no-update"
 */
class UpdateCheckerImpl : UpdateChecker {
    override suspend fun execute(client: OkHttpClient, updateUrl: String): Result<UpdateVersion> {
        val request = Request.Builder().url(updateUrl).build()
        val call = client.newCall(request)
        return try {
            val response = call.executeSuspend()
            if (response.isSuccessful) {
                // TODO This call blocks suspend
                val json = response.peekBody(Long.MAX_VALUE).string()
                val version = Gson().fromJson(json, UpdateVersion::class.java)
                Result.Success(version)
            } else {
                Result.Error(IOException("Unsuccessful response"), response.code)
            }
        } catch (throwable: Throwable) {
            Result.Error(throwable, 500)
        }
    }
}