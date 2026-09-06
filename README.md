# お買い物計算ツール（ShopTools）

主婦の味方！電卓だけではわかりにくい計算を助けます！

Android アプリ（Kotlin + Jetpack Compose）

---

## 配布

公式の配布先は **Google Play のみ**（現在準備中）。GitHub Releases への APK 添付は終了した。

タグ push 時に CI が生成するのは Play へアップロードするための AAB のみで、Actions artifact としてのみ保持する。リポジトリからビルドした独自ビルドは公式版・公式サポート対象として扱わない。

---

## 機能

### 🛒 単価比較
- 商品の価格・内容量・入数を入力して単価を自動計算
- 最もお得な商品を大きくわかりやすく表示
- 内容量は小数OK（0.5L、1.25kgなど）
- 単位は表示用のみ（計算には使用しない）

### ✉️ 切手組み合わせ
- 手持ちの切手在庫（額面・枚数）を登録
- 目標金額に対して「ぴったり」「ちょっと足りない」「ちょっと超える」の組み合わせを提案
- 差分・枚数の少ない順でランキング表示

### ⚙️ 設定
- フォントサイズのプリセット切り替え（標準・大・特大）
- 金額の桁区切り表示（ON/OFF）

---

## セットアップ

### 必要環境
- JDK 17 以上（Android Studio 付属の JBR でも可）
- Android SDK（Android Studio 付属、または cmdline-tools のみでも可）
- Android Studio は **任意**

### ビルド方法
```bash
# クローン
git clone https://github.com/scottlz0310/costcalc.git
cd costcalc

# ビルド
./gradlew assembleDebug

# テスト実行
./gradlew test
```

### Android SDK セットアップ（Android Studio なしの場合）

Android Studio なしで Android SDK Command-line Tools のみを使用してビルドできます。

**SDK インストール先（Windows 推奨）**

```
%LOCALAPPDATA%\Android\Sdk
```

**cmdline-tools のダウンロード**

[Android Studio ダウンロードページ](https://developer.android.com/studio#command-line-tools-only) の「Command line tools only」から Windows 用 ZIP を取得し、以下の構成で展開する。

```
%LOCALAPPDATA%\Android\Sdk\
└── cmdline-tools\
    └── latest\        ← ZIP 内の cmdline-tools フォルダをここに配置
        └── bin\
            └── sdkmanager.bat
```

**必要パッケージのインストール**

```powershell
$sdkmanager = "$env:LOCALAPPDATA\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat"

# ライセンスに同意
& $sdkmanager --licenses

# 必要パッケージをインストール
& $sdkmanager "platform-tools" "platforms;android-36" "build-tools;36.0.0"
```

**local.properties の作成**

プロジェクトルートに `local.properties` を作成する（`<username>` を実際のユーザー名に置き換える）。

```properties
sdk.dir=C\:\\Users\\<username>\\AppData\\Local\\Android\\Sdk
```

> `local.properties` はローカル環境依存のため `.gitignore` に含まれており、コミットしない。

**環境変数（省略可）**

```powershell
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:ANDROID_SDK_ROOT = "$env:LOCALAPPDATA\Android\Sdk"
$env:PATH = "$env:ANDROID_HOME\cmdline-tools\latest\bin;$env:ANDROID_HOME\platform-tools;$env:PATH"
```

**SDK の更新**

```powershell
& $sdkmanager --update
```

**実機への転送（APK 直接インストール）**

```powershell
# APK ビルド
.\gradlew.bat assembleDebug

# adb でインストール（USB デバッグ有効な端末を接続済みの場合）
adb install app\build\outputs\apk\debug\app-debug.apk
```

### Git フック（lefthook）

本リポジトリは [lefthook](https://github.com/evilmartians/lefthook) で以下を設定している。

| フック | 内容 |
|-------|------|
| pre-commit | ktlint による Kotlin コードフォーマット検査・自動整形 |
| pre-push | Android ユニットテスト |

**クローン後に一度だけ** 下記を実行すること（未実行だとフックは動作しない）。

```bash
# 1. ktlint CLI をインストール（~/.local/bin に配置）
bash scripts/install-ktlint.sh

# 2. lefthook をインストールしてフックを登録
pnpm install
```

lefthook は `devDependencies` で管理しており、`pnpm install` の `prepare` スクリプトが
`lefthook install` まで実行する。lefthook 自体を別途インストールする必要はない。

> **Windows の場合**: Git Bash または WSL 上で `scripts/install-ktlint.sh` を実行するか、
> [ktlint GitHub Releases](https://github.com/pinterest/ktlint/releases/tag/1.5.0) から
> `ktlint` バイナリを手動ダウンロードして PATH の通ったディレクトリに配置する。

---

## アーキテクチャ

```
io.github.scottlz0310.shoptools
├─ app/        (Application, MainActivity, NavHost)
├─ design/     (Theme, LargeResultCard, ErrorText)
├─ core/       (NumberFormat, Validation)
└─ feature/
   ├─ unitprice/
   │  ├─ ui/             (UnitPriceScreen)
   │  │  └─ ocr/         (CameraOcrScreen, OcrViewModel, TextParser, OcrCandidate)
   │  ├─ domain/         (UnitPriceCalculator)
   │  └─ UnitPriceViewModel.kt
   ├─ stamps/
   │  ├─ ui/             (StampsScreen)
   │  ├─ domain/         (BoundedSubsetSum)
   │  └─ StampsViewModel.kt
   └─ settings/
      ├─ ui/             (SettingsScreen)
      ├─ data/           (SettingsDataStore, SettingsRepository)
      └─ SettingsViewModel.kt
```

**技術スタック**
- Kotlin + Jetpack Compose (Material 3)
- Navigation Compose
- ViewModel + StateFlow (MVVM)
- Hilt (DI)
- DataStore Preferences (設定の永続化)
- CameraX 1.5.1（カメラプレビュー・画像解析）
- ML Kit Text Recognition Japanese（オフライン OCR）
- JUnit 4 (単体テスト)

---

## 将来TODO

- [ ] CSV/JSON の入出力（権限と保存先の方針検討）
- [ ] 単価の「/100g」などの換算表示スイッチ
- [ ] 切手DPの高速化オプション（maxTotal制限、ビーム探索）
- [ ] 単価桁数スライダーUI
- [ ] ダークモード対応
