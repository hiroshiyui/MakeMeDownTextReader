---
name: commit-and-push
description: Commit code changes and push via Git. Use when the user asks to commit, push, or save their work to the repository.
argument-hint: commit message or description of changes
---

# Commit and Push

You are committing and pushing code changes for **MakeMeDown Text Reader**.

## Commit Message Convention

This project uses **Conventional Commits**-style prefixes. Look at `git log` before writing a message — every existing commit follows this pattern.

- **Subject line**: `<type>: <imperative summary>`, lowercase after the colon, no trailing period.
- **Body** (optional, blank line after subject): explain *why* the change was made, not just what. Wrap to ~72 chars. Include context when the change is non-trivial.
- **No `Signed-off-by` trailer.** This project's commits are **GPG-signed** (`-S`), not DCO-signed-off. Don't add `--signoff` unless the user asks for it.
- **No `Co-Authored-By` trailer** unless the user asks for one. Existing commits don't carry one and adding one silently changes the project's authorship pattern.

### Allowed types

Match the prefixes already in the log:

- `feat:` — a new user-visible feature
- `fix:` — a bug fix
- `docs:` — documentation only (README, comments, license headers)
- `chore:` — tooling, build config, ignores, license headers, dependency bumps
- `refactor:` — code restructuring with no behavior change
- `style:` — formatting / cosmetic
- `test:` — adding or fixing tests
- `perf:` — performance work

If a change spans multiple types, pick the dominant one. If you can't, that's a hint the change should be split into multiple commits.

### Examples from this repo

```
feat: add gated share/export from the private space
feat: add biometric-locked private space to reader
docs: cover the locked private space, share gate, and FileProvider
chore: extend GPL v3+ headers to kept Gradle/resource files
chore: relicense project under GPL v3 or later
```

## Workflow

1. **Review changes** — run `git status` and `git diff` (and `git diff --cached` if anything is already staged) to understand what will be committed. Read the actual diff before writing the message; do not infer from filenames.
2. **Stage files** — add specific files by name. **Do not** use `git add -A` or `git add .`. Be careful not to stage:
   - `local.properties` (contains `sdk.dir` — already gitignored, but double-check)
   - `app/build/`, `build/`, `.gradle/`, `.cxx/` (already gitignored)
   - `.idea/` (entire directory is gitignored per the project's `chore: ignore the entire .idea directory` commit — do not re-add files from there)
   - Any `*.jks`, `*.keystore`, `keystore.properties`, or anything resembling signing material
   - `.env`, `secrets.*`, anything with credentials
3. **GPL header check** — when adding new `.kt`, `.kts`, or significant resource files, verify the GPL v3+ header is present. The project deliberately extends GPL v3+ headers across the tree (see `chore: extend GPL v3+ headers to kept Gradle/resource files`). The Android Studio copyright profile lives at `.idea/copyright/GPLv3_or_later.xml`.
4. **Compose the message** — pick the right type prefix, write a concise imperative subject. If the change is non-trivial, add a body explaining *why*.
5. **Commit with GPG signing** — pass the message via HEREDOC for proper formatting:
   ```bash
   git commit -S -m "$(cat <<'EOF'
   feat: <imperative summary>

   Optional body explaining why.
   EOF
   )"
   ```
   The `-S` flag triggers GPG signing. The user's signing key is configured in their git config and may prompt for a passphrase via `gpg-agent` / pinentry — that's expected. **Do not** pass `--no-gpg-sign` or `-c commit.gpgsign=false` to bypass it; if signing fails, stop and report the error rather than producing an unsigned commit.
6. **Verify** — run `git log -1 --show-signature` to confirm the commit landed and the signature is valid.

## Push

- **Always confirm with the user before pushing.** Pushing is visible to others; do not push automatically just because the commit succeeded.
- The project has a single remote: `origin → git@github.com:hiroshiyui/MakeMeDownTextReader.git`.
- The default (and currently only) branch is `main`. There is no `master`, no `current`, no `develop`.
- Push with `git push origin main` (or the user's current branch name if they're working on a feature branch).
- **Never force-push to `main`** without an explicit, unmistakable request. Never use `--force` when `--force-with-lease` would do.
- Never pass `--no-verify`. If a pre-push hook fails, investigate and fix the underlying issue.

## Branching

The repo currently has only `main`. If you need to create a feature branch, use a descriptive lowercase-with-dashes name (e.g. `private-space-export-cache`, `epub-zip-slip-fix`). Branch off `main` and target `main` for the eventual merge.

## Task: $ARGUMENTS
