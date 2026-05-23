#!/usr/bin/env bash
# End-to-end smoke test: assumes the service is running on localhost:8080.
set -euo pipefail

BASE="${BASE:-http://localhost:8080}"

red()   { printf "\033[31m%s\033[0m\n" "$*"; }
green() { printf "\033[32m%s\033[0m\n" "$*"; }
hr()    { printf "%s\n" "------------------------------------------------------------"; }

step() { hr; printf "[STEP] %s\n" "$*"; hr; }

step "Health"
curl -fsS "$BASE/health" | tee /dev/null && echo

step "Ready"
curl -fsS "$BASE/ready" | tee /dev/null && echo

step "Get token"
TOKEN=$(curl -fsS -X POST "$BASE/api/v1/auth/token" \
  -H "Content-Type: application/json" \
  -d '{"username":"analyst","password":"analyst-pass"}' \
  | python3 -c "import sys, json; print(json.load(sys.stdin)['access_token'])")
green "Token: ${TOKEN:0:32}..."

step "POST indicator"
ID="indicator--$(uuidgen | tr 'A-Z' 'a-z')"
NOW="$(date -u +%Y-%m-%dT%H:%M:%S.000Z)"
curl -fsS -X POST "$BASE/api/v1/indicators" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"type\": \"indicator\",
    \"spec_version\": \"2.1\",
    \"id\": \"$ID\",
    \"created\": \"$NOW\",
    \"modified\": \"$NOW\",
    \"name\": \"Smoke test indicator\",
    \"pattern\": \"[file:hashes.'SHA-256' = 'aec070645fe53ee3b3763059376134f058cc337247c978add178b6ccdfb0019f']\",
    \"pattern_type\": \"stix\",
    \"valid_from\": \"$NOW\",
    \"confidence\": 80
  }" | tee /dev/null && echo

step "GET indicator"
curl -fsS -H "Authorization: Bearer $TOKEN" "$BASE/api/v1/indicators/$ID" | tee /dev/null && echo

step "Query indicators"
curl -fsS -H "Authorization: Bearer $TOKEN" "$BASE/api/v1/indicators?pattern_type=stix" | tee /dev/null && echo

green "All smoke checks passed."
