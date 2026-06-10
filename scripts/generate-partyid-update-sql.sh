#!/usr/bin/env bash
#
# generate-partyid-update-sql.sh
#
# Reads a CSV of stakeholder rows whose external_id is NOT a valid UUID (produced by
# scripts/select-nonuuid-stakeholders.sql, exported from DBeaver) and, for each row, resolves
# the partyId via the Sundsvall Party service:
#
#     GET {PARTY_BASE_URL}/{municipalityId}/{type}/{legalId}/partyId   (type = PRIVATE | ENTERPRISE)
#
# It then prints ready-to-run UPDATE statements to STDOUT, which you review and run in DBeaver.
# No database connection is made by this script - only HTTP calls to the party service.
#
# Usage:
#     ./generate-partyid-update-sql.sh candidates.csv  > updates.sql
#     # or via stdin:
#     cat candidates.csv | ./generate-partyid-update-sql.sh > updates.sql
#
# CSV format (comma- OR semicolon-separated - auto-detected; optional header, optional
# double-quotes around fields):
#     id, external_id, external_id_type, municipality_id
#
# Configuration
#   Set the values directly in the CONFIG block below, or pass them as environment
#   variables (an environment variable, when set, overrides the in-script value).
#   Anything still missing at runtime is prompted for interactively (the secret is
#   read without echoing):
#   PARTY_BASE_URL        Party service base, e.g. https://api-i-test.sundsvall.se/party/2.0
#   TOKEN_URL             OAuth2 token endpoint (client_credentials)
#   CLIENT_ID CLIENT_SECRET   OAuth2 client credentials for the party service
#
# Requirements: bash, curl, sed, tr (no jq/python needed - the OAuth2 token is parsed with bash).
#
# STDOUT = pure SQL (UPDATE statements + comment lines). All progress/diagnostics go to STDERR,
# so you can safely redirect STDOUT to a .sql file.
# ---------------------------------------------------------------------------------------------

set -euo pipefail

# =============================================================================
# CONFIG - set your values here between the quotes.
#
# The "${VAR:-...}" form means: use the matching environment variable when it is
# set, otherwise fall back to the value you type here. Anything left empty here
# (and not set in the environment) is prompted for interactively when you run the
# script - so filling these in is optional.
#
# SECURITY: CLIENT_SECRET is a credential. The scripts/ directory is untracked -
# keep it that way and do NOT commit this file with a real secret filled in.
# Prefer leaving CLIENT_SECRET empty and exporting it in your shell when running.
# =============================================================================
PARTY_BASE_URL="${PARTY_BASE_URL:-}"   # e.g. https://api-i-test.sundsvall.se/party/2.0
TOKEN_URL="${TOKEN_URL:-}"             # OAuth2 token endpoint (client_credentials)
CLIENT_ID="${CLIENT_ID:-}"             # OAuth2 client id
CLIENT_SECRET="${CLIENT_SECRET:-}"     # OAuth2 client secret (keep out of version control)

UUID_REGEX_BASH='^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$'

die() { echo "ERROR: $*" >&2; exit 1; }
log() { echo "$*" >&2; }

# Ensure the named variable has a value: if it is empty (not set in the CONFIG
# block and not provided as an environment variable), prompt the user for it.
# Prompts are read from the controlling terminal (/dev/tty) - never from stdin,
# which may be carrying the piped-in CSV - and are written to stderr so stdout
# stays pure SQL. Pass a non-empty third argument to read the value silently.
prompt_for() {
  local var="$1" label="$2" silent="${3:-}"
  [[ -n "${!var:-}" ]] && return 0
  # /dev/tty may exist as a device node yet not be openable (no controlling
  # terminal, e.g. CI) - actually try to open it rather than trusting -r.
  ( : < /dev/tty ) 2>/dev/null || die "Missing $var and no terminal is available to prompt for it. Set it in the CONFIG block or export it as an environment variable."
  local value=""
  if [[ -n "$silent" ]]; then
    read -r -s -p "Enter $label: " value < /dev/tty
    printf '\n' >&2
  else
    read -r -p "Enter $label: " value < /dev/tty
  fi
  [[ -n "$value" ]] || die "$var must not be empty"
  printf -v "$var" '%s' "$value"
}
require_cmd() { for c in "$@"; do command -v "$c" >/dev/null 2>&1 || die "Required command not found: $c"; done; }

# Extract a string field's value from a (flat) JSON object using only bash.
# Matches  "field" : "value"  allowing whitespace around the colon and any
# backslash-escaped chars inside the value, then unescapes \" \/ \\ . This is
# enough for an OAuth2 token response, which is a flat object of string fields.
json_value() {
  local field="$1" json="$2"
  if [[ "$json" =~ \"$field\"[[:space:]]*:[[:space:]]*\"(([^\"\\]|\\.)*)\" ]]; then
    local v="${BASH_REMATCH[1]}"
    v="${v//\\\//\/}"      # \/ -> /
    v="${v//\\\"/\"}"      # \" -> "
    v="${v//\\\\/\\}"      # \\ -> \   (do last)
    printf '%s' "$v"
  fi
}

sql_escape() { printf '%s' "$1" | sed "s/'/''/g"; }

# Trim whitespace/CR and a single pair of surrounding double-quotes from a CSV field.
clean_field() {
  local v="$1"
  v="${v%$'\r'}"
  v="${v#"${v%%[![:space:]]*}"}"   # ltrim
  v="${v%"${v##*[![:space:]]}"}"   # rtrim
  v="${v%\"}"; v="${v#\"}"          # strip surrounding quotes
  printf '%s' "$v"
}

party_type_for() {
  case "$(printf '%s' "$1" | tr '[:lower:]' '[:upper:]')" in
    ENTERPRISE|COMPANY|ORGANIZATION|ORGANISATION) echo "ENTERPRISE" ;;
    PRIVATE)                                       echo "PRIVATE" ;;
    *)                                             echo "" ;;
  esac
}

