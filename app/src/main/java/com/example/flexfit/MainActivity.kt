package com.example.flexfit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.flexfit.data.repository.AppPreferencesRepository
import com.example.flexfit.data.repository.BodyCalibrationRepository
import com.example.flexfit.data.repository.UserProfileRepository
import com.example.flexfit.data.repository.WorkoutRecordRepository
import com.example.flexfit.ui.i18n.LocalAppLanguage
import com.example.flexfit.ui.navigation.FlexFitNavigation
import com.example.flexfit.ui.theme.FlexFitTheme
import com.example.flexfit.ui.theme.LightBackground

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppPreferencesRepository.initialize(applicationContext)
        BodyCalibrationRepository.initialize(applicationContext)
        WorkoutRecordRepository.initialize(applicationContext)
        UserProfileRepository.initialize(applicationContext)
        enableEdgeToEdge()
        setContent {
            val appLanguage by AppPreferencesRepository.language.collectAsState()

            CompositionLocalProvider(LocalAppLanguage provides appLanguage) {
                FlexFitTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = LightBackground
                    ) {
                        FlexFitNavigation()
                    }
                }
            }
        }
    }
}
