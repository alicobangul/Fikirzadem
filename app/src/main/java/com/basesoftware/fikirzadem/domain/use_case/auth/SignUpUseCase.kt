package com.basesoftware.fikirzadem.domain.use_case.auth

import com.basesoftware.fikirzadem.domain.repository.AuthRepository
import com.basesoftware.fikirzadem.domain.util.AuthError
import com.basesoftware.fikirzadem.domain.util.Error
import com.basesoftware.fikirzadem.domain.util.Result
import com.basesoftware.fikirzadem.util.ExtensionUtil.message
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class SignUpUseCase @Inject constructor(private val authRepository: AuthRepository) {

    suspend operator fun invoke(email: String, password: String) : Result<AuthResult, Error> {

        return try {

            val result = authRepository.signUp(email, password).await()

            result.user?.let { return Result.Success(Unit) } ?: throw Exception()

        }

        catch (exception : Exception) {

            when(exception) {

                is FirebaseAuthUserCollisionException -> return Result.Error(AuthError.AUTH_COLLISION)

                else -> Result.Error(AuthError.UNKNOWN_ERROR)

            }

        }

    }

}