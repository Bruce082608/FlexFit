package com.example.flexfit.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.flexfit.data.model.BodyProportions
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.bodyCalibrationDataStore by preferencesDataStore(name = "body_calibration")

object BodyCalibrationRepository {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()
    private val proportionsKey = stringPreferencesKey("body_proportions")

    private var dataStore: DataStore<Preferences>? = null
    private var initialized = false

    private val _bodyProportions = MutableStateFlow<BodyProportions?>(null)
    val bodyProportions: StateFlow<BodyProportions?> = _bodyProportions.asStateFlow()

    val isCalibrated: Boolean
        get() = _bodyProportions.value?.isComplete == true

    fun initialize(context: Context) {
        if (initialized) return

        initialized = true
        val store = context.applicationContext.bodyCalibrationDataStore
        dataStore = store

        repositoryScope.launch {
            store.data
                .map { preferences -> preferences[proportionsKey]?.toBodyProportions() }
                .catch { emit(null) }
                .collect { proportions -> _bodyProportions.value = proportions?.takeIf { it.isComplete } }
        }
    }

    fun save(proportions: BodyProportions) {
        _bodyProportions.value = proportions

        repositoryScope.launch {
            dataStore?.edit { preferences ->
                preferences[proportionsKey] = gson.toJson(proportions)
            }
        }
    }

    fun clear() {
        _bodyProportions.value = null

        repositoryScope.launch {
            dataStore?.edit { preferences ->
                preferences.remove(proportionsKey)
            }
        }
    }

    private fun String.toBodyProportions(): BodyProportions? {
        return runCatching {
            gson.fromJson(this, BodyProportions::class.java)
        }.getOrNull()
    }
}
