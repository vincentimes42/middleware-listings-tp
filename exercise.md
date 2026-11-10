# Classifieds API - Progressive Implementation

## Overview

You are building a **classifieds listings** micro-service, in pairs, in a session of
about **6 hours** (the core is sized for ~5h30-6h; the bonus steps overflow). The
skeleton you receive already compiles, starts, and passes a context test. Your job
is to implement the API **feature by feature**, in the order below.

**How each step is validated** — every step ends with a `Verify` checklist that you
run by hand with `curl`, `psql` or `grep` against the running stack (see
*Conventions — how to validate each step*). That checklist is the contract.
Writing Java tests is **encouraged but optional**: it is not part of the
deliverable (see Appendix C if you want to write some).

The specification describes **behaviors, contracts, and response codes**. It does
not prescribe the internal design: you may structure the code as you see fit,
provided the **observable behavior** matches.

> Prereq — the stack runs and `curl -fsS http://localhost:8080/actuator/health`
> answers `UP` (see **README**, *Prerequisites* and *Run*).

---

## Before you start — the two tools you will use

New to Maven or Spring Boot? This 2-minute mental model will make the steps
below click. (Installation and run details are in `README.md`.)

### Maven — what you actually do

**Maven builds a Java project from a `pom.xml`** (dependencies, plugins).
A small set of commands does the same everywhere:

| Maven command | What it does |
|---|---|
| `mvn spring-boot:run` | start the application (port 8080) |
| `mvn clean verify` | clean, compile, test, and package |

Key idea: to use a library you add a `<dependency>` in `pom.xml` — you never
touch `jar` files by hand.

### Spring Boot — what you actually do

**You write annotated classes; Spring Boot wires the rest.** `@RestController`,
`@Service`, `@Repository`, `@Entity`, `@Configuration` + a few `application.yml`
keys give you the HTTP server, DataSource, security chain and Liquibase — you
don't write that plumbing yourself. If a behaviour feels "magical", look up the
annotation or `yml` key in the reference documentation of the exact versions
used by the project:

- Spring Boot 4.1.1: <https://docs.spring.io/spring-boot/4.1.1/index.html>
- Spring Framework 7 (web, API versioning): <https://docs.spring.io/spring-framework/reference/web/webmvc-versioning.html>
- Spring Security 7: <https://docs.spring.io/spring-security/reference/index.html>

---

## Domain & data model

Before looking at the steps, here is **what you are modelling**. Each step below
is one facet of this domain.

### Actors

| Actor | What they can do |
|---|---|
| **Visitor** (anonymous) | Browse and read any *active* listing. No account. |
| **Seller** (authenticated, role `SELLER`) | Creates listings and manages **the ones they own**. Every registered account has the role `SELLER`; there is no admin role. |

Two words, two ideas — keep them apart:

- **`SELLER`** is a **role**: it says *who may write at all* (otherwise **401**).
- **owner** is a **relationship** between one listing and one user: it says
  *who may change this listing* (otherwise **403**). Every owner is a `SELLER`;
  most `SELLER`s are **not** the owner of a given listing.

### Entities

**`User`** — a seller account (provided by the skeleton).

| attribute | note |
|---|---|
| `id` | primary key (surrogate) |
| `email` | **unique** — used as the Basic-auth login |
| `username` | displayed human name |
| `passwordHash` | BCrypt hash — the raw password is never stored |
| `role` | always `SELLER` |
| `registrationDate` | account creation time (ISO-8601) |

**`Listing`** — an item offered for sale.

| attribute | V1 response | V2 response (Steps 5-6) |
|---|:--:|---|
| `id`, `title` (1-120), `description` (0-2000), `price` (>0), `category` (9 values), `creationDate` | yes | yes — everyone |
| `negotiable` | no | yes — everyone |
| `active` | no (write-only, via PUT) | owner only |
| `streetAddress`, `city`, `postalCode`, `country`, `latitude`, `longitude` | no | owner only |

### Relationship

`User` **1 —— 0..N** `Listing` — a listing always belongs to exactly one user
(its **owner**); a user can own many listings. Carried by the column
`listing.user_id` (`@ManyToOne owner` on the `Listing` entity). Referential
integrity is JPA's job; a DB-level foreign key is optional.

### Lifecycle

A listing is born `active = true`. The owner can **hide** it (`active = false`)
and reactivate it later. A hidden listing is absent from lists and reads as
**404** for everyone but its owner; the owner still sees it.

> Hold these four ideas in mind — *actors (role vs owner), the two entities, the
> 1-to-N ownership, and the active lifecycle* — because almost every requirement
> below is a rule about one of them.

---

## Scope

| Track | Steps | Budget (pair) |
|---|---|---|
| **Core** (required, in this order) | 1 · 2 · 3 · 5 · 6a · 6b · 8 | ~5h30-6h |
| **Bonus** (start only when the core checklists are green) | 4 (JSON + XML) · 7 (GPS enrichment) · 9 (distance search) | — |
| **Delivery** | 10 (final checks) | — |

| Step | Title | Track | Budget |
|---|---|---|---|
| 1 | User accounts | CORE | 0h45 |
| 2 | Listings: create and browse (V1) | CORE | 1h15 |
| 3 | Security rules: ownership and lifecycle | CORE | 1h15 |
| 4 | Content negotiation (JSON + XML) | BONUS | — |
| 5 | Schema evolution (V2 columns) | CORE | 0h20 |
| 6a | Versioning: routing, default, deprecation | CORE | 1h00 |
| 6b | V2 contract: visibility and OpenAPI | CORE | 1h00 |
| 7 | Geolocation (GPS enrichment) | BONUS | — |
| 8 | Metrics | CORE | 0h20 |
| 9 | Distance search | BONUS | — |
| 10 | Final checks | DELIVERY | 0h15 |

> The bonus steps keep their historical numbers (4, 7, 9) so that the follow-up
> TPs can reference "TP1 Step N" unambiguously. **Skip them on the first pass**:
> do 1 → 2 → 3 → 5 → 6a → 6b → 8, then come back.

---

## Conventions

- All timestamps are ISO-8601.
- All monetary amounts are decimal (2 digits).
- Content type: `application/json` by default (`application/xml` is added in Step 4, bonus).
- Authentication is **HTTP Basic** (no form, no session, no login endpoint).
- The API supports **two versions** once Step 6a is done: `V1` and `V2`, selected
  with the query parameter `?version=`.
- Every error response is JSON:
  `{ "timestamp": "...", "status": 4xx, "error": "...", "message": "..." }`
  (+ a `fields` map for validation errors, + `availableVersions` for an unknown version).
