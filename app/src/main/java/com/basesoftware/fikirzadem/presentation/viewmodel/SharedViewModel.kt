package com.basesoftware.fikirzadem.presentation.viewmodel

import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistryOwner
import com.basesoftware.fikirzadem.model.recycler.NotificationRecyclerModel
import com.basesoftware.fikirzadem.model.UserModel

class SharedViewModel (private val state : SavedStateHandle, app : Application) : ViewModel() {


    // ViewModel başlangıcında sharedPreferences aracılığıyla değerler okunuyor ve state'e kaydediliyor
    init {

        app.getSharedPreferences("com.basesoftware.fikirzadem", Context.MODE_PRIVATE).apply {

            setMyUserId(getString("userId","null").toString())
            setStaggeredLayout(getBoolean("_staggeredLayout", true))
            setSideMenuAlignment(getBoolean("_sideMenuAlignment", true))
            setNotification(getBoolean("_notification", true))
            setAnimation(getBoolean("_animation", true))
            setBottomMenu(getBoolean("_bottomMenu", true))
            setRightMenu(getBoolean("_rightMenu", false))

        }

    }


    /**
     * - Statik yapı oluşturmamak ve olası memory leak (bellek sızıntısı) durumlarının önüne geçmek için AndroidViewModel yerine ViewModel kullanıldı
     * - ViewModel içerisinde sharedPreferences'a ulaşmak için application parametre olarak verilmeli
     * - ViewModel'a parametreler ViewModelFactory aracılığıyla iletilir bu durumda ayrı bir sınıf açmak yerine companion object kullanıldı
     * - ViewModelProvider.Factory yerine AbstractSavedStateViewModelFactory kullanılmasının sebebi:
     * Application'ı gönderebilmenin yanında SavedStateHandle yapısını kullanabilmek
     */
    companion object {
        fun provideFactory(app: Application, owner: SavedStateRegistryOwner, defaultArgs: Bundle? = null): AbstractSavedStateViewModelFactory =
            object : AbstractSavedStateViewModelFactory(owner, defaultArgs) {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(key: String, modelClass: Class<T>, state: SavedStateHandle): T = SharedViewModel(state, app) as T
            }
    }



    //------------------------------ AD FUNCTION ------------------------------

    fun getBannerRotateCount() : Int { return state.get("_bannerRotateCount") ?: 0}
    fun addBannerRotateCount() { state.set("_bannerRotateCount", getBannerRotateCount() + 1) }
    fun deleteBannerRotateCount() { state.set("_bannerRotateCount", 0) }

    //------------------------------ AD FUNCTION ------------------------------





    private val _notificationRecyclerList : MutableLiveData<ArrayList<NotificationRecyclerModel>> = state.getLiveData("_notificationList", arrayListOf())
    fun getNotificationList() : ArrayList<NotificationRecyclerModel> = _notificationRecyclerList.value!!
    fun setNotification(value : NotificationRecyclerModel) {
        state.getLiveData<ArrayList<NotificationRecyclerModel>>("_notificationList", arrayListOf()).value?.add(value)
    }

    fun getUserLive() : MutableLiveData<UserModel> = state.getLiveData("myUser")
    fun getUser() : UserModel? = getUserLive().value
    fun setUser(model : UserModel) {
        getUserLive().value = model
        setMyUserId(model.userId)
    }

    fun getShowMyProfile() : Boolean = state.get<Boolean>("_showMyProfile") ?: false
    fun setShowMyProfile(value : Boolean) = state.set("_showMyProfile", value)

    fun getMyUserId() : String = state.get<String>("_myUserId") ?: "null"
    fun setMyUserId(value : String) = state.set("_myUserId", value)

    fun getLastFollow() : String = state.get<String>("_lastFollow") ?: "null"
    fun setLastFollow(value : String) = state.set("_lastFollow", value)

    fun getLastRefreshProfile() : String = state.get<String>("_lastRefreshProfile") ?: "-"
    fun setLastRefreshProfile(value : String) = state.set("_lastRefreshProfile", value)

    fun getScrollDownLive() : MutableLiveData<Boolean> = state.getLiveData("_scrollDown")
    fun setScrollDown(value : Boolean) { getScrollDownLive().value = value }

    fun getSuggestionDialog() : Boolean = state.get<Boolean>("_isSuggestionDialog") ?: true
    fun setSuggestionDialog(value : Boolean) = state.set("_isSuggestionDialog", value)






    // Feed postları staggered tipinde gösterilsinmi
    fun getStaggeredLayout() : Boolean = state.get<Boolean>("_staggeredLayout") ?: true
    fun setStaggeredLayout(value : Boolean) = state.set("_staggeredLayout", value)

    // Yan menü kapanınca otomatik yukarı scroll edilsin mi
    fun getSideMenuAlignment() : Boolean = state.get<Boolean>("_sideMenuAlignment") ?: true
    fun setSideMenuAlignment(value : Boolean) = state.set("_sideMenuAlignment", value)

    // Bildirimler kontrol edilsin mi
    fun getNotification() : Boolean = state.get<Boolean>("_notification") ?: true
    fun setNotification(value : Boolean) = state.set("_notification", value)

    // Feed search gibi yerlerde animasyon kullanılsın mı (soldan -> sağa)
    fun getAnimation() : Boolean = state.get<Boolean>("_animation") ?: true
    fun setAnimation(value : Boolean) = state.set("_animation", value)

    // Alt menü gösterilsin mi
    fun getBottomMenu() : Boolean = state.get<Boolean>("_bottomMenu") ?: true
    fun setBottomMenu(value : Boolean) = state.set("_bottomMenu", value)

    // Sağ menü gösterilsin mi
    fun getRightMenu() : Boolean = state.get<Boolean>("_rightMenu") ?: false
    fun setRightMenu(value : Boolean) = state.set("_rightMenu", value)

}