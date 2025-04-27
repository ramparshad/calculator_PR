package com.metzger100.calculator

import TopAppBar
import android.app.Application
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.metzger100.calculator.ui.navigation.BottomNavBar
import com.metzger100.calculator.ui.navigation.NavGraph
import dagger.hilt.android.HiltAndroidApp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import com.metzger100.calculator.features.calculator.viewmodel.CalculatorViewModel
import com.metzger100.calculator.features.currency.viewmodel.CurrencyViewModel
import com.metzger100.calculator.ui.theme.CalculatorTheme
import dagger.hilt.android.AndroidEntryPoint

@HiltAndroidApp
class MainApplication : Application()

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setHighRefreshRate()
        setContent {
            AppContent()
        }
    }
}

fun MainActivity.setHighRefreshRate() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val display = display ?: return
        val highestMode = display.supportedModes
            .maxByOrNull { it.refreshRate }

        highestMode?.let {
            window.attributes = window.attributes.apply {
                preferredDisplayModeId = it.modeId
            }
        }
    }
}


@Composable
fun AppContent() {
    CalculatorTheme {
        val navController = rememberNavController()
        val CalcViewModel: CalculatorViewModel = hiltViewModel()
        val CurViewModel: CurrencyViewModel = hiltViewModel()

        val navBackStack by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStack?.destination?.route

        val showBack = currentRoute?.startsWith("unit/") == true

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    showBackButton = showBack,
                    onBackClick    = { navController.navigateUp() },
                    onClearHistory = CalcViewModel::clearHistory,
                    onRefreshRates = { CurViewModel.forceRefreshData(CurViewModel.base.value) }
                )
            },
            bottomBar = {
                BottomNavBar(navController = navController)
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                NavGraph(
                    navController = navController,
                    calculatorViewModel = CalcViewModel,
                    currencyViewModel = CurViewModel
                )
            }
        }
    }
}