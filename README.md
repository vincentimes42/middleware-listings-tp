# Spring Boot exercise - Skeleton

Classifieds **listings** micro-service base. It serves as the foundation for the
common exercise (interface contract, versioning, content negotiation).
See [`exercise.md`](exercise.md) for the full statement.

---

> This README is **operational** (install, run, build, tree). Read
> [`exercise.md`](exercise.md) first: it is the source of truth for the steps,
> the domain, and how each step is verified.

---

## Prerequisites

The project is **Spring Boot 4.1.1 / Java 27** and uses **PostgreSQL** as its
database. There are two ways to run it, and they need different things on your
machine:

| Mode | What it is | Needs installed on the host |
|---|---|---|
| **1. Docker Compose** (recommended) | build + run the API and its PostgreSQL in containers | **Docker Engine + Docker Compose plugin** — nothing else |
| **2. Run on the host with Maven** | develop in your IDE / terminal against a local PostgreSQL | **OpenJDK 27 + Maven 3.9+ AND Docker** (for the PostgreSQL container; you could also install Postgres natively) |

> Mode 1 is the **default path for this TP** and the one the follow-up TPs
> (Keycloak, Monitoring) build on. It needs only Docker: the `Dockerfile` builds
> the app with its own Java 27, and `docker-compose.yml` starts both the API and
> its PostgreSQL. Use mode 2 when you want your IDE's debugger / test runner
> attached to the running app.

### Install — mode 1 (Docker Compose) [recommended]

```bash
# Docker Engine + Compose plugin (Debian / Ubuntu)
sudo apt update
sudo apt install -y docker.io docker-compose-plugin
sudo systemctl enable --now docker
sudo usermod -aG docker $USER        # then log out + in, or use `sudo docker`

# sanity
docker compose version                # expect "Docker Compose version v2.x"
```

That's all you need for mode 1 — **no Java, no Maven** on the host.

### Install — mode 2 (Maven) [optional]

```bash
# 1) OpenJDK 27 — first try your distro's package:
sudo apt install -y openjdk-27-jdk           # Debian / Ubuntu
# sudo dnf   install -y java-openjdk-devel   # Fedora / RHEL
# sudo pacman -S jdk-openjdk                 # Arch

# 2) Maven 3.9+:
sudo apt install -y maven
# (or your distro / brew / choco equivalent)

# 3) sanity
java -version      # expect 27.x
mvn -version       # expect 3.9+

# 4) PostgreSQL (for `mvn spring-boot:run`). Easiest: the compose file's Postgres.
cd <this folder>   # springboot/
docker compose up -d listings-postgres
# -> now reachable at localhost:5435 (db/user/pass: listings)
```

> If your distro doesn't ship OpenJDK 27 yet, install it via SDKMAN!:
> `curl -s "https://get.sdkman.io" | bash` then `sdk install java 27.0.1-tem`.
> Pick **one** source for the JDK (distro **or** SDKMAN!) and stick with it;
> set `JAVA_HOME` accordingly.

### IDE setup (either mode)

- **IntelliJ** — `File → Project Structure → SDKs → + → Download JDK 27` (any
  vendor: Temurin, Zulu, Corretto, Oracle); Maven is bundled; run the main
  class `ListingsApplication` or `mvn spring-boot:run`.
- **VS Code** — Extensions: "Extension Pack for Java" (includes Red Hat Java,
  can download a JDK); "Maven for Java". Point the Java extension at a JDK 27
  (e.g. `java.jdt.ls.java.home`).
- **Either IDE** — `File → Open` on the folder containing `pom.xml`, then
  `Build → Build Project` (IntelliJ) or Maven panel → `life-cycle: compile`
  (VS Code) to load the project.

## Run

You can run the app two ways. **Pick one per session; don't run both at once**
(both would fight over the same PostgreSQL and you will get `5xx` from the
loser).

### Mode 1 — Docker Compose [recommended]

```bash
cd <this folder>   # springboot/
docker compose up -d --build        # build the image, start API + PostgreSQL
docker compose ps                   # wait for "healthy" / "Up" on both
curl -fsS http://localhost:8080/actuator/health
```

- On the **host**, the API lives at <http://localhost:8080> and PostgreSQL is
  exposed on <http://localhost:5435> (db/user/pass `listings`).
- The `Dockerfile` does the Java build (JDK 27) and ships a slim JRE 27 image —
  no host Java needed.
- The schema is created by **Liquibase** at startup (`v1.0`, then the additive
  `v1.1-v2-fields.yaml` you add in Step 5).
- Stop: `docker compose down`. Wipe data: `docker compose down -v`.
- Tail logs: `docker compose logs -f listings-api`.

