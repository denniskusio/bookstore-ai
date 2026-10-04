# syntax=docker/dockerfile:1
#
# Multi-stage build for assistant-service. The build context is the repository root:
#   docker build -f services/assistant-service/Dockerfile -t bookstore/assistant-service:dev .

# ---- Stage 1: build the jar and split it into layers --------------------------------------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

# The whole multi-module build is needed (convention plugins, shared library). The Gradle cache
# mount keeps dependency downloads out of the image and makes repeated builds fast. sharing=locked lets
# parallel image builds (docker compose build) take turns: Gradle's cache locks do not work across containers.
COPY gradlew settings.gradle.kts gradle.properties ./
COPY gradle gradle
COPY build-logic build-logic
COPY libs libs
COPY services services
RUN --mount=type=cache,target=/root/.gradle,sharing=locked \
    ./gradlew :services:assistant-service:bootJar --no-daemon --console=plain

# Spring Boot's "tools" mode splits the fat jar into layers (dependencies change rarely, application
# code often), so rebuilding the image after a code change only re-copies a few KB.
RUN java -Djarmode=tools -jar services/assistant-service/build/libs/assistant-service.jar \
    extract --layers --destination /layers

# ---- Stage 2: slim runtime image -----------------------------------------------------------
# Alpine variant: about half the size of the Ubuntu-based JRE image, and BusyBox ships the wget that the
# Compose healthchecks use. (Kubernetes probes are done by the kubelet over HTTP and need no tool in the image.)
FROM eclipse-temurin:25-jre-alpine-3.24 AS runtime

# Run as an unprivileged user. The numeric UID matters: Kubernetes' runAsNonRoot can only
# verify a numeric user.
RUN addgroup -S -g 10001 app \
 && adduser -S -u 10001 -G app -H -s /sbin/nologin app
WORKDIR /app

# Least-changing layers first, so Docker reuses as many cached layers as possible.
COPY --from=build --chown=app:app /layers/dependencies/ ./
COPY --from=build --chown=app:app /layers/spring-boot-loader/ ./
COPY --from=build --chown=app:app /layers/snapshot-dependencies/ ./
COPY --from=build --chown=app:app /layers/application/ ./

USER 10001:10001
EXPOSE 8084
ENV SPRING_PROFILES_ACTIVE=docker

# Exec form, so the JVM is PID 1 and receives SIGTERM (graceful shutdown).
# MaxRAMPercentage sizes the heap relative to the container's memory limit;
# ExitOnOutOfMemoryError lets the orchestrator restart a JVM that is out of memory instead of keeping a zombie.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:+ExitOnOutOfMemoryError", "-jar", "assistant-service.jar"]
