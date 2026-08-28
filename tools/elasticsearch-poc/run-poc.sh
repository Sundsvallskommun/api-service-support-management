#!/bin/bash
# Elasticsearch PoC — One-command setup
# Loads real production data, scales to 700k errands, reindexes into ES, runs measurements
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
COMPOSE_DIR="$SCRIPT_DIR"
COMPOSE="docker-compose"
command -v docker-compose >/dev/null 2>&1 || COMPOSE="docker compose"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m'

step() { echo -e "\n${CYAN}▶ $1${NC}"; }
ok()   { echo -e "${GREEN}  ✓ $1${NC}"; }
fail() { echo -e "${RED}  ✗ $1${NC}"; exit 1; }

wait_for_app() {
    local max_wait=${1:-180}
    local elapsed=0
    while [ $elapsed -lt $max_wait ]; do
        local code=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api-docs 2>/dev/null || echo "000")
        if [ "$code" = "200" ]; then return 0; fi
        sleep 5
        elapsed=$((elapsed + 5))
        echo -n "."
    done
    echo ""
    return 1
}

wait_for_mariadb() {
    local container="$1"
    local max_wait=${2:-90}
    local elapsed=0
    while [ $elapsed -lt $max_wait ]; do
        # Test actual SQL connection, not just ping (root user setup may lag behind ping)
        if docker exec "$container" mariadb -uroot -proot -e "SELECT 1" >/dev/null 2>&1; then return 0; fi
        sleep 3
        elapsed=$((elapsed + 3))
        echo -n "."
    done
    echo ""
    return 1
}

