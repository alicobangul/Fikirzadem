package com.basesoftware.fikirzadem.model

import android.os.Parcelable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.parcelize.Parcelize

@Parcelize
data class SocialModel(
    var userId: String,
    var actionType : String,
    @ServerTimestamp var actionDate: Timestamp?
    ) : Parcelable