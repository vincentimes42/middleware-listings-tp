# Classifieds API - Progressive Implementation

## Overview

You are building a **classifieds listings** micro-service, in pairs, for a session of
about 8 hours. The skeleton project you receive already compiles, starts, and passes
a context test. Your job is to implement the API **feature by feature**, in the order
below.

For each step you are invited to **write tests first**, see them fail, then implement
until they pass. The tests you write *are* part of your deliverable.

The specification below describes **behaviors, contracts, and response codes**.
It does NOT prescribe the internal implementation (framework classes, design
patterns, or library choices). You are free to structure the code however you
see fit, provided the **observable behavior** matches the specification.

> **Testing note**: `spring-boot-starter-test` and `spring-security-test` are
> already on the classpath. Use the web test client of your Boot version to test
> endpoints end-to-end, and simulate HTTP Basic users with `httpBasic(...)`
> from `spring-security-test`. External collaborators (e.g. the geocoding
> client) must be **mocked** in tests - never call the real Nominatim service.

---

## Before you start — the two tools you will use

New to Maven or Spring Boot? This 2-minute mental model will make the steps
below click. (Installation and run details are in `README.md`.)

### Maven — a build tool with a standard layout

**Maven is a tool that knows how to build a Java project.** You feed it a
`pom.xml` (Project Object Model) that declares what the project depends on, how
to compile / test / package it, and which plugins to run. Because the layout,
goals, and dependencies are all **conventional**, every Maven project looks the
same (`pom.xml`, `src/main/java`, `src/test/java`) and a small set of commands
do the same everywhere:

| Maven command | What it does |
|---|---|
| `mvn spring-boot:run` | start the application (port 8080) |
| `mvn clean verify` | clean, compile, test, and package |

