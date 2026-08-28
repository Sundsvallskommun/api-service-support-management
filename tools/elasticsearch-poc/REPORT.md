# Elasticsearch PoC — Hybrid Search for Dynamic JSON Parameters

**Service:** SupportManagement / dept44
**Date:** March 2026
**Status:** PoC Complete

---

## 1. Sammanfattning

Denna PoC validerar att Elasticsearch kan användas som komplement till MariaDB i SupportManagement för att söka i dynamiska `jsonParameters` — schemalösa JSON-blobbar som lagras som `LONGTEXT` och inte kan sökas effektivt med SQL.

### Nyckelresultat

|          Mätvärde          |    Resultat     |                Kommentar                |
|----------------------------|-----------------|-----------------------------------------|
| Dataset                    | 700 000 ärenden | 107 256 jsonParameters                  |
| Reindex-tid (700k)         | ~6 minuter      | Batch 500, session-clear mellan batchar |
| Exakt fältsökning (ES)     | 2–3 ms snitt    | `match_phrase` på specifikt fält        |
| Cross-schema-sökning (ES)  | 4–5 ms snitt    | Hittar värde oavsett nyckelnamn         |
| Fritextsökning (ES)        | 9–22 ms snitt   | Söker alla jsonParameter-fält           |
| Hybrid: status + jsonParam | 69–89 ms snitt  | spring-filter + ES, genom app-endpoint  |
| Baslinje enbart MariaDB    | 19 ms snitt     | Utan ES, ren spring-filter              |
| Noll-resultat (hybrid)     | 12 ms snitt     | ES returnerar tom lista → `Page.empty`  |

---

## 2. Problemet

### 2.1 jsonParameter-utmaningen

SupportManagement lagrar ärendemetadata i MariaDB. De flesta fält (status, kategori, titel, intressenter) är välstrukturerade kolumner som kan sökas med SQL via turkraft spring-filter.

Tabellen `json_parameter` lagrar dock **godtycklig JSON-data som longtext**. Denna data representerar klientdefinierade scheman — anläggningsinformation, inspektionsrapporter, avvikelserapporter — som varierar per namespace och utvecklas över tid. SQL kan inte söka effektivt inuti dessa JSON-blobbar.

### 2.2 Schemaevolutionsproblemet

JSON-scheman utvecklas när klienter itererar sina datamodeller. Ett fält som heter `facilityId` i v1 kan bli `anläggningsId` i v2, `facility_id` i v3 och `objektId` i v8. En sökning efter "FAC-0001" måste hitta matchningar oavsett vilken nyckelnamnsvariant som använts.

PoC:n simulerar detta med **8 olika nyckelnamn** för samma koncept, fördelat över 700 000 ärenden.

### 2.3 Krav

- Söka inuti godtyckliga JSON-strukturer lagrade i MariaDB longtext
- Hitta värden över schemaversioner med olika nyckelnamn (cross-schema-sökning)
- Integrera med det befintliga spring-filter-flödet (hybridfrågor)
- Ingen regression på befintliga endpoints eller testtäckning (≥85% JaCoCo)
- ES-fel får inte påverka MariaDB-operationer (graceful degradation)
- Feature-flaggat: ES är valfritt och kan inaktiveras utan kodändringar

---

## 3. Arkitektur

### 3.1 Hybridflöde

```
Klientförfrågan:
  GET /2281/NS/errands?filter=status:'OPEN'&jsonParameterFilter="FAC-0001"

1. ES-fråga:    namespace=NS, municipalityId=2281, jsonParameters.* = "FAC-0001"
                → returnerar ärende-ID:n: [id-1, id-5, id-9]

2. MariaDB:     namespace=NS AND municipalityId=2281 AND status='OPEN'
                AND id IN (id-1, id-5, id-9)
                + access control + paginering
                → returnerar Page<Errand>

3. Svar:        Standard Page<Errand> — identiskt med befintligt API-kontrakt
```

ES tillfrågas **först** eftersom det är det enda systemet som kan söka inuti JSON-blobbar. MariaDB avgränsar sedan resultaten med strukturerade filter, åtkomstkontroll och paginering.

### 3.2 Vad bor var

|         Ansvar         |                           MariaDB                            |         Elasticsearch         |
|------------------------|--------------------------------------------------------------|-------------------------------|
| Strukturerade fält     | Titel, status, kategori, typ, prioritet, intressenter, datum | —                             |
| Åtkomstkontroll        | Labels, behörigheter, rollbaserad filtrering                 | —                             |
| Paginering & sortering | Spring Data Pageable                                         | —                             |
| jsonParameters         | Lagring (longtext, source of truth)                          | Sökindex (dynamisk mappning)  |
| Cross-schema-sökning   | —                                                            | `query_string` över alla fält |

