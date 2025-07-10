# Camunda Projekt

## Container starten / Start containers
docker compose up -d

## Start the application (wenn Änderungen an der Anwendung vorgenommen wurden) / Start the application (if changes were made to the application)
docker compose build app --no-cache
docker compose build app

## Container-Status überprüfen / Check container status
docker compose ps

## Logs anzeigen / View logs
docker compose logs -f
