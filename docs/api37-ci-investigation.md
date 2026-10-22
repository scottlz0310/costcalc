# API 37 CI の Ubuntu 調査（#101）

調査日: 2026-10-16。対象コミット: `67038b9cb3c855dfd8bddbe240e30bb4ae9853eb`。

## 判明したこと

API 37 のイメージそのものの更新待ちに限定せず、**AVD を生成する Command-line Tools の更新**を先に試す。
旧 `avdmanager` は小数付きの API バージョンを扱えず、`android-37.0` の AVD に `target=android-0` を記録する。
Ubuntu 上で Command-line Tools 19.0 と 22.0 を比較し、前者では既知のクラッシュを再現、後者では起動が安定することを確認した。
新ツールで作った AVD では UI テスト **24 件が初回ですべて成功**した。

## CI の証跡

[PR #123 の実行](https://github.com/scottlz0310/costcalc/actions/runs/38888574524/job/116932248960) の artifact `android-ui-test-report-api-37.0-28` を調査。

- Ubuntu 24.04.5、runner image `20261012.334.1`、emulator `37.2.12.0` / build `16428233`。
- fingerprint: `google/sdk_gphone64_x86_64/emu64xa:17/CE2A.260420.019/15611780:userdebug/dev-keys`。
- 最初の試行は Gradle 終了コード 0、テスト 0 件。2 回目は終了コード 1、24 件中 24 件失敗。
- `surfaceflinger` の `RegionSampling` が `mapper.ranchu.so` の `GoldfishMapper::readFromHost` で SIGABRT。
  `Assertion failed: !rcEnc->featureInfo()->hasReadColorBufferDma` を記録してフレームワークの再起動を繰り返す。
- `Unable to resolve activity` は、この端末障害の後で発生している。

[実行時 runner のソフトウェア一覧](https://github.com/actions/runner-images/blob/ubuntu24/20261012.334/images/ubuntu/Ubuntu2404-Readme.md) では Command-line Tools は **12.0**。
[実行時 Action の SDK インストール処理](https://github.com/ReactiveCircus/android-emulator-runner/blob/a421e43855164a8197daf9d8d40fe71c6996bb0d/src/sdk-installer.ts) は
`cmdline-tools` ディレクトリが存在するとダウンロードを省略し、既存の `cmdline-tools/latest/bin` を PATH に追加する。
Action 内に新しいツールの URL があっても、既存 runner のツールは更新されない。
なお過去 CI の AVD の `.ini` 自体は artifact に含まれないため、その `target` の実値は今回直接確認できていない。

## Ubuntu での比較条件

- ネイティブ Ubuntu 26.04.1 / kernel `7.0.0-15-generic`、Intel Core i7-12700K。
- KVM: `emulator -accel-check` が `KVM (version 12) is installed and usable.`。
- emulator `37.2.12.0` / build `16428233`。
- `system-images;android-37.0;google_apis;x86_64` revision 6。
- Pixel 6、4 cores、RAM 4096 MB、データ約 6 GB、headless、`-gpu swiftshader_indirect`、`-no-snapshot`。
- 同じシステムイメージから、Command-line Tools 19.0 / 22.0 で AVD を作成した。
- JDK はビルド用に Eclipse Temurin 25 を `/tmp` に配置。製品署名は使用しない。

| 項目 | 旧ツール 19.0 | 新ツール 22.0 |
|---|---|---|
| AVD `.ini` の target | `android-0` | `android-37.0` |
| emulator が認識した API | 3 | 37 |
| gfxstream GlDirectMem | 0 | 1 |
| gfxstream HasSharedSlotsHostMemoryAllocator | 0 | 1 |
| 起動直後 | 同じ assertion による SurfaceFlinger 再起動を再現 | 同じ assertion は観測されず |

`config.ini` のキーと値を空白を除いて比較すると、target とデータサイズ表記に差がある。
さらに、同じ旧 AVD の `.ini` の `target=android-0` だけを `target=android-37.0` に修正して再起動した。
エミュレータの認識 API が 3 から 37 に、上記 2 機能が 0 から 1 に変わることを確認した。
この target のみ修正した旧 AVD でも **24 件が初回ですべて成功**し、既知の assertion は 0 件だった。
GPU バックエンドやシステムイメージを交換せずに改善しており、今回のローカル再現では AVD の API 誤認が原因と判断できる。

## 再現手順

同じ SDK に Command-line Tools 19.0 と 22.0 を並べて導入し、専用の AVD ディレクトリを指定する。
次の作成コマンドを両バージョンの `avdmanager` で実行し、`api37-old` / `api37-new` を作る。

```bash
export ANDROID_AVD_HOME=/tmp/costcalc-101/avd
echo no | "$ANDROID_HOME/cmdline-tools/22.0/bin/avdmanager" create avd \
  --force -n api37-new --package 'system-images;android-37.0;google_apis;x86_64' --device pixel_6
cat "$ANDROID_AVD_HOME/api37-new.ini"
"$ANDROID_HOME/emulator/emulator" -avd api37-new -port 5554 \
  -no-window -gpu swiftshader_indirect -noaudio -no-boot-anim \
  -no-snapshot -cores 4 -memory 4096 -verbose
```

今回の旧ツール側は SDK 配置前に AVD を生成したため、`image.sysdir.1` を SDK からの正しい相対パスへ揃えた。
両 AVD の `config.ini` のデータ領域は約 6 GB に揃え、起動した。
アニメーションを無効化してから、CI と同じ `bash .github/scripts/run-ui-tests.sh` を実行する。

## 実行結果

新 AVD は `run-ui-tests.sh` の初回で `gradle 終了コード=0 tests=24 failures=0 errors=0`。
起動後からテスト終了まで `hasReadColorBufferDma` の assertion は 0 件。
5 秒間隔の PID 観測は、停止前の 35 サンプル（171.5 秒）で SurfaceFlinger `518` / system_server `662` のまま。
target のみ修正した旧 AVD も `gradle 終了コード=0 tests=24 failures=0 errors=0`。
終了直前の SurfaceFlinger PID `493` / system_server PID `643` は起動時と同じだった。

最後の 1 サンプルの `error: closed` は、比較のため明示的に `adb emu kill` した後のもの。

## 週次通知と結果判定

`continue-on-error` によってジョブ全体は success になるが、通知条件は `steps.ui-tests.outcome == 'success'`。
#123 の実行では警告ステップが実行され、通知ステップは skipped。
したがってこの既知の偽の job success が、そのまま週次の成功通知になる構成ではない。
通知は schedule のみなので、手動実行では成功しても投稿されない。

`run-ui-tests.sh` を Gradle スタブと結果 XML で実行して、次を確認した。

| 結果 | スクリプト終了コード |
|---|---|
| 0 件、Gradle 成功（2 回試行） | 1 |
| 24 件失敗、Gradle 失敗 | 1 |
| 24 件成功、Gradle 成功 | 0 |
| XML は成功だが Gradle 失敗 | 1 |
| XML の errors が 1 | 1 |

既存 `notify-api37-green.test.sh` の 5 チェックも成功。実投稿は行っていない。
ただし現行判定は `tests > 0` と failures/errors/終了コードの確認であり、期待件数 24 件や skipped 件数までは検証しない。
今回の 0 件・24 件失敗は除外されるが、将来のテスト欠落を完全に保証する判定ではない。

## CI への対応と確認事項

1. エミュレータ起動前に `cmdline-tools;22.0` を導入し、Action が使う `cmdline-tools/latest` をそのバージョンへ向ける。
   PATH の追加だけでは Action が旧パスを前に戻すため不十分。
2. `avdmanager` の実際のバージョンと AVD `.ini` / `config.ini` を診断 artifact に保存する。
3. 修正した CI を複数回動かし、テスト 24 件成功と SurfaceFlinger / system_server のクラッシュ不在を確認する。
4. 安定性を確認してから API 37 の `continue-on-error` とローカル PowerShell の暫定運用を廃止する。

今回の変更では上記 1・2 をワークフローに組み込む。`check-avd-target.sh` は target 不一致・欠落を失敗にし、AVD の `.ini` と `config.ini` を artifact に保存する。
API 37 の非必須設定とローカル検証の暫定運用は維持する。必須化は CI 上での連続成功を確認してから行う。

## 参考資料

- [runner-images #14484](https://github.com/actions/runner-images/issues/14484): 同じ Major.Minor の解析問題の報告。仮説の手がかりに使い、上記ローカル比較で検証した。
- [runner-images #14676](https://github.com/actions/runner-images/pull/14676): Ubuntu/Windows 向けに Command-line Tools を 22.0 へ更新する上流 PR。マージとランナーイメージ展開がワークアラウンド解消の指標となる（#127）。
- [AOSP gfxstream RenderControl.cpp](https://android.googlesource.com/platform/hardware/google/gfxstream/+/refs/heads/main/host/RenderControl.cpp): read-color-buffer DMA の広告条件は GlDirectMem と HasSharedSlotsHostMemoryAllocator の両方。
- [AOSP emulator main.cpp](https://android.googlesource.com/platform/external/qemu/+/emu-master-dev/android-qemu2-glue/main.cpp): API レベルによる機能設定。今回の観測は実行バイナリの verbose ログでも確認した。

生ログ、AVD、SDK、Gradle キャッシュは `/tmp/costcalc-101/` に保存。
再起動後も参照できる証跡アーカイブは `build/api37-investigation/evidence-2026-10-16.tar.gz`（Git 管理外）。
SDK・JDK・大きい AVD ディスクはアーカイブに含めず、ログ・設定・CI artifact・結果 XML を保存した。
検証終了後、エミュレータは停止済み。
`/tmp` はこの環境では tmpfs のため、再起動すると消える。
