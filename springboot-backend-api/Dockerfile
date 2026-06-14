# syntax=docker/dockerfile:1.7

# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp -q dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp -q -DskipTests package

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

RUN groupadd --system --gid 1001 app \
    && useradd --system --uid 1001 --gid 1001 --home /app app

COPY --from=build /workspace/target/flight-agent-backend-*.jar /app/app.jar
RUN chown -R app:app /app
USER app

EXPOSE 8000
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS=""

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=5 \
    CMD wget -q -O - "http://localhost:8000/api/airport-code?city=London" >/dev/null 2>&1 || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
