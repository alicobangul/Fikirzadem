package com.basesoftware.fikirzadem.model

import android.os.Parcelable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.parcelize.Parcelize
import kotlin.random.Random

@Parcelize
data class UserModel(
    var userId: String,
    var userMail: String,
    var userName: String = "user" + Random.nextInt(10000, 99999).toString(),
    var userRealName: String = userName,
    var userBiography: String = "-",
    var userProfilePicture: String = "default",
    var userTwitter: String = "-",
    var userFacebook: String = "-",
    var userInstagram: String = "-",
    var userAdminMessageTr: String = "Fikirzademe hoşgeldiniz",
    var userAdminMessageEn: String = "Welcome to the Fikirzadem",
    var userFollowing: Int = 0,
    var userFollower: Int = 0,
    var userReport: Int = 0,
    var userSpamPost: Int = 0,
    var userSpamComment: Int = 0,
    var userSpamReport: Int = 0,
    var userSpamContact: Int = 0,
    var userAddPost: Boolean = true,
    var userAddComment: Boolean = true,
    var userAddReport: Boolean = true,
    var userAddContact: Boolean = true,
    var userEditProfile: Boolean = true,
    var userIsActive: Boolean = true,
    var userEmailConfirm: Boolean = false,
    @ServerTimestamp var userAdminMessageDate: Timestamp? = null,
    @ServerTimestamp var userRegisterDate: Timestamp? = null
) : Parcelable