#!/usr/bin/env bash
# Shared helpers for the device scripts. Source it, do not run it.
set -euo pipefail

TV_ADDR="${TV_ADDR:-192.168.0.240:5555}"
PKG="dev.johan.tvlauncher"
HOME_ACTIVITY="$PKG/.MainActivity"
STOCK_PKG="com.google.android.tvlauncher"
STOCK_ACTIVITY="$STOCK_PKG/.MainActivity"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if [[ -z "${JAVA_HOME:-}" ]] && [[ -x /usr/libexec/java_home ]]; then
    JAVA_HOME="$(/usr/libexec/java_home -v 21 2>/dev/null || true)"
    export JAVA_HOME
fi

log() { printf '\033[1;33m==>\033[0m %s\n' "$*"; }

tv() { adb -s "$TV_ADDR" "$@"; }

ensure_connected() {
    if ! adb devices | grep -q "^$TV_ADDR[[:space:]]*device"; then
        log "Connecting to $TV_ADDR"
        adb connect "$TV_ADDR" >/dev/null
        sleep 1
    fi
    adb devices | grep -q "^$TV_ADDR[[:space:]]*device" || {
        echo "TV $TV_ADDR is not reachable over ADB (accept the prompt on the TV?)" >&2
        exit 1
    }
}

current_home() {
    tv shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -1
}

# Makes $1 (package/.Activity) the HOME app, both as preferred activity and role holder.
set_home() {
    local activity="$1"
    tv shell cmd package set-home-activity "$activity" >/dev/null
    tv shell cmd role add-role-holder android.app.role.HOME "${activity%%/*}" >/dev/null 2>&1 || true
}
