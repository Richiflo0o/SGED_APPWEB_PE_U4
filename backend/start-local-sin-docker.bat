@echo off
REM Ajusta estos 3 valores a tu instalacion:
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot
set DB_PASSWORD=
set DB_USER=postgres

REM Base de datos: Flyway crea el esquema solo al arrancar (V1..V6 en
REM backend/src/main/resources/db/migration/). Solo falta correr
REM db/seed.sql a mano UNA VEZ para los datos de ejemplo (admin, etc).
set DB_URL=jdbc:postgresql://localhost:5432/sged_db
set FLYWAY_ENABLED=false

REM Redis (Memurai o WSL corriendo en localhost:6379)
set REDIS_HOST=localhost
set REDIS_PORT=6379

REM JWT (application.yml los exige, sin default -> el backend no arranca sin esto)
set JWT_SECRET=SGED_2026_SECRET_KEY_MUY_LARGA_Y_SEGURA_123456789
set JWT_EXPIRATION_MS=3600000
set JWT_REFRESH_EXPIRATION_MS=604800000
set JWT_ISSUER=sged-backend
set JWT_AUDIENCE=sged-frontend

REM Cookies (sin HTTPS local, debe ser false o el navegador no las guarda)
set COOKIE_SECURE=false

REM Rate limiting login
set LOGIN_MAX_INTENTOS=5
set LOGIN_VENTANA_MINUTOS=15

REM Cache TTL
set CACHE_TTL_SECONDS=60

cd /d %~dp0
mvnw.cmd spring-boot:run