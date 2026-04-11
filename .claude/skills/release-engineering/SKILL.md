---
name: release-engineering
description: Release engineering tasks including version bumping, building release APKs, creating git tags, writing changelogs, and preparing F-Droid releases. Use when the user asks to prepare a release, bump version, tag a release, or build for distribution.
argument-hint: task description
---

# Release Engineering

You are performing release engineering tasks for **MakeMeDown Text Reader**.

## Current state of the project

Read this before assuming anything. The repo is young and several pieces of release infrastructure that exist in the user's other projects do **not** yet exist here:

- **No tags yet.** This will be the first release; the tag convention below is a proposal — confirm with the user before committing to it.
- **No `bumpPatchVersion` Gradle task.** Version bumps are done by editing [app/build.gradle.kts](app/build.gradle.kts) directly.
- **No `fastlane/` directory.** F-Droid metadata does not exist yet. If the user asks to "prepare an F-Droid release," step zero is to confirm whether they want to create that directory tree at all.
- **No release signing config.** `release` build type has `isMinifyEnabled = false` and no `signingConfig` declared. Signed builds happen in Android Studio (Build → Generate Signed App Bundle / APK) or via a user-provided keystore — never automate this.
- **`applicationId`**: `org.ghostsinthelab.app.makedown` (note: `app`, not `apps`).
- **`namespace`**: `org.ghostsinthelab.app.makedown`.

Current version (verify in [app/build.gradle.kts](app/build.gradle.kts) before acting — these go stale):

```kotlin
versionCode = 1
versionName = "1.0"
```

## Version Scheme

- **versionName**: SemVer `MAJOR.MINOR.PATCH` (e.g. `1.0.0`, `1.0.1`, `1.1.0`).
- **versionCode**: monotonically increasing integer, +1 per release.
- Both live in [app/build.gradle.kts](app/build.gradle.kts) inside `defaultConfig { … }`.

To bump:

1. Edit `versionCode` (+1) and `versionName` in [app/build.gradle.kts](app/build.gradle.kts).
2. Sync Gradle (or just rebuild) to confirm the file still parses.
3. Commit the bump as part of the release commit (see below) — do not split the bump and the release into two commits unless the user wants it that way.

## Release Process

The general shape, in order:

