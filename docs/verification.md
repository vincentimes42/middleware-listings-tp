# TP1 (Spring Boot) — verification commands

Copy-pasteable commands for every `Verify` section of
[`../exercise.md`](../exercise.md): register → CRUD → access rules → V2 schema
→ versioning → metrics. Each section number below is the one the exercise
links to.

> **Prerequisite:** the stack runs (see [`../README.md`](../README.md), "Run").
> Commands use `bash`, `curl`, and `python3`.

## 1) Ports

| Host port | Service | Notes |
|---|---|---|
| `8080` | `listings-api` | the target of every command |
| `5435` | `listings-postgres` | db / user / password: `listings` |

## 2) Variables and helpers (once per terminal)

```bash
export TP1_DIR="/path/to/springboot"          # this folder
export API="http://localhost:8080"
ALICE='alice@test.org:secret123'              # owner
BOB='bob@test.org:secret123'                  # non-owner
JSON='Content-Type: application/json'

# print only the HTTP status of a request
code() { curl -s -o /dev/null -w "%{http_code}\n" "$@"; }
# run SQL in the database container
sql()  { (cd "$TP1_DIR" && docker compose exec -T listings-postgres psql -U listings -d listings -c "$1"); }
# read one field of a JSON body on stdin, e.g.  ... | field id
field() { python3 -c "import sys,json; print(json.load(sys.stdin)$(printf '["%s"]' "$@"))"; }
# wait until the API answers again (after a restart or a rebuild)
waitup() { until curl -fsS "$API/actuator/health" >/dev/null 2>&1; do sleep 2; done; echo "up"; }
```

## 3) Start the stack

```bash
cd "$TP1_DIR"
docker compose up -d --build        # rebuild after every Java change
docker compose ps                   # both services "healthy"
curl -fsS "$API/actuator/health"    # {"groups":[...],"status":"UP"}
```

## 4) Create the two accounts

Run once per fresh database (a second run gives 409, which is fine).

```bash
curl -s -X POST "$API/auth/register" -H "$JSON" \
  -d '{"email":"alice@test.org","username":"Alice","password":"secret123"}'; echo
curl -s -X POST "$API/auth/register" -H "$JSON" \
  -d '{"email":"bob@test.org","username":"Bob","password":"secret123"}'; echo
```

## 5) Step 1 — user accounts

```bash
# 201 on the first run (409 when you replay this section): role SELLER, no password
curl -s -i -X POST "$API/auth/register" -H "$JSON" \
  -d '{"email":"carol@test.org","username":"Carol","password":"secret123"}'; echo

# 409 duplicate
code -X POST "$API/auth/register" -H "$JSON" \
  -d '{"email":"alice@test.org","username":"Alice2","password":"secret123"}'

# 422 + fields (3 entries: email, username, password)
curl -s -X POST "$API/auth/register" -H "$JSON" \
  -d '{"email":"x","username":"","password":"123"}'; echo

# 401 anonymous, 401 wrong password, 200 valid
code "$API/auth/me"
code -u 'alice@test.org:wrong' "$API/auth/me"
curl -s -u "$ALICE" "$API/auth/me"; echo

# BCrypt at rest: prefix $2a$, never the raw password
sql "SELECT email, left(password_hash, 7) AS hash_prefix, role FROM app_user ORDER BY id"
```

## 6) Steps 2a-2b — CRUD (V1)

### 6a) Step 2a — create and read

