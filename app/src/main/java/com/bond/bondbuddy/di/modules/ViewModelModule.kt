package com.bond.bondbuddy.di.modules

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.ExperimentalComposeUiApi
import com.bond.bondbuddy.repo.CompanyRepository
import com.bond.bondbuddy.repo.UserRepository
import com.bond.bondbuddy.viewmodels.CompanyViewModel
import com.bond.bondbuddy.viewmodels.MapViewModel
import com.bond.bondbuddy.viewmodels.UserViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(
    ViewModelComponent::class
)
object ViewModelModule {
    @ExperimentalComposeUiApi
    @ExperimentalPermissionsApi
    @ExperimentalMaterialApi
    @ExperimentalAnimationApi
    @ExperimentalStdlibApi
    @ViewModelScoped
    @Provides
    fun provideUserViewModel(): UserViewModel {
        return UserViewModel(UserRepository())
    }

    @ViewModelScoped
    @Provides
    fun provideCompanyViewModel(): CompanyViewModel {
        return CompanyViewModel(
            companyRepository = CompanyRepository(
                UserRepository()
            )
        )
    }

}

@Module
@InstallIn(ViewModelComponent::class)
object MapViewModelModule {
    @ViewModelScoped
    @Provides
    fun provideMapViewModel(): MapViewModel {
        return MapViewModel()
    }
}