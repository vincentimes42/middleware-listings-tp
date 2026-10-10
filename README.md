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

You need an **OpenJDK 27** build (the skeleton is compiled against Java 27) and
**Maven 3.9+**, or the IDE equivalents that bundle both. No Docker required.

> "OpenJDK" is the open-source reference implementation of the Java platform —
> every JDK you'll find in a distro repo or via SDKMAN! is an OpenJDK build,
> just packaged by a different party (Temurin, Zulu, Corretto, Oracle OpenJDK,
> Microsoft, ...). All behave identically for this exercise.

Pick the setup that fits your workflow:

### Option A — Native Linux (command line / Maven CLI)

```bash
# 1) Maven (Debian/Ubuntu)
sudo apt update
sudo apt install -y maven

# 2) OpenJDK 27 — first try your distro's package:
sudo apt install -y openjdk-27-jdk           # Debian / Ubuntu
# sudo dnf   install -y java-openjdk-devel   # Fedora / RHEL (version depends on release)
# sudo pacman -S jdk-openjdk                 # Arch

# 3) If 27 is not yet in your distro repos, install it via SDKMAN!
#    (all the 27.x entries listed below are OpenJDK builds):
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk list java                                # look for the 27.x entries
sdk install java 27.0.1-tem                  # example — Temurin OpenJDK 27
                                             # (or -zulu / -amzn / -oracle / -lib,
                                             #  any of them are OpenJDK)

# 4) sanity
java -version                                # expect 27.x
mvn -version                                 # expect 3.9+
```

> **Tip**: pick **one** source for the JDK (distro **or** SDKMAN!) and stick
> with it — do not mix them on the same machine. Set `JAVA_HOME` accordingly:
>
> ```bash
> # distro (deb):
> JAVA_HOME=/usr/lib/jvm/java-27-openjdk-amd64
> # SDKMAN!:
> JAVA_HOME=$HOME/.sdkman/candidates/java/current
> ```

### Option B — IntelliJ IDEA (JDK + Maven bundled)

1. **OpenJDK 27**: `File → Project Structure → SDKs → + → Download JDK…`,
   then pick **27** and any vendor you like (Temurin, Corretto, Oracle OpenJDK,
   Zulu, ...) — they are all OpenJDK builds.
   Alternatively, choose **Add JDK…** and point IntelliJ at the local
   `openjdk-27-jdk` install (e.g. `/usr/lib/jvm/java-27-openjdk-amd64`).
2. **Maven**: bundled — `Settings → Build, Execution, Deployment → Build
   Tools → Maven` (the `Maven home path` already points at the IDEA copy; no
   install needed).
3. **Import**: `File → Open` and select the folder containing `pom.xml`.
4. **Run**: the `spring-boot:run` goal is available from the Maven tool window
   (right pane → `Plugins → spring-boot → run`), or use the `src/main/java`
   green ▶.

### Option C — VS Code (Red Hat Java + Maven extensions)

Open the project folder and VS Code will suggest:

- **Extension Pack for Java** (includes the Red Hat Java extension; it can
  download a JDK for you), and
- **Maven for Java** (Maven panel on the left).

Point the Java extension at your **local OpenJDK 27** (install it with Option A
first, or let the extension pick one for you). In `.vscode/settings.json`
(or your user settings):

```json
{
  "java.jdt.ls.java.home": "/usr/lib/jvm/java-27-openjdk-amd64",
  "java.configuration.runtimes": [
    { "name": "JavaSE-27", "path": "/usr/lib/jvm/java-27-openjdk-amd64", "default": true }
  ]
}
```

If Maven is not installed natively, either install it (Option A, Maven only) or
let the extension prompt you to download it.

> **Check** the IDE is using an OpenJDK 27 build — in IntelliJ: `Help → Find
> Action → Switch JDK`; in VS Code: the status bar (bottom-right) shows the
> active Java language level.

## Run

```bash
mvn spring-boot:run
```

