# Multi-stage build for Java application
# Première étape : Build de l'application / First stage: Build the application
FROM maven:3.9.4-eclipse-temurin-17 AS builder

# Arbeitsverzeichnis setzen / Set working directory
WORKDIR /app

# POM-Datei kopieren für bessere Docker-Layer-Caching / Copy POM file for better Docker layer caching
COPY pom.xml .

# Dependencies herunterladen / Download dependencies
RUN mvn dependency:go-offline -B

# Quellcode kopieren / Copy source code
COPY src ./src

# Anwendung bauen / Build application
RUN mvn clean package -DskipTests

# Zweite Stufe: Runtime-Image / Second stage: Runtime image
FROM eclipse-temurin:17-jre

# Installer curl pour les health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Arbeitsverzeichnis setzen / Set working directory
WORKDIR /app

# JAR-Datei aus Builder-Stage kopieren / Copy JAR file from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Port freigeben / Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Anwendung starten / Start application
CMD ["java", "-jar", "app.jar"]