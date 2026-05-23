# curl runbook for stix-feed-raw

Copy-paste-able curl commands for every endpoint, including error cases.
Assumes the service is running on `localhost:8080` (default `SERVER_PORT`).

If you started the service via Docker Compose with the stack from
`docker-compose.yml`, you're already on `8080`.
If you started it via `java -jar`, point your terminal at the same port.

---

## 0. Set up two shell variables

```bash
BASE=http://localhost:8080
TOKEN=""
```

---

## 1. Health and readiness (no auth)

```bash
curl -s "$BASE/health"
# → {"status":"UP"}

curl -s "$BASE/ready"
# → {"status":"READY","checks":{"kafka":"UP","store":"UP"}}
```

---

## 2. Get a JWT for the `analyst` user

```bash
TOKEN=$(curl -s -X POST "$BASE/api/v1/auth/token" \
  -H "Content-Type: application/json" \
  -d '{"username":"analyst","password":"analyst-pass"}' \
  | python3 -c "import sys, json; print(json.load(sys.stdin)['access_token'])")

echo "Token (first 32 chars): ${TOKEN:0:32}..."
```

`reader-pass` and `admin-pass` also work — see `README.md` for the role matrix.

### Wrong password — 401

```bash
curl -s -X POST "$BASE/api/v1/auth/token" \
  -H "Content-Type: application/json" \
  -d '{"username":"analyst","password":"wrong"}'
# → {"timestamp":"...","status":401,"error":"unauthorized","message":"Invalid credentials","trace_id":"..."}
```

---

## 3. Submit a single indicator (file-hash example)

```bash
curl -i -X POST "$BASE/api/v1/indicators" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d @examples/indicators/file-hash.json
# → 201 Created, body is the stored indicator
```

Same example, but using one of the other prepared payloads:

```bash
curl -i -X POST "$BASE/api/v1/indicators" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d @examples/indicators/ip-address.json

curl -i -X POST "$BASE/api/v1/indicators" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d @examples/indicators/domain.json

curl -i -X POST "$BASE/api/v1/indicators" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d @examples/indicators/url.json
```

### Re-POST the same indicator — 409 Conflict

```bash
curl -i -X POST "$BASE/api/v1/indicators" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d @examples/indicators/file-hash.json
# → 409 Conflict
```

To re-test without conflict, generate a fresh one:

```bash
./examples/post-indicator.sh
```

### Validation failure — 400 with details

```bash
curl -s -X POST "$BASE/api/v1/indicators" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d @examples/indicators/invalid.json | python3 -m json.tool
# → 400, with a "details" array listing every failed field
```

### No token — 401

```bash
curl -i -X POST "$BASE/api/v1/indicators" \
  -H "Content-Type: application/json" \
  -d @examples/indicators/file-hash.json
# → 401 Unauthorized, "Missing Bearer token"
```

### Wrong role — 403

```bash
READER_TOKEN=$(curl -s -X POST "$BASE/api/v1/auth/token" \
  -H "Content-Type: application/json" \
  -d '{"username":"reader","password":"reader-pass"}' \
  | python3 -c "import sys,json;print(json.load(sys.stdin)['access_token'])")

curl -i -X POST "$BASE/api/v1/indicators" \
  -H "Authorization: Bearer $READER_TOKEN" \
  -H "Content-Type: application/json" \
  -d @examples/indicators/file-hash.json
# → 403 Forbidden, "Requires role: analyst"
```

---

## 4. Batch submission

```bash
curl -i -X POST "$BASE/api/v1/indicators/batch" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d @examples/indicators/batch.json | python3 -m json.tool
# → 201 Created (all accepted), or 207 Multi-Status (partial)
```

The response body has shape:
```json
{
  "total": 3, "accepted": 3, "rejected": 0,
  "items": [
    { "index": 0, "id": "indicator--...", "status": 201 },
    ...
  ]
}
```

---

## 5. GET a single indicator by id

```bash
ID="indicator--7c8a3d2f-1e4b-4a8e-9c1d-3f5a8e9b2c1a"   # the file-hash example
curl -s -H "Authorization: Bearer $TOKEN" "$BASE/api/v1/indicators/$ID" | python3 -m json.tool
```

### Unknown id — 404

```bash
curl -i -H "Authorization: Bearer $TOKEN" \
  "$BASE/api/v1/indicators/indicator--00000000-0000-0000-0000-000000000000"
# → 404 Not Found
```

---

## 6. Query indicators

All filters are optional. Combine freely.

```bash
# All indicators (default page of 50)
curl -s -H "Authorization: Bearer $TOKEN" "$BASE/api/v1/indicators" | python3 -m json.tool

# Filter by pattern_type
curl -s -H "Authorization: Bearer $TOKEN" \
  "$BASE/api/v1/indicators?pattern_type=stix" | python3 -m json.tool

# Filter by minimum confidence
curl -s -H "Authorization: Bearer $TOKEN" \
  "$BASE/api/v1/indicators?confidence_gte=80" | python3 -m json.tool

# Filter by label (multi-valued — repeat the param)
curl -s -H "Authorization: Bearer $TOKEN" \
  "$BASE/api/v1/indicators?label=phishing&label=apt" | python3 -m json.tool

# Filter by valid_from after a timestamp
curl -s -H "Authorization: Bearer $TOKEN" \
  "$BASE/api/v1/indicators?valid_from_after=2026-05-05T14:00:00Z" | python3 -m json.tool

# Pagination
curl -s -H "Authorization: Bearer $TOKEN" \
  "$BASE/api/v1/indicators?limit=2&offset=0" | python3 -m json.tool
```

### Bad query parameter — 400

```bash
curl -i -H "Authorization: Bearer $TOKEN" \
  "$BASE/api/v1/indicators?confidence_gte=banana"
# → 400 Bad Request, "confidence_gte must be an integer"
```

---

## 7. OpenAPI spec (raw)

```bash
curl -s "$BASE/openapi.yaml" | head -40
```

Or open the rendered docs in a browser: **http://localhost:8080/docs**
