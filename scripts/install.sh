#!/usr/bin/env bash
# Builds the release APK (unless an APK path is given) and installs it on the TV.
#   scripts/install.sh               build + install current sources
#   scripts/install.sh path/to.apk   install a specific APK (e.g. a downloaded release)
source "$(dirname "$0")/common.sh"

ensure_connected
APK="${1:-}"
if [[ -z "$APK" ]]; then
    log "Building release APK"
    (cd "$ROOT_DIR" && ./gradlew --quiet assembleRelease)
    APK="$ROOT_DIR/app/build/outputs/apk/release/app-release.apk"
fi
[[ -f "$APK" ]] || { echo "APK not found: $APK" >&2; exit 1; }

log "Installing $APK"
if ! OUTPUT="$(tv install -r "$APK" 2>&1)"; then
    echo "$OUTPUT" >&2
    # Only a downgrade or a signing key change justifies wiping the installed app;
    # any other failure leaves it untouched.
    if [[ "$OUTPUT" != *INSTALL_FAILED_VERSION_DOWNGRADE* && "$OUTPUT" != *INSTALL_FAILED_UPDATE_INCOMPATIBLE* ]]; then
        exit 1
    fi
    WAS_HOME=false
    [[ "$(current_home)" == "$HOME_ACTIVITY" ]] && WAS_HOME=true
    # Uninstalling the only enabled HOME app: bring the stock launcher back if anything fails.
    trap 'log "Install failed, restoring the stock launcher"; tv shell pm enable --user 0 "$STOCK_PKG" >/dev/null; set_home "$STOCK_ACTIVITY"' ERR
    log "Older version or different signing key: reinstalling from scratch (favorites will reset)"
    tv uninstall "$PKG" >/dev/null
    tv install "$APK" >/dev/null
    trap - ERR
    # Uninstalling cleared the preferred HOME activity.
    if [[ "$WAS_HOME" == true ]]; then set_home "$HOME_ACTIVITY"; fi
fi

# Sideloaded apps start interpreted/JIT; compile ahead of time for a fast first launch.
log "Compiling ahead of time"
tv shell cmd package compile -m speed -f "$PKG" >/dev/null
log "Installed $(tv shell dumpsys package "$PKG" | grep -m1 versionName | xargs)"
