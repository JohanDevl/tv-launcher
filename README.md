# tv-launcher

A custom home screen (launcher) for Android TV: dark, quiet and fast, built around the apps'
own artwork and the remote's D-pad. Developed for a Philips Android TV 12 set, it works on any
Android TV 9+ device.

![Home](docs/screenshots/home.jpg)

| Apps & inputs | Long-press menu | Reordering favorites |
|---|---|---|
| ![Apps](docs/screenshots/apps.jpg) | ![Menu](docs/screenshots/menu.jpg) | ![Move](docs/screenshots/move.jpg) |

## Contents

- [Features](#features)
- [Reference device](#reference-device)
- [Setting up a development machine](#setting-up-a-development-machine)
- [Everyday workflow](#everyday-workflow)
- [Rollback](#rollback)
- [Releases](#releases)
- [Signing key](#signing-key)
- [Architecture](#architecture)
- [Common changes](#common-changes)
- [Updating dependencies](#updating-dependencies)
- [Troubleshooting](#troubleshooting)
- [License](#license)

## Features

- **Favorites row**: large banners, pre-filled on first run with the streaming apps that are installed.
- **Inputs row**: HDMI devices that are actually plugged in, listed by their HDMI-CEC name (e.g. "Apple TV"), opened in one click.
- **All apps** grid, alphabetical, updated live when an app is installed, updated or removed.
- **Quick actions** in the header: Live TV, Sources, Settings (one press up from the favorites).
- **Long-press any app**: add to / remove from favorites, move (◀ ▶ then OK), hide, app info, uninstall.
  Hidden apps stay reachable from the last tile of the grid.
- **Ambient background** tinted by the focused app's artwork.
- Back and Home always return to the first favorite: the launcher can never be "exited".
- English and French UI (follows the TV language).

## Reference device

| | |
|---|---|
| Model | Philips 58PUS7304/12 |
| OS | Android TV 12 (build `STT2.230526.001`) |
| SoC / RAM | MediaTek MT5887 (armv7), 2 GB |
| UI | 1920x1080 @ 320 dpi (960x540 dp) |
| Access | ADB over the local network |

Measured on it (v0.1.x, one run each): cold start ~1.0 s (`am start -W`), ~56 MB PSS, 2 MB APK.

## Setting up a development machine

Instructions are for macOS with Homebrew; adapt package names on Linux.

### 1. Toolchain

```bash
brew install --cask temurin@21 android-commandlinetools
yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-37.0" "build-tools;36.0.0"
```

- Use **JDK 21**. Newer JDKs (e.g. brew's `openjdk` 27) are not supported by Gradle/AGP yet.
  The scripts pick JDK 21 automatically through `/usr/libexec/java_home -v 21`; for raw Gradle
  commands, `export JAVA_HOME=$(/usr/libexec/java_home -v 21)`.
- Point Gradle at the SDK with `local.properties` (`sdk.dir=/path/to/sdk`) or `ANDROID_HOME`.
  Missing SDK packages are downloaded by AGP on the first build once licenses are accepted.
- Install `gh` (GitHub CLI) for releases: `brew install gh && gh auth login`.

### 2. Enable ADB on the TV

1. Settings → *Android settings* → *Device Preferences* → *About* → press **Build** seven times.
2. Settings → *Device Preferences* → *Developer options* → enable **USB debugging**.
   On the reference TV this also opens ADB over the network on port 5555.
3. Find the TV's IP address (Settings → Network) and connect:

```bash
adb connect <tv-ip>:5555        # accept the prompt on the TV ("always allow")
export TV_ADDR=<tv-ip>:5555     # scripts default to 192.168.0.240:5555
```

### 3. Signing key

Release builds read `keystore.properties` at the repository root (git-ignored).
Restore it and the keystore from your backup, or create a new key: see [Signing key](#signing-key).
Without it, release builds fall back to the debug key, which is enough to try the app.

### 4. First install

```bash
scripts/install.sh      # build, install and AOT-compile on the TV
scripts/set-home.sh     # make tv-launcher the HOME app (Home button)
```

## Everyday workflow

| Task | Command |
|---|---|
| Build + install on the TV | `scripts/install.sh` |
| Install a given APK | `scripts/install.sh path/to/app.apk` |
| Make it the HOME app | `scripts/set-home.sh` |
| Screenshot of the TV | `scripts/screenshot.sh [out.png]` (default: `captures/`) |
| Unit tests | `./gradlew testDebugUnitTest` |
| Live logs | `adb logcat --pid=$(adb shell pidof dev.johan.tvlauncher)` |
| Crash after launch | `adb logcat -b crash` |
| Simulate the remote | `adb shell input keyevent KEYCODE_DPAD_DOWN` (`_CENTER`, `KEYCODE_HOME`, `KEYCODE_BACK`, `--longpress KEYCODE_DPAD_CENTER`) |
| Cold start time | `adb shell am force-stop dev.johan.tvlauncher && adb shell am start -W -n dev.johan.tvlauncher/.MainActivity` |
| Memory | `adb shell dumpsys meminfo dev.johan.tvlauncher` |

The install script runs `cmd package compile -m speed` after each install: sideloaded apps
otherwise start interpreted, and baseline profiles cannot be generated on Android 12 without root.

## Rollback

Nothing on the TV is replaced or uninstalled: tv-launcher is a separate app and the stock
launcher is only *disabled*, which is fully reversible.

| Goal | Command |
|---|---|
| Back to the stock Google TV launcher (keep tv-launcher installed) | `scripts/rollback.sh` |
| Back to stock and remove tv-launcher | `scripts/rollback.sh --uninstall` |
| Go back to an older tv-launcher release | `scripts/rollback.sh --version v0.2.0` |

Installing an older version, or one signed with another key, requires an uninstall first:
`install.sh` does it automatically and favorites and hidden apps reset in that case. If the
reinstall fails, the script re-enables the stock launcher so the TV is never left without a
home screen.

**Without a computer**: Settings → Apps → *See all apps* → *Show system apps* →
**Android TV Home** (*Accueil d'Android TV* in French) → *Enable*, then press Home.

### Why the stock launcher is disabled

The stock launcher is a system app whose HOME intent filter has priority 2. Android always
resolves HOME to a strictly higher-priority match before looking at the user's preferred
app, so `cmd package set-home-activity` alone has no effect. `scripts/set-home.sh` therefore
runs `pm disable-user --user 0 com.google.android.tvlauncher`, and `scripts/rollback.sh`
runs `pm enable` to undo it. Both survive reboots.

## Releases

Every release is a git tag plus a GitHub release with the signed APK attached, so any
version can be reinstalled later with `scripts/rollback.sh --version vX.Y.Z`.

1. Bump `versionName` in `app/build.gradle.kts` (`MAJOR.MINOR.PATCH`, minor/patch < 100).
   `versionCode` is derived from it.
2. Commit and push `main`.
3. `scripts/release.sh`: checks the tree is clean and pushed, runs the tests, builds the
   signed APK into `dist/` and publishes the GitHub release (which creates the tag).
4. `scripts/install.sh dist/tv-launcher-vX.Y.Z.apk` to put it on the TV.

## Signing key

Android only installs an update over an app signed with the same key, so all releases must be
signed with one key that never changes.

- Location: `~/.config/tv-launcher/release.jks` and `keystore.properties` (also copied there).
  Copy `keystore.properties` to the repository root on each development machine.
- **Back both files up** (password manager or encrypted storage). If they are lost, the next
  release must be installed with an uninstall first (favorites reset), and older releases can
  no longer be installed over new ones.
- Never commit them: `.gitignore` excludes `keystore.properties`, `*.jks` and `*.keystore`.

`keystore.properties` format:

```properties
storeFile=/absolute/path/to/release.jks
storePassword=...
keyAlias=tvlauncher
keyPassword=...
```

Create a new key (only when starting from scratch):

```bash
keytool -genkeypair -keystore ~/.config/tv-launcher/release.jks -storetype PKCS12 \
  -alias tvlauncher -keyalg RSA -keysize 4096 -validity 36500 -dname "CN=tv-launcher"
```

Releases up to v0.1.1 were signed with an earlier key that is retired (its file is still in the
git history and must not be trusted). Installing those versions goes through the uninstall
path described in [Rollback](#rollback).

## Architecture

Single-activity Kotlin app, Jetpack Compose for TV (`androidx.tv:tv-material`), coroutines +
Flow, DataStore. No DI framework: `LauncherApp` holds the few singletons.
AGP 9.4, Gradle 9.8, minSdk 28, R8 enabled.

```
app/src/main/kotlin/dev/johan/tvlauncher/
├── MainActivity.kt          HOME + launcher entry point; Home presses reset the focus
├── LauncherApp.kt           dependency container, trims the artwork cache in background
├── data/
│   ├── AppRepository.kt     leanback apps, re-queried (debounced, off main thread) on package broadcasts
│   ├── InputRepository.kt   connected HDMI inputs via TvInputManager, CEC device names
│   ├── SettingsStore.kt     DataStore: ordered favorites, hidden apps, first-run flag
│   ├── HomeLogic.kt         pure logic (favorites ops, home state), unit tested
│   └── Models.kt
├── image/ArtworkCache.kt    banners rasterised at card size + accent color, LRU bounded in bytes
└── ui/
    ├── theme/Theme.kt       design tokens (Palette), Manrope typography
    ├── components/          AppCard, ActionTile, QuickAction, Header, menus, AmbientBackground
    └── home/                HomeScreen (layout, focus, move mode), HomeViewModel, Actions (intents)
scripts/                     install / set-home / rollback / screenshot / release (+ common.sh)
```

Data flow: `AppRepository`, `SettingsStore` and `InputRepository` are combined in
`HomeViewModel` through `HomeLogic.buildState` into one `HomeState`, collected by `HomeScreen`.
User actions go back through the view model into `SettingsStore`.

Design decisions worth knowing before changing things:

- **Low-end hardware first**: 2 GB RAM and a weak GPU. No blur, no offscreen layers; the
  ambient color is animated in the draw phase so focus changes never recompose the screen.
  Artwork is rasterised once at card size (384x216) and the cache is capped at 24 MB, trimmed
  when another app is in front.
- **Focus**: the first favorite (or first app when there are none) is the entry point.
  Back, Home and any action that removes the focused card bring focus back to it.
- **Inputs**: only `TYPE_HDMI` inputs in state `CONNECTED` are shown (Philips reports empty
  ports as `CONNECTED_STANDBY`; analog inputs cannot detect a connection). A CEC device hides
  its parent port.
- **Philips integration**: Live TV opens `org.droidtv.playtv`, Sources fires
  `com.android.tv.action.VIEW_INPUTS` (Philips sources drawer). Both fail soft with a toast.

## Common changes

| Change | Where |
|---|---|
| Default favorites on first run | `HomeLogic.DEFAULT_FAVORITES` (package names) |
| Colors, typography | `ui/theme/Theme.kt` (`Palette`, `typography`) |
| Card sizes, spacing, apps per row | constants at the top of `ui/home/HomeScreen.kt` |
| Header shortcuts | `Header(...) { QuickAction(...) }` in `HomeScreen.kt`, intents in `Actions.kt` |
| Long-press menu entries | `AppMenu` in `HomeScreen.kt` |
| UI text | `res/values/strings.xml` (English) and `res/values-fr/strings.xml` (French) |
| Icons | `res/drawable/ic_*.xml` (Material icon paths, 24dp) |

Check on the TV after any UI change: `scripts/install.sh && scripts/screenshot.sh`.
Keep D-pad behavior in mind: every new focusable must be reachable with arrows only.

## Updating dependencies

Versions live in `gradle/libs.versions.toml`; the Gradle wrapper in
`gradle/wrapper/gradle-wrapper.properties`.

- Bump one group at a time (AGP + Gradle, Kotlin, Compose BOM, AndroidX), then run
  `./gradlew testDebugUnitTest assembleRelease` and check the app on the TV.
- Recent AndroidX releases may require a higher `compileSdk`: the build error says which.
  `compileSdk` can go up freely; keep `minSdk` 28 and only raise `targetSdk` after checking
  behavior changes.
- R8 is enabled: a crash only in release builds usually means a missing keep rule in
  `app/proguard-rules.pro`.

## Troubleshooting

| Symptom | Fix |
|---|---|
| `TV ... is not reachable over ADB` | TV on and on the same network? `adb connect <ip>:5555`, accept the prompt on the TV. After a TV IP change, set `TV_ADDR`. |
| `unauthorized` in `adb devices` | Accept the RSA prompt on the TV; if it does not show, toggle USB debugging off and on. |
| Home opens the Google launcher again | A firmware update may have re-enabled it: `scripts/set-home.sh`. |
| Blank home screen or the launcher crash-loops | `scripts/rollback.sh` (or the no-computer path above), then `adb logcat -b crash`. |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | APK signed with another key: `install.sh` reinstalls automatically; check `keystore.properties`. |
| An app is missing from the grid | Only apps with a TV (leanback) launcher entry are listed; check it is not in *Hidden apps*. |
| An input shows a raw name (`Playback_2`) | Right after boot, HDMI-CEC names arrive a bit later; inputs are re-read each time the home screen is shown. |
| `release.sh`: `keystore.properties missing` | Copy `keystore.properties` to the repository root (see [Signing key](#signing-key)). |

## License

Copyright © 2026 JohanDevl.

tv-launcher is licensed under [Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International](LICENSE) (CC BY-NC-SA 4.0):

- You may use, study, modify and share it, with credit to the author.
- **No commercial use**: you may not sell it, ship it in a commercial product, or use it for a commercial service.
- **ShareAlike**: modified versions must be published under the same license.

For any use outside these terms (commercial licensing included), open an issue to ask.

Third-party components keep their own licenses: the Manrope typeface (Mikhail Sharanda) is
under the SIL Open Font License 1.1, and the AndroidX / Jetpack Compose / Kotlin libraries
are under the Apache License 2.0.
