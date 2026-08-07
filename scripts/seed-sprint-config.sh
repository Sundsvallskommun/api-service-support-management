#!/usr/bin/env bash
#
# seed-sprint-config.sh
#
# Re-creates the SupportManagement configuration on the "sprint" instance through the public
# REST API. Run it after the sprint database has been wiped to bring the instance back to a
# known baseline.
#
# The script is idempotent: every item is checked before it is written, so it can be run
# repeatedly. Existing items are replaced (singleton configs) or left untouched (collection
# items such as statuses and categories, which are keyed by a generated id).
#
# Usage:
#     ./seed-sprint-config.sh                 # apply the configuration
#     ./seed-sprint-config.sh --dry-run       # show what would be sent, change nothing
#     ./seed-sprint-config.sh --only NS[,NS]  # limit the run to the given namespace(s)
#
# Configuration
#   Set the values directly in the CONFIG block below, or pass them as environment variables
#   (an environment variable, when set, overrides the in-script value):
#     BASE_URL         Base address of the sprint instance, without trailing slash
#     MUNICIPALITY_ID  Municipality the namespaces belong to
#     TOKEN_URL        OAuth2 token endpoint (client_credentials). Leave empty when the
#     CLIENT_ID        instance is reached without authentication (e.g. port-forwarded
#     CLIENT_SECRET    straight to the pod) - no Authorization header is then sent.
#
# SECURITY: CLIENT_SECRET is a credential - do NOT commit this file with a real secret filled
# in. Prefer leaving it empty and exporting it in your shell when running.
#
# Requirements: bash 4.3+, curl (no jq/python needed).
# ---------------------------------------------------------------------------------------------

set -euo pipefail

# =============================================================================================
# CONFIG - set your values here between the quotes.
#
# The "${VAR:-...}" form means: use the matching environment variable when it is set, otherwise
# fall back to the value you type here.
# =============================================================================================
BASE_URL="${BASE_URL:-https://support-management-sprint-af-supportmanagement.apps.ocp201.sundsvall.se}"
MUNICIPALITY_ID="${MUNICIPALITY_ID:-2281}"

TOKEN_URL="${TOKEN_URL:-}"          # empty => no OAuth2, requests are sent unauthenticated
CLIENT_ID="${CLIENT_ID:-}"
CLIENT_SECRET="${CLIENT_SECRET:-}"

# ---------------------------------------------------------------------------------------------
# Namespaces, one per line:
#
#   namespace|displayName|shortCode|notificationTTLInDays|accessControl|notifyReporter
#
# namespace             A-Z, a-z, 0-9, - and _ only
# shortCode             prefix used in generated errand numbers
# notificationTTLInDays days a notification is kept before it is cleaned up
# accessControl         true/false - enables access control for the namespace
# notifyReporter        true/false - notify reporter stakeholder on internal messages
#
# The namespaces listed here are the ones the script configures; every section below is applied
# to each of them.
# ---------------------------------------------------------------------------------------------
NAMESPACES=(
  "HEALTHCAREDEVIATIONVOF|Avvikelser hälso- och sjukvård VoF|VOF|40|false|false"
  "HEALTHCAREDEVIATIONIAF|Avvikelser hälso- och sjukvård IAF|IAF|40|false|false"
)

# ---------------------------------------------------------------------------------------------
# HOW TO ADD MORE CONFIGURATION
#
# Every section below is pure data - one array (or string) per resource. Two conventions apply
# to all of them:
#
#   * A variable named <SECTION> holds the values used for ALL namespaces.
#   * A variable named <SECTION>_<NAMESPACE> overrides it for that single namespace, e.g.
#     STATUSES_HEALTHCAREDEVIATIONVOF=( ... ). The override replaces the common value, it is
#     not merged with it.
#
# Values are raw JSON request bodies, exactly as documented in Swagger
# (<BASE_URL>/api-docs) - so a new field never requires a script change, only a data change.
# Leave a section empty to skip it.
# ---------------------------------------------------------------------------------------------