1. **Confirm intent** — ask the user what version they're cutting and whether it's a patch, minor, or major bump. Don't guess.
2. **Ensure the working tree is clean** on `main` — `git status` must be empty before bumping.
3. **Run tests** — `./gradlew :app:test` at minimum. If the user has connected a device/emulator, `./gradlew :app:connectedAndroidTest` is a bonus. Stop and report if anything fails.
4. **Bump version** in [app/build.gradle.kts](app/build.gradle.kts) (versionCode +1, versionName per the user's choice).
5. **Write changelog entries** if the user maintains them — see "Changelogs" below. As of this writing, no changelog convention is in place yet, so confirm with the user what they want.
6. **Build signed APKs** — **do not run this automatically.** The release build needs a keystore passphrase. Prompt the user to build via Android Studio (Build → Generate Signed App Bundle / APK) or via the command line with their own keystore. Wait for confirmation before proceeding.
7. **Create the release commit** — see "Git Conventions" below.
8. **Tag the release** — see "Tag Convention" below.
9. **Push commit and tag** only after the user explicitly confirms.
10. **Create the GitHub Release** with `gh` if the user wants one (see "GitHub Release" below).

### Git Conventions

- Release commit message: `chore: release <versionName>` (matches the project's existing `chore:` convention from `git log`). If the user prefers a non-prefixed `Release X.Y.Z`, follow their lead — but ask, don't assume.
- Commits are GPG-signed (`git commit -S`). See the `commit-and-push` skill for the full commit workflow. Do **not** add `--signoff`; this project uses GPG signatures, not DCO sign-off.
- Tag the release commit (see next section).

### Tag Convention

The repo currently has zero tags, so there is no precedent. **Propose** the following and let the user confirm:

- Lightweight tag: `git tag <versionName>` (e.g. `1.0.0`)
- No `v` prefix (this matches the user's other projects)
- GPG-signed annotated tag is also reasonable: `git tag -s <versionName> -m "<versionName>"` — ask which the user prefers.

Once the user picks a convention, stick with it for future releases.

## Build Commands Reference

```bash
./gradlew :app:assembleDebug         # Debug build → app/build/outputs/apk/debug/
./gradlew :app:assembleRelease       # Release build (currently UNSIGNED, isMinifyEnabled=false)
./gradlew :app:test                  # Unit tests (JVM)
./gradlew :app:connectedAndroidTest  # Instrumented tests (needs device/emulator)
./gradlew :app:lint                  # Android lint
./gradlew :app:clean                 # Clean build artifacts
```

The debug APK lands at `app/build/outputs/apk/debug/app-debug.apk`. The release APK (when configured) lands at `app/build/outputs/apk/release/`.

Note: because no `archivesName` is configured, the APK filename is the default `app-<buildType>.apk`. If the user wants a self-describing filename (e.g. `org.ghostsinthelab.app.makedown_v1.0.0-release.apk`), they should add `archivesName` under `defaultConfig` in [app/build.gradle.kts](app/build.gradle.kts) before the release.

## GPG Signing the APK

Independent of the release build's keystore signing (which is the Android signing config), some workflows GPG-sign the APK file itself for distribution verification. **Do not run `gpg` automatically** — passphrase entry is interactive.

If the user wants this, provide the commands to run manually:

```bash
gpg --detach-sign --armor app/build/outputs/apk/release/app-release.apk
gpg --detach-sign --armor app/build/outputs/apk/debug/app-debug.apk
```

This produces `.asc` signature files alongside the APKs. Wait for the user to confirm signing is complete before moving on.

## Changelogs

As of this writing, the project has **no changelog file and no `fastlane/` directory**. Don't fabricate one. When the user asks for a changelog:

1. Ask whether they want a `CHANGELOG.md` at the repo root (Keep-a-Changelog style is common), an F-Droid `fastlane/metadata/.../changelogs/<versionCode>.txt`, GitHub Release notes only, or some combination.
2. Once they choose, stick with it for future releases.

To gather material for a changelog, list commits since the previous tag:

```bash
git log --oneline <previous-tag>..HEAD     # if a previous tag exists
git log --oneline                          # for the very first release
```

Group entries by type (`feat:`, `fix:`, `docs:`, `chore:`, etc.) since the project already uses Conventional Commit prefixes — that maps cleanly onto changelog sections.

### F-Droid (only if/when set up)

If and when the user adds an F-Droid metadata tree, it would conventionally live at:

```
fastlane/metadata/android/en-US/
  title.txt              # max 50 chars
  short_description.txt  # max 80 chars
  full_description.txt   # max 4000 chars
  changelogs/<versionCode>.txt   # max 500 chars
  images/                # icon, feature graphic, screenshots
```

If the user is multilingual, mirror under `zh-TW/`. Don't pre-create this tree without being asked.

## GitHub Release

After building (and optionally GPG-signing) the APKs, create a GitHub Release with `gh`:

```bash
gh release create <versionName> \
  --title "<versionName>" \
  --notes "$(cat <<'EOF'
<release notes body>
EOF
)" \
  app/build/outputs/apk/release/app-release.apk \
  app/build/outputs/apk/release/app-release.apk.asc
```

For release notes, prefer a hand-written summary over `--generate-notes` alone. A good structure:

```markdown
## Highlights

- (1–3 user-visible bullets — what changed for the reader)

## Changes

- (grouped by feat: / fix: / docs: / chore: from `git log`)

**Full Changelog**: https://github.com/hiroshiyui/MakeMeDownTextReader/compare/<previous-tag>...<versionName>
```

For the **first** release, omit the `compare` link (there's nothing to compare against) and use `https://github.com/hiroshiyui/MakeMeDownTextReader/commits/<versionName>` instead.

Upload whichever assets the user wants — typically the release APK plus its `.asc` signature. Debug APKs are usually not attached to releases unless the user says so.

## Important Reminders

- **Confirm before any push, tag-push, or release publish.** All three are visible to others and hard to undo.
- **Never automate keystore signing or `gpg`** — both need interactive passphrase entry, and silently skipping signing is worse than asking.
- Run tests before tagging. If the user is in a hurry and skips, note it in the release commit body.
- F-Droid (when/if enabled) builds from source using the tagged commit, so the tag must point to a buildable, signable state.
- The release `proguard-rules.pro` file exists but `isMinifyEnabled = false` — minification is currently OFF. If the user enables it, audit ProGuard rules for `kotlinx.serialization` and `BiometricPrompt` callbacks before tagging.

## Task: $ARGUMENTS
