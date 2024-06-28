package com.basesoftware.fikirzadem.data.local

import androidx.paging.PagingSource
import androidx.room.*
import com.basesoftware.fikirzadem.data.local.dto.SavedPostModel

@Dao
interface FikirzademDao {

    @Query("SELECT * FROM SavedPost WHERE SaveUserId IN (:savedUser) ORDER BY SaveDate DESC")
    fun getAllSavedPost(savedUser: String) : PagingSource<Int, SavedPostModel>

    @Query("SELECT * FROM SavedPost WHERE SaveUserId IN (:savedUser) AND PostId IN (:postId)")
    fun getSavedPost(savedUser : String, postId : String) : SavedPostModel?

    @Query("SELECT COUNT(*) FROM SavedPost WHERE SaveUserId IN (:savedUser)")
    fun getSavedPostSize(savedUser: String) : Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addSavedPost(savedPostModel : SavedPostModel)

    @Query("DELETE FROM SavedPost WHERE SaveUserId IN (:savedUser) AND PostId IN (:postId)")
    fun deleteSavedPostWithId(savedUser : String, postId : String)

    @Query("DELETE FROM SavedPost")
    fun deleteAllSavedPost()

}