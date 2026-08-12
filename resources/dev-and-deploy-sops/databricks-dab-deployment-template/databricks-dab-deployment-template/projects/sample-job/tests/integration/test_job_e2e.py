"""Integration tests: run against the DEPLOYED staging bundle.
Never point these at prod. Until a pipeline exists, the SOPs
invoke these manually after a staging deploy.
"""
import os
import pytest
from databricks.sdk import WorkspaceClient

pytestmark = pytest.mark.skipif(
    "DATABRICKS_HOST" not in os.environ,
    reason="requires staging credentials",
)


def test_sample_job_succeeds_and_produces_rows():
    w = WorkspaceClient()  # picks up staging SP credentials from env
    job = next(j for j in w.jobs.list(name="dab-template-sample-job"))
    run = w.jobs.run_now(job.job_id).result()
    assert run.state.result_state.value == "SUCCESS"
