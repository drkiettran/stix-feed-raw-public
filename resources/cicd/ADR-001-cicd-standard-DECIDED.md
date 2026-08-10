# CI/CD Standard for Databricks Jobs and Apps — Decisions

**Status:** Accepted (team meeting, 2026-08-10) — one open item under D7
**Applies to:** all new job and app deployments on the platform

| # | Decision | Choice |
|---|---|---|
| D1 | CI platform | **GitHub Actions** (OIDC federation to Databricks service principals — no stored secrets for CI auth) |
| D2 | Deployment tool | **Databricks Asset Bundles (DABs)** |
| D3 | Repo strategy | **Monorepo** — all job/app projects live as folders in one platform repo |
| D4 | Branching model | **Trunk-based**: short-lived branches → PR → main |
| D5 | Environments | **dev → staging → prod**, as DABs targets mapped to Unity Catalog catalogs |
| D6 | Deployment identity | **Service principals** (one per environment; apps additionally run as their own SP for data access) |
| D7 | Promotion gates | **PR-gated** — all checks and review happen at PR. *Open item below.* |
| D8 | Secrets | **Databricks secret scopes** (runtime secrets; CI auth is OIDC per D1) |
| D9 | Job/app separation | **Jobs write, apps read.** Apps query gold via SQL warehouse (SELECT only); heavy/write work is always a job |
| D10 | Quality & security gates | **Reference pipeline (diagram 07)**: blocking PR checks <10 min (lint, secrets scan, tests, incremental SAST, SCA, Sonar new-code gate, bundle validate, policy-as-code on databricks.yml, review); integration verification in dev; async nightly for slow scans |

---

## Open item — D7: what triggers staging and prod?

"PR-gated" settles *what stands between a change and main* (checks + review). It does
not yet settle *what pushes a merged change onward*. Pick one:

- **(a) Merge to main → auto-deploy dev → auto staging → manual approve → prod**
  (continuous; every merge marches toward prod)
- **(b) Merge → auto dev; release tag → staging → manual approve → prod**
  (batched releases; rollback = redeploy prior tag) — *recommended: cleaner
  versioning and rollback story, matches diagram 07*
- Either way: name who can approve prod, and whether app deploys get a deploy
  window (apps restart a live service on deploy; jobs don't).

## Consequence of D3 (monorepo) — changes to the build plan

The templates (S2/S4) were scoped as separate starter repos. Monorepo changes their
shape, not their content:

- One repo, one folder per project, each folder its own bundle:

```
platform-monorepo/
  .github/workflows/        # shared pipelines, path-filtered per project
  libs/shared/              # D9 shared logic, built as a wheel
  projects/
    rva-ingestion/          # a job bundle (databricks.yml, src/, tests/)
    rva-dashboard/          # an app bundle
  policy/                   # policy-as-code checks for databricks.yml
  docs/                     # this doc + the two SOPs
```

- CI workflows use **path filters** so a PR touching `projects/rva-dashboard/` only
  runs that project's pipeline — without this, every PR runs everything and the
  <10-minute budget dies
- "Clone the template" in the SOPs becomes "copy the example project folder"
- One repo's permissions govern everyone — branch protection on main is now
  platform-wide policy
- Revisit trigger: if cross-team PR contention or CI queueing becomes chronic,
  split hot projects out (record as a new decision if it happens)

## What happens next

1. Close the D7 open item (one Slack thread, not a meeting)
2. Build the monorepo skeleton: shared workflow + one working job project
   (dev → staging → prod, end to end)
3. Write the job SOP as `docs/deploy-jobs.md`
4. Add the app example project + app SOP
5. Someone outside the platform team deploys using only the docs

## Enforcement

The policy-as-code check in CI asserts these decisions on every PR: run_as is a
service principal, required tags present, no PATs, app SPs hold SELECT-only on
gold. The standard is enforced by the pipeline, not by memory.
