package com.example.shoptools.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.shoptools.R
import com.example.shoptools.design.ShopToolsTheme
import com.example.shoptools.feature.settings.SettingsViewModel
import com.example.shoptools.feature.settings.ui.SettingsScreen
import com.example.shoptools.feature.stamps.StampsViewModel
import com.example.shoptools.feature.stamps.ui.StampsScreen
import com.example.shoptools.feature.unitprice.UnitPriceViewModel
import com.example.shoptools.feature.unitprice.ui.UnitPriceScreen
import dagger.hilt.android.AndroidEntryPoint

sealed class Screen(val route: String, val labelRes: Int, val icon: ImageVector) {
    object UnitPrice : Screen("unit_price", R.string.tab_unit_price, Icons.Filled.ShoppingCart)
    object Stamps : Screen("stamps", R.string.tab_stamps, Icons.Filled.Email)
    object Settings : Screen("settings", R.string.tab_settings, Icons.Filled.Settings)
}

private val bottomNavItems = listOf(Screen.UnitPrice, Screen.Stamps, Screen.Settings)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settingsVm: SettingsViewModel = hiltViewModel()
            val settingsState by settingsVm.uiState.collectAsState()
            ShopToolsTheme(fontSizePreset = settingsState.fontSizePreset) {
                MainScaffold()
            }
        }
    }
}

@Composable
private fun MainScaffold() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = stringResource(screen.labelRes),
                            )
                        },
                        label = { Text(stringResource(screen.labelRes)) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.UnitPrice.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.UnitPrice.route) {
                val vm: UnitPriceViewModel = hiltViewModel()
                UnitPriceScreen(viewModel = vm)
            }
            composable(Screen.Stamps.route) {
                val vm: StampsViewModel = hiltViewModel()
                StampsScreen(viewModel = vm)
            }
            composable(Screen.Settings.route) {
                val vm: SettingsViewModel = hiltViewModel()
                SettingsScreen(viewModel = vm)
            }
        }
    }
}
