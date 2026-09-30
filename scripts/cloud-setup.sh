#!/usr/bin/env bash
# Installs the Android SDK for Claude Code cloud sessions (Linux; JDK 21 is preinstalled there).
# Runs as SessionStart hook (.claude/settings.json) and can double as the cloud environment's
# setup script. No-op on local machines. Needs dl.google.com in the environment's allowed domains.
set -uo pipefail

[ "${CLAUDE_CODE_REMOTE:-}" = "true" ] || [ "${FORCE_ANDROID_SDK:-}" = "1" ] || exit 0

SDK="${ANDROID_HOME:-$HOME/android-sdk}"
TOOLS="$SDK/cmdline-tools/latest/bin/sdkmanager"
TOOLS_ZIP="https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip"

if [ ! -x "$TOOLS" ]; then
  echo "Installing Android command-line tools into $SDK …"
  tmp="$(mktemp -d)"
  if ! curl -fsSL -o "$tmp/tools.zip" "$TOOLS_ZIP"; then
    echo "WARN: could not download the Android SDK (is dl.google.com allowed?). Gradle builds won't work; CI still tests every push." >&2
    exit 0
  fi
  mkdir -p "$SDK/cmdline-tools"
  unzip -q "$tmp/tools.zip" -d "$tmp"
  rm -rf "$SDK/cmdline-tools/latest"
  mv "$tmp/cmdline-tools" "$SDK/cmdline-tools/latest"
fi

if [ ! -d "$SDK/platforms/android-36" ]; then
  yes | "$TOOLS" --sdk_root="$SDK" --licenses >/dev/null 2>&1 || true
  "$TOOLS" --sdk_root="$SDK" "platforms;android-36" "build-tools;36.0.0" "platform-tools" >/dev/null \
    || echo "WARN: sdkmanager failed" >&2
fi

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." 2>/dev/null && pwd || true)"
if [ -n "$repo_root" ] && [ -f "$repo_root/settings.gradle.kts" ]; then
  echo "sdk.dir=$SDK" > "$repo_root/local.properties"
fi
if [ -n "${CLAUDE_ENV_FILE:-}" ] && ! grep -qs "^export ANDROID_HOME=$SDK\$" "$CLAUDE_ENV_FILE"; then
  echo "export ANDROID_HOME=$SDK" >> "$CLAUDE_ENV_FILE"
fi
echo "Android SDK ready at $SDK"

# Warm up Gradle and Robolectric in the background: Robolectric downloads its Android jar on the
# first UI test, and that download once failed mid-session (false red). One smoke test here
# fetches it early without blocking the session start. Log: /tmp/robolectric-warmup.log
if [ -n "$repo_root" ] && [ -f "$repo_root/gradlew" ]; then
  (cd "$repo_root" && nohup bash ./gradlew :app:testGithubDebugUnitTest --tests '*AppSmokeTest' -q \
    >/tmp/robolectric-warmup.log 2>&1 &)
  echo "Robolectric warm-up started in the background (log: /tmp/robolectric-warmup.log)"
fi
exit 0
