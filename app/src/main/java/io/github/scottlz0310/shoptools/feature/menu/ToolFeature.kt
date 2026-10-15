package io.github.scottlz0310.shoptools.feature.menu

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector

/** メニューに並ぶ計算機能 1 件分の定義。 */
data class ToolFeature(
    val route: String,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val icon: ImageVector,
)
