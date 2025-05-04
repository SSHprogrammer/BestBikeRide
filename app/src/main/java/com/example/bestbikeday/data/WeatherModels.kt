package com.example.bestbikeday.data

data class WeatherResponse(
    val daily: List<DailyForecast>
)

data class DailyForecast(
    val dt: Long,
    val temp: Temperature,
    val weather: List<Weather>,
    val humidity: Int,
    val wind_speed: Double,
    val pop: Double, // Probability of precipitation (0.0 - 1.0)
    val score: Int? = null // Bike ride score (0-100)
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