# Spring Boot TP1 — listings API skeleton

Operational guide (install, run, build, project layout) for the classifieds
**listings** micro-service. The statement, the steps, and the contracts are in
[`exercise.md`](exercise.md), the source of truth for the exercise. The
copy-pasteable checks are in [`docs/verification.md`](docs/verification.md).

This folder is also **the base of every TP2** (Keycloak, Monitoring, IA,
Messaging): its `docker-compose.yml` creates the shared `middleware` network
that the TP2 stacks attach to.

---

## Prerequisites

The project is **Spring Boot 4.1.1 / Java 27** with **PostgreSQL 16**. Two ways
to run it:

| Mode | What it is | Needs on the host |
|---|---|---|
| **1. Docker Compose** (default) | build and run the API and its PostgreSQL in containers | **Docker Engine + Compose plugin** only |
| **2. Maven on the host** | run the app from your IDE / terminal, against the compose PostgreSQL | **JDK 27 + Maven 3.9+**, and Docker for PostgreSQL |

Mode 1 is the default path and the one the TP2s build on. Use mode 2 when you
want your IDE's debugger attached to the app.

### Operating system and shell

All commands are **Bash on Linux**: a Linux distribution, or **WSL2** on
Windows (Ubuntu or Debian, Docker Desktop with WSL integration, repos cloned
in `~/`). PowerShell, cmd and Git Bash are not supported.

### Install — mode 1 (Docker Compose)

Skip the Docker lines with Docker Desktop (WSL integration): Docker comes from
Windows.

```bash
sudo apt update
sudo apt install -y python3 curl docker.io docker-compose-v2   # Ubuntu
sudo apt install -y python3 curl docker.io docker-compose      # Debian
sudo systemctl enable --now docker
sudo usermod -aG docker $USER        # then log out and in again

docker compose version               # expect "Docker Compose version v2.x"
```

No Java and no Maven are needed on the host in this mode.

### Install — mode 2 (Maven on the host)

