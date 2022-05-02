package com.bond.bondbuddy.di

import android.app.Application
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestoreSettings
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class HiltApp :
    Application() {
    override fun onCreate() {
        super.onCreate()
        val fireStoreSettings = firestoreSettings{
            isPersistenceEnabled = true
        }

        FirebaseFirestore.getInstance().firestoreSettings = fireStoreSettings
    }
    }