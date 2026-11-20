package com.example.employeemanager

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

class PolygonMathTest {

    private data class LatLng(val lat: Double, val lon: Double)

    private fun calculatePolygonArea(points: List<LatLng>): Double {
        if (points.size < 3) return 0.0
        val earthRadius = 6371000.0
        val avgLatRad = Math.toRadians(points.map { it.lat }.average())
        var area = 0.0
        val n = points.size
        for (i in points.indices) {
            val j = (i + 1) % n
            val xi = Math.toRadians(points[i].lon) * earthRadius * Math.cos(avgLatRad)
            val yi = Math.toRadians(points[i].lat) * earthRadius
            val xj = Math.toRadians(points[j].lon) * earthRadius * Math.cos(avgLatRad)
            val yj = Math.toRadians(points[j].lat) * earthRadius
            area += (xi * yj - xj * yi)
        }
        return abs(area) / 2.0
    }

    private fun haversineDistance(p1: LatLng, p2: LatLng): Double {
        val R = 6371000.0
        val lat1 = Math.toRadians(p1.lat)
        val lat2 = Math.toRadians(p2.lat)
        val dLat = Math.toRadians(p2.lat - p1.lat)
        val dLon = Math.toRadians(p2.lon - p1.lon)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }

    @Test
    fun testPolygonAreaPositive() {
        val points = listOf(
            LatLng(19.0760, 72.8777),
            LatLng(19.0800, 72.8777),
            LatLng(19.0800, 72.8850)
        )
        val area = calculatePolygonArea(points)
        assertTrue("Area should be greater than 0", area > 0.0)
        // Triangle approx 440m x 760m / 2 ~ 167,000 m2
        assertTrue("Area should be roughly 100,000 - 300,000 m2", area in 100000.0..300000.0)
    }

    @Test
    fun testPolygonLessThanThreePointsReturnsZero() {
        val points = listOf(
            LatLng(19.0760, 72.8777),
            LatLng(19.0800, 72.8777)
        )
        val area = calculatePolygonArea(points)
        assertEquals(0.0, area, 0.0001)
    }

    @Test
    fun testPerimeterCalculation() {
        val p1 = LatLng(19.0760, 72.8777)
        val p2 = LatLng(19.0800, 72.8777)
        val dist = haversineDistance(p1, p2)
        assertTrue("Distance should be approximately 400-500 meters", dist in 400.0..500.0)
    }
}
