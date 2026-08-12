"""Pure transformation logic.

Nothing in this module touches Databricks, Spark sessions, or
credentials — which is exactly why tests/unit can import and test
it in milliseconds on any machine or CI runner.
"""

SEVERITY_MAP = {"LOW": 1, "MEDIUM": 2, "HIGH": 3, "CRITICAL": 4}


def normalize_severity(label: str | None) -> int | None:
    """Map a free-text severity label to a numeric rank."""
    if label is None:
        return None
    return SEVERITY_MAP.get(label.strip().upper())
