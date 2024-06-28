package com.basesoftware.fikirzadem.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.basesoftware.fikirzadem.model.recycler.SearchRecyclerModel

class SearchViewModel(private val state : SavedStateHandle) : ViewModel() {

    fun getSearchList() : ArrayList<SearchRecyclerModel> = state.get("_searchList") ?: arrayListOf()
    fun setSearchUser(value : SearchRecyclerModel) { state.set("_searchList", getSearchList().apply { add(value) }) }
    fun searchListClear() = state.set("_searchList", arrayListOf<SearchRecyclerModel>())

    fun getSearchType() : String = state.get("_searchType") ?: "userName"
    fun setSearchType(value : String) = state.set("_searchType",value)

    fun getSearchText() : String = state.get("_searchText") ?: "x"
    fun setSearchText(value : String) = state.set("_searchText", value)

    fun getLastSearchUser() : String = state.get("_lastSearchUser") ?: "x"
    fun setLastSearchUser(value : String) = state.set("_lastSearchUser",value)

    fun getNewQuery() : Boolean = state.get("_newQuery") ?: true
    fun setNewQuery(value : Boolean) = state.set("_newQuery", value)

    fun getScrolling() : Boolean = state.get("_scrolling") ?: false
    fun setScrolling(value : Boolean) = state.set("_scrolling", value)
}