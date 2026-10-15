package io.github.scottlz0310.shoptools

import androidx.annotation.StringRes

/**
 * ボトムナビで到達できる画面と、その画面の初期表示で必ず出るテキスト。
 * 画面の追加・削除時はここだけを更新する。
 */
enum class ScreenCase(
    @StringRes val tabLabel: Int,
    @StringRes val contentText: Int,
) {
    UNIT_PRICE(R.string.tab_unit_price, R.string.empty_unit_price),
    STAMPS(R.string.tab_stamps, R.string.empty_stamps),
    SETTINGS(R.string.tab_settings, R.string.settings_font_size),
}
