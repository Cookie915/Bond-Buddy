package com.bond.bondbuddy.viewmodels

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.bond.bondbuddy.R
import com.bond.bondbuddy.components.toLatLng
import com.bond.bondbuddy.models.User
import com.bond.bondbuddy.models.UserLocation
import com.bond.bondbuddy.util.LocationClusterItem
import com.bond.bondbuddy.util.LocationClusterRenderer
import com.bond.bondbuddy.util.mapActiveProfilePicture
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.firebase.storage.FirebaseStorage
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.collections.MarkerManager
import com.google.maps.android.ktx.awaitAnimateCamera
import com.google.maps.android.ktx.awaitMap
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor() : ViewModel() {
    private val tag = "MapViewModel"
    var map: GoogleMap? = null
    var clusterManager: ClusterManager<LocationClusterItem>? = null
    var clusterRenderer: LocationClusterRenderer? = null
    var normalMarkers: MarkerManager.Collection? = null

    override fun onCleared() {
        super.onCleared()
        map = null
        clusterManager = null
        clusterRenderer = null
        normalMarkers = null
        Log.i(tag, "MapViewModel Cleared")
    }

    @SuppressLint("PotentialBehaviorOverride")
    suspend fun initializeMap(ctx: Context, mapView: MapView, onUserInfoClick: (Marker) -> Unit, onMapInit: () -> Unit) {
        if (this.isInit()) {
            return
        } else {
            Log.i("MapViewModel", "Redrawing map")
            map = mapView.awaitMap()
            clusterManager = ClusterManager(ctx, map)
            clusterRenderer = LocationClusterRenderer(ctx, map!!, clusterManager!!, map!!.cameraPosition.zoom, 14.0f)
            clusterManager!!.renderer = clusterRenderer
            normalMarkers = clusterManager!!.markerManager.newCollection()
            map!!.setOnCameraMoveListener(clusterRenderer)
            map!!.setOnCameraIdleListener(clusterManager)
            map!!.setOnMarkerClickListener(clusterManager!!.markerManager)
            normalMarkers!!.setOnMarkerClickListener {
                Log.i(tag, "normal marker clicked")
                it.showInfoWindow()
                animateZoomInCamera(it.position, 6f)
                return@setOnMarkerClickListener true
            }
            normalMarkers!!.setOnInfoWindowClickListener {
                Log.i(tag, "Normal Marker Info Window clicked")
                onUserInfoClick(it)
            }
            clusterManager!!.setOnClusterClickListener {
                Log.i(tag, "Cluster clicked")
                it.items.first().getUserID()
                animateZoomInCamera(it.position, 6f)
                return@setOnClusterClickListener true
            }
            clusterManager!!.setOnClusterItemClickListener {
                Log.i(tag, "Cluster Item clicked")
                animateZoomInCamera(it.position, 14f)
                clusterManager!!.markerCollection.markers.forEach { marker ->
                    if (marker.title == it.title) {
                        clusterManager!!.markerCollection.markers.forEach { mark ->
                            mark.hideInfoWindow()
                        }
                        marker.showInfoWindow()
                    }
                }
                return@setOnClusterItemClickListener true
            }
            clusterManager!!.setOnClusterInfoWindowClickListener {
                Log.i(tag, "Cluster Marker Info Window clicked")
            }
            Log.i("MapViewModel", "map init")
            onMapInit()
        }
    }

    fun showLastLocationMarkers(users: List<User>?, ctx: Context) {
        if (users != null) {
            map!!.clear()
            clusterManager!!.clearItems()
            clusterManager!!.cluster()
            users.forEach { user ->
                try {
                    if (user.profilepicurl.isBlank()) {
                        mapActiveProfilePicture(ctx, normalMarkers, user, R.drawable.ic_pfp_placeholder)
                    } else {
                        FirebaseStorage.getInstance().getReferenceFromUrl(user.profilepicurl).downloadUrl.addOnCompleteListener {
                            if (it.isSuccessful) {
                                val uri = it.result.toString()
                                mapActiveProfilePicture(ctx = ctx, normalMarkers = normalMarkers, user = user, uri)
                            }
                            if (!it.isSuccessful) {
                                mapActiveProfilePicture(ctx = ctx, normalMarkers, user, R.drawable.ic_pfp_placeholder)
                            }
                        }
                    }
                } catch (ex: Exception) {
                    Log.e("MapViewModel", null, ex)
                    mapActiveProfilePicture(ctx, normalMarkers, user, R.drawable.ic_pfp_placeholder)
                }
            }
        }
    }

    fun animateZoomInCamera(latLng: LatLng, zoom: Float) {
        map!!.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom), 750, null)
    }

    companion object {
        fun MapViewModel.isInit(): Boolean {
            if (this.clusterManager == null || this.clusterRenderer == null || this.normalMarkers == null || this.map == null) {
                return false
            }
            return true
        }

        fun MapViewModel.moveCameraDefaultPosition() {
            map!!.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(39.8097343, -98.5556199), 3f))
        }

        fun MapViewModel.animateCameraDefaultPosition(){
            map!!.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(39.8097343, -98.5556199), 3f))
        }

        //  add all user locations to map, remove latest location markers and cluster
        fun MapViewModel.addLocationsAndPan(ctx: Context, locations: List<UserLocation>) {
            if (locations.isNullOrEmpty()){
                Toast.makeText(ctx, "No locations on record", Toast.LENGTH_SHORT).show()
            } else {
                val bounds = LatLngBounds.builder()
                val clusterItems: MutableList<LocationClusterItem> = mutableListOf()
                locations.forEach { location ->
                    val date = Date(location.timestamp!!.time)
                    val formattedDate = DateFormat.getDateTimeInstance().format(date)
                    val title = "[${location.latlng!!.latitude}, ${location.latlng!!.longitude}]"
                    val clusterItem = LocationClusterItem(
                        lat = location.latlng!!.latitude,
                        lng = location.latlng!!.longitude,
                        title = title,
                        snippet = formattedDate
                    )
                    clusterItems.add(clusterItem)
                    bounds.include(location.latlng!!.toLatLng())
                }
                CoroutineScope(Dispatchers.Main).launch {
                    normalMarkers!!.clear()
                    clusterManager!!.addItems(clusterItems)
                    clusterManager!!.cluster()
                    map!!.setMaxZoomPreference(14.0f)
                    var cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds.build(), 150)
                    map!!.awaitAnimateCamera(cameraUpdate, 750)
                    cameraUpdate = CameraUpdateFactory.scrollBy(0.0f, 200.0f)
                    map!!.awaitAnimateCamera(cameraUpdate = cameraUpdate, 1000)
                    map!!.resetMinMaxZoomPreference()
                }
            }
        }
    }

    init {
        Log.i(tag, "MapViewModel Initialized")
    }
}






