package dev.forcetower.oversee.testing

class OverImpl : OverContract {
    override suspend fun method1(a: Int, b: Int): Int {
        println("Now adding $a, $b")
        return a + b
    }
}