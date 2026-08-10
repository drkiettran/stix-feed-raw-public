# CI/CD Standard for Databricks Jobs and Apps — Decisions

**Status:** Accepted (team meeting, 2026-08-10; D7 promotion flow finalized 2026-08-10)
**Applies to:** all new job and app deployments on the platform

| # | Decision | Choice |
|---|---|---|
| D1 | CI platform | **GitHub Actions** (OIDC federation to Databricks service principals — no stored secrets for CI auth) |
| D2 | Deployment tool | **Databricks Asset Bundles (DABs)** |
| D3 | Repo strategy | **Monorepo** — all job/app projects live as folders in one platform repo |
| D4 | Branching model | **Trunk-based**: short-lived branches → PR → main |
| D5 | Environments | **dev → staging → prod**, as DABs targets mapped to Unity Catalog catalogs |
| D6 | Deployment identity | **Service principals** (one per environment; apps additionally run as their own SP for data access) |
| D7 | Promotion gates | **PR-gated + continuous promotion**: checks and review at PR; merge to main → auto-deploy dev → auto-deploy staging → manual approval → prod |
| D8 | Secrets | **Databricks secret scopes** (runtime secrets; CI auth is OIDC per D1) |
| D9 | Job/app separation | **Jobs write, apps read.** Apps query gold via SQL warehouse (SELECT only); heavy/write work is always a job |
| D10 | Quality & security gates | **Reference pipeline (diagram 07)**: blocking PR checks <10 min (lint, secrets scan, tests, incremental SAST, SCA, Sonar new-code gate, bundle validate, policy-as-code on databricks.yml, review); integration verification in dev; async nightly for slow scans |

---

## D7 promotion flow (decided)

**Merge to main → auto-deploy dev → auto-deploy staging → manual approval → prod.**
Continuous promotion: every merge marches toward prod; the only human gate is the
prod approval. Consequences to build in:

- **Staging must stay healthy** — it deploys on every merge, so the integration
  verification (job sample run / app health check) runs in dev *before* staging
  promotion; a failed dev verification stops the march
- **Rollback = revert the commit and let the pipeline redeploy** (no release tags
  in the flow; git history is the version history). Tag prod deploys automatically
  at approval time so "what is running in prod" stays answerable
- **Actions to close out:** name who can approve prod (GitHub environment
  protection rule on the prod environment), and decide whether app approvals are
  held to a deploy window since app deploys restart a live service

## Consequence of D3 (monorepo) — changes to the build plan

The templates (S2/S4) were scoped as separate starter repos. Monorepo changes their
shape, not their content:

- One repo, one folder per project, each folder its own bundle:

```
platform-monorepo/
  .github/workflows/        # shared pipelines, path-filtered per project
  libs/shared/              # D9 shared logic, built as a wheel
  projects/
    example-job/            # a job bundle (databricks.yml, src/, tests/)
    example-app/            # an app bundle
  policy/                   # policy-as-code checks for databricks.yml
  docs/                     # this doc + the two SOPs
```

- CI workflows use **path filters** so a PR touching `projects/example-app/` only
  runs that project's pipeline — without this, every PR runs everything and the
  <10-minute budget dies
- "Clone the template" in the SOPs becomes "copy the example project folder"
- One repo's permissions govern everyone — branch protection on main is now
  platform-wide policy
- Revisit trigger: if cross-team PR contention or CI queueing becomes chronic,
  split hot projects out (record as a new decision if it happens)

## What happens next

1. Set the prod approval rule in GitHub (environment protection: named approvers) and decide the app deploy-window question
2. Build the monorepo skeleton: shared workflow + one working job project
   (dev → staging → prod, end to end)
3. Write the job SOP as `docs/deploy-jobs.md`
4. Add the app example project + app SOP
5. Someone outside the platform team deploys using only the docs

## Enforcement

The policy-as-code check in CI asserts these decisions on every PR: run_as is a
service principal, required tags present, no PATs, app SPs hold SELECT-only on
gold. The standard is enforced by the pipeline, not by memory.
