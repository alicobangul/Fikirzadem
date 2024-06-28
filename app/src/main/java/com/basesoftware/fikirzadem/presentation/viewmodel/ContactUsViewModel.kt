package com.basesoftware.fikirzadem.presentation.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp

class ContactUsViewModel (private val state : SavedStateHandle) : ViewModel() {

    fun getUiEnable() : MutableLiveData<Boolean> = state.getLiveData("_uiEnable", true)
    fun getUiEnableValue() : Boolean =  state.getLiveData("_uiEnable", true).value ?: true
    fun setUiEnable(value : Boolean) { state.getLiveData("_uiEnable", true).value = value }


    fun getLastMessageDate() : Timestamp = state.get("_lastMessageDate") ?: Timestamp.now()
    fun setLastMessageDate(value : Timestamp) = state.set("_lastMessageDate", value)


    fun getLastOperation() : String? = state.get("_lastOperation")
    fun setLastOperation(value : String?) = state.set("_lastOperation", value)

}