package com.bond.bondbuddy.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.view.animation.AccelerateInterpolator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.bond.bondbuddy.R
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import com.google.maps.android.collections.MarkerManager
import com.google.maps.android.ktx.utils.collection.addMarker

/**
 * Remembers a MapView and gives it the lifecycle of the current LifecycleOwner
 */
@Composable
fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val mapView = remember {
        MapView(
            context
        ).apply {
            id = R.id.map
        }
    }
    // Makes MapView follow the lifecycle of this composable
    val lifecycleObserver = rememberMapLifecycleObserver(
        mapView
    )
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(
        lifecycle
    ) {
        lifecycle.addObserver(
            lifecycleObserver
        )
        onDispose {
            lifecycle.removeObserver(
                lifecycleObserver
            )
        }
    }
    return mapView
}

@Composable
private fun rememberMapLifecycleObserver(
    mapView: MapView
): LifecycleEventObserver = remember(mapView) {
    LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_CREATE -> mapView.onCreate(
               Bundle()
            )
            Lifecycle.Event.ON_START -> mapView.onStart()
            Lifecycle.Event.ON_RESUME -> mapView.onResume()
            Lifecycle.Event.ON_PAUSE -> mapView.onPause()
            Lifecycle.Event.ON_STOP -> mapView.onStop()
            Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
            else -> throw IllegalStateException()
        }
    }
}

class LocationClusterItem(
    lat: Double,
    lng: Double,
    private val title: String,
    private val snippet: String? = null,
    private val userID: String? = null
) : ClusterItem {
    private val position: LatLng = LatLng(lat, lng)

    override fun getPosition(): LatLng {
        return position
    }

    override fun getTitle(): String {
        return title
    }

    override fun getSnippet(): String? {
        return snippet
    }

    fun getUserID(): String? {
        return userID
    }
}

class LocationClusterRenderer(
    val ctx: Context,
    private val map: GoogleMap,
    clusterManager: ClusterManager<LocationClusterItem>,
    private var currentZoomLevel: Float,
    private val maxZoomLevel: Float
) : DefaultClusterRenderer<LocationClusterItem>(ctx, map, clusterManager),
    GoogleMap.OnCameraMoveListener {
    override fun onCameraMove() {
        currentZoomLevel = map.cameraPosition.zoom
    }

    override fun shouldRenderAsCluster(cluster: Cluster<LocationClusterItem>): Boolean {
        return super.shouldRenderAsCluster(cluster) && currentZoomLevel < maxZoomLevel
    }

    //  customize cluster item marker
    override fun onBeforeClusterItemRendered(
        item: LocationClusterItem, markerOptions: MarkerOptions
    ) {
        markerOptions.title(item.title)
        super.onBeforeClusterItemRendered(item, markerOptions)
    }

    override fun onClusterRendered(cluster: Cluster<LocationClusterItem>, marker: Marker) {
        super.onClusterRendered(cluster, marker)
        marker.showInfoWindow()
        animateMarkerDropping(marker)
    }

    override fun onClusterItemRendered(clusterItem: LocationClusterItem, marker: Marker) {
        val drawable = ContextCompat.getDrawable(ctx, R.drawable.ic_userblip)
        val bitmap = drawable?.toBitmap()
        val bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(bitmap!!)
        marker.setIcon(bitmapDescriptor)
        super.onClusterItemRendered(clusterItem, marker)
        animateMarkerDropping(marker)
    }

    private fun animateMarkerDropping(marker: Marker) {
        val handler = Handler(ctx.mainLooper)
        val start = SystemClock.uptimeMillis()
        val duration = 500
        val interpolator = AccelerateInterpolator()
        handler.post(object : Runnable {
            override fun run() {
                val elapsed = SystemClock.uptimeMillis() - start
                val t =
                    (1 - interpolator.getInterpolation(((elapsed / duration).toFloat()))).coerceAtLeast(
                        (0).toFloat()
                    )
                marker.setAnchor(0.5f, 1f + 14 * t)

                if (t > 0) {
                    handler.postDelayed(this, 15)
                }
            }
        })
    }

    init {
        this.minClusterSize = 3
    }
}

// glide image loading for map avatars
fun mapActiveProfilePicture(
    ctx: Context,
    normalMarkers: MarkerManager.Collection?,
    user: com.bond.bondbuddy.models.User,
    loadingPlaceHolder: Any?,
){
    Glide.with(ctx)
        .asBitmap()
        .load(loadingPlaceHolder)
        .circleCrop()
        .override(100,100)
        .placeholder(R.drawable.ic_pfp_placeholder)
        .into(object : CustomTarget<Bitmap>() {
            override fun onResourceReady(
                resource: Bitmap, transition: Transition<in Bitmap>?
            ) {
                val marker = normalMarkers!!.addMarker {
                    this.position(LatLng(user.lastlocation.latlng!!.latitude, user.lastlocation.latlng!!.longitude))
                    this.title(user.displayname)
                    this.snippet("Tap To View All Locations")
                    this.icon(BitmapDescriptorFactory.fromBitmap(resource))
                    if (!user.active || user.cached){
                        this.alpha(0.5f)
                    }
                }
                marker.tag = user.id
            }
            override fun onLoadCleared(placeholder: Drawable?) {
                val marker = normalMarkers!!.addMarker {
                    this.position(LatLng(user.lastlocation.latlng!!.latitude, user.lastlocation.latlng!!.longitude))
                    this.title(user.displayname)
                    this.snippet("Tap To View All Locations")
                    this.icon(BitmapDescriptorFactory.fromBitmap(placeholder!!.toBitmap()))
                    if (!user.active || user.cached){
                        this.alpha(0.4f)
                    }
                }
                marker.tag = user.id
            }
        })
}