# --- Collections: metadata items, created only when an item with the same name is missing -----
# Statuses            POST /{municipalityId}/{namespace}/metadata/statuses
#
# displayName is what HandläggningsDraken shows, externalDisplayName what Katlan shows.
# The activity each status represents (from the agreed status table):
#   NEW                Anmälaren skickar in ärendet
#   REVIEW             Ärendet går till granskning
#   INQUIRY            Ärende går till utredningen
#   DECISION           Ärende går till beslut
#   FOLLOW_UP          Ärendet går till uppföljning
#   SOLVED             Ärendet avslutas
#   AWAITING_RESPONSE  Handläggare begär komplettering
#   ASSIGNED           Ärendet tilldelas mellan olika roller/enheter
STATUSES=(
  '{"name":"NEW","displayName":"Inkommit","externalDisplayName":"Inskickat","sortOrder":0}'
  '{"name":"REVIEW","displayName":"Under granskning","externalDisplayName":"Under granskning","sortOrder":1}'
  '{"name":"INQUIRY","displayName":"Under utredning","externalDisplayName":"Under utredning","sortOrder":2}'
  '{"name":"DECISION","displayName":"Under beslut","externalDisplayName":"Under beslut","sortOrder":3}'
  '{"name":"FOLLOW_UP","displayName":"Under uppföljning","externalDisplayName":"Under uppföljning","sortOrder":4}'
  '{"name":"SOLVED","displayName":"Avslutat","externalDisplayName":"Avslutat","sortOrder":5}'
  '{"name":"AWAITING_RESPONSE","displayName":"Komplettering","externalDisplayName":"Komplettering","sortOrder":6}'
  '{"name":"ASSIGNED","displayName":"Tilldelat","externalDisplayName":"Tilldelat","sortOrder":7}'
)

# Categories          POST /{municipalityId}/{namespace}/metadata/categories
CATEGORIES=(
  # '{"name":"CATEGORY_1","displayName":"Kategori 1","types":[{"name":"TYPE_1","displayName":"Typ 1","escalationEmail":"escalation@example.com"}]}'
)

# Roles               POST /{municipalityId}/{namespace}/metadata/roles
# sortOrder follows the order the roles were listed in.
ROLES=(
  '{"name":"REPORTER","displayName":"Rapportör","sortOrder":0}'
  '{"name":"UNIT_MANAGER","displayName":"Enhetschef","sortOrder":1}'
  '{"name":"HEAD_OF_OPERATIONS","displayName":"Verksamhetschef","sortOrder":2}'
  '{"name":"LEX_RESPONSIBLE","displayName":"LEX-ansvarig","sortOrder":3}'
  '{"name":"LEX_INVESTIGATOR","displayName":"LEX-utredare","sortOrder":4}'
  '{"name":"MAR_MAS","displayName":"MAR/MAS","sortOrder":5}'
  '{"name":"CONTACT","displayName":"Kontaktperson","sortOrder":6}'
  '{"name":"PRIMARY","displayName":"Ärendeägare","sortOrder":7}'
)

# External id types   POST /{municipalityId}/{namespace}/metadata/external-id-types
EXTERNAL_ID_TYPES=(
  # '{"name":"PRIVATE"}'
  # '{"name":"EMPLOYEE"}'
)

# Contact reasons     POST /{municipalityId}/{namespace}/metadata/contactreasons
# Keyed by "reason" instead of "name" - see CONTACT_REASONS_KEY below.
CONTACT_REASONS=(
  # '{"reason":"Telefon"}'
)

