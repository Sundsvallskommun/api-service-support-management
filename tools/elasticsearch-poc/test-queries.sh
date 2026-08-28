#!/bin/bash
# Elasticsearch PoC — Performance Measurements
# Queries ES directly at localhost:9200 to measure search latency on jsonParameters.

ES="http://localhost:9200"
INDEX="errand"
RUNS=10

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
RED='\033[0;31m'
NC='\033[0m'

echo ""
echo -e "${CYAN}╔══════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║          Elasticsearch PoC — Performance Measurements          ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Build a query (accepts raw JSON query clause)
build_qs_query() {
    local q="$1"
    local size="${2:-10000}"
    cat <<QUERY
{"size":$size,"_source":false,"query":{"bool":{"must":[{"query_string":{"query":"$q","default_field":"jsonParameters.*","lenient":true}}],"filter":[{"term":{"municipalityId":"2281"}}]}}}
QUERY
}

# Build a match query for fields with special characters (hyphens etc)
build_match_query() {
    local field="$1"
    local value="$2"
    local size="${3:-10000}"
    cat <<QUERY
{"size":$size,"_source":false,"query":{"bool":{"must":[{"match":{"$field":"$value"}}],"filter":[{"term":{"municipalityId":"2281"}}]}}}
QUERY
}

# Build a match_phrase query for exact phrase matching on text fields
build_phrase_query() {
    local field="$1"
    local value="$2"
    local size="${3:-10000}"
    cat <<QUERY
{"size":$size,"_source":false,"query":{"bool":{"must":[{"match_phrase":{"$field":"$value"}}],"filter":[{"term":{"municipalityId":"2281"}}]}}}
QUERY
}

# Build a wildcard query for fields with special characters
build_wildcard_query() {
    local field="$1"
    local value="$2"
    local size="${3:-10000}"
    cat <<QUERY
{"size":$size,"_source":false,"query":{"bool":{"must":[{"wildcard":{"$field":{"value":"$value"}}}],"filter":[{"term":{"municipalityId":"2281"}}]}}}
QUERY
}

# Build a term query for exact keyword matching
build_term_query() {
    local field="$1"
    local value="$2"
    local size="${3:-10000}"
    cat <<QUERY
{"size":$size,"_source":false,"query":{"bool":{"must":[{"term":{"$field":"$value"}}],"filter":[{"term":{"municipalityId":"2281"}}]}}}
QUERY
}

