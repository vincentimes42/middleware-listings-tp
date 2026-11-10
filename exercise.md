# Classifieds API — Progressive Implementation (TP1)

## Overview

You are building a **classifieds listings** micro-service, in pairs. The
skeleton you receive compiles, boots, and passes a context test
(`ListingsSmokeTest`). Your job is to implement the API **feature by
feature**, in the order below. The **core** steps fit in about **5h30**; the
**bonus** steps fill the rest of the session.

The specification describes **behaviors, contracts, and status codes**. It does
**not** prescribe the internal design: structure the code as you see fit, as
long as the **observable behavior** matches the contract.

**Validation method (the contract):** every step has a **`Verify`** section
(Step 10: **`Final checks`**), a checklist of **`curl`** calls, **`psql`**
reads, and **`grep`** of your own Java. That checklist **is** the definition of
done for the step. Every command is copy-pasteable in
[`docs/verification.md`](docs/verification.md). Writing Java tests is
**encouraged but optional**; it is **not** part of the deliverable.
`ListingsSmokeTest` is the minimum that must stay green (`mvn clean verify`).

> **Prerequisite:** none — this is TP1. Install and run instructions are in
> [`README.md`](README.md).

### How to read a step

| Section | What it gives you |
|---|---|
| **Outcome** | the observable result, in one or two lines |
| **Why this step** | the concept taught and the real problem it solves |
| **How it fits** | where the request goes and which part you change (when several layers are involved) |
| **Build it** | the changes, grouped by layer, in order; the `→` line under each item says what breaks without it |
| **Checkpoint** | a quick call to confirm a group works before starting the next one |
| **Expected behavior** | the contract: statuses, bodies, rules |
| **Verify** | the definition of done (commands in `docs/verification.md`) |
| **If you are stuck** | pointers to the class, annotation, or documentation; never the full code |

---

## Before you start — the two tools you will use

New to Maven or Spring Boot? This 2-minute mental model makes the steps below
easier to follow.

### Maven — what you actually do

**Maven builds a Java project from a `pom.xml`** (dependencies, plugins). Two
commands cover this TP:

| Maven command | What it does |
|---|---|
| `mvn spring-boot:run` | start the application (port 8080) |
| `mvn clean verify` | clean, compile, test, and package |

Key idea: to use a library you add a `<dependency>` to `pom.xml`; you never
copy `jar` files by hand.

### Spring Boot — what you actually do

**You write annotated classes; Spring Boot wires the rest.** `@RestController`,
`@Service`, `@Repository`, `@Entity`, `@Configuration` and a few
`application.yml` keys give you the HTTP server, the DataSource, the security
chain, and Liquibase. When a behavior looks implicit, look up the annotation or
the `yml` key in the reference for the **exact versions of this project**:

| Library | Version | Reference |
|---|---|---|
| Spring Boot | 4.1.1 | <https://docs.spring.io/spring-boot/4.1/reference/index.html> |
| Spring Framework (MVC) | 7.0 | <https://docs.spring.io/spring-framework/reference/7.0/web/webmvc.html> |
| Spring Security | 7.1 | <https://docs.spring.io/spring-security/reference/7.1/index.html> |
| Spring Data JPA | 4.1 | <https://docs.spring.io/spring-data/jpa/reference/4.1/index.html> |
| Jackson | 3.1 (`tools.jackson.*`, not `com.fasterxml.jackson.*`) | <https://github.com/FasterXML/jackson> |
| springdoc-openapi | 3.1.1 | <https://springdoc.org/> |

### The layers of this project

Every request crosses the same layers. Each step changes one or more of them.

```
HTTP request
  │
  ▼
security chain    config/SecurityConfig            who may call? (401)
  │
  ▼
controller        web/controller/*                 HTTP in/out: parameters, DTOs, status codes
  │                web/dto/*                        the JSON contract (what the client sends and sees)
  ▼
service           application/service/*            business rules: ownership, visibility, defaults
  │
  ▼
repository        domain/repository/*              queries
  │                domain/entity/*                  Java view of the tables (JPA)
  ▼
database          db/changelog/*.yaml              the tables themselves (Liquibase)

errors from any layer ──> web/advice/GlobalExceptionHandler ──> JSON error + status
```

---

## Domain & data model (this TP)

Everything you build sits on this domain. Each step below is one facet of it.

### Actors

| Actor | What they can do |
|---|---|
| **Visitor** (anonymous) | Browse and read any *active* listing. No account. |
| **Seller** (the only role) | A registered user who creates and manages **their own** listings. Every registered account is a `SELLER`. There is no admin role. |

### Entities

**`User`** — a seller account (table `app_user`, **provided**).

| attribute | note |
|---|---|
| `id` | primary key (surrogate) |
| `email` | **unique**, the Basic-auth login |
| `username` | displayed name |
| `passwordHash` | BCrypt hash; the raw password is never stored |
| `role` | always `SELLER` |
| `registrationDate` | account creation time (ISO-8601) |

**`Listing`** — an item offered for sale (table `listing`, **you complete it**).

| attribute | V1 (Step 2) | V2 (Steps 5-6) | visible to |
|---|:--:|:--:|---|
| `id`, `title` (1-120), `description` (0-2000), `price` (>0), `category` (9 values), `creationDate` | ✓ | ✓ | everyone |
| `active` | stored | ✓ | owner only (V2) |
| `negotiable` | — | ✓ | everyone (V2) |
| `streetAddress`, `city`, `postalCode`, `country`, `latitude`, `longitude` | — | ✓ | owner only (V2) |

### Relationship

`User` **1 —— 0..N** `Listing`. A listing belongs to exactly one seller, its
**owner**; a seller can own many listings. The relation is the `owner`
attribute of `Listing` (`@ManyToOne`), stored in the column `listing.user_id`.

### Lifecycle

A listing is created with `active = true`. The owner can **hide** it
(`active = false`) and show it again later. A hidden listing is excluded from
lists for everyone except its owner, and reads as **404** for non-owners.

> Keep these four ideas in mind (*actors, the two entities, the 1-to-N
> ownership, the active lifecycle*): almost every `Expected behavior` below is
> a rule about one of them.

---

## Scope

