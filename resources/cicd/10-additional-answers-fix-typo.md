# Request:
please remove all references to rva
D7: (a) Merge to main → auto-deploy dev → auto staging → manual approve → prod

# Reply:
Done — both changes are in:

**RVA removed.** The folder examples in the monorepo layout are now generic (`projects/example-job/`, `projects/example-app/`), and the path-filter example references `example-app`. Those were the only real occurrences — the grep also matched "conse**rva**tive" and "obse**rva**bility," which stay put.

**D7 finalized as option (a):** merge to main → auto-deploy dev → auto-deploy staging → manual approval → prod. The doc's status line, decision table, and flow section all reflect it, and I replaced the old open-item section with the three consequences this choice carries:

1. **Dev verification becomes the gatekeeper for staging.** Since staging deploys on every merge, the integration check (job sample run / app health) must run in dev *first* and stop the march on failure — otherwise staging is just a second dev.
2. **Rollback changes shape.** With no release tags in the flow, rollback = revert the commit and let the pipeline redeploy. To keep "what's running in prod?" answerable, the pipeline should auto-tag at prod approval time — you get the audit trail without the tagging ceremony.
3. **Two small closeouts remain** (now item 1 in "what happens next"): set the GitHub environment protection rule naming who can approve prod, and decide whether app approvals get a deploy window since apps restart a live service.

The decisions doc is now fully accepted with no open decisions — just those two configuration actions. Next real step is item 2: the monorepo skeleton with a working job project running your merge → dev → staging → approve → prod flow end to end. Ready to build it whenever you are.