```bash
# create two listings, keep their ids
BIKE=$(curl -s -u "$ALICE" -X POST "$API/listings" -H "$JSON" \
  -d '{"title":"Bike","description":"road bike","price":350,"category":"SPORTS"}' | field id)
LAMP=$(curl -s -u "$ALICE" -X POST "$API/listings" -H "$JSON" \
  -d '{"title":"Lamp","price":25,"category":"HOUSEHOLD"}' | field id)
echo "BIKE=$BIKE LAMP=$LAMP"

# checkpoint: the GET body equals the POST body (creationDate included)
CHAIR=$(curl -s -u "$ALICE" -X POST "$API/listings" -H "$JSON" \
  -d '{"title":"Chair","price":15,"category":"FURNITURE"}')
diff <(echo "$CHAIR") <(curl -s -u "$ALICE" "$API/listings/$(echo "$CHAIR" | field id)"; echo) \
  && echo "ok: same body"

# 201 + Location header
curl -s -i -u "$ALICE" -X POST "$API/listings" -H "$JSON" \
  -d '{"title":"Desk","price":80,"category":"FURNITURE"}' | grep -iE '^HTTP|^Location'

# 404 unknown
curl -s -u "$ALICE" -w "\n-> %{http_code}\n" "$API/listings/999999"

# 422: title too long + negative price
curl -s -u "$ALICE" -X POST "$API/listings" -H "$JSON" \
  -d "{\"title\":\"$(printf 'x%.0s' {1..130})\",\"price\":-1,\"category\":\"SPORTS\"}" \
  -w "\n-> %{http_code}\n"

# 400: unknown enum value (a parse error, not a validation error)
curl -s -u "$ALICE" -X POST "$API/listings" -H "$JSON" \
  -d '{"title":"x","price":1,"category":"NONSENSE"}' -w "\n-> %{http_code}\n"

# schema
sql "SELECT id, filename FROM databasechangelog ORDER BY orderexecuted"
sql '\d listing'
```

### 6b) Step 2b — list, update, delete

```bash
# page shape: page.size = 2, page.totalElements = all listings
curl -s -u "$ALICE" "$API/listings?page=0&size=2" | python3 -c '
import sys, json; d = json.load(sys.stdin)
print("items =", len(d["content"]), "| page =", d["page"])'

# filters: only bikes (one more per replay of §6a), then an empty list
curl -s -u "$ALICE" "$API/listings?category=SPORTS&maxPrice=400" | python3 -c '
import sys, json; print([i["title"] for i in json.load(sys.stdin)["content"]])'
curl -s -u "$ALICE" "$API/listings?category=SPORTS&maxPrice=100" | python3 -c '
import sys, json; print([i["title"] for i in json.load(sys.stdin)["content"]])'

# PUT 200, DELETE 204, then 404
curl -s -u "$ALICE" -X PUT "$API/listings/$LAMP" -H "$JSON" \
  -d '{"title":"Desk lamp","price":30,"category":"HOUSEHOLD"}'; echo
code -u "$ALICE" -X DELETE "$API/listings/$LAMP"
code -u "$ALICE" "$API/listings/$LAMP"
```

## 7) Step 3 — access rules

