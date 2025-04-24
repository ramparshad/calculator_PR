package com.metzger100.calculator.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavItem(val route: String, val icon: ImageVector, val label: String) {
    object Calculator : NavItem("calculator", Icons.Filled.Calculate, "Calculator")
    object Currency : NavItem("currency", Icons.Filled.CurrencyExchange, "Currency")
    object Units : NavItem("units", Icons.Filled.Straighten, "Units")
}
