#!/usr/bin/env bash
# Publishes the current commit as a GitHub release with the signed APK attached.
#   scripts/release.sh            uses versionName from app/build.gradle.kts
# Bump versionName first (versionCode is derived from it), commit, then run this.
source "$(dirname "$0")/common.sh"
cd "$ROOT_DIR"

VERSION="$(sed -n 's/^ *versionName = "\(.*\)"/\1/p' app/build.gradle.kts)"
[[ "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || { echo "Cannot read versionName from app/build.gradle.kts" >&2; exit 1; }
TAG="v$VERSION"
[[ -f keystore.properties ]] || { echo "keystore.properties missing: releases must be signed with the release key (see README)" >&2; exit 1; }
[[ -z "$(git status --porcelain)" ]] || { echo "Working tree is dirty, commit first" >&2; exit 1; }
[[ "$(git rev-parse --abbrev-ref HEAD)" == main ]] || { echo "Release from main only" >&2; exit 1; }
git fetch -q --tags origin
[[ "$(git rev-parse HEAD)" == "$(git rev-parse origin/main)" ]] || { echo "Push main before releasing" >&2; exit 1; }
git rev-parse "$TAG" >/dev/null 2>&1 && { echo "Tag $TAG already exists, bump versionName" >&2; exit 1; }

log "Building $TAG"
./gradlew --quiet testDebugUnitTest assembleRelease
mkdir -p dist
APK="dist/tv-launcher-$TAG.apk"
cp app/build/outputs/apk/release/app-release.apk "$APK"

log "Publishing $TAG"
# gh creates and pushes the tag itself, so a failed upload leaves no orphan tag behind.
gh release create "$TAG" "$APK" --target "$(git rev-parse HEAD)" --title "tv-launcher $TAG" --generate-notes
git fetch -q --tags origin