# --- Singletons: one config per namespace, replaced when it already exists ---------------------
# Labels              POST/PUT /{municipalityId}/{namespace}/metadata/labels
# Body is a bare JSON array of labels: [ {label}, ... ]  (note that GET returns them wrapped in
# a Labels object, {"labelStructure":[ ... ]} - POST/PUT take the array on its own)
#
# A label has classification, resourceName, displayName and nested labels. resourceName may only
# contain A-Z, 0-9 and _ ; it is the identity of the label and forms the resource path
# (parent/child), which is what must be unique within the namespace - the same resourceName under
# two different parents (HSL below) is therefore fine.
#
# Levels and their classification:
#   1  provision-root / category-root / report-type-root   the three roots
#   2  provision / provision-category / report-type        legal basis, report type
#   3  category                                            deviation category (Fall, Läkemedel, ...)
#   4  type                                                deviation type (Fel dos, Skada, ...)
#
# Resource names are English short forms of the Swedish displayName; the displayName is what is
# shown to users. Meaning of the resource names on the two upper levels, for reference (the API
# has no description field):
#   PROVISION    Lagrum
#     HSL          Hälso- och sjukvårdslagen
#     SOL          Socialtjänstlagen
#     LSS          Lag om stöd och service till vissa funktionshindrade
#   CATEGORY     Kategorisering
#     HSL          Lagrum - carries the deviation categories below it
#     SOL_LSS      Lagrum - carries the deviation categories below it
#   REPORT_TYPE  Rapporttyp
#     DEVIATION    Avvikelse
#     ABUSE        Missförhållande
LABELS='[
  {
    "classification": "provision-root",
    "resourceName": "PROVISION",
    "displayName": "Lagrum",
    "labels": [
      { "classification": "provision", "resourceName": "HSL", "displayName": "HSL" },
      { "classification": "provision", "resourceName": "SOL", "displayName": "SOL" },
      { "classification": "provision", "resourceName": "LSS", "displayName": "LSS" }
    ]
  },
  {
    "classification": "category-root",
    "resourceName": "CATEGORY",
    "displayName": "Kategori",
    "labels": [
      {
        "classification": "provision-category",
        "resourceName": "HSL",
        "displayName": "HSL",
        "labels": [
          {
            "classification": "category",
            "resourceName": "OTHER",
            "displayName": "Annat",
            "labels": [
              { "classification": "type", "resourceName": "INSUFFICIENT_INFORMATION", "displayName": "Bristande information" },
              { "classification": "type", "resourceName": "HYGIENE_ROUTINE_NOT_FOLLOWED", "displayName": "Ej följt hygienrutin" },
              { "classification": "type", "resourceName": "HEALTHCARE_ORDER_NOT_PERFORMED", "displayName": "Ej utförd hälso- och sjukvårdsordination" },
              { "classification": "type", "resourceName": "MEDICAL_DEVICE_PERSONAL_AIDS", "displayName": "Medicinteknik - Personliga hjälpmedel" },
              { "classification": "type", "resourceName": "MEDICAL_DEVICE_EQUIPMENT", "displayName": "Medicinteknik - Utrustning" },
              { "classification": "type", "resourceName": "INJURY", "displayName": "Skada" },
              { "classification": "type", "resourceName": "SUICIDE", "displayName": "Självmord" }
            ]
          },
          {
            "classification": "category",
            "resourceName": "FALL",
            "displayName": "Fall",
            "labels": [
              { "classification": "type", "resourceName": "FALL_OWN_ACTIVITY_ASSISTIVE_DEVICE", "displayName": "Fall egen aktivitet - Hjälpmedel" },
              { "classification": "type", "resourceName": "FALL_WITH_STAFF_ASSISTIVE_DEVICE", "displayName": "Fall med personal - Hjälpmedel" },
              { "classification": "type", "resourceName": "FALL_DURING_TRANSFER_WITH_STAFF", "displayName": "Fall vid förflyttning med personal" },
              { "classification": "type", "resourceName": "FALL_DURING_OWN_ACTIVITY", "displayName": "Fall vid patientens egna aktiviteter" }
            ]
          },
          { "classification": "category", "resourceName": "FALL_WITHOUT_INJURY", "displayName": "Fall utan skada" },
          { "classification": "category", "resourceName": "URINARY_CATHETER", "displayName": "KAD" },
          {
            "classification": "category",
            "resourceName": "MEDICATION",
            "displayName": "Läkemedel",
            "labels": [
              { "classification": "type", "resourceName": "OTHER", "displayName": "Annat" },
              { "classification": "type", "resourceName": "WRONG_DOSE", "displayName": "Fel dos" },
              { "classification": "type", "resourceName": "WRONG_PERSON", "displayName": "Fel person" },
              { "classification": "type", "resourceName": "INCORRECT_ADMINISTRATION", "displayName": "Felaktig administrering" },
              { "classification": "type", "resourceName": "LOSS_OF_MEDICATION", "displayName": "Förlust av läkemedel" },
              { "classification": "type", "resourceName": "DELAYED_DOSE", "displayName": "Fördröjd dos" },
              { "classification": "type", "resourceName": "COMMUNICATION_INFORMATION", "displayName": "Kommunikation/Information" },
              { "classification": "type", "resourceName": "MEDICATION_MISSING", "displayName": "Läkemedel saknas" },
              { "classification": "type", "resourceName": "THEFT_OF_MEDICATION", "displayName": "Stöld av läkemedel" },
              { "classification": "type", "resourceName": "MISSED_DOSE", "displayName": "Utebliven dos" }
            ]
          },
          { "classification": "category", "resourceName": "MEDICATION_DISPENSER", "displayName": "Läkemedelsautomat" },
          { "classification": "category", "resourceName": "ORAL_HEALTH", "displayName": "Munhälsa" },
          {
            "classification": "category",
            "resourceName": "NUTRITION",
            "displayName": "Nutrition",
            "labels": [
              { "classification": "type", "resourceName": "WEIGHT_LOSS", "displayName": "Viktnedgång" },
              { "classification": "type", "resourceName": "WEIGHT_GAIN", "displayName": "Viktuppgång" }
            ]
          },
          {
            "classification": "category",
            "resourceName": "REHAB",
            "displayName": "Rehab",
            "labels": [
              { "classification": "type", "resourceName": "ORDER_NOT_PERFORMED", "displayName": "Ej utförd ordination" },
              { "classification": "type", "resourceName": "ASSISTIVE_DEVICE", "displayName": "Hjälpmedel" },
              { "classification": "type", "resourceName": "ASSESSMENT_TREATMENT_NOT_PERFORMED", "displayName": "Utebliven bedömning/behandling" }
            ]
          },
          {
            "classification": "category",
            "resourceName": "PRESSURE_ULCER",
            "displayName": "Trycksår",
            "labels": [
              { "classification": "type", "resourceName": "GRADE_1", "displayName": "Grad 1: Syns som en rodnad på huden" },
              { "classification": "type", "resourceName": "GRADE_2", "displayName": "Grad 2: Blåsbildning eller avskavning av överhuden" },
              { "classification": "type", "resourceName": "GRADE_3", "displayName": "Grad 3: Ett sår har utvecklats som även omfattar underhuden" },
              { "classification": "type", "resourceName": "GRADE_4", "displayName": "Grad 4: Djup vävnadsskada som också omfattar muskel-, ben- eller stödjevävnad" }
            ]
          },
          {
            "classification": "category",
            "resourceName": "HEALTHCARE_ASSOCIATED_INFECTION",
            "displayName": "VRI (Vårdrelaterad infektion)",
            "labels": [
              { "classification": "type", "resourceName": "ESBL", "displayName": "ESBL" },
              { "classification": "type", "resourceName": "MRSA", "displayName": "MRSA" },
              { "classification": "type", "resourceName": "VRE", "displayName": "VRE" }
            ]
          },
          {
            "classification": "category",
            "resourceName": "PALLIATIVE_CARE",
            "displayName": "Vård i livets slut (Palliativ vård)",
            "labels": [
              { "classification": "type", "resourceName": "END_OF_LIFE_CONVERSATION", "displayName": "Brytpunktssamtal" },
              { "classification": "type", "resourceName": "PAIN_ASSESSMENT", "displayName": "Smärtskattning" }
            ]
          }
        ]
      },
      {
        "classification": "provision-category",
        "resourceName": "SOL_LSS",
        "displayName": "SOL/LSS",
        "labels": [
          {
            "classification": "category",
            "resourceName": "WORK_METHODS_AND_ROUTINES",
            "displayName": "Brister i arbetssätt, metoder och rutiner",
            "labels": [
              { "classification": "type", "resourceName": "METHOD_LACKS_EVIDENCE_BASE", "displayName": "Arbetssätt eller metod saknar kunskapsstöd" },
              { "classification": "type", "resourceName": "INSUFFICIENT_COMPETENCE", "displayName": "Bristande kompetens för uppdraget" },
              { "classification": "type", "resourceName": "ROUTINE_MISSING_OR_NOT_FOLLOWED", "displayName": "Brister i rutin, saknas eller följs inte" },
              { "classification": "type", "resourceName": "METHOD_USED_INCORRECTLY", "displayName": "Metod används felaktigt" },
              { "classification": "type", "resourceName": "INSUFFICIENT_INTRODUCTION", "displayName": "Otillräcklig introduktion" },
              { "classification": "type", "resourceName": "UNCLEAR_WORK_METHOD_OR_RESPONSIBILITY", "displayName": "Otydligt arbetssätt eller ansvar" }
            ]
          },
          {
            "classification": "category",
            "resourceName": "TREATMENT_BY_STAFF",
            "displayName": "Brister i bemötande av anställda m.",
            "labels": [
              { "classification": "type", "resourceName": "INSUFFICIENT_PARTICIPATION_AND_INFLUENCE", "displayName": "Bristande delaktighet och inflytande" },
              { "classification": "type", "resourceName": "INSUFFICIENT_COMMUNICATION", "displayName": "Bristande kommunikation" },
              { "classification": "type", "resourceName": "DISCRIMINATION", "displayName": "Diskriminering" },
              { "classification": "type", "resourceName": "UNPROFESSIONAL_OR_DISRESPECTFUL_TREATMENT", "displayName": "Oprofessionellt eller respektlöst bemötande" }
            ]
          },
          {
            "classification": "category",
            "resourceName": "PHYSICAL_ENVIRONMENT_AND_EQUIPMENT",
            "displayName": "Brister i fysisk miljö, utrustning (hjälpmedel) och teknik",
            "labels": [
              { "classification": "type", "resourceName": "FIRE_SAFETY_OR_LOCKING_SYSTEM", "displayName": "Bristande brandsäkerhet eller låssystem" },
              { "classification": "type", "resourceName": "UNSAFE_PHYSICAL_ENVIRONMENT", "displayName": "Otrygg eller olämplig fysisk miljö, begränsning av självständighet" },
              { "classification": "type", "resourceName": "ASSISTIVE_TECHNOLOGY_USED_INCORRECTLY", "displayName": "Tekniska hjälpmedel används felaktigt" },
              { "classification": "type", "resourceName": "EQUIPMENT_MISSING_OR_FAULTY", "displayName": "Utrustning saknas eller fungerar inte" }
            ]
          },
          {
            "classification": "category",
            "resourceName": "LEGAL_CERTAINTY",
            "displayName": "Brister i rättssäkerhet vid handläggning och genomförande",
            "labels": [
              { "classification": "type", "resourceName": "DOCUMENTATION_MISSING", "displayName": "Avsaknad av dokumentation" },
              { "classification": "type", "resourceName": "DECISION_NOT_EXECUTED", "displayName": "Beslut verkställs inte" },
              { "classification": "type", "resourceName": "INSUFFICIENT_CHILD_PERSPECTIVE", "displayName": "Bristande barnperspektiv" },
              { "classification": "type", "resourceName": "INSUFFICIENT_CONSENT_OR_INFORMATION", "displayName": "Bristande samtycke eller information" },
              { "classification": "type", "resourceName": "LACK_OF_OBJECTIVITY_AND_IMPARTIALITY", "displayName": "Brister i saklighet och opartiskhet" },
              { "classification": "type", "resourceName": "DELAYED_DOCUMENTATION", "displayName": "Försenad dokumentation" },
              { "classification": "type", "resourceName": "MEASURES_WITHOUT_LEGAL_BASIS", "displayName": "Åtgärder genomförs utan lagstöd, t ex begränsningsåtgärder" }
            ]
          },
          {
            "classification": "category",
            "resourceName": "EXECUTION_OF_SERVICES",
            "displayName": "Brister i utförandet av insatser",
            "labels": [
              { "classification": "type", "resourceName": "DEVIATION_FROM_IMPLEMENTATION_PLAN", "displayName": "Avvikelse från genomförandeplan" },
              { "classification": "type", "resourceName": "INSUFFICIENT_CONTINUITY", "displayName": "Bristande kontinuitet" },
              { "classification": "type", "resourceName": "NEGLECT", "displayName": "Försummelse / underlåtenhet" },
              { "classification": "type", "resourceName": "SERVICE_MISSED_OR_INCORRECT", "displayName": "Insats uteblir, utförs delvis, för sent eller fel" }
            ]
          },
          {
            "classification": "category",
            "resourceName": "FINANCIAL_ABUSE",
            "displayName": "Ekonomiska övergrepp (oegentligheter)",
            "labels": [
              { "classification": "type", "resourceName": "INSUFFICIENT_ACCOUNTING_OF_PRIVATE_FUNDS", "displayName": "Bristande redovisning av privata medel" },
              { "classification": "type", "resourceName": "UNAUTHORISED_USE_OF_CLIENT_FUNDS", "displayName": "Otillåten användning av brukarens medel" },
              { "classification": "type", "resourceName": "PRESSURE_TO_GIVE_AWAY_ASSETS", "displayName": "Påtryckningar att ge bort pengar eller egendom" },
              { "classification": "type", "resourceName": "THEFT_OF_MONEY_OR_BELONGINGS", "displayName": "Stöld av pengar eller ägodelar" }
            ]
          },
          {
            "classification": "category",
            "resourceName": "PHYSICAL_ABUSE",
            "displayName": "Fysiska övergrepp",
            "labels": [
              { "classification": "type", "resourceName": "VIOLENCE_BETWEEN_CLIENTS", "displayName": "Våld mellan brukare" },
              { "classification": "type", "resourceName": "VIOLENCE_BETWEEN_STAFF_AND_CLIENT", "displayName": "Våld mellan personal och brukare" }
            ]
          },
          {
            "classification": "category",
            "resourceName": "PSYCHOLOGICAL_ABUSE",
            "displayName": "Psykiska övergrepp",
            "labels": [
              { "classification": "type", "resourceName": "ABUSIVE_OR_CONTROLLING_BEHAVIOUR", "displayName": "Kränkande eller kontrollerande handlingar" }
            ]
          },
          { "classification": "category", "resourceName": "SEXUAL_ABUSE", "displayName": "Sexuella övergrepp" }
        ]
      }
    ]
  },
  {
    "classification": "report-type-root",
    "resourceName": "REPORT_TYPE",
    "displayName": "Rapporttyp",
    "labels": [
      { "classification": "report-type", "resourceName": "DEVIATION", "displayName": "Avvikelse" },
      { "classification": "report-type", "resourceName": "ABUSE", "displayName": "Missförhållande" }
    ]
  }
]'
# LABELS_HEALTHCAREDEVIATIONVOF='[ ... ]'   # per-namespace override

