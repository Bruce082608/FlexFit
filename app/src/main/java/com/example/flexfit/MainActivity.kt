package com.example.flexfit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.flexfit.data.repository.UserProfileRepository
import com.example.flexfit.data.repository.WorkoutRecordRepository
import com.example.flexfit.ui.navigation.FlexFitNavigation
import com.example.flexfit.ui.theme.FlexFitTheme
import com.example.flexfit.ui.theme.LightBackground

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WorkoutRecordRepository.initialize(applicationContext)
        UserProfileRepository.initialize(applicationContext)
        enableEdgeToEdge()
        setContent {
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
