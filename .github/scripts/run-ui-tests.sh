#!/usr/bin/env bash
# エミュレータ上で UI テストを実行し、「テストが実際に走って全件成功した」ことを結果 XML で判定する。
#
# エミュレータは sys.boot_completed の後も system_server の初期化が続き、この間の APK インストールは
# SecurityException (GET_USAGE_STATS) で失敗する。このとき AGP は警告ログだけで BUILD SUCCESSFUL を返し、
# テストが 1 件も走らないまま CI が緑になる。そのため Gradle の終了コードだけを信用せず、
# 結果 XML の有無・件数で判定し、実行されなかった場合は待ってから再試行する。
set -euo pipefail

RESULTS_DIR="${RESULTS_DIR:-app/build/outputs/androidTest-results/connected}"
GRADLE_CMD="${GRADLE_CMD:-./gradlew :app:connectedDebugAndroidTest --no-daemon}"
MAX_ATTEMPTS="${MAX_ATTEMPTS:-3}"
RETRY_WAIT_SEC="${RETRY_WAIT_SEC:-30}"

# 結果 XML から指定属性（tests / failures / errors）の合計を返す。XML が無ければ 0。
sum_attr() {
  local attr="$1" total=0 value
  while IFS= read -r file; do
    value="$(grep -o -m1 " ${attr}=\"[0-9]*\"" "$file" | head -n1 | grep -o '[0-9]*' || true)"
    total=$((total + ${value:-0}))
  done < <(find "$RESULTS_DIR" -name 'TEST-*.xml' 2>/dev/null)
  echo "$total"
}

# system_server が安定するまで待つ。boot_completed 直後は package サービスが一時的に消えることがある。
# 連続して応答した場合のみ準備完了とみなす（起動・クラッシュ・再起動の途中を拾わないため）。
wait_for_system_ready() {
  local ok=0 i
  for i in $(seq 1 90); do
    if adb shell pm path android >/dev/null 2>&1 && adb shell cmd activity get-current-user >/dev/null 2>&1; then
      ok=$((ok + 1))
    else
      ok=0
    fi
    [ "$ok" -ge 5 ] && return 0
    sleep 2
  done
  echo "::warning::エミュレータの system_server が 3 分以内に安定しなかった"
  return 1
}

# --- 証跡 ---
# 失敗の原因（system_server のクラッシュ・容量・メモリなど）を後から追えるよう、判断材料をファイルで残す。
# DIAG_DIR は workflow が artifact としてアップロードする。診断自体の失敗は本処理に影響させない。
DIAG_DIR="${DIAG_DIR:-ui-test-diagnostics}"
LOGCAT_PID=""

has_adb() { command -v adb >/dev/null 2>&1; }

# 起動から終了までの logcat（全バッファ）を連続保存する。system_server が再起動しても logd は残るため、
# クラッシュ直前直後のログを取りこぼさない。
start_live_logcat() {
  has_adb || return 0
  mkdir -p "$DIAG_DIR"
  adb logcat -b all -v threadtime >"$DIAG_DIR/logcat-live.txt" 2>&1 &
  LOGCAT_PID=$!
}

# その時点のエミュレータ状態を試行単位のディレクトリへ保存する。
snapshot_diagnostics() {
  has_adb || return 0
  local tag="$1" dir="$DIAG_DIR/$1"
  mkdir -p "$dir"
  {
    echo "== devices";           adb devices -l
    echo "== fingerprint";       adb shell getprop ro.build.fingerprint
    echo "== boot_completed";    adb shell getprop sys.boot_completed
    echo "== uptime";            adb shell uptime
    echo "== system_server pid"; adb shell pidof system_server
    echo "== df";                adb shell df
    echo "== meminfo";           adb shell cat /proc/meminfo
    echo "== loadavg";           adb shell cat /proc/loadavg
    echo "== dmesg";             adb shell dmesg
    echo "== packages";          adb shell pm list packages io.github.scottlz0310
  } >"$dir/state.txt" 2>&1 || true
  # クラッシュ・watchdog・ANR の記録（system_server の再起動原因はここに残る）。
  adb shell dumpsys dropbox --print >"$dir/dropbox.txt" 2>&1 || true
  adb logcat -d -b crash -v threadtime >"$dir/logcat-crash.txt" 2>&1 || true
  adb logcat -d -b all -v threadtime >"$dir/logcat-all.txt" 2>&1 || true
  # 試行ごとに結果を消すため、XML と AGP のレポートを退避する。
  cp -r "$RESULTS_DIR" "$dir/results" 2>/dev/null || true
  cp -r app/build/reports/androidTests "$dir/reports" 2>/dev/null || true
  # adb root は adbd を再起動して連続 logcat を切るため使わない（tombstone の要約は dropbox に残る）。
  # CI ログ側には要点だけ出す（全量は artifact を参照）。
  echo "::group::エミュレータ診断 (${tag})"
  grep -E "^== (uptime|system_server|df)|^/dev|^[0-9]+$| up " "$dir/state.txt" | head -n 20 || true
  grep -i -E "system_server|Watchdog|FATAL|has died|lowmemory|lmkd|Out of memory" "$dir/logcat-all.txt" | tail -n 40 || true
  echo "::endgroup::"
}

finish() {
  if [ -n "$LOGCAT_PID" ]; then kill "$LOGCAT_PID" 2>/dev/null || true; fi
}
trap finish EXIT

start_live_logcat

for attempt in $(seq 1 "$MAX_ATTEMPTS"); do
  rm -rf "$RESULTS_DIR"
  # スタブ実行（adb 無し）ではスキップできるよう、adb が使える場合のみ待つ。
  if command -v adb >/dev/null 2>&1; then wait_for_system_ready || true; fi
  status=0
  # shellcheck disable=SC2086
  $GRADLE_CMD || status=$?

  tests="$(sum_attr tests)"
  failures="$(sum_attr failures)"
  errors="$(sum_attr errors)"
  echo "試行 ${attempt}/${MAX_ATTEMPTS}: gradle 終了コード=${status} tests=${tests} failures=${failures} errors=${errors}"

  snapshot_diagnostics "attempt-${attempt}"
  if [ "$tests" -gt 0 ]; then
    if [ "$status" -ne 0 ] || [ "$failures" -gt 0 ] || [ "$errors" -gt 0 ]; then
      echo "::error::UI テストが失敗した (failures=${failures} errors=${errors} gradle=${status})"
      exit 1
    fi
    exit 0
  fi

  echo "::warning::UI テストが 1 件も実行されなかった（試行 ${attempt}）。エミュレータの初期化待ちの可能性があるため再試行する"
  [ "$attempt" -lt "$MAX_ATTEMPTS" ] && sleep "$RETRY_WAIT_SEC"
done

echo "::error::UI テストが ${MAX_ATTEMPTS} 回試行しても 1 件も実行されなかった"
exit 1
