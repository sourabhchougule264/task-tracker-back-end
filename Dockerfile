# Multi-stage build for Task Tracker Application

# Stage 1: Build
FROM eclipse-temurin:17-jdk-alpine AS build
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

# Copy the latest built WAR artifact from build stage (version-agnostic)
COPY --from=build target/tasktrackerapp-*.war tasktrackerapp.war

# Expose port
EXPOSE 8080

# Run the application
CMD ["java", "-war", "tasktrackerapp.war"]
