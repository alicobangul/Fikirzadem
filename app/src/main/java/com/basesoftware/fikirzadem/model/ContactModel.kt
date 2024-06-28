package com.basesoftware.fikirzadem.model

import android.os.Parcelable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.parcelize.Parcelize

@Parcelize
data class ContactModel(
    var contactId : String,
    var contactUserId : String,
    var contactMessage : String,
    @ServerTimestamp var contactDate : Timestamp?
    ) : Parcelable
