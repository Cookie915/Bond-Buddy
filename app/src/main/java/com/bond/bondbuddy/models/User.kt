package com.bond.bondbuddy.models

import androidx.annotation.Keep
import com.google.firebase.firestore.GeoPoint

@Keep
data class User(
    var displayname: String? = null,
    var id: String? = null,
    var companyname: String? = null,
    var owner: Boolean? = null,
    var registered: Boolean = false,
    var contactemail: String? = null,
    var contactnumber: String? = null,
    var lastlocation: UserLocation = UserLocation(GeoPoint(0.0,0.0)),
    var boundingState: String? = null,
    var profilepicurl: String = " ",
    var fcmToken: String? = null,
    var active: Boolean = true,
    var cached: Boolean = false
)



