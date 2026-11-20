package com.example.employeemanager

import android.app.Application
import android.content.Context
import org.osmdroid.config.Configuration
import java.io.File

class EmployeeApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Ensure OsmDroid uses the app's internal private cache to avoid Scoped Storage permission issues on Android 10+
        val osmBase = File(cacheDir, "osmdroid")
        val osmTiles = File(osmBase, "tiles")
        osmBase.mkdirs()
        osmTiles.mkdirs()

        val config = Configuration.getInstance()
        val prefs = getSharedPreferences("osmdroid_config", Context.MODE_PRIVATE)
        config.load(this, prefs)
        config.osmdroidBasePath = osmBase
        config.osmdroidTileCache = osmTiles

        // Set compliant, non-generic User-Agent so OSM servers do not block requests
        val customUserAgent = "EmployeeHubApp/1.0.0 (contact: admin@employeehubapp.internal)"
        config.userAgentValue = customUserAgent
        config.userAgentHttpHeader = "User-Agent"
    }
}