- List responses are paginated with this shape (Spring Data `PagedModel`):

  ```json
  { "content": [ { "id": 1, "title": "..." } ],
    "page": { "size": 20, "number": 0, "totalElements": 1, "totalPages": 1 } }
  ```

### Error handling & status codes (cross-cutting)

Whenever a step says "→ 409", the HTTP response must carry status 409 **and** a
JSON body in the format above.

| Status | Trigger | Raised by | Mapped by |
|---|---|---|---|
| 400 | Unknown API version (Step 6a); invalid distance parameters (Step 9) | framework (`InvalidApiVersionException`) / your code | your `@ExceptionHandler` |
| 401 | Write without valid Basic credentials | Spring Security (filter) | Spring Security |
| 403 | Authenticated user who is not the listing's owner (Step 3) | your service (`AccessDeniedException`) | your `@ExceptionHandler` |
| 404 | Unknown listing; hidden listing seen by a non-owner (Steps 2-3) | your service (`ResourceNotFoundException`, to create) | your `@ExceptionHandler` |
| 406 | `Accept` type not supported (Step 4) | framework | your `@ExceptionHandler` |
| 409 | Duplicate email on registration (Step 1) | provided service | **provided** |
| 415 | `Content-Type` not supported (Step 4) | framework | your `@ExceptionHandler` |
| 422 | DTO validation errors, with `fields` (Steps 1-2) | framework (`@Valid`) | **provided** |
| 500 | Unexpected error | — | **provided** (generic handler) |

> **Trap** — the provided generic handler catches `Exception`. Any exception you
> do not map explicitly (an `AccessDeniedException` thrown by your service, for
> instance) becomes a **500**. Each status above needs its own `@ExceptionHandler`
> in `web/advice/GlobalExceptionHandler.java`.

Custom domain exceptions live in `com.app.listings.domain.exception` (next to the
provided `EmailAlreadyExistsException`).

### Conventions — how to validate each step

Every `Verify` item is a command you run against the stack started with
`docker compose up -d --build` (rebuild after each code change). Define these
shell variables once per terminal:

```bash
API=http://localhost:8080
ALICE=alice@test.org:secret123     # owner of the listings she creates
BOB=bob@test.org:secret123         # another SELLER, never the owner of Alice's listings
PSQL="docker compose exec -T listings-postgres psql -U listings -d listings -c"
```

- `curl -i` prints the status line and headers: read the status **there**.
- `-u "$ALICE"` sends HTTP Basic credentials; no `-u` = anonymous.
- `$PSQL "select ..."` reads the database directly.
- Logs: `docker compose logs listings-api | grep <pattern>`.

A step is done when **every** box of its `Verify` list is ticked.

---

## Step 1 - User accounts   [CORE]

**Outcome** — a visitor can create an account, then authenticate with HTTP Basic
and read their own profile on `GET /auth/me`.

### Build it

1. **Read** what is provided (do not re-implement it): `User`, `UserRepository`,
   `UserService` / `UserServiceImpl` (uniqueness → 409, BCrypt, `role = SELLER`),
   `UserRequestDto` (validation), `UserResponseDto.from(User)` (no password),
   `GlobalExceptionHandler` (409, 422) and `SecurityConfig` (`httpBasic`, BCrypt,
   `UserDetailsService` by email, `/auth/register` public).
   - *Why:* Knowing what already exists keeps this step about wiring an endpoint, not about rebuilding security plumbing.
2. **Modify** `web/controller/AuthController.java` — inject `UserService`.
   - *Why:* The controller only translates HTTP into a service call; the business rules stay in the service.
3. **Wire** `register`: take a validated `UserRequestDto` body, call
   `userService.register(...)`, return the `UserResponseDto` with status **201**
   and a `Location` header pointing at the new resource.
   - *Why:* A 201 with a `Location` header is the REST convention that tells the client a resource was created and where to find it.
4. **Wire** `me`: resolve the authenticated principal (its username is the
   email), load the user through `UserService`, return its `UserResponseDto`.
   - *Why:* `/auth/me` proves that Basic authentication works end to end: credentials in, the matching account out.
5. **Rebuild** the stack and run `Verify`.
   - *Why:* The Docker image embeds the compiled code: without a rebuild, the container keeps running the previous version.

### Expected behavior

| # | Rule |
|---|---|
| 1.1 | `POST /auth/register` with `email`, `username`, `password` → **201**, body = the account, `Location` header set. The body never contains `password` nor `passwordHash`. |
| 1.2 | Same email twice → **409**. |
| 1.3 | Missing or invalid field (bad email, password < 6 chars) → **422** + `fields`. |
| 1.4 | `GET /auth/me` with valid Basic credentials → **200** (`id`, `email`, `username`, `role`, `registrationDate`); anonymous or wrong password → **401**. |
| 1.5 | The password is stored hashed (BCrypt). |
| 1.6 | Every new account has the role `SELLER`. |

| Method | Path | Auth | Success |
|---|---|---|---|
| POST | `/auth/register` | public | 201 |
| GET | `/auth/me` | HTTP Basic | 200 |

### Verify

- [ ] `curl -i -X POST $API/auth/register -H 'Content-Type: application/json' -d '{"email":"alice@test.org","username":"alice","password":"secret123"}'` → **201**, `Location:` header, `"role":"SELLER"`, no `password` in the body
- [ ] Same command again → **409** + JSON body with a `message`
- [ ] `curl -i -X POST $API/auth/register -H 'Content-Type: application/json' -d '{"email":"nope","username":"x","password":"123"}'` → **422** + `fields.email` and `fields.password`
- [ ] Register Bob the same way (`bob@test.org` / `secret123`) → **201**
- [ ] `curl -i $API/auth/me` → **401**
- [ ] `curl -i -u "$ALICE" $API/auth/me` → **200**, `"email":"alice@test.org"`
- [ ] `curl -i -u alice@test.org:wrong $API/auth/me` → **401**
- [ ] `$PSQL "select email, role, left(password_hash, 4) from app_user"` → hashes start with `$2a$` (BCrypt), never the raw password

### If you are stuck

- Validation is triggered by `@Valid` on the `@RequestBody` parameter; the 422 mapping is already in `GlobalExceptionHandler`.
- `Location`: `ServletUriComponentsBuilder.fromCurrentRequest()` + `ResponseEntity.created(uri)`.
- Current user: an `Authentication` (or `@AuthenticationPrincipal UserDetails`) method parameter, or `SecurityContextHolder.getContext().getAuthentication()`. Its `getName()` is the email (see `SecurityConfig#userDetailsService`).
- 401 on `/auth/me` is produced by Spring Security before your code runs — nothing to code for it.
- Docs: Spring Security 7, *Servlet Authentication Architecture* — `SecurityContextHolder`.

---

## Step 2 - Listings: create and browse (V1)   [CORE]

