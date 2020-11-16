package dev.forcetower.remoteupdater.example

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.net.toFile
import androidx.lifecycle.lifecycleScope
import dev.forcetower.android.lib.AndroidLibContract
import dev.forcetower.oversee.testing.OverContract
import dev.forcetower.remoteupdater.Updater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val test = true
        lifecycleScope.launch {
            if (test) {
                withContext(Dispatchers.IO) {
                    val service =
                        Updater.Builder("non_android", this@MainActivity, OverContract::class.java)
                            .updateUrl("http://192.168.15.2/api/test")
                            .updateType(Updater.EAGER)
                            .initialVersion(0)
                            .build()
                            .create("dev.forcetower.oversee.testing.OverImpl")

                    var result = service.method1(1, 2)
                    println("Result of loader $result")
                    result = service.method1(2, 5)
                    println("Result of loader $result")
                    result = service.method1(6, 10)
                    println("Result of loader $result")
                }
            } else {
                withContext(Dispatchers.IO) {
                    val service =
                        Updater.Builder("androidx", this@MainActivity, AndroidLibContract::class.java)
                            .updateUrl("http://192.168.15.2/api/test2")
                            .updateType(Updater.LAZY)
                            .initialVersion(0)
                            .build()
                            .create("dev.forcetower.android.lib.AndroidLibImpl")

                    val result = service.reallyImportantMethod(this@MainActivity, 100)
                    println("Result of loader [$result]")
                }
            }
        }
    }
}
