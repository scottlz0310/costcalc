package io.github.scottlz0310.shoptools.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import io.github.scottlz0310.shoptools.R
import io.github.scottlz0310.shoptools.design.ShopToolsTheme
import io.github.scottlz0310.shoptools.feature.menu.ToolFeature
import io.github.scottlz0310.shoptools.feature.menu.ui.MenuScreen
import io.github.scottlz0310.shoptools.feature.settings.SettingsViewModel
import io.github.scottlz0310.shoptools.feature.settings.ui.SettingsScreen
import io.github.scottlz0310.shoptools.feature.stamps.StampsViewModel
import io.github.scottlz0310.shoptools.feature.stamps.ui.StampsScreen
import io.github.scottlz0310.shoptools.feature.unitprice.UnitPriceViewModel
import io.github.scottlz0310.shoptools.feature.unitprice.ui.UnitPriceScreen
import io.github.scottlz0310.shoptools.feature.unitprice.ui.ocr.CameraOcrScreen
import io.github.scottlz0310.shoptools.feature.unitprice.ui.ocr.OcrViewModel

/** TopAppBar を持つ画面。OCR（全画面のカメラ）は含めない。 */
sealed class Screen(
    val route: String,
    @StringRes val titleRes: Int,
) {
    object Menu : Screen("menu", R.string.app_name)

    object UnitPrice : Screen("unit_price", R.string.screen_unit_price)

    object Stamps : Screen("stamps", R.string.screen_stamps)

    object Settings : Screen("settings", R.string.screen_settings)
}

private const val ROUTE_OCR = "unit_price_ocr/{rowId}"

private val screens = listOf(Screen.Menu, Screen.UnitPrice, Screen.Stamps, Screen.Settings)

/**
 * メニューに並ぶ計算機能。新しい機能は、ここへ 1 件追加し、NavHost に composable を追加する。
 * 設定は機能ではないため、メニュー右上のアイコンから開く。
 */
private val toolFeatures =
    listOf(
        ToolFeature(
            route = Screen.UnitPrice.route,
            titleRes = R.string.screen_unit_price,
            descriptionRes = R.string.menu_unit_price_description,
            graphicRes = R.drawable.menu_unit_price,
        ),
        ToolFeature(
            route = Screen.Stamps.route,
            titleRes = R.string.screen_stamps,
            descriptionRes = R.string.menu_stamps_description,
            graphicRes = R.drawable.menu_stamps,
        ),
    )

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
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
    // 現在の画面は最初のフレームでは null のため、開始画面（メニュー）として扱う。
    // こうしないと、起動直後だけ TopAppBar が無く、直後に内容が下へずれる。
    val currentRoute =
        navController
            .currentBackStackEntryAsState()
            .value
            ?.destination
            ?.route
            ?: Screen.Menu.route
    val currentScreen = screens.firstOrNull { it.route == currentRoute }
    // 単価比較の入力は保存されないため、メニューへ戻っても残るよう Activity にスコープする。
    // ボトムナビ時代にナビゲーショングラフが保持していた挙動を維持する。
    val activityOwner = checkNotNull(LocalViewModelStoreOwner.current)

    Scaffold(
        topBar = {
            if (currentScreen != null) {
                MainTopBar(
                    screen = currentScreen,
                    onBack = { navController.popBackStack() },
                    onOpenSettings = { navController.navigate(Screen.Settings.route) },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Menu.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Screen.Menu.route) {
                MenuScreen(
                    features = toolFeatures,
                    onOpenFeature = { route -> navController.navigate(route) },
                )
            }
            composable(Screen.UnitPrice.route) {
                val vm: UnitPriceViewModel = hiltViewModel(activityOwner)
                UnitPriceScreen(
                    viewModel = vm,
                    onOpenCamera = { rowId ->
                        navController.navigate("unit_price_ocr/$rowId")
                    },
                )
            }
            composable(ROUTE_OCR) { backStackEntry ->
                val rowId = backStackEntry.arguments?.getString("rowId") ?: return@composable
                val unitPriceVm: UnitPriceViewModel = hiltViewModel(activityOwner)
                val ocrVm: OcrViewModel = hiltViewModel()
                CameraOcrScreen(
                    rowId = rowId,
                    unitPriceViewModel = unitPriceVm,
                    ocrViewModel = ocrVm,
                    onDismiss = { navController.popBackStack() },
                )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTopBar(
    screen: Screen,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val isMenu = screen == Screen.Menu
    TopAppBar(
        title = { Text(stringResource(screen.titleRes)) },
        navigationIcon = {
            if (!isMenu) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.nav_back),
                    )
                }
            }
        },
        actions = {
            if (isMenu) {
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = stringResource(R.string.screen_settings),
                    )
                }
            }
        },
    )
}
