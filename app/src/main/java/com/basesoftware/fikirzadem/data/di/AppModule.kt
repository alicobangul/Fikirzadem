package com.basesoftware.fikirzadem.data.di

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.basesoftware.fikirzadem.data.repository.AuthRepositoryImpl
import com.basesoftware.fikirzadem.data.repository.BlockRepositoryImpl
import com.basesoftware.fikirzadem.data.repository.UserRepositoryImpl
import com.basesoftware.fikirzadem.data.repository.UserSettingsRepositoryImpl
import com.basesoftware.fikirzadem.domain.repository.AuthRepository
import com.basesoftware.fikirzadem.domain.repository.BlockRepository
import com.basesoftware.fikirzadem.domain.repository.UserRepository
import com.basesoftware.fikirzadem.domain.repository.UserSettingsRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    private val Context.dataStore by preferencesDataStore(name = "userSettings")

    @Singleton
    @Provides
    fun firestoreProvider() : FirebaseFirestore {

        return FirebaseFirestore.getInstance().apply {
            firestoreSettings = FirebaseFirestoreSettings
                .Builder()
                .setCacheSizeBytes(26214400)
                .setPersistenceEnabled(true)
                .build()
        }

    }

    @Singleton
    @Provides
    fun authProvider() : FirebaseAuth = FirebaseAuth.getInstance()


    @Singleton
    @Provides
    fun authRepositoryProvider(firebaseAuth: FirebaseAuth) : AuthRepository = AuthRepositoryImpl(firebaseAuth)

    @Singleton
    @Provides
    fun userRepositoryProvider(firestore: FirebaseFirestore) : UserRepository = UserRepositoryImpl(firestore)

    @Singleton
    @Provides
    fun blockRepositoryProvider(firestore: FirebaseFirestore) : BlockRepository = BlockRepositoryImpl(firestore)

    @Singleton
    @Provides
    fun userSettingsProvider(@ApplicationContext context: Context) : UserSettingsRepository = UserSettingsRepositoryImpl(context.dataStore)

}