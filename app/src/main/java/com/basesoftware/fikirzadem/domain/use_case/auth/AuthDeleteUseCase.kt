package com.basesoftware.fikirzadem.domain.use_case.auth

import com.basesoftware.fikirzadem.domain.repository.AuthRepository
import com.basesoftware.fikirzadem.domain.util.AuthError
import com.basesoftware.fikirzadem.domain.util.Error
import com.basesoftware.fikirzadem.domain.util.Result
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthDeleteUseCase @Inject constructor(
    private var firebaseAuth : FirebaseAuth,
    private val authRepository: AuthRepository
) {

    suspend operator fun invoke() : Result<Boolean, Error> {

        firebaseAuth.currentUser ?: return Result.Error(AuthError.CURRENTUSER_NOTFOUND)

        return try {
            authRepository.deleteAuth()?.await()
            Result.Success(true)
        } catch (exception: Exception) {
            when (exception) {
                is FirebaseAuthInvalidUserException -> Result.Error(AuthError.AUTH_NOTFOUND)
                else -> Result.Error(AuthError.UNKNOWN_ERROR)
            }
        }

    }

}