"""Unit tests: pure logic, zero credentials, zero network.
Rule: if a test needs a workspace, it is an integration test
and belongs in tests/integration instead.
"""
from shared_core.transforms import normalize_severity


def test_maps_known_labels():
    assert normalize_severity("CRITICAL") == 4
    assert normalize_severity("low") == 1


def test_tolerates_whitespace_and_case():
    assert normalize_severity("  High ") == 3


def test_unknown_and_none_return_none():
    assert normalize_severity("banana") is None
    assert normalize_severity(None) is None
