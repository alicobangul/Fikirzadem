package com.basesoftware.fikirzadem.model.recycler

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class NotificationRecyclerModel(
    var icon : Int,
    var message : String,
    var time : Long
    ) : Parcelable
