#!/usr/bin/env bash
# Makes tv-launcher the HOME app and opens it. Undo with scripts/rollback.sh.
source "$(dirname "$0")/common.sh"

ensure_connected
tv shell pm path "$PKG" >/dev/null || { echo "$PKG is not installed, run scripts/install.sh first" >&2; exit 1; }
log "Setting $HOME_ACTIVITY as HOME"
set_home "$HOME_ACTIVITY"
# The stock launcher is a priority-2 system HOME, so Android always picks it over a
# preferred third-party HOME. Disabling it (reversible, see rollback.sh) is the only way.
log "Disabling $STOCK_PKG (re-enabled by scripts/rollback.sh)"
tv shell pm disable-user --user 0 "$STOCK_PKG" >/dev/null
tv shell input keyevent KEYCODE_HOME
log "HOME is now: $(current_home)"