### Mode 2 — Maven on the host

```bash
cd <this folder>   # springboot/
docker compose up -d listings-postgres      # start only the DB if not already up
mvn spring-boot:run                         # run the app in your terminal
curl -fsS http://localhost:8080/actuator/health
```

- Requires JDK 27 **+** Maven 3.9+ on the host (see Install — mode 2).
- The app reads the connection values from `application.yml`
  (`jdbc:postgresql://localhost:5435/listings`, user/pass `listings`) or
  overrides from `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASS`.

### URLs available at startup

| URL | What it is |
|---|---|
| <http://localhost:8080/actuator/health> | health check (JSON) |
| <http://localhost:8080/actuator/info> | application info |
| <http://localhost:8080/actuator/metrics> | list of exposed metrics |
| <http://localhost:8080/v3/api-docs> | OpenAPI document (JSON) |
| <http://localhost:8080/swagger-ui/index.html> | Swagger UI (interactive explorer) |
| <http://localhost:8080/swagger-ui.html> | redirect to the Swagger UI above |

> The business endpoints (`/auth/*`, `/listings*`) exist in the skeleton but
> are **not implemented yet** (calling them returns a 500 with a
> `Not implemented yet` message) - implement them following
> [exercise.md](exercise.md).

> Authentication is **HTTP Basic** (no login form, no session) - see
> [exercise.md](exercise.md), Step 1.

## Build & test

```bash
# make sure a Postgres is reachable on localhost:5435 (e.g. the base compose),
# then:
mvn clean verify
```

The provided `ListingsSmokeTest` boots the full context, so it needs that
Postgres. Writing more tests is optional (see exercise.md, Appendix C).

## Tree

```
<project>/                                  # this folder
├── README.md                           # this file — how to install / run / build
├── exercise.md                         # the statement (Steps 1-10)
├── pom.xml                             # Maven build (Boot 4.1.1, Java 27, PostgreSQL)
├── Dockerfile                          # 3-stage build: JDK 27 + Maven -> JRE 27
├── docker-compose.yml                  # THE BASE for the whole lab: API (8080) +
│                                       #   PostgreSQL (5435) on the shared `middleware`
│                                       #   network (follow-up TPs attach to that same
│                                       #   network; see ../keycloak, ../monitoring)
├── .dockerignore                       # build context exclusions (no target/, .git, *.db)
├── .gitignore
│
├── src/main/java/com/app/listings
│   ├── ListingsApplication.java        # entry point
│   ├── application/
│   │   └── service/                    # application ports (interface) + implementation
│   │       ├── UserService.java
│   │       ├── UserServiceImpl.java
│   │       ├── ListingService.java
│   │       └── ListingServiceImpl.java
│   ├── config/
│   │   ├── ApiVersioningConfig.java    # API versioning - Step 6a
│   │   └── SecurityConfig.java         # HTTP Basic - provided, refined in Steps 2-3
│   ├── domain/
│   │   ├── Category.java               # enum (9 values)
│   │   ├── Role.java                   # enum (SELLER)
│   │   ├── entity/
│   │   │   ├── User.java               # table `app_user` - provided (identity fields)
│   │   │   └── Listing.java            # table `listing` (id only) - Steps 2, 5
│   │   ├── exception/
│   │   │   └── EmailAlreadyExistsException.java  # provided (409)
│   │   └── repository/
│   │       ├── UserRepository.java     # provided
│   │       └── ListingRepository.java  # Step 2
│   └── web/
│       ├── controller/
│       │   ├── AuthController.java     # /auth/register, /auth/me - Step 1
│       │   └── ListingController.java  # /listings - Steps 2-3, split in Step 6a
│       ├── dto/
│       │   ├── UserRequestDto.java     # provided
│       │   ├── UserResponseDto.java    # provided
│       │   ├── ListingRequestDto.java  # Step 2
│       │   └── ListingResponseDto.java # Step 2
│       └── advice/
│           └── GlobalExceptionHandler.java  # 409/422/500 provided, rest to add
│
├── src/main/resources
│   ├── application.yml                 # main configuration (PostgreSQL, actuator,
│   │                                   #   jackson; every DB_* is env-overridable)
│   ├── application-dev.yml
│   └── db/changelog/
│       ├── db.changelog-master.yaml    # orchestrates the migrations
│       ├── v1.0-init.yaml              # initial schema (ids only) - never edit
│       └── v1.0.1-identity.yaml        # identity columns on app_user - never edit
│
└── src/test/java/com/app/listings
    └── ListingsSmokeTest.java          # context test (starting point; needs a
                                        #   Postgres reachable on localhost:5435)
```

