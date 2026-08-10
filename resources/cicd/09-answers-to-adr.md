# Request:
Meeting was held. Answers to the 10 questions:
D1: CI Platform: Github Actions
D2: Deployment tool: DAB
D3: Repo strategy: Monorepo
D4: Branching model: Short lived branches -> PR -> Main
D5: Environment topology: Dev -> Staging -> Production
D6: Deployment entity: Service principle
D7: Promotion gates & versioning: PR
D8: Secret management: Databricks secret scopes.
D9: Job/app separation of concerns: jobs write, apps read
D10: Pipeline quality & Security gates: Proposed standard (see reference pipeline diagram 07)

# Response:
Congrats — you have a standard now. Let me lock these into the decisions doc and flag the two things in your answers that change the plan or need one more call.The decisions doc is finalized — two pages, table format, no ceremony. Ten decisions recorded, and the standard is done except for two things worth your attention:

**One genuine open item: D7 is half-answered.** "PR" settles what stands between a change and main — checks plus review. It doesn't settle what pushes a *merged* change to staging and prod. The doc lays out the two options: (a) every merge auto-marches dev → staging → manual approval → prod, or (b) merge auto-deploys dev, and a release tag drives staging → approval → prod. I'd recommend (b) — it gives you a clean versioning and rollback story ("redeploy the prior tag") and matches the reference pipeline your team just approved in D10. Either way, two names need attaching: who can approve prod, and whether app deploys get a deploy window since they restart a live service. This is a Slack-thread decision, not another meeting.

**One choice that changes the build plan: monorepo.** The team went the other way from the per-project proposal, which is fine — but it reshapes what "template" means. Instead of a starter repo people clone, you build one platform repo where each project is a folder with its own bundle, and CI uses **path filters** so a PR touching only `projects/rva-dashboard/` runs only that project's pipeline. That path-filtering isn't optional polish — without it every PR runs every project's checks and your ten-minute budget is dead by the third project. The doc includes the proposed folder layout and a revisit trigger (if cross-team PR contention gets chronic, split hot projects out and record it as a new decision).

The "what happens next" list is now five concrete items, and item 2 is where the real work starts: the monorepo skeleton with one working job project deploying dev → staging → prod end to end.

That skeleton is something I can build with you when you're ready — the repo structure, the shared GitHub Actions workflow with path filtering and OIDC auth, a working `databricks.yml` with your three targets, and the policy-as-code checks that enforce D5–D7. Say the word and we start there.
