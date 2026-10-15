package io.github.scottlz0310.shoptools

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
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

/** 3 画面がボトムナビから表示・遷移できること。 */
@HiltAndroidTest
@RunWith(Parameterized::class)
class MainScreensTest(
    private val screen: ScreenCase,
) {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() = hiltRule.inject()

    @Test
    fun navigatesToScreenFromBottomNav() {
        val activity = composeRule.activity
        composeRule
            .onNode(hasText(activity.getString(screen.tabLabel)) and hasClickAction())
            .performClick()

        composeRule.onNodeWithText(activity.getString(screen.contentText)).assertIsDisplayed()
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun screens() = ScreenCase.entries.toList()
    }
}
