package io.github.scottlz0310.shoptools.feature.menu

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/** メニューに並ぶ計算機能 1 件分の定義。 */
data class ToolFeature(
    val route: String,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    @DrawableRes val graphicRes: Int,
)
