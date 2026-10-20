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

log "Installing $APK"
if ! tv install -r "$APK"; then
    log "In-place install refused (downgrade?). Reinstalling from scratch; favorites will reset."
    tv uninstall "$PKG" >/dev/null || true
    tv install "$APK"
fi
# Sideloaded apps start interpreted/JIT; compile ahead of time for a fast first launch.
log "Compiling ahead of time"
tv shell cmd package compile -m speed -f "$PKG" >/dev/null
log "Installed $(tv shell dumpsys package "$PKG" | grep -m1 versionName | xargs)"
