# ShopTools（お買い物ツール）

主婦の味方！電卓だけではわかりにくい計算を助けます！

Android アプリ（Kotlin + Jetpack Compose）

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
- Android Studio Ladybug（2024.2）以降
- Android SDK 35
- JDK 17

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

---

## アーキテクチャ

```
com.example.shoptools
├─ app/        (Application, MainActivity, NavHost)
├─ design/     (Theme, LargeResultCard, ErrorText)
├─ core/       (NumberFormat, Validation)
└─ feature/
   ├─ unitprice/
   │  ├─ ui/             (UnitPriceScreen)
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
- JUnit 4 (単体テスト)

---

## 将来TODO

- [ ] CSV/JSON の入出力（権限と保存先の方針検討）
- [ ] 単価の「/100g」などの換算表示スイッチ
- [ ] 切手DPの高速化オプション（maxTotal制限、ビーム探索）
- [ ] 単価桁数スライダーUI
- [ ] ダークモード対応
