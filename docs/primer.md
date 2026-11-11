# TP1 primer — Maven, Spring Boot, tests

Background for [`../exercise.md`](../exercise.md): the two tools of the TP,
the reference documentation at the exact versions of this project, and how to
test if you choose to. Read it once before Step 1.

## The two tools

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

## Testing strategy (optional)

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
