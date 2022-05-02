package com.bond.bondbuddy.di.modules

import com.bond.bondbuddy.repo.CompanyRepository
import com.bond.bondbuddy.repo.UserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(
    SingletonComponent::class
)
object AppRepoModule {

    @Singleton
    @Provides
    fun provideRepo(): UserRepository {
        return UserRepository()
    }

    @Singleton
    @Provides
    fun provideCompanyRepo(): CompanyRepository {
        return CompanyRepository(
            UserRepository()
        )
    }

}