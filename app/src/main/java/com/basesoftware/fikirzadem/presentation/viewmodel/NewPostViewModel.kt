package com.basesoftware.fikirzadem.presentation.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.basesoftware.fikirzadem.R
import com.basesoftware.fikirzadem.util.WorkUtil.systemLanguage

class NewPostViewModel(private val state : SavedStateHandle) : ViewModel() {

    fun getUiEnable() : MutableLiveData<Boolean> = state.getLiveData("_uiEnable", true)
    fun getUiEnableValue() : Boolean = state.getLiveData("_uiEnable", true).value ?: true
    fun setUiEnable(value : Boolean) { state.getLiveData("_uiEnable", true).value = value }

    fun getMaxLength() : Int = state.get<Int>("_maxLength") ?: 350
    fun setMaxLength(value : Int) = state.set("_maxLength", value)

    fun getIsAddChar() : Boolean = state.get<Boolean>("_isAddChar") ?: false
    fun setIsAddChar(value : Boolean) = state.set("_isAddChar", value)

    fun getCategory() : Int {
        return state.get<Int>("_category") ?: if (systemLanguage().matches(Regex("tr"))) R.id.category_shop else R.id.category_animals
    }
    fun setCategory(value : Int) = state.set("_category", value)

    fun getLikeMode() : Int = state.get<Int>("_likeMode") ?: R.id.postPublic
    fun setLikeMode(value : Int) = state.set("_likeMode", value)

    fun getGoFeed() : Boolean = state.get<Boolean>("_goFeed") ?: false
    fun setGoFeed(value : Boolean) = state.set("_goFeed", value)

}