# Email integration   POST/PUT /{municipalityId}/{namespace}/email-integration-config
# Required fields: enabled, statusForNew, ignoreAutoReply, ignoreNoReply
EMAIL_INTEGRATION=""
# EMAIL_INTEGRATION_HEALTHCAREDEVIATIONVOF='{"enabled":true,"statusForNew":"NEW","ignoreAutoReply":true,"ignoreNoReply":true,"addSenderAsStakeholder":true,"stakeholderRole":"REPORTER","errandChannel":"EMAIL"}'

# Message exchange    POST/PUT /{municipalityId}/{namespace}/messageexchange-integration-config
MESSAGE_EXCHANGE_INTEGRATION=""

# =============================================================================================
# End of CONFIG - everything below is generic machinery.
# =============================================================================================

DRY_RUN=false
ONLY_NAMESPACES=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run) DRY_RUN=true; shift ;;
    --only)    ONLY_NAMESPACES="${2:-}"; shift 2 ;;
    -h|--help) sed -n '2,30p' "$0"; exit 0 ;;
    *)         echo "Unknown argument: $1 (see --help)" >&2; exit 2 ;;
  esac
done

die() { echo "ERROR: $*" >&2; exit 1; }
log() { echo "$*" >&2; }
require_cmd() { for c in "$@"; do command -v "$c" >/dev/null 2>&1 || die "Required command not found: $c"; done; }

