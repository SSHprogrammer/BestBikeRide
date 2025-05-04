package com.example.bestbikeday.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.bestbikeday.data.DailyForecast
import com.example.bestbikeday.data.Location
import com.example.bestbikeday.data.LocationRepository
import com.example.bestbikeday.ui.theme.ThemeManager
import com.example.bestbikeday.ui.theme.ThemeMode
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val weatherState by viewModel.weatherState.collectAsState()
    val locationSearchResults by viewModel.locationSearchResults.collectAsState()
    val selectedLocation by viewModel.selectedLocation.collectAsState()
    val recommendations by viewModel.recommendations.collectAsState()
    val lastUpdated by viewModel.lastUpdated.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var showMap by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var searchByPostalCode by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }

    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    var isRefreshing by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "loading")

    val scale1 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale1"
    )
    val scale2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing, delayMillis = 200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale2"
    )
    val scale3 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing, delayMillis = 400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale3"
    )

    var offsetX by remember { mutableStateOf(0f) }
    var currentTab by remember { mutableStateOf(0) }

    val uriHandler = LocalUriHandler.current
    val locationRepository = remember { LocationRepository(context) }
    val favorites by locationRepository.getFavoritesFlow().collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        AnimatedContent(
                            targetState = selectedLocation?.name ?: "Best Bike Day",
                            transitionSpec = {
                                fadeIn() + slideInVertically() with fadeOut() + slideOutVertically()
                            }
                        ) { target ->
                            Text(target)
                        }
                        lastUpdated?.let { timestamp ->
                            Text(
                                text = "Last updated: ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(timestamp)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                actions = {
                    selectedLocation?.let { location ->
                        IconButton(
                            onClick = { 
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModelScope.launch {
                                    if (favorites.any { it.name == location.name && it.country == location.country }) {
                                        locationRepository.removeFavorite(location)
                                    } else {
                                        locationRepository.addFavorite(location)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (favorites.any { it.name == location.name && it.country == location.country }) {
                                    Icons.Default.Favorite
                                } else {
                                    Icons.Default.FavoriteBorder
                                },
                                contentDescription = if (favorites.any { it.name == location.name && it.country == location.country }) {
                                    "Remove from favorites"
                                } else {
                                    "Add to favorites"
                                },
                                tint = if (favorites.any { it.name == location.name && it.country == location.country }) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                        IconButton(
                            onClick = { 
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                // Share weather data
                                val shareText = buildString {
                                    append("Weather in ${location.name}:\n")
                                    (weatherState as? WeatherState.Success)?.forecasts?.forEach { forecast ->
                                        val date = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
                                            .format(Date(forecast.dt * 1000))
                                        append("\n$date: ${forecast.temp.day.toInt()}°C, ")
                                        append(forecast.weather.firstOrNull()?.description ?: "")
                                    }
                                }
                                uriHandler.openUri("mailto:?subject=Weather Forecast&body=$shareText")
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share weather data"
                            )
                        }
                    }
                    IconButton(
                        onClick = { 
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            showThemeDialog = true 
                        }
                    ) {
                        Icon(
                            imageVector = when (ThemeManager.themeMode) {
                                ThemeMode.LIGHT -> Icons.Default.LightMode
                                ThemeMode.DARK -> Icons.Default.DarkMode
                                ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                            },
                            contentDescription = "Change theme"
                        )
                    }
                    IconButton(
                        onClick = { 
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            showMap = !showMap 
                        }
                    ) {
                        Icon(
                            imageVector = if (showMap) Icons.Default.List else Icons.Default.Map,
                            contentDescription = if (showMap) "Show list view" else "Show map view"
                        )
                    }
                    IconButton(
                        onClick = { 
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.refreshWeatherData()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh weather data"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pointerInput(Unit) {
                    detectVerticalDragGestures { change, dragAmount ->
                        if (dragAmount > 50 && !isRefreshing) {
                            isRefreshing = true
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.refreshWeatherData()
                            isRefreshing = false
                        }
                    }
                }
        ) {
            when (weatherState) {
                is WeatherState.Loading -> {
                    LoadingAnimation()
                }
                is WeatherState.Error -> {
                    ErrorMessage(
                        message = (weatherState as WeatherState.Error).message,
                        onRetry = { viewModel.refreshWeatherData() }
                    )
                }
                is WeatherState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp)
                    ) {
                        // Search Bar with Postal Code Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { 
                                    searchQuery = it
                                    if (it.length >= 2) {
                                        isSearching = true
                                        if (searchByPostalCode) {
                                            viewModel.searchByPostalCode(it)
                                        } else {
                                            viewModel.searchLocations(it)
                                        }
                                    } else {
                                        isSearching = false
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .animateContentSize(),
                                label = { 
                                    AnimatedContent(
                                        targetState = if (searchByPostalCode) "Postal Code" else "Search location",
                                        transitionSpec = {
                                            fadeIn() + slideInHorizontally() with fadeOut() + slideOutHorizontally()
                                        }
                                    ) { target ->
                                        Text(target)
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        if (searchQuery.length >= 2) {
                                            if (searchByPostalCode) {
                                                viewModel.searchByPostalCode(searchQuery)
                                            } else {
                                                viewModel.searchLocations(searchQuery)
                                            }
                                        }
                                    }
                                )
                            )
                            IconButton(
                                onClick = { 
                                    searchByPostalCode = !searchByPostalCode
                                    searchQuery = ""
                                },
                                modifier = Modifier.animateContentSize()
                            ) {
                                Icon(
                                    imageVector = if (searchByPostalCode) Icons.Default.Numbers else Icons.Default.Search,
                                    contentDescription = if (searchByPostalCode) "Search by name" else "Search by postal code"
                                )
                            }
                        }

                        // Swipeable Content
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectHorizontalDragGestures { change, dragAmount ->
                                        offsetX += dragAmount
                                        if (change.consume()) {
                                            when {
                                                offsetX > 100f -> {
                                                    currentTab = (currentTab - 1).coerceAtLeast(0)
                                                    offsetX = 0f
                                                }
                                                offsetX < -100f -> {
                                                    currentTab = (currentTab + 1).coerceAtMost(2)
                                                    offsetX = 0f
                                                }
                                            }
                                        }
                                    }
                                }
                        ) {
                            AnimatedContent(
                                targetState = currentTab,
                                transitionSpec = {
                                    if (targetState > initialState) {
                                        slideInHorizontally { width -> width } + fadeIn() with
                                                slideOutHorizontally { width -> -width } + fadeOut()
                                    } else {
                                        slideInHorizontally { width -> -width } + fadeIn() with
                                                slideOutHorizontally { width -> width } + fadeOut()
                                    }
                                }
                            ) { tab ->
                                when (tab) {
                                    0 -> {
                                        // Weather Forecast
                                        when (weatherState) {
                                            is WeatherState.Success -> {
                                                val forecasts = (weatherState as WeatherState.Success).forecasts
                                                LazyColumn {
                                                    items(forecasts) { forecast ->
                                                        WeatherCard(forecast = forecast)
                                                    }
                                                }
                                            }
                                            is WeatherState.Error -> {
                                                Text(
                                                    text = (weatherState as WeatherState.Error).message,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            }
                                            else -> {}
                                        }
                                    }
                                    1 -> {
                                        // Best Days to Ride
                                        if (recommendations.isNotEmpty()) {
                                            Column {
                                                Text(
                                                    text = "Best Days to Ride",
                                                    style = MaterialTheme.typography.titleLarge,
                                                    modifier = Modifier.padding(vertical = 16.dp)
                                                )
                                                LazyColumn {
                                                    items(recommendations.sortedByDescending { it.score }) { recommendation ->
                                                        RecommendationCard(recommendation)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    2 -> {
                                        // Map View
                                        MapScreen(
                                            selectedLocation = selectedLocation,
                                            onLocationSelected = { location ->
                                                viewModel.selectLocation(location)
                                                showMap = false
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(300.dp)
                                                .padding(vertical = 16.dp)
                                        )
                                    }
                                }
                            }

                            // Tab Indicators
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                repeat(3) { index ->
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (currentTab == index) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                            )
                                    )
                                }
                            }
                        }

                        // Location Suggestions
                        AnimatedVisibility(
                            visible = isSearching && locationSearchResults.isNotEmpty(),
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Column {
                                    locationSearchResults.forEach { location ->
                                        ListItem(
                                            headlineContent = { 
                                                AnimatedContent(
                                                    targetState = location.name,
                                                    transitionSpec = {
                                                        fadeIn() + slideInVertically() with fadeOut() + slideOutVertically()
                                                    }
                                                ) { target ->
                                                    Text(target)
                                                }
                                            },
                                            supportingContent = { 
                                                AnimatedContent(
                                                    targetState = "${location.state ?: ""} ${location.country}".trim(),
                                                    transitionSpec = {
                                                        fadeIn() + slideInVertically() with fadeOut() + slideOutVertically()
                                                    }
                                                ) { target ->
                                                    Text(target)
                                                }
                                            },
                                            modifier = Modifier
                                                .clickable {
                                                    viewModel.selectLocation(location)
                                                    searchQuery = location.displayName
                                                    isSearching = false
                                                }
                                                .animateContentSize()
                                        )
                                    }
                                }
                            }
                        }

                        // Favorites Section
                        if (favorites.isNotEmpty() && currentTab == 0) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Favorite Locations",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(favorites) { location ->
                                        Card(
                                            modifier = Modifier
                                                .width(150.dp)
                                                .clickable {
                                                    viewModel.selectLocation(location)
                                                }
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = location.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = location.country,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Theme Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Choose Theme") },
            text = {
                Column {
                    ThemeMode.values().forEach { mode ->
                        ListItem(
                            headlineContent = { Text(mode.name) },
                            leadingContent = {
                                Icon(
                                    imageVector = when (mode) {
                                        ThemeMode.LIGHT -> Icons.Default.LightMode
                                        ThemeMode.DARK -> Icons.Default.DarkMode
                                        ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                                    },
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier
                                .clickable {
                                    ThemeManager.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                                .animateContentSize()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun RecommendationCard(recommendation: BikeRideRecommendation) {
    val dateFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
    val score = recommendation.score
    val cardColor = bikeScoreToColor(score)
    val shape = RoundedCornerShape(16.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .shadow(4.dp, shape)
            .clip(shape),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = shape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = dateFormat.format(recommendation.date),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = "Score: $score%",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = "Temperature: ${recommendation.temperature.toInt()}°C",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray
            )
            Text(
                text = "Rain Chance: ${(recommendation.rainChance * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray
            )
            Text(
                text = "Wind Speed: ${recommendation.windSpeed} m/s",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray
            )
        }
    }
}

@Composable
fun WeatherCard(forecast: DailyForecast) {
    var offsetX by remember { mutableStateOf(0f) }
    var isExpanded by remember { mutableStateOf(false) }

    val date = Date(forecast.dt * 1000)
    val dateFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
    val score = forecast.score ?: 0
    val cardColor = bikeScoreToColor(score)
    val shape = RoundedCornerShape(24.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .offset(x = offsetX.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    offsetX += dragAmount
                    if (change.consume()) {
                        when {
                            offsetX > 100f -> {
                                isExpanded = !isExpanded
                                offsetX = 0f
                            }
                            offsetX < -100f -> {
                                isExpanded = !isExpanded
                                offsetX = 0f
                            }
                        }
                    }
                }
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            // Expanded content
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Additional weather details
                Text(
                    text = "Detailed Forecast",
                    style = MaterialTheme.typography.titleMedium
                )
                // Add more detailed information here
            }
        }

        // Original card content
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = dateFormat.format(date),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = forecast.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.DarkGray
                )
                Text(
                    text = "Humidity: ${forecast.humidity}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.DarkGray
                )
                Text(
                    text = "Wind: ${forecast.wind_speed} m/s",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.DarkGray
                )
                // Show bike ride score
                Text(
                    text = "Bike Ride Score: $score%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
            
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AsyncImage(
                    model = "https://openweathermap.org/img/wn/${forecast.weather.firstOrNull()?.icon}@2x.png",
                    contentDescription = "Weather icon",
                    modifier = Modifier.size(60.dp)
                )
                Text(
                    text = "${forecast.temp.day.toInt()}°C",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = "H:${forecast.temp.max.toInt()}° L:${forecast.temp.min.toInt()}°",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.DarkGray
                )
            }
        }
    }
}

@Composable
fun LoadingAnimation() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Loading weather data...",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun ErrorMessage(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = "Error",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("Retry")
        }
    }
}

// Interpolate color from red (0) to yellow (50) to green (100)
fun bikeScoreToColor(score: Int): Color {
    return when {
        score <= 50 -> {
            // Red to Yellow
            val ratio = score / 50f
            Color(
                red = 0xFF,
                green = (0x00 + (0xFF * ratio)).toInt(),
                blue = 0x00
            )
        }
        else -> {
            // Yellow to Green
            val ratio = (score - 50) / 50f
            Color(
                red = (0xFF - (0xFF * ratio)).toInt(),
                green = 0xFF,
                blue = 0x00
            )
        }
    }
} 