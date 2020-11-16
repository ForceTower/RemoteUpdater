package dev.forcetower.remoteupdater

import android.content.Context
import androidx.annotation.IntDef
import androidx.room.Room
import dev.forcetower.remoteupdater.internals.classloader.UpdaterClassLoader
import dev.forcetower.remoteupdater.internals.database.UpdaterDB
import dev.forcetower.remoteupdater.internals.tasks.UpdateTask
import dev.forcetower.remoteupdater.model.UpdateVersion
import dev.forcetower.remoteupdater.operations.checker.UpdateCheckerImpl
import dev.forcetower.remoteupdater.operations.download.DownloaderImpl
import okhttp3.OkHttpClient
import java.io.File
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.coroutines.Continuation

typealias SuspendInvoker = suspend (method: Method, arguments: List<Any?>) -> Any?

private interface SuspendFunction {
    suspend fun invoke(): Any?
}

private val SuspendRemover = SuspendFunction::class.java.methods[0]

class Updater<T : Any> private constructor(
    private val module: String,
    private val context: Context,
    private val service: Class<T>,
    private val constructorTypes: Array<out Class<*>>,
    private val constructorArgs: Array<out Any>,
    @UpdateType private val updateType: Int,
    private val initialArtifact: File?,
    private val updateUrl: String,
    private val initialVersion: Int,
    private val database: UpdaterDB,
    private val client: OkHttpClient
) {
    private lateinit var implementation: T

    @Suppress("UNCHECKED_CAST")
    suspend fun create(concrete: String): T {
        performCreateOfInitialVersion()

        if (updateType == EAGER) {
            performUpdate()
            instantiateConcrete(concrete)
        }

        val proxy = Proxy.newProxyInstance(service.classLoader, arrayOf(service)) { _, method, args ->
            println("Invoke method ${method.name}")
            val continuation = args.last() as Continuation<*>
//            val argumentsWithoutContinuation = args.take(args.size - 1).toTypedArray()

            return@newProxyInstance SuspendRemover.invoke(object : SuspendFunction {
                override suspend fun invoke(): Any? {
                    onPreMethodExecution(concrete)
                    return method.invoke(implementation, *args)
                }
            }, continuation)

//            if (args != null) {
//                return@newProxyInstance method.invoke(implementation, *args)
//            } else {
//                return@newProxyInstance method.invoke(implementation)
//            }
        }
        return proxy as T
    }

    private suspend fun onPreMethodExecution(concrete: String) {
        if (!::implementation.isInitialized) {
            if (updateType == LAZY) {
                performUpdate()
            }
            instantiateConcrete(concrete)
        }
    }

    private fun instantiateConcrete(concrete: String) {
        val constructor = findMatchingConstructorOrFail(concrete)
        implementation = constructor.newInstance(*constructorArgs)
    }

    private suspend fun performCreateOfInitialVersion() {
        val current = database.updateVersionDao().getUpdateInfo(module)
        if (current == null) {
            database.updateVersionDao().insert(
                UpdateVersion(module, initialVersion, "", "")
            )
        } else if (current.version < initialVersion) {
            database.updateVersionDao().insert(current.copy(version = initialVersion))
        }
    }

    private suspend fun performUpdate() {
        // TODO Make this more composable.
        // The final goal if it works is to make a Factory out of this
        val checker = UpdateCheckerImpl()
        val downloader = DownloaderImpl()
        println(UpdateTask(context, module, client, database, updateUrl, initialArtifact, checker, downloader).execute())
    }

    @Suppress("UNCHECKED_CAST")
    private fun findMatchingConstructorOrFail(concrete: String): Constructor<out T> {
        try {
            val clazz = UpdaterClassLoader.instance.loadClass(concrete) as Class<out T>
//            val clazz = Class.forName(concrete) as Class<out T>
            return clazz.getDeclaredConstructor(*constructorTypes)
        } catch (exception: NoSuchMethodException) {
            throw NoSuchMethodException("No constructor matching this call")
        }
    }

    class Builder<T: Any> (
        private val module: String,
        private val context: Context,
        private val service: Class<T>
    ) {
        @UpdateType private var updateType: Int = EAGER
        private var constructorArgs: Array<out Any> = emptyArray()
        private var constructorTypes: Array<out Class<*>> = emptyArray()
        private var updateUrl: String? = null
        private var initialVersion: Int = 1
        private var client: OkHttpClient? = null
        private var initialArtifact: File? = null

        init {
            if (!service.isInterface)
                throw IllegalArgumentException("Contract declaration must be an interface.");
        }

        fun client(client: OkHttpClient): Builder<T> {
            this.client = client
            return this
        }

        fun initialVersion(version: Int): Builder<T> {
            initialVersion = version
            return this
        }

        fun updateUrl(url: String): Builder<T> {
            updateUrl = url
            return this
        }

        fun updateType(@UpdateType type: Int): Builder<T> {
            updateType = type
            return this
        }

        fun initialArtifact(file: File): Builder<T> {
            initialArtifact = file
            return this
        }

        fun constructorArguments(vararg args: Any): Builder<T> {
            constructorTypes = args.map {
                when (it) {
                    is Boolean -> Boolean::class.java
                    is Byte -> Byte::class.java
                    is Char -> Char::class.java
                    is Double -> Double::class.java
                    is Float -> Float::class.java
                    is Int -> Int::class.java
                    is Long -> Long::class.java
                    is Short -> Short::class.java
                    else -> it.javaClass
                }
            }.toTypedArray()
            constructorArgs = args
            return this
        }

        fun build(): Updater<T> {
            println(this.updateUrl)
            val updateUrl = this.updateUrl ?: throw IllegalStateException("Update url must be set to a non null value.")

            val database = Room.databaseBuilder(context, UpdaterDB::class.java, "updater.db")
                .allowMainThreadQueries()
                .build()

            val client = this.client ?: OkHttpClient.Builder().build()

            return Updater(
                module,
                context,
                service,
                constructorTypes,
                constructorArgs,
                updateType,
                initialArtifact,
                updateUrl,
                initialVersion,
                database,
                client
            )
        }
    }

    companion object {
        // Be aware that eager updates will block caller thread until download is finished
        const val EAGER = 0
        // Lazy updates will block caller thread until download is finished
        const val LAZY = 1
        // Will load default shipped code, and will only update when you explicitly request the update
        const val MANUAL = 3

        @IntDef(value = [EAGER, LAZY, MANUAL])
        @Retention(AnnotationRetention.SOURCE)
        annotation class UpdateType
    }
}