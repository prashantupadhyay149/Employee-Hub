package com.example.employeemanager.ui.map

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.employeemanager.R
import com.example.employeemanager.data.db.AppDatabase
import com.example.employeemanager.data.model.PolygonPoint
import com.example.employeemanager.data.repository.EmployeeRepository
import com.example.employeemanager.databinding.ActivityMapPolygonBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import java.io.File
import kotlin.math.abs
import kotlin.math.sqrt

class MapPolygonActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_EMPLOYEE_ID = "extra_employee_id"
        const val EXTRA_EMPLOYEE_NAME = "extra_employee_name"

        // High-contrast, vibrant Humanitarian OSM street map (No API key needed, never blocks)
        val OSM_HUMANITARIAN = XYTileSource(
            "OSMHumanitarian",
            0, 19, 256, ".png",
            arrayOf(
                "https://a.tile.openstreetmap.fr/hot/",
                "https://b.tile.openstreetmap.fr/hot/"
            ),
            "© OpenStreetMap contributors, Humanitarian OpenStreetMap Team"
        )

        // OpenStreetMap France (Crisp worldwide street tiles with POIs and landmarks)
        val OSM_FRANCE = XYTileSource(
            "OSMFrance",
            0, 19, 256, ".png",
            arrayOf(
                "https://a.tile.openstreetmap.fr/osmfr/",
                "https://b.tile.openstreetmap.fr/osmfr/",
                "https://c.tile.openstreetmap.fr/osmfr/"
            ),
            "© OpenStreetMap France contributors"
        )

        // OpenTopoMap (Clean topographic terrain with elevation contours)
        val OPEN_TOPO = XYTileSource(
            "OpenTopoMap",
            0, 17, 256, ".png",
            arrayOf(
                "https://a.tile.opentopomap.org/",
                "https://b.tile.opentopomap.org/",
                "https://c.tile.opentopomap.org/"
            ),
            "© OpenTopoMap, OpenStreetMap contributors"
        )
    }

    private lateinit var binding: ActivityMapPolygonBinding
    private lateinit var repository: EmployeeRepository

    private var employeeId: Int = -1
    private var employeeName: String = ""

    private val polygonPoints = mutableListOf<GeoPoint>()
    private val markers = mutableListOf<Marker>()
    private var polygonOverlay: Polygon? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ensure internal app cache is used for tile storage (Scoped Storage safe on Android 10-15)
        val osmBase = File(cacheDir, "osmdroid")
        val osmTiles = File(osmBase, "tiles")
        try {
            File(osmTiles, "CartoVoyager").deleteRecursively()
            File(osmTiles, "CartoPositron").deleteRecursively()
            File(osmTiles, "Mapnik").deleteRecursively()
        } catch (_: Exception) {}
        osmBase.mkdirs()
        osmTiles.mkdirs()

        val config = Configuration.getInstance()
        config.load(this, getSharedPreferences("osmdroid_config", Context.MODE_PRIVATE))
        config.osmdroidBasePath = osmBase
        config.osmdroidTileCache = osmTiles

        // Set compliant, non-generic User-Agent so tile servers do not block requests
        val customUA = "EmployeeHubApp/1.0.0 (contact: admin@employeehubapp.internal)"
        config.userAgentValue = customUA
        config.userAgentHttpHeader = "User-Agent"

        // Register custom tile sources with TileSourceFactory
        TileSourceFactory.addTileSource(OSM_HUMANITARIAN)
        TileSourceFactory.addTileSource(OSM_FRANCE)
        TileSourceFactory.addTileSource(OPEN_TOPO)

        binding = ActivityMapPolygonBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Dynamically adjust bottom margin so the HUD card and buttons stay safely ABOVE the Android navigation bar
        applyWindowInsets()

        val dao = AppDatabase.getInstance(this).employeeDao()
        repository = EmployeeRepository(dao)

        employeeId = intent.getIntExtra(EXTRA_EMPLOYEE_ID, -1)
        employeeName = intent.getStringExtra(EXTRA_EMPLOYEE_NAME) ?: "Employee"

        binding.toolbar.title = "$employeeName - Map Area"
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        setupToolbarMenu()

        setupMap()
        loadExistingPolygon()
        setupButtons()
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.cardBottomHud) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val baseMargin = (16 * resources.displayMetrics.density).toInt()
            val params = view.layoutParams as ViewGroup.MarginLayoutParams
            params.bottomMargin = baseMargin + insets.bottom
            view.layoutParams = params
            windowInsets
        }
    }

    private fun setupToolbarMenu() {
        binding.toolbar.inflateMenu(R.menu.menu_map)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_fit_polygon -> {
                    fitMapToPolygon()
                    true
                }
                R.id.action_layers -> {
                    showMapLayerDialog()
                    true
                }
                else -> false
            }
        }
    }

    private fun showMapLayerDialog() {
        val styles = arrayOf("Humanitarian Street (Default)", "OpenStreetMap France", "OpenTopoMap Terrain")
        MaterialAlertDialogBuilder(this)
            .setTitle("Select Map Style")
            .setItems(styles) { _, which ->
                when (which) {
                    0 -> binding.mapView.setTileSource(OSM_HUMANITARIAN)
                    1 -> binding.mapView.setTileSource(OSM_FRANCE)
                    2 -> binding.mapView.setTileSource(OPEN_TOPO)
                }
                binding.mapView.invalidate()
            }
            .show()
    }

    private fun setupMap() {
        val mapView = binding.mapView

        // Use Humanitarian OpenStreetMap by default (rich street tiles, no API key required, 100% reliable)
        mapView.setTileSource(OSM_HUMANITARIAN)
        mapView.setMultiTouchControls(true)
        mapView.setUseDataConnection(true)
        mapView.isTilesScaledToDpi = true
        mapView.minZoomLevel = 3.0
        mapView.maxZoomLevel = 20.0
        mapView.controller.setZoom(15.0)

        // Default initial center: Mumbai, India
        mapView.controller.setCenter(GeoPoint(19.0760, 72.8777))

        // Tap listener to add vertices
        val mapEventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                p?.let { addPoint(it) }
                return true
            }
            override fun longPressHelper(p: GeoPoint?): Boolean = false
        }
        mapView.overlays.add(0, MapEventsOverlay(mapEventsReceiver))
    }

    private fun loadExistingPolygon() {
        lifecycleScope.launch {
            val saved = repository.getPolygonPoints(employeeId)
            if (saved.isNotEmpty()) {
                polygonPoints.clear()
                saved.forEach { polygonPoints.add(GeoPoint(it.latitude, it.longitude)) }
                refreshMapOverlays()

                // Center camera on the saved polygon
                binding.mapView.post { fitMapToPolygon() }
            }
        }
    }

    private fun fitMapToPolygon() {
        if (polygonPoints.isEmpty()) return
        if (polygonPoints.size == 1) {
            binding.mapView.controller.animateTo(polygonPoints[0])
            binding.mapView.controller.setZoom(16.0)
            return
        }
        val minLat = polygonPoints.minOf { it.latitude }
        val maxLat = polygonPoints.maxOf { it.latitude }
        val minLon = polygonPoints.minOf { it.longitude }
        val maxLon = polygonPoints.maxOf { it.longitude }

        // Add padding around bounding box
        val padLat = (maxLat - minLat).coerceAtLeast(0.002) * 0.25
        val padLon = (maxLon - minLon).coerceAtLeast(0.002) * 0.25

        val box = BoundingBox(maxLat + padLat, maxLon + padLon, minLat - padLat, minLon - padLon)
        binding.mapView.zoomToBoundingBox(box, true, 120)
    }

    private fun addPoint(geoPoint: GeoPoint) {
        polygonPoints.add(geoPoint)
        refreshMapOverlays()
    }

    private fun refreshMapOverlays() {
        val mapView = binding.mapView

        // Remove existing polygon and markers (retaining the events overlay at index 0)
        mapView.overlays.removeAll { it is Polygon || it is Marker }
        markers.clear()

        // Create draggable marker for each vertex
        polygonPoints.forEachIndexed { index, geoPoint ->
            val marker = Marker(mapView).apply {
                position = geoPoint
                title = "Point ${index + 1}"
                snippet = "Drag to move • Tap to remove"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = resources.getDrawable(R.drawable.bg_marker_pin, theme)
                isDraggable = true

                // Drag listener to support moving existing polygon points in real time
                setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                    override fun onMarkerDragStart(m: Marker) {}

                    override fun onMarkerDrag(m: Marker) {
                        val pos = markers.indexOf(m)
                        if (pos != -1 && pos < polygonPoints.size) {
                            polygonPoints[pos] = m.position
                            updatePolygonOnly()
                        }
                    }

                    override fun onMarkerDragEnd(m: Marker) {
                        val pos = markers.indexOf(m)
                        if (pos != -1 && pos < polygonPoints.size) {
                            polygonPoints[pos] = m.position
                            refreshMapOverlays()
                        }
                    }
                })

                // Tap to delete this vertex
                setOnMarkerClickListener { _, _ ->
                    MaterialAlertDialogBuilder(this@MapPolygonActivity)
                        .setTitle("Remove Point ${index + 1}?")
                        .setMessage("Remove vertex at (${String.format("%.5f", geoPoint.latitude)}, ${String.format("%.5f", geoPoint.longitude)})?")
                        .setPositiveButton("Remove") { _, _ ->
                            if (index < polygonPoints.size) {
                                polygonPoints.removeAt(index)
                                refreshMapOverlays()
                            }
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                    true
                }
            }
            markers.add(marker)
            mapView.overlays.add(marker)
        }

        // Draw polygon overlay if 3+ vertices
        updatePolygonOnly()

        mapView.invalidate()
        updateHUD()
    }

    private fun updatePolygonOnly() {
        val mapView = binding.mapView
        polygonOverlay?.let { mapView.overlays.remove(it) }

        if (polygonPoints.size >= 3) {
            val closedPoints = polygonPoints.toMutableList().also { it.add(it[0]) }
            polygonOverlay = Polygon(mapView).apply {
                points = closedPoints
                fillPaint.color = resources.getColor(R.color.polygon_fill, theme)
                outlinePaint.color = resources.getColor(R.color.polygon_stroke, theme)
                outlinePaint.strokeWidth = 4f
            }
            mapView.overlays.add(1, polygonOverlay)
        }
        updateHUD()
    }

    private fun updateHUD() {
        val count = polygonPoints.size
        binding.tvVertexCount.text = "Vertices: $count"

        if (count >= 3) {
            val areaSqM = calculatePolygonArea(polygonPoints)
            val areaStr = when {
                areaSqM >= 1_000_000 -> String.format("%.2f km²", areaSqM / 1_000_000.0)
                areaSqM >= 10_000 -> String.format("%.2f ha", areaSqM / 10_000.0)
                else -> String.format("%.0f m²", areaSqM)
            }
            binding.tvPolygonArea.text = "Area: $areaStr"

            val perimM = calculatePerimeter(polygonPoints)
            val perimStr = if (perimM >= 1000.0) String.format("%.2f km", perimM / 1000.0)
                           else String.format("%.0f m", perimM)
            binding.tvPolygonPerimeter.text = "P: $perimStr"
        } else {
            binding.tvPolygonArea.text = "Area: N/A"
            binding.tvPolygonPerimeter.text = "P: N/A"
        }
    }

    /**
     * Calculates geodesic polygon area accurately using an equirectangular planar projection
     * centered on the polygon centroid.
     */
    private fun calculatePolygonArea(points: List<GeoPoint>): Double {
        if (points.size < 3) return 0.0
        val earthRadius = 6371000.0 // Earth radius in meters
        val avgLatRad = Math.toRadians(points.map { it.latitude }.average())
        var area = 0.0
        val n = points.size
        for (i in points.indices) {
            val j = (i + 1) % n
            val xi = Math.toRadians(points[i].longitude) * earthRadius * Math.cos(avgLatRad)
            val yi = Math.toRadians(points[i].latitude) * earthRadius
            val xj = Math.toRadians(points[j].longitude) * earthRadius * Math.cos(avgLatRad)
            val yj = Math.toRadians(points[j].latitude) * earthRadius
            area += (xi * yj - xj * yi)
        }
        return abs(area) / 2.0
    }

    private fun calculatePerimeter(points: List<GeoPoint>): Double {
        if (points.size < 2) return 0.0
        var perimeter = 0.0
        val n = points.size
        for (i in points.indices) {
            val j = (i + 1) % n
            perimeter += haversineDistance(points[i], points[j])
        }
        return perimeter
    }

    private fun haversineDistance(p1: GeoPoint, p2: GeoPoint): Double {
        val R = 6371000.0
        val lat1 = Math.toRadians(p1.latitude)
        val lat2 = Math.toRadians(p2.latitude)
        val dLat = Math.toRadians(p2.latitude - p1.latitude)
        val dLon = Math.toRadians(p2.longitude - p1.longitude)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }

    private fun setupButtons() {
        binding.btnSavePolygon.setOnClickListener {
            if (polygonPoints.size < 3) {
                Toast.makeText(this, getString(R.string.polygon_need_more_points), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                val points = polygonPoints.mapIndexed { index, geoPoint ->
                    PolygonPoint(
                        employeeId = employeeId,
                        latitude = geoPoint.latitude,
                        longitude = geoPoint.longitude,
                        pointOrder = index
                    )
                }
                repository.savePolygonPoints(employeeId, points)
                Snackbar.make(binding.root, getString(R.string.polygon_saved_success), Snackbar.LENGTH_SHORT).show()
            }
        }

        binding.btnClearPolygon.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.dialog_clear_polygon_title))
                .setMessage(getString(R.string.dialog_clear_polygon_message))
                .setPositiveButton("Clear") { _, _ ->
                    lifecycleScope.launch {
                        repository.clearPolygonPoints(employeeId)
                        polygonPoints.clear()
                        refreshMapOverlays()
                        Toast.makeText(this@MapPolygonActivity,
                            getString(R.string.polygon_cleared_success), Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.btnUndoPoint.setOnClickListener {
            if (polygonPoints.isNotEmpty()) {
                polygonPoints.removeAt(polygonPoints.size - 1)
                refreshMapOverlays()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }
}
