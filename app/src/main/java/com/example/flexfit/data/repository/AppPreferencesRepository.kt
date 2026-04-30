package com.example.flexfit.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.flexfit.ui.i18n.AppLanguage
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

private val Context.appPreferencesDataStore by preferencesDataStore(name = "app_preferences")

object AppPreferencesRepository {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val languageKey = stringPreferencesKey("language")

    private var dataStore: DataStore<Preferences>? = null
    private var initialized = false

    private val _language = MutableStateFlow(AppLanguage.ENGLISH)
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    fun initialize(context: Context) {
        if (initialized) return

        initialized = true
        val store = context.applicationContext.appPreferencesDataStore
        dataStore = store

        repositoryScope.launch {
            store.data
                .map { preferences ->
                    AppLanguage.fromCode(preferences[languageKey])
                }
                .catch { emit(AppLanguage.ENGLISH) }
                .collect { appLanguage -> _language.value = appLanguage }
        }
    }

    fun updateLanguage(language: AppLanguage) {
        _language.value = language

        repositoryScope.launch {
            dataStore?.edit { preferences ->
                preferences[languageKey] = language.code
            }
        }
    }
}