### 3.3 Feature-flagga & graceful degradation

```yaml
elasticsearch:
  enabled: true  # eller false för att inaktivera helt
```

Alla ES-komponenter använder `@ConditionalOnProperty` och `@Nullable`-injection. ES-undantag fångas och loggas — de propagerar aldrig till anroparen.

---

## 4. Implementation

### 4.1 ES-dokumentmodell (`JsonParameterDocument`)

Enbart jsonParameters indexeras. Dokumentet är medvetet litet:

|       Fält       |        ES-typ         |                       Syfte                       |
|------------------|-----------------------|---------------------------------------------------|
| `id`             | Keyword (@Id)         | Ärende-UUID — kopplar tillbaka till MariaDB       |
| `namespace`      | Keyword               | Avgränsar frågor per namespace                    |
| `municipalityId` | Keyword               | Avgränsar frågor per kommun                       |
| `jsonParameters` | Object (dynamic=TRUE) | Flat-mappad JSON från alla `json_parameter`-rader |

### 4.2 JSON-parameter-mappning

`JsonParameterDocumentMapper` transformerar varje ärendes jsonParameters till en flat map:

```
jsonParameters.{parameterKey}.schemaId  → schemaversion
jsonParameters.{parameterKey}.data.{…}  → parsad JSON-data, dynamiskt mappad av ES
```

**Exempel:** Ärende med "facility" jsonParameter (schema v1.0):

```json
{
  "id": "errand-uuid",
  "namespace": "ES-POC",
  "municipalityId": "2281",
  "jsonParameters": {
    "facility": {
      "schemaId": "2281_facility_1.0",
      "data": {
        "facilityId": "FAC-0001",
        "address": "Storgatan 1",
        "type": "building"
      }
    }
  }
}
```

ES dynamisk mappning skapar automatiskt sökbara fält för varje nyckel. **Ingen schemamigration behövs.**

### 4.3 Sökimplementation

`ElasticsearchSearchService` använder `query_string` med Lucene-syntax på `defaultField("jsonParameters.*")`. Flaggan `lenient(true)` gör att typkonflikter (t.ex. textsökning i datumfält) hoppas över.

Frågor avgränsas alltid av `namespace` och `municipalityId` som filter-klausuler. Sökningen returnerar max 10 000 ärende-ID:n som skickas till MariaDB:s JPA-fråga som `IN`-klausul.

### 4.4 Indexeringsstrategi

- **Create/Update:** Dokument indexeras synkront efter `ErrandService.createErrand()` / `updateErrand()`
- **Delete:** ES-dokument tas bort i `deleteErrand()`
- **Full reindex:** `ElasticsearchReindexRunner` (ApplicationRunner, opt-in via `elasticsearch.reindex.on-startup=true`)
- **Minneshantering:** Hibernates first-level cache rensas mellan batchar för stora dataset
- **Selektiv indexering:** Enbart ärenden med jsonParameters indexeras

### 4.5 Nyckelfiler

|                               Fil                                |                      Syfte                      |
|------------------------------------------------------------------|-------------------------------------------------|
| `integration/elasticsearch/model/JsonParameterDocument.java`     | ES-dokumentmodell                               |
| `integration/elasticsearch/JsonParameterDocumentRepository.java` | Spring Data ES-repository med `@CircuitBreaker` |
| `integration/elasticsearch/ElasticsearchSearchService.java`      | Frågebyggare, returnerar matchande ärende-ID:n  |
| `integration/elasticsearch/ElasticsearchIndexService.java`       | Index/delete/reindex-operationer                |
| `integration/elasticsearch/ElasticsearchReindexRunner.java`      | Startup-reindex (opt-in)                        |
| `service/mapper/JsonParameterDocumentMapper.java`                | ErrandEntity → JsonParameterDocument            |
| `service/ErrandService.java`                                     | Hybridfråge-integration (`findErrands`)         |

---

## 5. Prestandaresultat

### 5.1 Testmiljö

- **Docker-containrar:** MariaDB 10.6 (512MB buffer pool), Elasticsearch 9.0.0 (1GB heap), SupportManagement-app
- **Dataset:** 700 000 ärenden, 107 256 jsonParameters över 20+ namespaces
- **Datakälla:** Riktig produktionsdump (4 804 ärenden) uppskalat med syntetisk data
- **Schemaevolution:** 8 facility-nyckelnamnsvarationer, 4 ytterligare parametertyper
- **ES-konfiguration:** Single-node, single-shard, inga replikor (PoC)

