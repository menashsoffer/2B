#!/usr/bin/env bash
# Runs inside the Android emulator step of .github/workflows/e2e.yml (docs/E2E.md).
# MAESTRO_TAGS: empty = every flow, "smoke" = only flows tagged smoke.
set -euo pipefail
adb install -r app/build/outputs/apk/e2e/app-e2e.apk
# A freshly booted CI emulator sometimes shows "Pixel Launcher isn't responding" over everything,
# which hides the app from Maestro. Hide system error dialogs; an app crash still fails the flow
# and its FATAL line still reaches the log below.
adb shell settings put global hide_error_dialogs 1 || true
adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS >/dev/null 2>&1 || true
adb logcat -c || true
adb logcat -v time > e2e-logcat.txt 2>&1 &
logcat_pid=$!
status=0
maestro test e2e/maestro \
  ${MAESTRO_TAGS:+--include-tags="${MAESTRO_TAGS}"} \
  --format junit --output e2e-report.xml \
  --test-output-dir e2e-output || status=$?
kill "${logcat_pid}" || true
if [ "${status}" -ne 0 ]; then
  # CI artifacts are not always reachable, so the failure state also goes to the log.
  echo "::group::Screen at failure (ids and texts)"
  maestro hierarchy 2>/dev/null | grep -oE '"(resource-id|text|accessibilityText)" *: *"[^"]+"' | sort | uniq | head -200 || true
  echo "::endgroup::"
  echo "::group::App log (errors and e2e)"
  grep -E "E2eActivity|AndroidRuntime|FATAL|PERMISSION_DENIED|permission|Firestore|ErrorReporter|reportError|Exception" e2e-logcat.txt | grep -v "GoogleApiManager\|Phenotype" | tail -200 || true
  echo "::endgroup::"
fi
exit "${status}"