| Track | Steps | Indicative time |
|---|---|---|
| **CORE** (required) | 1 · 2 · 3 · 5 · 6 · 8 | ~5h30 |
| **BONUS** (start only when the core `Verify` lists are green) | 4 (JSON + XML) · 7 (GPS enrichment) · 9 (distance search) | ~2h30 |
| **DELIVERY** | 10 (final integration) | ~0h30 |

Work the core steps **in order**. Step 4 is a bonus placed before Step 5: skip
it on the first pass and come back to it once the core is green.

---

## Conventions

- Timestamps are ISO-8601 (`2026-11-10T09:30:00Z`).
- Monetary amounts are decimals with 2 digits (`350.00`).
- Error responses are JSON:
  `{ "timestamp": "...", "status": 4xx, "error": "...", "message": "..." }`,
  plus a `fields` map for validation errors.
- Content type: `application/json` by default; `application/xml` is added in
  Step 4 (bonus).
- API versions: `V1` and `V2`, selected with the query parameter
  `?version=V1|V2` (Step 6). No parameter means V1.
- Authentication: **HTTP Basic**, no login form, no session.
- Test accounts used in every `Verify`: `alice@test.org` (owner) and
  `bob@test.org` (non-owner), password `secret123`.
- In Docker mode, every Java change needs a rebuild:
  `docker compose up -d --build listings-api` (see [`README.md`](README.md)).

---

## Error handling & status codes (cross-cutting)

Every step relies on this table. When `Expected behavior` says "→ 404", the
response carries status **404** and a **JSON body** in the shape above.

| Status | Trigger | Mechanism | Where |
|---|---|---|---|
| 400 | Malformed JSON body, unknown enum value (Step 2) | `HttpMessageNotReadableException` | you add it |
| 400 | Unknown API version, e.g. `?version=V99` (Step 6) | Spring's `org.springframework.web.accept.InvalidApiVersionException` | you add it |
| 400 | Missing or invalid distance parameters (Step 9) | `MissingServletRequestParameterException`, `MethodArgumentTypeMismatchException`, `HandlerMethodValidationException` | you add it |
| 401 | Missing or invalid Basic credentials | Spring Security (the request never reaches your controller) | provided |
| 403 | Authenticated non-owner `PUT` / `DELETE` (Step 3) | `org.springframework.security.access.AccessDeniedException` | you add it |
| 404 | Unknown listing; hidden listing read by a non-owner (Steps 2-3) | your `ResourceNotFoundException` (`com.app.listings.domain.exception`) | you add it |
| 406 | `Accept` type not supported, e.g. `text/csv` (Step 4) | `HttpMediaTypeNotAcceptableException` | you add it |
| 409 | Email already registered (Step 1) | `EmailAlreadyExistsException` | provided |
| 415 | `Content-Type` not supported, e.g. `application/octet-stream` (Step 4) | `HttpMediaTypeNotSupportedException` | you add it |
| 422 | DTO validation errors, with a `fields` map (Steps 1-2) | `MethodArgumentNotValidException` | provided |
| 500 | Unexpected error | catch-all `@ExceptionHandler(Exception.class)` | provided |

> **Read this before Step 2.** The provided `GlobalExceptionHandler` has a
> **catch-all** handler for `Exception`. It catches **every** exception that
> has no more specific handler, including the framework's own 4xx exceptions.
> A status in this table that you have not mapped explicitly comes out as
> **500**, not as the expected 4xx. Each step tells you which handler to add.

---

## Testing strategy (only if you write tests — optional)

Java tests are **not** a deliverable. If you write some, three levels fit this
TP:

| Level | What it covers | Boots Spring? | Fast? |
|---|---|---|---|
| **Unit** | one service with its collaborators faked (Mockito) | no | yes |
| **Slice** | the web layer only (controllers, validation, `@ControllerAdvice`) | a thin slice | yes |
| **Integration** | the real stack: PostgreSQL, Liquibase, JPA, Security, end to end | yes | no |

A good split: business rules (ownership, `active=false`, V2 visibility) as
**unit** tests; status codes and JSON shape as a few **integration** tests.
`spring-boot-starter-test` and `spring-security-test` are on the classpath.
The geocoding port (Step 7) is always **mocked** in tests; never call the real
Nominatim service from a test.

---

## Step 1 - User accounts   CORE

**Outcome** — A visitor registers with `POST /auth/register` (**201**), then
reads their own account with `GET /auth/me` over HTTP Basic (**200**).

**Why this step** — You learn how a controller **delegates**: it translates
HTTP into a service call and the result back into HTTP, and holds no business
logic. Accounts are also the prerequisite of everything else: a listing needs
an owner.

### How it fits

```
POST /auth/register ──> AuthController ──> UserService.register ──> UserRepository ──> app_user
GET  /auth/me       ──> security chain ──> AuthController ──> UserService.findByEmail
                        (checks Basic,     ▲
                         provided)         └── the only part you write in this step
```

### Build it

Everything except the controller is **provided**: entity, repository,
service, DTOs, BCrypt encoder, `UserDetailsService`, and the 409 / 422
handlers. Do not re-implement them.

1. **Inject** `UserService` into `web/controller/AuthController.java`.
   → The controller does not create accounts itself: the provided service
   checks uniqueness (409), hashes the password, and sets the role.
2. **Wire** `register` — take a `@Valid` `UserRequestDto` request body, call
   `UserService.register`, return `UserResponseDto.from(user)` with **201**.
   → Without `@Valid`, invalid input reaches the database instead of
   returning 422. Returning the `User` entity instead of the DTO would put
   the password hash in the JSON.
3. **Wire** `me` — get the authenticated email from Spring Security, load the
   user with `UserService.findByEmail`, return `UserResponseDto.from(user)`.
   → With HTTP Basic there is no session or token: Spring Security has
   already checked the credentials of this request; `me` only reads whose
   they are.
4. **Rebuild** the image and restart.
   → In Docker mode the container runs the image built earlier: without a
   rebuild, your change is not in it.

### Expected behavior

| Request | Status | Body |
|---|---|---|
| `POST /auth/register`, valid payload | **201** | `id`, `email`, `username`, `role: "SELLER"`, `registrationDate`; **no** `password`, **no** `passwordHash` |
| `POST /auth/register`, email already used | **409** | JSON error |
| `POST /auth/register`, invalid email / blank username / password < 6 chars | **422** | JSON error with a `fields` map |
| `GET /auth/me`, no credentials or wrong password | **401** | — |
| `GET /auth/me`, valid Basic credentials | **200** | same shape as the register response |

