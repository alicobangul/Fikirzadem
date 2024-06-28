package com.basesoftware.fikirzadem.presentation.ui.login

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class LoginState : Parcelable {

    @Parcelize
    data class LoginUiState(
        var userMailText : String = "", // Kullanıcı mail adresi
        var userPasswordText : String = "", // Kullanıcı şifresi
        var rememberMe : Boolean = false, // Beni hatırla checkbox seçildi mi
        var uiEnable : Boolean = true, // UI bileşenleri etkin mi
    ) : Parcelable

    @Parcelize
    object Empty : LoginState()

}
