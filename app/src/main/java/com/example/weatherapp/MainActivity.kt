package com.example.weatherapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WindPower
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.weatherapp.data.ForecastItem
import com.example.weatherapp.data.ForecastResponse
import com.example.weatherapp.data.WeatherResponse
import com.example.weatherapp.ui.theme.WeatherAppTheme
import com.example.weatherapp.viewmodel.WeatherState
import com.example.weatherapp.viewmodel.WeatherViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val darkTheme = remember { mutableStateOf(false) }
            WeatherAppTheme(darkTheme = darkTheme.value) {
                WeatherAppScreen(onThemeChanged = { darkTheme.value = it })
            }
        }
    }
}
@Composable
fun WeatherAppScreen(
    viewModel: WeatherViewModel = koinViewModel(),
    onThemeChanged: (Boolean) -> Unit
) {
    val weatherState by viewModel.weatherState.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var cityInput by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val lastCity = viewModel.repository.getLastCity()
        if (lastCity != null) {
            cityInput = lastCity
            viewModel.fetchWeather(lastCity)
        }
    }

    var isDarkMode by remember { mutableStateOf(false) }
    val popularCities = remember { listOf("London", "New York", "Tokyo", "Paris", "Sydney", "Berlin") }

    val backgroundBrush = when {
        isDarkMode -> Brush.verticalGradient(colors = listOf(Color(0xFF1F2933), Color(0xFF0F1620)))
        weatherState is WeatherState.Success && (weatherState as WeatherState.Success).weather.weather.firstOrNull()?.description?.contains("clear") == true ->
            Brush.verticalGradient(colors = listOf(Color(0xFF4A90E2), Color(0xFF87CEFA)))
        weatherState is WeatherState.Success && (weatherState as WeatherState.Success).weather.weather.firstOrNull()?.description?.contains("rain") == true ->
            Brush.verticalGradient(colors = listOf(Color(0xFF54717A), Color(0xFF373B44)))
        weatherState is WeatherState.Success && (weatherState as WeatherState.Success).weather.weather.firstOrNull()?.description?.contains("cloud") == true ->
            Brush.verticalGradient(colors = listOf(Color(0xFF8693AB), Color(0xFFBDC3C7)))
        else -> Brush.verticalGradient(colors = listOf(Color(0xFF4A90E2), Color(0xFF87CEFA)))
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Weather App",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                    IconButton(
                        onClick = {
                            isDarkMode = !isDarkMode
                            onThemeChanged(isDarkMode)
                        }
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Filled.Brightness7 else Icons.Filled.Brightness4,
                            contentDescription = "Toggle theme",
                            tint = Color.White
                        )
                    }
                }

                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            TextField(
                                value = cityInput,
                                onValueChange = { cityInput = it },
                                label = { Text("Enter city") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                ),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search"
                                    )
                                },
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    if (cityInput.isNotEmpty()) {
                                        viewModel.fetchWeather(cityInput)
                                    }
                                },
                                modifier = Modifier.align(Alignment.End),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Search")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Popular cities:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                items(popularCities) { city ->
                                    SuggestionChip(
                                        onClick = {
                                            cityInput = city
                                            viewModel.fetchWeather(city)
                                        },
                                        label = { Text(city) }
                                    )
                                }
                            }
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (weatherState) {
                        is WeatherState.Loading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center),
                                color = Color.White
                            )
                        }
                        is WeatherState.Success -> {
                            val successState = weatherState as WeatherState.Success
                            Column {
                                WeatherContent(weather = successState.weather, isOffline = successState.isOffline)
                                successState.forecast?.let { forecast ->
                                    ForecastContent(forecast = forecast)
                                }
                            }
                            if (successState.isOffline) {
                                LaunchedEffect(Unit) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "Offline mode: Showing cached data",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                            }
                        }
                        is WeatherState.Error -> {
                            LaunchedEffect(weatherState) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = (weatherState as WeatherState.Error).message,
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                        }
                    }
                }
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
fun WeatherContent(weather: WeatherResponse, isOffline: Boolean) {
    val animatedAlpha = remember { Animatable(0f) }
    val animatedOffset = remember { Animatable(-50f) }

    LaunchedEffect(weather) {
        launch {
            animatedAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500)
            )
        }
        launch {
            animatedOffset.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .alpha(animatedAlpha.value)
            .offset(y = animatedOffset.value.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CurrentWeatherCard(weather)
        if (isOffline) {
            Text(
                text = "Offline Mode",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
@Composable
fun ForecastContent(forecast: ForecastResponse) {
    val dailyForecast = forecast.list
        .groupBy { item ->
            java.text.SimpleDateFormat("yyyy-MM-dd").format(java.util.Date(item.dt * 1000))
        }
        .map { it.value.first() } // Uzmi prvi zapis za svaki dan
        .take(5) // Ograniči na 5 dana

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(dailyForecast) { forecastItem ->
            ForecastCard(forecastItem)
        }
    }
}

@Composable
fun ForecastCard(forecast: ForecastItem) {
    val animatedScale = remember { Animatable(0.8f) }

    LaunchedEffect(forecast) {
        animatedScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    Card(
        modifier = Modifier
            .width(120.dp)
            .scale(animatedScale.value),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = java.text.SimpleDateFormat("EEE").format(java.util.Date(forecast.dt * 1000)),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data("https://openweathermap.org/img/wn/${forecast.weather[0].icon}@2x.png")
                    .crossfade(true)
                    .build(),
                contentDescription = "Forecast Icon",
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "${forecast.main.temp.toInt()}°C",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )
        }
    }
}
@Composable
fun WeatherContent(weather: WeatherResponse) {
    val animatedAlpha = remember { Animatable(0f) }
    val animatedOffset = remember { Animatable(-50f) }

    LaunchedEffect(weather) {
        launch {
            animatedAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500)
            )
        }
        launch {
            animatedOffset.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .alpha(animatedAlpha.value)
            .offset(y = animatedOffset.value.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CurrentWeatherCard(weather)
    }
}

@Composable
fun CurrentWeatherCard(weather: WeatherResponse) {
    val animatedScale = remember { Animatable(0.8f) }

    LaunchedEffect(weather) {
        animatedScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .scale(animatedScale.value),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = weather.name,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = java.text.SimpleDateFormat("EEEE, dd MMM", java.util.Locale.getDefault()).format(
                    java.util.Date()
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data("https://openweathermap.org/img/wn/${weather.weather[0].icon}@2x.png")
                        .crossfade(true)
                        .build(),
                    contentDescription = "Weather Icon",
                    modifier = Modifier.size(100.dp)
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${weather.main.temp.toInt()}°C",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = weather.weather[0].description.replaceFirstChar { it.titlecase() },
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                WeatherDetailItem(
                    icon = Icons.Default.Thermostat,
                    title = "Humidity",
                    value = "${weather.main.humidity}%"
                )
                WeatherDetailItem(
                    icon = Icons.Default.WindPower,
                    title = "Wind",
                    value = "${weather.wind.speed} m/s"
                )
            }
        }
    }
}

@Composable
fun WeatherDetailItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = Color.White
        )
    }
}