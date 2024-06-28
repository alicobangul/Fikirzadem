package com.basesoftware.fikirzadem.model

import android.os.Parcelable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.parcelize.Parcelize

@Parcelize
data class ReportModel(
    var reportId : String,
    var reportCategory : Int,
    var reportContentId : String,
    var reportContentExtraId : String,
    var reportContent : String,
    var reportContentDetail : String,
    var reportType : String,
    var reporterUserId : String,
    @ServerTimestamp var reportDate : Timestamp?
    ) : Parcelable