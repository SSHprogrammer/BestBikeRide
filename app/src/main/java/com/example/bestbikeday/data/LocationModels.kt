package com.example.bestbikeday.data

data class LocationSearchResponse(
    val results: List<Location>
)

data class Location(
    val name: String,
    val country: String,
    val state: String?,
    val lat: Double,
    val lon: Double
) {
    val displayName: String
        get() = if (state != null) {
            "$name, $state, $country"
        } else {
            "$name, $country"
        }
} 