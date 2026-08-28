# Elasticsearch PoC — SupportManagement

Validates using Elasticsearch for querying dynamic `jsonParameters` that can't be efficiently searched in MariaDB (stored as longtext JSON blobs).

## Architecture

```
MariaDB (structured data)  ←→  SupportManagement  ←→  Elasticsearch (jsonParameters only)
         ↑                                                      ↑
   spring-filter (turkraft)                          query_string (Lucene syntax)
   status, category, title...                        jsonParameters.facility.data.*
```

- **MariaDB** remains the single source of truth for all structured fields
- **Elasticsearch** only indexes `jsonParameters` — the schemaless JSON data
- **Hybrid queries**: ES returns matching errand IDs → MariaDB narrows with structured filters + access control

## Quick Start

```bash
./tools/elasticsearch-poc/run-poc.sh
```

This builds the app, starts 3 Docker containers (MariaDB, Elasticsearch 9.0, SupportManagement), seeds 50,000 errands with jsonParameters across 8 schema versions, reindexes into ES, and runs performance measurements.

## Dataset

- **50,000 errands** in namespace `ES-POC`, municipality `2281`
- **8 facility schema versions** with different key names for the same concept:
  - v1.0: `facilityId`, v2.0: `anläggningsId`, v3.0: `facility_id`, v4.0: `anlaggning`
  - v5.0: `anlaggningsNr`, v6.0: `fastighetsId`, v7.0: `propertyId`, v8.0: `objektId`
- **500 unique facility IDs** (`FAC-0001` through `FAC-0500`)
- Additional parameter types: inspection (30%), contact (20%), environment (15%), maintenance (10%)

## Query Examples

The hybrid query flow: client passes `jsonParameterFilter` to the existing errands endpoint. ES searches jsonParameters, returns matching errand IDs, and MariaDB narrows with structured filters.

```bash
# Hybrid: spring-filter on status + ES on jsonParameters
curl 'http://localhost:8080/2281/ES-POC/errands?filter=status:%27OPEN%27&jsonParameterFilter="FAC-0001"'

# Direct ES query examples (for testing/debugging)
# Field-specific (note: quote values with hyphens for exact match)
curl -X POST 'http://localhost:9200/errand/_search' \
  -H 'Content-Type: application/json' \
  -d '{"query":{"bool":{"must":[{"query_string":{"query":"jsonParameters.facility.data.facilityId:\"FAC-0001\"","default_field":"jsonParameters.*","lenient":true}}],"filter":[{"term":{"namespace":"ES-POC"}},{"term":{"municipalityId":"2281"}}]}}}'

# Cross-schema (finds FAC-0001 regardless of key name)
curl -X POST 'http://localhost:9200/errand/_search' \
  -H 'Content-Type: application/json' \
  -d '{"query":{"bool":{"must":[{"query_string":{"query":"\"FAC-0001\"","default_field":"jsonParameters.*","lenient":true}}],"filter":[{"term":{"namespace":"ES-POC"}},{"term":{"municipalityId":"2281"}}]}}}'
```

## Re-run Measurements

```bash
./tools/elasticsearch-poc/test-queries.sh
```

## Cleanup

```bash
cd tools/elasticsearch-poc && docker-compose down -v
```

