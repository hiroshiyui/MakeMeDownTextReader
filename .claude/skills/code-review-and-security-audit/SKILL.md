---
name: code-review-and-security-audit
description: Review code for quality, correctness, and security vulnerabilities. Use when the user asks to review code, audit for security issues, or check for bugs and anti-patterns.
argument-hint: file path, component name, or scope of review
---

# Code Review and Security Audit

You are performing code review and security auditing for **MakeMeDown Text Reader** — a Kotlin / Jetpack Compose Android reader and editor for EPUB 3, Markdown, and plain text, with a biometric-locked private documents space.

## Scope

This skill covers two complementary concerns:

1. **Code Review** — correctness, readability, maintainability, and adherence to project conventions.
2. **Security Audit** — identifying vulnerabilities, unsafe patterns, and potential attack surfaces.

## Project context to keep in mind

- **No WebView, no JNI, no native code.** All rendering goes through Compose `Text` / `AnnotatedString`. Don't waste time auditing JNI buffer handling or `addJavascriptInterface` — they don't exist here.
- **Single declared permission**: `USE_BIOMETRIC`. No storage, no network, no contacts.
- **Document I/O is SAF-based** for everything except the private space, which uses `filesDir/private_documents/` directly via `java.io.File`.
- **Persistence** is JSON-on-disk via `kotlinx.serialization` (`recents.json`, `settings.json`) — not `SharedPreferences`, not Room.
- **Navigation** is a hand-rolled sealed `Screen` state with a custom `Saver` — no `androidx.navigation`.
- **`MainActivity` is a `FragmentActivity`** because `BiometricPrompt` requires it. Don't suggest converting it to `ComponentActivity`.
- **Auto Backup is intentionally disabled** for `private_documents/` in both `backup_rules.xml` and `data_extraction_rules.xml`. Any review of those files must preserve that exclusion.

## Review Checklist

### Code Quality (Kotlin / Compose)

- **Null safety**: proper use of Kotlin null-safe operators; avoid `!!` unless justified by an invariant the caller can see.
- **Coroutines**: long-running file I/O (`DocumentLoader`, `DocumentSaver`, `EpubParser`, `PrivateStore`) must run on `Dispatchers.IO`; UI updates must come back to the main dispatcher. Flag any blocking I/O on the composition thread.
- **Lifecycle scoping**: prefer `rememberCoroutineScope`, `LaunchedEffect(key)`, and `produceState` over manually managed `Job`s. Composables must not leak coroutines on recomposition.
- **State hoisting**: Compose state should be hoisted to the lowest common owner; flag composables that own state which a parent already needs.
- **`remember` keys**: any `remember { … }` whose computation depends on inputs must use `remember(key1, …)` — flag stale captures.
- **Resource management**: `InputStream`, `OutputStream`, `ZipFile`, `ZipInputStream`, and `ParcelFileDescriptor` must be closed via `use { }`. No bare `close()` in `finally`.
- **Error handling**: `DocumentLoader` / `DocumentSaver` return sealed result types or throw — don't silently swallow `IOException`. The reader screen surfaces failures via Snackbar; preserve that contract.
- **Consistency with project conventions**: Compose + `kotlinx.serialization` + sealed `Screen` navigation. Don't introduce `androidx.navigation`, Hilt, Room, RxJava, or Moshi.

### Code Smells

- **Long composables**: composables doing too many things; should be broken into smaller, focused composables with hoisted state.
- **Recomposition hazards**: unstable lambdas, captured `var`s, missing `key()` in `LazyColumn`, expensive work outside `remember`.
- **Duplicated rendering logic** between `MarkdownReader`, `EpubReader`, and `PlainTextReader` — when AnnotatedString span construction is repeated, extract.
- **Deep nesting**: excessive `if`/`when`/`try` nesting; prefer early returns or extraction.
- **Magic numbers/strings**: especially font sizes, padding values, and file/MIME constants — promote to named constants or `Settings`.
- **Primitive obsession**: file paths and document identifiers passed as raw `String`s where a sealed `DocumentSource` (SAF `Uri` vs `private://name`) would be clearer.
- **Mutable shared state**: prefer immutable data classes and `StateFlow`/`mutableStateOf`; flag unnecessary `var` or top-level mutable collections.
- **Long parameter lists** in composables: consider grouping into a state holder or data class.

### Refactoring Suggestions

- **Extract composable**: pull out blocks that render a distinct visual unit (e.g. a code block, a heading, an image card).
- **Replace conditional with polymorphism**: `when (block)` chains over `EpubBlock` / Markdown AST nodes that grow large can sometimes be cleaned up via a renderer interface — but only if the win is real, sealed-class `when` is idiomatic and exhaustive.
- **Use Kotlin idioms**: `let`/`apply`/`also`, destructuring, extension functions, `buildList`/`buildString`, `runCatching` for boundary-only error capture.
- **Compose idioms**: prefer `derivedStateOf` for derived state, `snapshotFlow` for bridging to coroutines, `CompositionLocalProvider` for ambient values (the project already does this for reader font / scale).
- **Improve testability**: flag code that mixes `Context` lookups with logic; suggest passing dependencies in. `EpubParser`, `MarkdownToBlocks`, and `PrivateStore` should be unit-testable without an Android device.