```bash
# BIKE from §6 belongs to alice
for who in anonymous bob alice; do
  case $who in anonymous) U=();; bob) U=(-u "$BOB");; alice) U=(-u "$ALICE");; esac
  printf "%-9s GET list=%s  GET detail=%s  POST=%s  PUT=%s\n" "$who" \
    "$(code "${U[@]}" "$API/listings")" \
    "$(code "${U[@]}" "$API/listings/$BIKE")" \
    "$(code "${U[@]}" -X POST "$API/listings" -H "$JSON" -d '{"title":"t","price":1,"category":"OTHER"}')" \
    "$(code "${U[@]}" -X PUT "$API/listings/$BIKE" -H "$JSON" -d '{"title":"Bike","price":350,"category":"SPORTS"}')"
done
# expected: anonymous 200 200 401 401 | bob 200 200 201 403 | alice 200 200 201 200

# DELETE by a non-owner: anonymous 401, bob 403
code -X DELETE "$API/listings/$BIKE"
code -u "$BOB" -X DELETE "$API/listings/$BIKE"

# 401 carries a Basic challenge
curl -s -i -X POST "$API/listings" -H "$JSON" -d '{}' | grep -i '^WWW-Authenticate'

# hide the bike, then compare views
curl -s -o /dev/null -u "$ALICE" -X PUT "$API/listings/$BIKE" -H "$JSON" \
  -d '{"title":"Bike","price":350,"category":"SPORTS","active":false}'
for who in anonymous bob alice; do
  case $who in anonymous) U=();; bob) U=(-u "$BOB");; alice) U=(-u "$ALICE");; esac
  IN_LIST=$(curl -s "${U[@]}" "$API/listings?size=100" | python3 -c "
import sys, json; print(any(i['id'] == $BIKE for i in json.load(sys.stdin)['content']))")
  echo "$who: in list=$IN_LIST  detail=$(code "${U[@]}" "$API/listings/$BIKE")"
done
# expected: anonymous False 404 | bob False 404 | alice True 200
# a non-owner write on a hidden listing is 404 too (not 403: no leak)
code -u "$BOB" -X PUT "$API/listings/$BIKE" -H "$JSON" -d '{"title":"x","price":1,"category":"OTHER"}'
code -u "$BOB" -X DELETE "$API/listings/$BIKE"
curl -s -o /dev/null -u "$ALICE" -X PUT "$API/listings/$BIKE" -H "$JSON" \
  -d '{"title":"Bike","price":350,"category":"SPORTS","active":true}'   # show it again

# stateless: no session cookie
curl -s -i -u "$ALICE" "$API/auth/me" | grep -i '^Set-Cookie' || echo "ok: no cookie"

# public endpoints
for p in /actuator/health /actuator/info /actuator/metrics /v3/api-docs; do
  echo "$p -> $(code "$API$p")"; done
```

## 8) Step 4 (bonus) — JSON + XML

```bash
# 1. JSON in, JSON out
curl -s -i -u "$ALICE" -X POST "$API/listings" -H "$JSON" \
  -d '{"title":"Bike","price":350,"category":"SPORTS"}' | grep -iE '^HTTP|^Content-Type'
# 2. XML in, XML out
curl -s -i -u "$ALICE" -X POST "$API/listings" \
  -H 'Content-Type: application/xml' -H 'Accept: application/xml' \
  -d '<listing><title>Kayak</title><price>500</price><category>SPORTS</category></listing>'; echo
# 3-4. same resource, two formats
curl -s -H 'Accept: application/xml'  "$API/listings/$BIKE"; echo
curl -s -H 'Accept: application/json' "$API/listings/$BIKE"; echo
# 5. 415, JSON body
curl -s -i -u "$ALICE" -X POST "$API/listings" -H 'Content-Type: application/octet-stream' -d 'x' \
  | grep -iE '^HTTP|^Content-Type|status'
# 6. 406, JSON body
curl -s -i -H 'Accept: text/csv' "$API/listings/$BIKE" | grep -iE '^HTTP|^Content-Type|status'
# 7. an error requested as XML is still JSON
curl -s -i -H 'Accept: application/xml' "$API/listings/999999" | grep -iE '^HTTP|^Content-Type'
```

## 9) Step 5 — V2 schema (additive)

```bash
sql '\d listing'                                   # 7 new nullable columns
sql "SELECT id, title, negotiable, latitude, longitude FROM listing ORDER BY id"
sql "SELECT orderexecuted, id, filename FROM databasechangelog ORDER BY orderexecuted"
# restart: no new row in databasechangelog
(cd "$TP1_DIR" && docker compose restart listings-api) && waitup
sql "SELECT count(*) FROM databasechangelog"
grep -n "v1.1-v2-fields" "$TP1_DIR/src/main/resources/db/changelog/db.changelog-master.yaml"
# V1 unchanged: no negotiable key
curl -s "$API/listings/$BIKE"; echo
```

Then replay §6 and §7: same results.

## 10) Steps 6a-6b — versioning V1 + V2

### 10a) Step 6a — routing and deprecation

