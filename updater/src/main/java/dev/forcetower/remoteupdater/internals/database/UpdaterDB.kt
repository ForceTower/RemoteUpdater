package dev.forcetower.remoteupdater.internals.database

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.forcetower.remoteupdater.internals.database.dao.UpdateDexDao
import dev.forcetower.remoteupdater.internals.database.dao.UpdateVersionDao
import dev.forcetower.remoteupdater.model.UpdateDex
import dev.forcetower.remoteupdater.model.UpdateVersion

@Database(entities = [
    UpdateVersion::class,
    UpdateDex::class
], version = 1, exportSchema = false)
abstract class UpdaterDB : RoomDatabase() {
    abstract fun updateVersionDao(): UpdateVersionDao
    abstract fun updateDexDao(): UpdateDexDao
}