- The password is stored as a BCrypt hash in `app_user.password_hash`, never in
  plain text.
- Every account receives the role `SELLER`.

### Verify

- [ ] Register `alice@test.org` → **201**, body has `role: "SELLER"` and no password field ([`docs/verification.md` §5](docs/verification.md))
- [ ] Register `alice@test.org` again → **409** + JSON body
- [ ] Register `{"email":"x","username":"","password":"123"}` → **422** + `fields` with 3 entries
- [ ] `GET /auth/me` without `-u` → **401**; with `-u alice@test.org:secret123` → **200**
- [ ] `psql`: `password_hash` of `alice@test.org` starts with `$2a$`, never `secret123`

### If you are stuck

- `ResponseEntity.status(...)` or `@ResponseStatus` sets the 201.
- The current user: an `Authentication` (or `Principal`) controller parameter;
  its `getName()` is the email.
- There is **no** `/login` endpoint: HTTP Basic sends the credentials on every
  request. Reference:
  <https://docs.spring.io/spring-security/reference/7.1/servlet/authentication/passwords/basic.html>

---

## Step 2 - Listings CRUD (V1)   CORE

**Outcome** — An authenticated seller creates, lists (paginated, filtered),
reads, updates, and deletes listings; invalid input is **422**, a malformed
body is **400**, an unknown id is **404**.

**Why this step** — This is the full vertical slice of a REST resource, from
the table to the JSON. You build each layer once, here; every later step only
adds rules to layers that already exist.

> In this step **every** call is authenticated (the skeleton requires it).
> Public reads, the owner-only rule, and hidden listings come in Step 3.

### How it fits

```
POST /listings ──> ListingController ──> ListingService ──> ListingRepository ──> listing table
                   (group C)             (group C)          (group C)              (group A)
                      │  ▲
          ListingRequestDto  ListingResponseDto                 Listing entity
                   (group B)                                    (group A)
errors ──> GlobalExceptionHandler (group D)
```

### Build it

**A. Storage — the table and its Java view**

1. **Create** `src/main/resources/db/changelog/v1.0.2-listing.yaml` — one or
   more `addColumn` changeSets on `listing`: `title` (VARCHAR 120, not null),
   `description` (VARCHAR 2000), `price` (NUMERIC 10,2, not null), `category`
   (VARCHAR 32, not null), `creation_date` (TIMESTAMP, not null), `active`
   (BOOLEAN, not null, default `true`), `user_id` (BIGINT, not null).
   → `ddl-auto: none`: Hibernate never creates columns. Without this file the
   table has only `id` and the first insert fails.
2. **Add** the `include` of `v1.0.2-listing.yaml` to `db.changelog-master.yaml`,
   after `v1.0.1-identity.yaml`.
   → Liquibase runs only the files listed in the master, in that order; a
   file that is not included is silently ignored.
3. **Complete** `domain/entity/Listing.java` — map the V1 columns, `category`
   as an enum stored by name, `owner` as a `@ManyToOne` to `User` on
   `user_id`; fluent `with*` setters, as in `User`.
   → The entity is how Java reads and writes the table. `owner` is what
   Step 3 compares with the caller. An enum stored by position would change
   meaning if someone reorders `Category`.

> **Checkpoint** — rebuild; the app starts, and `psql` `\d listing` shows the
> 7 new columns.

**B. Contract — what the client sends and sees**

4. **Complete** `web/dto/ListingRequestDto.java` — `title`, `description`,
   `price`, `category`, and an optional `active`, with Jakarta validation
   annotations matching the data contract.
   → The annotations produce the 422 before your code runs. A DTO separate
   from the entity prevents a client from setting `id`, `owner`, or
   `creationDate`.
5. **Complete** `web/dto/ListingResponseDto.java` — the V1 fields (`id`,
   `title`, `description`, `price`, `category`, `creationDate`) and a static
   factory from `Listing`.
   → Step 6 makes V1 and V2 responses differ; that is only possible if the
   JSON comes from a DTO, not from the entity.

**C. Behavior — rules and HTTP**

6. **Add** to `domain/repository/ListingRepository.java` — a paginated query
   that applies the optional `category` and `maxPrice` filters.
   → Filtering and paging in SQL: the database returns one page, not the
   whole table for Java to sort through.
7. **Create** `domain/exception/ResourceNotFoundException.java`.
   → The service says "no such listing" in domain terms; group D turns it
   into a 404. Without it, the service can only return `null` or fail.
8. **Implement** `ListingService` and `ListingServiceImpl` — replace the
   `Object dto` parameters with your request DTO, make `update` return the
   updated `Listing`; `create` sets the owner, `creationDate`, and
   `active = true`; an unknown id → `ResourceNotFoundException`.
   → Rules live in the service so that every endpoint, and V2 later, reuses
   them. `Object dto` was only a placeholder.
9. **Implement** `web/controller/ListingController.java` — the five endpoints
   of the table below, a `Pageable` parameter with a default size of 20, the
   two optional filters, `@Valid` request bodies; resolve the caller's id with
   `UserService.findByEmail`.
   → The controller only translates HTTP. The caller's id is needed to set
   the owner on create, and in Step 3 to check it.
10. **Annotate** `ListingsApplication` (or a `@Configuration` class) with
    `@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)`.
    → Serializing Spring's `PageImpl` as-is gives a JSON shape that may change
    between versions (Spring logs a WARN). `VIA_DTO` gives the stable
    `{content, page}` shape used in `Verify`.

> **Checkpoint** — `POST /listings` as alice → **201**; `GET /listings` → a
> page containing it.

**D. Errors**

11. **Add** to `web/advice/GlobalExceptionHandler.java` — a **404** handler for
    `ResourceNotFoundException` and a **400** handler for
    `HttpMessageNotReadableException`.
    → Without them both cases reach the catch-all and return **500** (see
    *Error handling*).

### Expected behavior

| Method | Path | Success | Errors |
|---|---|---|---|
| POST | `/listings` | **201** + body + `Location: /listings/{id}` | 422, 400, 401 |
| GET | `/listings` | **200**, a page | 401 |
| GET | `/listings/{id}` | **200** | 404, 401 |
| PUT | `/listings/{id}` | **200** + updated body | 404, 422, 400, 401 |
| DELETE | `/listings/{id}` | **204**, no body | 404, 401 |

