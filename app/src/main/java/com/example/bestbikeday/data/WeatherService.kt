package com.example.bestbikeday.data

import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherService {
    @GET("onecall")
    suspend fun getWeatherForecast(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("exclude") exclude: String = "current,minutely,hourly,alerts",
        @Query("units") units: String = "metric",
        @Query("appid") apiKey: String
    ): WeatherResponse
} 