#!/usr/bin/env bash
# Device test on a running emulator (CI: pipeline.yml → device-test).
#   1. Upgrade: install the previous release, use it (monkey), install the new build over it.
#   2. Fresh install of the new build.
# After each install the app must start and survive 500 random taps. Any crash fails the build.
# Usage: device-smoke.sh <new.apk> [previous-release.apk]
set -euo pipefail

NEW_APK="$1"
PREV_APK="${2:-}"
PKG="dev.suspension.app"
ACTIVITY="$PKG/.MainActivity"

fail() {
  echo "::error::$1"
  adb logcat -d -b crash | tail -n 80 || true
  exit 1
}

assert_no_crash() {
  if adb logcat -d -b crash | grep -q "$PKG"; then fail "App crashed ($1)"; fi
}

launch_and_check() {
  adb shell am start -W -n "$ACTIVITY" >/dev/null
  sleep 6
  adb shell pidof "$PKG" >/dev/null || fail "App not running after start ($1)"
  assert_no_crash "$1"
}

# Random taps/swipes inside the app only (no back/home/app switching).
monkey() {
  adb shell monkey -p "$PKG" -s "$1" --throttle 120 --pct-syskeys 0 --pct-appswitch 0 --pct-anyevent 0 "$2" >/dev/null
}

adb uninstall "$PKG" >/dev/null 2>&1 || true

if [ -n "$PREV_APK" ] && [ -f "$PREV_APK" ]; then
  echo "== Upgrade: $(basename "$PREV_APK") -> $(basename "$NEW_APK")"
  adb install "$PREV_APK" >/dev/null
  adb shell am start -W -n "$ACTIVITY" >/dev/null
  sleep 5
  monkey 7 300 || echo "(old release had trouble under monkey — not this build's fault)"
  adb shell am force-stop "$PKG"
  adb logcat -b all -c
  adb install -r "$NEW_APK" >/dev/null || fail "New build does not install over the previous release (signature or version code)"
  launch_and_check "after upgrade"
  monkey 42 500 || fail "Crash under random use after upgrade"
  assert_no_crash "random use after upgrade"
else
  echo "== No previous release — skipping the upgrade test"
fi

echo "== Fresh install"
adb uninstall "$PKG" >/dev/null
adb logcat -b all -c
adb install "$NEW_APK" >/dev/null || fail "Fresh install failed"
launch_and_check "fresh install"
monkey 1234 500 || fail "Crash under random use on a fresh install"
assert_no_crash "random use on a fresh install"

echo "Device test passed."
