package com.example.weatherapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.example.weatherapp.ui.theme.WeatherAppTheme
import com.example.weatherapp.viewmodel.WeatherViewModel
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WeatherAppTheme {
                WeatherAppScreen()
            }
        }
    }
}

@Composable
fun WeatherIcon(iconCode: String) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data("https://openweathermap.org/img/wn/${iconCode}@2x.png")
            .crossfade(true)
            .build(),
        contentDescription = "Weather Icon",
        modifier = Modifier.size(64.dp)
    )
}

@Composable
fun WeatherAppScreen(viewModel: WeatherViewModel = koinViewModel()) {
    val weather by viewModel.weather.collectAsState()
    var cityInput by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TextField(
                value = cityInput,
                onValueChange = { cityInput = it },
                label = { Text("Enter city") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { viewModel.fetchWeather(cityInput) },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Search")
            }
            weather?.let { weatherData ->
                Text(
                    text = "Temperature: ${weatherData.main.temp}°C",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Description: ${weatherData.weather[0].description}",
                    style = MaterialTheme.typography.bodyLarge
                )
                WeatherIcon(iconCode = weatherData.weather[0].icon)
            }
        }
    }
}