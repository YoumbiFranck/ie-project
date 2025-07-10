# Container starten / Start containers
docker compose up -d


# Start the application (wenn Änderungen an der Anwendung vorgenommen wurden) / Start the application (if changes were made to the application)
docker compose build app --no-cache

docker compose build app 

# Container-Status überprüfen / Check container status
docker compose ps

# Logs anzeigen / View logs
docker compose logs -f

# Spezifische Service-Logs / Specific service logs
docker compose logs -f app
docker compose logs -f camunda
docker compose logs -f mysql

# Container stoppen / Stop containers
docker compose down

# Container stoppen und Volumes löschen / Stop containers and remove volumes
docker compose down -v

# Anwendung neu bauen / Rebuild application
docker compose build app

# Alles neu starten / Restart everything
docker compose down && docker compose up -d

# In Container einloggen / Login to container
docker exec -it ie_project_app /bin/bash
docker exec -it camunda_platform /bin/bash
docker exec -it camunda_mysql /bin/bash


# Zugang

Deine App: http://localhost:8080
Camunda Platform: http://localhost:8090
phpMyAdmin: http://localhost:8082 
MySQL: localhost:3306


---

# Nettoyer les anciens conteneurs
docker compose down -v

# Construire et démarrer
docker compose up --build

# Ou pour construire sans cache
docker-compose build --no-cache
docker-compose up