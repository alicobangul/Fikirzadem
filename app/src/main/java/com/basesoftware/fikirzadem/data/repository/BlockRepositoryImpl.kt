package com.basesoftware.fikirzadem.data.repository

import com.basesoftware.fikirzadem.domain.repository.BlockRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source

class BlockRepositoryImpl (private var firestore : FirebaseFirestore) : BlockRepository {

    override fun searchUserInBlockList(mail: String) : Task<QuerySnapshot?> {

        return firestore.collection("Block").whereEqualTo("blockMail", mail).limit(1).get(Source.SERVER)

    }

}