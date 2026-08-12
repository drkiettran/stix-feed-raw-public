"""Unit tests for the app's own logic. Zero credentials, zero Streamlit.
Shared-logic tests live with the library (libs/shared_core/tests);
these cover only what is app-local.
"""
from app.helpers import severity_badge


def test_known_labels_get_badges():
    assert severity_badge("CRITICAL").endswith("critical")
    assert severity_badge("low").endswith("low")


def test_unknown_input_is_safe_not_fatal():
    assert severity_badge("banana").endswith("unknown")
    assert severity_badge(None).endswith("unknown")
