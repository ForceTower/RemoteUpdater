package dev.forcetower.android.lib

import android.content.Context

class AndroidLibImpl : AndroidLibContract {
    override suspend fun reallyImportantMethod(context: Context, value: Int): String {
        return "The usage is $value at ${context.packageName}"
    }
}