**Outcome** — an authenticated `SELLER` publishes a listing; anyone can read one
listing or browse a paginated, filtered list without an account.

### Build it

1. **Create** `src/main/resources/db/changelog/v1.0.2-listing.yaml` — one new
   changeSet that **adds** the V1 columns to `listing`: `title`, `description`,
   `price`, `category`, `creation_date`, `active`, `user_id` (`addColumn` only;
   never edit `v1.0-init.yaml`, it is already applied).
   - *Why:* The schema is code: a new changeSet adds the columns without touching what is already applied in every database.
2. **Modify** `db.changelog-master.yaml` — include `v1.0.2-listing.yaml` after
   `v1.0.1-identity.yaml`.
   - *Why:* Liquibase only runs the files listed in the master changelog, in that order.
3. **Modify** `domain/entity/Listing.java` — map the V1 columns and the
   `@ManyToOne` relation `owner` (→ `user_id`), with getters and fluent setters.
   - *Why:* `ddl-auto` is `none`, so Hibernate only maps the columns Liquibase created; the `owner` relation is what ties a listing to its owner.
4. **Complete** `web/dto/ListingRequestDto.java` (V1 writable fields + validation)
   and `web/dto/ListingResponseDto.java` (V1 public fields only: no `active`).
   - *Why:* Separate request and response DTOs keep the HTTP contract independent of the entity, and validation rejects bad input before it reaches the service.
5. **Create** `domain/exception/ResourceNotFoundException.java` and **add** its
   `@ExceptionHandler` (→ 404) in `GlobalExceptionHandler`.
   - *Why:* A dedicated domain exception lets the service say "not found" without knowing anything about HTTP.
6. **Modify** `domain/repository/ListingRepository.java` — paginated queries for
   the list, with the optional filters `category` and `maxPrice`, active listings only.
   - *Why:* Filtering and paginating in the database avoids loading the whole table in memory.
7. **Implement** `create`, `findById` and the list use-case in
   `ListingServiceImpl` (the creator becomes the owner; `active = true`;
   `creationDate = now`).
   - *Why:* The service is the single place where business rules live (who owns what, default values).
8. **Modify** `ListingController` — `POST /listings`, `GET /listings`,
   `GET /listings/{id}`; return the list as a `PagedModel` (see Conventions).
   - *Why:* `PagedModel` gives the list a stable JSON shape (`content` + `page`) that clients can rely on.
9. **Modify** `config/SecurityConfig.java` — make `GET /listings` and
   `GET /listings/**` public. All other `/listings` requests stay authenticated.
   - *Why:* Browsing must not require an account: Spring Security denies everything not explicitly allowed.
10. **Rebuild** and run `Verify`.
    - *Why:* Liquibase applies the new changeSet at startup, so the stack must restart with the new image.

### Expected behavior

| # | Rule |
|---|---|
| 2.1 | `POST /listings` by an authenticated `SELLER` → **201**, body = the created listing (`id`, `creationDate` set), `Location` header. The caller becomes the owner. |
| 2.2 | `GET /listings` (public) → **200**, paginated (`?page=`, `?size=`, default size ≤ 20), shape of *Conventions*. Optional filters `category` and `maxPrice` (inclusive), combinable. |
| 2.3 | `GET /listings/{id}` (public) → **200**, or **404** + JSON body if unknown. |
| 2.4 | Invalid payload (title empty or > 120, description > 2000, price ≤ 0, missing category) → **422** + `fields`. |
| 2.5 | `POST /listings` without credentials → **401**. |
| 2.6 | The V1 response contains only `id`, `title`, `description`, `price`, `category`, `creationDate`. |

| Method | Path | Auth | Success |
|---|---|---|---|
| POST | `/listings` | `SELLER` | 201 |
| GET | `/listings` | public | 200 |
| GET | `/listings/{id}` | public | 200 |

### Verify

- [ ] `$PSQL "select id from databasechangelog order by orderexecuted"` → contains your `v1.0.2` changeSet id after `v1.0.1-1`
- [ ] `$PSQL "\d listing"` → the 7 V1 columns exist
- [ ] `curl -i -u "$ALICE" -X POST $API/listings -H 'Content-Type: application/json' -d '{"title":"Bike","description":"City bike","price":120.00,"category":"SPORTS"}'` → **201**, `id` and `creationDate` in the body, no `active` field
- [ ] Create two more as Alice: a `BOOKS` at `15.00` and a `SPORTS` at `900.00`
- [ ] `curl -i $API/listings` (anonymous) → **200**, `content` has 3 items, `page.totalElements` = 3
- [ ] `curl -s "$API/listings?category=SPORTS&maxPrice=200"` → only the bike
- [ ] `curl -s "$API/listings?size=2&page=1"` → 1 item, `page.number` = 1
- [ ] `curl -i $API/listings/1` → **200** · `curl -i $API/listings/9999` → **404** + JSON body
- [ ] `curl -i -X POST $API/listings -H 'Content-Type: application/json' -d '{"title":"x","price":1,"category":"BOOKS"}'` → **401**
- [ ] `curl -i -u "$ALICE" -X POST $API/listings -H 'Content-Type: application/json' -d '{"title":"","price":0}'` → **422** + `fields.title`, `fields.price`, `fields.category`
- [ ] `$PSQL "select id, title, active, user_id from listing"` → `active = t`, `user_id` = Alice's id

### If you are stuck

- Liquibase `addColumn` example: README, *Schema migrations (Liquibase)*. Column types: `VARCHAR(120)`, `VARCHAR(2000)`, `NUMERIC(12,2)`, `VARCHAR(32)`, `TIMESTAMP`, `BOOLEAN`, `BIGINT`.
- `ddl-auto` is `none`: if Hibernate complains about a missing column, the changelog is missing it.
- `category` is an enum stored as text: `@Enumerated(EnumType.STRING)`.
- Owner relation: `@ManyToOne(fetch = LAZY) @JoinColumn(name = "user_id")`. The current user: same technique as Step 1 `/auth/me`.
- Optional filters: derived queries (`findByActiveTrueAndCategoryAndPriceLessThanEqual(..., Pageable)`) or one `@Query` with `(:category is null or l.category = :category)`. Spring Data JPA 4 docs, *Query Methods*.
- Pagination: a `Pageable` controller parameter (`@PageableDefault(size = 20)`); return `new PagedModel<>(page)` (`org.springframework.data.web.PagedModel`).
- Public GET: `requestMatchers(HttpMethod.GET, "/listings", "/listings/**").permitAll()` **before** `anyRequest()`.
- DTO validation: `@NotBlank`, `@Size`, `@Positive`, `@NotNull` (jakarta.validation) + `@Valid` in the controller.

