package com.example.masterenglishfluency

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.masterenglishfluency.data.UserProgressRepository
import com.example.masterenglishfluency.ui.theme.MasterEnglishFluencyTheme
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val firebaseApp = FirebaseApp.initializeApp(this)
        if (firebaseApp != null) {
            Log.d("FirebaseInit", "Firebase App initialized successfully: ${firebaseApp.name}")
        } else {
            Log.e("FirebaseInit", "Firebase App initialization failed")
        }

        UserProgressRepository.initialize(applicationContext)

        enableEdgeToEdge()

        setContent {
            MasterEnglishFluencyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigation()
                }
            }
        }
    }
}