The application starts on <http://localhost:8080>. It uses a local SQLite
database (`listings.db`) whose schema is created by **Liquibase** (`v1.0`).

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
mvn clean verify
```

## Tree

```
src/main/java/com/app/listings
├── ListingsApplication.java        # entry point
├── application/
│   └── service/                    # application ports (interface) + implementation
│       ├── UserService.java
│       ├── UserServiceImpl.java
│       ├── ListingService.java
│       └── ListingServiceImpl.java
├── config/
│   ├── ApiVersioningConfig         # API versioning
│   └── SecurityConfig              # HTTP Basic (no session)
├── domain/
│   ├── Category.java               # enum (9 values)
│   ├── Role.java                   # enum (SELLER)
│   ├── entity/
│   │   ├── User.java               # minimal skeleton (id)
│   │   └── Listing.java            # minimal skeleton (id)
│   └── repository/
│       ├── UserRepository.java
│       └── ListingRepository.java
└── web/
    ├── controller/
    │   ├── AuthController.java     # /auth/register, /auth/me (Basic auth)
    │   └── ListingController.java
    ├── dto/
    │   ├── ListingRequestDto.java  # to be completed
    │   └── ListingResponseDto.java # to be completed
    └── advice/
        └── GlobalExceptionHandler.java

src/main/resources
├── application.yml                 # main configuration (dev profile)
├── application-dev.yml
└── db/changelog/
    ├── db.changelog-master.yaml    # orchestrates the migrations
    └── v1.0-init.yaml              # initial schema (id) - to be completed (v1.1, v1.2)

src/test/java/com/app/listings
└── ListingsSmokeTest.java          # context test (starting point)
```

## Schema migrations (Liquibase)

The SQLite file `listings.db` is versioned by **Liquibase**, not by Hibernate
(`spring.jpa.hibernate.ddl-auto` is `none`). This means **the schema is code**:
every change you make to the database lives in a YAML **changelog** that is
applied at startup, in order, and recorded.

### Why

- The schema **evolves** through the exercise (Steps 1, 2, 5 add columns).
  Liquibase lets you change it **without deleting `listings.db`** — it only
  applies changes it has not run yet.
- Every change is auditable in `DATABASECHANGELOG` (a table Liquibase creates
  inside the SQLite file) so you can see exactly what ran.
- It works the same for any database (PostgreSQL, MySQL, …) — the only thing
  you should not do here is change the engine.

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
  # Step 5: add these two lines here (in order):
  # - include:
  #     file: classpath:db/changelog/v1.1-negotiable.yaml
  # - include:
  #     file: classpath:db/changelog/v1.2-geo.yaml
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

---

## To implement (reminder - see exercise.md)

**Core** (must fit the 8h session)
1. **User accounts** (register, HTTP Basic auth, `/auth/me`)
2. **Business endpoints** (POST / GET / PUT / DELETE `/listings`, ownership rules)
3. **Security rules** (public read, SELLER write, owner-only, actuator public)
4. **Schema evolution**: changelogs `v1.1` (negotiable) + `v1.2` (address/GPS)
5. **Versioning** V1 + V2 (native Spring, header + param, fallback; private-field
   visibility in V2; two OpenAPI specs)
6. **GPS enrichment**: Nominatim consumption (`RestClient`) + 3 s fallback
7. **Metrics & logging** (`listings.created`, `geo.enrich_failed`)

**Bonus** (start only when the core is green)
8. **Content negotiation** JSON + XML (add `jackson-dataformat-xml`, see exercise.md Appendix B)
9. **Distance search** (V2)
10. **Hexagonal architecture** (+ ArchUnit test)

> **Technical note**: SQLite + Liquibase have a few limitations (no FK
> support managed, `AUTOINCREMENT` limited to `INTEGER PRIMARY KEY`,
> no `remark`). The referential integrity between `listing` and `user`
> should be added via a JPA relation (`@ManyToOne`) and, if appropriate,
> a database-level constraint. This choice is deliberate to keep the
> skeleton portable and focused on the interface contract.

## Notes on Spring Boot 4

- Boot **4.1.x** is used; **Liquibase** is the schema manager.
- Spring Boot 4 introduced **new web testing mechanisms**
  (see `org.springframework.boot.test.web` / `test.http`); adapt your
  endpoint testing approach accordingly (WebTestClient, localTestWebServer, etc.).
