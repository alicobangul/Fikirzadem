package com.basesoftware.fikirzadem.domain.repository

interface UserSettingsRepository {

    suspend fun getRememberMe(): Boolean

    suspend fun setRememberMe(isRemember : Boolean)

}