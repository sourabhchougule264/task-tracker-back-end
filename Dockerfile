# Multi-stage build for Task Tracker Application

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the built jar from build stage
COPY --from=build /app/target/*.war app.war

# Expose port
EXPOSE 8080

# Set environment variables
ENV SPRING_PROFILES_ACTIVE=prod
ENV DB_USERNAME=postgres
ENV DB_PASSWORD=password

# Run the application
ENTRYPOINT ["java", "-jar", "app.war"]
