package com.basesoftware.fikirzadem.model

import android.os.Parcelable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.parcelize.Parcelize

@Parcelize
data class PostModel(
    var postCategoryId: Int,
    var postContent: String,
    var postLikePublic: Boolean,
    var postId: String,
    var postUserId: String,
    @ServerTimestamp var postDate: Timestamp?
    ) : Parcelable
