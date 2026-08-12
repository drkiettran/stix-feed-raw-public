# Setup Checklist (before first use)

Replace the placeholders — all of them are marked `REPLACE-...` and are findable with:

```bash
grep -rn "REPLACE-" --include="*.yml" --include="*.md" .
```

1. **Workspace URL** — in every `projects/*/databricks.yml` (three targets each).
2. **Service principal IDs** — staging and prod `run_as` in every `projects/*/databricks.yml`.
3. **Secret store** — name your actual store in both SOPs' prerequisites (currently "the approved secret store").
4. **Catalogs** — create `proj_dev` / `proj_stg` / `proj_prod` (or rename the `catalog` variable values to yours).
5. **App SP grants** — after each app's first deploy per environment, run the GRANT statements in the app SOP's prerequisites.
6. **Sample framework** — the sample app is Streamlit; swap `src/app/` and `requirements.txt` if your users' default framework differs.

Sanity check when done: `grep -rn "REPLACE-" .` returns nothing, and the Quick Start in README.md runs end to end against your dev target.
