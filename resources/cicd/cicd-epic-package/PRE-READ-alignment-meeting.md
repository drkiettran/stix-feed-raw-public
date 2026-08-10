# Pre-read: CI/CD Standardization — Alignment Meeting

**Please read before the meeting (~3 minutes). Full detail: `EPIC-BRIEF-cicd-standardization.md`.**

---

## Why we're meeting

We have a ticket to "complete our CI/CD deployment process plan" for Databricks jobs and apps. When we tried to make it implementable, it turned out to be three deliverables wearing one story's clothes — a standards decision, a job SOP, and an app SOP — with untestable acceptance criteria and several contested technical decisions hidden inside "documentation work."

The proposal: promote it to an epic with six child stories, and spend **this sprint on alignment, not implementation** — getting the technical decisions made and approved before anyone builds anything.

## The problem we're solving

Today teams deploy through manual UI edits, ad-hoc scripts, and personal access tokens. When a production job breaks, we cannot reliably answer *what changed, when, by whom, or how to roll back*. Deployments tied to individual credentials mean an offboarding can silently break production. The target state is one paved road: a template repo, a CI pipeline, service-principal identities, and SOPs a platform user can follow without our help.

## Proposed structure (19 pts, ~2 sprints of build after this one)

| # | Story | Pts |
|---|-------|-----|
| S1 | CI/CD standard (ADR): branching, environments, identity, secrets, gates, rollback, **CI tool selection** | 3 |
| S2 | Job template repo + CI pipeline | 5 |
| S3 | Job deployment SOP | 2 |
| S4 | App template repo + CI pipeline | 5 |
| S5 | App deployment SOP | 2 |
| S6 | Pilot: external team deploys using docs only; publish | 2 |

S1 blocks everything. S2→S3 and S4→S5 then run as parallel tracks. S6 gates the epic — an SOP nobody has followed unaided is a draft, not a standard.

## What we're asking for in the meeting

1. **Agree** to promote the story to an epic with the structure above
2. **Commit this sprint to S1 + refinement**: ADR drafted, reviewed, approved; S2–S6 re-estimated against it and made sprint-ready
3. **Name the ADR approvers** and put the review date on the calendar
4. **Schedule the CI tool decision** — one timeboxed meeting; comparison table in the brief is the agenda (GitHub Actions / Jenkins-CloudBees / Azure DevOps / GitLab CI)
5. **Identify who owns service-principal / IAM provisioning** — if it's external to us, we file the dependency ticket this week (this is historically where a 5-point story becomes a 13)
6. **Nominate the pilot team** for S6 so they can plan for it

## Come ready to discuss

- Any deployment incident or near-miss the current process caused or worsened — real examples strengthen the case with stakeholders
- Whether the decision surface in S1 is complete, or too broad (scope discipline: the ADR covers only what blocks S2–S6)
- Who outside this team must sign off before we can call the ADR approved

## What this sprint does *not* include

No templates, no pipelines, no SOPs get built this sprint. If the ADR lands early, S2 may start — but the gate is ADR approval, not the sprint boundary. Deciding slowly once beats building twice.
