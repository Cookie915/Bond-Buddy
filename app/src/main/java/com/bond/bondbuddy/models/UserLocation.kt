package com.bond.bondbuddy.models

import androidx.annotation.Keep
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ServerTimestamp
import java.util.*

@Keep
data class UserLocation(
    var latlng: GeoPoint? = null,
    @ServerTimestamp var timestamp: Date? = null
)