#!/bin/bash
# Append this commit to the main checkout's journal (+ vault note if configured).
# Worktree-safe: resolves the MAIN checkout via --git-common-dir so worktree
# commits do not fragment across checkouts. Append-only everywhere (iCloud rule).
set -u
COMMON="$(git rev-parse --git-common-dir 2>/dev/null)" || exit 0
case "$COMMON" in /*) ;; *) COMMON="$PWD/$COMMON" ;; esac
MAIN_ROOT="$(cd "$COMMON/.." && pwd)"
REPO="$(basename "$MAIN_ROOT")"
HASH="$(git log -1 --format=%h)"; SUBJECT="$(git log -1 --format=%s)"
BRANCH="$(git rev-parse --abbrev-ref HEAD)"; WHEN="$(git log -1 --format=%cI)"
mkdir -p "$MAIN_ROOT/.harness" "$MAIN_ROOT/docs/memory"
ESC_SUBJECT="$(printf '%s' "$SUBJECT" | sed 's/\\/\\\\/g; s/"/\\"/g')"
ESC_BRANCH="$(printf '%s' "$BRANCH" | sed 's/\\/\\\\/g; s/"/\\"/g')"
printf '{"ts": "%s", "hash": "%s", "branch": "%s", "subject": "%s"}\n' \
  "$WHEN" "$HASH" "$ESC_BRANCH" "$ESC_SUBJECT" >> "$MAIN_ROOT/.harness/journal.jsonl"
JOURNAL="$MAIN_ROOT/docs/memory/change-journal.md"
[ -f "$JOURNAL" ] || printf '# Change journal — %s\n\nAppend-only, one line per commit (via .githooks/post-commit).\n\n' "$REPO" > "$JOURNAL"
printf -- '- %s · `%s` · %s — %s\n' "${WHEN%%T*}" "$HASH" "$BRANCH" "$SUBJECT" >> "$JOURNAL"
VAULT_NOTE="$(git config --get harness.vaultlog || true)"
if [ -n "$VAULT_NOTE" ] && [ -d "$(dirname "$VAULT_NOTE")" ]; then
  [ -f "$VAULT_NOTE" ] || printf '# %s — change log\n\nAppend-only commit log (agent-kit journal).\n\n' "$REPO" > "$VAULT_NOTE"
  printf -- '- %s · `%s` · %s — %s\n' "${WHEN%%T*}" "$HASH" "$BRANCH" "$SUBJECT" >> "$VAULT_NOTE"
fi
exit 0
