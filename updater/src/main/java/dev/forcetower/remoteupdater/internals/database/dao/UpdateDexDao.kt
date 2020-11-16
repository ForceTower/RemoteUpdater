package dev.forcetower.remoteupdater.internals.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.forcetower.remoteupdater.model.UpdateDex

@Dao
interface UpdateDexDao {
    @Query("SELECT * FROM dex_files WHERE name = :name")
    suspend fun getCurrentDex(name: String): UpdateDex?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(value: UpdateDex)
}