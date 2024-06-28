package com.basesoftware.fikirzadem.domain.use_case.user

import com.basesoftware.fikirzadem.domain.repository.UserRepository
import com.basesoftware.fikirzadem.domain.util.Error
import com.basesoftware.fikirzadem.domain.util.Result
import com.basesoftware.fikirzadem.domain.util.UserError
import com.basesoftware.fikirzadem.util.ExtensionUtil.toUserModel
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ActiveUserControlUseCase @Inject constructor(private val userRepository: UserRepository) {

    suspend fun findUser(idOrEmail : String, searchField : SearchField) : Result<Boolean, Error> {

        return try {

            val result = when(searchField) {

                SearchField.ID -> userRepository.findUserWithId(idOrEmail).await()

                SearchField.EMAIL -> userRepository.findUserWithMail(idOrEmail).await()

            }

            result?.let {

                return when {

                    it.isEmpty -> Result.Error(UserError.USER_NOTFOUND)

                    it.documents[0].toUserModel().userIsActive -> Result.Success(true)

                    else -> Result.Success(false)

                }

            } ?: throw Exception()

        }

        catch (exception : Exception) {

            when((exception as FirebaseFirestoreException).code) {

                FirebaseFirestoreException.Code.NOT_FOUND -> return Result.Error(UserError.USER_NOTFOUND)

                else -> Result.Error(UserError.UNKNOWN_ERROR)

            }

        }

    }

    enum class SearchField {
        ID,
        EMAIL
    }

}