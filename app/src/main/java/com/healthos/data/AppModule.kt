package com.healthos.data

import android.content.Context
import androidx.room.Room
import com.healthos.bluetooth.BleConnectionManager
import com.healthos.data.local.HealthOsDao
import com.healthos.data.local.HealthOsDatabase
import com.healthos.data.remote.AuthApiService
import com.healthos.data.remote.CaregiverApiService
import com.healthos.data.remote.NetworkFactory
import com.healthos.data.remote.PatientApiService
import com.healthos.data.repository.AuthRepositoryImpl
import com.healthos.data.repository.CaregiverRepositoryImpl
import com.healthos.data.repository.PatientRepositoryImpl
import com.healthos.domain.repository.AuthRepository
import com.healthos.domain.repository.CaregiverRepository
import com.healthos.domain.repository.PatientRepository
import com.healthos.security.DatabasePassphraseProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds abstract fun bindPatientRepository(impl: PatientRepositoryImpl): PatientRepository

    @Binds abstract fun bindCaregiverRepository(impl: CaregiverRepositoryImpl): CaregiverRepository
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        passphraseProvider: DatabasePassphraseProvider,
    ): HealthOsDatabase {
        SQLiteDatabase.loadLibs(context)
        return Room.databaseBuilder(context, HealthOsDatabase::class.java, "healthos.db")
            .openHelperFactory(SupportFactory(passphraseProvider.passphrase()))
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides fun provideDao(database: HealthOsDatabase): HealthOsDao = database.dao()

    @Provides
    @Singleton
    fun provideBleConnectionManager(
        @ApplicationContext context: Context,
    ) = BleConnectionManager(context)

    @Provides
    @Singleton
    fun provideAuthApiService(networkFactory: NetworkFactory): AuthApiService =
        networkFactory.retrofit().create(AuthApiService::class.java)

    @Provides
    @Singleton
    fun providePatientApiService(networkFactory: NetworkFactory): PatientApiService =
        networkFactory.retrofit().create(PatientApiService::class.java)

    @Provides
    @Singleton
    fun provideCaregiverApiService(networkFactory: NetworkFactory): CaregiverApiService =
        networkFactory.retrofit().create(CaregiverApiService::class.java)
}
