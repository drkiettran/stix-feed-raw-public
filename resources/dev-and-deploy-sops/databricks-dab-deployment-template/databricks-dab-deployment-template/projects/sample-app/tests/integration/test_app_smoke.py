"""App integration smoke test — runs against the DEPLOYED staging app.
Invoked by SOP-app-deployment Stage 4.3: pytest tests/integration -k app
Never point this at prod.
"""
import os
import pytest
import urllib.request

pytestmark = pytest.mark.skipif(
    "DATABRICKS_HOST" not in os.environ,
    reason="requires staging credentials",
)


def _app_url() -> str:
    from databricks.sdk import WorkspaceClient
    w = WorkspaceClient()
    app = w.apps.get("dab-template-sample-app")
    return app.url


def test_app_serves_http_200():
    """Proves the app STARTED (env vars resolved, wheel installed)."""
    with urllib.request.urlopen(_app_url(), timeout=30) as resp:
        assert resp.status == 200


@pytest.mark.skip(reason="enable once the app exposes a catalog-backed route")
def test_app_read_path_touches_catalog():
    """Proves the APP SP's grants work — a 200 on a static page does not.
    Point this at a route that runs a real query; see SOP Stage 4.2."""
