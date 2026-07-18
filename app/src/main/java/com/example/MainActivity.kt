package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.data.FamilyDatabase
import com.example.ui.screens.DashboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.FamilyViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: FamilyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Safely initialize and validate Room Database on startup to prevent any migration or schema crashes
        try {
            FamilyDatabase.getDatabase(this)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error validating/initializing Room database on startup: ${e.localizedMessage}")
        }

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    DashboardScreen(viewModel = viewModel)
                }
            }
        }
    }
}
