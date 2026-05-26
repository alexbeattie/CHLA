# Git History Scrub Runbook

## Why this exists

The DB password `CHLASecure2024` and the prior `DJANGO_SECRET_KEY` value
(`k^v1yy9u1z+ztuj9wg))si(8q5s8%7k3#aorgm78jqwy@k@kg#`) were committed in
plain text in earlier history of this repository. Both have been rotated
in AWS, so the live blast radius is zero. They remain in git history,
which means anyone who has cloned the repo or who can pull old commit
SHAs from GitHub can still grep them.

This runbook removes those strings from every commit on every branch.
The credentials become permanently unrecoverable from this repository.

## Pre-flight checklist

- [ ] Confirm both credentials are already rotated and dead in production
      (RDS no longer accepts the old DB password; the old DJANGO_SECRET_KEY
      is no longer in use because EB containers fetch from
      `kindd/prod/django-secret-key`).
- [ ] Identify all collaborators and notify them: their clones will need
      to be re-cloned after the rewrite. Any open feature branches need
      to be backed up to a patch file or a separate remote first.
- [ ] Confirm CI runners with cached checkouts will pull fresh on the
      next run (clear the cache if needed).
- [ ] Pause merges to `main` for the duration of the operation.
- [ ] Have a current full backup of the repo (a fresh `git clone --mirror`
      to a sibling directory is sufficient).

## The rewrite

Requires `git-filter-repo` (install via `brew install git-filter-repo`
or `pip install git-filter-repo`).

```bash
# 1. Take a safety mirror clone in a sibling directory
cd ..
git clone --mirror git@github.com:alexbeattie/CHLA.git CHLA-mirror-backup-$(date +%Y%m%d)

# 2. Work from a fresh mirror clone (filter-repo refuses to run on a
#    non-fresh clone unless you pass --force, which is risky)
git clone --mirror git@github.com:alexbeattie/CHLA.git CHLA-scrub
cd CHLA-scrub

# 3. Build the replacements file
cat > /tmp/kindd-scrub-replacements.txt <<'EOF'
CHLASecure2024==>***REMOVED***
k^v1yy9u1z+ztuj9wg))si(8q5s8%7k3#aorgm78jqwy@k@kg#==>***REMOVED***
EOF

# 4. Run the scrub
git filter-repo --replace-text /tmp/kindd-scrub-replacements.txt

# 5. Verify the strings are gone
git log --all --oneline | head -5
git grep CHLASecure2024 $(git rev-list --all) 2>&1 | head -5  # should be empty
git grep 'k\^v1yy9u1z' $(git rev-list --all) 2>&1 | head -5   # should be empty

# 6. Force-push every branch and tag
git push --force --all origin
git push --force --tags origin

# 7. Clean up
rm /tmp/kindd-scrub-replacements.txt
```

## After the rewrite

- [ ] Every collaborator deletes their local clone and re-clones fresh
      (`rm -rf CHLA && git clone git@github.com:alexbeattie/CHLA.git`).
      Local feature branches must be re-applied as patches against the
      new history; the original SHAs no longer exist.
- [ ] Any open PR is invalidated. Re-open against the new HEAD if needed.
- [ ] Verify CI passes on the rewritten history before resuming merges.
- [ ] Optional: contact GitHub Support to expire the old, unreachable
      commits from their cache. Without this step, the old commits are
      accessible by SHA for ~30-90 days, so the leaked credential is
      still discoverable to someone with a stale commit URL. Submit a
      request via https://support.github.com/contact/private-information
      pointing at `https://github.com/alexbeattie/CHLA` and the SHA
      ranges where the credentials lived.
- [ ] Delete the safety mirror clone (`rm -rf CHLA-mirror-backup-*`)
      once you're satisfied the rewrite is good and CI is green.

## Risk matrix

| Action | Reversibility | Blast radius |
|---|---|---|
| `git filter-repo --replace-text` (local) | Reversible from the safety mirror | Local clone only, until pushed |
| `git push --force --all` | NOT reversible without the safety mirror | Every consumer of this repo |
| GitHub stale-commit cache | Cleared automatically in 30-90d, or via support | Anyone with a leaked SHA |

If anything looks wrong post-push, you can recover by force-pushing the
safety mirror back over the wrong state — but only if no one has pushed
new commits in the meantime. Hence the "pause merges" pre-flight item.
