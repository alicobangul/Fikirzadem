package com.basesoftware.fikirzadem.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.basesoftware.fikirzadem.R

class AppActivityViewModel(private val state : SavedStateHandle) : ViewModel() {

    private var _arrayNavMenuFragment : ArrayList<Int> = arrayListOf()
    fun getArrayNavMenuFragment() = _arrayNavMenuFragment

    private var _arrayBottomMenuFragment : ArrayList<Int> = arrayListOf()
    fun getArrayBottomMenuFragment() = _arrayBottomMenuFragment

    fun getUserError() : Boolean = state.get<Boolean>("_userError") ?: false
    fun setUserError(value : Boolean) = state.set("_userError", value)

    private var _backgroundMessage = false
    fun getBackgroundMessage() : Boolean = _backgroundMessage
    fun setBackgroundMessage(value : Boolean) { _backgroundMessage = value }

    private var _bottomVisibility = true
    fun getBottomVisibility() : Boolean = _bottomVisibility
    fun setBottomVisibility(value : Boolean) { _bottomVisibility = value }

    private var _touchStartY = 0f
    fun getTouchStartY() : Float = _touchStartY
    fun setTouchStartY(value : Float) { _touchStartY = value }

    private var _touchEndY = 0f
    fun getTouchEndY() : Float = _touchEndY
    fun setTouchEndY(value : Float) { _touchEndY = value }

    init {
        _arrayNavMenuFragment.add(R.id.feedFragment)
        _arrayNavMenuFragment.add(R.id.searchFragment)
        _arrayNavMenuFragment.add(R.id.newPostFragment)
        _arrayNavMenuFragment.add(R.id.notificationFragment)
        _arrayNavMenuFragment.add(R.id.profileFragment)
        _arrayNavMenuFragment.add(R.id.favoriteFragment)
        _arrayNavMenuFragment.add(R.id.interestFragment)
        _arrayNavMenuFragment.add(R.id.contactUsFragment)
        _arrayNavMenuFragment.add(R.id.editProfileFragment)
        _arrayNavMenuFragment.add(R.id.settingsFragment)

        _arrayBottomMenuFragment.add(R.id.feedFragment)
        _arrayBottomMenuFragment.add(R.id.searchFragment)
        _arrayBottomMenuFragment.add(R.id.notificationFragment)
        _arrayBottomMenuFragment.add(R.id.profileFragment)
    }

}