package com.basesoftware.fikirzadem.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.room.Room
import com.basesoftware.fikirzadem.data.local.dto.SavedPostModel
import com.basesoftware.fikirzadem.data.local.FikirzademDatabase
import kotlinx.coroutines.flow.Flow

class FavoriteViewModel(app : Application) : AndroidViewModel(app) {

    private var user = "-"
    fun setUser(value : String) { user = value }

    private val _fragmentResumeLiveData = MutableLiveData(true)
    fun getFragmentResume() : MutableLiveData<Boolean> = _fragmentResumeLiveData
    fun setFragmentResume(value : Boolean) { getFragmentResume().value = value }

    private val dao = Room.databaseBuilder(app, FikirzademDatabase::class.java, "FikirzademDB").build().fikirzademDao()

    fun getDbData() : Flow<PagingData<SavedPostModel>> {
        return Pager(PagingConfig(pageSize = 15)) { dao.getAllSavedPost(user) }.flow.cachedIn(viewModelScope)
    }

}