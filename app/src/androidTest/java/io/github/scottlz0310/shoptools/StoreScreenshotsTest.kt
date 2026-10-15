package io.github.scottlz0310.shoptools

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.scottlz0310.shoptools.app.MainActivity
import io.github.scottlz0310.shoptools.feature.stamps.data.StampsRepository
import io.github.scottlz0310.shoptools.feature.unitprice.UnitPriceEvent
import io.github.scottlz0310.shoptools.feature.unitprice.UnitPriceViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.io.File
import javax.inject.Inject

/**
 * Google Play のストア掲載用に、各画面のスクリーンショットを撮る。通常のテスト実行では、何もせずに成功する。
 *
 * 撮影するときだけ、エミュレータまたは端末を起動して、次の引数で実行する。
 *
 *     ./gradlew :app:connectedDebugAndroidTest \
 *       -Pandroid.testInstrumentationRunnerArguments.class=io.github.scottlz0310.shoptools.StoreScreenshotsTest \
 *       -Pandroid.testInstrumentationRunnerArguments.captureScreenshots=true
 *
 * 画像は、端末の `Android/data/<applicationId>/files/screenshots/` に保存される。`adb pull` で取得する。
 * システムバー（ステータスバー・ナビゲーションバー）を除いた、アプリの画面だけを保存する。
 *
 * 入力は、画面の文字入力ではなく、ViewModel とリポジトリの状態を直接セットする。LazyColumn の画面外にある欄は
 * 文字入力できず、状態が撮影のたびにぶれないようにするため。
 */
@HiltAndroidTest
class StoreScreenshotsTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var stampsRepository: StampsRepository

    @Test
    fun captureStoreScreenshots() {
        // 撮影が有効でなければ、何もせずに成功する。assumeTrue によるスキップは、AGP の結果 XML では
        // 失敗（<failure>）として記録され、結果 XML で判定する CI（run-ui-tests.sh）が落ちるため使わない。
        if (InstrumentationRegistry.getArguments().getString(ARG_CAPTURE) != "true") return
        hiltRule.inject()
        val activity = composeRule.activity

        capture("1-menu")

        // 単価比較: 2 商品を入力し、最安がハイライトされた状態。
        val unitPrice = ViewModelProvider(activity)[UnitPriceViewModel::class.java]
        val rows = unitPrice.uiState.value.rows
        check(rows.size >= 2) { "単価比較の初期の行が 2 行未満" }
        listOf(
            Triple("おいしいコーヒー", "1234", "250"),
            Triple("お得なコーヒー", "2500", "600"),
        ).forEachIndexed { index, (name, price, quantity) ->
            unitPrice.onEvent(
                UnitPriceEvent.UpdateRow(rows[index].copy(name = name, price = price, quantity = quantity, unit = "g")),
            )
        }
        ScreenCase.UNIT_PRICE.open(composeRule, activity)
        capture("2-unit-price")
        goBackToMenu()

        // 切手: 目標金額 180 円に対して、ぴったり・足りない・超えるの 3 カテゴリが並んだ状態。
        runBlocking {
            stampsRepository.saveState(
                rows = listOf("110" to "1", "84" to "2", "63" to "2", "10" to "10"),
                target = "180",
            )
        }
        ScreenCase.STAMPS.open(composeRule, activity)
        val list = composeRule.onNode(hasScrollAction())
        list.performScrollToNode(hasText(activity.getString(R.string.btn_calculate)) and hasClickAction())
        composeRule.onNode(hasText(activity.getString(R.string.btn_calculate)) and hasClickAction()).performClick()
        list.performScrollToIndex(0)
        capture("3-stamps")
        goBackToMenu()

        // 設定: フォントサイズ「特大」を適用した状態。撮影後は、既定の「標準」に戻す。
        ScreenCase.SETTINGS.open(composeRule, activity)
        composeRule.onNodeWithText(activity.getString(R.string.settings_font_xlarge)).performClick()
        composeRule.waitForIdle()
        capture("4-settings")
        composeRule.onNodeWithText(activity.getString(R.string.settings_font_normal)).performClick()
    }

    private fun goBackToMenu() {
        composeRule
            .onNodeWithContentDescription(composeRule.activity.getString(R.string.nav_back))
            .performClick()
        composeRule.waitForIdle()
    }

    /** システムバーを除いた、アプリの画面だけを PNG で保存する。 */
    private fun capture(name: String) {
        composeRule.waitForIdle()
        val full = composeRule.onRoot().captureToImage().asAndroidBitmap()

        var top = 0
        var bottom = 0
        composeRule.runOnUiThread {
            // システムバーだけでなく、パンチホールなどの切り欠きも含める。ステータスバーの高さは、実行によって
            // 63px になったり切り欠きを含む 128px になったりする（エミュレータで確認）一方、TopAppBar は常に
            // 切り欠きを含めた余白を取る。システムバーだけで切り取ると、タイトルの上に空白が残り、画像の高さもぶれる。
            val insets =
                checkNotNull(ViewCompat.getRootWindowInsets(composeRule.activity.window.decorView)) {
                    "ウィンドウが未アタッチで WindowInsets を取得できない"
                }.getInsetsIgnoringVisibility(
                    WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
                )
            top = insets.top
            bottom = insets.bottom
        }
        val content = Bitmap.createBitmap(full, 0, top, full.width, full.height - top - bottom)

        val dir = File(composeRule.activity.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { content.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private companion object {
        const val ARG_CAPTURE = "captureScreenshots"
    }
}