No distribution packages JDK 27, and Ubuntu 24.04 ships Maven 3.8. Use
SDKMAN (<https://sdkman.io>):

```bash
sdk list java | grep -- '-tem'      # pick the 27.x Temurin identifier
sdk install java <27.x-tem>
sdk install maven
java -version                       # expect 27.x
mvn -version                        # expect 3.9+
```

### IDE setup (either mode)

- **IntelliJ IDEA** — `File → Open` on this folder (the one with `pom.xml`);
  `File → Project Structure → SDKs → Download JDK 27`; run
  `ListingsApplication`.
- **VS Code** — "Extension Pack for Java"; point `java.jdt.ls.java.home` to a
  JDK 27.

---

## Run

Pick **one** mode per session. Both use port 8080: running both at once makes
the second one fail to start.

### Mode 1 — Docker Compose

```bash
cd springboot/
docker compose up -d --build         # build the image, start API + PostgreSQL
docker compose ps                    # wait for "healthy" on both
curl -fsS http://localhost:8080/actuator/health
```

- **After every Java or resource change**, rebuild:
  `docker compose up -d --build listings-api`. A plain `restart` reruns the old
  image.
- Logs: `docker compose logs -f listings-api`.
- Stop: `docker compose down`. Wipe the database: `docker compose down -v`.

### Mode 2 — Maven on the host

```bash
cd springboot/
docker compose up -d listings-postgres       # only the database
mvn spring-boot:run
curl -fsS http://localhost:8080/actuator/health
```

The app reads `jdbc:postgresql://localhost:5435/listings` (user / password
`listings`) from `application.yml`; override with `DB_HOST`, `DB_PORT`,
`DB_NAME`, `DB_USER`, `DB_PASS`.

### URLs available at startup

| URL | What it is |
|---|---|
| <http://localhost:8080/actuator/health> | health check (JSON, `"status":"UP"`) |
| <http://localhost:8080/actuator/info> | application info |
| <http://localhost:8080/actuator/metrics> | list of metric names |
| <http://localhost:8080/v3/api-docs> | OpenAPI document (JSON); after Step 6b, use the groups `/v3/api-docs/listings-v1` and `/v3/api-docs/listings-v2` |
| <http://localhost:8080/swagger-ui.html> | Swagger UI |
| `localhost:5435` | PostgreSQL (db / user / password `listings`) |

On day 1 the business endpoints (`/auth/*`, `/listings*`) exist but return
**500** with a `Not implemented yet` message, or **401** without credentials.
The steps of [`exercise.md`](exercise.md) implement them.

---

## Build & test

`mvn clean verify` compiles, runs `ListingsSmokeTest`, and packages the jar.
The smoke test boots the full context, so it needs PostgreSQL.

```bash
# mode 2 (JDK 27 on the host), with the compose database running
mvn clean verify
```

```bash
# mode 1 (no Java on the host): run Maven in a container on the compose network
docker compose up -d listings-postgres
docker run --rm --network middleware \
  -e DB_HOST=listings-postgres -e DB_PORT=5432 \
  -v "$PWD":/app -v listings-m2:/root/.m2 -w /app \
  maven:3-eclipse-temurin-27 mvn -B clean verify
```

In the container variant, `target/` is created by root; delete it with
`sudo rm -rf target` if your IDE complains.

---

## Tree

```
springboot/
├── README.md                          # this file: install / run / build
├── exercise.md                        # the statement (Steps 1-10)
├── docs/verification.md               # copy-pasteable checks, one section per step
├── docs/primer.md                     # Maven, Spring Boot, doc versions, test strategy
├── pom.xml                            # Boot 4.1.1, Java 27, PostgreSQL, Liquibase, springdoc
├── Dockerfile                         # multi-stage: JDK 27 + Maven -> JRE 27
├── docker-compose.yml                 # API (8080) + PostgreSQL (5435) on the `middleware` network
├── .dockerignore
├── .gitignore
│
├── src/main/java/com/app/listings
│   ├── ListingsApplication.java       # entry point
│   ├── application/service/
│   │   ├── UserService.java           # provided
│   │   ├── UserServiceImpl.java       # provided: register (409, BCrypt, SELLER), lookups
│   │   ├── ListingService.java        # to implement (Steps 2-3, 6-9)
│   │   └── ListingServiceImpl.java    # to implement
│   ├── config/
│   │   ├── ApiVersioningConfig.java   # to implement (Step 6)
│   │   └── SecurityConfig.java        # provided: BCrypt, UserDetailsService, Basic chain; tightened in Step 3
│   ├── domain/
│   │   ├── Category.java              # enum, 9 values
│   │   ├── Role.java                  # enum, SELLER
│   │   ├── entity/
│   │   │   ├── User.java              # provided, table app_user
│   │   │   └── Listing.java           # id only, to complete (Steps 2, 5)
│   │   ├── exception/
│   │   │   └── EmailAlreadyExistsException.java   # provided (409)
│   │   └── repository/
│   │       ├── UserRepository.java    # provided
│   │       └── ListingRepository.java # to complete
│   └── web/
│       ├── advice/GlobalExceptionHandler.java     # provided: 409, 422, 500; extended in every step
│       ├── controller/
│       │   ├── AuthController.java    # stubs, wired in Step 1
│       │   └── ListingController.java # stubs, Steps 2-3, 6, 9
│       └── dto/
│           ├── UserRequestDto.java    # provided
│           ├── UserResponseDto.java   # provided
│           ├── ListingRequestDto.java # empty, to complete
│           └── ListingResponseDto.java# empty, to complete
│
├── src/main/resources
│   ├── application.yml                # PostgreSQL, actuator, Jackson non_null, listings.* properties
│   ├── application-dev.yml            # dev profile: SQL logging
│   └── db/changelog/
│       ├── db.changelog-master.yaml   # includes the changelogs, in order
│       ├── v1.0-init.yaml             # provided: app_user.id, listing.id
│       └── v1.0.1-identity.yaml       # provided: identity columns of app_user
│
└── src/test/java/com/app/listings
    └── ListingsSmokeTest.java         # context test, needs PostgreSQL
```

---

## Schema migrations (Liquibase)

The PostgreSQL schema is managed by **Liquibase**, not by Hibernate
(`spring.jpa.hibernate.ddl-auto: none`). Every schema change is a YAML
**changeSet**, applied once at startup and recorded in the table
`databasechangelog`.

| File | Provided / added | Content |
|---|---|---|
| `v1.0-init.yaml` | provided | `app_user.id`, `listing.id` |
| `v1.0.1-identity.yaml` | provided | `email`, `username`, `password_hash`, `role`, `registration_date` on `app_user` |
| `v1.0.2-listing.yaml` | you, Step 2a | V1 columns of `listing` |
| `v1.1-v2-fields.yaml` | you, Step 5 | V2 columns of `listing` |

Rules:

- **Never edit an applied changeSet.** Liquibase stores a checksum per
  changeSet and refuses to start when an applied one has changed. Add a new
  changeSet instead.
- **Additive only**: add tables and columns; do not rename or drop.
- Each new file needs its `include` line in `db.changelog-master.yaml`, in
  execution order.

The table is `app_user`, not `user`: `user` is a reserved word in PostgreSQL.

Inspect the database:

```bash
docker compose exec listings-postgres psql -U listings -d listings -c '\dt'
docker compose exec listings-postgres psql -U listings -d listings -c '\d listing'
docker compose exec listings-postgres psql -U listings -d listings \
  -c 'SELECT orderexecuted, id, filename FROM databasechangelog ORDER BY orderexecuted'
```

---

## Dependencies added during the TP

The skeleton's `pom.xml` contains everything the core needs. The only
dependency the exercise adds is for **Step 4 (bonus, XML)**. Paste it in
`<dependencies>` (the why is in `exercise.md`, Appendix B):

```xml
<dependency>
    <groupId>tools.jackson.dataformat</groupId>
    <artifactId>jackson-dataformat-xml</artifactId>
    <!-- version managed by the Spring Boot BOM -->
</dependency>
```

Then rebuild (`docker compose up -d --build listings-api`).

---

## Notes on Spring Boot 4

- **Jackson 3**: packages and coordinates are `tools.jackson.*`. Jackson 2
  artifacts (`com.fasterxml.jackson.*`, except the annotations) are not used by
  Boot 4's auto-configuration.
- **Native API versioning** (Spring Framework 7): configured with
  `WebMvcConfigurer#configureApiVersioning`, used in Step 6.
- **Spring Security 7**: `DaoAuthenticationProvider` takes the
  `UserDetailsService` in its constructor (see `SecurityConfig`).
- The startup WARN `Global AuthenticationManager configured with an
  AuthenticationProvider bean` comes from the provided `SecurityConfig` and is
  harmless.

---

## To implement (summary — the contract is in `exercise.md`)

**Core**

1. **User accounts** — wire `/auth/register` and `/auth/me`.
2. **Listings CRUD (V1)** — `v1.0.2` changelog, entity, DTOs, pagination,
   filters, 404 / 400 / 422.
3. **Access rules** — public reads, `SELLER` writes, owner-only 403, hidden
   listings, stateless session.
5. **Schema evolution** — `v1.1` changelog, 7 V2 columns.
6. **Versioning** — `?version=V1|V2`, default V1, deprecation headers, V2
   visibility, two OpenAPI groups.
8. **Metrics** — counter `listings.creations`.

**Bonus** (start only when the core is green)

4. **Content negotiation** — JSON + XML, 415 / 406.
7. **GPS enrichment** — Nominatim through `RestClient`, 3 s timeout, fallback.
9. **Distance search** — V2 only.

**Delivery**

10. **Final integration** — green build, clean code, complete README.

> **Technical note**: referential integrity between `listing` and `app_user` is
> carried by the JPA relation (`@ManyToOne owner`). You may add a database
> foreign key in your `v1.0.2-listing.yaml` changeSet. Keep every migration
> additive.
