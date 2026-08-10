# Epic Brief: Standardized CI/CD for Databricks Jobs and Apps

**Audience:** platform team + stakeholders
**Author:** Platform Admin
**Status:** Proposed — requesting agreement to promote the existing story to an epic and adopt the breakdown below.

---

## Why this matters (the 2-minute version)

Today there is no standard way to get a job or an app into our Databricks environment. Teams deploy through manual UI edits, ad-hoc scripts, and personal access tokens. Each of those paths works — until it doesn't. When a production job breaks, we cannot reliably answer *what changed, when, who deployed it, and how do we roll back*. Deployments tied to individual credentials also mean a single offboarding can silently break production.

This epic replaces those paths with **one paved road**: a template repository, a CI pipeline, service-principal identities, and step-by-step SOPs that any platform user can follow without asking the platform team for help. It is the difference between the platform team *reviewing every deployment forever* and the platform team *building the road once*.

![Current state](diagrams/01-current-state.svg)

![Target state](diagrams/02-target-state.svg)

---

## Why the original story becomes an epic

The original ticket read:

> *As a platform admin, I want to complete our CI/CD deployment process plan, so that we have a standardized method for platform users to deploy both jobs and apps.*

Three problems surfaced when we tried to make it implementable:

1. **It bundles three distinct deliverables** — a decision on standards, a job SOP, and an app SOP — each with its own audience and its own definition of done. A single story carrying all three would stay in-progress for weeks with no demonstrable increments.
2. **Its acceptance criteria weren't testable.** "Processes defined for CI/CD best practices" gives a reviewer nothing to verify. Testable criteria name artifacts (an ADR, a template repo, a published SOP) and a verification event (an external team deploys using only the docs).
3. **It hid contested decisions inside documentation work.** You cannot write a deployment SOP before settling the branching model, environment targets, deployment identity, secrets handling, and promotion gates. Those decisions deserve their own ticket (an ADR) so debate doesn't stall the writing.

Promoting the story to an epic makes each deliverable independently reviewable, lets two engineers work in parallel, and surfaces the external IAM dependency *before* it blows up a sprint.

---

## The breakdown

![Epic breakdown](diagrams/03-epic-breakdown.svg)

| # | Story | Points | Deliverable |
|---|-------|--------|-------------|
| S1 | CI/CD standard (ADR) | 3 | Approved decision record: branching, targets, identity, secrets, gates, rollback |
| S2 | Job template + pipeline | 5 | Working reference repo: DABs bundle, CI validate/deploy, dev→prod |
| S3 | Job deployment SOP | 2 | Published step-by-step doc linked to the template |
| S4 | App template + pipeline | 5 | Working reference repo for Databricks Apps incl. grants + source sync |
| S5 | App deployment SOP | 2 | Published doc incl. app identity and access model |
| S6 | Pilot validation + publish | 2 | External team deploys unaided; docs fixed and announced |
| | **Total** | **19** | |

**Sequencing:** S1 blocks everything and should be approved as a team. After it lands, Track A (S2→S3) and Track B (S4→S5) run in parallel. S6 gates the epic — an SOP nobody outside the platform team has followed is a draft, not a standard.

**Known risk:** service-principal provisioning and CI-to-Databricks authentication (OIDC vs. stored secrets). If this is owned by a separate cloud/IAM team, we split it into its own dependency ticket now — this is historically where a 5-point story becomes a 13.

---

## What the finished road looks like

The sequence below is what the SOPs will document. This is the flow every job and app deployment follows once the epic ships.

![Deployment flow](diagrams/04-deployment-flow.svg)

---

## Indicative timeline

Roughly one sprint with two engineers; 2–3 weeks for one. Relative sizing — calibrate to team velocity.

![Timeline](diagrams/05-timeline.svg)

---

## The ask

1. Agree to promote the story to an epic with the six child stories above.
2. Nominate reviewers for the ADR (S1) — target approval within the first two days.
3. Identify the pilot team for S6 now, so they can plan for it.
4. Confirm who owns service-principal provisioning; if external, we file the dependency ticket this week.

---

## Appendix: diagram sources

All diagrams are PlantUML. Source files live in `diagrams/*.puml`; regenerate with:

```bash
java -jar plantuml.jar -tsvg diagrams/*.puml
```

Embed in Markdown with standard image syntax, e.g. `![Epic breakdown](diagrams/03-epic-breakdown.svg)`.
