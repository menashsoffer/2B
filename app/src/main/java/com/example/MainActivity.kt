package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.calculators.ai.AiSubscriptionsScreen
import com.example.ui.calculators.car.CarComparisonScreen
import com.example.ui.calculators.pension.PensionSavingsScreen
import com.example.ui.home.HomeScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(navController = navController)
                        }
                        composable("car_calculator") {
                            CarComparisonScreen(onBack = { navController.popBackStack() })
                        }
                        composable("ai_calculator") {
                            AiSubscriptionsScreen(onBack = { navController.popBackStack() })
                        }
                        composable("pension_calculator") {
                            PensionSavingsScreen(onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}
