# Release 最適化の検証

Release は R8 のコード最適化・難読化とリソース圧縮を有効にする。
Debug のテスト成功だけでは、反射や JNI の参照が Release で保持されることを確認できない。

## ローカル検証用の署名

製品版の署名は従来どおり `keystore.properties` と Release ワークフローで管理する。
既存の debug キーで手元の Release を検証するときは、一時的な Gradle init script を使う。
この設定は、明示的に `-I` を指定したビルドにだけ適用する。

PowerShell:

```powershell
$releaseInit = Join-Path $env:TEMP 'costcalc-local-release-signing.gradle'
@'
allprojects {
    plugins.withId('com.android.application') {
        androidComponents.finalizeDsl { extension ->
            extension.buildTypes.getByName('release').signingConfig = extension.signingConfigs.getByName('debug')
        }
    }
}
'@ | Set-Content -LiteralPath $releaseInit -Encoding utf8
./gradlew.bat -I scripts/prepare-release-ndk.gradle -I $releaseInit :app:assembleRelease :app:bundleRelease
```

この APK/AAB はローカル検証専用で、Play Console には入稿しない。
APK は `app/build/outputs/apk/release/app-release.apk` に出力される。
アプリ ID は製品版と同じなので、専用エミュレータで検証する。

## 検証する内容

- メニュー・単価比較・切手・設定・カメラ OCR が開くこと。
- 単価計算と切手の組み合わせ計算ができること。
- 設定と切手の入力を変更し、プロセス終了・再起動後にも復元されること（DataStore）。
- 日本語の値札から価格・内容量を読み取り、単価比較の入力に反映できること（CameraX / ML Kit / Hilt）。
- `adb logcat -b crash -d` にクラッシュがなく、`ComponentDiscovery` の Registrar 生成エラーがないこと。

ML Kit の Registrar は manifest の名前から反射で生成される。
`firebase-components:16.1.0` 同梱の consumer ルールだけでは、R8 full mode で公開の無引数コンストラクタが削除され、
カメラ画面で `NullPointerException` が発生した。
`app/proguard-rules.pro` はそのコンストラクタのみを追加保持する。
パッケージ全体の keep に戻さず、Release の実行結果で確認する。

## AAB のメタデータと CI

`app/build/outputs/bundle/release/app-release.aab` には以下を同梱する。

- `BUNDLE-METADATA/com.android.tools/r8.json`: 最適化設定・指標・DEX サイズ
- `BUNDLE-METADATA/com.android.tools.build.obfuscation/proguard.map`: 難読化マッピング

マッピングはローカルの `app/build/outputs/mapping/release/mapping.txt` と一致する。
Release ワークフローが保持する AAB artifact にも含まれるため、別の公開アセットにはしない。

PR の「Release 最適化検証」ジョブは署名シークレットを使わずに AAB を生成し、
コード・リソースの最適化が有効であること、最適化対象外の割合が各 75% 未満であること、
マッピングの同梱と一致を検証する。生成した AAB は配布しない。

Play の最終的な警告解消は、次回リリース入稿後に確認する。
Configuration Analyzer の最適化前の評価と、Play が参照する `r8.json` の指標は区別する。

