package com.basesoftware.fikirzadem.domain.validator

import com.basesoftware.fikirzadem.domain.util.AuthInputError
import com.basesoftware.fikirzadem.domain.util.Result
import javax.inject.Inject

class AuthInputValidator @Inject constructor() {

    fun validateEmail(email: String) : Result<Unit, AuthInputError> {

        return if(email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex())) Result.Success(Unit) else Result.Error(AuthInputError.EMAIL_TYPE)
    }

    fun validatePassword(password: String) : Result<Unit, AuthInputError> {

        return if(password.length >= 6) Result.Success(Unit) else Result.Error(AuthInputError.SHORT_PASSWORD)

    }

    fun validateEmailAndPassword(email: String, password: String) : Result<Unit, AuthInputError> {

        val emailResult = validateEmail(email)
        val passwordResult = validatePassword(password)

        return when {
            emailResult is Result.Error -> emailResult
            passwordResult is Result.Error -> passwordResult
            else -> Result.Success(Unit)
        }

    }

}