---

## Step 3 - Security rules: ownership and lifecycle   [CORE]

**Outcome** — only a listing's owner can update, hide or delete it; a hidden
listing disappears for everyone else, and the full security matrix holds.

### Build it

1. **Modify** `config/SecurityConfig.java` — `POST`, `PUT`, `DELETE` on
   `/listings/**` require the role `SELLER`; `/auth/register`, `/actuator/**`,
   OpenAPI and Swagger UI stay public.
   - *Why:* The role answers "may this caller write at all?" (401 otherwise); it is checked before any controller code runs.
2. **Modify** `ListingRequestDto` — accept an optional `active` boolean (write-only).
   - *Why:* `active` is how the owner hides a listing without deleting it.
3. **Implement** `update` and `delete` in `ListingServiceImpl` — load the listing
   (404 if unknown), then compare its owner with the current user: not the owner
   → throw `AccessDeniedException`.
   - *Why:* Ownership depends on data (who owns this listing), which only the service knows, hence the 403 lives there and not in `SecurityConfig`.
4. **Add** the `@ExceptionHandler(AccessDeniedException.class)` → **403** in
   `GlobalExceptionHandler` (see the *Trap* in Conventions).
   - *Why:* Without an explicit mapping, the provided generic handler turns the exception into a 500.
5. **Modify** `findById` and the list use-case — a hidden listing (`active = false`)
   is visible to its owner only: absent from lists, **404** on detail for others.
   - *Why:* Applying the same visibility rule in list and detail keeps the API consistent: a hidden listing cannot leak through either path.
6. **Modify** `ListingController` — `PUT /listings/{id}` (200 + body) and
   `DELETE /listings/{id}` (204).
   - *Why:* PUT and DELETE complete the CRUD so that the ownership rule has something to protect.
7. **Rebuild** and run `Verify`.
   - *Why:* Rebuild before running the security matrix: every cell must be observed on the new code.

### Expected behavior

| # | Rule |
|---|---|
| 3.1 | Reads (`GET /listings*`) are public. |
| 3.2 | Writes (`POST/PUT/DELETE /listings*`) require an authenticated `SELLER` → otherwise **401**. |
| 3.3 | `PUT` / `DELETE` by an authenticated user who is **not the owner** → **403** (never 401, never 404 — the listing exists). |
| 3.4 | `PUT /listings/{id}` replaces the writable fields → **200** + body (an absent `active` leaves it unchanged); `DELETE` → **204**, then `GET` → **404**. |
| 3.5 | `PUT` with `"active": false` hides the listing: absent from `GET /listings` and **404** on `GET /listings/{id}` for anonymous users and other `SELLER`s; the owner still gets it (list and detail). `"active": true` restores it. |
| 3.6 | `/auth/register`, `/actuator/health`, `/actuator/info`, `/actuator/metrics` are public. |

**Client x endpoint x status** (L = a listing owned by Alice):

| Client | GET list | GET L | POST | PUT L | DELETE L |
|---|---|---|---|---|---|
| Anonymous | 200 | 200 (404 if hidden) | 401 | 401 | 401 |
| Bob (`SELLER`, not owner) | 200 | 200 (404 if hidden) | 201 | 403 | 403 |
| Alice (`SELLER`, owner) | 200 | 200 (even hidden) | 201 | 200 | 204 |

### Verify

Use listing `1` (Alice's bike) and listing `2` (Alice's book) from Step 2.

- [ ] `curl -i -X PUT $API/listings/1 -H 'Content-Type: application/json' -d '{"title":"Bike","price":100,"category":"SPORTS"}'` → **401**
- [ ] Same with `-u "$BOB"` → **403** + JSON body
- [ ] Same with `-u "$ALICE"` → **200**, `"price":100.00`
- [ ] `curl -i -u "$BOB" -X DELETE $API/listings/2` → **403**
- [ ] `curl -i -u "$ALICE" -X PUT $API/listings/1 -H 'Content-Type: application/json' -d '{"title":"Bike","price":100,"category":"SPORTS","active":false}'` → **200**
- [ ] `curl -s $API/listings` and `curl -s -u "$BOB" $API/listings` → listing 1 absent
- [ ] `curl -i $API/listings/1` and `curl -i -u "$BOB" $API/listings/1` → **404**
- [ ] `curl -s -u "$ALICE" $API/listings` → listing 1 present · `curl -i -u "$ALICE" $API/listings/1` → **200**
- [ ] Same `PUT` with `"active":true` → listing 1 is back in `curl -s $API/listings` (keep it active for the next steps)
- [ ] `curl -i -u "$ALICE" -X DELETE $API/listings/2` → **204** · then `curl -i $API/listings/2` → **404**
- [ ] `curl -i $API/actuator/health`, `.../actuator/info`, `.../actuator/metrics` → **200** without credentials
- [ ] Every cell of the matrix above has been observed at least once

### If you are stuck

- Matchers: `requestMatchers(HttpMethod.POST, "/listings/**").hasRole("SELLER")` (same for PUT, DELETE). `hasRole("SELLER")` matches the authority `ROLE_SELLER` built by `SecurityConfig#userDetailsService`.
- Who answers what: **401** comes from the security filter (no or bad credentials); **403** comes from **your service** (credentials OK, wrong owner). That is why the owner check is not in `SecurityConfig`.
- Owner check: compare ids (`listing.getOwner().getId()` vs current user id), not entity instances.
- Visibility: one condition, reused by list and detail — "active, **or** owned by the caller". Anonymous callers have no id.
- Docs: Spring Security 7, *Authorize HttpServletRequests*.

---

## Step 4 - Content negotiation (JSON + XML)   [BONUS]

**Outcome** — the same endpoints read and write XML as well as JSON, chosen by
`Content-Type` and `Accept`; errors stay JSON.

### Build it

1. **Add** the Jackson XML dependency (exact block: **README**, *Optional
   dependency edits*; what/why: Appendix B).
   - *Why:* Spring Boot ships a JSON converter only; the XML module adds the second format without touching the DTOs.
2. **Add** `@ExceptionHandler`s for `HttpMediaTypeNotSupportedException` (→ 415)
   and `HttpMediaTypeNotAcceptableException` (→ 406) in `GlobalExceptionHandler`.
   - *Why:* The framework detects unsupported media types itself; mapping them keeps the error format of the Conventions.
3. **Force** every error response of `GlobalExceptionHandler` to
   `application/json`, whatever the `Accept` header.
   - *Why:* A client that cannot parse XML errors would be stuck; a single error format keeps error handling simple for every client.
4. **Rebuild** and run `Verify`.
   - *Why:* Content negotiation is set up when the context starts, so the new dependency needs a rebuilt image.

