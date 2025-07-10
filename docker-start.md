# Container starten / Start containers
docker compose down -v
docker compose up -d

docker compose up --build -d
# Start the application (wenn Änderungen an der Anwendung vorgenommen wurden) / Start the application (if changes were made to the application)
docker compose build app --no-cache

docker compose build app 

docker compose restart app

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



# Commandes pour développement :
# Démarrer en mode développement
docker compose -f docker-compose.dev.yml up --build -d

# Arrêter le développement
docker compose -f docker-compose.dev.yml down

# 3. Commandes pour production :
# Démarrer en mode production
docker compose up --build

# Arrêter la production
docker compose down

# Modifications qui nécessitent un redémarrage de l'app (1 commande) :
🔄 Redémarrage app seulement pour :

- Nouveaux fichiers BPMN
- Modifications dans les fichiers BPMN existants
- Nouvelles classes Java
- Changements dans les annotations Spring


docker compose -f docker-compose.dev.yml restart app


---
#


# Nettoyage complet
docker compose down --rmi all --volumes


# Rebuild sans cache
docker compose build --no-cache app

# Démarrage
docker compose up -d


docker compose logs app | grep -i mysql

docker compose logs app | grep -i "deploying\|bpmn"

docker compose logs app --tail=100