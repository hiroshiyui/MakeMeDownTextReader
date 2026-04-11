---
name: docs-engineering
description: Writing/updating project documentation (README, PRIVACY-POLICY, NOTICES, changelogs) and maintaining F-Droid metadata. Use when the user asks to update docs, write changelogs, or modify F-Droid store listings.
argument-hint: task description
---

# Documentation Engineering

You are performing documentation tasks for **MakeMeDown Text Reader**.

## Current state of project documentation

Read this before assuming files exist. The repo is young; most documentation files have not been created yet.

**Currently exists:**

| File | Status | Purpose |
|---|---|---|
| [README.md](README.md) | ✅ exists, English only | Project overview, features, build instructions |
| [LICENSE](LICENSE) | ✅ exists | GPL v3 full text |

**Does not yet exist** (don't pretend they do; ask before creating):

- `PRIVACY-POLICY.md`
- `NOTICES.md` (third-party license summary — Roboto Slab, jetbrains/markdown, kotlinx.serialization, androidx.biometric, Solarized palette, AndroidX. The README already lists these in its "Third-party" section.)
- `CLAUDE.md`
- `CHANGELOG.md`
- `fastlane/` directory (no F-Droid metadata tree at all yet)

When the user asks about one of the missing files, confirm whether to create it before writing — do not silently scaffold a tree of new docs.

## Tone and writing style

- The README is written in clear, slightly opinionated, English technical prose. Match it.
- Third-person technical writing for the README and any future `PRIVACY-POLICY.md`, `NOTICES.md`, `CLAUDE.md`.
- For changelogs and store listings (when/if added), first-person from the app's perspective is a reasonable default — but match whatever convention the user picks for the first one.
- **Do not add emojis** unless the user explicitly asks. The existing README has none.

## Bilingual? (currently no)

Unlike the user's other Android projects (e.g. Guileless Bopomofo, which is bilingual zh-TW + en-US), **MakeMeDown Text Reader's README is English-only** as of this writing. Don't reflexively produce Traditional Chinese mirrors of new docs unless the user asks for them.

If the user does ask to add Traditional Chinese:

- Use **Traditional Chinese characters only** (no Simplified). The user is Taiwanese.
- Decide with the user whether to interleave (en/zh paragraphs in the same file) or split (separate `README.zh-TW.md`). The Guileless Bopomofo project uses interleaved; this project hasn't picked yet.

## Project facts to keep accurate

When writing or editing docs, double-check these from the source rather than copying from another project. They've gone stale on the user before.

- **App name**: "MakeMeDown Text Reader" (sometimes shortened to "MakeMeDown" — verify with the user before shortening in headings).
- **Package / applicationId**: `org.ghostsinthelab.app.makedown` (note: `app`, **not** `apps`). This is different from the user's other projects which use `org.ghostsinthelab.apps.*`.
- **Source root**: [app/src/main/java/org/ghostsinthelab/app/makedown/](app/src/main/java/org/ghostsinthelab/app/makedown/)
- **Language / framework**: Kotlin 2.2.10, Jetpack Compose, Material 3. **No WebView, no JNI, no native code.**
- **minSdk** 26, **targetSdk** 36, **compileSdk** 36.1, AGP 9.1.0.
- **Permissions declared**: only `USE_BIOMETRIC`. Nothing else.
- **License**: GPL v3 **or later** (not "GPL v3" — the "or later" matters and is in the existing copyright headers).
- **Copyright holder**: `Hui-Hong You`, year `2026`.
- **Third-party libraries** to credit (already listed in the README's "Third-party" section): Roboto Slab (Apache 2.0), `org.jetbrains:markdown` 0.7.3 (Apache 2.0), `kotlinx.serialization` 1.7.3 (Apache 2.0), `androidx.biometric` 1.1.0 (Apache 2.0), Solarized palette (MIT), AndroidX / Compose (Apache 2.0).
- **No Chewing, no IME, no keyboard layouts.** This is a reader/editor app, not the user's input method project. Don't carry over Dachen / Hsu / E-Ten / 大千 references.
- **Architecture quick map** (for any doc that needs it): `data/`, `io/`, `auth/`, `reader/text/`, `reader/markdown/`, `reader/epub/`, `ui/`, `ui/theme/`, `MainActivity.kt`. Navigation is a hand-rolled sealed `Screen` state with a custom `Saver` — **no `androidx.navigation`**.

When updating any "third-party" / `NOTICES.md` content, verify the current dependency set against [gradle/libs.versions.toml](gradle/libs.versions.toml) and [app/build.gradle.kts](app/build.gradle.kts) — don't trust this skill or the README to be in sync indefinitely.

## Specific document guidance

### README.md

The existing README is structured as: tagline → Features (Reading, Editing, Private documents, Reading controls, Look and feel) → Requirements → Building → Architecture → Third-party → License → Contribution note. When updating it:

- Preserve that structure.
- Update the "Features" section in lockstep with code changes — the README is currently the project's most authoritative description of behavior, especially for the private space / share gate.
- The "Architecture" table maps packages to files. Keep it accurate when files move.

### PRIVACY-POLICY.md (if/when created)

The privacy story is unusually clean and worth stating plainly:

- The only declared permission is `USE_BIOMETRIC`.
- The app **makes no network requests** and has no analytics, telemetry, ads, or crash reporting.
- All data stays on-device. SAF documents stay where the user picked them; private documents live in `filesDir/private_documents/` and never leave unless the user explicitly shares them.
- The private documents directory is excluded from both cloud Auto Backup (`backup_rules.xml`) and device-to-device transfer (`data_extraction_rules.xml`).
- Sharing from the private space requires both an explicit confirmation dialog and a fresh biometric / device-credential check — no time-based cache.
- Biometric matching happens entirely in the system `BiometricPrompt` API; the app never sees fingerprint / face data.

Verify each of these against the code before publishing a privacy policy — claims must be true.

### NOTICES.md (if/when created)

Should contain the full license text (or a clear pointer + summary) for each third-party component listed in the README's "Third-party" section. Roboto Slab's license text is already vendored at `app/src/main/assets/fonts/RobotoSlab-LICENSE.txt` — link or include that.

### CLAUDE.md (if/when created)

This is project guidance for Claude Code itself. Keep it short: project layout, tech stack, commit convention pointer, and the "things this project intentionally doesn't have" list (no WebView, no JNI, no androidx.navigation, no Hilt, no Room).

### Changelogs

The project has no changelog file yet. When the user asks for one, ask whether they want:

- A `CHANGELOG.md` at the repo root (Keep-a-Changelog style is a good default, since the project already uses Conventional Commits).
- F-Droid `fastlane/metadata/.../changelogs/<versionCode>.txt` (only if/when a fastlane tree is added — see below).
- GitHub Release notes only.

To gather material:

```bash
git log --oneline <previous-tag>..HEAD     # if a previous tag exists
git log --oneline                          # for the very first version, no tags exist yet
```

The project's commit prefixes (`feat:`, `fix:`, `docs:`, `chore:`, `refactor:`) map cleanly to changelog sections — group by prefix.

### F-Droid metadata (if/when set up)

There is currently **no `fastlane/` directory.** Don't pre-create one. If the user wants to publish to F-Droid, the conventional layout is:

```
fastlane/metadata/android/en-US/
  title.txt              # max 50 chars
  short_description.txt  # max 80 chars
  full_description.txt   # max 4000 chars
  changelogs/<versionCode>.txt   # max 500 chars per file; filename is versionCode (integer), not versionName
  images/                # icon, feature graphic, screenshots
```

If/when the user wants Traditional Chinese, mirror under `zh-TW/`.

For changelog files: the filename is the **versionCode** (integer from [app/build.gradle.kts](app/build.gradle.kts)), not the versionName. Cap content at 500 characters. Use `*` for bullet points in en-US, `＊` (fullwidth asterisk) in zh-TW if/when zh-TW is added.

## Task: $ARGUMENTS
