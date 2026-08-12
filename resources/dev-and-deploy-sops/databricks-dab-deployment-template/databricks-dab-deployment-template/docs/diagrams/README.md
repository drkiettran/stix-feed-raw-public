# Diagrams

Each diagram exists as an editable `.puml` source and a rendered `.svg` (what the SOPs embed).
If you edit a source, re-render and commit BOTH files:

```bash
java -jar plantuml.jar -tsvg docs/diagrams/*.puml
```

(PlantUML: https://plantuml.com/download — any recent version works.)

Conventions the SOP walkthrough text depends on — keep them if you edit:
- Diagram 02: **solid arrows = wheel distribution**, **dotted arrows = runtime import**.
- Diagram 05: app-specific steps are marked **(A)**.
- Notes on diagrams 01–03 carry the takeaway; don't delete them when restyling.