### Expected behavior

| # | Rule |
|---|---|
| 4.1 | `GET` endpoints answer `application/json` or `application/xml` according to `Accept` (default JSON). |
| 4.2 | `POST` / `PUT` accept `application/json` or `application/xml` according to `Content-Type`. |
| 4.3 | Same field names, structure and values in both formats. |
| 4.4 | Unsupported `Content-Type` (e.g. `application/octet-stream`) → **415**. |
| 4.5 | Unsupported `Accept` (e.g. `text/csv`) → **406**. |
| 4.6 | Every 4xx body is JSON, even when XML was requested. |

### Verify

- [ ] `curl -i -u "$ALICE" -X POST $API/listings -H 'Content-Type: application/xml' -H 'Accept: application/xml' -d '<listing><title>Lamp</title><price>25.00</price><category>HOUSEHOLD</category></listing>'` → **201**, `Content-Type: application/xml`
- [ ] `curl -i -H 'Accept: application/xml' $API/listings/1` → **200** XML · same with `Accept: application/json` → **200** JSON, identical values
- [ ] `curl -i -u "$ALICE" -X POST $API/listings -H 'Content-Type: application/octet-stream' -d 'x'` → **415**, JSON body
- [ ] `curl -i -H 'Accept: text/csv' $API/listings/1` → **406**, JSON body
- [ ] `curl -i -H 'Accept: application/xml' $API/listings/9999` → **404**, `Content-Type: application/json`

### If you are stuck

- Once `jackson-dataformat-xml` is on the classpath, Boot registers the XML converter: the DTOs do not change.
- Forcing JSON on errors: `ResponseEntity.status(...).contentType(MediaType.APPLICATION_JSON).body(...)`.
- 406 on the error itself: if the response cannot be written as JSON because of `Accept: text/csv`, check that the handler sets the content type explicitly.
- Docs: Spring Boot 4.1.1, *Servlet Web Applications — HttpMessageConverters*.

---

## Step 5 - Schema evolution (V2 columns)   [CORE]

**Outcome** — the database gains the V2 columns through one additive migration;
the V1 API is unchanged and existing rows are untouched.

### Build it

1. **Create** `src/main/resources/db/changelog/v1.1-v2-fields.yaml` — one
   changeSet that **adds** 7 nullable columns to `listing`: `negotiable` (BOOLEAN),
   `street_address`, `city`, `postal_code`, `country` (VARCHAR), `latitude`,
   `longitude` (DOUBLE PRECISION). `addColumn` only.
   - *Why:* Additive, nullable columns let the schema evolve without breaking existing rows or the running V1 API.
2. **Modify** `db.changelog-master.yaml` — include it last (the commented line is already there).
   - *Why:* Adding the file last guarantees it runs after every previous migration.
3. **Modify** `domain/entity/Listing.java` — map the 7 attributes (no DTO change yet).
   - *Why:* Mapping the columns now prepares V2 while keeping the V1 contract unchanged.
4. **Rebuild** and run `Verify`.
   - *Why:* The restart applies the migration: `DATABASECHANGELOG` then shows exactly what ran.

### Expected behavior

| # | Rule |
|---|---|
| 5.1 | The migration is **additive**: no existing column is modified or dropped; no published changelog is edited. |
| 5.2 | The new columns are **nullable**; rows created before the migration keep `null`. |
| 5.3 | The Step 2-3 behavior is unchanged (V1 responses do not show the new fields). |
| 5.4 | `mvn clean verify` stays green. |

### Verify

- [ ] `$PSQL "select id, filename from databasechangelog order by orderexecuted"` → `v1.0-1`, `v1.0-2`, `v1.0.1-1`, your `v1.0.2` changeSet(s), then your `v1.1` changeSet — no row disappeared
- [ ] `$PSQL "\d listing"` → the 7 new columns, all nullable
- [ ] `$PSQL "select id, negotiable, city, latitude from listing"` → listings created in Step 2 have `null` in the new columns
- [ ] `curl -s $API/listings/1` → same fields as before (no `negotiable`, no address)
- [ ] `docker compose logs listings-api | grep -i "liquibase"` → no checksum error (`ValidationFailedException`)

### If you are stuck

- Reuse the `addColumn` example of the README, *Schema migrations (Liquibase)*.
- Checksum error at startup = an already-applied changeSet was edited. Revert that edit; add a new changeSet instead.
- Java field names are camelCase, columns snake_case: `@Column(name = "street_address")`.
- Docs: Liquibase, *addColumn* change type.

---

## Step 6a - Versioning: routing, default, deprecation   [CORE]

**Outcome** — `/listings` is served in two versions selected by `?version=`, V1 by
default, unknown versions are refused with **400**, and V1 announces its sunset.

> In this step V2 returns the **same fields as V1** plus `negotiable`. The
> owner-only fields come in Step 6b.

### Build it

1. **Modify** `config/ApiVersioningConfig.java` — override
   `configureApiVersioning(ApiVersionConfigurer)`: resolve the version from the
   query parameter `version`, supported versions `V1` and `V2`, default `V1`.
   - *Why:* Declaring versions centrally lets Spring route each request to the right handler and reject unknown versions for you.
2. **Split** `ListingController` into `ListingControllerV1` and
   `ListingControllerV2` (same `/listings` path, `version = "V1"` / `"V2"` on the
   mappings). Both delegate to the same `ListingService`.
   - *Why:* One controller per version keeps each contract readable; the shared service guarantees identical business rules.
3. **Create** the V2 response DTO (`ListingV2ResponseDto`: V1 fields + `negotiable`)
   and **accept** `negotiable` in the V2 request DTO.
   - *Why:* A distinct V2 DTO makes the contract difference explicit instead of hiding it behind conditions.
4. **Add** an `@ExceptionHandler` for the framework's
   `InvalidApiVersionException` → **400** with the body of rule 6.3.
   - *Why:* The framework raises the exception; mapping it gives the client the list of versions it can use.
5. **Add** the `Deprecation` and `Sunset` headers on every V1 response, and only V1.
   - *Why:* `Deprecation` and `Sunset` tell existing V1 clients, in a machine-readable way, that they must migrate and by when.
6. **Rebuild** and run `Verify`.
   - *Why:* Routing changes are only visible on a restarted application.

### Expected behavior

| # | Rule |
|---|---|
| 6.1 | `?version=V1` or `?version=V2` selects the version (`v1` / `v2` also accepted). |
| 6.2 | No `version` parameter → **V1**. |
| 6.3 | Unknown version (e.g. `V99`) → **400**, body contains `"error": "API version not available"` and `"availableVersions": ["V1", "V2"]` (+ the usual `timestamp`, `status`, `message`). |
| 6.4 | V1 responses carry `Deprecation` (value `true`, or the RFC 9745 date form `@<epoch>`) and `Sunset: Sun, 01 Jan 2027 00:00:00 GMT`. |
| 6.5 | V2 responses carry neither header. |
| 6.6 | V1 never returns `negotiable`, `active`, address or geo fields. V2 returns `negotiable`. |
| 6.7 | Pagination, filters, ownership (403) and hidden listings (404) behave identically in V1 and V2. |

