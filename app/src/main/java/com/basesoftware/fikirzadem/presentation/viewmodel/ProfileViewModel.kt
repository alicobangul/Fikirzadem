package com.basesoftware.fikirzadem.presentation.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.basesoftware.fikirzadem.model.PostModel
import com.basesoftware.fikirzadem.model.SocialModel
import com.basesoftware.fikirzadem.model.UserModel

class ProfileViewModel (private val state : SavedStateHandle) : ViewModel() {

    private val _fragmentResumeLiveData = MutableLiveData(true)
    fun getFragmentResume() : MutableLiveData<Boolean> = _fragmentResumeLiveData
    fun setFragmentResume(value : Boolean) { getFragmentResume().value = value }



    fun getUserPostList() : ArrayList<PostModel> = state.get("_userPostList") ?: arrayListOf()
    fun setUserPost(value : PostModel) { state.set("_userPostList", getUserPostList().apply { add(value) }) }
    fun clearUserPostList() = state.set("_userPostList", arrayListOf<PostModel>())

    fun getSocialUserList() : ArrayList<SocialModel> = state.get("_socialUserList") ?: arrayListOf()
    fun setSocialUser(value : SocialModel) { state.set("_socialUserList", getSocialUserList().apply { add(value) }) }
    fun clearSocialUserList() = state.set("_socialUserList", arrayListOf<SocialModel>())



    fun getLoadingProfile() : Boolean = state.get("_loadingProfile") ?: true
    fun setLoadingProfile(value : Boolean) { state.set("_loadingProfile", value) }

    fun getDataDownloading() : Boolean = state.get("_dataDownloading") ?: false
    fun setDataDownloading(value : Boolean) { state.set("_dataDownloading", value) }


    fun getSocialSheetShow() : Boolean = state.get<Boolean>("_socialSheetShow") ?: false
    fun setSocialSheetShow (value : Boolean) = state.set("_socialSheetShow", value)





    fun getUpdateScreen() : MutableLiveData<Boolean> = state.getLiveData("_updateScreen", false)
    fun setUpdateScreen(value : Boolean) { state.getLiveData("_updateScreen", false).postValue(value) }

    fun getUiEnable() : Boolean = state.get<Boolean>("_uiEnable") ?: false
    fun setUiEnable (value : Boolean) {
        setUpdateScreen(true)
        state.set("_uiEnable", value)
    }

    fun getProfile() : UserModel? = state.get<UserModel>("_profile")
    fun setProfile(model : UserModel) = state.set("_profile", model)

    fun getArgumentUserId() : String = state.get<String>("_argumentUserId") ?: "null"
    fun setArgumentUserId (value : String) = state.set("_argumentUserId", value)

    fun getSocialComplete() : Boolean = state.get<Boolean>("_socialComplete") ?: false
    fun setSocialComplete (value : Boolean) = state.set("_socialComplete", value)

    fun getMyProfile() : Boolean = state.get<Boolean>("_myProfile") ?: false
    fun setMyProfile (value : Boolean) = state.set("_myProfile", value)

    fun getFollow() : Boolean = state.get<Boolean>("_ifollow") ?: false
    fun setFollow (value : Boolean) = state.set("_ifollow", value)

    fun getNowFollow() : Boolean = state.get<Boolean>("_nowFollow") ?: false
    fun setNowFollow (value : Boolean) = state.set("_nowFollow", value)

    fun getNowUnfollow() : Boolean = state.get<Boolean>("_nowUnfollow") ?: false
    fun setNowUnfollow (value : Boolean) = state.set("_nowUnfollow", value)

    fun getNowReport() : Boolean = state.get<Boolean>("_nowReport") ?: false
    fun setNowReport (value : Boolean) = state.set("_nowReport", value)

}