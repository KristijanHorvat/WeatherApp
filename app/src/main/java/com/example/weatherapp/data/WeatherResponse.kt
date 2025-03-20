package com.example.weatherapp.data
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson

data class WeatherResponse(
    val main: Main,
    val weather: List<Weather>,
    val wind: Wind,
    val name: String
)

data class Main(
    val temp: Float,
    val humidity: Int
)

data class Weather(
    val description: String,
    val icon: String
)

data class Wind(
    val speed: Float
)

@Entity(tableName = "last_city")
data class LastCityEntity(
    @PrimaryKey val id: Int = 1,
    val cityName: String
)

@Entity(tableName = "current_weather")
data class CurrentWeatherEntity(
    @PrimaryKey val cityName: String,
    val temp: Float,
    val humidity: Int,
    val description: String,
    val icon: String,
    val windSpeed: Float
)

@Entity(tableName = "forecast")
data class ForecastEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cityName: String,
    val dt: Long,
    val temp: Float,
    val description: String,
    val icon: String,
    val windSpeed: Float
)

object Converters {
    @TypeConverter
    fun fromWeatherList(value: List<Weather>?): String = Gson().toJson(value)
    @TypeConverter
    fun toWeatherList(value: String): List<Weather> = Gson().fromJson(value, Array<Weather>::class.java).toList()
}