created=0; updated=0; skipped=0; failed=0

# Extract a flat string field from a JSON object using bash only. Good enough for the OAuth2
# token response and for reading the name out of a configured request body.
json_value() {
  local field="$1" json="$2"
  if [[ "$json" =~ \"$field\"[[:space:]]*:[[:space:]]*\"(([^\"\\]|\\.)*)\" ]]; then
    local v="${BASH_REMATCH[1]}"
    v="${v//\\\//\/}"
    v="${v//\\\"/\"}"
    v="${v//\\\\/\\}"
    printf '%s' "$v"
  fi
}

# Escape a value for inclusion in a JSON string.
json_escape() {
  local v="$1"
  v="${v//\\/\\\\}"
  v="${v//\"/\\\"}"
  printf '%s' "$v"
}

ACCESS_TOKEN=""
AUTH_ENABLED=false
fetch_token() {
  local resp
  resp=$(curl -sS -X POST "$TOKEN_URL" -u "$CLIENT_ID:$CLIENT_SECRET" -d 'grant_type=client_credentials') \
    || die "Token request failed"
  ACCESS_TOKEN=$(json_value access_token "$resp")
  [[ -n "$ACCESS_TOKEN" ]] || die "No access_token in token response: $resp"
}

# api <method> <path> [body]
# Performs the request and sets RESP_CODE / RESP_BODY. Refreshes the token once on 401.
RESP_CODE=""; RESP_BODY=""
api() {
  local method="$1" path="$2" body="${3:-}" attempt="${4:-1}"
  local -a args=(-sS -X "$method" -w '\n%{http_code}' -H 'Accept: application/json')
  $AUTH_ENABLED && args+=(-H "Authorization: Bearer $ACCESS_TOKEN")
  [[ -n "$body" ]] && args+=(-H 'Content-Type: application/json' --data-binary "$body")

  local out
  out=$(curl "${args[@]}" "$BASE_URL$path") || { RESP_CODE="000"; RESP_BODY="curl failed"; return 0; }
  RESP_CODE="${out##*$'\n'}"
  RESP_BODY="${out%$'\n'*}"

  if [[ "$RESP_CODE" == "401" && "$AUTH_ENABLED" == true && "$attempt" -eq 1 ]]; then
    log "    token rejected - refreshing and retrying"
    fetch_token
    api "$method" "$path" "$body" 2
  fi
  return 0
}

# Resolve the value to use for a namespace: <base>_<namespace> when defined, otherwise <base>.
# Works for both scalars and arrays; the result is placed in the global RESOLVED / RESOLVED_ARR.
RESOLVED=""
resolve_scalar() {
  local base="$1" namespace="$2" specific="${1}_${2}"
  specific="${specific//-/_}"
  if [[ -n "${!specific+x}" && -n "${!specific}" ]]; then
    RESOLVED="${!specific}"
  else
    RESOLVED="${!base:-}"
  fi
}

RESOLVED_ARR=()
resolve_array() {
  local base="$1" namespace="$2" specific="${1}_${2}"
  specific="${specific//-/_}"
  local -n src="$base"
  if declare -p "$specific" >/dev/null 2>&1; then
    local -n override="$specific"
    RESOLVED_ARR=("${override[@]}")
  else
    RESOLVED_ARR=("${src[@]}")
  fi
}

# Singleton config (namespace-config, labels, email-integration-config, ...):
# create it when it is missing, replace it when it is already there.
#
#   upsert_singleton <label> <path> <body> [exists-marker]
#
# Existence is a 200 from GET; when <exists-marker> is given the response body must also contain
# that string (needed for endpoints that answer 200 with an empty payload, such as labels).
upsert_singleton() {
  local label="$1" path="$2" body="$3" marker="${4:-}"

  if $DRY_RUN; then
    log "    [dry-run] $label -> POST|PUT $BASE_URL$path"
    log "              $body"
    return 0
  fi

  local exists=false
  api GET "$path"
  case "$RESP_CODE" in
    200)
      if [[ -z "$marker" || "$RESP_BODY" == *"$marker"* ]]; then exists=true; fi
      ;;
    404) exists=false ;;
    *)
      log "    FAILED   $label - GET returned HTTP $RESP_CODE: $RESP_BODY"
      failed=$((failed + 1))
      return 0
      ;;
  esac

  if $exists; then
    api PUT "$path" "$body"
    if [[ "$RESP_CODE" =~ ^2 ]]; then
      log "    updated  $label"
      updated=$((updated + 1))
    else
      log "    FAILED   $label - PUT returned HTTP $RESP_CODE: $RESP_BODY"
      failed=$((failed + 1))
    fi
  else
    api POST "$path" "$body"
    if [[ "$RESP_CODE" =~ ^2 ]]; then
      log "    created  $label"
      created=$((created + 1))
    else
      log "    FAILED   $label - POST returned HTTP $RESP_CODE: $RESP_BODY"
      failed=$((failed + 1))
    fi
  fi
}

