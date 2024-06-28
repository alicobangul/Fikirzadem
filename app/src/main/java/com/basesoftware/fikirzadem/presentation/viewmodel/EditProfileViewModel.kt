package com.basesoftware.fikirzadem.presentation.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

class EditProfileViewModel(private val state : SavedStateHandle) : ViewModel() {

    private val _emptyText = ""

    // LiveData ->>

    fun getReAuth() : MutableLiveData<Boolean> = state.getLiveData("_reAuth", false)
    fun getReAuthValue() : Boolean = state.getLiveData("_reAuth", false).value ?: false
    fun setReAuth(value : Boolean) { state.getLiveData("_reAuth", false).postValue(value) }

    fun getUiEnable() : MutableLiveData<Boolean> = state.getLiveData("_uiEnable", true)
    fun getUiEnableValue() : Boolean = state.getLiveData("_uiEnable", true).value ?: true
    fun setUiEnable(value : Boolean) { state.getLiveData("_uiEnable", true).postValue(value) }


    // Değişkenler

    fun getCurrentFunction() : String? = state.get("_currentFunction")
    fun setCurrentFunction(value : String?) = state.set("_currentFunction", value)

    fun getCurrentPassword() : String = state.get("_currentPassword") ?: _emptyText
    fun setCurrentPassword(value : String) = state.set("_currentPassword", value)

    fun getNowImgStatus() : String = state.get("_nowImgStatus") ?: "myProfile"
    fun setNowImgStatus(value : String) = state.set("_nowImgStatus", value)

    fun getSelectImgByteArray() : ByteArray? = state.get("_selectImgByteArray")
    fun setSelectImgByteArray(value : ByteArray?) = state.set("_selectImgByteArray", value)

    fun getNewPpLink() : String = state.get("_newPpLink") ?: _emptyText
    fun setNewPpLink(value : String) = state.set("_newPpLink", value)

    fun getNewRealName() : String = state.get("_newRealName") ?: _emptyText
    fun setNewRealName(value : String) = state.set("_newRealName", value)

    fun getNewUserName() : String = state.get("_newUserName") ?: _emptyText
    fun setNewUserName(value : String) = state.set("_newUserName", value)

    fun getNewBiography() : String = state.get("_newBiography") ?: _emptyText
    fun setNewBiography(value : String) = state.set("_newBiography", value)

    fun getNewInstagram() : String = state.get("_newInstagram") ?: _emptyText
    fun setNewInstagram(value : String) = state.set("_newInstagram", value)

    fun getNewTwitter() : String = state.get("_newTwitter") ?: _emptyText
    fun setNewTwitter(value : String) = state.set("_newTwitter", value)

    fun getNewFacebook() : String = state.get("_newFacebook") ?: _emptyText
    fun setNewFacebook(value : String) = state.set("_newFacebook", value)

    fun getNewMail() : String = state.get("_newMail") ?: _emptyText
    fun setNewMail(value : String) = state.set("_newMail", value)

    fun getNewPass() : String = state.get("_newPass") ?: _emptyText
    fun setNewPass(value : String) = state.set("_newPass", value)

}