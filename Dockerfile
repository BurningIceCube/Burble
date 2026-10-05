# syntax=docker/dockerfile:1

##### Stage 1: Build the orchestrator bootJar with Gradle #####
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

# Copy Gradle wrapper and build files first to leverage Docker layer caching
COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle settings.gradle ./
COPY shared/build.gradle shared/build.gradle
COPY ontology/build.gradle ontology/build.gradle
COPY story/build.gradle story/build.gradle
COPY state/build.gradle state/build.gradle
COPY game/build.gradle game/build.gradle
COPY forum/build.gradle forum/build.gradle
COPY orchestrator/build.gradle orchestrator/build.gradle

RUN chmod +x gradlew

# Now copy the rest of the sources and build the bootable jar
COPY shared shared
COPY ontology ontology
COPY story story
COPY state state
COPY game game
COPY forum forum
COPY orchestrator orchestrator

RUN ./gradlew :orchestrator:bootJar --no-daemon -x test

##### Stage 2: Slim runtime image #####
FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app

RUN useradd --create-home --shell /bin/bash burble
USER burble

COPY --from=build /workspace/orchestrator/build/libs/orchestrator-*.jar /app/burble.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/burble.jar"]
