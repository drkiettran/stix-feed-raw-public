#!/usr/bin/env bash
# Generates a fresh STIX indicator with a new UUID and current timestamp,
# fetches a token, and POSTs it. Use when example files conflict (409) on re-POST.
set -euo pipefail

BASE="${BASE:-http://localhost:8080}"
USER="${USER:-analyst}"
PASS="${PASS:-analyst-pass}"

ID="indicator--$(uuidgen | tr 'A-Z' 'a-z')"
NOW="$(date -u +%Y-%m-%dT%H:%M:%S.000Z)"

echo "Fetching token..."
TOKEN=$(curl -fsS -X POST "$BASE/api/v1/auth/token" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$USER\",\"password\":\"$PASS\"}" \
  | python3 -c "import sys, json; print(json.load(sys.stdin)['access_token'])")

echo "Posting indicator $ID..."
curl -fsS -X POST "$BASE/api/v1/indicators" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"type\": \"indicator\",
    \"spec_version\": \"2.1\",
    \"id\": \"$ID\",
    \"created\":  \"$NOW\",
    \"modified\": \"$NOW\",
    \"name\": \"Generated indicator $(date +%H:%M:%S)\",
    \"indicator_types\": [\"malicious-activity\"],
    \"pattern\": \"[file:hashes.'SHA-256' = 'aec070645fe53ee3b3763059376134f058cc337247c978add178b6ccdfb0019f']\",
    \"pattern_type\": \"stix\",
    \"valid_from\": \"$NOW\",
    \"labels\": [\"generated\"],
    \"confidence\": 70
  }"
echo ""
echo "Done. ID: $ID"