# Collection item (statuses, categories, roles, ...): these are keyed by a generated id, so
# existence is decided by looking for an item with the same name in the collection. Existing
# items are left as they are - delete them by hand if they need to change.
#
#   create_in_collection <label> <collection-path> <key-field> <body>
create_in_collection() {
  local label="$1" path="$2" key_field="$3" body="$4"
  local key
  key=$(json_value "$key_field" "$body")
  [[ -n "$key" ]] || die "$label: request body has no \"$key_field\" field: $body"

  if $DRY_RUN; then
    log "    [dry-run] $label '$key' -> POST $BASE_URL$path"
    log "              $body"
    return 0
  fi

  api GET "$path"
  if [[ ! "$RESP_CODE" =~ ^2 ]]; then
    log "    FAILED   $label '$key' - GET returned HTTP $RESP_CODE: $RESP_BODY"
    failed=$((failed + 1))
    return 0
  fi

  # Match on the exact key/value pair, ignoring whitespace variations in the response.
  local needle="\"$key_field\":\"$(json_escape "$key")\""
  local compact="${RESP_BODY// /}"
  if [[ "$compact" == *"$needle"* ]]; then
    log "    exists   $label '$key'"
    skipped=$((skipped + 1))
    return 0
  fi

  api POST "$path" "$body"
  if [[ "$RESP_CODE" =~ ^2 ]]; then
    log "    created  $label '$key'"
    created=$((created + 1))
  else
    log "    FAILED   $label '$key' - POST returned HTTP $RESP_CODE: $RESP_BODY"
    failed=$((failed + 1))
  fi
}

