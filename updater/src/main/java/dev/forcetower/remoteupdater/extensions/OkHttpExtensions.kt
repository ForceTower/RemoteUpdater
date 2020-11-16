package dev.forcetower.remoteupdater.extensions
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import okio.Buffer
import okio.Okio
import okio.buffer
import okio.sink
import java.io.File
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun Call.executeSuspend() = suspendCancellableCoroutine<Response> { continuation ->
    this.enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            continuation.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
            continuation.resume(response)
        }
    })
    
    continuation.invokeOnCancellation { this.cancel() }
}

suspend fun Call.downloadAndSaveTo(
    output: File,
    bufferSize: Long = DEFAULT_BUFFER_SIZE.toLong(),
    blockingDispatcher: CoroutineDispatcher = Dispatchers.IO,
    progress: ((downloaded: Long, total: Long) -> Unit)? = null
): File = withContext(blockingDispatcher) {
    suspendCancellableCoroutine { cont ->
        cont.invokeOnCancellation {
            cancel()
        }
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                cont.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    cont.resumeWithException(IOException("Unexpected HTTP code: ${response.code}"))
                    return
                }
                try {
                    val body = response.body
                    if (body == null) {
                        cont.resumeWithException(IllegalStateException("Body is null"))
                        return
                    }
                    val contentLength = body.contentLength()
                    val buffer = Buffer()
                    var finished = false
                    output.sink().buffer().use { out ->
                        body.source().use { source ->
                            var totalLength = 0L
                            while (cont.isActive) {
                                val read = source.read(buffer, bufferSize)
                                if (read == -1L) {
                                    finished = true
                                    break
                                }
                                out.write(buffer, read)
                                out.flush()
                                totalLength += read
                                progress?.invoke(totalLength, contentLength)
                            }
                        }
                    }
                    if (finished) {
                        cont.resume(output)
                    } else {
                        cont.resumeWithException(IOException("Download cancelled"))
                    }
                } catch (e: Exception) {
                    cont.resumeWithException(e)
                }
            }
        })
    }
}