- The V1 body contains exactly `id`, `title`, `description`, `price`,
  `category`, `creationDate` (`description` is omitted when null).
- A page has the shape
  `{ "content": [...], "page": { "size", "number", "totalElements", "totalPages" } }`;
  the default `size` is 20; `?page=0&size=2` is honored.
- `?category=SPORTS` and `?maxPrice=400` (inclusive) filter the list and
  combine; a missing parameter applies no filter.
- `PUT` replaces `title`, `description`, `price`, `category`; `active` is
  optional (absent = unchanged).
- After `DELETE`, `GET /listings/{id}` → **404**.
- **422** + `fields` for: blank title or title > 120 chars, description > 2000
  chars, `price ≤ 0`, missing `price` or `category`.
- **400** + JSON body for a malformed JSON body or an unknown `category` value
  (e.g. `"NONSENSE"`); this is a parse error, not a validation error.

### Verify

- [ ] `POST /listings` as alice → **201**, body has `id` and `creationDate`, response has a `Location` header ([`docs/verification.md` §6](docs/verification.md))
- [ ] `GET /listings?page=0&size=2` → `page.size` is 2 and `page.totalElements` counts all listings
- [ ] `?category=SPORTS&maxPrice=400` returns the bike; `?maxPrice=100` does not
- [ ] `GET /listings/999999` → **404** + JSON body
- [ ] `PUT` own listing → **200** with the new title; `DELETE` → **204**, then `GET` → **404**
- [ ] Title of 130 chars and `price: -1` → **422**, `fields` names `title` and `price`
- [ ] `"category":"NONSENSE"` → **400** + JSON body (not 500)
- [ ] `psql`: `SELECT id FROM databasechangelog` lists `v1.0.2-…`; `\d listing` shows the 7 V1 columns

### If you are stuck

- Liquibase `addColumn`: <https://docs.liquibase.com/change-types/add-column.html>.
  Never edit `v1.0-init.yaml` or `v1.0.1-identity.yaml`: they are already
  applied, and Liquibase refuses to start when an applied changeSet changes.
- `@Enumerated(EnumType.STRING)` stores `SPORTS`, not `5`.
- Optional filters: one `@Query` with `(:category IS NULL OR …)` clauses, or a
  Spring Data `Specification`. Derived queries need one method per filter
  combination. Reference:
  <https://docs.spring.io/spring-data/jpa/reference/4.1/repositories/query-methods-details.html>
- `@PageableDefault(size = 20)` on the `Pageable` parameter sets the default.
- `ServletUriComponentsBuilder.fromCurrentRequest()` builds the `Location`.

---

## Step 3 - Access rules: public reads, owner-only writes, hidden listings   CORE

**Outcome** — Anyone reads active listings without an account; only the
**owner** updates or deletes (others get **403**); a hidden listing is visible
to its owner only.

