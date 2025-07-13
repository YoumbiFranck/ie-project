# Image de base avec Maven + JDK
FROM maven:3.9.4-eclipse-temurin-17

# Répertoire de travail
WORKDIR /workspace

# Installer curl pour le healthcheck
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Expose le port
EXPOSE 8080

# Health check (optionnel, utile avec Docker Compose)
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Lancer l'application avec Spring Boot
CMD ["mvn", "spring-boot:run"]
