package dev.forcetower.remoteupdater.example

class NonAndroidServiceImpl constructor(
    private val second: Int,
    private val third: String,
    private val fourth: IntArray
): NonAndroidService {
    override fun method1() {
        println("On Impl: Exec Method 1")
    }

    override fun method2(): Int {
        println("On Impl: Exec Method 2")
        return second
    }

    override fun method3(): String {
        println("On Impl: Exec Method 3")
        return third
    }

    override fun method4(): String {
        println("On Impl: Exec Method 4")
        return fourth.contentToString()
    }

    override fun method5(param: String): String {
        println("On Impl: Exec Method 5")
        return "Wow! Its amazing! $param is here!"
    }
}