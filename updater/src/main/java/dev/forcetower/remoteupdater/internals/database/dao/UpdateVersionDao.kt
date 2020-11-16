package dev.forcetower.remoteupdater.internals.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.forcetower.remoteupdater.model.UpdateVersion

@Dao
abstract class UpdateVersionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(value: UpdateVersion)

    @Query("SELECT * FROM updatable_names WHERE name = :name")
    abstract suspend fun getUpdateInfo(name: String): UpdateVersion?
}