Key idea: to use a library, you add a `<dependency>` in `pom.xml` — **not** a
`jar`. You never touch classpath files by hand. (Maven docs: <https://maven.apache.org/guides/>.)

### Spring Boot — an opinionated framework for HTTP services

**Spring Boot turns Spring into "just enough" to build a production-grade HTTP
service with very little boilerplate.** You write Java classes annotated with
`@RestController`, `@Service`, `@Entity`, `@Repository`, `@Configuration`, and
Spring Boot takes care of the rest:

- **Dependency injection**: classes "ask" for their collaborators in the
  constructor; Spring instantiates and wires them. No `new` for your beans.
- **Auto-configuration**: a `starter` on the classpath (web, data-jpa, security,
  liquibase, actuator…) + a few `application.yml` keys produce a working
  DataSource, transaction manager, embedded HTTP server, security chain, and
  schema migrations.
- **The result here**: one entry point (`ListingsApplication.java`), a handful
  of annotated classes, and one config file. Everything else is wiring done by
  Spring Boot behind the scenes.

> **Tip:** when a behaviour feels "magical", search the annotation or the `yml`
> key in the Spring Boot reference (4.1.x, matching this project) — it links to
> the auto-configuration class that drives it:
> <https://docs.spring.io/spring-boot/4.1.1/reference/html/index.html>

---

## Domain & data model

Before looking at the steps, here is **what you are modelling**. Each step below
is one facet of this domain.

### Actors

| Actor | What they can do |
|---|---|
| **Visitor** (anonymous) | Browse and read any *active* listing. No account. |
| **Seller** (the only role) | A registered user who creates and manages **their own** listings. Every registered account is a `SELLER`. There is no platform-admin role in this exercise. |

### Entities

**`User`** — a seller account.

| attribute | note |
|---|---|
| `id` | primary key (surrogate) |
| `email` | **unique** — used as the Basic-auth login |
| `username` | displayed human name |
| `passwordHash` | BCrypt hash — the raw password is never stored |
| `role` | always `SELLER` |
| `registrationDate` | account creation time (ISO-8601) |

**`Listing`** — an item offered for sale.

| attribute | V1 | V2 (added in Steps 5-6) | visible to |
|---|:--:|:--:|---|
| `id`, `title` (1-120), `description` (0-2000), `price` (>0), `category` (9 values), `creationDate` | ✓ | ✓ | everyone (public) |
| `active` | ✓ | ✓ | owner only (others see only `true` listings) |
| `negotiable` | — | ✓ | everyone (public) |
| `streetAddress`, `city`, `postalCode`, `country`, `latitude`, `longitude` | — | ✓ | owner only (private) |

### Relationship

`User` **1 —— 0..N** `Listing` — a listing always belongs to exactly one seller
(its owner); a seller can own many listings. Carried by `listing.user_id`
(`@ManyToOne` on the `Listing` entity) — referential integrity is JPA's job
(the skeleton does not declare SQL foreign keys; see README).

### Lifecycle

A listing is born `active = true`. The seller can **hide** it (`active = false`)
and reactivate it later. A hidden listing is invisible in lists for everyone
but the owner and reads as **404** for non-owners; the owner alone still sees it.

> Hold these four ideas in mind — *actors, the two entities, the 1-to-N
> ownership, and the active lifecycle* — because almost every requirement
> below is a rule about one of them.

---

## Scope

| Track | Steps |
|---|---|
| **Core** (required - must fit in the 8h) | 1 · 2 · 3 · 5 · 6 · 7 · 8 |
| **Bonus** (start only when the core checklists are green) | 4 (JSON + XML) · 9 (distance search) · 10 (hexagonal architecture) |
| **Delivery** | 11 (final integration) |

Work the core steps **in order**; each step's acceptance checklist is your
"definition of done" for that step.

---

## Conventions

- All timestamps are ISO-8601.
- All monetary amounts are decimal (2 digits).
- All error responses are JSON: `{ "timestamp": "...", "status": 4xx, "error": "...", "message": "..." }` (add a `fields` map for validation errors).
- Content types: `application/json` by default; `application/xml` is added if you complete Step 4 (bonus).
- The API supports **two versions** (once enabled): `V1` and `V2` (Step 6).
- Authentication is **HTTP Basic** (no form, no session).

---

## Error handling & status codes (cross-cutting)

Every step below relies on this table. Whenever a behavior says "→ 409", you must
make the actual HTTP response carry status 409 with a JSON body and a useful
`message`.

| Status | Trigger | Suggested mechanism |
|---|---|---|
| 400 | Unknown API version (e.g. `V99`); header/param version conflict (Step 6); missing or invalid distance parameters (Step 9) | custom `InvalidApiVersionException` |
| 401 | Unauthenticated write (Basic credentials missing or invalid) | Spring Security |
| 403 | Non-seller PUT / DELETE (Step 3.3) | `AccessDeniedException` |
| 404 | Unknown listing; `active=false` listing seen by a non-seller (Step 2) | custom `ResourceNotFoundException` (domain) |
| 409 | Duplicate email on registration (Step 1.2) | custom `EmailAlreadyExistsException` (domain) |
| 415 | Request with unsupported `Content-Type` (Step 4) | `HttpMediaTypeNotSupportedException` |
| 406 | Response type unsupported by `Accept` (Step 4) | `HttpMediaTypeNotAcceptableException` |
| 422 | DTO validation errors, with a `fields` map (Steps 1, 2) | `MethodArgumentNotValidException` (already handled in the skeleton's `GlobalExceptionHandler`) |
| 500 | Unexpected error | generic handler (already in the skeleton) |

Hints:

- Extend `web/advice/GlobalExceptionHandler.java` with one `@ExceptionHandler` per
  missing case (400 / 403 / 404 / 409 / 415 / 406). Keep the response format above.
- Put your custom domain exceptions under `com.app.listings.domain` (or
  `com.app.listings.domain.exception`) so that services can throw them and
  controllers stay thin.
- Error (4xx) bodies are **JSON**, even if the client requested XML (Step 4.6).

---

## Testing strategy (how to test what)

`spring-boot-starter-test` and `spring-security-test` are already on the
classpath (JUnit 5, AssertJ, Mockito come with it). Here is **which test to
write where**, and a small recipe for each. Pick the level that matches the
thing under test — the lower and more focused the test, the faster and cheaper
it is, so most of your logic should live in unit tests.

### The three levels

| Level | What it covers | Boots Spring? | Fast? | Typical annotation / client |
|---|---|---|---|---|
| **Unit** | one service / use-case with its collaborators faked | no | yes | `@ExtendWith(MockitoExtension.class)` |
| **Slice** (optional) | the web layer only (controllers + validation + `@ControllerAdvice`), with collaborators faked | a thin Spring slice | yes | `@WebMvcTest` |
| **Integration** | the real stack: context, SQLite, Liquibase, JPA, Security, end-to-end behaviour | yes | no | `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `WebTestClient` |

You do **not** have to cover everything at every level. A good split is:
- **most** business rules (Steps 1-3, ownership, `active=false`, versioning
  visibility) as **unit tests** on your services, with a mocked repository;
- **end-to-end contracts** (status codes, JSON shape, Security 401/403) as a
  handful of **integration tests** (Step 11 asks for ≥ 20 tests *in total*).

### 1) Unit test: test a service with mocks

Test the behaviour of a method in isolation. Collaborators (repository, the
geocoding port, …) are **mocks** created with Mockito — no Spring, no database.

```java
@ExtendWith(MockitoExtension.class)
class ListingServiceImplTest {

    @Mock ListingRepository repository;      // fake collaborator
    @Mock GeoEnrichmentPort geoPort;          // fake external service
    @InjectMocks ListingServiceImpl service;  // real SUT, collaborators injected

    @Test
    void nonOwnerUpdateIsRejected() {
        Long other = 2L;
        when(repository.findById(1L)).thenReturn(Optional.of(aListingOwnedBy(1L)));
        assertThatThrownBy(() -> service.update(1L, other, new ListingRequestDto()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void failedGeocodingStillSucceeds() {
        when(geoPort.resolve("1 rue x")).thenThrow(new GeoException("boom"));
        // act: service.update(...) -> still returns/saves, lat/lng null, WARN logged
        // assert coordinates are null and the listing was saved (verify(save))
    }
}
```

Rules of thumb:
- Name the test for the **behaviour**, not the method (`nonOwnerUpdateIsRejected`).
- `when(...)` sets the **arrange**, call the method (**act**), AssertJ checks it
  (**assert**). `verify(repository).save(...)` checks an expected interaction.
- If the object under test has dependencies, prefer a **constructor** so they
  are injectable (this is also what the DI framework in the app does).

### 2) Integration test: the real stack, the HTTP contract

Boot the entire application on a random local port and exercise the endpoints
the way an API consumer would. The skeleton's note in
`README.md#notes-on-spring-boot-4` points to the Boot 4 web test mechanism;
the canonical recipe is `@SpringBootTest(webEnvironment = RANDOM_PORT)` +
`@LocalServerPort` + `WebTestClient.bindToServer()`:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ListingsApiIntegrationTest {

    @LocalServerPort int port;
    @Autowired ListingRepository repository;

    WebTestClient client;

    @BeforeEach
    void setUp() {
        repository.deleteAll();          // start from a known, empty database
        client = WebTestClient.bindToServer()
                               .baseUri("http://localhost:" + port)
                               .build();
    }

    @Test
    void unauthenticatedSellerGets401OnCreate() {
        client.post().uri("/listings")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"title\":\"bike\",\"price\":99,\"category\":\"SPORTS\"}")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void sellerCanCreateAListing() {
        register("a@t.org");

        client.post().uri("/listings")
                .header(HttpHeaders.AUTHORIZATION, basic("a@t.org", "secret1"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"title\":\"bike\",\"price\":99,\"category\":\"SPORTS\"}")
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().valueEquals(HttpHeaders.CONTENT_TYPE, "application/json");
        // ... then register a second seller and assert PUT/DELETE -> 403
    }

    // ---------- test helpers ------------------------------------------------
    void register(String email) {
        client.post().uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"email\":\"" + email + "\",\"username\":\"u\",\"password\":\"secret1\"}")
                .exchange().expectStatus().isCreated();
    }
    static String basic(String user, String pass) {
        return "Basic " + Base64.getEncoder()
                                .encodeToString((user + ":" + pass).getBytes());
    }
}
```

> **Alternative client — `MockMvc`:** if you prefer the Spring MVC
> `MockMvc` flavour (no real server), you can attach Basic credentials
> with `spring-security-test`'s
> `SecurityMockMvcRequestPostProcessors.httpBasic("user", "pass")`:
>
> ```java
> .perform(post("/listings").with(httpBasic("a@t.org", "secret1"))...)
> ```
>
> Pick one client and stick with it; the same assertions apply.

A few practical points:
- **Isolate test data**: an integration test that boots against the same
  `listings.db` will see rows created by other tests / previous runs. Either
  wipe the tables in a `@BeforeEach`, or run tests on an in-memory SQLite
  (`jdbc:sqlite::memory:`) via a test profile — that is what
  `application-dev.yml` already suggests.
- **Assert the contract, not the code**: status, JSON field presence /
  absence, and response headers (e.g. Step 6 `Deprecation`, `Sunset`,
  `X-API-Version-*`) are what a real client observes.
- **Speed**: an integration test boots the whole world (context, Liquibase,
  Security) — keep **few** of them and put the bulk of your logic into
  **unit tests** above.

### 3) Mocking an external service (Nominatim) — never call the real one

The geocoding client (Step 7) talks to the network. Two safe ways to fake it
in tests:

- **Unit level**: depend on a *port* (an interface) and inject a Mockito mock,
  exactly like the `@Mock GeoEnrichmentPort geoPort` in the unit-test example
  above. Fastest, and the cleanest spot — which is why Step 7 asks for the
  client to be behind an interface.
- **Integration level**: replace the *real* geocoding bean with a test double
  so the whole app starts but never opens a network connection. With Boot 3.4/4
  use `@MockitoBean` (import
  `org.springframework.test.context.bean.override.mockito.MockitoBean`):

  ```java
  @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
  class GeoEnrichmentIntegrationTest {
      @MockitoBean GeoEnrichmentPort geo;        // replaces the real bean

      @Test
      void failedGeocodingStillReturns201() {
          when(geo.resolve(ArgumentMatchers.anyString()))
                   .thenThrow(new RuntimeException("timeout"));
          // act: POST /listings with a streetAddress -> expect 201 and lat/lng null
      }
  }
  ```
  (`@MockBean` is the older name; `@MockitoBean` is the current one.)

  The important part is the **intent**: the test controls success / time-out /
  empty result, the production code only ever sees the interface.

### What NOT to do

- Call `https://nominatim.openstreetmap.org` from a test (rate-limited, flaky,
  and it violates Step 7 — external collaborators must be mocked).
- Test private methods or internal bean wiring — test **observable behaviour**.
- Write one giant integration test that checks everything — keep the small,
  fast unit tests for the rules, and a few integration tests for the contract.

---

## Step 1 - User Accounts `CORE`

> **User story** — *As a **Visitor** who wants to sell, I want to **register
> an account** so that I can **publish, update and withdraw my listings** —
> and **be identified as their owner by the platform**.*

### Behavior

| # | Requirement |
|---|---|
| 1.1 | A user can **register** a new account with `email`, `username`, and `password`. On success the account is created and a **201** is returned with the created resource (the response must NOT contain the password or its hash). |
| 1.2 | If the email is already registered → **409 Conflict**. |
| 1.3 | If any field is missing or invalid (e.g. empty email, password < 6 chars) → **422 Unprocessable Entity** with per-field error messages. |
| 1.4 | Authentication is **HTTP Basic** - no login form, no session. An authenticated request simply carries the `Authorization` header. `GET /auth/me` returns the currently authenticated account (**200**) or **401** when anonymous. |
| 1.5 | Passwords must be **hashed** at rest (plain text never persisted). |
| 1.6 | All new accounts receive the role **SELLER**. |

### Endpoints

| Method | Path | Auth | Success |
|---|---|---|---|
| POST | `/auth/register` | Public | 201 |
| GET | `/auth/me` | HTTP Basic | 200 |

### Hints

- `SecurityConfig` already enables `httpBasic()`; add a `UserDetailsService` that
  loads users from `UserRepository` (email as the login, BCrypt hash, authority
  `SELLER`).
- Hash the password at registration time with the existing `PasswordEncoder`
  bean - never persist plain text.
- 409: in `UserServiceImpl.create`, check uniqueness (e.g.
  `UserRepository.existsByEmail(...)`, a derived query) and throw
  `EmailAlreadyExistsException`.
- 422: annotate the request DTO with jakarta.validation (`@Email`, `@NotBlank`,
  `@Size(min = 6)` on the password) and mark it `@Valid` in the controller;
  the skeleton's 422 handler already builds the `fields` map.
- `/auth/me`: resolve the current user from `SecurityContextHolder` and map it to
  a small response shape (id, email, username, role).
- Test with `spring-security-test`: `httpBasic("alice@test.org", "secret123")`.

### Acceptance checklist

- [ ] POST `/auth/register` valid → 201 + body (email, username, role, … - no password)
- [ ] POST `/auth/register` same email twice → 409 + JSON body
- [ ] POST `/auth/register` invalid email / short password → 422 + `fields`
- [ ] GET `/auth/me` without credentials → 401
- [ ] GET `/auth/me` with valid Basic credentials → 200

---

## Step 2 - Listings CRUD (Version 1) `CORE`

> **User stories** —
>
> - *As a **Seller**, I want to **publish, read, update and withdraw an item** so
>   that I can **manage my offers**.*
> - *As a **Visitor**, I want to **browse active listings, with filters and
>   pagination**, so that I can **find what I'm looking for without an account**.*

### Data contract (V1)

The V1 listing exposes **only public fields**:

| Field | Type | Required (write) |
|---|---|---|
| `id` | long | read-only |
| `title` | string (1–120) | Yes |
| `description` | string (0–2000) | No |
| `price` | decimal (positive) | Yes |
| `category` | enum (9 values, see `Category`) | Yes |
| `creationDate` | date | read-only |
| `active` | boolean | managed via PUT (seller only) |

### Behavior

| # | Requirement |
|---|---|
| 2.1 | **Create**: a `SELLER` can POST a new listing. Returns **201** + created resource. |
| 2.2 | **List**: `GET` returns a **paginated** list (default page size ≤ 20). Supports filters: `category`, `maxPrice`. |
| 2.3 | **Read**: `GET /{id}` returns the listing or **404** if not found. |
| 2.4 | **Update**: the **seller only** can PUT. Returns **200**. Non-seller → **403**. |
| 2.5 | **Delete**: the **seller only** can DELETE. Returns **204**. Non-seller → **403**. |
| 2.6 | Unauthenticated write attempts → **401**. |
| 2.7 | Validation errors → **422** with `fields` map. |
| 2.8 | `active` can be toggled by the seller via PUT. |
| 2.9 | An `active=false` listing is **not** returned in list responses for non-sellers (anonymous or other SELLERs). |
| 2.10 | An `active=false` listing returns **404** on detail for non-sellers. The seller still sees it. |

### Endpoints

| Method | Path | Auth | Success |
|---|---|---|---|
| POST | `/listings` | SELLER | 201 |
| GET | `/listings` | Public | 200 |
| GET | `/listings/{id}` | Public | 200 |
| PUT | `/listings/{id}` | SELLER (seller) | 200 |
| DELETE | `/listings/{id}` | SELLER (seller) | 204 |

### Hints

- Pagination: bind Spring Data's `Pageable` in the controller (e.g.
  `?page=0&size=10`), default size ≤ 20.
- Filters: optional parameters `category` and `maxPrice` → derived repository
  methods (e.g. `findByCategoryAndPriceLessThanEqual(...)`) or a `@Query`.
- Sellership: in the service, before PUT/DELETE, compare
  `listing.getSeller().getId()` with the current user id (see `/auth/me` for how
  to get it) → throw `AccessDeniedException` (→ 403).
- Not found / inactive-for-others: throw `ResourceNotFoundException` (→ 404).
- The `Listing` entity needs an `seller` relation (`@ManyToOne` to `User`) -
  the skeleton leaves it to you; referential integrity is carried by JPA
  (SQLite/Liquibase are not used for FKs - see README).
- Tests: three clients (anonymous, seller, another SELLER) × each endpoint.

### Acceptance checklist

- [ ] POST → 201, body has `id`, `creationDate`; response is JSON
- [ ] GET list is paginated (default size ≤ 20); `category` and `maxPrice` filters work
- [ ] GET `/listings/{unknown}` → 404 + JSON body
- [ ] PUT own → 200 · PUT other → 403 · DELETE own → 204 · DELETE other → 403
- [ ] Anonymous POST/PUT/DELETE → 401
- [ ] Invalid payload (title > 120, price ≤ 0, missing category) → 422 + `fields`
- [ ] Seller toggles `active=false` → listing disappears from the list of anonymous/other users
- [ ] `active=false` → `GET /{id}` is 404 for anonymous/other, still 200 for the seller

---

## Step 3 - Security Rules `CORE`

> **User stories** —
>
> - *As a **Seller**, I want **only my own listings** to be editable and
>   deletable by me so that **no one else can tamper with my offers**.*
> - *As a **Visitor**, I want to keep being able to **browse freely**, so that
>   **browsing requires no account**, even though **writing requires one**.*

### Behavior

| # | Requirement |
|---|---|
| 3.1 | **Read** (`GET /listings*`) is **public** - no authentication required. |
| 3.2 | **Write** (`POST/PUT/DELETE /listings*`) requires an authenticated user with role **SELLER** (HTTP Basic). |
| 3.3 | **Sellership**: regardless of anything else, only the listing's seller may PUT or DELETE. Other authenticated SELLERs receive **403**. |
| 3.4 | `/auth/register` and **all actuator endpoints** (`/actuator/health`, `/actuator/info`, `/actuator/metrics`) remain public. |

### Test matrix

| Client | GET list | GET detail | POST | PUT own | PUT other | DELETE own | DELETE other |
|---|---|---|---|---|---|---|---|
| Anonymous | 200 | 200/404 | 401 | 401 | 401 | 401 | 401 |
| SELLER (not seller) | 200 | 200/404 | 201 | 403 | 403 | 403 | 403 |
| SELLER (seller) | 200 | 200 | 201 | 200 | - | 204 | - |

### Hints

- In `SecurityConfig.requestMatchers(...)`: public `GET /listings**`,
  authenticated + `hasRole("SELLER")` on `POST/PUT/DELETE /listings`, public
  `/auth/register` and `/actuator/**`.
- The **403** for non-sellers comes from your **service** layer (Step 2), which is
  why an anonymous writer gets 401 (Security) while an authenticated non-seller
  gets 403 (service).
- Test with `httpBasic(...)` for the SELLER rows; no header for the anonymous row.

### Acceptance checklist

- [ ] Every row of the test matrix behaves exactly as above
- [ ] `/actuator/health`, `/actuator/info`, `/actuator/metrics` reachable without credentials
- [ ] POST without credentials → 401 (Basic challenge)
- [ ] PUT on someone else's listing with valid credentials → 403 (not 401, not 404)

---

## Step 4 - Content Negotiation (JSON + XML) `BONUS`

> **User story** — *As an **API consumer** (developer), I want the same
> **endpoint to answer in the format I ask for** (JSON or XML), so that my
> client — Java, .NET, mobile, … — **consumes the response natively** without
> an adapter.*

> This step adds `application/xml` alongside `application/json`. It requires one
> extra dependency - see **Appendix B** before starting.

### Behavior

| # | Requirement |
|---|---|
| 4.1 | `GET` endpoints respond in `application/json` or `application/xml` based on the `Accept` header. |
| 4.2 | `POST`/`PUT` endpoints accept `application/json` or `application/xml` based on the `Content-Type` header. |
| 4.3 | The **same data** is serialized/deserialized in both formats (field names, structure, values identical). |
| 4.4 | If the `Content-Type` is not supported (e.g. `application/yaml`) → **415 Unsupported Media Type**. |
| 4.5 | If the `Accept` type is not supported (e.g. `application/csv`) → **406 Not Acceptable**. |
| 4.6 | Error responses (4xx) are always returned as **JSON** regardless of `Accept`. |

### Test scenarios

| Request | Expected |
|---|---|
| `POST /listings` with `Content-Type: application/json` | 201, JSON body |
| `POST /listings` with `Content-Type: application/xml` | 201, XML body |
| `GET /listings/{id}` with `Accept: application/xml` | 200, XML body |
| `GET /listings/{id}` with `Accept: application/json` | 200, JSON body |
| `POST /listings` with `Content-Type: application/octet-stream` | 415 |
| `GET /listings/{id}` with `Accept: text/csv` | 406 |

### Hints

- Add the dependency from Appendix B - once on the classpath, Spring Boot
  registers the XML message converter automatically; your DTOs stay unchanged.
- 4.4/4.5 are raised by the framework by default; just add the two `@ExceptionHandler`
  entries from the "Error handling" table so they get the standard JSON body.

### Acceptance checklist

- [ ] POST JSON → 201 JSON; POST XML → 201 XML (identical data)
- [ ] GET with `Accept: application/xml` → 200 XML; with `Accept: application/json` → 200 JSON
- [ ] POST with `Content-Type: application/octet-stream` → 415 JSON body
- [ ] GET with `Accept: text/csv` → 406 JSON body
- [ ] All error bodies stay JSON even when XML was requested

---

## Step 5 - Schema Evolution (V2 fields in the database) `CORE`

> **User story** — As the **team of developers**, we want to add new columns
> to the database **without touching or breaking** the existing tables and
> data, so that the **next features** (negotiable flag, geo fields, distance
> search, …) can be built progressively on top of the same dataset.

### Behavior

The database schema is **extended** with new columns. This step is purely
structural - the V1 API behavior does not change yet.

| Migration | Added columns |
|---|---|
| v1.1 | `negotiable` (boolean) |
| v1.2 | `street_address`, `city`, `postal_code`, `country`, `latitude` (double), `longitude` (double) |

### Requirements

| # | Requirement |
|---|---|
| 5.1 | Migrations are **additive** (ADD COLUMN). No existing column is modified or dropped. |
| 5.2 | All existing tests from Steps 1-3 remain **green** after migration. |
| 5.3 | The new columns are **nullable** (existing rows have no value). |
| 5.4 | `mvn clean verify` passes with all migrations applied cumulatively. |

### Hints

- Two new changelog files next to `v1.0-init.yaml`:
  `v1.1-negotiable.yaml` (`negotiable` BOOLEAN) and `v1.2-geo.yaml` (the 6 geo
  columns); each changeSet uses `addColumn` only.
- Uncomment the two `include`s in `db.changelog-master.yaml`.
- Map the new attributes on the `Listing` entity - `ddl-auto` is `none`, so only
  Liquibase creates the columns; Hibernate only maps them.
- Verify: the `DATABASECHANGELOG` table (visible in `listings.db`) must list
  v1.0, v1.1 and v1.2 changeSets.
- Never edit an already published changelog; a need for a new column → a new
  changeSet.

### Acceptance checklist

- [ ] App starts with v1.0 + v1.1 + v1.2 applied (3+ rows in `DATABASECHANGELOG`)
- [ ] `mvn clean verify` green
- [ ] Steps 1-3 tests still pass
- [ ] A listing created before v1.1/v1.2 reads `negotiable`/geo as `null`

---

## Step 6 - Versioning (V1 + V2) `CORE`

> **User stories** —
>
> - *As an **API consumer**, I want to **explicitly select V1 or V2** via a
>   header or a query parameter, and the API to **return the matching fields**
>   (public vs. owner-only) so that **my code can rely on a stable contract
>   while the platform evolves**.*
> - *As a **platform admin**, I want to **deprecate V1 with clear headers**
>   (`Deprecation`, `Sunset`) so that **existing clients have a deadline to
>   migrate**.*

### V2 data contract

V2 extends V1 with additional fields:

| Field | V1 | V2 | Visibility |
|---|:---:|:---:|---|
| Public fields (title, description, price, category, creationDate) | ✓ | ✓ | Always |
| `negotiable` | - | ✓ | Public |
| `active` | managed in V1 | ✓ | **Seller only** |
| `streetAddress`, `city`, `postalCode`, `country` | - | ✓ | **Seller only** |
| `latitude`, `longitude` | - | ✓ | **Seller only** |

### Versioning behavior

| # | Requirement |
|---|---|
| 6.1 | The client selects the API version via **header** (`X-API-Version: V1` or `V2`) **or** **query parameter** (`?version=V1`). Both are equivalent. |
| 6.2 | **Default**: if no version is specified, the API responds with **V1** and includes the response header `X-API-Version-Default: V1`. |
| 6.3 | **Invalid version** (e.g. `V99`) → **400** with body: `{ "error": "API version not available", "availableVersions": ["V1", "V2"] }` |
| 6.4 | **Conflict** (header says `V1`, param says `V2`) → **400** (explicit error, no silent resolution). |
| 6.5 | **V1 responses** must include the headers: `Deprecation: true` and `Sunset: Sun, 01 Jan 2027 00:00:00 GMT`. |
| 6.6 | **V2 responses** must NOT include deprecation headers. |
| 6.7 | **V1** never returns `negotiable` or the private fields (active, address, geo) - regardless of authentication. |
| 6.8 | **V2 non-seller**: returns `negotiable` but **never** `active`, `streetAddress`, `city`, `postalCode`, `country`, `latitude`, `longitude`. |
| 6.9 | **V2 seller**: returns ALL fields including private ones. |
| 6.10 | All business rules from Steps 2-3 (pagination, sellership, 404 on inactive) apply identically in both versions. |

### OpenAPI

| # | Requirement |
|---|---|
| 6.11 | Two **separate** OpenAPI specs are published: one for V1, one for V2, with distinct titles. |

### Hints

- Native Spring versioning: register the two conditions (header `X-API-Version`
  and parameter `version`) for versions `V1`/`V2` - see
  `ApiVersioningConfig` (javadoc there points to the mechanism) - then declare
  `@RequestMapping(value = "/listings", version = "V1")` (and `"V2"`) on the
  controllers.
- The fallback (6.2-6.4) is **not** given for free by native versioning: an
  unspecified version must be routed to V1, an unknown one to 400, and a header/param
  conflict to 400. Think about where you can inspect both inputs before dispatch
  (a filter or a `HandlerInterceptor`), or how the V1 controller can also be
  reachable without any version input.
- 6.5/6.6: add the `Deprecation` / `Sunset` headers to V1 responses only (e.g. in
  the same interceptor that knows which version was selected).
- 6.7-6.9: `application.yml` already sets Jackson to `non_null` inclusion - if the
  private fields are `null` in the response object, they are omitted from the
  JSON/XML payload. Use that: build the V2 response without private fields for
  non-sellers.
- 6.11: publish two `GroupedOpenApi` beans (springdoc), one per version, each with
  its own title.

### Acceptance checklist

- [ ] GET `/listings` (no version) → V1 payload + response header `X-API-Version-Default: V1`
- [ ] `X-API-Version: V2` → V2 payload; `?version=V2` → identical payload (equivalence)
- [ ] `X-API-Version: V99` → 400 + `{ "error": "API version not available", "availableVersions": ["V1", "V2"] }`
- [ ] Header `V1` + `?version=V2` → 400
- [ ] V1 responses carry `Deprecation: true` and `Sunset: Sun, 01 Jan 2027 00:00:00 GMT`; V2 responses carry neither
- [ ] V1 payload never contains `negotiable`, `active`, address or geo fields (for any client)
- [ ] V2 non-seller: `negotiable` present, all private fields absent · V2 seller: all present
- [ ] Pagination, sellership, and `active=false` → 404 rules behave identically in V1 and V2
- [ ] Two OpenAPI documents (V1, V2) with distinct titles

---

## Step 7 - Geolocation (GPS enrichment) `CORE`

> **User stories** —
>
> - *As a **Seller**, I want to provide just an address and have **lat/lng
>   resolved automatically** so that **buyers can find me on a map** without
>   knowing coordinates.*
> - *As a **Visitor / buyer**, I want listings to be **geolocated when the
>   seller gave an address**, so that I can **search by distance later**
>   (Step 9).*

### Behavior

| # | Requirement |
|---|---|
| 7.1 | When a listing is **updated** (V2) with a `streetAddress` plus `city`/`country` if available, the API attempts to resolve `latitude` and `longitude` from a **free public geocoding service** (Nominatim - see Appendix A). |
| 7.2 | If the geocoding call **succeeds**, the resolved coordinates are persisted and returned in the response. |
| 7.3 | If the geocoding call **fails** (network error, timeout, no result): the listing is **still updated successfully** (200) with `latitude`/`longitude` left as `null`. A WARN is logged with the reason. |
| 7.4 | The geocoding call must NOT block the request for more than **3 seconds** (timeout). |
| 7.5 | The geocoding call is only made when `streetAddress` is non-empty. If absent, no call is made and coordinates remain null. |
| 7.6 | **Bonus**: implement a remediation mechanism (retry with backoff, a re-sync endpoint, or a pending-geocoding queue) so failed enrichments can be retried without user intervention. |

### Hints

- Use Boot's native `RestClient` (no webclient needed):
  `RestClient.builder().baseUrl(endpoint).requestFactory(factory).build()`,
  where `factory` is a request factory configured with a 3-second
  connect/read timeout.
- Call `GET /search?format=json&limit=1&q={address}` - the response is a JSON
  **array** of objects whose `lat`/`lon` values are **strings** to parse.
- Put the client behind a small interface (a "port") that the listing service
  depends on - that is what you will **mock** in tests, and it prepares Step 10.
- Failure path: catch the exception, log at WARN (address + reason), leave the
  coordinates `null`, continue → 200.
- The `listings.geo.*` block in `application.yml` already exists for the endpoint /
  timeout configuration.
- Real Nominatim is limited to ~1 request/s (Appendix A). Tests must mock the port.

### Test scenarios

| Scenario | Expected |
|---|---|
| Update with valid `streetAddress` (mock → valid result) | 200, `latitude`/`longitude` populated |
| Update with valid `streetAddress` (mock → 500) | 200, `latitude`/`longitude` null, WARN in log |
| Update with valid `streetAddress` (mock → empty result) | 200, `latitude`/`longitude` null |
| Update without `streetAddress` | 200, no geocoding call made (assert on the mock), coordinates null |

### Acceptance checklist

- [ ] The four scenarios above behave exactly as expected (mock-based, no real network)
- [ ] A slow mock (e.g. 5 s) is aborted and treated as a failure (timeout ≤ 3 s)
- [ ] WARN log lines include the addressed street and the failure reason

---

## Step 8 - Metrics & Logging `CORE`

> **User story** — *As a **developer / ops**, I want the service to expose
> **counters for successful creations and failed geocodings**, and to log
> failures with a **reason**, so that I can **monitor, and prove** that the
> service is working (or failing) — and **diagnose** the cause.*

### Custom metrics

| Metric | Type | Trigger |
|---|---|---|
| `listings.created` | Counter | Increment on every successful listing creation (201) |
| `geo.enrich_failed` | Counter | Increment on every failed geocoding call |

### Logging requirements

| # | Requirement |
|---|---|
| 8.1 | Failed geocoding → log at **WARN** level with the reason (see Step 7.3). |
| 8.2 | Application name is tagged in all metrics (already configured in `application.yml`). |
| 8.3 | Debug-level SQL logging is available in the `dev` profile only (not by default). |

### Hints

- Inject `io.micrometer.core.instrument.MeterRegistry`; use
  `meterRegistry.counter("listings.created").increment()` in the create use-case
  (after the 201 path succeeds), and `counter("geo.enrich_failed")` in the
  geocoding fallback path (Step 7.3).
- `management.metrics.tags.application` is already set - every metric you
  register gets the `application="listings"` tag.
- Check from the browser: `GET /actuator/metrics/listings.created`.
- `application-dev.yml` already turns on `org.hibernate.SQL: DEBUG` - the
  requirement is that this must **not** happen outside the `dev` profile.

### Acceptance checklist

- [ ] `GET /actuator/metrics/listings.created` exists and increments after a successful creation
- [ ] `GET /actuator/metrics/geo.enrich_failed` increments when geocoding fails (mocked)
- [ ] Both metrics carry the tag `application="listings"`
- [ ] Failed geocoding produces a WARN log with a reason
- [ ] Running without profile: no DEBUG SQL · running with `--spring.profiles.active=dev`: DEBUG SQL visible

---

## Step 9 - Distance Search `BONUS`

> **User story** — *As a **Visitor / buyer**, I want to **search listings by
> my current lat/lng + a radius in km**, so that I can **choose the closest
> one** without scrolling through the whole catalogue.*

### Behavior

| # | Requirement |
|---|---|
| 9.1 | **V2 only**: `GET /listings?lat={lat}&lng={lng}&radius={km}` returns listings whose coordinates fall within the specified radius (in **kilometers**). |
| 9.2 | Listings without coordinates are **excluded** from results. |
| 9.3 | Parameters `lat`, `lng`, `radius` are **all required** together. If any is missing → **400**. |
| 9.4 | `radius` must be a positive integer (km). `radius < 1` → **400**. |
| 9.5 | `lat` must be in [-90, 90], `lng` in [-180, 180]. Out of range → **400**. |
| 9.6 | Calling this on **V1** → **400** (feature unavailable in this version). |
| 9.7 | Visibility rules (`active=false`, seller-only private fields) apply as in Step 6. |
| 9.8 | **(Optional)** Results sorted by distance, closest first. |
| 9.9 | **(Bonus)** Justify your implementation approach (in-memory filtering over SQLite, SQL-level, bounding box + precision, etc.) in the README. |

---

## Step 10 - Hexagonal Architecture `BONUS`

> **User story** — *As a **developer**, I want the codebase to be
> **layered into `domain` / `application` / `infrastructure`** with dependency
> rules I can **verify by an architectural test**, so that the **business
> rules can be tested without Spring or a database**, and the framework stays
> in the outer ring.*

### Behavior

| # | Requirement |
|---|---|
| 10.1 | The codebase is structured in **three layers** with strict dependency direction: |
| | - `domain`: pure business logic, no framework imports |
| | - `application`: use cases, ports (interfaces for both inbound and outbound) |
| | - `infrastructure`: adapters (web, persistence, external services) |
| 10.2 | `domain` does NOT depend on Spring, JPA, or any web framework. |
| 10.3 | `application` does NOT depend on `infrastructure` (depends on interfaces/ports instead). |
| 10.4 | `infrastructure` depends on `domain` and `application`, never the reverse. |
| 10.5 | All tests from Steps 1-8 (and 9 if done) remain **green** after restructuring. |
| 10.6 | Provide an architectural test (ArchUnit or similar) that verifies the dependency rules. |

> Note: Step 7 already suggests the "port" split for the geocoding client -
> re-use it here.

---

## Step 11 - Final Integration

> **User story** — *As the **team of developers** (and the class evaluating
> us), we want to **ship a runnable, tested, documented micro-service**, so
> that **anyone can reproduce, inspect, and trust** the work in 10 minutes.*

### Requirements

| # | Requirement |
|---|---|
| 11.1 | `mvn clean verify` is **green** - all tests pass, including the cumulative Liquibase migrations. |
| 11.2 | The README documents: how to run, all endpoints with examples (JSON + XML if Step 4 done), versioning usage, GPS fallback behavior, metrics, and your justified technical choices. |
| 11.3 | Total test count ≥ **20** (unit + integration, + architectural if Step 10 done). |
| 11.4 | No `TODO` comments and no `Not implemented yet` stubs remain in production code. |
| 11.5 | Oral presentation: 10 minutes (architecture, key decisions, demo) + 5 minutes Q&A. |

---

## Deliverables

| Item | Detail |
|---|---|
| Git repository (pair) | Clean history, meaningful commits |
| `mvn clean verify` | Green, all tests passing |
| README.md | Complete (run instructions, endpoint examples, decisions) |
| Oral | 10 min presentation + Q&A |

---

## Appendix A - Nominatim (geocoding)

Free public geocoder (OpenStreetMap). Reference for Step 7.

- Base URL: `https://nominatim.openstreetmap.org`
- Endpoint: `GET /search?format=json&limit=1&q={address}`
  (`q` is URL-encoded; combine street + city for better results).
- Response (array - note `lat`/`lon` are **strings**):

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

- Parse `lat`/`lon` with `Double.parseDouble` inside a `try/catch` (defensive).
- Rate limit: ~1 request/second; a `User-Agent` identifying your app is required
  by the usage policy. **Do not call the real service in tests** - mock the port.
- An empty array or a non-2xx response = "no result" → Step 7.3 fallback.

---

## Appendix B - Adding XML support (Step 4)

Why an extra dependency: `spring-boot-starter-web` ships Jackson **JSON** only.
To (de)serialize `application/xml` you add the Jackson XML module - once on the
classpath, Spring Boot registers `MappingJackson2XmlHttpMessageConverter`
automatically and your DTOs remain unchanged:

```xml
<dependency>
    <groupId>com.fasterxml.jackson.dataformat</groupId>
    <artifactId>jackson-dataformat-xml</artifactId>
    <!-- version managed by the Spring Boot BOM -->
</dependency>
```

- Element names follow the Java bean property names (e.g. `<negotiable>true</negotiable>`).
- This is the **only** `pom.xml` change the exercise prescribes.
