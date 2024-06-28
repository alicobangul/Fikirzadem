package com.basesoftware.fikirzadem.domain.util

enum class AuthInputError : Error {
    EMAIL_TYPE,
    SHORT_PASSWORD
}

enum class AuthError : Error {
    AUTH_NOTFOUND,
    CURRENTUSER_NOTFOUND,
    WRONG_PASSWORD,
    AUTH_COLLISION,
    UNKNOWN_ERROR
}

enum class UserError : Error {
    USER_NOTFOUND,
    UNKNOWN_ERROR
}

enum class BlockError : Error {
    UNKNOWN_ERROR
}