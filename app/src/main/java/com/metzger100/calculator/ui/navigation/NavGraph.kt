package com.metzger100.calculator.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.material3.Text

import com.metzger100.calculator.features.calculator.ui.CalculatorScreen
import com.metzger100.calculator.features.calculator.viewmodel.CalculatorViewModel
import com.metzger100.calculator.features.currency.ui.CurrencyConverterScreen
import com.metzger100.calculator.features.currency.viewmodel.CurrencyViewModel

@Composable
fun NavGraph(navController: NavHostController, calculatorViewModel: CalculatorViewModel, currencyViewModel: CurrencyViewModel) {
    NavHost(navController, startDestination = NavItem.Calculator.route) {
        composable(NavItem.Calculator.route) {
            CalculatorScreen(viewModel = calculatorViewModel)
        }
        composable(NavItem.Currency.route) {
            CurrencyConverterScreen(viewModel = currencyViewModel)
        }
        composable(NavItem.Units.route) {
            Text("Unit Converter Screen")
        }
    }
}