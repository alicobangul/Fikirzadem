package com.basesoftware.fikirzadem.domain.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.QuerySnapshot

interface BlockRepository {

    fun searchUserInBlockList(mail : String)  : Task<QuerySnapshot?>

}