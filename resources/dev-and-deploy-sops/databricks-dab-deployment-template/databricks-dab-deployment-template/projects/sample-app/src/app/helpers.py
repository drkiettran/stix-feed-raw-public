"""App-local logic, kept OUT of app.py so it can be unit tested.

Pattern: app.py is untestable Streamlit glue; anything with behavior
worth asserting lives here (or, if a second project needs it, in
libs/shared_core). Never bury logic inside st.* callbacks.
"""
from shared_core.transforms import normalize_severity

_BADGES = {1: "🟢 low", 2: "🟡 medium", 3: "🟠 high", 4: "🔴 critical"}


def severity_badge(label: str | None) -> str:
    """Human-readable badge for a severity label; safe on unknown input."""
    rank = normalize_severity(label)
    return _BADGES.get(rank, "⚪ unknown")