参考: [R8 の有効化](https://developer.android.com/topic/performance/app-optimization/enable-app-optimization)、
[Android vitals の DEX 最適化指標](https://developer.android.com/google/play/vitals/code-optimization)

## ネイティブデバッグシンボル（#124）

Release の `ndk.debugSymbolLevel = "SYMBOL_TABLE"` で、抽出可能な関数名を
`BUNDLE-METADATA/com.android.tools.build.debugsymbols/` に同梱する。
自前の C/C++ コードはなく、依存 AAR の prebuilt ライブラリが対象となる。

| ライブラリ | 依存元 | 配布時のシンボル表 |
|---|---|---|
| `libdatastore_shared_counter.so` | DataStore 1.2.1 | 4 ABI とも `.symtab` あり |
| `libandroidx.graphics.path.so` | graphics-path 1.0.1 | 削除済み |
| `libimage_processing_util_jni.so` / `libsurface_util_jni.so` | CameraX 1.6.2 | 削除済み |
| `libmlkit_google_ocr_pipeline.so` | text-recognition-bundled-common 17.0.0 | 削除済み |

ABI は `arm64-v8a` / `armeabi-v7a` / `x86` / `x86_64` の4種類。
動的リンク用の `.dynsym` はデバッグ用の `.symtab` と区別する。
削除済みのシンボルや行番号は `FULL` に変更しても復元できないため、
関数名の取得に必要な `SYMBOL_TABLE` を選択する。

抽出には NDK が必要。NDK がない環境ではビルドが成功してもシンボルが生成されず、
削除済みというログが出る場合があるため、ログだけで判定しない。
CI と Release ワークフローは `scripts/prepare-release-ndk.gradle` を init script として適用し、
AGP の公開 API `sdkComponents.ndkDirectory` をビルド前に解決する。
未導入なら SDK ライセンスの同意を前提に AGP がインストールし、失敗時はビルドを停止する。
NDK の版は AGP の既定に従い、別途固定しない。
この PC で初回の調査時に導入した版は AGP 9.4.1 既定の以下の版。

```powershell
& "$env:ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager.bat" 'ndk;28.2.13676358'
```

PR の CI は DataStore の4 ABI分について、AAB 内の `.so.sym` が空でないこと、
`.symtab` を持つこと、AGP の抽出ファイルと一致することを検証する。
他の16本は依存元ですでにシンボル削除済みのため、同梱を要求しない。
シンボルは AAB 内に保持し、別の公開アセットやダミーのシンボルファイルは作成しない。

Play Console の警告が完全に消えるかは次回入稿時に確認する。
残る場合は、上記のシンボル削除済み prebuilt があることを根拠として記録する。
R8 の mapping は Java/Kotlin 用で、このネイティブシンボル表とは別に保持する。

2026-10-17、同じコード・NDK・未署名 Release で設定の有無を比較した。

| 対象 | シンボルなし | `SYMBOL_TABLE` |
|---|---:|---:|
| AAB | 26,221,610 bytes | 26,235,914 bytes |
| ネイティブシンボル | なし | 4ファイル・非圧縮35,408 bytes（ZIP内13,102 bytes） |

AAB 全体の増加は14,304 bytes。追加された4ファイル以外の ZIP エントリはすべて同一で、
DEX・ネイティブ本体・R8 メタデータ・mapping も変わらない。
4ファイルの `.symtab` と AGP 出力との一致、ユニットテスト42件、Android Lint、actionlint を確認した。
この比較はローカルの未署名 AAB で、Play への入稿確認ではない。

参考: [ネイティブシンボルの同梱と prebuilt の制約](https://developer.android.com/build/include-native-symbols)

## #119 の検証記録（2026-10-16）

比較元は #120 マージ後の `b8e4fcf`。同じ依存関係・既存 debug キーで生成した Release を比較した。

| 対象 | 最適化前 | 最適化後 |
|---|---:|---:|
| 非圧縮 DEX | 61,401,688 bytes（4ファイル） | 3,987,156 bytes（1ファイル） |
| AAB の R8 メタデータ | なし | R8 9.4.24、コード・リソース最適化有効 |

`r8.json` の最適化対象外の割合は、難読化 7.39%、最適化 8.25%、圧縮 7.27%。
mapping の同梱とローカル出力との一致も確認した。

API 37 のエミュレータに最適化済み Release APK を入れ、以下を確認した。

- メニュー・単価比較・切手・設定・カメラ OCR の起動。
- カメラ入力を日本語の値札画像（「本体価格 ¥398」「内容量 500g」）に固定し、
  価格と内容量を選択、入数をスキップ。398円・500g・入数1が入力に反映され、0.80円/unit と表示された。
- 切手の目標金額を180円から220円に変更して計算し、84円1枚・63円2枚・10円1枚の結果が表示された。
- フォント設定を「標準」から「大」に変更し、プロセス終了・再起動後も「大」が選択された。
  切手の目標金額220円も復元された。
- 最終検証中の crash バッファと Registrar 生成エラーは0件。

ユニットテスト42件・Android Lint、および CI に追加した未署名 AAB のビルド／メタデータ検証も成功した。
Play Console への入稿・警告解消の確認は次回リリース時に行う。