```bash
# default = V1 = ?version=V1
diff <(curl -s "$API/listings/$BIKE") <(curl -s "$API/listings/$BIKE?version=V1") && echo "ok: same body"

# headers of a GET (not curl -I: HEAD is not a permitted method): present on V1, absent on V2
curl -s -o /dev/null -D - "$API/listings"            | grep -iE '^(HTTP|Deprecation|Sunset)'
curl -s -o /dev/null -D - "$API/listings?version=V2" | grep -iE '^(HTTP|Deprecation|Sunset)'
# expected: HTTP 200 + Deprecation + Sunset, then HTTP 200 alone

# lower-case and numeric values
for v in v1 1 v2 2; do echo "$v -> $(code "$API/listings?version=$v")"; done

# 400 on unknown version
curl -s -w "\n-> %{http_code}\n" "$API/listings?version=V99"
```

### 10b) Step 6b — the V2 contract

```bash
# alice writes V2 fields
curl -s -u "$ALICE" -X PUT "$API/listings/$BIKE?version=V2" -H "$JSON" \
  -d '{"title":"Bike","price":350,"category":"SPORTS","negotiable":true,
       "streetAddress":"10 Rue de Rivoli","city":"Paris","postalCode":"75004","country":"France"}'; echo

# visibility per client
show() { python3 -c 'import sys, json; d = json.load(sys.stdin); print(sorted(d))'; }
echo "V1 alice:"; curl -s -u "$ALICE" "$API/listings/$BIKE?version=V1" | show
echo "V2 anon :"; curl -s            "$API/listings/$BIKE?version=V2" | show
echo "V2 bob  :"; curl -s -u "$BOB"   "$API/listings/$BIKE?version=V2" | show
echo "V2 alice:"; curl -s -u "$ALICE" "$API/listings/$BIKE?version=V2" | show

# a V1 PUT does not erase V2 fields
curl -s -o /dev/null -u "$ALICE" -X PUT "$API/listings/$BIKE?version=V1" -H "$JSON" \
  -d '{"title":"Bike","price":340,"category":"SPORTS"}'
curl -s -u "$ALICE" "$API/listings/$BIKE?version=V2" | field streetAddress

# two OpenAPI documents (the ungrouped /v3/api-docs mixes both versions: do not use it here)
for g in listings-v1 listings-v2; do
  curl -s "$API/v3/api-docs/$g" | python3 -c 'import sys, json; print(json.load(sys.stdin)["info"]["title"])'
done
```

## 11) Step 7 (bonus) — GPS enrichment

```bash
# success path (real Nominatim; at most 1 request per second)
curl -s -u "$ALICE" -X PUT "$API/listings/$BIKE?version=V2" -H "$JSON" \
  -d '{"title":"Bike","price":350,"category":"SPORTS",
       "streetAddress":"10 Rue de Rivoli","city":"Paris","country":"France"}'; echo
sql "SELECT id, latitude, longitude FROM listing WHERE id = $BIKE"
```

Failure path: point the geocoder to a server that accepts the connection and
never answers, so that the **read timeout** is what ends the call (a
non-routable address often fails at once and proves nothing about timeouts).

```bash
docker run -d --rm --name slow-geo --network middleware alpine:3.16.3 \
  sh -c 'nc -lk -p 80 -e sleep 3600'
```

In `docker-compose.yml`, add to `listings-api.environment` the line
`LISTINGS_GEO_ENDPOINT: "http://slow-geo"`, then
`docker compose up -d listings-api && waitup` (no rebuild needed).

```bash
curl -s -o /dev/null -w "HTTP %{http_code} in %{time_total}s\n" -u "$ALICE" \
  -X PUT "$API/listings/$BIKE?version=V2" -H "$JSON" \
  -d '{"title":"Bike","price":350,"category":"SPORTS","streetAddress":"10 Rue de Rivoli","city":"Paris"}'
# expected: HTTP 200 in about 3 s (the read timeout), < 4 s
sql "SELECT latitude, longitude FROM listing WHERE id = $BIKE"   # NULL, NULL
(cd "$TP1_DIR" && docker compose logs --since 2m listings-api | grep WARN | tail -1)
# expected: your WARN, naming the address and "Read timed out"
```

