# Copilot 指示書

## プロジェクト概要

`costcalc`（アプリ名: ShopTools）は Android 向け家計計算アプリ。電卓では難しい「単価比較」と「切手組み合わせ計算」を提供する。

## 技術スタック

- Kotlin + Jetpack Compose (Material 3)
- Navigation Compose（フラットなボトムナビゲーション）
- ViewModel + StateFlow（MVVM）
- Hilt（DI）
- DataStore Preferences（設定の永続化）
- JUnit 4（単体テスト）
- minSdk 26 / targetSdk 35 / JDK 17

## ビルド・テスト

```bash
# デバッグビルド
./gradlew assembleDebug

# 全テスト実行
./gradlew test

# 特定テストクラスのみ実行
./gradlew test --tests "com.example.shoptools.feature.stamps.domain.BoundedSubsetSumTest"
```

## アーキテクチャ

```
com.example.shoptools
├─ app/        # Application クラス、MainActivity、NavHost、Screen sealed class
├─ design/     # ShopToolsTheme、LargeResultCard、ErrorText（共通 UI パーツ）
├─ core/       # NumberFormat（formatUnitPrice / formatAmount）、Validation（validateXxx 関数群）
└─ feature/
   ├─ unitprice/  ui/ + domain/UnitPriceCalculator + UnitPriceViewModel
   ├─ stamps/     ui/ + domain/BoundedSubsetSum    + StampsViewModel
   └─ settings/   ui/ + data/SettingsRepository    + SettingsViewModel
```

- **ナビゲーション**: `MainActivity` 内の `NavHost` でフラットに管理。`Screen` sealed class がルート文字列・ラベル・アイコンを保持する。
- **設定の伝播**: `SettingsRepository.settingsFlow`（`Flow<AppSettings>`）を各 ViewModel が `launchIn(viewModelScope)` で購読し、`useDigitSeparator` や `fontSizePreset` を UiState に反映する。
- **テーマのフォントスケール**: `ShopToolsTheme` が `FontSizePreset`（NORMAL/LARGE/XLARGE）に応じた `scale`（1.0 / 1.2 / 1.5）で全 Typography を生成する。

## 言語とコミュニケーション

- このリポジトリでは日本語を使用します。
- AI エージェントの返答は日本語で行います。
- ドキュメントは日本語で記述します。
- コミットメッセージは日本語で記述します。
- 英語ドキュメントを見つけた場合は、日本語版を作成して置き換えます。

## ブランチ運用

- 原則として `main` に直接コミットせず、サブブランチで作業して PR でマージします。
- 大きいタスクは先に Issue を作成して整理します。
- ドキュメント更新やリリース準備は `main` で直接作業しても構いません。

## PR 作成後の自動レビュー対応ルーティン

PR を作成した後は、Copilot 自動レビューおよび CI の結果を確認し、指摘がなくなるまで自動で修正イテレーションを行います。

### 監視ループ

- PR 作成直後に監視ループを開始します。
- 監視頻度：**2 分間隔**、最大継続時間：**10 分（6 回チェック）**。
- 各チェックで以下を確認します。
  1. PR のレビューコメント（Copilot 自動レビュー等）と通常コメント（Issue comments: Codecov など Bot コメントを含む）の有無
  2. CI（GitHub Actions）のステータス（pending / running / success / failure）
- 「Copilot code review」ワークフロー（Copilot がレビューリクエスト時に自動実行する特別なワークフロー）が実行中の場合は、レビュー未完了として待機を継続します。

### 指摘・CI エラー検出時の修正

- レビューコメント、通常コメント（Issue comments）、または CI 失敗を検出した場合は、即座に修正を行います。
- レビューコメントと通常コメント（Issue comments）は内容を精査し、採否を判断します。正当な指摘は修正し、的外れな指摘は理由を付けて却下します。いずれの場合もコメントに返信を残してください。
- 修正後は**必ず新しいコミットを作成して push** します（Copilot 自動レビューは新しいコミットの push でのみ再実行されるため）。
- コード変更がない場合でも、空コミットで push して再レビューを発火させます。
  ```bash
  git commit --allow-empty -m "chore: Copilot 自動レビューを再実行"
  git push
  ```
- push 完了後、**監視ループを最初からリセット**（再度 10 分間の監視）して再確認します。

### 完了条件

以下がすべて満たされた場合、PR を「レビュー完了」とみなしループを終了します。

- 新しいレビューコメントと通常コメント（Issue comments）が無い
- CI がすべて成功している
- 「Copilot code review」ワークフローが完了済みである

## バージョン調査の注意

- AI から見て不自然に新しいバージョンに感じても、勝手にバージョンダウンしないでください。
- 学習時期のタイムラグを前提に、必要に応じて Web で最新情報を確認します。

## コーディング規約

### ViewModel パターン
各 feature の ViewModel は以下の構造を持つ：
- `XxxUiState` data class（StateFlow で保持）
- `XxxEvent` sealed class（`onEvent(event)` 関数でディスパッチ）
- `_uiState: MutableStateFlow` + `uiState: StateFlow`（asStateFlow でラップ）

### バリデーション
- `core/Validation.kt` の `validateXxx(input: String): ValidationResult` を使用する。
- 入力中（typing 時）は**空白ならエラー非表示**、値ありならリアルタイム検証。
- 計算ボタン押下時に全フィールドを一括検証する。

### 数値フォーマット
- `core/NumberFormat.kt` の `formatUnitPrice` / `formatAmount` を使用する。
- 桁区切り有無は `useDigitSeparator: Boolean` で切り替え。`Locale.JAPAN` で `%,` 書式を使用する（`Locale.US` は区切りなし用）。

### テスト
- テストは domain 層（`UnitPriceCalculator`、`BoundedSubsetSum` など）を対象とする。
- ViewModel・UI 層のテストは現状なし。

## 備考

- 将来 TODO は `README.md` の末尾に記載されている。
- ダークモード未対応（`LightColorScheme` のみ定義）。
