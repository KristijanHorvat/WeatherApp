package com.example.weatherapp

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.text.style.TextAlign
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
    val scrollState = rememberScrollState()

    var cityInput by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }

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
                    .verticalScroll(scrollState)
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

                ExpandableSearchCard(
                    cityInput = cityInput,
                    onCityInputChange = { cityInput = it },
                    onSearchClick = {
                        if (cityInput.isNotEmpty()) {
                            viewModel.fetchWeather(cityInput)
                            isSearchExpanded = false
                        }
                    },
                    popularCities = popularCities,
                    onPopularCityClick = { city ->
                        cityInput = city
                        viewModel.fetchWeather(city)
                        isSearchExpanded = false
                    },
                    isExpanded = isSearchExpanded,
                    onToggleExpand = { isSearchExpanded = !isSearchExpanded }
                )

                when (weatherState) {
                    is WeatherState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            color = Color.White
                        )
                    }
                    is WeatherState.Success -> {
                        val successState = weatherState as WeatherState.Success
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            WeatherContent(weather = successState.weather, isOffline = successState.isOffline)
                            successState.forecast?.let { forecast ->
                                ForecastContent(forecast = forecast)
                                TemperatureGraph(forecast = forecast)
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

                Spacer(modifier = Modifier.height(80.dp))
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
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
        WeatherDescriptionCard(weather)
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
@SuppressLint("SimpleDateFormat")
@Composable
fun ForecastContent(forecast: ForecastResponse) {
    val dailyForecast = forecast.list
        .groupBy { item ->
            java.text.SimpleDateFormat("yyyy-MM-dd").format(java.util.Date(item.dt * 1000))
        }
        .map { it.value.first() }
        .take(5)

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 8.dp),
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
fun getWeatherDescriptionDetail(weather: WeatherResponse): String {
    return when {
        weather.main.humidity > 80 -> "high humidity with potential for precipitation"
        weather.wind.speed > 5 -> "windy conditions with moderate air movement"
        weather.main.temp > 30 -> "hot weather that may require hydration and sun protection"
        weather.main.temp < 10 -> "cold temperatures, recommend warm clothing"
        else -> "generally mild and comfortable weather conditions"
    }
}
@Composable
fun WeatherDescriptionCard(weather: WeatherResponse) {
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
                text = "Weather Description",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Today's weather is ${weather.weather[0].description}. " +
                        "The temperature feels like ${weather.main.temp.toInt()}°C. " +
                        "Current atmospheric conditions suggest ${getWeatherDescriptionDetail(weather)}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}
@Composable
fun TemperatureGraph(forecast: ForecastResponse) {
    val dailyForecast = forecast.list
        .groupBy { item ->
            java.text.SimpleDateFormat("yyyy-MM-dd").format(java.util.Date(item.dt * 1000))
        }
        .map { it.value.first() }
        .take(5)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "Temperature Next 5 Days",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                dailyForecast.forEach { forecastItem ->
                    val temp = forecastItem.main.temp.toInt()
                    val maxTemp = dailyForecast.maxOf { it.main.temp }.toInt()
                    val minTemp = dailyForecast.minOf { it.main.temp }.toInt()
                    val heightFactor = if (maxTemp == minTemp) 1f else
                        (temp - minTemp).toFloat() / (maxTemp - minTemp).toFloat()

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(30.dp)
                                .height((100.dp * heightFactor).coerceAtLeast(20.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0xFFFF6B6B), Color(0xFFFECA57))
                                    ),
                                    shape = RoundedCornerShape(4.dp)
                                )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${temp}°C",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                        Text(
                            text = java.text.SimpleDateFormat("EEE").format(java.util.Date(forecastItem.dt * 1000)),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun ExpandableSearchCard(
    cityInput: String,
    onCityInputChange: (String) -> Unit,
    onSearchClick: () -> Unit,
    popularCities: List<String>,
    onPopularCityClick: (String) -> Unit,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Search City",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand/Collapse",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    TextField(
                        value = cityInput,
                        onValueChange = onCityInputChange,
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
                        onClick = onSearchClick,
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
                                onClick = { onPopularCityClick(city) },
                                label = { Text(city) }
                            )
                        }
                    }
                }
            }
        }
    }
}
