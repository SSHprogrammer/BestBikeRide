package com.example.bestbikeday.data

import retrofit2.http.GET
import retrofit2.http.Query

interface LocationService {
    @GET("geo/1.0/direct")
    suspend fun searchLocations(
        @Query("q") query: String,
        @Query("limit") limit: Int = 5,
        @Query("appid") apiKey: String
    ): List<Location>

    @GET("geo/1.0/zip")
    suspend fun searchByPostalCode(
        @Query("zip") postalCode: String,
        @Query("appid") apiKey: String
    ): Location
} 