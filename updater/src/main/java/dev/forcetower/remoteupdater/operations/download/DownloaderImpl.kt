package dev.forcetower.remoteupdater.operations.download

import dev.forcetower.remoteupdater.extensions.downloadAndSaveTo
import dev.forcetower.remoteupdater.extensions.executeSuspend
import dev.forcetower.remoteupdater.operations.Result
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.IOException
import okio.buffer
import okio.sink
import java.io.File

class DownloaderImpl : Downloader {
    override suspend fun execute(client: OkHttpClient, url: String, outFile: File): Result<Unit> {
        val request = Request.Builder().url(url).build()
        val call = client.newCall(request)
        return try {
            if (outFile.exists()) outFile.delete()
            outFile.createNewFile()
            call.downloadAndSaveTo(outFile)
            Result.Success(Unit)
        } catch (exception: IOException) {
            exception.printStackTrace()
            Result.Error(exception, 500)
        }
    }
}