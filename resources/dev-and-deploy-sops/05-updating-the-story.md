Here's the reframe, built on one principle: nobody's original words get corrected — they get *fulfilled and extended*. The customer asked for a documented CI/CD process; your TL asked for a template repo with SOPs. Both of those things now exist, so the updated story's job is to describe what was delivered in language that makes their asks look prescient, and to let the Phase 2 story carry the forward vision they get credit for starting.

**Updated story (Phase 1 — this is the one that closes):**

> **As a** platform admin,
> **I want** to establish our standardized deployment process for Databricks jobs and apps — Phase 1 of our CI/CD roadmap —
> **So that** platform users have one clear, documented, repeatable method for deploying to production, and a defined path to full automation.
>
> **Acceptance criteria:**
> * CI/CD is defined for our platform as a two-phase maturity path (standardized process → pipeline-enforced process), documented in the template repository's README
> * Deployment standards defined: monorepo layout, one bundle per project, shared library as a wheel, three-target model (dev/staging/prod), service-principal identity
> * Template repository `databricks-dab-deployment-template` published with a working sample job and sample app demonstrating the standard structure
> * SOP for job deployment, written for users new to Databricks, with visual walkthroughs
> * SOP for app deployment, likewise, covering app-specific identity and verification
> * Architecture views published (C4 context/container, end-to-end delivery sequence including rollback)
> * Each SOP step annotated with the Phase 2 pipeline stage that will automate it, so the SOPs double as the automation requirements
> * Phase 2 story captured in the backlog

Notice what the ACs do quietly. The original three ACs are all still in there — "process defined," "SOP for jobs," "SOP for apps" — just sharpened into verifiable artifacts, so the customer and TL can see their exact asks satisfied line by line. The word "CI/CD" survives, now attached to a definition instead of floating. And the last two ACs are the bridge: they make Phase 2 an *outcome of this story* rather than a criticism of it.

**Companion story (Phase 2 — sits in the backlog, unscheduled is fine):**

> **As a** platform admin,
> **I want** the standardized deployment process automated as a CI/CD pipeline — Phase 2 of the roadmap —
> **So that** the process defined in Phase 1 is enforced by the platform on every change, rather than depending on manual adherence to the SOPs.
>
> **Acceptance criteria:**
> * PR checks run automatically: shared-library build, unit tests, `bundle validate` for all targets, with path filters scoping checks to changed projects
> * Merge to `main` auto-deploys to staging and runs the integration verification from the SOPs
> * Approved release tag deploys to prod with post-deploy verification; failed verification blocks release completion
> * SP credentials held as environment-scoped pipeline secrets; humans no longer handle them
> * Both SOPs updated: manual execution steps retired, documents retained as the process-of-record the pipeline implements

The Phase 2 ACs are deliberately just the ⚙ annotations from your SOPs, promoted to acceptance criteria — which lets you say, truthfully, "the requirements for Phase 2 were written the day we wrote the SOPs."

For the conversation itself — sprint review or wherever you present this — the framing sentence that brings both of them along is some version of: *"The story you wrote turned out to be the foundation of something bigger — it defined Phase 1 of our CI/CD roadmap, and it produced the requirements for Phase 2 for free."* That sentence gives the customer authorship of the roadmap, gives your TL the template repo he asked for as the centerpiece deliverable, and positions the automation work as the natural continuation of *their* initiative rather than a correction of it. If you show one artifact in that meeting, show the sequence diagram (08) and point at the two lifeline sections where the engineer gets replaced by CI — Phase 1 and Phase 2 in a single picture, no slides needed.

One small honest caution: don't oversell the "for free" — Phase 2 is still real work (the two-sprint, ~19-point shape you scoped in the ADR process is the honest estimate). The gift Phase 1 gives is *clarity of requirements*, not reduced effort, and saying it that way keeps your conservative-planning credibility intact.

---
Honest status, AC by AC — and since you value gap-flagging, I'll separate "artifact exists" from "artifact accepted," because a few of these are done in the first sense but not the second.

| # | Acceptance criterion | Status | What's actually left |
|---|---|---|---|
| 1 | Two-phase CI/CD definition in the README | ✅ Done | Nothing in the artifact — but it hasn't been *seen* by the customer or TL yet, and this AC only does its political work once they've read and nodded at it |
| 2 | Deployment standards defined | ✅ Mostly | Documented in the README and embodied in the repo structure. Gap: not cross-referenced to your ADR-001 (D1–D10). One line in the README — "these standards implement decisions D2–D8 of ADR-001" — turns documentation into ratified standard and connects this deliverable to the decision record your team already approved |
| 3 | Template repo published with working samples | 🟡 Partial | The content is complete on your side, but "published" isn't true yet: the zip hasn't been pushed to the GitHub repo, the repo name typo (`deploymnet-tempate`) needs fixing first, and the `REPLACE-` placeholders in SETUP.md must be filled before anyone can clone-and-run. And "working" is unverified even setting aside general testing — specifically: the sample job reads `<catalog>.bronze.findings`, a table nobody has created; the integration test has a placeholder warehouse ID; and the app's `requirements.txt` pins a wheel filename that must match what `build_shared.sh` produces |
| 4 | Job SOP with visual walkthroughs | ✅ Done | Artifact-complete. Pending: one real newcomer walks it end to end — that's part of the validation you've already set aside, but worth naming as the SOP-specific version of it |
| 5 | App SOP | ✅ Done | Same caveat; also the "approved secret store" placeholder needs your actual store's name |
| 6 | Architecture views published | ✅ Done | C4 context, C4 container, delivery sequence, plus the five conceptual diagrams — all with sources and reading guides |
| 7 | SOP steps annotated with Phase 2 stages | ✅ Done | Every stage in both SOPs carries its ⚙ annotation; Phase 2's ACs were derived directly from them, closing the loop |
| 8 | Phase 2 story captured in backlog | ❌ Not done | The story text exists only in this conversation. It needs to be entered in your tracker — and ideally linked from the Phase 1 story — before Phase 1 can honestly claim this AC. Five minutes of work, but it's the AC most likely to be forgotten, and it's the one carrying the entire forward-thinking framing |

So the summary you can give verbally: **six of eight ACs are artifact-complete; the two open items are publishing (push to the correctly-named repo with placeholders filled) and backlog capture of Phase 2.** Both are administrative rather than creative work — there's nothing left to *build* for Phase 1.

One thing that isn't an AC but belongs in your definition of done: **review and acceptance.** Several ACs use words like "defined" and "published," which implicitly mean *agreed*, not just written. A short review pass — TL reads the SOPs, customer sees the README's two-phase definition and the sequence diagram — is what converts these artifacts from "what Kiet built" into "what we decided." Given the dynamics we discussed, that review is also your moment: it's where the customer discovers their story became a roadmap, and where your TL sees the template repo he asked for, delivered better than he specified. I'd sequence it deliberately: fix the repo name, push, fill placeholders, enter the Phase 2 story, *then* invite the review — so what they see is finished, not almost-finished.