# Measure: run a query N times, collect latencies
measure() {
    local label="$1"
    local query_body="$2"
    local times_csv=""
    local result_count=""

    for ((run=1; run<=RUNS; run++)); do
        local output=$(curl -s -X POST "$ES/$INDEX/_search" \
            -H "Content-Type: application/json" \
            -d "$query_body" \
            -o /tmp/es-poc-response.json \
            -w "%{time_total}")
        if [ -z "$times_csv" ]; then
            times_csv="$output"
        else
            times_csv="$times_csv,$output"
        fi
    done

    if [ -f /tmp/es-poc-response.json ]; then
        result_count=$(python3 -c "
import json
try:
    d = json.load(open('/tmp/es-poc-response.json'))
    total = d.get('hits',{}).get('total',{})
    if isinstance(total, dict):
        v = total.get('value', 0)
        r = total.get('relation', 'eq')
        print(f'{v}+' if r == 'gte' else v)
    else:
        print(total)
except:
    print('-')
" 2>/dev/null)
    fi

    local stats=$(python3 -c "
times = [$times_csv]
times_ms = [t * 1000 for t in times]
times_ms.sort()
n = len(times_ms)
p95_idx = min(int(n * 0.95), n - 1)
print(f'{min(times_ms):.0f} {sum(times_ms)/n:.0f} {times_ms[p95_idx]:.0f} {max(times_ms):.0f}')
" 2>/dev/null)

    local min_ms=$(echo "$stats" | awk '{print $1}')
    local avg_ms=$(echo "$stats" | awk '{print $2}')
    local p95_ms=$(echo "$stats" | awk '{print $3}')
    local max_ms=$(echo "$stats" | awk '{print $4}')

    printf "  %-58s %7s  %5s  %5s  %5s  %5s\n" "$label" "$result_count" "${min_ms}ms" "${avg_ms}ms" "${p95_ms}ms" "${max_ms}ms"
}

get_hits() {
    local query_body="$1"
    curl -s -X POST "$ES/$INDEX/_search" \
        -H "Content-Type: application/json" \
        -d "$query_body" | python3 -c "
import json, sys
try:
    d = json.load(sys.stdin)
    total = d.get('hits',{}).get('total',{})
    print(total.get('value',0) if isinstance(total,dict) else total)
except:
    print(0)
" 2>/dev/null
}

# --- Dataset info ---
echo -e "${YELLOW}Dataset:${NC}"
es_count=$(curl -s "$ES/$INDEX/_count" | python3 -c "import json,sys; print(json.load(sys.stdin).get('count','?'))" 2>/dev/null)
echo "  Documents in ES index: $es_count"
echo ""

# ============================================================
# SECTION 1: Real data patterns (avvikelse-plats-handelse)
# Uses match/wildcard/term queries to avoid query_string issues
# with hyphenated field names
# ============================================================
echo -e "${YELLOW}1. Real Data — Healthcare Deviation Reports ($RUNS runs each):${NC}"
printf "  %-58s %7s  %5s  %5s  %5s  %5s\n" "Test" "Hits" "Min" "Avg" "P95" "Max"
printf "  %-58s %7s  %5s  %5s  %5s  %5s\n" "----------------------------------------------------------" "-------" "-----" "-----" "-----" "-----"

# Org name (match query — handles hyphenated field path)
measure 'Org name: "VOF HOS Korttidsboende"' \
    "$(build_match_query 'jsonParameters.avvikelse-plats-handelse.data.facilityInfo.orgName' 'VOF HOS Korttidsboende')"

measure 'Org name: "Hemtjänst"' \
    "$(build_match_query 'jsonParameters.avvikelse-plats-handelse.data.facilityInfo.orgName' 'Hemtjänst')"

measure 'Org name wildcard: VOF*' \
    "$(build_wildcard_query 'jsonParameters.avvikelse-plats-handelse.data.facilityInfo.orgName' 'vof*')"

# Free text (cross-field, uses query_string — works since no field prefix)
measure 'Free text: "Korttidsboende" (any field)' \
    "$(build_qs_query '\"Korttidsboende\"')"

measure 'Free text: "Hemtjänst" (any field)' \
    "$(build_qs_query '\"Hemtjänst\"')"

# Org ID (term query for exact numeric match)
measure 'Org ID: orgId=7514' \
    "$(build_term_query 'jsonParameters.avvikelse-plats-handelse.data.facilityInfo.orgId' '7514')"

echo ""

# ============================================================
# SECTION 2: Schema evolution — facility ID across 8 key names
# ============================================================
echo -e "${YELLOW}2. Schema Evolution — Facility ID across 8 key names ($RUNS runs each):${NC}"
printf "  %-58s %7s  %5s  %5s  %5s  %5s\n" "Test" "Hits" "Min" "Avg" "P95" "Max"
printf "  %-58s %7s  %5s  %5s  %5s  %5s\n" "----------------------------------------------------------" "-------" "-----" "-----" "-----" "-----"

# Field-specific (one schema version, phrase match for exact)
measure 'Field-specific: facilityId:"FAC-0001"' \
    "$(build_phrase_query 'jsonParameters.facility.data.facilityId' 'FAC-0001')"

# Cross-schema (finds across ALL key names via free text)
measure 'Cross-schema exact: "FAC-0001" (all 8 keys)' \
    "$(build_qs_query '\"FAC-0001\"')"

measure 'Cross-schema exact: "FAC-0500" (all 8 keys)' \
    "$(build_qs_query '\"FAC-0500\"')"

# Inspector
measure 'Inspector: "Anna Svensson"' \
    "$(build_match_query 'jsonParameters.inspection.data.inspectorName' 'Anna Svensson')"

# Address
measure 'Address: Storgatan' \
    "$(build_match_query 'jsonParameters.facility.data.address' 'Storgatan')"

echo ""

# ============================================================
# SECTION 3: General performance
# ============================================================
echo -e "${YELLOW}3. General Performance ($RUNS runs each):${NC}"
printf "  %-58s %7s  %5s  %5s  %5s  %5s\n" "Test" "Hits" "Min" "Avg" "P95" "Max"
printf "  %-58s %7s  %5s  %5s  %5s  %5s\n" "----------------------------------------------------------" "-------" "-----" "-----" "-----" "-----"

measure 'Wildcard: FAC-00*' \
    "$(build_qs_query 'FAC-00*')"

measure 'Unquoted: FAC-0001 (tokenized, broad match)' \
    "$(build_qs_query 'FAC-0001')"

measure 'No results: nonexistent value' \
    "$(build_qs_query '\"zzz-nonexistent-xyz-99999\"')"

echo ""

# ============================================================
# SECTION 4: Cross-schema verification
# ============================================================
echo -e "${YELLOW}4. Cross-Schema Verification:${NC}"
echo '  Searching "FAC-0001" across all 8 facility key name variations'
echo ""

all_hits=$(get_hits "$(build_qs_query '\"FAC-0001\"')")
hits_v1=$(get_hits "$(build_phrase_query 'jsonParameters.facility.data.facilityId' 'FAC-0001')")
hits_v2=$(get_hits "$(build_phrase_query 'jsonParameters.facility.data.anläggningsId' 'FAC-0001')")
hits_v3=$(get_hits "$(build_phrase_query 'jsonParameters.facility.data.facility_id' 'FAC-0001')")
hits_v4=$(get_hits "$(build_phrase_query 'jsonParameters.facility.data.anlaggning' 'FAC-0001')")
hits_v5=$(get_hits "$(build_phrase_query 'jsonParameters.facility.data.anlaggningsNr' 'FAC-0001')")
hits_v6=$(get_hits "$(build_phrase_query 'jsonParameters.facility.data.fastighetsId' 'FAC-0001')")
hits_v7=$(get_hits "$(build_phrase_query 'jsonParameters.facility.data.propertyId' 'FAC-0001')")
hits_v8=$(get_hits "$(build_phrase_query 'jsonParameters.facility.data.objektId' 'FAC-0001')")
sum_specific=$((hits_v1 + hits_v2 + hits_v3 + hits_v4 + hits_v5 + hits_v6 + hits_v7 + hits_v8))

printf "  %-40s %s\n" "v1.0 facilityId:" "$hits_v1 hits"
printf "  %-40s %s\n" "v1.1 anläggningsId:" "$hits_v2 hits"
printf "  %-40s %s\n" "v2.0 facility_id:" "$hits_v3 hits"
printf "  %-40s %s\n" "v2.1 anlaggning:" "$hits_v4 hits"
printf "  %-40s %s\n" "v3.0 anlaggningsNr:" "$hits_v5 hits"
printf "  %-40s %s\n" "v3.1 fastighetsId:" "$hits_v6 hits"
printf "  %-40s %s\n" "v4.0 propertyId:" "$hits_v7 hits"
printf "  %-40s %s\n" "v4.1 objektId:" "$hits_v8 hits"
echo "  ---"
printf "  %-40s %s\n" "Sum per-field:" "$sum_specific hits"
printf "  %-40s %s\n" 'Cross-schema ("FAC-0001"):' "$all_hits hits"

if [ "$sum_specific" -gt 0 ] && [ "$all_hits" -ge "$sum_specific" ] 2>/dev/null; then
    echo -e "  ${GREEN}✓ Cross-schema found $all_hits (field-specific sum: $sum_specific)${NC}"
elif [ "$all_hits" -gt 0 ] 2>/dev/null; then
    echo -e "  ${YELLOW}⚠ Cross-schema=$all_hits vs field-specific=$sum_specific${NC}"
else
    echo -e "  ${RED}✗ No hits — is data indexed?${NC}"
fi

echo ""

# --- Tokenization ---
echo -e "${YELLOW}5. Tokenization Impact:${NC}"
unquoted=$(get_hits "$(build_qs_query 'FAC-0001')")
quoted=$(get_hits "$(build_qs_query '\"FAC-0001\"')")
echo "  Unquoted 'FAC-0001' (tokenized): $unquoted hits"
echo "  Quoted '\"FAC-0001\"' (exact):    $quoted hits"
echo "  → Always quote values with hyphens for exact matching"

# ============================================================
# SECTION 6: Hybrid queries via app endpoint (MariaDB + ES)
# Tests the full production query path: ES returns IDs,
# MariaDB narrows with spring-filter + access control + pagination
# ============================================================
APP="http://localhost:8080"

measure_app() {
    local label="$1"
    local url="$2"
    local times_csv=""
    local result_count=""

    for ((run=1; run<=RUNS; run++)); do
        local output=$(curl -s -o /tmp/es-poc-response.json -w "%{time_total}" "$url")
        if [ -z "$times_csv" ]; then
            times_csv="$output"
        else
            times_csv="$times_csv,$output"
        fi
    done

    if [ -f /tmp/es-poc-response.json ]; then
        result_count=$(python3 -c "
import json
try:
    d = json.load(open('/tmp/es-poc-response.json'))
    if isinstance(d, dict) and 'totalElements' in d:
        print(d['totalElements'])
    elif isinstance(d, dict) and 'status' in d:
        print('ERR')
    else:
        print('-')
except:
    print('-')
" 2>/dev/null)
    fi

    local stats=$(python3 -c "
times = [$times_csv]
times_ms = [t * 1000 for t in times]
times_ms.sort()
n = len(times_ms)
p95_idx = min(int(n * 0.95), n - 1)
print(f'{min(times_ms):.0f} {sum(times_ms)/n:.0f} {times_ms[p95_idx]:.0f} {max(times_ms):.0f}')
" 2>/dev/null)

    local min_ms=$(echo "$stats" | awk '{print $1}')
    local avg_ms=$(echo "$stats" | awk '{print $2}')
    local p95_ms=$(echo "$stats" | awk '{print $3}')
    local max_ms=$(echo "$stats" | awk '{print $4}')

    printf "  %-58s %7s  %5s  %5s  %5s  %5s\n" "$label" "$result_count" "${min_ms}ms" "${avg_ms}ms" "${p95_ms}ms" "${max_ms}ms"
}

# Check if app is running
APP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$APP/api-docs" 2>/dev/null)
if [ "$APP_STATUS" = "200" ]; then
    echo -e "${YELLOW}6. Hybrid Queries — MariaDB + ES via App ($RUNS runs each):${NC}"
    printf "  %-58s %7s  %5s  %5s  %5s  %5s\n" "Test" "Hits" "Min" "Avg" "P95" "Max"
    printf "  %-58s %7s  %5s  %5s  %5s  %5s\n" "----------------------------------------------------------" "-------" "-----" "-----" "-----" "-----"

    # ES-only via app (no spring-filter, just jsonParameterFilter)
    measure_app 'App: jsonParam only "Storgatan"' \
        "$APP/2281/CONTACTCENTER/errands?jsonParameterFilter=%22Storgatan%22&page=0&size=20"

    # Hybrid: spring-filter status + jsonParameterFilter
    measure_app 'App: status=ASSIGNED + "Storgatan"' \
        "$APP/2281/CONTACTCENTER/errands?filter=status%3A%27ASSIGNED%27&jsonParameterFilter=%22Storgatan%22&page=0&size=20"

    # Hybrid: multi-status + inspector name
    measure_app 'App: status=NEW|ASSIGNED + "Anna Svensson"' \
        "$APP/2281/CONTACTCENTER/errands?filter=(status%3A%27NEW%27%20or%20status%3A%27ASSIGNED%27)&jsonParameterFilter=%22Anna+Svensson%22&page=0&size=20"

    # Hybrid: cross-schema facility search + status filter
    measure_app 'App: status=ASSIGNED + "FAC-0001"' \
        "$APP/2281/CONTACTCENTER/errands?filter=status%3A%27ASSIGNED%27&jsonParameterFilter=%22FAC-0001%22&page=0&size=20"

    # No ES results → empty page
    measure_app 'App: no results (nonexistent jsonParam)' \
        "$APP/2281/CONTACTCENTER/errands?filter=status%3A%27ASSIGNED%27&jsonParameterFilter=%22zzz-nonexistent-xyz%22&page=0&size=20"

    # No jsonParameterFilter (baseline — pure MariaDB)
    measure_app 'App: baseline (no jsonParam, MariaDB only)' \
        "$APP/2281/CONTACTCENTER/errands?filter=status%3A%27ASSIGNED%27&page=0&size=20"

    echo ""
else
    echo -e "${YELLOW}6. Hybrid Queries — SKIPPED (app not running at $APP)${NC}"
    echo ""
fi

echo -e "${CYAN}╔══════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║                      Measurements Complete                     ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════════════════╝${NC}"
echo ""

rm -f /tmp/es-poc-response.json
