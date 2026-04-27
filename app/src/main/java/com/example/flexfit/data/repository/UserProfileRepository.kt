package com.example.flexfit.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.flexfit.data.model.UserProfile
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

private val Context.userProfileDataStore by preferencesDataStore(name = "user_profile")

object UserProfileRepository {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val nameKey = stringPreferencesKey("name")
    private val emailKey = stringPreferencesKey("email")
    private val avatarStyleKey = intPreferencesKey("avatar_style")
    private val avatarUriKey = stringPreferencesKey("avatar_uri")
    private val heightKey = floatPreferencesKey("height_cm")
    private val weightKey = floatPreferencesKey("weight_kg")
    private val fitnessGoalKey = stringPreferencesKey("fitness_goal")

    private var dataStore: DataStore<Preferences>? = null
    private var initialized = false

    private val _profile = MutableStateFlow(UserProfile())
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    fun initialize(context: Context) {
        if (initialized) return

        initialized = true
        val store = context.applicationContext.userProfileDataStore
        dataStore = store

        repositoryScope.launch {
            store.data
                .map { preferences -> preferences.toUserProfile() }
                .catch { emit(UserProfile()) }
                .collect { userProfile -> _profile.value = userProfile }
        }
    }

    fun updateBodyStats(heightCm: Float, weightKg: Float) {
        val updated = _profile.value.copy(
            height = heightCm.coerceIn(80f, 250f),
            weight = weightKg.coerceIn(25f, 250f)
        )
        saveProfile(updated)
    }

    fun updateProfile(name: String, email: String, avatarStyle: Int, avatarUri: String?) {
        val updated = _profile.value.copy(
            name = name.trim().ifBlank { UserProfile().name },
            email = email.trim().ifBlank { UserProfile().email },
            avatarStyle = avatarStyle.coerceIn(0, AVATAR_STYLE_COUNT - 1),
            avatarUri = avatarUri?.takeIf { it.isNotBlank() }
        )
        saveProfile(updated)
    }

    fun updateFitnessGoal(goal: String) {
        saveProfile(_profile.value.copy(fitnessGoal = goal))
    }

    private fun saveProfile(profile: UserProfile) {
        _profile.value = profile

        repositoryScope.launch {
            dataStore?.edit { preferences ->
                preferences[nameKey] = profile.name
                preferences[emailKey] = profile.email
                preferences[avatarStyleKey] = profile.avatarStyle
                profile.avatarUri?.let { preferences[avatarUriKey] = it }
                    ?: preferences.remove(avatarUriKey)
                preferences[heightKey] = profile.height
                preferences[weightKey] = profile.weight
                preferences[fitnessGoalKey] = profile.fitnessGoal
            }
        }
    }

    private fun Preferences.toUserProfile(): UserProfile {
        val defaults = UserProfile()
        return UserProfile(
            name = this[nameKey] ?: defaults.name,
            email = this[emailKey] ?: defaults.email,
            avatarStyle = this[avatarStyleKey] ?: defaults.avatarStyle,
            avatarUri = this[avatarUriKey],
            height = this[heightKey] ?: defaults.height,
            weight = this[weightKey] ?: defaults.weight,
            fitnessGoal = this[fitnessGoalKey] ?: defaults.fitnessGoal
        )
    }

    const val AVATAR_STYLE_COUNT = 4
}
