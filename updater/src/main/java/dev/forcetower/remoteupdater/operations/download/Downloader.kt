package dev.forcetower.remoteupdater.operations.download

import dev.forcetower.remoteupdater.operations.Result
import okhttp3.OkHttpClient
import java.io.File

interface Downloader {
    suspend fun execute(client: OkHttpClient, url: String, outFile: File): Result<Unit>
}