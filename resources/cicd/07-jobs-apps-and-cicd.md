# Request:
Tell me the differences between jobs and applications in databricks. Can they both share the same logic/processing criteria using the same cicd pipeline?

# Response:
The cleanest mental model: a **job is a verb, an app is a place**. A job is finite work that starts, runs, and ends — triggered by a schedule, a file arrival, or an API call. An app is a long-running web service that sits there waiting for humans — a Streamlit dashboard, a Dash tool, a Flask/Gradio interface — served continuously until you stop it.

That one difference drives everything else:

| Dimension | Job (Lakeflow Job) | Databricks App |
|---|---|---|
| Execution | Batch/streaming runs; terminates | Long-running HTTP server; restarts on deploy |
| Compute | Job clusters or serverless, spun up per run | Dedicated serverless app compute, always on while running |
| Trigger | Schedule, file arrival, API, upstream task | A human opening a URL |
| Identity | Run-as service principal per run | App's own service principal, persistent — data grants go to *it* |
| Failure model | Run fails → retry/alert; next run picks up fixes | App crashes → users see an outage; deploy = restart |
| Consumers | Tables, downstream tasks | People, interactively |
| Cost | Pay per run | Pay while deployed/serving |
| Typical role in your architecture | Bronze/silver/gold processing — your Auto Loader ingestion is a job | The window *onto* gold tables |

## Sharing logic

Yes — but with a strong architectural preference for *how*. Two legitimate patterns:

**1. Shared code, packaged once.** Put transformation logic in a Python package inside the repo (`src/rva_pipeline/`), and both the job's tasks and the app import from it. The job calls `transform_findings(df)` in a scheduled run; the app calls the same function on a user-uploaded sample. Same code path, one place to fix bugs. DABs handles this naturally — the bundle can build the wheel and both resources depend on it.

**2. Shared *data*, not shared execution — usually the better answer.** The job does the heavy processing and writes gold tables; the app just queries those tables through a SQL warehouse. The app stays thin (query + render), and you never have a dashboard silently running its own variant of the pipeline. For your RVA case this is almost certainly the right split: Auto Loader → bronze → silver → gold is job territory; an app visualizing assessment findings reads gold and does nothing else.

The anti-pattern is an app that *does* the processing — re-deriving metrics with its own logic, or worse, writing tables. Then you have two sources of truth and the app's compute becomes an unaudited pipeline. If an app genuinely needs to kick off heavy work, the clean pattern is the app triggering the *job* via the SDK, not doing the work in-process.

## Same CI/CD pipeline?

**Yes at the skeleton, no at the last mile** — which is precisely why your epic has S2 and S4 as separate stories sharing one ADR.

What's identical: repo-from-template, branching model, `bundle validate` on PR, deploy-to-target on merge/tag, service-principal auth, promotion gates. A single DABs bundle can even declare both a job resource and an app resource and deploy them together with one `bundle deploy` — same targets, same variables, same pipeline run. Everything ADR-001 decides applies to both.

What differs at the end of the pipeline:

1. **Apps have a two-phase deploy.** `bundle deploy` uploads source and updates the app's configuration, but the app serves the new code only after the app deployment/start step runs. The job equivalent doesn't exist — a job's next run just picks up the new definition. So S4's CI workflow has an extra step S2's doesn't.
2. **Deploy semantics are user-facing.** Deploying a job mid-day is invisible; deploying an app restarts the serving process while someone may be mid-dashboard. Your promotion gates (D7) might reasonably be *stricter or time-windowed* for apps.
3. **Identity and grants differ.** The job runs as the deployment-target SP you configured; the app has its own SP that needs explicit grants on the warehouse and tables it reads. That's a provisioning step in S4/S5 with no S2 analog — and another line item for the IAM dependency ticket.
4. **Verification differs.** "Did the job deploy work" = trigger a run, check it succeeds. "Did the app deploy work" = hit the URL, check health. S6's pilot should exercise both.

So the honest one-liner for your team: **one standard, one pipeline skeleton, two deployment tails** — roughly 80% shared, and the ADR governs all of it; S2 and S4 exist to build the two 20%s.



---
Honestly — only partially. Auditing the outline against what I just told you:

**Covered:** D6 has the app identity distinction (app runs as its own SP; grants go to the app's SP, never the developer's) and ties it to the IAM dependency ticket. That's the most consequential difference, and it's in.

**Not covered:**

1. **App deploys are user-facing restarts.** D7's promotion gates are written job-shaped — nothing acknowledges that a prod app deploy interrupts live users, which might warrant stricter or time-windowed gates for apps. This is a genuine D7 gap.
2. **The job/app separation of concerns** — jobs write, apps read gold; heavy work triggered as jobs, never done in-app. That's an architectural standard worth deciding *once*, and it's not in the ADR at all. Without it, nothing stops the first app from becoming a shadow pipeline.
3. **Whether one bundle may declare both a job and an app.** D3's repo strategy is silent on it — a small thing, but it shapes the S2/S4 templates.
4. The two-phase app deploy mechanic itself I'd deliberately *leave out* — that's implementation detail for S4's workflow, not a decision. ADRs record choices, not mechanics.

Gaps 1–3 are decisions, so they belong. Let me amend:The outline is now nine decisions, with three amendments from that gap analysis:

**D7 gained an open sub-question:** whether prod *app* deploys need stricter handling than jobs, since an app deploy restarts a service users may be mid-session in while a job deploy is invisible — with three candidate answers (same gate / deploy window / announce-then-deploy) so the review discussion has options rather than a blank.

**D3 gained a sub-decision:** one project bundle may declare both a job and an app, deployed together, with shared logic packaged once as a wheel both import. Templates stay separate as starting points; combining is supported, not forbidden.

**D9 is new — "jobs write, apps read":** apps are thin consumers of gold via SQL warehouse; heavy or write-path work is always a job (triggered via SDK if app-initiated); shared logic lives in a library, never re-implemented per surface. Its "Blocks:" line makes it enforceable rather than aspirational — S4's template defaults to read-only warehouse access, and S5's SOP grants app SPs `SELECT` on gold, not `MODIFY`. The permission model *is* the enforcement mechanism.

Counts and done criteria are updated throughout, and the zip is rebuilt.

Worth saying: this is exactly why the "did you actually write that down?" check matters before review, and your question caught a real one — D9 in particular is the kind of principle everyone nods along to in a meeting and nobody records, and then eighteen months later there's a dashboard writing tables and no document to point at. Now there will be.
