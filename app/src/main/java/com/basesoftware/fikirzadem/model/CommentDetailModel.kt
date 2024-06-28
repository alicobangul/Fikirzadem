package com.basesoftware.fikirzadem.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CommentDetailModel(
    var postComment: String,
    var actionUserId: String,
    var isDeletePermission: Boolean,
    var isReportPermission: Boolean) : Parcelable
