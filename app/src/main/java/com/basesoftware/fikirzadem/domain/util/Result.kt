package com.basesoftware.fikirzadem.domain.util

import com.basesoftware.fikirzadem.util.RootError

typealias RootError = Error

sealed interface Result<out D, out E: RootError> {

    data class Success<out D, out E: RootError>(val data: Any? = null): Result<D, E>

    data class Error<out D, out E: RootError>(val error: E): Result<D, E>

}

sealed interface Error