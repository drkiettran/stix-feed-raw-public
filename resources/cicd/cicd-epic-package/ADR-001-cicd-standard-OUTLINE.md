# ADR-001: CI/CD Standard for Databricks Jobs and Apps

**Status:** Draft — target: Accepted by end of sprint
**Deciders:** _[named approvers from alignment meeting]_
**Date drafted:** _[date]_ · **Review date:** _[from calendar]_
**Traces to:** Epic "Standardized CI/CD for Databricks jobs and apps" — blocks S2–S6

---

## Format note

This record covers eight related decisions (D1–D8) in one document because they must be
made coherently — the identity model constrains the CI tool, the environment topology
constrains the promotion gates. Each decision section carries its own status and
alternatives, so any single one can be superseded later by a new ADR without reopening
the rest.

## Context

Teams currently deploy Databricks jobs and apps via manual UI edits, ad-hoc scripts,
and personal access tokens. There is no audit trail, no rollback path, no promotion
process, and deployments are tied to individual credentials. This ADR fixes the
foundational decisions that the job/app templates (S2, S4) and SOPs (S3, S5) will be
built on.

## In scope

The eight decisions below — and only what blocks S2–S6.

## Out of scope (future ADRs if needed)

- Workspace consolidation or multi-workspace strategy changes
- Testing standards (unit/integration test requirements for pipelines)
- Observability, alerting, and job monitoring standards
- Data quality gates and governance tiers for non-production work
- App-level UX/access design beyond deployment identity
- Migration plan for existing, already-deployed jobs (separate ticket once the
  standard exists)

---

## D1 — CI platform

**Status:** OPEN — to be settled in the timeboxed tool meeting
**Question:** Which CI system runs validate/deploy?
**Options:** GitHub Actions · Jenkins/CloudBees · Azure DevOps · GitLab CI
**Selection criteria (priority order):**
1. Already licensed and security-approved in the org
2. Supports secretless OIDC federation to Databricks service principals
3. Runner network reachability to the workspace (private link / IP ACLs)
4. Team familiarity
**Blocks:** S2, S4 (workflow syntax), D6 (auth mechanism)
**Decision:** _[record outcome + rejected options with reasons]_

## D2 — Deployment tool

**Status:** PROPOSED — Databricks Asset Bundles (DABs)
**Question:** What tool defines and deploys jobs/apps as code?
**Rationale:** First-party, declarative, per-target configuration, covers both jobs
and apps, active roadmap. CLI is the only local dependency.
**Alternatives considered:**
- Terraform provider directly — better for workspace/infra config; heavier for
  per-project job deployment; splits ownership between platform and product teams
- dbx — predecessor tooling, effectively superseded by DABs
- Manual/UI — the status quo this epic retires
**Blocks:** everything downstream
**Decision:** _[confirm]_

## D3 — Repository strategy

**Status:** PROPOSED — one repo per project, cloned from the platform template repo
**Question:** Monorepo vs. per-project repos?
**Rationale:** Per-project repos keep bundle scope, permissions, and CI triggers
simple; the template repo carries the standard. Monorepo revisit-able if project
count grows and drift becomes a problem.
**Alternatives:** platform monorepo (central control, but cross-team PR contention
and complex path-filtered CI)
**Blocks:** S2, S4 (template shape), S3/S5 (onboarding steps)
**Decision:** _[confirm]_

## D4 — Branching model

**Status:** PROPOSED — trunk-based: short-lived branches → PR → main; release tags
for prod
**Question:** What git workflow do the SOPs teach?
**Rationale:** Smallest teachable model; maps cleanly to the promotion flow in D7.
Gitflow adds ceremony without benefit at our team sizes.
**Alternatives:** gitflow; environment branches (dev/stage/prod branches — rejected:
drift and merge-order bugs)
**Blocks:** S3, S5 (SOP steps), D7
**Decision:** _[confirm]_

## D5 — Environment topology

**Status:** PROPOSED — dev / staging / prod as DABs targets mapped to Unity Catalog
catalogs (catalog-level isolation), not separate workspaces
**Question:** What are the deployment targets and how are they isolated?
**Rationale:** Catalog-level isolation is the dominant current pattern; two to three
deployed environments plus CI covers real needs without five-stage ceremony.
Per-target variables set catalog/schema; sandbox/exploratory work stays outside this
pipeline entirely (no CI/CD required for exploration — over-governance drives
evasion, not compliance).
**Open sub-question:** does staging deploy on every merge or on release-branch cut?
**Blocks:** S2, S4 (target blocks in databricks.yml), D7
**Decision:** _[confirm + record staging trigger]_

## D6 — Deployment identity

**Status:** PROPOSED — one service principal per environment; bundles run-as the SP;
CI authenticates via OIDC federation if D1 supports it, else stored OAuth secret
with documented rotation
**Question:** What identity deploys, and what identity do jobs/apps run as?
**Rationale:** Retires personal tokens; survives offboarding; per-environment SPs
give least-privilege blast radius. Apps additionally run with their own SP identity
for data access (grants to the app's SP, never the developer's).
**Alternatives:** shared single SP (rejected: prod blast radius); user PATs (the
status quo)
**External dependency:** SP provisioning + UC grants — tracked in the IAM
dependency ticket. Name the owner here: _[owner]_
**Blocks:** S2, S4; DEP ticket
**Decision:** _[confirm]_

## D7 — Promotion gates and versioning

**Status:** PROPOSED —
- PR: `bundle validate` as a required status check + peer review
- Merge to main: auto-deploy to dev
- Release tag (semver): deploy to prod behind a manual approval gate
- Rollback: redeploy the prior tag
**Question:** What triggers each environment's deploy, and what stands between a
change and prod?
**Open sub-questions:** who can approve prod deploys; is staging gated or automatic
(pairs with D5's open question)
**Blocks:** S2, S4 (workflow definition), S3, S5 (SOP promotion steps)
**Decision:** _[confirm + record approvers]_

## D8 — Secrets management

**Status:** OPEN
**Question:** Where do runtime secrets for jobs/apps live, and how do bundles
reference them?
**Options:** Databricks secret scopes (native, simple) · AWS Secrets Manager backed
scopes (central rotation/audit, one more IAM surface) — note this is runtime
secrets; CI-to-Databricks auth is D6's concern
**Blocks:** S2, S4 (template examples), S3, S5 (how users request a secret)
**Decision:** _[record]_

---

## Consequences (fill at acceptance)

- All new jobs/apps deploy via the template + pipeline; UI edits to prod resources
  are prohibited once migrated
- The Databricks CLI becomes a required local dev dependency
- Platform team owns the template repo and this standard; product teams own their
  project repos
- Existing deployed jobs are unaffected until the migration ticket (out of scope
  here) is planned

## Done criteria for this ADR (sprint exit)

1. D1 and D8 moved from OPEN to decided; D2–D7 confirmed or amended
2. Every decision has its alternatives-considered filled in (reviewers: engage with
   these specifically — agreement with what was *rejected* is what makes this stick)
3. Open sub-questions in D5/D7 answered and recorded
4. Approved by named deciders on or before the review date
5. IAM dependency ticket filed with a named owner (from D6)
