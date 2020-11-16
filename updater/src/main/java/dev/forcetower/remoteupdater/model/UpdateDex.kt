package dev.forcetower.remoteupdater.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dex_files")
data class UpdateDex(
    @PrimaryKey
    val name: String,
    val dexPath: String,
    val optimizedDexPath: String
)