package dev.forcetower.android.lib

import android.content.Context

interface AndroidLibContract {
    suspend fun reallyImportantMethod(context: Context, value: Int): String
}