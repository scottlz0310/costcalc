package io.github.scottlz0310.shoptools

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.scottlz0310.shoptools.app.MainActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** メニューに、各機能のカードが表示されること。 */
@HiltAndroidTest
@RunWith(Parameterized::class)
class MenuScreenTest(
    private val feature: ScreenCase,
) {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() = hiltRule.inject()

    @Test
    fun showsFeatureCard() {
        val activity = composeRule.activity
        val card = feature.entry as Entry.Card

        composeRule
            .onNode(hasText(activity.getString(card.title)) and hasClickAction())
            .assertIsDisplayed()
    }

    companion object {
        /** メニューにカードとして並ぶ機能。 */
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun features() = ScreenCase.entries.filter { it.entry is Entry.Card }
    }
}