### Verify

- [ ] `curl -i $API/listings/1` → **200**, `Deprecation:` and `Sunset: Sun, 01 Jan 2027 00:00:00 GMT` headers, no `negotiable`
- [ ] `curl -s "$API/listings/1?version=V1"` → same body as without parameter
- [ ] `curl -i -u "$ALICE" -X PUT "$API/listings/1?version=V2" -H 'Content-Type: application/json' -d '{"title":"Bike","price":100,"category":"SPORTS","active":true,"negotiable":true}'` → **200**, `"negotiable":true`, no `Deprecation` / `Sunset` headers
- [ ] `curl -i "$API/listings/1?version=V2"` → **200**, `"negotiable":true`, no `Deprecation` / `Sunset` headers
- [ ] `curl -s "$API/listings/1"` → no `negotiable` (V1)
- [ ] `curl -i "$API/listings/1?version=v2"` → **200** V2
- [ ] `curl -i "$API/listings?version=V99"` → **400**, `"availableVersions":["V1","V2"]`
- [ ] `curl -i -u "$BOB" -X PUT "$API/listings/1?version=V2" -H 'Content-Type: application/json' -d '{"title":"x","price":1,"category":"SPORTS"}'` → **403**
- [ ] `curl -s "$API/listings?version=V2&category=SPORTS&size=1"` → paginated like V1

### If you are stuck

- `ApiVersionConfigurer`: `useQueryParam("version")`, `addSupportedVersions(...)`, `setDefaultVersion(...)`. Spring Framework 7 reference, *Web MVC — API Versioning*.
- Mapping side: `@GetMapping(version = "V1")` or `@RequestMapping(path = "/listings", version = "V1")` at class level.
- `V1` vs `1`: the default parser is semantic (`major.minor.patch`). If it rejects the `V` prefix or the lower case, give the configurer your own `ApiVersionParser` (`setVersionParser`) that normalises the value (upper-case, strip the `V`).
- With supported versions declared, an unknown version raises `org.springframework.web.accept.InvalidApiVersionException` before any controller runs. Map it in `GlobalExceptionHandler` like the other exceptions — do not create your own class with the same name.
- Headers: `ApiVersionConfigurer#setDeprecationHandler` with a `StandardApiVersionDeprecationHandler` (configure V1 with a deprecation date and the sunset date), or add the two headers yourself in `ListingControllerV1` (`ResponseEntity.ok().header(...)`).
- The `listings.api.*` keys in `application.yml` already hold `V1` / `V1,V2`.

---

## Step 6b - V2 contract: visibility and OpenAPI   [CORE]

**Outcome** — V2 shows the owner-only fields to the owner alone, and the API
publishes one OpenAPI document per version.

### Build it

1. **Modify** `ListingV2ResponseDto` — add `active`, `streetAddress`, `city`,
   `postalCode`, `country`, `latitude`, `longitude`.
   - *Why:* The V2 contract carries the owner-only fields; whether they are filled is decided per caller.
2. **Modify** the V2 request DTO — accept the address fields (`latitude` /
   `longitude` are not writable: Step 7 computes them).
   - *Why:* Coordinates are computed by the server (Step 7), so a client must not be able to set them.
3. **Modify** `ListingControllerV2` (or a mapper) — fill the owner-only fields
   **only** when the caller is the owner; leave them `null` otherwise.
   - *Why:* With Jackson `non_null`, a field left `null` is not serialized: that is how the same DTO serves owners and others.
4. **Create** an OpenAPI configuration class in `config/` — two `GroupedOpenApi`
   beans (`v1`, `v2`), each with its own title.
   - *Why:* One OpenAPI document per version lets each client generate code against the exact contract it uses.
5. **Rebuild** and run `Verify`.
   - *Why:* Rebuild and compare the same request as owner, as another seller and anonymously.

### Expected behavior

| # | Rule |
|---|---|
| 6.8 | **V2, not the owner** (anonymous or other `SELLER`): public fields + `negotiable`; never `active`, `streetAddress`, `city`, `postalCode`, `country`, `latitude`, `longitude`. |
| 6.9 | **V2, owner**: all fields, including the owner-only ones. |
| 6.10 | **V1**: unchanged — never the V2 fields, for any caller. |
| 6.11 | Two OpenAPI documents, one per version, with distinct titles. |

| Field | V1 (all) | V2 not owner | V2 owner |
|---|:--:|:--:|:--:|
| public fields | yes | yes | yes |
| `negotiable` | no | yes | yes |
| `active`, address, `latitude`, `longitude` | no | no | yes |

### Verify

- [ ] `curl -i -u "$ALICE" -X PUT "$API/listings/1?version=V2" -H 'Content-Type: application/json' -d '{"title":"Bike","price":100,"category":"SPORTS","negotiable":true,"streetAddress":"10 rue de Rivoli","city":"Paris","postalCode":"75004","country":"France"}'` → **200**, address fields present
- [ ] `curl -s -u "$ALICE" "$API/listings/1?version=V2"` → `active`, `streetAddress`, `city`, `postalCode`, `country` present
- [ ] `curl -s "$API/listings/1?version=V2"` and `curl -s -u "$BOB" "$API/listings/1?version=V2"` → `negotiable` present; no `active`, no address, no `latitude` / `longitude`
- [ ] `curl -s -u "$ALICE" "$API/listings/1?version=V1"` → none of the V2 fields
- [ ] `curl -s -u "$ALICE" "$API/listings?version=V2"` → owner fields on Alice's items only (create one as Bob to compare)
- [ ] `curl -s $API/v3/api-docs/v1 | grep -o '"title":"[^"]*"'` and same for `v2` → two different titles
- [ ] Swagger UI (`$API/swagger-ui/index.html`) offers a V1 / V2 selector

### If you are stuck

- `application.yml` sets Jackson to `non_null`: a field left `null` is not serialized. Leaving owner-only fields `null` for non-owners is enough.
- The owner test is the same as in Step 3 (ids compared); anonymous callers are never owners.
- `GroupedOpenApi.builder().group("v1").addOpenApiCustomizer(api -> api.info(new Info().title(...)))`; restrict each group to its handlers (`addOperationCustomizer`, or `pathsToMatch` + the version). springdoc 3 docs, *Groups*.

---

