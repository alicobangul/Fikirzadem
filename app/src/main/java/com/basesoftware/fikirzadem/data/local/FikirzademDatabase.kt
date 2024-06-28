package com.basesoftware.fikirzadem.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.basesoftware.fikirzadem.data.local.dto.SavedPostModel

@Database(entities = [SavedPostModel::class], version = 1)
abstract class FikirzademDatabase : RoomDatabase() { abstract fun fikirzademDao(): FikirzademDao }