Afterwards: remove the `LISTINGS_GEO_ENDPOINT` line,
`docker compose up -d listings-api`, and `docker rm -f slow-geo`.

## 12) Step 8 — metrics

```bash
metric() { curl -s "$API/actuator/metrics/listings.creations" | python3 -c '
import sys, json; d = json.load(sys.stdin); print(d["measurements"][0]["value"], d["availableTags"])'; }

# after a restart: 200, value 0.0
(cd "$TP1_DIR" && docker compose restart listings-api) && waitup
code "$API/actuator/metrics/listings.creations"
metric

# one 201 -> +1, one 422 -> unchanged
code -u "$ALICE" -X POST "$API/listings" -H "$JSON" -d '{"title":"Counter","price":10,"category":"OTHER"}'
metric
code -u "$ALICE" -X POST "$API/listings" -H "$JSON" -d '{"title":"","price":0,"category":"OTHER"}'
metric
```

## 13) Step 9 (bonus) — distance search

Needs listings with coordinates (Step 7, or `latitude` / `longitude` written
through a V2 `PUT`).

```bash
curl -s -u "$ALICE" "$API/listings?version=V2&lat=48.86&lng=2.35&radius=5" | python3 -c '
import sys, json
for i in json.load(sys.stdin)["content"]:
    print(i["id"], i["title"], i.get("latitude"), i.get("longitude"))'

for q in "version=V2&lat=48.86" \
         "version=V2&lat=48.86&lng=2.35&radius=0" \
         "version=V2&lat=48.86&lng=2.35&radius=0.5" \
         "version=V2&lat=91&lng=2.35&radius=5" \
         "version=V1&lat=48.86&lng=2.35&radius=5"; do
  echo "?$q -> $(code "$API/listings?$q")"     # all 400
done
```

## 14) Step 10 — final checks

```bash
cd "$TP1_DIR"
docker compose down -v && docker compose up -d --build && waitup   # fresh database
mvn clean verify                     # or the container command in README "Build & test"
grep -rnE "TODO|Not implemented|UnsupportedOperationException" src/main/java || echo "ok: no stub"
sql '\dt'
docker compose logs listings-api | grep -E " ERROR " || echo "ok: no ERROR"
```

Then replay §4 to §8 and §12 (and the bonus sections you did).

## 15) Stop

```bash
cd "$TP1_DIR"
docker compose down          # keep the data
docker compose down -v       # wipe the database volume
```

## 16) Common issues

| Symptom | Cause | Fix |
|---|---|---|
| `500` with `Not implemented yet` | the endpoint is still a stub | implement it (the step that owns it) |
| `500` where a `4xx` is expected | the exception reached the catch-all `@ExceptionHandler(Exception.class)` | add the specific handler (exercise, Appendix C) |
| `401` on `GET /listings` | public reads not permitted yet | Step 3 |
| `403` on your own `PUT` | owner check compares the wrong ids | compare `listing.owner.id` with the caller's `User.id`, not the email |
| Liquibase `checksum` error at boot | an applied changeSet was edited | revert it; put the change in a new changeSet |
| `?version=V2` returns V1 fields | the V2 mapping is missing or not versioned | Step 6b, `version = "2"` |
| `403` instead of `404` for bob on a hidden listing | the write path checks the owner before the visibility | load the listing with the read rule first (Step 3, item 5) |
| `?version=V99` returns 500 | `InvalidApiVersionException` not mapped | Step 6a, item 3 |
| `/actuator/metrics/listings.creations` is 404 | counter created lazily | register it in the constructor (Step 8) |
| XML request answered with 415 | XML dependency missing, or Jackson 2 groupId | Step 4, `tools.jackson.dataformat` |
| Java change not visible | image not rebuilt | `docker compose up -d --build listings-api` |
