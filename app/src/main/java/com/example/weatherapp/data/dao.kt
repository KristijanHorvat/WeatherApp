package com.example.weatherapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LastCityDao {
    @Query("SELECT * FROM last_city WHERE id = 1")
    suspend fun getLastCity(): LastCityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCity(city: LastCityEntity)

    @Query("SELECT * FROM current_weather WHERE cityName = :cityName")
    suspend fun getCurrentWeather(cityName: String): CurrentWeatherEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurrentWeather(weather: CurrentWeatherEntity)

    @Query("SELECT * FROM forecast WHERE cityName = :cityName")
    suspend fun getForecast(cityName: String): List<ForecastEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertForecast(forecast: List<ForecastEntity>)

    @Query("DELETE FROM forecast WHERE cityName = :cityName")
    suspend fun deleteForecast(cityName: String)
}