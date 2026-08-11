#!/usr/bin/env bash
set -euo pipefail

# scripts/bench-ab.sh
#
# Bloque B.3 (Integrante B - Frontend + mediciones):
# 1. Hace login contra /api/auth/login y captura la cookie JWT (sged_access).
# 2. Corre Apache Bench (ab) contra un endpoint autenticado usando esa cookie.
# 3. Guarda la salida cruda de ab + un resumen parseado (P50/P95/P99, req/s,
#    %error) en docs/mediciones/perf/ab/.
#
# Requiere: curl, ab (Apache Bench). En Windows: Git Bash + ab.exe de
# Apache Haus, o correr este script dentro de WSL con
# `sudo apt install apache2-utils`.
#
# Uso:
#   ./scripts/bench-ab.sh
#   BASE_URL=http://localhost:8080 SGED_USER=admin SGED_PASS='Admin2026!' \
#     N=1000 C=50 ./scripts/bench-ab.sh

BASE_URL="${BASE_URL:-http://localhost:8080}"
SGED_USER="${SGED_USER:-admin}"
SGED_PASS="${SGED_PASS:-Admin2026!}"
N="${N:-1000}"
C="${C:-50}"
TARGET_PATH="${TARGET_PATH:-/api/estudiantes?page=0&size=10}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
OUT_DIR="$REPO_ROOT/docs/mediciones/perf/ab"
mkdir -p "$OUT_DIR"

TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
COOKIE_JAR="$(mktemp)"
LOGIN_RESPONSE_FILE="$(mktemp)"
RAW_OUT="$OUT_DIR/ab-raw-$TIMESTAMP.txt"
SUMMARY_OUT="$OUT_DIR/ab-resumen-$TIMESTAMP.md"

cleanup() { rm -f "$COOKIE_JAR" "$LOGIN_RESPONSE_FILE"; }
trap cleanup EXIT

command -v curl >/dev/null || { echo "ERROR: curl no esta instalado." >&2; exit 1; }
command -v ab   >/dev/null || { echo "ERROR: ab (Apache Bench) no esta instalado o no esta en PATH." >&2; exit 1; }

echo "==> 1) Login contra $BASE_URL/api/auth/login como '$SGED_USER'..."
HTTP_STATUS=$(curl -s -o "$LOGIN_RESPONSE_FILE" -w "%{http_code}" \
  -c "$COOKIE_JAR" \
  -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$SGED_USER\",\"password\":\"$SGED_PASS\"}") || {
    echo "ERROR: curl no pudo conectar a $BASE_URL. Revisa que el backend este corriendo y que BASE_URL sea correcto (echo \$BASE_URL)." >&2
    exit 1
  }

if [ "$HTTP_STATUS" != "200" ]; then
  echo "ERROR: login fallo (HTTP $HTTP_STATUS). Respuesta:" >&2
  cat "$LOGIN_RESPONSE_FILE" >&2
  exit 1
fi

# El cookie jar de curl usa formato Netscape: la 7ma columna es el valor.
ACCESS_TOKEN=$(awk -F'\t' '$6 == "sged_access" { print $7 }' "$COOKIE_JAR")

if [ -z "$ACCESS_TOKEN" ]; then
  echo "ERROR: no se encontro la cookie sged_access en la respuesta de login." >&2
  echo "Contenido del cookie jar:" >&2
  cat "$COOKIE_JAR" >&2
  exit 1
fi

echo "==> 2) Cookie JWT capturada (sged_access), corriendo ab -n $N -c $C..."
ab -n "$N" -c "$C" \
  -H "Cookie: sged_access=$ACCESS_TOKEN" \
  "$BASE_URL$TARGET_PATH" | tee "$RAW_OUT"

echo "==> 3) Parseando resultados a $SUMMARY_OUT..."

REQ_PER_SEC=$(grep "Requests per second:" "$RAW_OUT" | awk '{print $4}')
TIME_PER_REQ=$(grep "Time per request:" "$RAW_OUT" | head -1 | awk '{print $4}')
FAILED=$(grep "Failed requests:" "$RAW_OUT" | awk '{print $3}')
COMPLETE=$(grep "Complete requests:" "$RAW_OUT" | awk '{print $3}')
P50=$(grep " 50% " "$RAW_OUT" | awk '{print $2}')
P95=$(grep " 95% " "$RAW_OUT" | awk '{print $2}')
P99=$(grep " 99% " "$RAW_OUT" | awk '{print $2}')

if [ -n "${COMPLETE:-}" ] && [ "$COMPLETE" -gt 0 ] 2>/dev/null; then
  PCT_ERROR=$(awk -v f="${FAILED:-0}" -v c="$COMPLETE" 'BEGIN { printf "%.2f", (f/c)*100 }')
else
  PCT_ERROR="N/A"
fi

cat > "$SUMMARY_OUT" <<EOF
# Resultado Apache Bench - $TIMESTAMP

- **Endpoint**: \`GET $TARGET_PATH\`
- **Concurrencia (-c)**: $C
- **Peticiones totales (-n)**: $N
- **Autenticacion**: cookie \`sged_access\` (JWT obtenido via /api/auth/login)

## Metricas

| Metrica | Valor |
|---|---|
| Requests completados | $COMPLETE |
| Requests fallidos | $FAILED |
| % error | $PCT_ERROR % |
| Requests/seg | $REQ_PER_SEC |
| Tiempo promedio por request (ms) | $TIME_PER_REQ |
| P50 (ms) | $P50 |
| P95 (ms) | $P95 |
| P99 (ms) | $P99 |

Salida cruda completa: \`$(basename "$RAW_OUT")\`
EOF

echo "==> Listo. Archivos generados:"
echo "    - $RAW_OUT"
echo "    - $SUMMARY_OUT"