package com.example.weatherapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherapp.BuildConfig
import com.example.weatherapp.api.WeatherApi
import com.example.weatherapp.repository.WeatherRepository
import com.example.weatherapp.data.ForecastResponse
import com.example.weatherapp.data.WeatherResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class WeatherState {
    object Loading : WeatherState()
    data class Success(
        val weather: WeatherResponse,
        val forecast: ForecastResponse? = null,
        val isOffline: Boolean = false
    ) : WeatherState()
    data class Error(val message: String) : WeatherState()
}

class WeatherViewModel(
    val repository: WeatherRepository,
    private val api: WeatherApi
) : ViewModel() {
    private val _weatherState = MutableStateFlow<WeatherState>(WeatherState.Loading)
    val weatherState: StateFlow<WeatherState> = _weatherState
    val apiKey = BuildConfig.WEATHER_API_KEY
    init {
        viewModelScope.launch {
            val lastCity = repository.getLastCity()
            if (lastCity != null) {
                fetchWeather(lastCity)
            } else {
                _weatherState.value = WeatherState.Error("No previous city found")
            }
        }
    }

    fun fetchWeather(city: String) {
        viewModelScope.launch {
            _weatherState.value = WeatherState.Loading
            val weatherResponse = repository.getWeather(city)
            val forecastResponse = repository.getForecast(city)

            if (weatherResponse != null) {
                val isOffline = try {
                    api.getWeather(city, apiKey)
                    false
                } catch (e: Exception) {
                    true
                }
                _weatherState.value = WeatherState.Success(weatherResponse, forecastResponse, isOffline)
            } else {
                val isOffline = try {
                    api.getWeather(city, apiKey)
                    false
                } catch (e: Exception) {
                    true
                }
                if (isOffline) {
                    _weatherState.value = WeatherState.Error("No internet connection and no cached data for $city")
                } else {
                    _weatherState.value = WeatherState.Error("Failed to fetch data for $city")
                }
            }
        }
    }
}