## Step 7 - Geolocation (GPS enrichment)   [BONUS]

**Outcome** — when an owner gives an address in V2, the API resolves `latitude`
and `longitude` through Nominatim; a geocoder failure never fails the request.

### Build it

1. **Create** a port `application/service/GeocodingPort.java` (address in,
   optional coordinates out) and an adapter `NominatimGeocodingClient` using
   `RestClient`, configured from `listings.geo.*` (`application.yml`).
   - *Why:* A port isolates the external service: the business code stays testable and the geocoder can be replaced.
2. **Configure** the adapter with a 3-second connect + read timeout and a
   `User-Agent` header (Nominatim usage policy, Appendix A).
   - *Why:* A slow external service must not freeze your API, and Nominatim refuses anonymous clients.
3. **Call** the port from `ListingServiceImpl` on V2 **create** and **update**,
   only when `streetAddress` is non-blank.
   - *Why:* Calling only when an address is given avoids useless calls to a rate-limited public service.
4. **Handle** every failure (network, timeout, non-2xx, empty array, bad number):
   log one WARN line with the address and the reason, keep the coordinates `null`,
   continue.
   - *Why:* Enrichment is best effort: a geocoder outage must never fail the user's request.
5. **Rebuild** and run `Verify`.
   - *Why:* Rebuild, then test the success path and the failure path separately.

### Expected behavior

| # | Rule |
|---|---|
| 7.1 | V2 create / update with a non-blank `streetAddress` → one geocoding call (`street, postalCode city, country`). |
| 7.2 | Success → coordinates persisted and returned to the owner. |
| 7.3 | Failure (network, timeout, no result) → the request still succeeds (**201** / **200**), coordinates `null`, one WARN log line with the address and the reason. |
| 7.4 | The geocoding call never blocks the request for more than **3 s**. |
| 7.5 | No `streetAddress` → no call, coordinates `null`. |

### Verify

- [ ] The Step 6b `PUT` (10 rue de Rivoli, Paris) → `latitude` ≈ 48.85, `longitude` ≈ 2.35 in the owner's V2 response
- [ ] `$PSQL "select id, latitude, longitude from listing where id = 1"` → values persisted
- [ ] Failure path: add `LISTINGS_GEO_ENDPOINT: "http://10.255.255.1"` (non-routable) to `listings-api` in `docker-compose.yml`, `docker compose up -d`, then `curl -s -o /dev/null -w '%{http_code} %{time_total}\n' -u "$ALICE" -X PUT "$API/listings/1?version=V2" -H 'Content-Type: application/json' -d '{"title":"Bike","price":100,"category":"SPORTS","streetAddress":"1 rue X","city":"Paris"}'` → `200`, time ≤ ~3.5 s
- [ ] `docker compose logs listings-api | grep WARN` → one line with `1 rue X` and the reason (timeout)
- [ ] V2 PUT without `streetAddress` → `200`, no new WARN, no new geocoding log line
- [ ] Remove the `LISTINGS_GEO_ENDPOINT` override afterwards

**Optional extension** — a remediation path for failed enrichments (retry with
backoff, `POST /listings/{id}/geocode` re-sync, or a pending queue). Document the choice in your README.

### If you are stuck

- `RestClient.builder().baseUrl(...).requestFactory(factory).defaultHeader("User-Agent", ...)`; timeouts on `SimpleClientHttpRequestFactory` (`setConnectTimeout`, `setReadTimeout`) or `ClientHttpRequestFactorySettings`. Spring Framework 7 reference, *REST Clients*.
- Call `/search?format=json&limit=1&q={q}` with `uri(builder -> ...)` so that `q` is encoded. The response is a JSON **array**; `lat` / `lon` are **strings**.
- `@ConfigurationProperties(prefix = "listings.geo")` binds `endpoint` and `timeout-ms`; the env var `LISTINGS_GEO_ENDPOINT` overrides `listings.geo.endpoint`.
- One `try/catch (RestClientException | NumberFormatException e)` around the call → `log.warn(...)`.

---

## Step 8 - Metrics   [CORE]

**Outcome** — the service exposes a counter of successful listing creations,
tagged with the application name.

### Build it

1. **Inject** `MeterRegistry` in `ListingServiceImpl`.
   - *Why:* `MeterRegistry` is the entry point of Micrometer: what you register there is exposed by Actuator (and scraped by Prometheus in the Monitoring TP).
2. **Increment** the counter `listings.created` once per successful creation
   (after the listing is saved, whatever the API version).
   - *Why:* A business counter shows what the service does, not only that it is up.
3. **Rebuild** and run `Verify`.
   - *Why:* The counter must be read on the running application, before and after a creation.

### Expected behavior

| # | Rule |
|---|---|
| 8.1 | `listings.created` (counter) increases by 1 on every **201** of `POST /listings`; a 401 / 422 does not increment it. |
| 8.2 | The metric carries the tag `application=listings` (already configured in `application.yml`). |

### Verify

- [ ] `curl -s $API/actuator/metrics/listings.created` → note `measurements[0].value` (N)
- [ ] Create one listing (`POST`, **201**) → value is N + 1
- [ ] Send an invalid `POST` (**422**) → value stays N + 1
- [ ] Same JSON → `availableTags` contains `{"tag":"application","values":["listings"]}`
- [ ] `grep -rn '"listings.created"' src/main/java` → exactly one place registers the counter

### If you are stuck

- `meterRegistry.counter("listings.created").increment()`; or build the `Counter` once in the constructor.
- The metric only appears in `/actuator/metrics` after its first registration (first creation, or eager registration in the constructor).
- Docs: Spring Boot 4.1.1, *Actuator — Metrics — Registering Custom Metrics*.

---

## Step 9 - Distance search   [BONUS]

**Outcome** — in V2, a visitor finds the listings within a radius (km) around a
point, closest first.

### Build it

1. **Modify** `ListingControllerV2` — accept the optional `lat`, `lng`, `radius`
   query parameters on `GET /listings`.
   - *Why:* Distance search is a V2 feature: it relies on the coordinates that only V2 exposes.
2. **Validate** them (all three or none, ranges) → **400** via a dedicated
   exception and `@ExceptionHandler`.
   - *Why:* Rejecting incomplete or out-of-range input early gives a clear 400 instead of a wrong result.
3. **Implement** the search (in memory, SQL, or bounding box + exact distance —
   your choice), applying the Step 3 visibility and the Step 6b field rules.
   - *Why:* Several approaches are valid; choosing one and knowing its cost is the point of the step.
4. **Make** V1 reject these parameters → **400**.
   - *Why:* V1 is frozen: new features must not change its contract.
5. **Document** your approach and its limits in your README.
   - *Why:* Explaining the trade-off (precision, performance) is part of the deliverable.