## Schema migrations (Liquibase)

The **PostgreSQL** schema is versioned by **Liquibase**, not by Hibernate
(`spring.jpa.hibernate.ddl-auto` is `none`). This means **the schema is code**:
every change you make to the database lives in a YAML **changelog** that is
applied at startup, in order, and recorded.

### Why

- The schema **evolves** through the exercise (Steps 2 and 5 add columns).
  Liquibase lets you change it **without dropping/rebuilding Postgres** — it
  only applies the changes it has not run yet.
- Every change is auditable in `DATABASECHANGELOG` (a table Liquibase creates
  inside PostgreSQL) so you can see exactly what ran.
- It works the same for any database engine the JDBC URL points at.

> The tables are named `app_user` and `listing`. `app_user` (rather than
> `user`) is deliberate: `user` is a reserved keyword in PostgreSQL.

### How it's wired in this project

```
src/main/resources/db/changelog/
├── db.changelog-master.yaml    # the entry-point: includes the other files
├── v1.0-init.yaml              # provided: creates app_user.id + listing.id
├── v1.0.1-identity.yaml        # provided: identity columns on app_user
├── v1.0.2-listing.yaml         # you, Step 2: V1 columns on listing
└── v1.1-v2-fields.yaml         # you, Step 5: V2 columns on listing
```

`db.changelog-master.yaml` is a list of `include` statements, applied in order:

```yaml
databaseChangeLog:
  - include:
      file: classpath:db/changelog/v1.0-init.yaml
  - include:
      file: classpath:db/changelog/v1.0.1-identity.yaml
  # Step 2:
  # - include:
  #     file: classpath:db/changelog/v1.0.2-listing.yaml
  # Step 5:
  # - include:
  #     file: classpath:db/changelog/v1.1-v2-fields.yaml
```

Each migration file is made of **changeSets** tagged with a unique
`id`/`author`. When a changeSet runs for the first time, Liquibase
inserts it into `DATABASECHANGELOG`; on later starts it is **skipped** —
that's how you get "run once per changeSet" semantics.

A changeSet that **adds a column** (used in Step 5):

```yaml
- changeSet:
    id: v1.1-1
    author: tp
    changes:
      - addColumn:
          tableName: listing
          columns:
            - column:
                name: negotiable
                type: BOOLEAN
```

Which columns go in which file is defined by exercise.md (Steps 2 and 5).

> **Rule of the exercise**: never edit an already-published changeSet — add
> a new one instead.

> **Tip to inspect the database** (Postgres is on host port 5435):
>
> ```bash
> docker compose exec listings-postgres psql -U listings -d listings -c "\dt"
> docker compose exec listings-postgres psql -U listings -d listings -c "\d app_user"
> ```

---

## Optional dependency edits (bonus steps)

The skeleton's `pom.xml` is complete for the core. The only addition the
exercise prescribes is for **Step 4** (XML, bonus) — add it to `<dependencies>`
(what/why: exercise.md, Appendix B):

```xml
<dependency>
    <groupId>tools.jackson.dataformat</groupId>
    <artifactId>jackson-dataformat-xml</artifactId>
    <!-- version managed by the Spring Boot BOM -->
</dependency>
```

Then rebuild: `docker compose up -d --build`.

## To implement (reminder - see exercise.md)

Step numbers are the ones of exercise.md.

**Core** (~5h30-6h, in this order)
- **Step 1** — User accounts (wire `/auth/register`, `/auth/me`)
- **Step 2** — Listings: create and browse (V1 columns, POST, public GET, pagination, filters)
- **Step 3** — Security rules: ownership (403) and lifecycle (`active`)
- **Step 5** — Schema evolution: changelog `v1.1` (negotiable + address + GPS)
- **Step 6a** — Versioning: `?version=`, default V1, 400, Deprecation/Sunset
- **Step 6b** — V2 contract: owner-only fields, two OpenAPI specs
- **Step 8** — Metrics (`listings.created`)

**Bonus** (start only when the core is green)
- **Step 4** — Content negotiation JSON + XML
- **Step 7** — GPS enrichment (Nominatim, `RestClient`, 3 s fallback)
- **Step 9** — Distance search (V2)

**Delivery** — **Step 10**, final checks.

> **Technical note**: the database is **PostgreSQL**. The referential
> integrity between `listing` and `app_user` is carried by a JPA relation
> (`@ManyToOne` on the entity); you may also add a database-level foreign key
> in a Liquibase changeSet if you want it enforced at the DB level. Keep all
> migrations ADDITIVE.

## Notes on Spring Boot 4

- Boot **4.1.x** is used; **Liquibase** is the schema manager.
