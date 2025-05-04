package com.example.bestbikeday.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bestbikeday.data.DailyForecast
import com.example.bestbikeday.data.Location
import com.example.bestbikeday.data.WeatherState
import com.example.bestbikeday.data.BikeRideRecommendation
import com.example.bestbikeday.service.WeatherService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class WeatherViewModel(
    private val weatherService: WeatherService = WeatherService()
) : ViewModel() {

    private val _weatherState = MutableStateFlow<WeatherState>(WeatherState.Loading)
    val weatherState: StateFlow<WeatherState> = _weatherState.asStateFlow()

    private val _locationSearchResults = MutableStateFlow<List<Location>>(emptyList())
    val locationSearchResults: StateFlow<List<Location>> = _locationSearchResults.asStateFlow()

    private val _selectedLocation = MutableStateFlow<Location?>(null)
    val selectedLocation: StateFlow<Location?> = _selectedLocation.asStateFlow()

    private val _recommendations = MutableStateFlow<List<BikeRideRecommendation>>(emptyList())
    val recommendations: StateFlow<List<BikeRideRecommendation>> = _recommendations.asStateFlow()

    private val _lastUpdated = MutableStateFlow<Date?>(null)
    val lastUpdated: StateFlow<Date?> = _lastUpdated.asStateFlow()

    private var currentLocation: Location? = null

    init {
        refreshWeatherData()
    }

    fun refreshWeatherData() {
        viewModelScope.launch {
            try {
                _weatherState.value = WeatherState.Loading
                currentLocation?.let { location ->
                    val forecasts = weatherService.getWeatherForecast(location.lat, location.lon)
                    _weatherState.value = WeatherState.Success(forecasts)
                    _lastUpdated.value = Date()
                    updateRecommendations(forecasts)
                } ?: run {
                    _weatherState.value = WeatherState.Error("No location selected")
                }
            } catch (e: Exception) {
                _weatherState.value = WeatherState.Error(
                    when (e) {
                        is java.net.UnknownHostException -> "No internet connection"
                        is java.net.SocketTimeoutException -> "Connection timed out"
                        else -> "Failed to load weather data: ${e.message}"
                    }
                )
            }
        }
    }

    fun searchLocations(query: String) {
        viewModelScope.launch {
            try {
                val results = weatherService.searchLocations(query)
                _locationSearchResults.value = results
            } catch (e: Exception) {
                _locationSearchResults.value = emptyList()
            }
        }
    }

    fun searchByPostalCode(postalCode: String) {
        viewModelScope.launch {
            try {
                val results = weatherService.searchByPostalCode(postalCode)
                _locationSearchResults.value = results
            } catch (e: Exception) {
                _locationSearchResults.value = emptyList()
            }
        }
    }

    fun selectLocation(location: Location) {
        currentLocation = location
        _selectedLocation.value = location
        _locationSearchResults.value = emptyList()
        refreshWeatherData()
    }

    private fun updateRecommendations(forecasts: List<DailyForecast>) {
        val recommendations = forecasts.map { forecast ->
            BikeRideRecommendation(
                date = Date(forecast.dt * 1000),
                temperature = forecast.temp.day,
                rainChance = forecast.pop,
                windSpeed = forecast.wind_speed,
                score = calculateBikeScore(forecast)
            )
        }
        _recommendations.value = recommendations
    }

    private fun calculateBikeScore(forecast: DailyForecast): Int {
        var score = 100

        // Temperature penalty (ideal range: 15-25°C)
        val temp = forecast.temp.day
        when {
            temp < 5 -> score -= 40
            temp < 10 -> score -= 20
            temp > 30 -> score -= 30
            temp > 25 -> score -= 15
        }

        // Rain chance penalty
        score -= (forecast.pop * 100).toInt()

        // Wind speed penalty
        when {
            forecast.wind_speed > 10 -> score -= 30
            forecast.wind_speed > 5 -> score -= 15
        }

        return score.coerceIn(0, 100)
    }
} 