# --- Step 1: Build JAR ---
step "Building application JAR (skipping tests)..."
cd "$PROJECT_DIR"
mvn package -DskipTests -q -pl . 2>&1 | tail -5
if ! ls target/*.jar >/dev/null 2>&1; then
    fail "JAR not found in target/"
fi
ok "JAR built successfully"

# --- Step 2: Clean start ---
step "Stopping existing containers..."
cd "$COMPOSE_DIR"
$COMPOSE down -v 2>/dev/null || true
ok "Clean slate"

# --- Step 3: Start only MariaDB + Elasticsearch + Kibana (NOT the app yet) ---
step "Starting MariaDB + Elasticsearch + Kibana..."
$COMPOSE up -d --build mariadb elasticsearch kibana 2>&1 | tail -10

MARIADB_CONTAINER=$($COMPOSE ps -q mariadb)
if [ -z "$MARIADB_CONTAINER" ]; then
    fail "MariaDB container not found"
fi

step "Waiting for MariaDB to be ready..."
if ! wait_for_mariadb "$MARIADB_CONTAINER" 60; then
    fail "MariaDB not ready"
fi
echo ""
ok "MariaDB ready"

# --- Step 4: Load data ---
DUMP_FILE="$PROJECT_DIR/tools/db_scripts/sm-test-dump.sql"
SEED_START=$(date +%s)

if [ -f "$DUMP_FILE" ]; then
    step "Loading real production data dump (this may take a minute)..."
    # Convert UTF-16 to UTF-8 if needed
    MYSQL_OPTS="--max-allowed-packet=256M"
    if file "$DUMP_FILE" | grep -qi "utf-16\|unicode"; then
        iconv -f UTF-16 -t UTF-8 "$DUMP_FILE" | docker exec -i "$MARIADB_CONTAINER" mariadb $MYSQL_OPTS -uroot -proot supportmanagement 2>&1 | tail -3
    else
        docker exec -i "$MARIADB_CONTAINER" mariadb $MYSQL_OPTS -uroot -proot supportmanagement < "$DUMP_FILE" 2>&1 | tail -3
    fi
    BASE_COUNT=$(docker exec "$MARIADB_CONTAINER" mariadb -uroot -proot -N -e "SELECT COUNT(*) FROM supportmanagement.errand" 2>/dev/null | tr -d '[:space:]')
    ok "Real data loaded: $BASE_COUNT errands"

    step "Scaling to 700k errands (generating synthetic data matching real distributions)..."
    docker exec -i "$MARIADB_CONTAINER" mariadb -uroot -proot supportmanagement < "$SCRIPT_DIR/scale-data.sql" 2>&1 | grep -v "^$"
else
    step "No production dump found, using synthetic seed data..."
    docker exec -i "$MARIADB_CONTAINER" mariadb -uroot -proot supportmanagement < "$SCRIPT_DIR/seed-data.sql"
fi

SEED_END=$(date +%s)
SEED_TIME=$((SEED_END - SEED_START))

TOTAL_ERRANDS=$(docker exec "$MARIADB_CONTAINER" mariadb -uroot -proot -N -e "SELECT COUNT(*) FROM supportmanagement.errand" 2>/dev/null | tr -d '[:space:]')
TOTAL_JP=$(docker exec "$MARIADB_CONTAINER" mariadb -uroot -proot -N -e "SELECT COUNT(*) FROM supportmanagement.json_parameter" 2>/dev/null | tr -d '[:space:]')
ok "Data ready: $TOTAL_ERRANDS errands, $TOTAL_JP jsonParameters (${SEED_TIME}s)"

# --- Step 5: Start the app (Flyway validates existing schema, reindex runs with full data) ---
step "Starting application (Flyway validate + reindex $TOTAL_ERRANDS errands)..."
$COMPOSE up -d supportmanagement 2>&1 | tail -5

REINDEX_START=$(date +%s)
step "Waiting for application to start..."
if ! wait_for_app 900; then
    echo ""
    echo -e "${RED}Application failed to start${NC}"
    $COMPOSE logs supportmanagement 2>&1 | tail -40
    fail "Application not healthy"
fi
echo ""
ok "Application started"

# Wait for reindex to complete by monitoring ES document count until it stabilizes
step "Waiting for reindex to complete (monitoring ES document count)..."
PREV_COUNT=0
STABLE_CHECKS=0
for i in $(seq 1 120); do
    curl -s "http://localhost:9200/errand/_refresh" > /dev/null 2>&1
    CURRENT_COUNT=$(curl -s "http://localhost:9200/errand/_count" 2>/dev/null | python3 -c "import json,sys; print(json.load(sys.stdin).get('count',0))" 2>/dev/null || echo "0")
    if [ "$CURRENT_COUNT" = "$PREV_COUNT" ] && [ "$CURRENT_COUNT" != "0" ]; then
        STABLE_CHECKS=$((STABLE_CHECKS + 1))
        if [ $STABLE_CHECKS -ge 3 ]; then
            echo ""
            break
        fi
    else
        STABLE_CHECKS=0
        echo -ne "\r  ES documents: $CURRENT_COUNT"
    fi
    PREV_COUNT=$CURRENT_COUNT
    sleep 5
done

REINDEX_END=$(date +%s)
REINDEX_TIME=$((REINDEX_END - REINDEX_START))

ES_COUNT=$(curl -s "http://localhost:9200/errand/_count" | python3 -c "import json,sys; print(json.load(sys.stdin).get('count','?'))" 2>/dev/null)
ok "Reindex complete: $ES_COUNT documents indexed (took ${REINDEX_TIME}s)"

# --- Step 6: Run measurements ---
step "Running performance measurements..."
echo ""
chmod +x "$SCRIPT_DIR/test-queries.sh"
"$SCRIPT_DIR/test-queries.sh"

# --- Summary ---
echo -e "${YELLOW}Timing Summary:${NC}"
echo "  Data loading + scaling:       ${SEED_TIME}s"
echo "  Reindex to Elasticsearch:     ${REINDEX_TIME}s"
echo "  Total errands:                $TOTAL_ERRANDS"
echo "  Total jsonParameters:         $TOTAL_JP"
echo "  ES documents:                 $ES_COUNT"
echo ""
echo -e "${GREEN}Kibana dashboard:${NC}        http://localhost:5601"
echo -e "${GREEN}To re-run measurements:${NC}  ./tools/elasticsearch-poc/test-queries.sh"
echo -e "${GREEN}To stop everything:${NC}      cd tools/elasticsearch-poc && $COMPOSE down -v"
echo ""
