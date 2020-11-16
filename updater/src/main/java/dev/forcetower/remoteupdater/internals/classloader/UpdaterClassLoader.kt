package dev.forcetower.remoteupdater.internals.classloader

import dalvik.system.DexClassLoader
import java.io.File
import java.lang.reflect.Field

class UpdaterClassLoader private constructor(parent: ClassLoader) : ClassLoader(parent) {
    private val mAppClassLoader: ClassLoader

    init {
        mAppClassLoader = APP_CLASS_LOADER
    }

    private val addedLoaders = mutableListOf<ClassLoader>()

    fun installDex(dexFile: File, optimizedDexDir: File) {
        val loader = DexClassLoader(dexFile.absolutePath, optimizedDexDir.absolutePath, null, mAppClassLoader)
        addedLoaders.add(loader)
    }

    override fun findClass(name: String?): Class<*> {
        for (loader in addedLoaders) {
            try {
                val foundClass = loader.loadClass(name)
                if (foundClass != null) {
                    return foundClass
                }
            } catch (error: Throwable) {}
        }
        return parent.loadClass(name)
    }

    companion object {
        private val APP_CLASS_LOADER: ClassLoader = UpdaterClassLoader::class.java.classLoader!!
        private val SYSTEM_CLASSLOADER: ClassLoader
        private val CLASSLOADER_PARENT_FIELD: Field = ClassLoader::class.java.getDeclaredField("parent")
        private var sInstalledClassLoader: UpdaterClassLoader? = null

        init {
            CLASSLOADER_PARENT_FIELD.isAccessible = true
            SYSTEM_CLASSLOADER = CLASSLOADER_PARENT_FIELD.get(APP_CLASS_LOADER) as ClassLoader
            println("Running Updater Class loader")
        }

        @get:Synchronized
        val instance: UpdaterClassLoader
            get() {
                install()
                return sInstalledClassLoader!!
            }

        @Synchronized
        private fun install() {
            if (sInstalledClassLoader != null) {
                return
            }
            try {
                val cl = UpdaterClassLoader(APP_CLASS_LOADER)
                sInstalledClassLoader = cl
            } catch (e: IllegalAccessException) {
                throw RuntimeException(e)
            }
        }
    }
}