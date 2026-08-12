# databricks-dab-deployment-template

A reference **monorepo** showing how to structure, develop, test, and deploy Databricks **jobs** and **apps** using Databricks Asset Bundles (DABs). Copy a project folder to start a new workload; the deployment process documented here applies unchanged.

## What "CI/CD" means for this platform

Because the term gets used loosely, this repository defines it precisely as a two-phase maturity path:

**Phase 1 — Standardized deployment process (this repository, today).** Every job and app lives in a project folder with its own bundle, and every deployment follows the same documented steps: *validate → deploy → verify*. The steps are executed by a person following an SOP ([job SOP](docs/SOP-job-deployment.md), [app SOP](docs/SOP-app-deployment.md)).

**Phase 2 — The same process, enforced by a pipeline (future).** The identical steps run automatically: validation on every pull request, staging deployment on every merge, production deployment on an approved release — with **path filters** so a change to one project runs only that project's pipeline. Nothing about the repo changes between phases; the pipeline simply executes the SOPs so humans no longer have to.

An SOP is a promise that people will follow the steps; a pipeline is a system that makes the steps unavoidable. Each SOP step is annotated with the pipeline stage that will eventually own it, so the SOPs double as the requirements document for Phase 2.

## Monorepo layout

```text
databricks-dab-deployment-template/
├── README.md                   # start here
├── SETUP.md                    # publish checklist: every REPLACE- placeholder
├── libs/
│   └── shared_core/            # shared business logic — built as a wheel
│       ├── pyproject.toml
│       ├── src/shared_core/    #   pure logic (no Spark, no credentials)
│       └── tests/              #   the library tests itself
├── projects/
│   ├── sample-job/             # ONE bundle per project folder
│   │   ├── databricks.yml      #   own targets, own deploy scope
│   │   ├── resources/          #   Lakeflow Job definition
│   │   ├── src/sample_job/     #   project glue: entry point only
│   │   └── tests/              #   unit (glue) + integration (deployed job)
│   └── sample-app/
│       ├── databricks.yml
│       ├── resources/          #   Databricks App definition
│       ├── src/app/            #   app.py (glue) + helpers.py (testable logic)
│       │                       #   + app.yaml + requirements.txt
│       └── tests/              #   unit (helpers) + integration (app smoke)
├── scripts/
│   └── build_shared.sh         # builds shared_core wheel → consuming projects
└── docs/
    ├── SOP-job-deployment.md   # stage-by-stage, with diagrams + walkthroughs
    ├── SOP-app-deployment.md
    ├── architecture.md         # C4 views + dev-to-prod sequence (incl. rollback)
    ├── deployment-log.md       # Stage 3 record for every staging/prod deploy
    └── diagrams/               # all .puml sources + rendered .svg + regen guide
```

Three rules carry the design:

**One bundle per project folder.** Each project under `projects/` has its own `databricks.yml`, so its deploy scope is exactly itself: deploying `sample-job` can never touch `sample-app`'s resources, and (in Phase 2) a PR that changes only one project triggers only that project's pipeline. Adding a workload = copying a project folder and renaming the bundle.

**Shared logic lives in `libs/`, consumed as a wheel.** Business logic used by more than one surface is written once in `shared_core`, unit-tested where it lives, built by `scripts/build_shared.sh`, and *imported* by jobs (via task `libraries`) and apps (via `requirements.txt`) alike. Nothing is copy-pasted between projects, and nothing is `%run` from a notebook. The same discipline applies inside apps: `app.py` stays untestable UI glue, while logic worth asserting lives in `helpers.py` (app-local) or `shared_core` (shared) — both unit-tested.

**Nothing hardcodes an environment.** The catalog name flows in as a job parameter or app env var, resolved per-target by the `${var.catalog}` variable in each bundle. Environments are isolated at the **Unity Catalog catalog level** (`proj_dev` / `proj_stg` / `proj_prod`); the workspace is shared.

| Target | Mode | Deployed by | Catalog | Notes |
|---|---|---|---|---|
| `dev` | development | each developer (own identity) | `proj_dev` | resources auto-prefixed per user; schedules paused |
| `staging` | production | staging service principal | `proj_stg` | shared integration environment |
| `prod` | production | prod service principal | `proj_prod` | changes arrive only via the SOP / future pipeline |

For the zoomed-out system views — C4 context, C4 container, and a full dev-to-prod sequence including rollback — see [docs/architecture.md](docs/architecture.md).

## Quick start (dev)

> First time in this repo? Run through [SETUP.md](SETUP.md) once — the bundles contain `REPLACE-` placeholders (workspace URL, service principal IDs) that must be filled before anything deploys.

```bash
databricks auth login --host https://<workspace-url>

# 1. Build + distribute the shared library wheel
./scripts/build_shared.sh

# 2. Test the shared library where it lives
pip install -e "libs/shared_core[test]" && pytest libs/shared_core/tests

# 3. Work inside ONE project — every bundle command runs from its folder
cd projects/sample-job
pytest tests/unit
databricks bundle validate
databricks bundle deploy -t dev
databricks bundle run sample_job -t dev
```

For staging and prod, follow the SOPs in `docs/` — those environments are deployed only under service principals, never a personal identity.

## Adding a new project

Copy `projects/sample-job` (or `sample-app`), rename the folder and the `bundle.name`, replace the sample code, and keep the structure. If the new project needs shared logic, import it from `shared_core` — and if it needs logic that a *second* project will also want, the logic goes into `libs/shared_core`, not into the project.
