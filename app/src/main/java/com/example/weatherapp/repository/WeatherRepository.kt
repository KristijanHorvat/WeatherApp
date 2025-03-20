package com.example.weatherapp.repository

import com.example.weatherapp.api.WeatherApi
import com.example.weatherapp.data.WeatherResponse

class WeatherRepository(private val api: WeatherApi) {
    suspend fun getWeather(city: String): WeatherResponse {
        return api.getWeather(city, "64c8aba3ef26103a8fe305cc764bbf4b")
    }
}