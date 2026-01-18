FROM ubuntu:latest
LABEL authors="coleg"

ENTRYPOINT ["top", "-b"]

# Stage 1: Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy the pom.xml and source code
COPY pom.xml .
COPY src ./src

# Build the application and skip tests for faster deployment
RUN mvn clean package -DskipTests

# Stage 2: Runtime stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Create the external-images directory in the container
RUN mkdir -p /app/external-images

# Copy the jar from the build stage
COPY --from=build /app/target/cgsWeb-0.0.1-SNAPSHOT.jar app.jar

# Expose the port Spring Boot runs on
EXPOSE 8080

# Run the application
# We use -Djava.security.egd to speed up Tomcat startup in container environments
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]