**Why this step** — Authentication answers *who are you*; authorization
answers *what may you do*. They are checked in two different places, because
the second one needs data (the listing's owner) that only the database has.

### How it fits

```
request ──> security chain ──────────> controller ──> service ──────────> repository
            "may anyone call this?"                   "may THIS caller
            no credentials -> 401                      touch THIS listing?"
            (group A)                                  not owner -> 403
                                                       hidden    -> 404
                                                       (group B)
```

### Build it

**A. Who may call an endpoint (security chain)**

1. **Modify** `config/SecurityConfig.java#securityFilterChain` — permit
   anonymous `GET` on `/listings` and `/listings/**`; require the role
   `SELLER` for `POST`, `PUT`, `DELETE` on `/listings/**`; keep the existing
   public endpoints.
   → Today `anyRequest().authenticated()` rejects every visitor with 401.
2. **Modify** the same chain — make the session policy **stateless**.
   → Basic resends the credentials on every request; a session only adds
   server state and a `JSESSIONID` cookie nobody uses.

> **Checkpoint** — `curl -i $API/listings` without `-u` → **200**, no
> `Set-Cookie`.

**B. What this caller may do on this listing (service)**

3. **Modify** `ListingServiceImpl` — in `update` and `delete`, throw
   `AccessDeniedException` when the caller is not the owner.
   → The security chain cannot know the owner without loading the listing;
   the service has it.
4. **Add** a **403** handler for `AccessDeniedException` to
   `GlobalExceptionHandler`.
   → Without it, the catch-all handler turns the 403 into a **500**.
5. **Modify** the list and read paths (service + repository) — exclude
   `active = false` listings unless the caller is their owner; a hidden
   listing read by a non-owner → `ResourceNotFoundException`.
   → **404**, not 403: a non-owner must not even learn that the listing
   exists.
6. **Modify** `ListingController` — accept an anonymous caller on `GET`
   (caller id = `null`).
   → The service needs to know there is no caller to apply item 5.

### Expected behavior

| Client | `GET /listings` | `GET /listings/{id}` active | `GET /listings/{id}` hidden | `POST` | `PUT` / `DELETE` own | `PUT` / `DELETE` other's |
|---|---|---|---|---|---|---|
| Anonymous | **200** | **200** | **404** | **401** | — | **401** |
| bob (SELLER, not owner) | **200** | **200** | **404** | **201** | — | **403** |
| alice (SELLER, owner) | **200** | **200** | **200** | **201** | **200** / **204** | — |

- The list returned to anonymous and to bob contains **no** hidden listing; the
  list returned to alice contains her hidden listings.
- The owner check happens **after** authentication: an anonymous write is
  **401** (security chain), an authenticated non-owner write is **403**
  (service), never the reverse.
- `/auth/register`, `/actuator/health`, `/actuator/info`, `/actuator/metrics`,
  `/v3/api-docs/**`, `/swagger-ui/**` stay public.
- No response sets a `JSESSIONID` cookie.

### Verify

- [ ] Run the matrix loop of [`docs/verification.md` §7](docs/verification.md): every cell matches the table above
- [ ] Anonymous `POST /listings` → **401** with a `WWW-Authenticate: Basic` header
- [ ] bob `PUT` on alice's listing → **403** (not 401, not 404, not 500)
- [ ] alice sets `active: false` → anonymous and bob: absent from the list and **404** on detail; alice: present, **200**
- [ ] `curl -si -u alice@test.org:secret123 $API/auth/me | grep -i set-cookie` → no output
- [ ] `/actuator/health`, `/actuator/info`, `/actuator/metrics` without credentials → **200**

### If you are stuck

- `requestMatchers(HttpMethod.GET, …)` restricts a rule to one method; rules
  are evaluated top to bottom, first match wins. `hasRole("SELLER")` adds the
  `ROLE_` prefix itself. Reference:
  <https://docs.spring.io/spring-security/reference/7.1/servlet/authorization/authorize-http-requests.html>
- `sessionManagement(…)` and `SessionCreationPolicy.STATELESS`.
- Hidden listings in the list: a query with `active = true OR owner.id = :callerId`.
- With an anonymous caller, the `Authentication` controller parameter is `null`.

---

## Step 4 - Content negotiation (JSON + XML)   BONUS

**Outcome** — The same endpoints read and write `application/json` or
`application/xml` according to `Content-Type` and `Accept`; unsupported types
get **415** / **406**; error bodies are always JSON.

**Why this step** — One resource, several representations: the client chooses
the format, not the server. Clients in other languages or legacy systems can
consume the API without an adapter, and the controller code does not change.

### How it fits

```
request body ──> converter chosen by Content-Type ──> DTO ──> controller
response     <── converter chosen by Accept       <── DTO <──
                 (JSON today; XML after item 1)
no converter matches ──> 415 (Content-Type) or 406 (Accept) ──> GlobalExceptionHandler
```

### Build it

**A. A second converter**

1. **Add** the Jackson 3 XML dependency to `pom.xml` (block in
   [`README.md`, "Dependencies added during the TP"](README.md); the why is in
   Appendix B), then rebuild.
   → Spring Boot registers the XML converter only when the Jackson XML module
   is on the classpath.

> **Checkpoint** — `curl -H 'Accept: application/xml' $API/listings/{id}` →
> an XML body.

**B. Errors stay JSON**

2. **Add** to `GlobalExceptionHandler` — a **415** handler for
   `HttpMediaTypeNotSupportedException` and a **406** handler for
   `HttpMediaTypeNotAcceptableException`.
   → Spring raises them when no converter matches; without a handler they
   reach the catch-all and return 500.
3. **Modify** every handler of `GlobalExceptionHandler` — set the response
   content type to `application/json` explicitly.
   → Error bodies go through the same converters: an XML client would get
   XML errors, and a `text/csv` client no body at all (the 406 itself cannot
   be written).

### Expected behavior

| Request | Status | Response body |
|---|---|---|
| `POST /listings`, `Content-Type: application/json` | **201** | JSON |
| `POST /listings`, `Content-Type: application/xml` | **201** | JSON by default; XML with `Accept: application/xml` |
| `GET /listings/{id}`, `Accept: application/xml` | **200** | XML |
| `GET /listings/{id}`, `Accept: application/json` | **200** | JSON |
| `POST /listings`, `Content-Type: application/octet-stream` | **415** | JSON error |
| `GET /listings/{id}`, `Accept: text/csv` | **406** | JSON error |
| `GET /listings/999999`, `Accept: application/xml` | **404** | JSON error |

- Both formats carry the same field names and values; XML element names are
  the Java property names (`<title>`, `<price>`).

### Verify

- [ ] The 7 calls of [`docs/verification.md` §8](docs/verification.md) match the table, including the `Content-Type` of each response
- [ ] `grep -n "dataformat-xml" pom.xml` → one `tools.jackson.dataformat` dependency, no `<version>`
- [ ] `grep -n "APPLICATION_JSON" src/main/java/com/app/listings/web/advice/GlobalExceptionHandler.java` → one per handler

### If you are stuck

- Spring Boot 4 uses **Jackson 3**: the XML module is
  `tools.jackson.dataformat:jackson-dataformat-xml`. The Jackson 2 artifact
  (`com.fasterxml.jackson.dataformat`) is **not** picked up by Boot 4's
  auto-configuration.
- `ResponseEntity.contentType(...)` fixes the content type of an error
  response.
- Reference:
  <https://docs.spring.io/spring-boot/4.1/reference/web/servlet.html>

---

## Step 5 - Schema evolution (V2 columns)   CORE

**Outcome** — The `listing` table has 7 new nullable columns, added by one
**additive** Liquibase changelog; existing data and the V1 API are unchanged.

**Why this step** — In production the database is shared by running clients
and already holds data: you cannot drop and recreate it. Schema changes are
shipped as small, additive migrations, separately from the API changes that
use them (Step 6).

### Build it

1. **Create** `src/main/resources/db/changelog/v1.1-v2-fields.yaml` — one
   `addColumn` changeSet on `listing`: `negotiable` (BOOLEAN),
   `street_address` (VARCHAR 255), `city` (VARCHAR 100), `postal_code`
   (VARCHAR 20), `country` (VARCHAR 100), `latitude` (DOUBLE), `longitude`
   (DOUBLE). All nullable.
   → These are the columns V2 needs. They are nullable because existing rows
   have no value for them: a `NOT NULL` column would make the migration fail
   on a non-empty table.
2. **Add** its `include` to `db.changelog-master.yaml`, **after**
   `v1.0.2-listing.yaml` (replace the commented line).
   → Order matters: the changeSets run in the order of the master.
3. **Modify** `domain/entity/Listing.java` — map the 7 attributes
   (`negotiable`, `streetAddress`, `city`, `postalCode`, `country`, `latitude`,
   `longitude`) with fluent setters.
   → Step 6 reads and writes them. The V1 DTOs do not expose them, so the V1
   API does not change.
4. **Rebuild** and restart **without** `down -v`.
   → This is the actual test of an additive migration: it runs on the data
   you created in Steps 1-3, and that data survives.

### Expected behavior

- Liquibase applies only the new changeSet at startup; the earlier ones are
  skipped.
- Rows created before the migration have `NULL` in the 7 new columns.
- No existing column is modified or dropped.
- The V1 API answers exactly as before: no new field in any response.

### Verify

- [ ] `psql` `\d listing` lists the 7 new columns, all nullable ([`docs/verification.md` §9](docs/verification.md))
- [ ] `psql`: listings created in Step 2 show `NULL` for `negotiable` and `latitude`
- [ ] `psql`: `SELECT id FROM databasechangelog ORDER BY orderexecuted` ends with your `v1.1-…` changeSet; a second restart adds no row
- [ ] `grep -n "v1.1-v2-fields" src/main/resources/db/changelog/db.changelog-master.yaml` → one active `include` line
- [ ] The calls of [`docs/verification.md` §6 and §7](docs/verification.md) still give the same results
- [ ] `mvn clean verify` → `BUILD SUCCESS`

### If you are stuck

- An attribute mapped in the entity without its Liquibase column fails at the
  first query (`ddl-auto: none`).
- Column names are snake_case (`street_address`); `@Column(name = …)` maps them
  to the camelCase attributes.
- A mistake in an applied changeSet is fixed by a **new** changeSet, never by
  editing the old one.

---

## Step 6 - Versioning (V1 + V2)   CORE

**Outcome** — `?version=V1|V2` selects the contract (default V1); V1 responses
announce their deprecation; V2 exposes `negotiable` to everyone and the private
fields to the owner only; `?version=V99` → **400**; two OpenAPI documents are
published.

**Why this step** — An API cannot change its contract under existing clients.
Versioning lets V1 clients keep working, unchanged, while V2 adds fields; the
deprecation headers tell V1 clients, in a machine-readable way, when V1 ends.

### How it fits

```
GET /listings?version=2
  │
  ▼
Spring versioning (ApiVersioningConfig)   reads ?version, default 1, unknown -> InvalidApiVersionException
  │                                       adds Deprecation / Sunset to V1 responses
  ▼
ListingController   method declared version = "1"   or   version = "2"
  │                        │                                │
  │                 V1 DTOs (Step 2)              V2 DTOs (private fields null for non-owners)
  ▼
ListingService (same rules for both versions)
```

### Build it

**A. Routing by version**

1. **Implement** `config/ApiVersioningConfig.java` — override
   `configureApiVersioning(ApiVersionConfigurer)`: version read from the query
   parameter `version`, supported versions `1` and `2`, default version `1`.
   → This tells Spring where to read the version and what to do without one.
   Default `1` means existing clients, which send no parameter, keep the V1
   contract.
2. **Configure** in the same method a deprecation handler for version `1`:
   deprecation date `2026-01-01T00:00:00Z`, sunset date `2027-01-01T00:00:00Z`.
   → It produces the `Deprecation` and `Sunset` headers; without it, V1
   clients get no warning before V1 is removed.

> **Checkpoint** — `curl -sI $API/listings` shows `Deprecation` and `Sunset`.

**B. The V2 contract**

3. **Create** the V2 DTOs (or extend the V1 ones) — the V2 request adds
   `negotiable`, `streetAddress`, `city`, `postalCode`, `country`, `latitude`,
   `longitude`; the V2 response adds the same fields and `active`, with a
   factory that leaves the private fields `null` for a non-owner.
   → The difference between versions is a difference of contract, so it lives
   in the DTOs. A `null` field is dropped from the JSON (`non_null` in
   `application.yml`), which is how non-owners do not see private fields.
4. **Modify** `ListingController` — declare each endpoint for version `1` and
   version `2` (`version` attribute of the mapping annotations), in one
   controller or two.
   → This is how Spring routes `?version=2` to the V2 method.
5. **Modify** `ListingServiceImpl` — a V1 write never modifies the V2 columns.
   → A V1 client does not know the V2 fields exist. If its `PUT` erased them,
   old clients would destroy the data of new ones.

**C. Errors and documentation**

6. **Add** to `GlobalExceptionHandler` — a **400** handler for Spring's
   `InvalidApiVersionException` with the exact body below.
   → Without it the catch-all returns 500. The body tells the client which
   versions exist.
7. **Create** `config/OpenApiConfig.java` — two springdoc `GroupedOpenApi`
   beans, groups `listings-v1` and `listings-v2`, titles `Listings API V1` and
   `Listings API V2`.
   → One document per contract: V1 and V2 client developers each read only
   their own version.

### Expected behavior

| Request | Version served |
|---|---|
| no `version` parameter | V1 |
| `?version=V1`, `?version=v1`, `?version=1` | V1 |
| `?version=V2`, `?version=v2`, `?version=2` | V2 |
| `?version=V99` | **400** `{"error":"API version not available","availableVersions":["V1","V2"]}` |

| Field | V1, anyone | V2, anonymous or non-owner | V2, owner |
|---|:--:|:--:|:--:|
| `id`, `title`, `description`, `price`, `category`, `creationDate` | ✓ | ✓ | ✓ |
| `negotiable` | — | ✓ | ✓ |
| `active`, `streetAddress`, `city`, `postalCode`, `country`, `latitude`, `longitude` | — | — | ✓ |

- Every V1 response carries `Deprecation: @1767225600` (RFC 9745, the
  deprecation date as epoch seconds) and `Sunset: Fri, 1 Jan 2027 00:00:00 GMT`
  (RFC 8594). V2 responses carry neither.
- Non-versioned endpoints (`/auth/*`) resolve to the default version and may
  carry the same headers; this is expected.
- V2 `POST` / `PUT` accept the V2 fields. A V1 `PUT` leaves them unchanged.
- Pagination, filters, ownership (403), and hidden listings (404) behave the
  same in V1 and V2.
- `/v3/api-docs/listings-v1` and `/v3/api-docs/listings-v2` return two
  documents with distinct titles; each lists only its own version's operations.

### Verify

- [ ] No parameter and `?version=V1` return the same body; neither contains `negotiable` ([`docs/verification.md` §10](docs/verification.md))
- [ ] alice `PUT ?version=V2` with `negotiable` and an address → **200**; then V2 as alice shows `active` and the address, V2 as bob shows `negotiable` only
- [ ] alice V1 `PUT` on that listing → V2 `GET` still shows the address (V1 did not erase it)
- [ ] `curl -sI "$API/listings"` shows `Deprecation:` and `Sunset:`; `curl -sI "$API/listings?version=V2"` shows neither
- [ ] `?version=V99` → **400** with the exact body above
- [ ] The two `api-docs` groups return the titles `Listings API V1` and `Listings API V2`

### If you are stuck

- Spring Framework 7 has **native** API versioning: the default version and
  the 400 on an unsupported version come with the configuration; you do not
  write a filter. Reference:
  <https://docs.spring.io/spring-framework/reference/7.0/web/webmvc-versioning.html>
- The default version parser accepts an optional `v`/`V` prefix: `V2` is parsed
  as `2.0.0`. Declare mappings with `version = "1"` and `version = "2"`.
- Deprecation headers: `StandardApiVersionDeprecationHandler`
  (`org.springframework.web.accept`), `configureVersion("1")`.
- Map Spring's `InvalidApiVersionException`, not a class of your own with the
  same name.
- springdoc: V1 and V2 share the **same paths**, so `pathsToMatch` cannot
  separate them. Use `addOpenApiMethodFilter` (inspect the mapping's
  `version`) and `addOpenApiCustomizer` (set the title). Reference:
  <https://springdoc.org/#groups>

---

## Step 7 - Geolocation (GPS enrichment)   BONUS

**Outcome** — A V2 `PUT` with a `streetAddress` resolves `latitude` /
`longitude` through Nominatim; when Nominatim fails or is slow, the update
still succeeds within about 3 s, with null coordinates and a WARN log line.

**Why this step** — Calling an external service makes your API depend on
something you do not control. You learn to isolate that dependency behind an
interface, to bound its cost (timeout), and to degrade gracefully when it
fails.

### How it fits

```
ListingServiceImpl ──> GeocodingPort (interface, yours) ──> NominatimGeocodingAdapter ──HTTP──> Nominatim
                       ▲                                    (RestClient, timeouts)
                       └── in tests: a mock of the port
```

### Build it

1. **Create** a port `application/port/GeocodingPort.java` — one method that
   takes an address and returns optional coordinates.
   → The service depends on this interface, not on Nominatim: tests mock it,
   and another geocoder would be one new adapter, with no service change.
2. **Create** an adapter, e.g. `infrastructure/geo/NominatimGeocodingAdapter.java` —
   a `RestClient` on `listings.geo.endpoint`, connect and read timeouts from
   `listings.geo.timeout-ms`, a `User-Agent` header naming the app, and the
   call `GET /search?format=json&limit=1&q={address}` (Appendix A).
   → Without timeouts, a slow Nominatim blocks the request thread far beyond
   3 s. The `User-Agent` is required by Nominatim's usage policy.
3. **Modify** `ListingServiceImpl` — on a V2 `PUT` with a non-blank
   `streetAddress`, call the port and store the result; on failure or no
   result, log a WARN with the address and the reason, store `null`
   coordinates, continue.
   → Geocoding enriches the listing; it is not a condition for saving it. Its
   failure must not fail the update. The WARN is the only trace for the
   operator.
4. **Rebuild** and restart.

### Expected behavior

| Situation | Status | Coordinates | Log |
|---|---|---|---|
| Address found by Nominatim | **200** | set | — |
| Nominatim error, timeout, or empty result | **200** | `null` | one WARN with address and reason |
| No `streetAddress` in the request | **200** | unchanged; no outbound call | — |

- A slow or unreachable Nominatim never delays the response by more than about
  3 s (`timeout-ms: 3000`).

### Verify

- [ ] `grep -n "Timeout" src/main/java/com/app/listings/infrastructure/geo/*.java` → connect and read timeouts set from the property
- [ ] V2 `PUT` with a Paris address → **200**, `psql` shows non-null coordinates ([`docs/verification.md` §11](docs/verification.md))
- [ ] With `LISTINGS_GEO_ENDPOINT` pointing to an unreachable address: same `PUT` → **200** in less than 4 s (`curl -w %{time_total}`), `NULL` coordinates, one WARN in `docker compose logs listings-api`

### If you are stuck

- `RestClient.builder()` with a `SimpleClientHttpRequestFactory`
  (`setConnectTimeout`, `setReadTimeout`). Reference:
  <https://docs.spring.io/spring-framework/reference/7.0/integration/rest-clients.html>
- The response is a JSON **array**; `lat` / `lon` are **strings** (Appendix A).
- Catch `RestClientException` around the call.
- Optional extension: retry failed enrichments later (retry with backoff, a
  re-sync endpoint, or a pending queue). Document the choice in the README.

---

## Step 8 - Metrics   CORE

**Outcome** — The Micrometer counter `listings.created` is readable at
`/actuator/metrics/listings.created` from startup and increases by 1 on every
successful listing creation.

**Why this step** — Logs tell you what happened to one request; metrics tell
you how the service behaves over time. A business counter is the simplest
proof that the service does its job, and the Monitoring TP builds dashboards
and alerts on it.

### Build it

1. **Modify** `ListingServiceImpl` — inject `MeterRegistry` and register the
   counter `listings.created` once, at construction.
   → A counter created on first use does not exist before the first creation:
   the endpoint returns 404 and a dashboard shows no series.
2. **Modify** `ListingServiceImpl#create` — increment the counter after the
   listing is saved.
   → Counting after the save counts listings that exist, not attempts that
   failed.
3. **Rebuild** and restart. The tag `application=listings` is already
   configured (`management.metrics.tags.application`).

### Expected behavior

- Right after startup, `GET /actuator/metrics/listings.created` → **200**,
  value `0.0`.
- Each **201** on `POST /listings` (V1 or V2) adds 1. A 401, 422, or 400 adds
  nothing.
- The metric lists the tag `application` with the value `listings` in
  `availableTags`.

### Verify

- [ ] After a restart, `/actuator/metrics/listings.created` → **200**, `measurements[0].value` is `0.0` ([`docs/verification.md` §12](docs/verification.md))
- [ ] One valid `POST` → value +1; one invalid `POST` (422) → unchanged
- [ ] `availableTags` contains `{"tag":"application","values":["listings"]}`

### If you are stuck

- `Counter.builder(...).register(registry)` in the constructor.
- Reference:
  <https://docs.spring.io/spring-boot/4.1/reference/actuator/metrics.html>

---

## Step 9 - Distance search   BONUS

**Outcome** — In V2, `GET /listings?lat=…&lng=…&radius=…` returns the listings
within `radius` km of the point; invalid parameters and V1 calls get **400**.

**Why this step** — A new feature that exists only in one version of the
contract, with input that must be validated before any computation. You also
choose, and justify, where a computation runs (Java or SQL).

### Build it

**A. Input**

1. **Modify** the V2 list endpoint of `ListingController` — accept the
   optional parameters `lat`, `lng`, `radius`: all three → distance search;
   none → the normal list; one or two → **400**.
   → A partial set is ambiguous (a point without a radius); rejecting it is
   clearer than guessing.
2. **Validate** — `radius` is an integer ≥ 1, `lat` in [-90, 90], `lng` in
   [-180, 180].
   → Impossible coordinates are rejected before any computation.
3. **Modify** the V1 list endpoint — any of the three parameters → **400**.
   → The feature belongs to the V2 contract. If V1 silently ignored the
   parameters, it would return the whole list and mislead the client.
4. **Add** to `GlobalExceptionHandler` — **400** handlers for the parameter
   exceptions of the *Error handling* table.
   → Without them the catch-all returns 500.

**B. Search**

5. **Implement** the search in the service (or the repository) — exclude
   listings without coordinates; keep the visibility rules of Steps 3 and 6.
   → A new query path must not bypass existing rules: no hidden listing, no
   private field for non-owners.
6. **Document** your approach in the README (in-memory Haversine, SQL formula,
   bounding box, …) and its limits.
   → The trade-off (precision, performance with many listings) is part of the
   oral.

### Expected behavior

| Request | Status |
|---|---|
| `?version=V2&lat=48.86&lng=2.35&radius=5` | **200**, only listings with coordinates within 5 km |
| `?version=V2&lat=48.86` (incomplete) | **400** |
| `?version=V2&lat=48.86&lng=2.35&radius=0` or `radius=0.5` | **400** |
| `?version=V2&lat=91&lng=2.35&radius=5` | **400** |
| `?version=V1&lat=48.86&lng=2.35&radius=5` | **400** (not available in V1) |

- Optional: results sorted by distance, closest first.

### Verify

- [ ] The 5 calls of [`docs/verification.md` §13](docs/verification.md) give the statuses above
- [ ] Every item of the 200 response has `latitude` and `longitude` (V2 owner) or comes from a listing that has them (`psql`)
- [ ] The README contains a paragraph justifying the approach

### If you are stuck

- Haversine distance: `2 R asin(sqrt(sin²(Δφ/2) + cos φ1 cos φ2 sin²(Δλ/2)))`,
  `R = 6371` km. PostGIS is not required.
- `radius=0.5` on an `Integer` parameter raises
  `MethodArgumentTypeMismatchException`; `@Min` / `@DecimalMax` on parameters
  raise `HandlerMethodValidationException`.

---

## Step 10 - Final integration   DELIVERY

**Outcome** — The repository builds green, runs from a clean clone with one
command, and documents what you built and why.

**Why this step** — Work that only runs on your machine, with your database,
is not delivered. This step proves the project works for someone else, from
scratch.

### Build it

1. **Remove** every `Not implemented yet` stub and `TODO` comment from
   `src/main/java`.
   → A remaining stub is a 500 waiting for a client.
2. **Update** `README.md` — how to run, every endpoint with one example
   (JSON, and XML if Step 4 is done), versioning usage, GPS fallback (if
   Step 7 is done), metrics, and your technical choices (one line each).
   → It is how someone else runs and evaluates your work without reading the
   code.
3. **Rebuild** from scratch: `docker compose down -v`, then
   `docker compose up -d --build`, then replay the core `Verify` lists.
   → Proves that the migrations run on an empty database, not only on the one
   you built step by step.

### Final checks

- [ ] `mvn clean verify` → `BUILD SUCCESS` ([`docs/verification.md` §14](docs/verification.md))
- [ ] Every core step's `Verify` (1, 2, 3, 5, 6, 8) is green on a fresh database
- [ ] Every bonus step you attempted has its `Verify` green
- [ ] `grep -rnE "TODO|Not implemented|UnsupportedOperationException" src/main/java` → no output
- [ ] `psql` `\dt` → `app_user`, `listing`, `databasechangelog`, `databasechangeloglock`
- [ ] `docker compose logs listings-api | grep -E " ERROR "` → no output since the last boot
- [ ] The README covers the points of Build it, item 2

### If you are stuck

- `mvn clean verify` needs a PostgreSQL on `localhost:5435`, and a JDK 27 on
  the host. Without one, use the container command in
  [`README.md`, "Build & test"](README.md).
- A changeSet edited after it was applied blocks the start on a non-empty
  database (`checksum` error) but passes on a fresh one: test both.

---

## Deliverables

| Item | Detail |
|---|---|
| Git repository (pair) | clean history, meaningful commits |
| `mvn clean verify` | green |
| `README.md` | complete (Step 10, Build it, item 2) |
| Oral | 10 min (architecture, key decisions, demo) + 5 min Q&A |

---

## Appendix A — Nominatim (geocoding)

Free public geocoder (OpenStreetMap), used in Step 7.

- Base URL: `https://nominatim.openstreetmap.org` (property
  `listings.geo.endpoint`, overridable with `LISTINGS_GEO_ENDPOINT`).
- Endpoint: `GET /search?format=json&limit=1&q={address}` (`q` URL-encoded;
  street + city + country gives the best results).
- Response: a JSON array; `lat` / `lon` are **strings**.

  ```json
  [
    {
      "place_id": 78,
      "lat": "48.85340613427995",
      "lon": "2.3488002225875854",
      "display_name": "Paris, France"
    }
  ]
  ```

- An empty array or a non-2xx status means "no result" (Step 7 fallback).
- Usage policy: at most 1 request per second, and a `User-Agent` identifying
  the application. <https://operations.osmfoundation.org/policies/nominatim/>

## Appendix B — XML support (Step 4)

| Coordinates | `tools.jackson.dataformat:jackson-dataformat-xml` (version managed by the Spring Boot BOM) |
|---|---|
| Why | `spring-boot-starter-web` ships the Jackson **JSON** converter only. With this module on the classpath, Spring Boot registers the Jackson **XML** converter as well; DTOs stay unchanged. |
| Why this groupId | Spring Boot 4 uses **Jackson 3** (`tools.jackson.*`). The Jackson 2 module (`com.fasterxml.jackson.dataformat`) is not used by Boot 4's auto-configuration. |
| Additive | it adds one converter; JSON behavior is unchanged. |

The `pom.xml` block to paste is in [`README.md`](README.md). It is the only
dependency the exercise asks you to add.
