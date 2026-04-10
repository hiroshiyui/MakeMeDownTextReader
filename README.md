# MakeMeDown Text Reader

An Android reader and editor for **EPUB 3**, **Markdown**, and **plain text**
documents. Built with Kotlin and Jetpack Compose, rendered natively (no
WebView), themed with Solarized, and wrapped in Roboto Slab.

The idea: a small, opinionated reading app that soft-wraps everything to
the display width, keeps the chrome calm, and lets you edit your text and
Markdown files in place without bouncing out to a different tool.

## Features

### Reading
- **EPUB 3** — in-process zip + OPF + XHTML parser that emits a sealed
  `EpubBlock` tree. Each chapter is rendered to Compose `Text` with
  `AnnotatedString` spans for emphasis, strong, strikethrough, inline
  code, and clickable links. Embedded images are decoded from the zip
  and cached per-chapter.
- **Markdown** — hand-rolled AST walker over
  [`org.jetbrains:markdown`](https://github.com/JetBrains/markdown) (GFM
  flavour). Toggles between rendered and raw views via a button in the
  top app bar.
- **Plain text** — lazy line-by-line rendering that wraps to the display
  width regardless of how long a line is in the source file.
- Code blocks and inline code always render in a monospace font, even if
  the surrounding prose is using a proportional reader font.

### Editing
- `.md` and `.txt` files can be edited in place from the reader. EPUB
  stays read-only.
- "New Markdown" and "New plain text" actions on the home screen create
  an empty file via `ACTION_CREATE_DOCUMENT` and drop straight into edit
  mode.
- Save / Done split with a "you have unsaved changes" dialog, a `•`
  dirty marker in the title, and a Snackbar for save success/failure.

### Reading controls
- A **bottom app bar** in the reader with a back-to-home button,
  `A−` / pt label / `A+` zoom controls, and an `Aa` dropdown to switch
  the reader font family without leaving the document.
- **Base font size** (in pt) and **reader font family** both live in a
  Settings screen, persisted to `settings.json` in the app's `filesDir`.
  The same `baseFontSizePt` is edited by the Settings stepper and the
  reader bottom bar.
- **Recently opened** list on the home screen, backed by a persistable
  SAF URI and `recents.json`. Up to 32 entries, most recent first.

### Look and feel
- **Solarized** color scheme applied across every Material 3 role for
  both light and dark modes. Dynamic color (Material You) is off by
  default so the Solarized palette is what you actually see.
- **Roboto Slab** is vendored as the **interface** font for every piece
  of Material 3 chrome (top/bottom bars, dialogs, settings, snackbars,
  buttons). The reader content deliberately uses a separate,
  user-selectable font, so changing the reading font from Settings or
  the bottom bar has no effect on the app's chrome and vice versa.
- Custom adaptive launcher icon: an open book with text lines and a
  pair of round reading glasses resting above it, rendered in the
  Solarized palette. Includes a monochrome variant for Android 13+
  themed icons.

## Requirements

- **minSdk 26** (Android 8.0 Oreo).
- **targetSdk 36** (Android 17 minor API level 1).
- **compileSdk 36.1**.
- Built with Android Gradle Plugin 9.1.0 and Kotlin 2.2.10.

## Building

```bash
./gradlew assembleDebug
```

The debug APK ends up at `app/build/outputs/apk/debug/app-debug.apk`.
Install it on a device or emulator with `adb install`.

For a release build you'll need to provide a signing config; the default
`release` build type has `isMinifyEnabled = false` and no signing set up.

## Architecture (quick tour)

All code lives under `org.ghostsinthelab.app.makedown`:

| Package | What's there |
| --- | --- |
| `data/` | `DocumentType`, `RecentFile`/`RecentsRepository`, `ReaderFont`, `Settings`/`SettingsRepository` — JSON-backed persistence using kotlinx.serialization. |
| `io/` | `DocumentLoader` (`Uri → LoadedDocument` on `Dispatchers.IO`), `DocumentSaver` (write-back via `contentResolver.openOutputStream(uri, "wt")`). |
| `reader/text/` | `PlainTextReader` — line-by-line `LazyColumn`. |
| `reader/markdown/` | `MarkdownAst`, `MarkdownToBlocks` (AST walker), `MarkdownReader` (Compose renderer + raw toggle). |
| `reader/epub/` | `EpubModel`, `EpubParser` (zip + container + OPF), `XhtmlToBlocks` (XHTML walker), `EpubReader` (chapter pager + renderer). |
| `ui/` | `Screen` (sealed nav state), `HomeScreen`, `ReaderScreen`, `SettingsScreen`, `TextEditor`, `ReaderBottomBar`, `LocalReaderFontFamily` / `LocalReaderFontScale` + `TextStyle.scaledBy(factor)`. |
| `ui/theme/` | Solarized palette, Material 3 color scheme mapping, Roboto Slab-based `Typography`. |
| `MainActivity.kt` | `AppRoot` composable holding the `Screen` state and providing the reader font / scale composition locals from `SettingsRepository`. |

Navigation is a sealed `Screen` state saved via a custom `Saver` — no
`androidx.navigation` dependency.

## Third-party

- [**Roboto Slab**](https://github.com/googlefonts/robotoslab) — vendored
  at `app/src/main/res/font/roboto_slab_{regular,medium,bold}.ttf`.
  Apache License 2.0. Full text at
  `app/src/main/assets/fonts/RobotoSlab-LICENSE.txt`.
- [**org.jetbrains:markdown**](https://github.com/JetBrains/markdown)
  0.7.3 — Apache License 2.0.
- [**kotlinx.serialization**](https://github.com/Kotlin/kotlinx.serialization)
  1.7.3 — Apache License 2.0.
- **Solarized** palette by
  [Ethan Schoonover](https://ethanschoonover.com/solarized/) — MIT.
- **AndroidX** / Jetpack Compose — Apache License 2.0.

All of the above are compatible with GPL v3+.

## License

MakeMeDown Text Reader is free software, released under the **GNU
General Public License, version 3 or (at your option) any later
version**. See [`LICENSE`](LICENSE) for the full text.

```
Copyright (C) 2026 Hui-Hong You

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
```

Contributions are welcome; by submitting code you agree that it will be
released under the same terms.
