#!/bin/bash
# start-task <slug> [feat|fix|chore|wip] — worktree + branch off fresh origin base.
set -euo pipefail
SLUG="${1:?usage: start-task <slug> [feat|fix|chore|wip]}"
KIND="${2:-feat}"
case "$KIND" in feat|fix|chore|wip) ;; *) echo "kind must be feat|fix|chore|wip" >&2; exit 1 ;; esac
ROOT="$(git rev-parse --show-toplevel)"
BASE="$(git remote show origin 2>/dev/null | sed -n 's/.*HEAD branch: //p' || true)"; BASE="${BASE:-main}"
git fetch -q origin "$BASE" 2>/dev/null || true
START="origin/$BASE"; git rev-parse -q --verify "$START" >/dev/null || START="$BASE"
WT="$ROOT/.worktrees/$SLUG"
[ -e "$WT" ] && { echo "worktree $WT already exists" >&2; exit 1; }
git worktree add -q -b "$KIND/$SLUG" "$WT" "$START"
echo "Worktree ready: $WT (branch $KIND/$SLUG off $START)"
echo "Open: cursor \"$WT\""
