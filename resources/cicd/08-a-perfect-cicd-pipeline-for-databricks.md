# Request:
What are the tools on the cicd pipeline do you have in mind? For example, source code scan like cxone, binary/image scan like twistlock, code quality scan like sonarqube, etc... Show me a `perfect` pipeline that would include all necessary tools to take care of the deployment of jobs and apps for databricks platform.

# Response:
Another honest gap-check first: no, the pipeline so far only had `validate → deploy → gates`. Security and quality stages weren't in the brief or the ADR — and unlike the two-phase-deploy mechanics, these *are* decisions. Let me answer in three parts: what the tool categories map to on Databricks (some of your examples change shape), the reference pipeline, and the ADR amendment.

## The tool categories, Databricks-shaped

One of your three examples doesn't transfer directly, and it's worth being precise about why:

| Category | Example tools | Databricks reality | Gate |
|---|---|---|---|
| Secrets detection | gitleaks, TruffleHog | **The highest-value scan for this platform.** Notebooks and configs are where PATs and connection strings leak — the exact credential style your epic is retiring | Blocking on PR |
| SAST | CxOne/Checkmarx, Semgrep, Bandit | Applies normally — it's Python. Run incremental on PR, full scan nightly/on-main (full CxOne scans are too slow to block a PR) | Blocking (incremental) |
| SCA / dependency scan | Snyk, pip-audit, Safety, Renovate | **This replaces most of what Twistlock does elsewhere.** Standard jobs and serverless apps have no container image you build — your artifact is a wheel plus `requirements.txt`. Scan the dependency tree, not an image | Block on critical CVEs |
| Image scan | Twistlock/Prisma, Trivy | **Only if** you use Databricks Container Services with custom images. If not — and most bundles don't — this stage has nothing to scan. Don't add it for ceremony | Conditional |
| Code quality | SonarQube | Applies normally: coverage threshold + new-code quality gate. Point it at `src/`, not notebooks | Blocking (new code only) |
| Unit tests | pytest + coverage | Applies normally; the shared-logic wheel from D9 is what makes the code testable off-cluster | Blocking |
| Config/policy-as-code | `bundle validate` + OPA/conftest or pytest checks on `databricks.yml` | **The stage unique to this platform**, and the one that enforces your ADR: assert `run_as` is an SP, tags present, cluster policy compliant, no interactive clusters in prod targets. Without this, ADR decisions D5–D7 are suggestions | Blocking |
| Integration test | Trigger job run w/ sample data; app health check | Runs against the dev target post-deploy — the only place Spark logic really gets validated | Blocking for promotion |
| DAST | OWASP ZAP baseline | Marginal for apps behind Databricks SSO; worthwhile only if apps are exposed more broadly | Optional |
| SBOM / license | Syft/CycloneDX, FOSSA | Cheap to emit at release; do it if compliance ever asks | Advisory |

The design principle threading through the gates column is your own over-governance one: **blocking PR checks must stay under ~10 minutes or engineers route around them**. Anything slower runs async on main or nightly and files findings instead of blocking merges.

Here's the full reference pipeline:Renders cleanly. Now the ADR amendment — D10:Everything's in the package: diagram 07 (the full reference pipeline, `.puml` + SVG) and a new **D10** in the ADR covering the stage list, with tool selections left OPEN under the same rule as D1 — whatever the org already licenses and security has approved wins. The done criteria and out-of-scope list are reconciled (testing standards *beyond* the D10 gates stay future-ADR).

The "perfect" pipeline, stage by stage:

**PR (blocking, <10 min total):** lint/format → secrets scan → unit tests + coverage → incremental SAST → SCA with critical-CVE block → Sonar new-code gate → `bundle validate` across all targets → **policy-as-code on `databricks.yml`** → peer review.

**Merge → dev (automatic):** build the shared-logic wheel → deploy → integration verification, forked per resource type: jobs get a sample-data run asserted to succeed; apps get deploy + start + health check + smoke test.

**Tag → prod (gated):** manual approval (with the D7 app-deploy-window caveat annotated right on the gate) → deploy → post-deploy verification → SBOM + release record.

**Async/nightly (never blocking):** full SAST on main, license scan, Renovate, full Sonar analysis.

Three things I'd defend in front of your team:

**Twistlock mostly doesn't apply here, and that's worth saying out loud.** Standard bundles produce wheels, not container images — there's no image to scan unless you adopt Databricks Container Services. The security value Twistlock provides elsewhere is delivered here by SCA on the dependency tree plus the SBOM at release. Adding an image-scan stage with nothing to scan is exactly the ceremony-for-ceremony's-sake your governance philosophy warns against.

**The policy-as-code check is the most important stage nobody will have heard of.** CxOne and Sonar find code problems; the `databricks.yml` policy check (run_as must be an SP, required tags, cluster policy, no PATs) is what makes ADR decisions D5–D7 *machine-enforced* rather than documented hopes. It's also nearly free — a handful of assertions with conftest or plain pytest.

**"Perfect" is calibrated by the evasion test, and D10 says so in its alternatives-considered:** scan-everything-blocking was rejected because PR latency drives evasion, and no-security-stages was rejected because retiring PATs while letting secrets merge into git is incoherent. The <10-minute blocking budget is the line between a pipeline people use and a pipeline people work around — which is your criticality-tier principle applied to the pipeline itself.

One sequencing note for S2/S4: build the skeleton first (validate → deploy → verify), then add gates incrementally — secrets scan and policy-as-code first since they're cheap and highest-value, SAST/Sonar once the tool licensing question from D10 resolves. A template that ships with nine half-configured scanners is worse than one that ships with four working ones.
