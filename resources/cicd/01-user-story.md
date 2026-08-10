# cicd processes for databricks:

## User Story:

```user story
As a platform admin,
I want to complete our ci/cd deployment process plan
So that, we have a standardized method for platform users to deploy both jobs and apps
```

## Acceptance criteria:

* Processed defined for cicd best practices & standardization
* Develop SOP for job deployment
* Develop SOP for APP deployment


Help me improve this user story and make it clear. Provide a rough estimate of time/story point to complete this. If necessary break the story down smaller so it can be implemented correct.

---
# Response:
## Thinking:
I'm looking at what needs improvement in this Databricks CI/CD user story—the acceptance criteria seem vague, particularly around the "platform admin" role and what success actually looks like. I should focus on clarifying the story's scope, breaking down the work into reasonable chunks with story points, and identifying what's missing before suggesting a breakdown. Dependencies and secrets handling differ between jobs and apps, and apps need their own deployment semantics. I should structure this as a spike to nail down the key decisions first, then break it into focused stories: a reference job implementation with DABs and CI, job deployment SOP, app deployment with its own reference repo and SOP, plus optional shared pipeline templates. I'll estimate this at roughly 18-21 points across 1.5-2 sprints for one engineer, and I should be explicit about the point scale assumption and include acceptance criteria that actually matter — like a dry run where someone unfamiliar with the work follows the SOP independently.

## What's wrong with the story as written

Three things keep it from being implementable:

**It's an epic, not a story.** "Define best practices" + "SOP for jobs" + "SOP for apps" are three different deliverables with different audiences and different done-conditions. A single story that produces three documents will sit in-progress for a sprint and a half.

**The acceptance criteria restate the title.** "Processes defined for CI/CD best practices & standardization" isn't testable — there's no way for someone in sprint review to say yes or no. AC should name artifacts and a verification event.

**It skips the decisions the SOPs depend on.** You can't write a job-deployment SOP until you've settled branching model, environment/target topology, identity (service principal vs. user PAT), secrets handling, and what gates promotion. Those are contested choices, not documentation work, and they belong in their own ticket so the debate doesn't stall the writing.

One more: an SOP nobody has followed is a draft. The real acceptance test is a platform user deploying something using only the doc, without you in the room.

---

## Rewritten as an epic

**Epic: Standardized CI/CD deployment for Databricks jobs and apps**

> As a platform admin, I want a documented, tooled CI/CD path for deploying Databricks jobs and apps, so that platform users deploy consistently and safely without inventing their own process — and so I can audit what's running in each environment.

**Epic-level done:**
- A written CI/CD standard exists and is approved by the platform team
- A template repo exists that a user can clone and deploy from
- SOPs for jobs and apps are published where users will find them
- At least one team outside the platform team has deployed via the SOP unaided

---

## Child stories

**1. Decide and record the CI/CD standard (spike → ADR)** — **3 points**

> As a platform admin, I want the foundational CI/CD decisions recorded in an ADR, so that the SOPs and templates are built on settled choices rather than re-litigated per team.

AC:
- ADR covers: repo strategy (mono vs. per-project), branching model, environment targets (dev/staging/prod) and their catalog mapping, deployment identity (service principal per target), secrets management, promotion gates, versioning/tagging, rollback approach
- Explicitly names the CI platform and the deployment tool (assuming DABs — say so and say why, including what was rejected)
- Reviewed and approved by platform team; open questions listed with owners

**2. Reference bundle + CI pipeline for a job** — **5 points**

> As a platform user, I want a working template repo for a Databricks job, so that I start from a validated bundle instead of a blank directory.

AC:
- `databricks.yml` with dev/staging/prod targets, service principal run-as, per-target catalog/schema variables
- CI workflow: validate on PR, deploy to dev on merge, deploy to prod on tag/release
- Repo includes a trivial working job that runs end-to-end in dev and prod
- README covers first-time setup (auth, CLI version, required permissions)

**3. SOP: job deployment** — **2 points**

> As a platform user, I want a step-by-step deployment procedure, so that I can ship a job without asking the platform team.

AC:
- Covers onboarding a new project, local `bundle validate`/`deploy`, PR → dev → prod flow, how to request a service principal and permissions, rollback steps, three most common failure modes with fixes
- Links to the template repo rather than duplicating its content
- Published in the team's documented location (wiki/Confluence/repo docs — pick one)

**4. Reference bundle + CI pipeline for a Databricks App** — **5 points**

> As a platform user, I want a working template for deploying a Databricks App, so that app deployment follows the same guardrails as job deployment.

AC:
- Bundle defines the app resource, its compute, and its resource grants (SQL warehouse, secrets, UC objects)
- CI workflow handles source sync + `apps deploy`, not just bundle deploy
- Dependency pinning (`requirements.txt` or equivalent) documented
- Deployed to dev and prod successfully

*Note: this is separately estimated because app deploy semantics differ enough from jobs that reusing the job pipeline verbatim won't work — app permissions and the source-sync step are the sharp edges.*

**5. SOP: app deployment** — **2 points**

Same shape as #3, plus: app URL/access model, who can view vs. manage, and how app-to-data permissions are granted (app service principal identity, not the developer's).

**6. Pilot validation and publish** — **2 points**

> As a platform admin, I want an external team to deploy using only the SOPs, so that I know the docs work before I mandate them.

AC:
- One non-platform engineer deploys a job and an app following the docs only
- Every point where they got stuck is logged and either fixed in the doc or filed
- SOPs announced to platform users with a stated adoption expectation

---

## Estimate summary

| Story | Points |
|---|---|
| 1. CI/CD standard ADR | 3 |
| 2. Job template + pipeline | 5 |
| 3. Job SOP | 2 |
| 4. App template + pipeline | 5 |
| 5. App SOP | 2 |
| 6. Pilot validation | 2 |
| **Total** | **19** |

Scale I'm assuming: 1 ≈ half a day, 2 ≈ a day, 3 ≈ two days, 5 ≈ three to four days. So roughly **2–3 weeks of focused work for one engineer**, or a single sprint for two people since stories 2/3 and 4/5 parallelize cleanly after the ADR lands. Your team's velocity is the real calibration — treat these as relative sizes, not a schedule.

**Sequencing:** story 1 blocks everything. Stories 2→3 and 4→5 are pairs (build it, then document it — writing the SOP before the template exists produces fiction). Story 6 gates the epic.

**Estimate risk sits in stories 2 and 4**, and it's almost always the same thing: getting service principals provisioned with the right cross-workspace and Unity Catalog grants, and whatever your CI platform needs to authenticate to Databricks (OIDC federation vs. stored secrets). If that provisioning goes through a separate cloud/IAM team, pull it out as its own dependency ticket now — I've seen it turn a 5 into a 13.
