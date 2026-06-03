# ---- Build Stage ----
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Cache dependencies first
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Build the application
COPY src ./src
RUN mvn clean package -DskipTests -B

# ---- Runtime Stage ----
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# Create non-root user for security
RUN addgroup -S healthcare && adduser -S healthcare -G healthcare

# Copy JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Create logs directory
RUN mkdir -p logs && chown -R healthcare:healthcare /app

USER healthcare

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseG1GC", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
