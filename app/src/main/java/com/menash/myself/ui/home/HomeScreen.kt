package com.menash.myself.ui.home

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.menash.myself.ui.calculators.CalculatorsHubScreen

@Composable
fun HomeScreen(navController: NavController) {
    CalculatorsHubScreen(
        navController = navController,
        onBack = { navController.popBackStack() }
    )
}
