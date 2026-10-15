package io.github.scottlz0310.shoptools

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick

/** メニューから各画面へ到達する方法。 */
sealed interface Entry {
    /** 起動直後に表示されている（メニュー自身）。 */
    data object None : Entry

    /** メニューの機能カード（タイトルのテキストを持つ）をタップする。 */
    data class Card(
        @StringRes val title: Int,
    ) : Entry

    /** TopAppBar のアクション（contentDescription を持つアイコン）をタップする。 */
    data class Action(
        @StringRes val contentDescription: Int,
    ) : Entry
}

/**
 * アプリの画面と、その画面の初期表示で必ず出るテキスト。
 * 画面の追加・削除時はここだけを更新する。
 */
enum class ScreenCase(
    val entry: Entry,
    @StringRes val contentText: Int,
) {
    MENU(Entry.None, R.string.menu_tagline),
    UNIT_PRICE(Entry.Card(R.string.screen_unit_price), R.string.empty_unit_price),
    STAMPS(Entry.Card(R.string.screen_stamps), R.string.empty_stamps),
    SETTINGS(Entry.Action(R.string.screen_settings), R.string.settings_font_size),
    ;

    /** 起動直後のメニューから、この画面を開く。 */
    fun open(
        rule: ComposeContentTestRule,
        context: Context,
    ) {
        when (val e = entry) {
            Entry.None -> Unit
            is Entry.Card ->
                rule
                    .onNode(hasText(context.getString(e.title)) and hasClickAction())
                    .performClick()
            is Entry.Action ->
                rule
                    .onNodeWithContentDescription(context.getString(e.contentDescription))
                    .performClick()
        }
        rule.waitForIdle()
    }

    companion object {
        /** メニューから開く子画面（戻る操作の対象）。 */
        val children: List<ScreenCase> = entries.filter { it != MENU }
    }
}
