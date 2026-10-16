package io.github.scottlz0310.shoptools

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
 * 画面上のテキストがステータスバー／ナビゲーションバーと重ならないこと。
 *
 * targetSdk を引き上げた際の edge-to-edge・WindowInsets の挙動変化は、コンパイルにも
 * JVM ユニットテストにも現れない。実行端末の API が targetSdk 以上でないと検出できないため、
 * CI ではエミュレータ API を引き上げ先に合わせて実行する。
 */
@HiltAndroidTest
@RunWith(Parameterized::class)
class SystemBarInsetsTest(
    private val screen: ScreenCase,
) {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() = hiltRule.inject()

    @Test
    fun textStaysInsideSystemBars() {
        val activity = composeRule.activity
        screen.open(composeRule, activity)

        val decorView = activity.window.decorView
        val contentView = activity.findViewById<android.view.View>(android.R.id.content)
        val contentLocation = IntArray(2)
        composeRule.runOnUiThread { contentView.getLocationInWindow(contentLocation) }
        assertTrue(
            "コンテンツがウィンドウ全体に広がっていない: top=${contentLocation[1]} height=${contentView.height} window=${decorView.height}",
            contentLocation[1] == 0 && contentView.height == decorView.height,
        )
        val insets =
            checkNotNull(ViewCompat.getRootWindowInsets(decorView)) {
                "ウィンドウが未アタッチで WindowInsets を取得できない"
            }.getInsets(WindowInsetsCompat.Type.systemBars())
        val safeTop = insets.top
        val safeBottom = decorView.height - insets.bottom

        val textBounds =
            composeRule
                .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text))
                .fetchSemanticsNodes()
                .map { it.boundsInWindow }
                .filterNot { it.isEmpty }
        assertTrue("テキストノードが 1 つも見つからない", textBounds.isNotEmpty())

        val overlapping =
            textBounds.filter { it.top < safeTop - TOLERANCE_PX || it.bottom > safeBottom + TOLERANCE_PX }
        assertTrue(
            "システムバーと重なるテキストがある: safe=[$safeTop, $safeBottom] 違反=$overlapping",
            overlapping.isEmpty(),
        )
    }

    companion object {
        private const val TOLERANCE_PX = 1f

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun screens() = ScreenCase.entries.toList()
    }
}
