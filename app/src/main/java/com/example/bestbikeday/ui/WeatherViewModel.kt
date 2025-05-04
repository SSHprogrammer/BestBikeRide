package com.example.bestbikeday.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bestbikeday.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WeatherViewModel : ViewModel() {
    private val _weatherState = MutableStateFlow<WeatherState>(WeatherState.Loading)
    val weatherState: StateFlow<WeatherState> = _weatherState.asStateFlow()

    private val _locationSearchResults = MutableStateFlow<List<Location>>(emptyList())
    val locationSearchResults: StateFlow<List<Location>> = _locationSearchResults.asStateFlow()

    private val _selectedLocation = MutableStateFlow<Location?>(null)
    val selectedLocation: StateFlow<Location?> = _selectedLocation.asStateFlow()

    private val _recommendations = MutableStateFlow<List<BikeRideRecommendation>>(emptyList())
    val recommendations: StateFlow<List<BikeRideRecommendation>> = _recommendations.asStateFlow()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.openweathermap.org/data/3.0/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val weatherService = retrofit.create(WeatherService::class.java)
    private val locationService = retrofit.create(LocationService::class.java)

    fun searchLocations(query: String) {
        viewModelScope.launch {
            try {
                val results = locationService.searchLocations(
                    query = query,
                    apiKey = "YOUR_API_KEY" // Replace with your OpenWeatherMap API key
                )
                _locationSearchResults.value = results
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun selectLocation(location: Location) {
        _selectedLocation.value = location
        fetchWeatherForecast(location.lat, location.lon)
    }

    fun fetchWeatherForecast(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            try {
                _weatherState.value = WeatherState.Loading
                val response = weatherService.getWeatherForecast(
                    latitude = latitude,
                    longitude = longitude,
                    apiKey = "YOUR_API_KEY" // Replace with your OpenWeatherMap API key
                )
                val scored = response.daily.map { it.copy(score = calculateBikeScore(it)) }
                _weatherState.value = WeatherState.Success(scored)
                updateRecommendations(scored)
            } catch (e: Exception) {
                _weatherState.value = WeatherState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun updateRecommendations(forecasts: List<DailyForecast>) {
        val recommendations = forecasts.map { forecast ->
            BikeRideRecommendation(
                date = Date(forecast.dt * 1000),
                score = forecast.score ?: 0,
                temperature = forecast.temp.day,
                rainChance = forecast.pop,
                windSpeed = forecast.wind_speed
            )
        }
        _recommendations.value = recommendations
    }

    // Algorithm for bike ride score (0-100%)
    private fun calculateBikeScore(forecast: DailyForecast): Int {
        // Temperature: ideal 18-25°C
        val tempScore = when {
            forecast.temp.day in 18.0..25.0 -> 1.0
            forecast.temp.day in 15.0..18.0 || forecast.temp.day in 25.0..28.0 -> 0.7
            forecast.temp.day in 10.0..15.0 || forecast.temp.day in 28.0..32.0 -> 0.4
            else -> 0.1
        }
        // Rain: ideal 0%
        val rainScore = 1.0 - forecast.pop // pop is 0.0 (no rain) to 1.0 (100% rain)
        // Wind: ideal < 6 m/s
        val windScore = when {
            forecast.wind_speed < 6 -> 1.0
            forecast.wind_speed < 9 -> 0.7
            forecast.wind_speed < 12 -> 0.4
            else -> 0.1
        }
        // Weighted sum (adjust weights as needed)
        val score = (tempScore * 0.5 + rainScore * 0.3 + windScore * 0.2) * 100
        return score.toInt().coerceIn(0, 100)
    }
}

data class BikeRideRecommendation(
    val date: Date,
    val score: Int,
    val temperature: Double,
    val rainChance: Double,
    val windSpeed: Double
)

sealed class WeatherState {
    object Loading : WeatherState()
    data class Success(val forecasts: List<DailyForecast>) : WeatherState()
    data class Error(val message: String) : WeatherState()
} 