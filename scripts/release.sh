#!/usr/bin/env bash
# Publishes the current commit as a GitHub release with the signed APK attached.
#   scripts/release.sh            uses versionName from app/build.gradle.kts
# Bump versionName first (versionCode is derived from it), commit, then run this.
source "$(dirname "$0")/common.sh"
cd "$ROOT_DIR"

VERSION="$(sed -n 's/^ *versionName = "\(.*\)"/\1/p' app/build.gradle.kts)"
TAG="v$VERSION"
[[ -z "$(git status --porcelain)" ]] || { echo "Working tree is dirty, commit first" >&2; exit 1; }
git rev-parse "$TAG" >/dev/null 2>&1 && { echo "Tag $TAG already exists, bump versionName" >&2; exit 1; }

log "Building $TAG"
./gradlew --quiet testDebugUnitTest assembleRelease
mkdir -p dist
APK="dist/tv-launcher-$TAG.apk"
cp app/build/outputs/apk/release/app-release.apk "$APK"

log "Tagging and publishing $TAG"
git tag -a "$TAG" -m "tv-launcher $TAG"
git push -q origin "$TAG"
gh release create "$TAG" "$APK" --title "tv-launcher $TAG" --generate-notes
