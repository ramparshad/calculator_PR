package com.metzger100.calculator.ui.navigation

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomNavBar(navController: NavController) {
    val items = listOf(NavItem.Calculator, NavItem.Currency, NavItem.Units)
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route

    NavigationBar {
        items.forEach { item ->
            val labelText = stringResource(id = item.labelRes)

            // Wenn die Route "units" enthält, markiere den Tab als selektiert
            val isSelected = when {
                currentRoute == item.route -> true
                item == NavItem.Units && currentRoute?.startsWith("unit/") == true -> true
                else -> false
            }

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(item.icon, contentDescription = labelText) },
                label = { Text(labelText) }
            )
        }
    }
}