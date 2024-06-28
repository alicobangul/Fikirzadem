package com.basesoftware.fikirzadem.domain.repository

import com.basesoftware.fikirzadem.model.UserModel
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.QuerySnapshot

interface UserRepository {

    suspend fun findUserWithMail(email : String) : Task<QuerySnapshot?>

    suspend fun findUserWithId(userId : String) : Task<QuerySnapshot?>

    suspend fun createUserDocument(userId: String, userMail: String) : Task<Void>

}