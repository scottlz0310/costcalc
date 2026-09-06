# CHANGELOG

このファイルはプロジェクトの変更履歴を記録します。  
形式は [Keep a Changelog](https://keepachangelog.com/ja/1.0.0/) に準拠し、バージョン管理は [Semantic Versioning](https://semver.org/lang/ja/) に従います。

---

## [Unreleased]

---

## [1.2.0] - 2026-09-06

**Google Play での初回リリース。** 公式配布を Google Play 一本に切り替え、`applicationId` を移行した。

### 追加
- Google Play 掲載用の原稿を `docs/` に追加 (#73)
  - `docs/privacy-policy.md` — プライバシーポリシー。GitHub Pages で `https://scottlz0310.github.io/costcalc/privacy-policy/` として公開する
  - `docs/play-store-listing.md` — ストア掲載文・データセーフティ申告・コンテンツレーティング・素材チェックリスト
- `docs/` を GitHub Pages で配信する設定（`_config.yml` / `index.md`）(#73)
- `scripts/generate_play_assets.mjs` — Play の要求仕様に合わせて素材を変換する (#73)
  - スクリーンショットは撮影元の 1096×2560 が「長辺は短辺の2倍以内」を満たさないため、システムナビゲーションバーを除いて 9:16（1440×2560）へ整形する
  - フィーチャーグラフィックを 1024×500 へ
- `pnpm-lock.yaml` を追跡対象に追加し、`packageManager` で pnpm 11.25.0 を固定 (#73)
- lefthook を `devDependencies` で管理し、`pnpm install` の `prepare` でフック登録まで完了させる (#73)
- `pnpm-workspace.yaml` の `allowBuilds` で lefthook のビルドスクリプトを明示的に拒否 (#73)
  - pnpm 11 は `strictDepBuilds` が既定 true のため、未定義だと `pnpm install --frozen-lockfile` が終了コード 1 で失敗する

### 破壊的変更
- `applicationId` を `com.example.shoptools` から `io.github.scottlz0310.shoptools` へ変更 (#73)
  - **既存インストールは自動更新されない。** Android は `applicationId` が異なるアプリを別アプリとして扱うため、更新するには一度アンインストールしてから再インストールする必要がある
  - `namespace` と Kotlin の `package` も同じ名前空間へ統一

### 変更
- アプリ名を `ShopTools` から `お買い物計算ツール` へ変更 (#73)
  - Play 掲載にあたり、日本語話者に機能が伝わる名前にする。ランチャー表示名とストア掲載名を揃える
  - コード上の識別子（`ShopToolsApp` / `ShopToolsTheme` / `Theme.ShopTools`）はコード名として維持する
- `versionCode` を 4 へ、`versionName` を `1.2.0` へ更新 (#73)
- 公式配布を Google Play 一本に変更 (#73)
  - GitHub Releases への APK 添付を廃止。タグ push では Play アップロード用の AAB を生成し、Actions artifact としてのみ保持する（retention 7日）
  - GitHub Release の draft 作成はリリースノート用に継続するが、バイナリは添付しない
  - main push での debug APK artifact 公開を廃止（ビルド検証としての `assembleDebug` は継続）
- release 署名鍵を再作成 (#73)
  - 旧鍵は GitHub Actions secrets にしか存在せず読み出し不可のため実質失われていた
  - 新しい鍵は Play へアップロードする AAB のアップロード鍵として使う
- `compileSdk` を 36 → 37 に引き上げ（`androidx.core:core-ktx 1.19.0` の要件に対応）
- Dagger/Hilt 2.60 の生成 Java ソースが参照する `error_prone_annotations` を `compileOnly` 依存として明示し、推移依存への暗黙依存を解消
- Renovate に Node.js / pnpm / lefthook のプリセットを追加 (#73)
  - ロックファイルの追跡により npm エコシステムが対象になったため。`options/automerge` は required status checks でゲートできないため採用しない

---

## [1.1.1] - 2026-06-02

### 変更
- CI ワークフローを整備: `ci.yml`（全ブランチ push + PR → テスト + ktlint）と `release.yml`（main push → debug APK、タグ push → release APK + GitHub Release draft）に分離

---

## [1.1.0] - 2026-06-02

### 追加
- 単価比較：カメラ OCR によるフィールド自動入力（AR タップ方式）(#35)
  - `ProductRowEditor` に「カメラで入力」ボタンを追加
  - CameraX + ML Kit Text Recognition Japanese（オフライン）で値札をリアルタイム認識
  - ステップ式 UI（① 価格 → ② 内容量＋単位 → ③ 入数）で候補チップをタップして確定
  - confidence 0.5 以上を候補表示、0.75 以上を強調（黄色ボーダー）
  - 価格候補スコアリング（周辺語 ¥/円/税込、画面中央付近を加点）
  - ② パース失敗時はエラーメッセージを表示し手入力を促す
  - 「最初から」「スキップ」「閉じる」でミスリカバリー、閉じると手入力値を保持
  - `UnitPriceViewModel` を NavGraph スコープで共有（Activity-scoped 昇格なし）
  - `TextParser`（純粋ロジック）の JUnit テスト 19 ケースを追加
- Android SDK セットアップ手順を README.md に追加（Android Studio なし・cmdline-tools のみでビルド可能）
- `lefthook.yml` 導入: pre-push で `./gradlew :app:testDebugUnitTest` を実行し push 前ゲートとする (#33)

### 変更
- カメラ OCR を真の AR オーバーレイ実装に刷新 (#39)
  - `LazyRow` チップを廃止し、値札上のバウンディングボックスに半透明チップを Canvas 描画
  - `OcrScoreAccumulator`（Android 非依存）: フレーム横断スコア収束ロジック（検出→加算、非検出→減衰、閾値未満→削除）
  - `OcrCoordinateMapper`: `ImageProxyTransformFactory` + `PreviewView.getOutputTransform()` + `CoordinateTransform.mapRect()` で画像座標→View座標変換
  - `OcrRect` / `OcrRectF`: プラットフォーム非依存の座標型（`OcrScoreAccumulator` が Android 非依存であるための基盤）
  - `PreviewView.ImplementationMode.COMPATIBLE` に固定し座標変換の精度を確保
  - `CandidateKey`（step + normalizedText + regionBucket）で同一候補を跨フレームで識別
  - `OcrScoreAccumulatorTest`: 9 ケース追加（スコア蓄積・上限・減衰・削除・表示閾値・regionBucket・reset・ソート・正規化）
- `hiltViewModel` を `hilt-lifecycle-viewmodel-compose:1.3.0` の新パッケージに移行 (#37)
  - `androidx.hilt.navigation.compose.hiltViewModel` → `androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel`
  - `MainActivity.kt` の deprecated 警告を解消

### 修正
- AR オーバーレイ: `pointerInput(candidates)` を `pointerInput(Unit)` + `rememberUpdatedState` に変更し、OCR フレーム更新によるジェスチャー検出コルーチン再起動でタップが消失するバグを修正 (#42)
- AR オーバーレイ: `viewTransformRef` (AtomicReference) を廃止し、アナライザースレッドで `inputTransform` を取得・メインスレッドで `previewView.outputTransform` を直接参照する方式に変更（StateFlow 等値チェックによる recomposition スキップで変換が null 固定になるバグを修正）(#42)
- AR オーバーレイ: QUANTITY/COUNT ステップの `regionBucket=0` 固定化でカメラ微動による `CandidateKey` 分散を解消 (#42)
- `TextParser`: 価格候補抽出時に直前文字が ASCII 英字またはドットの場合を除外し、`900m2` → `2` 等の OCR ノイズ偽陽性を低減 (#42)
- `TextParser`: `QUANTITY_REGEX` を IGNORE_CASE 化し `QUANTITY_REGEX_NOISY_ML` を追加（`㎖` が `m2`/`m!`/`m&` 等に誤読された場合の noisy fallback）、`OCR_O_FOR_ZERO` で `O→0` 補正 (#42)
- `TextParser`: `UNIT_SUFFIXES` の suffix 判定を `ignoreCase=true` に変更し大文字 OCR 結果の価格候補混入を防止 (#42)
- `TextParser`: `QUANTITY_REGEX_NOISY_ML` に `(?![a-zA-Z])` 否定先読みを追加し `500mg`/`900mAh` 等の非 mL 単位の誤マッチを防止 (#42)

---

## [1.0.0] - 2026-02-22

### 追加
- 単価比較機能：商品名・価格・内容量・入数を入力して最安値を自動判定
- 切手組み合わせ機能：手持ちの切手在庫から目標金額を達成する組み合わせを提案
- 設定機能：フォントサイズのプリセット切り替え（標準・大・特大）、桁区切り表示の ON/OFF
- 切手在庫の永続化（DataStore - アプリ終了後もデータを保持）
- 最終行のごみ箱ボタンをクリアボタンとして機能（入力欄が消えない）
- 署名付きリリース APK ビルド環境（GitHub Actions CI）
- アプリアイコン（ShopTools デザイン）

### 修正
- 日本語IME（すみれキーボード等）での入力改善：`singleLine=true` → `maxLines=1` に変更

---

<!-- 次のリリースはここに追加 -->
<!-- ## [1.2.0] - YYYY-MM-DD -->
