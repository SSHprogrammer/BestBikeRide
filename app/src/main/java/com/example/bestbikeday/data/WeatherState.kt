package com.example.bestbikeday.data

sealed class WeatherState {
    object Loading : WeatherState()
    data class Success(val forecasts: List<DailyForecast>) : WeatherState()
    data class Error(val message: String) : WeatherState()
}

data class DailyForecast(
    val dt: Long,
    val temp: Temperature,
    val humidity: Int,
    val wind_speed: Double,
    val weather: List<Weather>,
    val pop: Double,
    val score: Int? = null
)

data class Temperature(
    val day: Double,
    val min: Double,
    val max: Double
)

data class Weather(
    val id: Int,
    val main: String,
    val description: String,
    val icon: String
)

data class Location(
    val name: String,
    val lat: Double,
    val lon: Double,
    val country: String,
    val state: String? = null
) {
    val displayName: String
        get() = if (state != null) "$name, $state, $country" else "$name, $country"
}

data class BikeRideRecommendation(
    val date: Date,
    val temperature: Double,
    val rainChance: Double,
    val windSpeed: Double,
    val score: Int
) 