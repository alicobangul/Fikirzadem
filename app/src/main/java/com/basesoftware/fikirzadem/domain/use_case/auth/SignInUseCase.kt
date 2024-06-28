package com.basesoftware.fikirzadem.domain.use_case.auth

import com.basesoftware.fikirzadem.domain.repository.AuthRepository
import com.basesoftware.fikirzadem.domain.util.AuthError
import com.basesoftware.fikirzadem.domain.util.Error
import com.basesoftware.fikirzadem.domain.util.Result
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class SignInUseCase @Inject constructor(private val authRepository: AuthRepository) {

    suspend operator fun invoke(email: String, password: String) : Result<AuthResult, Error> {

        return try {

            val result = authRepository.signIn(email, password).await()

            result.user?.let { return Result.Success(Unit) } ?: throw Exception()

        }

        catch (exception : Exception) {

            when(exception) {

                is FirebaseAuthInvalidUserException -> return Result.Error(AuthError.AUTH_NOTFOUND)

                is FirebaseAuthInvalidCredentialsException -> return Result.Error(AuthError.WRONG_PASSWORD)

                else -> Result.Error(AuthError.UNKNOWN_ERROR)

            }

        }

    }

}