package io.github.scottlz0310.shoptools

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.scottlz0310.shoptools.app.MainActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** 子画面から TopAppBar の戻るボタンでメニューへ戻れること。 */
@HiltAndroidTest
@RunWith(Parameterized::class)
class BackNavigationTest(
    private val screen: ScreenCase,
) {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() = hiltRule.inject()

    @Test
    fun backButtonReturnsToMenu() {
        val activity = composeRule.activity
        screen.open(composeRule, activity)

        composeRule.onNodeWithContentDescription(activity.getString(R.string.nav_back)).performClick()

        composeRule.onNodeWithText(activity.getString(ScreenCase.MENU.contentText)).assertIsDisplayed()
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun screens() = ScreenCase.children
    }
}
