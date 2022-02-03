package com.udacity.project4.locationreminders.savereminder.selectreminderlocation

/**
 * Google Map Zoom Level
 * 1: World
 * 5: Landmass/continent
 * 10: City
 * 15: Streets
 * 20: Buildings
 */

enum class MapZoomLevel(val value: Float) {
    World(1f),
    LandmassContinent(5f),
    City(10f),
    Streets(15f),
    Buildings(20f)
}
