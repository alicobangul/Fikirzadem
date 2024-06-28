package com.basesoftware.fikirzadem.presentation.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.basesoftware.fikirzadem.R
import com.basesoftware.fikirzadem.model.PostModel
import com.basesoftware.fikirzadem.util.WorkUtil.systemLanguage

class InterestViewModel (private val state : SavedStateHandle)  : ViewModel()  {

    private val _fragmentResumeLiveData = MutableLiveData(true)
    fun getFragmentResume() : MutableLiveData<Boolean> = _fragmentResumeLiveData
    fun setFragmentResume(value : Boolean) { getFragmentResume().value = value }

    fun getInterestPostList() : ArrayList<PostModel> = state.get("_interestPostList") ?: arrayListOf()
    fun addInterestPostList(value : PostModel) { state.set("_interestPostList", getInterestPostList().apply { add(value) }) }
    fun setNewInterestPost(value : PostModel) { state.set("_interestPostList", getInterestPostList().apply { add(0,value) }) }
    fun clearInterestPostList() = state.set("_interestPostList", arrayListOf<PostModel>())

    fun getCategory() : Int {
        return state.get<Int>("_category") ?: if (systemLanguage().matches(Regex("tr"))) R.id.category_shop else R.id.category_animals
    }

    fun setCategory(value : Int) = state.set("_category", value)

    fun getLoadingInterest() : Boolean = state.get("_loadingInterest") ?: true
    fun setLoadingInterest(value : Boolean) { state.set("_loadingInterest", value) }

    fun getDataDownloading() : Boolean = state.get("_dataDownloading") ?: false
    fun setDataDownloading(value : Boolean) { state.set("_dataDownloading", value) }

}