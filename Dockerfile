
FROM maven:3.9.4-eclipse-temurin-17


WORKDIR /workspace


RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*


EXPOSE 8080


HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1


CMD ["mvn", "spring-boot:run"]
