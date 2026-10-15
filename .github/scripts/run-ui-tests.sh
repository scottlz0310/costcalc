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

for attempt in $(seq 1 "$MAX_ATTEMPTS"); do
  rm -rf "$RESULTS_DIR"
  status=0
  # shellcheck disable=SC2086
  $GRADLE_CMD || status=$?

  tests="$(sum_attr tests)"
  failures="$(sum_attr failures)"
  errors="$(sum_attr errors)"
  echo "試行 ${attempt}/${MAX_ATTEMPTS}: gradle 終了コード=${status} tests=${tests} failures=${failures} errors=${errors}"

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
