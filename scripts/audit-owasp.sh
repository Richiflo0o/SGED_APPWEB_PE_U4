#!/usr/bin/env bash
# Auditoría automática de controles OWASP (A01-A07, A09, XSS) (Bloque C.2 + U4).
# Guarda la evidencia cruda en docs/mediciones/sec/ con fecha y commit.
set -uo pipefail

BASE="${BASE_URL:-http://localhost:8080}"
OUT="docs/mediciones/sec"
mkdir -p "$OUT"
FECHA=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
COMMIT=$(git rev-parse --short HEAD 2>/dev/null || echo "sin-git")

cabecera() {
  echo "# Evidencia OWASP $1"
  echo "# Fecha: $FECHA | Commit: $COMMIT | curl: $(curl --version | head -1)"
  echo "#"
}

# El control A07 de este mismo script agota a proposito los intentos de login.
# El contador de LoginAttemptService se lleva por IP de origen
# (login_attempts:<ip>), no por usuario: seis fallos dejan bloqueado durante 15
# minutos a *cualquier* cuenta que venga de ese equipo, incluida la de admin.
# Sin limpiarlo, una segunda corrida de la auditoria no logra autenticarse y
# A01 devuelve 401 en todo. Ese resultado parece correcto y no lo es: probaria
# que hace falta autenticacion, no que se respetan los roles.
docker exec sged_redis sh -c \
  'redis-cli --scan --pattern "login_attempts:*" | xargs -r redis-cli DEL' \
  > /dev/null 2>&1 \
  || echo "AVISO: no se pudo limpiar el contador de intentos; si A01 sale 401, esa es la causa."

echo "== A01: control de acceso (usuario sin rol pide recurso de admin -> 403) =="
# 0. /api/auth/registro exige rol ADMINISTRADOR (Bloque A.1): iniciamos
#    sesion con el admin sembrado para poder registrar la cuenta de prueba.
curl -s -c /tmp/sged_admin.jar -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin2026!"}' > /dev/null
# 1. el admin registra un usuario basico (rol USER por defecto)
curl -s -b /tmp/sged_admin.jar -X POST "$BASE/api/auth/registro" \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Audit","apellido":"A01","cedula":"0912345678","correo":"audit.a01@sged.test","fechaNacimiento":"2000-01-01","username":"audit_a01@sged.test","password":"Passw0rd!"}' > /dev/null
# 2. el usuario basico inicia su propia sesion
curl -s -c /tmp/sged_a01.jar -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"audit_a01@sged.test","password":"Passw0rd!"}' > /dev/null
{ cabecera "A01 - Broken Access Control";
  echo "-- 1. operacion administrativa sobre estudiantes (esperado 403) --";
  # El cuerpo debe ser un id numerico: la reestructuracion cambio la categoria
  # de texto libre (?categoria=SUB-12) a clave foranea. Con el cuerpo mal
  # formado la peticion moria antes de llegar al control de acceso.
  curl --include -s -b /tmp/sged_a01.jar -X POST \
    "$BASE/api/estudiantes/operaciones/desactivar-categoria" \
    -H "Content-Type: application/json" -d '1';
  echo "";
  echo "";
  # Los cinco recursos que agrego la reestructuracion no tenian @PreAuthorize:
  # con anyRequest().authenticated() bastaba una sesion valida de rol USER para
  # leer y modificar personas, categorias y entrenadores. Se comprueba recurso
  # por recurso para que la regresion no pueda repetirse sin que la auditoria
  # lo muestre.
  echo "-- 2. lectura de datos personales de terceros (esperado 403) --";
  curl -s -o /dev/null -w "GET  /api/personas                 -> %{http_code}\n" \
    -b /tmp/sged_a01.jar "$BASE/api/personas";
  curl -s -o /dev/null -w "GET  /api/personas/cedula/0000000000 -> %{http_code}\n" \
    -b /tmp/sged_a01.jar "$BASE/api/personas/cedula/0000000000";
  echo "";
  echo "-- 3. escritura sobre catalogos y cuentas (esperado 403) --";
  # Los cuerpos deben ser validos: @Valid se evalua al resolver el argumento,
  # antes que @PreAuthorize, asi que un payload invalido devuelve 422 y tapa
  # el resultado del control de acceso que se quiere evidenciar.
  curl -s -o /dev/null -w "POST   /api/categorias   -> %{http_code}\n" \
    -b /tmp/sged_a01.jar -X POST "$BASE/api/categorias" \
    -H "Content-Type: application/json" \
    -d '{"nombre":"AUDIT-A01","edadMin":10,"edadMax":12,"descripcion":"alta no autorizada"}';
  curl -s -o /dev/null -w "DELETE /api/categorias/1 -> %{http_code}\n" \
    -b /tmp/sged_a01.jar -X DELETE "$BASE/api/categorias/1";
  curl -s -o /dev/null -w "PUT    /api/personas/1   -> %{http_code}\n" \
    -b /tmp/sged_a01.jar -X PUT "$BASE/api/personas/1" \
    -H "Content-Type: application/json" \
    -d '{"nombre":"Alterado","apellido":"PorUsuario","cedula":"0999999999","correo":"alterado@sged.test","fechaNacimiento":"1990-01-01"}';
  curl -s -o /dev/null -w "GET    /api/usuarios     -> %{http_code}\n" \
    -b /tmp/sged_a01.jar "$BASE/api/usuarios";
  echo "";
  echo "-- 4. lectura permitida al rol USER (esperado 200: no se rompio el uso legitimo) --";
  curl -s -o /dev/null -w "GET /api/categorias/activas -> %{http_code}\n" \
    -b /tmp/sged_a01.jar "$BASE/api/categorias/activas";
  curl -s -o /dev/null -w "GET /api/estados_generales  -> %{http_code}\n" \
    -b /tmp/sged_a01.jar "$BASE/api/estados_generales"; \
} > "$OUT/a01-acceso-roto.txt"
echo "  -> $OUT/a01-acceso-roto.txt"
rm -f /tmp/sged_admin.jar

