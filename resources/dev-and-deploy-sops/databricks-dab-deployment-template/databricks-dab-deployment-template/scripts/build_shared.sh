#!/usr/bin/env bash
# Build the shared_core wheel and distribute it to every project that
# consumes it. Run before deploying any project.
# ⚙ Phase 2: this becomes a pipeline step; humans stop running it.
set -euo pipefail
cd "$(dirname "$0")/.."

pip install --quiet build
python -m build --wheel libs/shared_core -o /tmp/shared_dist

mkdir -p projects/sample-job/dist
cp /tmp/shared_dist/shared_core-*.whl projects/sample-job/dist/
cp /tmp/shared_dist/shared_core-*.whl projects/sample-app/src/app/

echo "shared_core wheel distributed to consuming projects."
