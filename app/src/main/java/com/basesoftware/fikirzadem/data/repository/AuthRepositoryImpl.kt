package com.basesoftware.fikirzadem.data.repository

import com.basesoftware.fikirzadem.domain.repository.AuthRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth

class AuthRepositoryImpl(private var firebaseAuth : FirebaseAuth) : AuthRepository {

    override suspend fun signIn(email: String, password: String): Task<AuthResult> {
        // Giriş yap
        return  firebaseAuth.signInWithEmailAndPassword(email, password)
    }

    override suspend fun signUp(email: String, password: String): Task<AuthResult> {
        // Kayıt ol
        return firebaseAuth.createUserWithEmailAndPassword(email, password)
    }

    override suspend fun deleteAuth() : Task<Void>? {

        return firebaseAuth.currentUser?.delete()

    }

    override fun getUserUId(): String { return firebaseAuth.currentUser?.uid ?: "" }

    override fun isLoggedIn(): Boolean { return firebaseAuth.currentUser != null }

    override fun signOut() { firebaseAuth.signOut() }

}