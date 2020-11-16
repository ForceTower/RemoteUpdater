package dev.forcetower.remoteupdater.operations.checker

import dev.forcetower.remoteupdater.operations.Result
import dev.forcetower.remoteupdater.model.UpdateVersion
import okhttp3.OkHttpClient

interface UpdateChecker {
    suspend fun execute(client: OkHttpClient, updateUrl: String): Result<UpdateVersion>
}