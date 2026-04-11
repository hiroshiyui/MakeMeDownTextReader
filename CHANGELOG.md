# Changelog

All notable changes to **MakeMeDown Text Reader** are documented in
this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.0.1] - 2026-04-11

Patch release that fixes a hard crash on the file-open / file-create
path, plus the first F-Droid metadata tree.

### Fixed

- **Crash when tapping *Open file* or any *New …* action.** The
  `androidx.fragment` library was being resolved transitively to
  `1.2.5` via `androidx.biometric:biometric:1.1.0` →
  `androidx.appcompat:1.2.0`. Fragment `1.2.5`'s
  `FragmentActivity.startActivityForResult` unconditionally rejects
  request codes with any of the upper 16 bits set, but
  `ActivityResultRegistry` generates request codes starting at
  `0x00010000`, so every SAF launcher (Open, Create Markdown, Create
  plain text, Share) crashed 100% of the time on first tap.
  `androidx.fragment` is now pinned to `1.8.5` in
  `gradle/libs.versions.toml` and declared as a direct dependency in
  `app/build.gradle.kts`, so Gradle's conflict resolution upgrades
  past the transitive `1.2.5`. Fragment `1.6+` delegates the result
  path to `ComponentActivity`'s registry and sidesteps the check.

### Added

- F-Droid metadata tree under `fastlane/metadata/android/en-US/`:
  title, short and full descriptions, a per-version changelog, and
  four 1080 × 2400 phone screenshots (home, settings, Markdown
  reader, Markdown editor). This is the first time the app has
  carried F-Droid listing metadata in-repo.

## [1.0.0] - 2026-04-11

First public release.

### Added

- EPUB 3, Markdown, and plain text reader, with a per-chapter pager
  for EPUBs and a `LazyColumn`-based renderer for plain text.
- Compose-based in-place editor for text and Markdown, with save
  through SAF.
- New-file creation flow from the home screen.
- Reader font preference with a dedicated Settings screen. The reader
  font is independent of the app's chrome font and switches live via
  a Compose composition local.
- Reader bottom bar with zoom and base font-size controls.
- **Solarized** colour scheme applied across every Material 3 role
  for both light and dark modes. Dynamic colour (Material You) is off
  by default so the Solarized palette is what you actually see.
- **Roboto Slab** vendored as the interface font for every piece of
  Material 3 chrome.
- App-private documents store under `filesDir/private_documents/`,
  explicitly excluded from both cloud Auto Backup and device-to-device
  transfer via `backup_rules.xml` and `data_extraction_rules.xml`.
- `DeviceAuth` helper wrapping `BiometricPrompt` with
  `setDeviceCredentialAllowed(true)` — any enrolled biometric, or the
  system PIN / pattern / password, satisfies the prompt.
- Biometric-locked private space screen, with process-scoped unlock
  (cold start after the process is killed re-prompts; brief
  backgrounding and rotation do not).
- Gated share / export from the private space: every outbound share
  requires an explicit confirmation dialog *and* a fresh
  `DeviceAuth` call. There is no time-based "I authenticated recently"
  cache.
- `FileProvider` for outbound sharing, declared with `exported=false`,
  `grantUriPermissions=true`, authority `${applicationId}.fileprovider`,
  and a `file_paths.xml` that exposes only the `private_documents/`
  subtree.
- Custom adaptive launcher icon: a Crying Cat Face (U+1F63F 😿)
  rasterised from Noto Color Emoji, sitting above a bold **MMD**
  wordmark whose letters are sliced by horizontal scanline stripes,
  on the Solarized base2 background. Includes a monochrome variant
  for Android 13+ themed icons that shows just the scanlined MMD
  wordmark.

[Unreleased]: https://github.com/hiroshiyui/MakeMeDownTextReader/compare/1.0.1...HEAD
[1.0.1]: https://github.com/hiroshiyui/MakeMeDownTextReader/compare/1.0.0...1.0.1
[1.0.0]: https://github.com/hiroshiyui/MakeMeDownTextReader/commits/1.0.0