# seed_collection <array-name> <namespace> <label> <collection-sub-path> [key-field]
seed_collection() {
  local array_name="$1" namespace="$2" label="$3" sub_path="$4" key_field="${5:-name}"
  resolve_array "$array_name" "$namespace"
  [[ "${#RESOLVED_ARR[@]}" -gt 0 ]] || return 0

  local item
  for item in "${RESOLVED_ARR[@]}"; do
    [[ -n "$item" ]] || continue
    create_in_collection "$label" "/$MUNICIPALITY_ID/$namespace$sub_path" "$key_field" "$item"
  done
}

# seed_singleton <variable-name> <namespace> <label> <sub-path> [exists-marker]
seed_singleton() {
  local var_name="$1" namespace="$2" label="$3" sub_path="$4" marker="${5:-}"
  resolve_scalar "$var_name" "$namespace"
  [[ -n "$RESOLVED" ]] || return 0

  upsert_singleton "$label" "/$MUNICIPALITY_ID/$namespace$sub_path" "$RESOLVED" "$marker"
}

# ---------------------------------------------------------------------------------------------
# Everything that is configured for a single namespace. Add new resources here - one line per
# resource, with the data declared in the CONFIG block above.
# ---------------------------------------------------------------------------------------------
seed_namespace() {
  local namespace="$1" display_name="$2" short_code="$3" ttl="$4" access_control="$5" notify_reporter="$6"

  log "--- $namespace ---"

  # Namespace config must come first - the other resources are hung off it.
  local ns_body
  ns_body=$(printf '{"displayName":"%s","shortCode":"%s","notificationTTLInDays":%s,"accessControl":%s,"notifyReporter":%s}' \
    "$(json_escape "$display_name")" "$(json_escape "$short_code")" "${ttl:-40}" "${access_control:-false}" "${notify_reporter:-false}")
  upsert_singleton "namespace-config" "/$MUNICIPALITY_ID/$namespace/namespace-config" "$ns_body"

  seed_collection STATUSES          "$namespace" "status"           "/metadata/statuses"
  seed_collection CATEGORIES        "$namespace" "category"         "/metadata/categories"
  seed_collection ROLES             "$namespace" "role"             "/metadata/roles"
  seed_collection EXTERNAL_ID_TYPES "$namespace" "external id type" "/metadata/external-id-types"
  seed_collection CONTACT_REASONS   "$namespace" "contact reason"   "/metadata/contactreasons" "reason"

  # The labels endpoint answers 200 with an empty structure when no labels exist, so existence
  # is decided by the presence of a label in the response rather than by the status code.
  seed_singleton LABELS                       "$namespace" "labels"            "/metadata/labels" '"resourceName"'
  seed_singleton EMAIL_INTEGRATION            "$namespace" "email config"      "/email-integration-config"
  seed_singleton MESSAGE_EXCHANGE_INTEGRATION "$namespace" "msg exchange conf" "/messageexchange-integration-config"
}