### 5.2 Riktig data — Avvikelserapporter (vård/omsorg)

Frågor mot riktiga `avvikelse-plats-handelse`-jsonParameters:

|                    Fråga                    | Träffar | Snitt |  P95  |
|---------------------------------------------|---------|-------|-------|
| Organisationsnamn: "VOF HOS Korttidsboende" | 4 260   | 22 ms | 48 ms |
| Organisationsnamn: "Hemtjänst"              | 2 095   | 9 ms  | 11 ms |
| Organisationsnamn wildcard: VOF*            | 4 260   | 16 ms | 19 ms |
| Fritext: "Korttidsboende" (alla fält)       | 2 165   | 10 ms | 12 ms |
| Fritext: "Hemtjänst" (alla fält)            | 2 095   | 11 ms | 12 ms |
| Org-ID exakt: 7514                          | 2       | 2 ms  | 3 ms  |

### 5.3 Schemaevolution — Facility-ID över 8 nyckelnamn

|                    Fråga                    | Träffar | Snitt |  P95  |
|---------------------------------------------|---------|-------|-------|
| Fältspecifik: `facilityId:"FAC-0001"`       | 5       | 2 ms  | 3 ms  |
| Cross-schema: `"FAC-0001"` (alla 8 nycklar) | 33      | 5 ms  | 7 ms  |
| Cross-schema: `"FAC-0500"` (alla 8 nycklar) | 40      | 4 ms  | 7 ms  |
| Inspektör: "Anna Svensson"                  | 2 120   | 12 ms | 13 ms |
| Adress: Storgatan                           | 6 367   | 21 ms | 24 ms |

### 5.4 Generell prestanda

|                  Fråga                  | Träffar | Snitt |  P95  |
|-----------------------------------------|---------|-------|-------|
| Wildcard: FAC-00*                       | 3 195   | 13 ms | 15 ms |
| Oquoterad: FAC-0001 (tokeniserad, bred) | 10 000+ | 35 ms | 42 ms |
| Inga resultat: obefintligt värde        | 0       | 2 ms  | 3 ms  |

### 5.5 Cross-schema-verifiering

Sökning efter `"FAC-0001"` över alla 8 nyckelnamnsvarationer:

|  Version   |  Nyckelnamn   | Träffar |
|------------|---------------|---------|
| v1.0       | facilityId    | 5       |
| v1.1       | anläggningsId | 3       |
| v2.0       | facility_id   | 2       |
| v2.1       | anlaggning    | 7       |
| v3.0       | anlaggningsNr | 1       |
| v3.1       | fastighetsId  | 7       |
| v4.0       | propertyId    | 8       |
| v4.1       | objektId      | 0       |
| **Totalt** |               | **33**  |

**Resultat:** Cross-schema-sökning hittade **33 träffar totalt**, matchande summan av individuella fältspecifika sökningar. En enda fråga hittar värdet oavsett nyckelnamn.

### 5.6 Tokeniseringseffekt

|                         Fråga                          | Träffar |
|--------------------------------------------------------|---------|
| Oquoterad `FAC-0001` (tokeniseras till "fac" + "0001") | 10 000+ |
| Quoterad `"FAC-0001"` (exakt frasmatchning)            | 33      |

**Quotera alltid värden med bindestreck eller specialtecken** för att undvika falska positiver.

### 5.7 Hybridfrågor — MariaDB + ES genom app-endpointet

Dessa mätningar går genom hela produktionsflödet: `GET /errands?filter=...&jsonParameterFilter=...`.
ES söker jsonParameters → returnerar ärende-ID:n → MariaDB filtrerar med spring-filter + åtkomstkontroll + paginering.

|                     Fråga                      | Träffar | Snitt  |  P95   |
|------------------------------------------------|---------|--------|--------|
| Enbart jsonParam: "Storgatan"                  | 1 331   | 177 ms | 700 ms |
| Hybrid: status=ASSIGNED + "Storgatan"          | 77      | 69 ms  | 86 ms  |
| Hybrid: status=NEW\|ASSIGNED + "Anna Svensson" | 48      | 89 ms  | 156 ms |
| Hybrid: status=ASSIGNED + "FAC-0001"           | 0       | 21 ms  | 108 ms |
| Inga resultat (obefintligt jsonParam-värde)    | 0       | 12 ms  | 54 ms  |
| Baslinje (enbart MariaDB, inget ES)            | 8 173   | 19 ms  | 26 ms  |

