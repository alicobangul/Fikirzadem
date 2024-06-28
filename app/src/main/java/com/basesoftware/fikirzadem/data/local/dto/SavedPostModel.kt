package com.basesoftware.fikirzadem.data.local.dto

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "SavedPost")
data class SavedPostModel(

    @PrimaryKey(autoGenerate = false)
    @NonNull var id: String,

    @ColumnInfo(name = "PostId")
    val postId: String?,

    @ColumnInfo(name = "SaveUserId")
    val saveUserId: String?,

    @ColumnInfo(name = "CategoryId")
    val categoryId: Int?,

    @ColumnInfo(name = "PostContent")
    val postContent: String?,

    @ColumnInfo(name = "PostDate")
    val postDate: Long,

    @ColumnInfo(name = "SaveDate")
    var saveDate: Long
)