ACCESS_TOKEN=""
fetch_token() {
  local resp
  resp=$(curl -sS -X POST "$TOKEN_URL" -u "$CLIENT_ID:$CLIENT_SECRET" -d 'grant_type=client_credentials') \
    || die "Token request failed"
  ACCESS_TOKEN=$(json_value access_token "$resp")
  [[ -n "$ACCESS_TOKEN" ]] || die "No access_token in token response: $resp"
}

# Echoes partyId on success; returns 4 on 404, non-zero otherwise. Refreshes token once on 401.
resolve_party_id() {
  local municipality_id="$1" type="$2" legal_id="$3" attempt="${4:-1}"
  local out http_code body
  out=$(curl -sS -w '\n%{http_code}' \
    -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Accept: text/plain' \
    "$PARTY_BASE_URL/$municipality_id/$type/$legal_id/partyId") || return 2
  http_code="${out##*$'\n'}"
  body="${out%$'\n'*}"
  case "$http_code" in
    200) printf '%s' "$body" | tr -d '"[:space:]'; return 0 ;;
    401) if [[ "$attempt" -eq 1 ]]; then fetch_token; resolve_party_id "$municipality_id" "$type" "$legal_id" 2; return $?; fi; return 3 ;;
    404) return 4 ;;
    *)   log "    party returned HTTP $http_code: $body"; return 5 ;;
  esac
}

# --- preflight -------------------------------------------------------------------------------

require_cmd curl sed tr

# Any value not set in the CONFIG block or via environment variables is prompted for.
prompt_for PARTY_BASE_URL "Party service base URL (e.g. https://api-i-test.sundsvall.se/party/2.0)"
prompt_for TOKEN_URL      "OAuth2 token URL"
prompt_for CLIENT_ID      "OAuth2 client id"
prompt_for CLIENT_SECRET  "OAuth2 client secret" silent

INPUT="${1:-/dev/stdin}"
[[ "$INPUT" == "/dev/stdin" || -r "$INPUT" ]] || die "Cannot read input CSV: $INPUT"

log "=== Generating partyId UPDATE statements ==="
log "  Party base : $PARTY_BASE_URL"
log "  Input      : $INPUT"
log

fetch_token
log "Obtained OAuth2 token."

total=0; resolved=0; skipped_type=0; not_found=0; errors=0

echo "-- Generated by generate-partyid-update-sql.sh"
echo "-- Review carefully, then run in DBeaver (inside a transaction)."
echo "-- Each UPDATE is guarded by the original external_id to avoid clobbering changed rows."
echo

# Auto-detect the field delimiter from the first data line: DBeaver exports with
# ';' under a Swedish locale but ',' elsewhere. Detected once, then reused.
# "|| [[ -n "$line" ]]" makes the loop also process a final line lacking a newline.
DELIM=""
while IFS= read -r line || [[ -n "$line" ]]; do
  line="${line%$'\r'}"
  [[ -n "$line" ]] || continue
  if [[ -z "$DELIM" ]]; then
    if [[ "$line" == *";"* ]]; then DELIM=';'; else DELIM=','; fi
    log "Using '$DELIM' as CSV delimiter."
  fi
  IFS="$DELIM" read -r id external_id external_id_type municipality_id _rest <<< "$line"
  id=$(clean_field "$id")
  external_id=$(clean_field "$external_id")
  external_id_type=$(clean_field "$external_id_type")
  municipality_id=$(clean_field "$municipality_id")

  # Skip empty lines and a possible header row (id column not numeric).
  [[ -n "$id" ]] || continue
  [[ "$id" =~ ^[0-9]+$ ]] || { log "Skipping non-numeric id line (header?): '$id'"; continue; }

  total=$((total + 1))

  type=$(party_type_for "$external_id_type")
  if [[ -z "$type" ]]; then
    echo "-- SKIP id=$id externalId='$external_id' type='$external_id_type' (not a party-resolvable type)"
    skipped_type=$((skipped_type + 1))
    continue
  fi

  legal_id="$(printf '%s' "$external_id" | tr -cd '0-9')"
  if [[ -z "$legal_id" ]]; then
    echo "-- SKIP id=$id externalId='$external_id' (no digits to use as legalId)"
    skipped_type=$((skipped_type + 1))
    continue
  fi

  set +e
  party_id=$(resolve_party_id "$municipality_id" "$type" "$legal_id")
  rc=$?
  set -e

  if [[ $rc -eq 4 ]]; then
    echo "-- MISS id=$id type=$type legalId=$legal_id (party returned 404 - no partyId)"
    not_found=$((not_found + 1)); continue
  elif [[ $rc -ne 0 ]]; then
    echo "-- ERROR id=$id type=$type legalId=$legal_id (lookup failed, rc=$rc)"
    errors=$((errors + 1)); continue
  elif [[ ! "$party_id" =~ $UUID_REGEX_BASH ]]; then
    echo "-- ERROR id=$id type=$type legalId=$legal_id (party returned non-UUID: '$party_id')"
    errors=$((errors + 1)); continue
  fi

  echo "UPDATE stakeholder SET external_id = '$(sql_escape "$party_id")' WHERE id = $id AND external_id = '$(sql_escape "$external_id")'; -- type=$type legalId=$legal_id"
  resolved=$((resolved + 1))
done < "$INPUT"

log
log "=== Summary ==="
log "  Candidates examined : $total"
log "  UPDATE statements   : $resolved"
log "  Skipped (type)      : $skipped_type"
log "  Not found in party  : $not_found"
log "  Errors              : $errors"