**Observationer:**
- Hybridfrågor adderar ~50–70 ms overhead jämfört med ren MariaDB (19 ms baslinje)
- Overheaden kommer från ES-frågan + `IN`-klausul med returnerade ID:n
- Första anropet ("Storgatan", 1 331 träffar) har högre latens pga stor `IN`-lista
- Inga resultat-frågor (ES returnerar tom lista → `Page.empty`) är snabba (~12 ms)
- Access control-bypass fungerar automatiskt för namespaces utan konfigurerad åtkomstkontroll

---

## 6. Användning

### 6.1 Hybridfråga (produktionsanvändning)

Lägg till `jsonParameterFilter` på det befintliga ärende-endpointet:

```
GET /2281/NS/errands?filter=status:'OPEN'&jsonParameterFilter="FAC-0001"
```

Parametern accepterar Elasticsearch `query_string`-syntax (Lucene-frågespråk).

### 6.2 Cross-schema-sökning

Hitta ett värde oavsett nyckelnamn (inget fältprefix):

```
jsonParameterFilter="FAC-0001"
```

### 6.3 Fältspecifik sökning

Rikta mot ett specifikt fält (kräver full sökväg inklusive `data`):

```
jsonParameterFilter=jsonParameters.facility.data.facilityId:"FAC-0001"
```

### 6.4 Fritextsökning

Sök efter en term i allt JSON-innehåll:

```
jsonParameterFilter="Korttidsboende"
```

### 6.5 Köra PoC:n lokalt

```bash
./tools/elasticsearch-poc/run-poc.sh
```

---

## 7. Överväganden & öppna frågor

### 7.1 Schemaevolutionsstrategi

PoC:n validerar att cross-schema-sökning fungerar. Dock bygger det på oquoterad (cross-field) sökning, som är bred av naturen.

**Alternativ A: Acceptera cross-field-sökning.** Klienter söker utan fältprefix. Enkelt men kan ge falska positiver om samma värde förekommer i orelaterade fält.

**Alternativ B: Normalisera vid indexering.** Mappa utvecklande nyckelnamn till kanoniska namn med json-schema-tjänsten. T.ex. `facilityId`, `anläggningsId`, `objektId` → `facility_id`. Kräver att schema-tjänsten spårar fältsläktskap över versioner.

**Valet beror på om json-schema-tjänsten spårar fältnamnsutveckling. Behöver klientinput.**

### 7.2 Bindestreck i fältnamn

Riktig produktionsdata använder parameternycklarna med bindestreck (t.ex. `avvikelse-plats-handelse`). ES `query_string` tolkar bindestreck som subtraktion. Fältspecifika frågor på dessa sökvägar kräver `match`- eller `match_phrase`-frågor istället.

### 7.3 Gräns på 10 000 ID:n

Söktjänsten returnerar max 10 000 ärende-ID:n. Breda sökningar kan bli avkortade. `search_after` kan användas vid behov.

### 7.4 Indexkonsistens

ES-indexet uppdateras synkront efter MariaDB-skrivningar. Om ES är nere blir indexet inaktuellt. För produktion, överväg:

- Asynkron event-driven indexeringspipeline (CDC/outbox-mönster)
- Schemalagt avstämningsjobb
- Övervakning av indexeringsfel

### 7.5 Produktionsdriftsättning

- Använd befintlig OpenShift ES-installation
- Aktivera TLS och autentisering
- Konfigurera replikor för hög tillgänglighet
- Dimensionera shards baserat på datavolym

### 7.6 Plattformsövergripande tillämpbarhet

Mönstret kan extraheras till en delad dept44-starter (`dept44-starter-elasticsearch`):

- Auto-konfiguration med circuit breaker
- Bas-dokumentklass med namespace/municipalityId-avgränsning
- Reindex-runner med batchbearbetning och minneshantering
- Integration med turkraft spring-filter

---

## 8. Slutsats

- **Prestanda är utmärkt** — sub-5ms för exakta sökningar, sub-20ms för fritext, på 700k ärenden
- **Cross-schema-sökning fungerar** — en enda fråga hittar värden över alla 8 nyckelnamnsvarationer
- **Integrationen är ren** — befintligt API-kontrakt oförändrat, ES är feature-flaggat och valfritt
- **Ingen regression** — alla 1 659+ enhetstester och 142+ integrationstester passerar
- **Graceful degradation** — ES-fel fångas och loggas, påverkar aldrig MariaDB

Hybridarkitekturen — MariaDB för strukturerad data, ES för schemalös JSON — passar naturligt för SupportManagement och kan tjäna som referensimplementation för andra dept44-tjänster.
