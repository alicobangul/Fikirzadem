package com.basesoftware.fikirzadem.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.basesoftware.fikirzadem.domain.repository.UserSettingsRepository
import kotlinx.coroutines.flow.first

class UserSettingsRepositoryImpl(var dataStore: DataStore<Preferences>) : UserSettingsRepository {

    private val rememberme = "REMEMBER_ME"

    override suspend fun getRememberMe() =  dataStore.data.first()[booleanPreferencesKey(rememberme)] ?: false

    override suspend fun setRememberMe(isRemember: Boolean) { dataStore.edit { settings -> settings[booleanPreferencesKey(rememberme)] = isRemember } }


}