echo "== A02: criptografía en tránsito (TLS 1.3) =="
{ cabecera "A02 - Cryptographic Failures";
  curl -vk "https://localhost:8443/actuator/health" 2>&1 | grep -Ei "TLS|SSL|cipher|subject|HTTP" || \
  echo "NOTA: TLS expuesto por el proxy/nginx en despliegue; capturar contra el puerto 8443/443 del entorno con TLS."; \
} > "$OUT/a02-tls.txt"
echo "  -> $OUT/a02-tls.txt"

echo "== A03: inyección (payload ' OR '1'='1 -> 422 ProblemDetails) =="
{ cabecera "A03 - Injection";
  curl --include -s -b /tmp/sged_a01.jar -X POST "$BASE/api/estudiantes" \
    -H "Content-Type: application/json" \
    -d "{\"nombre\":\"' OR '1'='1\",\"apellido\":\"\",\"categoria\":\"' OR '1'='1\"}"; \
} > "$OUT/a03-inyeccion.txt"
echo "  -> $OUT/a03-inyeccion.txt"

# ============================================================
# U4: A04, XSS y A06 (la evidencia de A01 queda disponible en
# /tmp/sged_a01.jar; aqui se abre sesion de admin de nuevo para
# ejercer operaciones de escritura). Debe correr ANTES de A07,
# que agota a proposito el rate limit de la IP.
# ============================================================
echo "== A04: diseño inseguro (reglas de negocio/seguridad en el servidor) =="
# Se re-autentica el admin: A01 borro /tmp/sged_admin.jar.
curl -s -c /tmp/sged_admin.jar -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin2026!"}' > /dev/null
{ cabecera "A04 - Insecure Design";
  echo "-- 1. politica de contraseñas aplicada en el servidor (esperado 422) --";
  curl -s -o /dev/null -w "POST /api/auth/registro (password corta) -> %{http_code}\n" \
    -b /tmp/sged_admin.jar -X POST "$BASE/api/auth/registro" \
    -H "Content-Type: application/json" \
    -d '{"nombre":"D","apellido":"A04","cedula":"1234567890","correo":"a04.weak@sged.test","fechaNacimiento":"2000-01-01","username":"a04_weak@sged.test","password":"abc"}';
  echo "";
  echo "-- 2. regla de negocio: catalogos validos obligatorios (categoria inexistente -> 404) --";
  curl --include -s -b /tmp/sged_admin.jar -X POST "$BASE/api/estudiantes" \
    -H "Content-Type: application/json" \
    -d '{"idPersona":999,"idCategoria":99999,"idEstadoGeneral":1,"codigoEstudiante":"A04-001","fechaIngreso":"2024-09-02","peso":60.0,"altura":1.70}' | head -12;
  echo "";
  echo "-- 3. credenciales duplicadas rechazadas (esperado 409) --";
  curl -s -o /dev/null -w "POST /api/auth/registro (username ya usado) -> %{http_code}\n" \
    -b /tmp/sged_admin.jar -X POST "$BASE/api/auth/registro" \
    -H "Content-Type: application/json" \
    -d '{"nombre":"Dup","apellido":"A04","cedula":"1234567891","correo":"a04.dup@sged.test","fechaNacimiento":"2000-01-01","username":"audit_a01@sged.test","password":"Passw0rd!"}';
  echo "";
  echo "-- nota: el modelo de amenazas y las decisiones de diseño se documentan en docs/informe-u4 (seccion 5.4). --"; \
} > "$OUT/a04-diseno-inseguro.txt"
echo "  -> $OUT/a04-diseno-inseguro.txt"

