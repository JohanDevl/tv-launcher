#!/usr/bin/env bash
# Restores the stock Google TV launcher, or pins tv-launcher to an older release.
#   scripts/rollback.sh                 stock launcher back as HOME (tv-launcher stays installed)
#   scripts/rollback.sh --uninstall     same, and uninstall tv-launcher
#   scripts/rollback.sh --version vX.Y.Z  reinstall that GitHub release of tv-launcher and keep it as HOME
source "$(dirname "$0")/common.sh"

ensure_connected
case "${1:-}" in
    --version)
        TAG="${2:?usage: rollback.sh --version vX.Y.Z}"
        DIR="$(mktemp -d)"
        trap 'rm -rf "$DIR"' EXIT
        log "Downloading release $TAG"
        gh release download "$TAG" --repo JohanDevl/tv-launcher --pattern '*.apk' --dir "$DIR"
        APK="$(ls "$DIR"/*.apk | head -1)"
        # install.sh handles downgrades (uninstall + install) and restores the stock
        # launcher if that fails, so the TV is never left without a HOME app.
        "$(dirname "$0")/install.sh" "$APK"
        "$(dirname "$0")/set-home.sh"
        exit 0
        ;;
    --uninstall | "") ;;
    *) echo "unknown option: $1" >&2; exit 2 ;;
esac

log "Restoring stock launcher $STOCK_ACTIVITY"
tv shell pm enable --user 0 "$STOCK_PKG" >/dev/null
set_home "$STOCK_ACTIVITY"
if [[ "${1:-}" == "--uninstall" ]]; then
    log "Uninstalling $PKG"
    tv uninstall "$PKG" >/dev/null || true
fi
tv shell input keyevent KEYCODE_HOME
log "HOME is now: $(current_home)"
