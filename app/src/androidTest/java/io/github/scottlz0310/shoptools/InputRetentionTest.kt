package io.github.scottlz0310.shoptools

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.scottlz0310.shoptools.app.MainActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * 単価比較の入力は保存されない（メモリ上のみ）。買い物中に戻る操作で消えないよう、
 * メニューへ戻って開き直しても残ることを確認する。
 */
@HiltAndroidTest
class InputRetentionTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() = hiltRule.inject()

    @Test
    fun unitPriceInputSurvivesGoingBackToMenu() {
        val activity = composeRule.activity
        val priceField = hasText(activity.getString(R.string.hint_price)) and hasSetTextAction()

        ScreenCase.UNIT_PRICE.open(composeRule, activity)
        composeRule.onAllNodes(priceField).onFirst().performTextInput(INPUT)

        composeRule.onNodeWithContentDescription(activity.getString(R.string.nav_back)).performClick()
        ScreenCase.UNIT_PRICE.open(composeRule, activity)

        composeRule.onAllNodes(hasText(INPUT) and hasSetTextAction()).onFirst().assertIsDisplayed()
    }

    private companion object {
        const val INPUT = "12345"
    }
}