echo "== XSS: defensa en profundidad (CSP + JSON + escape Angular) =="
{ cabecera "XSS - Cross-Site Scripting";
  echo "-- 1. cabecera CSP presente (default-src 'self'; frame-ancestors 'none') --";
  curl -I -s "$BASE/api/auth/ping" \
    | grep -iE "content-security-policy|x-frame-options|x-content-type-options";
  echo "";
  echo "-- 2. almacenamiento de payload (admin crea categoria con <script>) --";
  curl -s -o /dev/null -w "POST /api/categorias (payload XSS) -> %{http_code}\n" \
    -b /tmp/sged_admin.jar -X POST "$BASE/api/categorias" \
    -H "Content-Type: application/json" \
    -d '{"nombre":"<script>alert(1)</script>","edadMin":10,"edadMax":12,"descripcion":"payload xss"}';
  echo "";
  echo "-- 3. el payload se sirve como JSON (Content-Type application/json), no como HTML: no se interpreta --";
  curl -s -D /tmp/sged_xss_headers.txt -o /tmp/sged_xss_body.json \
    "$BASE/api/categorias/activas" -b /tmp/sged_admin.jar -H "Accept: application/json";
  grep -i "^content-type" /tmp/sged_xss_headers.txt;
  echo "  el payload viaja como dato escapable (Angular lo escapa por defecto en el SPA):";
  grep -o "<script>alert(1)</script>" /tmp/sged_xss_body.json | head -1; \
} > "$OUT/xss.txt"
echo "  -> $OUT/xss.txt"
rm -f /tmp/sged_admin.jar

echo "== A05: cabeceras de seguridad =="
{ cabecera "A05 - Security Misconfiguration";
  echo "-- via HTTP directo al backend ($BASE) --";
  curl -I -s "$BASE/api/auth/ping";
  echo "";
  echo "-- via HTTPS/nginx (HSTS solo aplica sobre conexion segura) --";
  curl -Ik -s "https://localhost:8443/api/auth/ping"; \
} > "$OUT/a05-cabeceras.txt"
echo "  -> $OUT/a05-cabeceras.txt"

echo "== A06: componentes vulnerables (inventario de versiones) =="
{ cabecera "A06 - Vulnerable and Outdated Components";
  echo "-- Spring Boot (parent) --";
  grep -A1 "spring-boot-starter-parent" backend/pom.xml | grep "<version>" | sed 's/[[:space:]]*<[^>]*>//g';
  echo "";
  echo "-- dependencias con version explicita (pom.xml) --";
  awk '
    /<artifactId>/ { a=$0; sub(/.*<artifactId>/,""); sub(/<\/artifactId>.*/,""); gsub(/[[:space:]]/,"",a) }
    /<version>/ { v=$0; sub(/.*<version>/,""); sub(/<\/version>.*/,""); gsub(/[[:space:]]/,"",v);
      if (a!="" && v!="") print "  " a " -> " v; a=""; v="" }
    /<\/dependency>/ { a=""; v="" }
  ' backend/pom.xml | head -20;
  echo "";
  echo "-- frontend (package.json) --";
  grep -E "\"(angular|@angular/[a-z-]+|rxjs|typescript|zone.js)\"" frontend/package.json;
  echo "";
  echo "-- nota: el escaneo OWASP dependency-check requiere descargar la base NVD;";
  echo "--   en el CI se documenta el inventario y se pinan digests de imagen (docker-compose.yml).";
  if [ -d frontend/node_modules ] && command -v npm > /dev/null 2>&1; then
    echo "";
    echo "-- npm audit (frontend) --";
    (cd frontend && npm audit --omit=dev --audit-level=high) 2>&1 | tail -20;
  else
    echo "-- frontend sin node_modules local: se omite npm audit en esta corrida. --";
  fi; \
} > "$OUT/a06-componentes.txt"
echo "  -> $OUT/a06-componentes.txt"

echo "== A07: 6 intentos fallidos -> 429 =="
{ cabecera "A07 - Identification and Authentication Failures";
  for i in 1 2 3 4 5 6; do
    code=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/api/auth/login" \
      -H "Content-Type: application/json" \
      -d '{"username":"admin","password":"incorrecta"}');
    echo "--- intento $i -> $code ---";
  done;
  echo "--- intento 7 (respuesta completa, confirma ProblemDetails en el 429) ---";
  curl --include -s -X POST "$BASE/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"incorrecta"}'; \
} > "$OUT/a07-rate-limit.txt"
echo "  -> $OUT/a07-rate-limit.txt"

echo "== A09: log de autenticación con ip, timestamp, sub =="
{ cabecera "A09 - Security Logging and Monitoring Failures";
  docker exec sged_backend sh -c 'grep -E "AUTH_LOGIN_(OK|FAIL)" logs/sged-auth.log | tail -20' 2>/dev/null || \
  grep -E "AUTH_LOGIN_(OK|FAIL)" backend/logs/sged-auth.log 2>/dev/null | tail -20 || \
  echo "Ejecutar tras algunos logins; el log vive en logs/sged-auth.log del backend."; \
} > "$OUT/a09-logging.txt"
echo "  -> $OUT/a09-logging.txt"

echo "Auditoría OWASP completada. Evidencia en $OUT/"
