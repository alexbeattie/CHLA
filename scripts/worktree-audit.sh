#!/bin/bash
# worktree-audit [clean [--apply]] — classify .worktrees/*; remove merged+clean with clean --apply.
set -euo pipefail
ROOT="$(git rev-parse --show-toplevel)"
MODE="${1:-report}"; APPLY="${2:-}"
BASE="$(git remote show origin 2>/dev/null | sed -n 's/.*HEAD branch: //p' || true)"; BASE="${BASE:-main}"
git rev-parse -q --verify "origin/$BASE" >/dev/null 2>&1 && REF="origin/$BASE" || REF="$BASE"
[ -d "$ROOT/.worktrees" ] || { echo "no worktrees"; exit 0; }
for WT in "$ROOT"/.worktrees/*/; do
  [ -d "$WT" ] || continue
  NAME="$(basename "$WT")"
  BR="$(git -C "$WT" rev-parse --abbrev-ref HEAD 2>/dev/null || echo '?')"
  if git merge-base --is-ancestor "$(git -C "$WT" rev-parse HEAD)" "$REF" 2>/dev/null; then M="merged"; else M="unmerged"; fi
  if [ -z "$(git -C "$WT" status --porcelain 2>/dev/null)" ]; then C="clean"; else C="dirty"; fi
  echo "$NAME $M $C ($BR)"
  if [ "$MODE" = "clean" ] && [ "$APPLY" = "--apply" ]; then
    if [ "$M" = "merged" ] && [ "$C" = "clean" ]; then
      git worktree remove "$WT" && echo "removed $NAME"
    elif [ "$C" = "dirty" ]; then
      echo "SKIP $NAME: dirty — resolve by hand"
    fi
  fi
done
