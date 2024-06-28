package com.basesoftware.fikirzadem.presentation.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.basesoftware.fikirzadem.model.PostModel


class FeedViewModel (private val state : SavedStateHandle) : ViewModel() {

    private val _fragmentResumeLiveData = MutableLiveData(true)
    fun getFragmentResume() : MutableLiveData<Boolean> = _fragmentResumeLiveData
    fun setFragmentResume(value : Boolean) { getFragmentResume().value = value }

    fun getScrollCountForAd() : Int = state.get("_scrollCountForAd") ?: 1
    fun addScrollCountForAd() = state.set("_scrollCountForAd", getScrollCountForAd() + 1)
    fun defaultScrollCountForAd() = state.set("_scrollCountForAd", 1)

    fun getPostList() : ArrayList<PostModel> = state.get("_arrayPostList") ?: arrayListOf()
    fun setPost(value : PostModel) { state.set("_arrayPostList", getPostList().apply { add(value) }) }
    fun setNewPost(value : PostModel) { state.set("_arrayPostList", getPostList().apply { add(0,value) }) }


    fun getIsRefreshing() : Boolean = state.get("_isRefreshing") ?: false
    fun setIsRefreshing(value : Boolean) = state.set("_isRefreshing", value)


    fun getLoadingFeed() : Boolean = state.get("_loadingFeed") ?: true
    fun setLoadingFeed(value : Boolean) { state.set("_loadingFeed", value) }

    fun getDataDownloading() : Boolean = state.get("_dataDownloading") ?: false
    fun setDataDownloading(value : Boolean) { state.set("_dataDownloading", value) }

}