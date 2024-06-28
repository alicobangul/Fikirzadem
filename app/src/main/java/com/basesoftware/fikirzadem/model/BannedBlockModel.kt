package com.basesoftware.fikirzadem.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class BannedBlockModel(
    var blockId : String,
    var blockMail : String,
    @ServerTimestamp var blockDate : Timestamp?
)
