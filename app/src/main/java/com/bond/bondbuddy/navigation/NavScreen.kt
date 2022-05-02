package com.bond.bondbuddy.navigation

import androidx.annotation.DrawableRes
import com.bond.bondbuddy.R

sealed class NavScreen(
    val route: String, val label: String, @DrawableRes val icon: Int, val contentDisc: String
) {
    //  Admin
    object AdminHomeScreen : NavScreen(
        "AdminHomeScreen", "Home", R.drawable.ic_profile, "Home Screen"
    )

    object AdminPeopleLocationsScreen : NavScreen(
        "AdminPeopleLocationsScreen", "Locations", R.drawable.ic_globe_grid, "Locations"
    )

    object AdminSettingsScreen : NavScreen(
        "AdminSettingsScreen", "Settings", R.drawable.ic_settings, "Settings"
    )

    //  User
    object UserHomeScreen: NavScreen(
        route = "UserHomeScreen", label = "Home", icon = R.drawable.ic_profile, contentDisc = "User Home Screen"
    )


    companion object {
        val AdminScreenList = listOf(
            AdminHomeScreen, AdminPeopleLocationsScreen, AdminSettingsScreen
        )
        val UserScreenList = listOf(
            UserHomeScreen,
        )

        val AdminHomeScreenRoute = AdminHomeScreen.route
        val UserHomeScreenRoute = UserHomeScreen.route

    }
}
