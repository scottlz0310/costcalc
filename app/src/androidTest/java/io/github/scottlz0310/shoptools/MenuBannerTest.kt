package io.github.scottlz0310.shoptools

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.scottlz0310.shoptools.app.MainActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** メニューのヘッダーに、バナー画像が表示されること。 */
@HiltAndroidTest
class MenuBannerTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() = hiltRule.inject()

    @Test
    fun showsBanner() {
        val activity = composeRule.activity

        composeRule
            .onNodeWithContentDescription(activity.getString(R.string.menu_banner_description))
            .assertIsDisplayed()
    }
}
