package com.xiaoyin.lifeatlas.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.Executor

class CurrentLocationProvider(context: Context) {
    private val appContext = context.applicationContext
    private val locationManager = appContext.getSystemService(LocationManager::class.java)

    fun hasLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fineGranted || coarseGranted
    }

    @SuppressLint("MissingPermission")
    fun requestCurrentLocation(onResult: (Location?) -> Unit) {
        if (!hasLocationPermission()) {
            onResult(null)
            return
        }

        val provider = bestProvider()
        if (provider == null) {
            onResult(null)
            return
        }

        val lastKnownLocation = lastKnownLocation()
        if (lastKnownLocation != null) {
            onResult(lastKnownLocation)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            locationManager.getCurrentLocation(
                provider,
                CancellationSignal(),
                MainThreadExecutor,
                { location ->
                    if (location != null) {
                        onResult(location)
                    } else {
                        requestSingleUpdate(provider, onResult)
                    }
                }
            )
        } else {
            requestSingleUpdate(provider, onResult)
        }
    }

    private fun bestProvider(): String? {
        return when {
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            else -> null
        }
    }

    @SuppressLint("MissingPermission")
    private fun lastKnownLocation(): Location? {
        if (!hasLocationPermission()) return null
        return listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            .mapNotNull { provider ->
                runCatching {
                    if (locationManager.allProviders.contains(provider)) {
                        locationManager.getLastKnownLocation(provider)
                    } else {
                        null
                    }
                }.getOrNull()
            }
            .maxByOrNull { it.time }
    }

    @SuppressLint("MissingPermission")
    private fun requestSingleUpdate(provider: String, onResult: (Location?) -> Unit) {
        val delivered = AtomicBoolean(false)
        val handler = Handler(Looper.getMainLooper())
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (delivered.compareAndSet(false, true)) {
                    locationManager.removeUpdates(this)
                    onResult(location)
                }
            }

            override fun onProviderDisabled(provider: String) {
                if (delivered.compareAndSet(false, true)) {
                    locationManager.removeUpdates(this)
                    onResult(null)
                }
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        }
        locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
        handler.postDelayed({
            if (delivered.compareAndSet(false, true)) {
                locationManager.removeUpdates(listener)
                onResult(lastKnownLocation())
            }
        }, LOCATION_TIMEOUT_MS)
    }

    private object MainThreadExecutor : Executor {
        private val handler = Handler(Looper.getMainLooper())

        override fun execute(command: Runnable) {
            handler.post(command)
        }
    }

    private companion object {
        const val LOCATION_TIMEOUT_MS = 8_000L
    }
}
