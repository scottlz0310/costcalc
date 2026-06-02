# CHANGELOG

このファイルはプロジェクトの変更履歴を記録します。  
形式は [Keep a Changelog](https://keepachangelog.com/ja/1.0.0/) に準拠し、バージョン管理は [Semantic Versioning](https://semver.org/lang/ja/) に従います。

---

## [Unreleased] - 2026-06-02 (2)

### 変更
- `hiltViewModel` を `hilt-lifecycle-viewmodel-compose:1.3.0` の新パッケージに移行 (#37)
  - `androidx.hilt.navigation.compose.hiltViewModel` → `androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel`
  - `MainActivity.kt` の deprecated 警告を解消

---

## [Unreleased] - 2026-06-02

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

---

## [Unreleased] - 2026-05-30

### 追加
- `lefthook.yml` — lefthook 導入。pre-push で `./gradlew :app:testDebugUnitTest`（Android ユニットテスト）を実行し push 前ゲートとする。Kotlin リンタ（ktlint）は後続 #32 で対応予定。

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
<!-- ## [1.1.0] - YYYY-MM-DD -->
