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
import androidx.core.content.ContextCompat
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            locationManager.getCurrentLocation(
                provider,
                CancellationSignal(),
                DirectExecutor,
                onResult
            )
        } else {
            val lastKnownLocation = locationManager.getLastKnownLocation(provider)
            if (lastKnownLocation != null) {
                onResult(lastKnownLocation)
                return
            }

            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    locationManager.removeUpdates(this)
                    onResult(location)
                }

                override fun onProviderDisabled(provider: String) {
                    locationManager.removeUpdates(this)
                    onResult(null)
                }

                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            }
            locationManager.requestSingleUpdate(provider, listener, null)
        }
    }

    private fun bestProvider(): String? {
        return when {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> null
        }
    }

    private object DirectExecutor : Executor {
        override fun execute(command: Runnable) {
            command.run()
        }
    }
}
