# =====================================================================
#  TP1 — listings micro-service
#  Multi-stage: build with JDK 27 + Maven -> run on slim JDK 27 JRE.
#  No tests are baked into the image; run `mvn clean verify` on the host.
# =====================================================================

# ---------- Stage 1: deps (JDK 27 + Maven from Docker Hub) ----------
FROM eclipse-temurin:27-jdk AS deps
WORKDIR /build
COPY pom.xml ./
# Fetch dependencies up-front; Maven is available in the image via apt.
RUN apt-get update && apt-get install -y --no-install-recommends maven \
  && rm -rf /var/lib/apt/lists/* \
  && mvn -B -ntp dependency:go-offline -Dmaven.test.skip=true || true

# ---------- Stage 2: build (JDK 27) ----------
FROM deps AS build
COPY src ./src
RUN mvn -B -ntp clean package -DskipTests

# ---------- Stage 3: run ----------
FROM eclipse-temurin:27-jre
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
