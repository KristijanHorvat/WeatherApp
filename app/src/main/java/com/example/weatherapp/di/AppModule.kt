package com.example.weatherapp.di

import androidx.room.Room
import com.example.weatherapp.api.WeatherApi
import com.example.weatherapp.data.AppDatabase
import com.example.weatherapp.repository.WeatherRepository
import com.example.weatherapp.viewmodel.WeatherViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

val appModule = module {
    single {
        Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApi::class.java)
    }
    single {
        Room.databaseBuilder(get(), AppDatabase::class.java, "weather_db")
            .build()
    }
    single { get<AppDatabase>().lastCityDao() }
    single { WeatherRepository(get<WeatherApi>(), get()) }
    viewModel { WeatherViewModel(get(), get()) }
}