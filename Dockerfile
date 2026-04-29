# =====================================================================
# Multi-stage build for the Flight Log Spring Boot app.
# =====================================================================

# ---- 1. Build stage --------------------------------------------------
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace

# Cache Gradle dependencies first so source-only changes don't refetch.
COPY gradle gradle
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
RUN ./gradlew --no-daemon -q help >/dev/null 2>&1 || true

COPY src src
RUN ./gradlew --no-daemon clean bootJar -x test

# ---- 2. Runtime stage ------------------------------------------------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Non-root user for OWASP / k8s best practice. UID 1001 because the
# Temurin base image already ships a user with UID 1000.
RUN useradd -r -u 1001 -m flightlog
USER flightlog

# Layered jar would be nicer but a single fat-jar is enough here.
COPY --from=builder /workspace/build/libs/*.jar /app/app.jar

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=10 \
    CMD wget -q -O- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar /app/app.jar"]