### Private space & biometric flow (HIGH-VALUE AREA)

This is the most security-sensitive surface in the app. Audit carefully.

- **Unlock semantics**: cold start after the process is killed must re-prompt; brief backgrounding and rotation must not. Verify the `PrivateSpaceSession` flag is process-scoped (not persisted) and is cleared on the right lifecycle events.
- **`DeviceAuth` (`auth/DeviceAuth.kt`)**: `BiometricPrompt` is configured with `setDeviceCredentialAllowed(true)` so any enrolled biometric *or* the system PIN/pattern/password satisfies it. Don't suggest tightening this to biometric-only — it's deliberate so users without enrolled fingerprints can still use the space.
- **Share gate**: every share from the private space requires (a) an explicit confirmation dialog *and* (b) a fresh `DeviceAuth` call. There must be **no time-based "I authenticated 30 seconds ago" cache**. Flag any change that introduces one.
- **`FileProvider` exposure**: the manifest `<provider>` must remain `exported=false`, `grantUriPermissions=true`, with authority `${applicationId}.fileprovider`. `res/xml/file_paths.xml` must expose **only** `<files-path name="private_documents" path="private_documents/" />` — anything broader (`<files-path path="."/>`, `<files-path path=""/>`, `<external-files-path>`, etc.) leaks `recents.json`, `settings.json`, or other internal state and must be flagged Critical.
- **Share intent flags**: outbound `Intent.ACTION_SEND` must use `FLAG_GRANT_READ_URI_PERMISSION` only — never `FLAG_GRANT_WRITE_URI_PERMISSION`, never `FLAG_GRANT_PERSISTABLE_URI_PERMISSION`.
- **Backup exclusions**: any change to `res/xml/backup_rules.xml` or `res/xml/data_extraction_rules.xml` must keep `private_documents/` excluded from both cloud backup *and* device-to-device transfer. Flag any removal as Critical.
- **Path safety in `PrivateStore`**: file names entering `PrivateStore` must be validated — no path separators (`/`, `\`), no `..`, no absolute paths, no NUL bytes. The store must resolve every path under `filesDir/private_documents/` and reject anything that escapes (canonical path check). Treat path traversal here as Critical.
- **No leakage to logs**: `Log.d` / `println` of file contents, file names, or unlock state should not exist in release builds. Flag any logging of private document content or names.

### Document parsing security

- **EPUB / zip handling (`reader/epub/EpubParser.kt`)**:
  - **Zip slip**: every `ZipEntry` name must be canonicalized and verified to live under the intended extraction root. Flag any `File(root, entry.name)` without that check as Critical.
  - **Decompression bombs**: cap per-entry uncompressed size and total uncompressed size. A 1 KB EPUB that expands to 4 GB must not OOM the device.
  - **XXE in OPF / container.xml / XHTML**: any `XmlPullParser`, `DocumentBuilder`, or `SAXParser` must disable external entities and DTDs (`FEATURE_SECURE_PROCESSING`, `disallow-doctype-decl`, no `XMLConstants.ACCESS_EXTERNAL_*`). Flag missing hardening as High.
  - **Path traversal in OPF references**: chapter `href`s and image `src`s must be resolved relative to the OPF base and confined to the zip — never opened as filesystem paths.
  - **Zip entry count cap**: refuse EPUBs with absurd entry counts.
- **Markdown link handling (`reader/markdown/`)**: clickable links must go through a safe URI launcher. Reject or neutralize `javascript:`, `file://`, `content://`, `intent:`, and other unexpected schemes; allow `http`, `https`, `mailto` (and consider whether `tel:` is desired). Flag raw `Intent(ACTION_VIEW, Uri.parse(href))` without scheme filtering.
- **Image decoding**: images extracted from EPUBs are attacker-controlled. Decoding via `BitmapFactory` is reasonable, but cap dimensions and use `inSampleSize` for large images to avoid OOM. Cache scoped per chapter, not globally.
- **JSON persistence (`RecentsRepository`, `SettingsRepository`)**: malformed `recents.json` / `settings.json` (corrupted file, partial write, schema drift) must not crash the app. Decoding should be wrapped and fall back to defaults. Writes should be atomic (write to temp + rename) to survive crashes mid-write.

### Android-Specific Security

- **SAF `Uri` handling**: persistable URI permissions (`takePersistableUriPermission`) must be released when entries are dropped from recents. `openInputStream` / `openOutputStream` results may be `null` — handle it.
- **Intent handling**: `MainActivity` should validate any incoming `ACTION_VIEW` URIs (MIME / scheme) before passing them to the loader. Don't trust `intent.data` blindly.
- **Component export**: `MainActivity` is the only exported component; `<provider>` is `exported=false`. Verify no new `<activity>`, `<service>`, `<receiver>`, or `<provider>` is added with `android:exported="true"` without an explicit reason.
- **`allowBackup`**: project intentionally allows backup at the app level but excludes `private_documents/` via the backup rules. Don't flip `android:allowBackup` to `false` without understanding the trade-off.
- **`networkSecurityConfig` / cleartext**: app does no network. If anything ever introduces an HTTP call, flag it.
- **R8/ProGuard**: release build currently has `isMinifyEnabled = false`. If that changes, ensure `kotlinx.serialization` keep rules and `BiometricPrompt` callbacks survive shrinking.

### General Security

- No hardcoded secrets, API keys, or credentials.
- No `Runtime.exec()` / `ProcessBuilder` — there should be none in this app; flag any introduction.
- No `System.loadLibrary` / native code — flag any introduction.
- Insecure random: `java.util.Random` is fine for non-security uses (e.g. picking a placeholder). Anything touching auth must use `SecureRandom`.
- Dependencies (`gradle/libs.versions.toml`): flag known-vulnerable versions; the dependency surface is small (Compose BOM, `org.jetbrains:markdown`, `kotlinx.serialization`, `androidx.biometric`) so this should be tractable.

## Output Format

Report findings using this structure:

### Critical / High

Issues that must be fixed — security vulnerabilities (zip slip, FileProvider over-exposure, share-gate bypass, path traversal in `PrivateStore`), crashes, data loss risks.

### Medium

Issues that should be fixed — logic bugs, recomposition hazards, missing error handling, code smell with real impact.

### Low / Informational

Suggestions for improvement — style, readability, minor optimizations, refactor opportunities.

For each finding, include:
- **File and line number** as a clickable link (e.g. [EpubParser.kt:42](app/src/main/java/org/ghostsinthelab/app/makedown/reader/epub/EpubParser.kt#L42))
- **Description** of the issue
- **Impact** — what could go wrong, and for whom
- **Recommendation** — how to fix it, with a code sketch if useful

## How to Run

When invoked without arguments, review recently changed files:

```bash
git diff --name-only HEAD~5
```

When invoked with a specific scope (file, directory, or component name), focus the review on that area.

For a full audit, systematically review in this order (highest-risk first):

1. **Private space surface** — [auth/DeviceAuth.kt](app/src/main/java/org/ghostsinthelab/app/makedown/auth/DeviceAuth.kt), [data/PrivateStore.kt](app/src/main/java/org/ghostsinthelab/app/makedown/data/PrivateStore.kt), [ui/PrivateSpaceScreen.kt](app/src/main/java/org/ghostsinthelab/app/makedown/ui/PrivateSpaceScreen.kt), [MainActivity.kt](app/src/main/java/org/ghostsinthelab/app/makedown/MainActivity.kt) (`PrivateSpaceSession` lifecycle).
2. **Manifest & resource exposure** — `app/src/main/AndroidManifest.xml`, `app/src/main/res/xml/file_paths.xml`, `app/src/main/res/xml/backup_rules.xml`, `app/src/main/res/xml/data_extraction_rules.xml`.
3. **Document parsing** — [reader/epub/EpubParser.kt](app/src/main/java/org/ghostsinthelab/app/makedown/reader/epub/EpubParser.kt), [reader/epub/XhtmlToBlocks.kt](app/src/main/java/org/ghostsinthelab/app/makedown/reader/epub/XhtmlToBlocks.kt), [reader/markdown/MarkdownToBlocks.kt](app/src/main/java/org/ghostsinthelab/app/makedown/reader/markdown/MarkdownToBlocks.kt).
4. **I/O layer** — [io/DocumentLoader.kt](app/src/main/java/org/ghostsinthelab/app/makedown/io/DocumentLoader.kt), [io/DocumentSaver.kt](app/src/main/java/org/ghostsinthelab/app/makedown/io/DocumentSaver.kt).
5. **Persistence** — [data/RecentsRepository.kt](app/src/main/java/org/ghostsinthelab/app/makedown/data/RecentsRepository.kt), [data/SettingsRepository.kt](app/src/main/java/org/ghostsinthelab/app/makedown/data/SettingsRepository.kt).
6. **UI** — [ui/HomeScreen.kt](app/src/main/java/org/ghostsinthelab/app/makedown/ui/HomeScreen.kt), [ui/ReaderScreen.kt](app/src/main/java/org/ghostsinthelab/app/makedown/ui/ReaderScreen.kt), [ui/SettingsScreen.kt](app/src/main/java/org/ghostsinthelab/app/makedown/ui/SettingsScreen.kt), [ui/TextEditor.kt](app/src/main/java/org/ghostsinthelab/app/makedown/ui/TextEditor.kt).
7. **Build & dependencies** — `app/build.gradle.kts`, `gradle/libs.versions.toml`.

## Task: $ARGUMENTS
