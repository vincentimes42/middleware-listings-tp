# Spring Boot exercise - Skeleton

Classifieds **listings** micro-service base. It serves as the foundation for the
common exercise (interface contract, versioning, content negotiation).
See [`exercise.md`](exercise.md) for the full statement.

---

## Before you start

New to **Maven** or **Spring Boot**, or to the domain? Start with the two
sections at the top of [`exercise.md`](exercise.md):

- **"Before you start — the two tools you will use"** — a 2-minute mental
  model of Maven and Spring Boot.
- **"Domain & data model"** — the two actors (Visitor, Seller), the two
  entities (`User`, `Listing`), their 1-to-N ownership, and the listing
  lifecycle. Every step below builds on one of these ideas.

Everything else in this README is operational (install, run, tree).

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
Postgres (or your Testcontainers setup, see exercise.md). Unit tests don't.

## Tree

```
<project>/                                  # this folder
├── README.md                           # this file — how to install / run / build
├── exercise.md                         # the statement (Steps 1–11)
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
│   │   ├── ApiVersioningConfig         # API versioning
│   │   └── SecurityConfig              # HTTP Basic (no session)
│   ├── domain/
│   │   ├── Category.java               # enum (9 values)
│   │   ├── Role.java                   # enum (SELLER)
│   │   ├── entity/
│   │   │   ├── User.java               # maps to table `app_user` (id only, to complete)
│   │   │   └── Listing.java            # maps to table `listing` (id only, to complete)
│   │   └── repository/
│   │       ├── UserRepository.java
│   │       └── ListingRepository.java
│   └── web/
│       ├── controller/
│       │   ├── AuthController.java     # /auth/register, /auth/me (Basic auth)
│       │   └── ListingController.java
│       ├── dto/
│       │   ├── ListingRequestDto.java  # to be completed
│       │   └── ListingResponseDto.java # to be completed
│       └── advice/
│           └── GlobalExceptionHandler.java
│
├── src/main/resources
│   ├── application.yml                 # main configuration (PostgreSQL, actuator,
│   │                                   #   jackson; every DB_* is env-overridable)
│   ├── application-dev.yml
│   └── db/changelog/
│       ├── db.changelog-master.yaml    # orchestrates the migrations
│       └── v1.0-init.yaml              # initial schema (id only) - to be completed
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

- The schema **evolves** through the exercise (Steps 1, 2, 5 add columns).
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
└── v1.0-init.yaml              # already provided: creates user.id + listing.id
```

`db.changelog-master.yaml` is a list of `include` statements in order:

```yaml
databaseChangeLog:
  - include:
      file: classpath:db/changelog/v1.0-init.yaml
  # Step 5: add this line here:
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

You do the same for the Step 1/2 columns (email, username, passwordHash,
role, registrationDate on `user`; title, description, price, category,
creationDate, active, user_id on `listing`) — either by adding changeSets
to `v1.0-init.yaml` **before** you run the app for the first time, or by
creating new `v1.x-*.yaml` files. Both are correct; the difference is only
whether the schema is "already built" or "grows on the fly".

> **Rule of the exercise**: never edit an already-published changeSet — add
> a new one instead.

> **Tip to inspect the database** (Postgres is on host port 5435):
>
> ```bash
> docker compose exec listings-postgres psql -U listings -d listings -c "\dt"
> docker compose exec listings-postgres psql -U listings -d listings -c "\d app_user"
> ```

---

## To implement (reminder - see exercise.md)

**Core** (must fit the 8h session)
1. **User accounts** (register, HTTP Basic auth, `/auth/me`)
2. **Business endpoints** (POST / GET / PUT / DELETE `/listings`, ownership rules)
3. **Security rules** (public read, SELLER write, owner-only, actuator public)
4. **Schema evolution**: changelog `v1.1` (all new V2 columns: negotiable + address + GPS)
5. **Versioning** V1 + V2 (native Spring query-param `?version=`, default V1;
   private-field visibility in V2; two OpenAPI specs)
6. **Metrics** (`listings.created`)

**Bonus** (start only when the core is green)
7. **GPS enrichment**: Nominatim consumption (`RestClient`) + 3 s fallback
8. **Content negotiation** JSON + XML (add `jackson-dataformat-xml`, see exercise.md Appendix B)
9. **Distance search** (V2)

> **Technical note**: the database is **PostgreSQL**. The referential
> integrity between `listing` and `app_user` is carried by a JPA relation
> (`@ManyToOne` on the entity); you may also add a database-level foreign key
> in a Liquibase changeSet if you want it enforced at the DB level. Keep all
> migrations ADDITIVE.

## Notes on Spring Boot 4

- Boot **4.1.x** is used; **Liquibase** is the schema manager.
