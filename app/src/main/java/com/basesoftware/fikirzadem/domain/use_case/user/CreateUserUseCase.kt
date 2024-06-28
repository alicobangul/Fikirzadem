package com.basesoftware.fikirzadem.domain.use_case.user

import com.basesoftware.fikirzadem.domain.repository.UserRepository
import com.basesoftware.fikirzadem.domain.util.AuthError
import com.basesoftware.fikirzadem.domain.util.Error
import com.basesoftware.fikirzadem.domain.util.Result
import com.basesoftware.fikirzadem.domain.util.UserError
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class CreateUserUseCase @Inject constructor(
    private var firebaseAuth : FirebaseAuth,
    private val userRepository: UserRepository) {

    suspend operator fun invoke(email: String) : Result<Boolean, Error> {

        firebaseAuth.currentUser ?: return Result.Error(UserError.UNKNOWN_ERROR)

        return try {

            firebaseAuth.uid?.let { userRepository.createUserDocument(it, email).await() }

            Result.Success(true)

        }

        catch (exception : Exception) {

            return Result.Error(UserError.UNKNOWN_ERROR)

        }

    }

}