package com.basesoftware.fikirzadem.data.repository

import com.basesoftware.fikirzadem.domain.repository.UserRepository
import com.basesoftware.fikirzadem.model.UserModel
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source

class UserRepositoryImpl(private var firestore : FirebaseFirestore) : UserRepository {

    override suspend fun findUserWithMail(email: String) : Task<QuerySnapshot?> {

        return firestore.collection("Users").whereEqualTo("userMail", email).limit(1).get(Source.SERVER)

    }

    override suspend fun findUserWithId(userId: String) : Task<QuerySnapshot?> {

        return firestore.collection("Users").whereEqualTo("userId", userId).limit(1).get(Source.SERVER)

    }

    override suspend fun createUserDocument(userId: String, userMail: String): Task<Void> {

        return firestore
            .collection("Users")
            .document(userId)
            .set(
                UserModel(userId = userId, userMail = userMail)
            )

    }

}