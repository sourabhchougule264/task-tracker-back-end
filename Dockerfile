# Multi-stage build for Task Tracker Application

# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the built WAR artifact from build stage
COPY --from=build /app/target/tasktrackerapp-*.war tasktrackerapp.war

# Expose port
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "tasktrackerapp.war"]
