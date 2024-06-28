package com.basesoftware.fikirzadem.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SplashViewModel : ViewModel() {

    private val _navigateToLogin = MutableLiveData(false)
    val navigateToLogin: LiveData<Boolean> get() = _navigateToLogin


    fun onAnimationEnd() {  _navigateToLogin.value = true }

}