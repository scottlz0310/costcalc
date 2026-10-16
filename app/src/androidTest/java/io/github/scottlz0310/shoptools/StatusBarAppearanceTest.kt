package io.github.scottlz0310.shoptools

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.core.view.WindowCompat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.scottlz0310.shoptools.app.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * ステータスバーのアイコンが濃い色であること。
 *
 * 画面はライト配色のみで背景が明るい。アイコンが白いと読めない（#107）。
 * 端末のダークモードに関わらず、ライト配色用（濃いアイコン）でなければならない。
 */
@HiltAndroidTest
@RunWith(Parameterized::class)
class StatusBarAppearanceTest(
    private val screen: ScreenCase,
) {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() = hiltRule.inject()

    @Test
    fun statusBarIconsAreDark() {
        screen.open(composeRule, composeRule.activity)

        var isLight = false
        composeRule.runOnUiThread {
            val window = composeRule.activity.window
            isLight = WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars
        }

        assertTrue("ステータスバーのアイコンが白い（ライト背景で読めない）", isLight)
    }

    @Test
    fun navigationBarIconsAreDark() {
        screen.open(composeRule, composeRule.activity)

        var isLight = false
        composeRule.runOnUiThread {
            val window = composeRule.activity.window
            isLight = WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars
        }

        assertTrue("ナビゲーションバーのアイコンが白い（ライト背景で読めない）", isLight)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun screens() = ScreenCase.entries.toList()
    }
}
