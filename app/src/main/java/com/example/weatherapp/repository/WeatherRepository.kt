package com.example.weatherapp.repository

import com.example.weatherapp.BuildConfig
import com.example.weatherapp.api.WeatherApi
import com.example.weatherapp.data.*

class WeatherRepository(
    private val api: WeatherApi,
    private val dao: LastCityDao
) {
    val apiKey = BuildConfig.WEATHER_API_KEY
    suspend fun getWeather(city: String): WeatherResponse {
        return try {
            val response = api.getWeather(city, apiKey)
            dao.insertCurrentWeather(
                CurrentWeatherEntity(
                    cityName = city,
                    temp = response.main.temp,
                    humidity = response.main.humidity,
                    description = response.weather[0].description,
                    icon = response.weather[0].icon,
                    windSpeed = response.wind.speed
                )
            )
            dao.insertCity(LastCityEntity(cityName = city))
            response
        } catch (e: Exception) {
            val cachedWeather = dao.getCurrentWeather(city)
            if (cachedWeather != null) {
                WeatherResponse(
                    main = Main(cachedWeather.temp, cachedWeather.humidity),
                    weather = listOf(Weather(cachedWeather.description, cachedWeather.icon)),
                    wind = Wind(cachedWeather.windSpeed),
                    name = cachedWeather.cityName
                )
            } else {
                throw e
            }
        }
    }

    suspend fun getForecast(city: String): ForecastResponse {
        return try {
            val response = api.getForecast(city, apiKey)
            val forecastEntities = response.list.map {
                ForecastEntity(
                    cityName = city,
                    dt = it.dt,
                    temp = it.main.temp,
                    description = it.weather[0].description,
                    icon = it.weather[0].icon,
                    windSpeed = it.wind.speed
                )
            }
            dao.deleteForecast(city)
            dao.insertForecast(forecastEntities)
            response
        } catch (e: Exception) {
            val cachedForecast = dao.getForecast(city)
            if (cachedForecast.isNotEmpty()) {
                ForecastResponse(
                    list = cachedForecast.map {
                        ForecastItem(
                            dt = it.dt,
                            main = Main(it.temp, 0),
                            weather = listOf(Weather(it.description, it.icon)),
                            wind = Wind(it.windSpeed)
                        )
                    }
                )
            } else {
                throw e
            }
        }
    }

    suspend fun getLastCity(): String? {
        return dao.getLastCity()?.cityName
    }
}