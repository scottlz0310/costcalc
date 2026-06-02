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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.example.shoptools.R
import com.example.shoptools.design.ShopToolsTheme
import com.example.shoptools.feature.settings.SettingsViewModel
import com.example.shoptools.feature.settings.ui.SettingsScreen
import com.example.shoptools.feature.stamps.StampsViewModel
import com.example.shoptools.feature.stamps.ui.StampsScreen
import com.example.shoptools.feature.unitprice.UnitPriceViewModel
import com.example.shoptools.feature.unitprice.ui.UnitPriceScreen
import com.example.shoptools.feature.unitprice.ui.ocr.CameraOcrScreen
import com.example.shoptools.feature.unitprice.ui.ocr.OcrViewModel
import dagger.hilt.android.AndroidEntryPoint

sealed class Screen(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
) {
    object UnitPrice : Screen("unit_price", R.string.tab_unit_price, Icons.Filled.ShoppingCart)

    object Stamps : Screen("stamps", R.string.tab_stamps, Icons.Filled.Email)

    object Settings : Screen("settings", R.string.tab_settings, Icons.Filled.Settings)
}

private const val UNIT_PRICE_GRAPH = "unit_price_graph"
private const val ROUTE_OCR = "unit_price_ocr/{rowId}"

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
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute != ROUTE_OCR

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
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
                            selected =
                                navBackStackEntry
                                    ?.destination
                                    ?.hierarchy
                                    ?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = UNIT_PRICE_GRAPH,
            modifier = Modifier.padding(innerPadding),
        ) {
            navigation(startDestination = Screen.UnitPrice.route, route = UNIT_PRICE_GRAPH) {
                composable(Screen.UnitPrice.route) { backStackEntry ->
                    val parentEntry =
                        remember(backStackEntry) {
                            navController.getBackStackEntry(UNIT_PRICE_GRAPH)
                        }
                    val vm: UnitPriceViewModel = hiltViewModel(parentEntry)
                    UnitPriceScreen(
                        viewModel = vm,
                        onOpenCamera = { rowId ->
                            navController.navigate("unit_price_ocr/$rowId")
                        },
                    )
                }
                composable(ROUTE_OCR) { backStackEntry ->
                    val rowId = backStackEntry.arguments?.getString("rowId") ?: return@composable
                    val parentEntry =
                        remember(backStackEntry) {
                            navController.getBackStackEntry(UNIT_PRICE_GRAPH)
                        }
                    val unitPriceVm: UnitPriceViewModel = hiltViewModel(parentEntry)
                    val ocrVm: OcrViewModel = hiltViewModel()
                    CameraOcrScreen(
                        rowId = rowId,
                        unitPriceViewModel = unitPriceVm,
                        ocrViewModel = ocrVm,
                        onDismiss = { navController.popBackStack() },
                    )
                }
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