# --- main ------------------------------------------------------------------------------------

require_cmd curl

[[ -n "$BASE_URL" ]] || die "BASE_URL must be set"
[[ "$BASE_URL" != */ ]] || die "BASE_URL must not end with a slash: $BASE_URL"
[[ -n "$MUNICIPALITY_ID" ]] || die "MUNICIPALITY_ID must be set"

log "=== Seeding SupportManagement configuration ==="
log "  Base URL        : $BASE_URL"
log "  Municipality id : $MUNICIPALITY_ID"
[[ -n "$ONLY_NAMESPACES" ]] && log "  Limited to      : $ONLY_NAMESPACES"
$DRY_RUN && log "  Mode            : DRY RUN (nothing is sent)"
log

if [[ -n "$TOKEN_URL" ]]; then
  [[ -n "$CLIENT_ID" && -n "$CLIENT_SECRET" ]] || die "TOKEN_URL is set - CLIENT_ID and CLIENT_SECRET are then required"
  AUTH_ENABLED=true
  if ! $DRY_RUN; then
    fetch_token
    log "Obtained OAuth2 token."
    log
  fi
else
  log "No TOKEN_URL configured - sending requests without authentication."
  log
fi

for line in "${NAMESPACES[@]}"; do
  IFS='|' read -r namespace display_name short_code ttl access_control notify_reporter <<< "$line"
  [[ -n "$namespace" ]] || continue
  [[ "$namespace" =~ ^[A-Za-z0-9_-]+$ ]] || die "Invalid namespace '$namespace' (allowed: A-Z, a-z, 0-9, - and _)"

  if [[ -n "$ONLY_NAMESPACES" && ",$ONLY_NAMESPACES," != *",$namespace,"* ]]; then
    continue
  fi

  seed_namespace "$namespace" "$display_name" "$short_code" "$ttl" "$access_control" "$notify_reporter"
done

log
log "=== Summary ==="
log "  Created : $created"
log "  Updated : $updated"
log "  Existed : $skipped"
log "  Failed  : $failed"

[[ "$failed" -eq 0 ]] || exit 1
