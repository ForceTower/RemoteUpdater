package dev.forcetower.remoteupdater.internals.tasks

import android.content.Context
import dev.forcetower.remoteupdater.internals.classloader.UpdaterClassLoader
import dev.forcetower.remoteupdater.internals.database.UpdaterDB
import dev.forcetower.remoteupdater.model.UpdateDex
import dev.forcetower.remoteupdater.model.UpdateVersion
import dev.forcetower.remoteupdater.operations.Result
import dev.forcetower.remoteupdater.operations.checker.UpdateChecker
import dev.forcetower.remoteupdater.operations.download.Downloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File
import java.util.UUID

class UpdateTask (
    private val context: Context,
    private val module: String,
    private val client: OkHttpClient,
    private val database: UpdaterDB,
    private val updateUrl: String,
    private val initialArtifact: File?,
    private val checker: UpdateChecker,
    private val downloader: Downloader
) {
    private suspend fun check() = checker.execute(client, updateUrl)
    private suspend fun download(url: String, file: File) = downloader.execute(client, url, file)

    /**
     * Returns whether an update happened or not
     */
    suspend fun execute(): Result<Boolean> = withContext(Dispatchers.IO) {
        val checkResult = check()
        if (checkResult is Result.Error) {
            println("Checker result failed")
            return@withContext Result.Error(checkResult.error, checkResult.code)
        }

        val check = checkResult as Result.Success

        val serverUpdate = check.value
        val serverVersion = serverUpdate.version
        val currentVersion = database.updateVersionDao().getUpdateInfo(module)?.version ?: 0

        if (serverVersion <= currentVersion) {
            println("Current version is up to date")
            loadCurrentVersion()
            return@withContext Result.Success(false)
        } else {
            downloadNewVersion(currentVersion, serverUpdate)
        }
    }

    private suspend fun loadCurrentVersion() {
        val current = database.updateDexDao().getCurrentDex(module)
            ?: throw IllegalStateException("not valid cached dex information")

        val dexFile = File(current.dexPath)
        val optimizedDexFile = File(current.optimizedDexPath)

        if (!dexFile.exists()) throw IllegalStateException("not valid common dex information")
        UpdaterClassLoader.instance.installDex(dexFile, optimizedDexFile)
    }

    private suspend fun downloadNewVersion(currentVersion: Int, serverUpdate: UpdateVersion): Result<Boolean> {
        println("Downloading next version. From $currentVersion to ${serverUpdate.version}")
        val downloadName = serverUpdate.downloadUrl.split("/").last().split("?").first()
        val child = "updater/${UUID.randomUUID().toString().substring(0..7)}"
        val dexDir = File(context.cacheDir, child)
        dexDir.mkdirs()
        val dexFile = File(dexDir, downloadName)
        println(dexFile.absolutePath)
        val downloadResult = download(serverUpdate.downloadUrl, dexFile)

        if (downloadResult is Result.Error) {
            return Result.Error(downloadResult.error, downloadResult.code)
        }

        val optimizedDexDir = context.getDir("outdex", Context.MODE_PRIVATE)
        val optimizedDexInnerDir = File(optimizedDexDir, child)
        optimizedDexInnerDir.mkdirs()
        val optimizedDexFile = File(optimizedDexInnerDir, downloadName)

        UpdaterClassLoader.instance.installDex(dexFile, optimizedDexFile)

        println("dexFile.absolutePath: ${dexFile.absolutePath}")
        println("optimizedDexFile.absolutePath: ${optimizedDexFile.absolutePath}")

        database.updateVersionDao().insert(serverUpdate)
        database.updateDexDao().insert(UpdateDex(module, dexFile.absolutePath, optimizedDexFile.absolutePath))
        return Result.Success(true)
    }
}