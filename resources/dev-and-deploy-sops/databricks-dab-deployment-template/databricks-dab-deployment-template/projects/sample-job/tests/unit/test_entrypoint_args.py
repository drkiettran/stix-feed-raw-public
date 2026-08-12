"""Project-level unit tests cover project glue (arg parsing, wiring).
Tests for shared logic live with the library: libs/shared_core/tests.
"""
import pytest
from sample_job import main


def test_run_requires_catalog(monkeypatch):
    monkeypatch.setattr("sys.argv", ["main"])
    with pytest.raises(SystemExit):
        main.run()
