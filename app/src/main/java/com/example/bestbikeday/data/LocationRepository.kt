package com.example.bestbikeday.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.*

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class LocationRepository(private val context: Context) {
    private val gson = Gson()
    private val favoritesKey = stringPreferencesKey("favorite_locations")
    private val cacheKey = stringPreferencesKey("weather_cache")
    private val cacheTimestampKey = stringPreferencesKey("cache_timestamp")

    suspend fun addFavorite(location: Location) {
        val favorites = getFavorites().toMutableList()
        if (!favorites.any { it.name == location.name && it.country == location.country }) {
            favorites.add(location)
            saveFavorites(favorites)
        }
    }

    suspend fun removeFavorite(location: Location) {
        val favorites = getFavorites().toMutableList()
        favorites.removeIf { it.name == location.name && it.country == location.country }
        saveFavorites(favorites)
    }

    fun getFavoritesFlow(): Flow<List<Location>> {
        return context.dataStore.data.map { preferences ->
            val json = preferences[favoritesKey] ?: "[]"
            gson.fromJson(json, object : TypeToken<List<Location>>() {}.type)
        }
    }

    private suspend fun getFavorites(): List<Location> {
        val json = context.dataStore.data.map { preferences ->
            preferences[favoritesKey] ?: "[]"
        }.first()
        return gson.fromJson(json, object : TypeToken<List<Location>>() {}.type)
    }

    private suspend fun saveFavorites(favorites: List<Location>) {
        context.dataStore.edit { preferences ->
            preferences[favoritesKey] = gson.toJson(favorites)
        }
    }

    suspend fun cacheWeatherData(forecasts: List<DailyForecast>) {
        context.dataStore.edit { preferences ->
            preferences[cacheKey] = gson.toJson(forecasts)
            preferences[cacheTimestampKey] = Date().time.toString()
        }
    }

    suspend fun getCachedWeatherData(): List<DailyForecast>? {
        val json = context.dataStore.data.map { preferences ->
            preferences[cacheKey] ?: return@map null
        }.first() ?: return null

        val timestamp = context.dataStore.data.map { preferences ->
            preferences[cacheTimestampKey]?.toLong() ?: return@map null
        }.first() ?: return null

        // Cache is valid for 30 minutes
        if (Date().time - timestamp > 30 * 60 * 1000) {
            return null
        }

        return gson.fromJson(json, object : TypeToken<List<DailyForecast>>() {}.type)
    }
} 