6. **Rebuild** and run `Verify`.
   - *Why:* Check both positive and negative cases on the rebuilt application.

### Expected behavior

| # | Rule |
|---|---|
| 9.1 | `GET /listings?version=V2&lat=&lng=&radius=` returns the listings whose coordinates are within `radius` km. |
| 9.2 | Listings without coordinates are excluded. |
| 9.3 | `lat`, `lng`, `radius` are required together → otherwise **400**. |
| 9.4 | `radius` is an integer ≥ 1 → otherwise **400**. |
| 9.5 | `lat` in [-90, 90], `lng` in [-180, 180] → otherwise **400**. |
| 9.6 | Same parameters on V1 → **400**. |
| 9.7 | Visibility (hidden listings, owner-only fields) as in Steps 3 and 6b. |
| 9.8 | *(optional)* Sorted by distance, closest first. |

### Verify

- [ ] With listing 1 geocoded in Paris: `curl -s "$API/listings?version=V2&lat=48.8566&lng=2.3522&radius=5"` → contains listing 1
- [ ] `curl -s "$API/listings?version=V2&lat=45.76&lng=4.83&radius=5"` (Lyon) → does not contain listing 1
- [ ] `curl -i "$API/listings?version=V2&lat=48.8&lng=2.3"` → **400**
- [ ] `curl -i "$API/listings?version=V2&lat=48.8&lng=2.3&radius=0"` → **400**
- [ ] `curl -i "$API/listings?version=V2&lat=91&lng=2.3&radius=5"` → **400**
- [ ] `curl -i "$API/listings?lat=48.8&lng=2.3&radius=5"` (V1) → **400**
- [ ] Your README has a short section justifying the approach

### If you are stuck

- Haversine distance: `2 R asin(sqrt(sin^2(dLat/2) + cos(lat1) cos(lat2) sin^2(dLng/2)))`, `R = 6371` km.
- A bounding box (`latitude between ... and ...`) in SQL reduces the candidates; the exact distance filters the rest.
- `@RequestParam(required = false) Double lat` lets you detect "some but not all" parameters yourself.

---

## Step 10 - Final checks   [DELIVERY]

**Outcome** — anyone can clone the repository, start it, and check the core in
10 minutes.

### Build it

1. **Complete** your team's `README.md`: how to run, every endpoint with one
   example (`curl`), versioning usage, metrics, the bonus steps you did, your
   technical choices.
   - *Why:* Another team must be able to run and check your work without asking you anything.
2. **Remove** every `TODO` and `Not implemented yet` from `src/main`.
   - *Why:* Leftover stubs mean unfinished code paths in production.
3. **Run** the `Final checks` on a fresh stack.
   - *Why:* A fresh stack (`down -v`) proves that the migrations and the run instructions work from zero.

### Expected behavior

| # | Rule |
|---|---|
| 10.1 | `mvn clean verify` is green (the provided smoke test, plus your own tests if you wrote any — they must pass). |
| 10.2 | The stack starts from scratch (`docker compose down -v && docker compose up -d --build`) and applies all migrations. |
| 10.3 | The README is enough to reproduce the core `Verify` lists. |
| 10.4 | Oral: 10 minutes (architecture, key decisions, demo) + 5 minutes Q&A. |

### Final checks

- [ ] `docker compose down -v && docker compose up -d --build` → `curl -fsS $API/actuator/health` → `UP`
- [ ] `mvn clean verify` → `BUILD SUCCESS`
- [ ] `grep -rn "TODO\|Not implemented yet" src/main` → no output
- [ ] Steps 1, 2, 3, 5, 6a, 6b, 8: every `Verify` box re-ticked on the fresh stack
- [ ] README: run instructions, endpoint examples, versioning, metrics, choices

### Deliverables

| Item | Detail |
|---|---|
| Git repository (pair) | Clean history, meaningful commits |
| `mvn clean verify` | Green |
| `README.md` | Run instructions, endpoint examples, decisions |
| Oral | 10 min presentation + 5 min Q&A |

---

## Appendix A - Nominatim (geocoding)

Free public geocoder (OpenStreetMap). Reference for Step 7.

- Base URL: `https://nominatim.openstreetmap.org` (`listings.geo.endpoint`).
- Endpoint: `GET /search?format=json&limit=1&q={address}` (`q` URL-encoded;
  street + postal code + city + country gives the best results).
- Response (array — `lat` / `lon` are **strings**):

  ```json
  [
    {
      "place_id": 78,
      "lat": "48.85340613427995",
      "lon": "2.3488002225875854",
      "display_name": "Paris, France",
      "type": "city",
      "class": "place"
    }
  ]
  ```

- An empty array or a non-2xx response = "no result" → rule 7.3.
- Usage policy: ~1 request/second, and a `User-Agent` identifying your app is
  required. Never call it in a loop, never from an automated test.

## Appendix B - XML support (Step 4)

| What | Coordinates | Why |
|---|---|---|
| Jackson XML module | `tools.jackson.dataformat:jackson-dataformat-xml` (version managed by the Spring Boot BOM) | `spring-boot-starter-web` ships the JSON converter only; with this module on the classpath Boot registers the XML converter automatically, and the DTOs stay unchanged. |

- The edit is additive and the only `pom.xml` change of this TP; the exact block
  lives in **README**, *Optional dependency edits*.
- Element names follow the Java property names (`<negotiable>true</negotiable>`).

## Appendix C - Writing your own tests (optional)

Tests are not part of the deliverable; they are a good way to replay the `Verify`
lists automatically. `spring-boot-starter-test` and `spring-security-test` are on
the classpath (JUnit 5, AssertJ, Mockito).

| Level | What it covers | Boots Spring? | Typical tools |
|---|---|---|---|
| **Unit** | one service with its collaborators mocked (ownership, visibility, versioning rules) | no | `@ExtendWith(MockitoExtension.class)` |
| **Slice** | controllers + validation + `@ControllerAdvice`, service mocked | thin slice | `@WebMvcTest` |
| **Integration** | the real stack: PostgreSQL, Liquibase, JPA, Security | yes | `@SpringBootTest(webEnvironment = RANDOM_PORT)` + the Boot 4 web test client |

- Name each test after the behavior it checks (`nonOwnerUpdateIsRejected`), and
  assert the observable contract (status, JSON fields, headers).
- Simulate Basic users with `httpBasic("alice@test.org", "secret123")` from `spring-security-test`.
- Mock the geocoding port (Step 7): never call the real Nominatim from a test.
- Integration tests need a PostgreSQL: the running `listings-postgres` (port 5435),
  or Testcontainers (`org.testcontainers:postgresql`, version managed by the BOM
  already imported in `pom.xml`).
