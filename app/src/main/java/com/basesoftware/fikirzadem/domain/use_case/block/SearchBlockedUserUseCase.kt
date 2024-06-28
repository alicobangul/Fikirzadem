package com.basesoftware.fikirzadem.domain.use_case.block

import com.basesoftware.fikirzadem.domain.repository.BlockRepository
import com.basesoftware.fikirzadem.domain.util.BlockError
import com.basesoftware.fikirzadem.domain.util.Error
import com.basesoftware.fikirzadem.domain.util.Result
import com.basesoftware.fikirzadem.domain.util.UserError
import com.basesoftware.fikirzadem.util.ExtensionUtil.toUserModel
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class SearchBlockedUserUseCase @Inject constructor(private val blockRepository: BlockRepository) {

    suspend fun findBlockedUser(mail : String) : Result<Boolean, Error> {

        return try {

            blockRepository.searchUserInBlockList(mail).await()?.let {

                return when {

                    !it.isEmpty -> Result.Success(true) // Kullanıcı bloklanmış

                    else -> Result.Success(false) // Kullanıcı blok listesinde yok

                }

            } ?: throw Exception()

        }

        catch (exception : Exception) {

            when((exception as FirebaseFirestoreException).code) {

                FirebaseFirestoreException.Code.NOT_FOUND -> return Result.Success(false)

                else -> Result.Error(BlockError.UNKNOWN_ERROR)

            }

        }

    }

}