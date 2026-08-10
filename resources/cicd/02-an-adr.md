An ADR is an **Architecture Decision Record** — a short document that captures a single significant technical decision: what you decided, why, what alternatives you considered, and what the consequences are.

The typical structure is just a few sections:

**Title** — "ADR-007: Use Databricks Asset Bundles for all job and app deployments"

**Status** — proposed / accepted / superseded

**Context** — the forces at play. What problem are you solving, what constraints exist? *"Teams currently deploy jobs manually through the UI or ad-hoc scripts. We need a standard mechanism that supports dev/staging/prod promotion and works with our CI platform."*

**Decision** — the choice itself, stated plainly. *"We will use DABs with per-environment targets, deployed by service principals via GitHub Actions."*

**Consequences** — what follows, good and bad. *"All projects must adopt the bundle structure; the CLI becomes a required local dependency; Terraform-managed workspace config stays out of scope."*

**Alternatives considered** — what you rejected and why (Terraform provider directly, dbx, manual UI deployment). This section is the one that pays off most later — it stops the "why didn't we just use X?" conversation from restarting every six months.

The key idea is that each ADR covers *one* decision, it's numbered, and it's immutable-ish: if you change your mind later, you write a new ADR that supersedes the old one rather than editing history. Over time the collection becomes a decision log for the platform — a new engineer can read ADR-001 through ADR-015 and understand not just how the system works but why it's shaped that way.

That's why I suggested it for story 1 of your epic. The branching model, identity model, and promotion-gate choices are exactly the kind of contested decisions that get re-argued forever if they only live in Slack threads or someone's memory. An ADR makes them settled, findable, and citable — when someone asks "why do we deploy with service principals instead of my token?", the SOP just links to the ADR.

They're usually stored as markdown in a repo (often a `docs/adr/` folder or a dedicated `platform-decisions` repo), which fits your setup well — the ADR can live alongside the DABs template repo so the standard and the implementation of it travel together.