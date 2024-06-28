package com.basesoftware.fikirzadem.model.recycler

import android.os.Parcelable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.parcelize.Parcelize

@Parcelize
data class SearchRecyclerModel(
    var userId : String,
    var userMail : String,
    var userName : String,
    var userRealName : String,
    var userBiography : String,
    var userProfilePicture : String,
    var userTwitter : String,
    var userFacebook : String,
    var userInstagram : String,
    var userAdminMessageTr : String,
    var userAdminMessageEn : String,
    var userFollowing : Int,
    var userFollower : Int,
    var userReport : Int,
    var userSpamPost : Int,
    var userSpamComment : Int,
    var userSpamReport : Int,
    var userSpamContact : Int,
    var userAddPost : Boolean,
    var userAddComment : Boolean,
    var userAddReport : Boolean,
    var userAddContact : Boolean,
    var userEditProfile : Boolean,
    var userIsActive : Boolean,
    var userEmailConfirm : Boolean,
    @ServerTimestamp var userAdminMessageDate : Timestamp?,
    @ServerTimestamp var userRegisterDate : Timestamp?,
    var sourceServer : Boolean) : Parcelable
