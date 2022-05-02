package com.bond.bondbuddy.models

import androidx.annotation.Keep
import com.google.firebase.firestore.ServerTimestamp
import java.util.*

@Keep
data class TokenWithTimeStampID(
    val id: String,
    val token: String,
    @ServerTimestamp val timestamp: Date? = null
)