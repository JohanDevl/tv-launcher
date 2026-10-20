#!/usr/bin/env bash
# Saves a PNG screenshot of the TV. Usage: scripts/screenshot.sh [output.png]
source "$(dirname "$0")/common.sh"

ensure_connected
OUT="${1:-$ROOT_DIR/captures/$(date +%Y%m%d-%H%M%S).png}"
mkdir -p "$(dirname "$OUT")"
tv exec-out screencap -p >"$OUT"
log "Saved $OUT"
