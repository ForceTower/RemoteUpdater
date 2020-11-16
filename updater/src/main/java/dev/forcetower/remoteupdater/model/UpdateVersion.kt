package dev.forcetower.remoteupdater.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

/**
 * This class represents an updatable entity (module).
 * It will store the module name, version, hash and downloadUrl.
 *
 * The name of each module should be unique.
 */
@Entity(tableName = "updatable_names")
data class UpdateVersion (
    /**
     * The module name, this is treated as the unique id for the module
     */
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "name")
    @SerializedName("name")
    val name: String,
    /**
     * The module version. This will determine if the module should be updated or not
     */
    @ColumnInfo(name = "version")
    @SerializedName("version")
    val version: Int,
    /**
     * The module hash. This will ensure that the downloaded data is the same as the server
     */
    @ColumnInfo(name = "hash")
    @SerializedName("hash")
    val hash: String,
    /**
     * The module download url. This should be the download path to the .dex or .apk
     */
    @ColumnInfo(name = "download_url")
    @SerializedName("download_url")
    val downloadUrl: String
)