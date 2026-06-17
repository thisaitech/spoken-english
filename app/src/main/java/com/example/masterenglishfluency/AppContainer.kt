package com.example.masterenglishfluency

import android.app.Application
import com.example.masterenglishfluency.data.database.AppDatabase
import com.example.masterenglishfluency.data.repository.AppRepository
import com.example.masterenglishfluency.data.repository.SettingsRepository

class AppContainer(private val application: Application) {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(application) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(application) }
    val repository: AppRepository by lazy { AppRepository(database, settingsRepository) }

    init {
        repository.initializeSampleData()
    }
}
