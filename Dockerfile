# syntax=docker/dockerfile:1

# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Cache dependencies first.
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Build the application.
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Run as a non-root user.
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /build/target/*.jar app.jar

EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=docker

ENTRYPOINT ["java", "-jar", "app.jar"]