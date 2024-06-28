package com.basesoftware.fikirzadem.model

import android.os.Parcelable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.parcelize.Parcelize

@Parcelize
data class LikeDislikeModel(
    var actionUserId : String,
    var postId : String,
    var postUserId : String,
    var postRating : String,
    var postComment : String,
    var postCommentExist : Boolean,
    @ServerTimestamp var actionDate : Timestamp?
    ) : Parcelable