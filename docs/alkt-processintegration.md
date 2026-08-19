# SupportManagement ↔ pw-alkt — processintegration

Designunderlag för ALKT-namespacets myndighetsprocess i Operaton, körd av tjänsten
[pw-alkt](https://github.com/Sundsvallskommun/pw-alkt). Innehåller datamodell, API-kontrakt,
regler för lås och loop-skydd, samt uppgiftsnedbrytning med acceptanskriterier.

**Jira:** DRAKEN-4692 (story) med DRAKEN-4694…4707 som deluppgifter. Avsnittsnumren nedan
refereras från respektive deluppgift.

**Öppen fråga:** när RabbitMQ är produktionsklar (§2.4). Blockerar inte T1–T8.

---

## 0. Beslutslogg

| # | Beslut | Valdes bort | Skäl |
|---|---|---|---|
| 1 | Outbox + REST-push via WSO2 + AFTER_COMMIT-nudge | Direkt RabbitMQ; SSE; polling | Outboxen behövs oavsett (dual-write). **AMQP är målbilden** |
| 2 | Publicering hookad i `EventService.createErrandEvent` | Hook i `ErrandService` + revisionsdiff | Intaget skapar inga revisioner ⇒ processen hade varit blind för komplettering |
| 3 | Trigger-filter på `(EventType, EventSubType)` | JsonDiff på attribut-paths | Fungerar på alla vägar. Mekanismen finns för notisprenumeranter |
| 4 | Konfiguration i befintlig `namespace_config` | Ny tabell; `application.yml` | Nycklarna driver redan beteende. Flervärd per nyckel. Cache + CRUD finns |
| 5 | Etikett → process via `metadata_label_attribute[processKey]`, utan trädtraversering | Path-matchning; subträdsarv | Subträd låter en process tillkomma implicit vid metadata-omflyttning |
| 6 | 1-1 mellan ärende och processtyp | Flera processtyper per ärende | Tillsyn är ett nytt ärende. Ger DB-constraint i stället för disciplin |
| 7 | Låset som kolumner på `errand_process_instance` | Egen låstabell | Låset hålls alltid av den aktiva instansen. Atomicitet gratis |
| 8 | **Ett** skrivendpoint bär tillstånd, aktiviteter och låseffekt | Separata endpoints | Färre anrop, en transaktion, omöjligt att hålla lås utan att ha rapporterat |
| 9 | Ingen force-unlock | `?force=true` | Fyra automatiska utgångar. SM saknar rollbaserad auktorisation |
| 10 | Inga processvariabler för idempotens | `updateAvailable`, versionsräknare | Racy read-modify-write. Skyddet finns strukturellt |
| 11 | `errand.status` frikopplat från processens faser | Process äger status | Skilda begrepp. Listvyn läser processläget ur `errand.process` |
| 12 | SM:s `phase`/`errand_phase` används inte för ALKT | Återanvänd fasmodellen | Processmodellen ägs av BPMN |
| 13 | pw-alkt:s `start`/`update`-endpoints tas bort | Deprecation | Inga konsumenter. Två startvägar ⇒ någon använder fel |
| 14 | **`ProcessStatus` med `releasesLock()` och `isTerminal()` som metoder** | Två fristående mappningstabeller | De styr *olika* saker och får inte drifta isär — se §4.1 |
| 15 | **Fem statusvärden**, `START_FAILED` blir `FAILED` + `errorCode` | Åtta värden | Skillnaden syns redan på `processInstanceId IS NULL` |
| 16 | **Modelleringsregel: inga parallella grenar som muterar ärendet** | Refcount på låset | Se §6.5. Refcount är eskaleringsvägen om regeln inte räcker |

---

## 1. Verifierade fakta som designen vilar på

### 1.1 `EventService` är den universella kanalen

`EventService.createErrandEvent` anropas från 13 ställen:

| Väg | Anropsställe |
|---|---|
| PATCH / create / delete | `ErrandService.java:142, 208, 251` |
| Bilagor | `ErrandAttachmentService.java:105, 151, 168` |
| Handover | `HandoverService.java:326, 332` |
| Konversationer | `MessageExchangeSyncService.java:63` |
| E-postintag | `EmailReaderWorker.java:149` |
| Webbmeddelanden | `WebMessageCollectorWorker.java:80` |
| Suspension | `SuspensionWorker.java:46` |

**Revisioner skapas bara på fem ställen** — `ErrandService:139, 204`, `ErrandAttachmentService:102, 148, 165`. Intaget (`EmailReaderWorker:145`, `MessageExchangeSyncService:109`) sparar direkt via repository. Därför kan trigger-filtret inte bygga på revisionsdiff.

`EventSubType` (befintlig enum): `ATTACHMENT, DECISION, ERRAND, HANDOVER_IN, HANDOVER_OUT, MESSAGE, NOTE, SYSTEM, SUSPENSION`. `EventType` kommer från eventlog-specen.

### 1.2 Leveransmaskineriet finns redan

`EventService:109-121` (outbox-rad), `NotificationDispatchEntity:20-24, 62-74`, `NotificationDispatchScheduler:27-53` (ShedLock + hälsoindikator), `NotificationDispatchWorker:112-160` (retry/backoff/dead letter), `:168-174` (eventtypfilter), `:162-166` (hoppa över upphovet).

Ärv **inte**: `NotificationDispatchRepository.findProcessable:14-23` saknar `LIMIT`; `NotificationDispatchWorker:24` har `TRANSACTION_BUFFER_SECONDS = 10`. Och `cleanUpDeadLetters:48-49` **raderar** dead letters efter 7 dagar utan möjlighet att köra om dem — se §8.3.

### 1.3 Chokepoint för errand-skrivningar

Allt går via `AccessControlService.getErrand(...)`, och **skrivvägar skickar `RW` ensamt, läsvägar `R, RW`**.

Skriv: `ErrandService:178, 219`, `ErrandParameterService:47, 74, 94`, `ErrandJsonParameterService:63, 104`, `ErrandAttachmentService:78, 133`, `CommunicationService:122, 169, 197, 209`, `ConversationService:87, 126, 153, 239`, `ErrandNoteService:54, 91, 111`, `NotificationService:82, 114, 123, 135`.

Undantag: **`HandoverService:135`** hämtar utan filter men muterar källärendet på `:309`. Schemaläggarna går förbi chokepointen helt — vilket är önskvärt (§6.4).

### 1.4 Revisionssnapshotten är hela entiteten

`RevisionMapper:18-24` Gson-serialiserar hela `ErrandEntity`; `CircularReferenceExclusionStrategy` tar bara bakåtreferenser. **Två hårda regler:** inga nya kolumner på `errand`, inga nya `@OneToMany` på `ErrandEntity`. Dessutom gör `ErrandService:184` `OPTIMISTIC_FORCE_INCREMENT` — en kolumn på `errand` hade invaliderat varje ETag.

### 1.5 `namespace_config` driver redan beteende

`ConfigPropertyExtractor:15-19` har `ACCESS_CONTROL` och `NOTIFY_REPORTER` — beteendeflaggor. `namespace_config_value` (`V1_19:2-7`): `key`, `value text`, `type` (`BOOLEAN|STRING|INTEGER`), unikt på **`(namespace_config_id, key, value)`** ⇒ flervärd per nyckel. `@ElementCollection(EAGER)`, cachad via `namespaceConfigCache`.

**Fallgrop:** `getNullableValue:36` gör `.findFirst()` och ignorerar tyst resten.

### 1.6 Övrigt

- `metadata_label_attribute` (`V1_37`): fri key/value, unik på `(metadata_label_id, key)`. Nycklar **inte** whitelistade (`ValidLabelAttributesConstraintValidator:34-42`).
- `ErrandPhaseService:38-40, 62-65` returnerar direkt när ingen fas finns ⇒ oanvänd fasmodell kostar noll.
- `errand.id` är `varchar(255)` (`V1_0:136`).
- pw-alkt är stateless. `AbstractTaskWorker:38-44` varnar för races vid skrivning av processvariabler. `alkt-ansokan.bpmn` innehåller **inget** `updateAvailable`; `clearUpdateAvailable` har **inga anropare**.
- Varje PW-tjänst har eget API i WSO2 ⇒ en OAuth2-registrering per PW-tjänst.
- SM saknar Testcontainers-uppsättning; `application-it.yml` har `schema-generation: validate`.
- `truncate.sql` måste utökas med varje ny tabell.

---

## 2. Arkitektur

### 2.1 Flödet

```
Handläggare/intag -> SM -> EventService -> ProcessEventPublisher -> process_event_outbox
                                                                          |
                                          AFTER_COMMIT-nudge / cron -> ProcessEventRelay
                                                                          |
                                                                    POST errand-events (WSO2)
                                                                          |
                                                                       pw-alkt
                                                                     /          \
                                                        start process        correlate message
                                                                     \          /
                                                                       Operaton
                                                                          |
                                                                   external task -> worker
                                                                          |
                                              PUT process-instances (tillstånd + aktiviteter + lås)
                                                                          |
                                                                         SM
```

### 2.2 Publiceringsregeln

`ProcessEventPublisher.publish(errandEntity, eventType, eventSubType, executedBy, requestGroupId)` anropas sist i `EventService.createErrandEvent`, i samma transaktion. `sendNotification`-flaggan är irrelevant — processevent är inte notiser.

```
1. PROCESS_CONSUMER för (municipalityId, namespace)?      nej -> return
2. Aktivt processlås på ärendet?                          ja  -> return        (loop-skydd, lager 1)
3. Levererade event för ärendet i fönstret > tröskel?      ja  -> ERROR-aktivitet, return  (lager 4)
4. eventSubType i PROCESS_TRIGGER?                        nej -> return        (lager 3)
5. processKey ur etiketterna                              0 -> return; >1 -> ERROR-aktivitet, return
6. INSERT process_event_outbox
```

Steg 1 är en cachad map-uppslagning. Namespace utan process betalar inget mer.

### 2.3 Latens

`@TransactionalEventListener(AFTER_COMMIT, fallbackExecution = true)` + `@Async("processEventExecutor")` (core 2 / max 4 / kö 500, **`AbortPolicy`**). Mönstret finns i `SubscriptionService:84-86`.

Cron `0 * * * * *` som nät, samma kodväg, buffert 5 s **endast** där. Ordning per ärende via `@Lock(PESSIMISTIC_WRITE)` på radgruppen sorterad på `created, id`.

### 2.4 Transport

**Nu REST**, eftersom outboxen byggs ändå, brokern inte är bevisat driftklar och SM saknar Testcontainers-infrastruktur.

**Målbild AMQP:** varje ny PW-tjänst kräver annars registrering + url + Feign-mål i SM. Migreringen är **en metod** — `ProcessEventDelivery.deliver(row)`. Varje REST-konsument som byggs innan bytet är kastat arbete.

*Driftklar =* quorum queues ≥3 noder, DLX/DLQ med `x-delivery-limit`, vhost per miljö, användare per tjänst med permission-regex, TLS, övervakning av kölängd/obekräftade/DLQ-djup/nodstatus, dokumenterad återställning.

---

## 3. Datamodell

### 3.1 DDL — `V1_47__add_process_integration_tables.sql`

```sql
-- Diagnostikfragan "vilka labels startar en process?" - befintligt index ar (metadata_label_id, key)
create index if not exists idx_metadata_label_attribute_key
    on metadata_label_attribute (`key`);

-- 1. Outbox. Medvetet UTAN FK mot errand: ett DELETE-event maste overleva att arendet raderas.
create table if not exists process_event_outbox (
    id                varchar(36)  not null,
    municipality_id   varchar(8)   not null,
    namespace         varchar(32)  not null,
    errand_id         varchar(36)  not null,
    process_key       varchar(128) not null,
    event_type        varchar(64)  not null,   -- CREATE | UPDATE | DELETE
    event_sub_type    varchar(64)  not null,   -- ERRAND | MESSAGE | ATTACHMENT | ...
    executed_by       varchar(255),            -- X-Sent-By-varde, for loop-filtret
    request_group_id  varchar(36),
    created           datetime(3)  not null,
    delivered_at      datetime(3),             -- soft delete; barer nodbromsens rakning
    retry_count       int          default 0 not null,
    next_retry_at     datetime(3),
    dead_letter       bit          default 0 not null,
    primary key (id)
) engine=InnoDB;

create index if not exists idx_peo_dispatch on process_event_outbox (delivered_at, dead_letter, next_retry_at, created);
create index if not exists idx_peo_guard    on process_event_outbox (errand_id, created);

-- 2. Processinstans, inklusive lasets tillstand.
create table if not exists errand_process_instance (
    id                    varchar(36)  not null,
    errand_id             varchar(255) not null,
    municipality_id       varchar(8)   not null,
    namespace             varchar(32)  not null,
    process_service       varchar(64)  not null,   -- 'pw-alkt'
    process_key           varchar(128) not null,   -- 'alkt-ansokan'
    process_instance_id   varchar(64),             -- null nar starten aldrig lyckades
    process_status        varchar(32)  not null,   -- RUNNING|WAITING|RETRYING|COMPLETED|FAILED
    current_activity_id   varchar(255),
    current_activity_name varchar(255),
    error_code            varchar(64),
    error_message         varchar(2048),
    -- Laset. Aktivt endast nar lock_expires > now().
    lock_token            varchar(36),
    lock_expires          datetime(3),
    lock_reason           varchar(512),
    lock_owner_task_id    varchar(64),             -- Operatons externalTaskId, se 6.5
    lock_first_acquired   datetime(3),             -- for max-total-taket
    lock_renew_count      int          default 0 not null,
    started               datetime(3),
    ended                 datetime(3),
    -- 1 medan instansen lever, NULL nar den ar terminal. NULL ar distinkt i unika index
    -- -> godtyckligt manga historiska instanser, hogst EN levande per arende.
    active_marker         tinyint      null,
    created               datetime(3)  not null,
    modified              datetime(3),
    primary key (id),
    constraint uq_epi_process_instance_id   unique (process_instance_id),
    constraint uq_epi_one_active_per_errand unique (errand_id, active_marker)
) engine=InnoDB;

create index if not exists idx_epi_errand_id  on errand_process_instance (errand_id);
create index if not exists idx_epi_lock_sweep on errand_process_instance (lock_expires);

alter table if exists errand_process_instance
    add constraint fk_epi_errand_id foreign key (errand_id) references errand (id) on delete cascade;

-- 3. Append-only faktalogg. Processagnostisk: inga FK mot SM-metadata, ingen validering.
create table if not exists errand_process_activity (
    id                         varchar(36)  not null,
    errand_process_instance_id varchar(36)  not null,
    errand_id                  varchar(255) not null,
    external_task_id           varchar(64),             -- idempotensnyckel, stabil over retries
    activity_type              varchar(64)  not null,   -- fri strang: PHASE | TASK | INCIDENT | LOCK | CONFIG
    activity_id                varchar(255),
    activity_name              varchar(255),
    severity                   varchar(16)  default 'INFO' not null,   -- INFO | WARN | ERROR
    message                    varchar(2048),
    error_code                 varchar(64),
    occurred_at                datetime(3)  not null,   -- processens klocka
    created                    datetime(3)  not null,   -- SM:s klocka
    primary key (id),
    constraint fk_epa_instance foreign key (errand_process_instance_id)
        references errand_process_instance (id) on delete cascade,
    constraint uq_epa_idempotency unique (errand_process_instance_id, external_task_id, activity_id)
) engine=InnoDB;

create index if not exists idx_epa_instance_occurred on errand_process_activity (errand_process_instance_id, occurred_at);
create index if not exists idx_epa_retention         on errand_process_activity (created);
```

**Förenklingar mot v3, medvetna:**

| Borttaget | Skäl |
|---|---|
| `business_key` | Är alltid `errand_id` per konstruktion. Härledbart |
| `errand_number` i outbox | pw hämtar ärendet ändå; fältet användes inte |
| `executed_by_type` | Filtret matchar på värdet. Typen tillförde inget |
| `started`/`ended` på aktivitet | Tre nullbara tidsstämplar där två oftast var null. En aktivitetslogg är en sekvens av händelser; UI parar ihop `occurred_at` för att visa varaktighet |
| `STARTING`, `CANCELLED`, `START_FAILED` som statusvärden | §4.1 |

### 3.2 FK och kaskad

| Tabell | FK | Motiv |
|---|---|---|
| `process_event_outbox` → `errand` | **Ingen, medvetet** | Samma val som `notification_dispatch` (`V1_38`). Ett DELETE-event måste överleva att ärendet raderas |
| `errand_process_instance` → `errand` | `ON DELETE CASCADE`, **ingen JPA-relation på `ErrandEntity`** | DB-kaskaden räcker för att undvika föräldralösa rader vid `repository.deleteById` (`ErrandService:246`). JPA-mappning vore aktivt skadlig (§1.4) |
| `errand_process_activity` → instans | `ON DELETE CASCADE` | `errand_id`-kolumnen får ingen egen FK — InnoDB kaskaderar rekursivt |

**Retention:** aktiviteter röjs på `created` (default 365 d); outbox-rader röjs när `delivered_at` är äldre än **max(loop-guard-fönstret × 6, 24 h)**; dead letters enligt §8.3.

---

## 4. Domänmodell

### 4.1 `ProcessStatus` — beteendet bor på enumet

**Detta är den viktigaste enskilda klassen i lösningen.** Två helt olika frågor styrs av statusen, och de har **olika** svarsmängder. Skiljs de åt i två fristående mappningar kommer de förr eller senare drifta isär.

```java
package se.sundsvall.supportmanagement.integration.db.model.enums;

public enum ProcessStatus {

    /** En worker exekverar just nu. Enda status som behåller processlåset. */
    RUNNING   (true,  false),

    /** Processen väntar på handläggare, timer eller extern part. Lever, men arbetar inte. */
    WAITING   (false, false),

    /** Ett försök fallerade, Operaton kommer att försöka igen. Lever, men arbetar inte. */
    RETRYING  (false, false),

    /** Processen nådde sitt slut. */
    COMPLETED (false, true),

    /** Retries uttömda, incident rest, eller starten misslyckades (errorCode säger vilket). */
    FAILED    (false, true);

    private final boolean holdsLock;
    private final boolean terminal;

    ProcessStatus(final boolean holdsLock, final boolean terminal) {
        this.holdsLock = holdsLock;
        this.terminal = terminal;
    }

    /** Styr lock_token/lock_expires. Allt utom RUNNING släpper låset. */
    public boolean holdsLock() { return holdsLock; }

    /** Styr active_marker. OBS: en annan mängd än holdsLock - WAITING lever men håller inget lås. */
    public boolean isTerminal() { return terminal; }
}
```

> **Buggen detta förhindrar:** i v3 stod att den slutna enumen "styr även låsets släpp", vilket är sant — men `active_marker` styrs av en *annan* mängd. Implementeras "terminal = släpper lås = `active_marker = NULL`" får en **`WAITING`-instans `active_marker = NULL`**, och då kan en andra processinstans startas för samma ärende. Hela 1-1-invarianten faller.

Motsvarande i pw-alkt (`se.sundsvall.alkt.api.model.ProcessStatus`) med samma fem värden.

### 4.2 Övriga enums

```java
public enum ActivitySeverity { INFO, WARN, ERROR }
```

`activityType` och `activityId` är **fria strängar** — SM tolkar dem inte. Konvention i pw: `PHASE`, `TASK`, `INCIDENT`. SM skriver själv `LOCK` (låsutgång) och `CONFIG` (tvetydig etikett, okänd processKey).

### 4.3 JPA-entitet — `ErrandProcessInstanceEntity`

Följer mönstret i `NamespaceConfigEntity`: `@PrePersist`/`@PreUpdate` för tidsstämplar, `@TimeZoneStorage(NORMALIZE)`.

```java
@Entity
@Table(name = "errand_process_instance",
    indexes = {
        @Index(name = "idx_epi_errand_id",  columnList = "errand_id"),
        @Index(name = "idx_epi_lock_sweep", columnList = "lock_expires")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_epi_process_instance_id",   columnNames = "process_instance_id"),
        @UniqueConstraint(name = "uq_epi_one_active_per_errand",  columnNames = {"errand_id", "active_marker"})
    })
public class ErrandProcessInstanceEntity {

    @Id
    @Column(name = "id")
    private String id;                       // UUID, satt i @PrePersist

    @Column(name = "errand_id", nullable = false, length = 255)
    private String errandId;                 // OBS varchar(255) - errand.id ar det

    @Column(name = "municipality_id", nullable = false, length = 8)
    private String municipalityId;

    @Column(name = "namespace", nullable = false, length = 32)
    private String namespace;

    @Column(name = "process_service", nullable = false, length = 64)
    private String processService;

    @Column(name = "process_key", nullable = false, length = 128)
    private String processKey;

    @Column(name = "process_instance_id", length = 64)
    private String processInstanceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "process_status", nullable = false, length = 32)
    private ProcessStatus processStatus;

    @Column(name = "current_activity_id",   length = 255) private String currentActivityId;
    @Column(name = "current_activity_name", length = 255) private String currentActivityName;
    @Column(name = "error_code",    length = 64)   private String errorCode;
    @Column(name = "error_message", length = 2048) private String errorMessage;

    @Column(name = "lock_token", length = 36)          private String lockToken;
    @Column(name = "lock_expires")                     private OffsetDateTime lockExpires;
    @Column(name = "lock_reason", length = 512)        private String lockReason;
    @Column(name = "lock_owner_task_id", length = 64)  private String lockOwnerTaskId;
    @Column(name = "lock_first_acquired")              private OffsetDateTime lockFirstAcquired;
    @Column(name = "lock_renew_count", nullable = false) private int lockRenewCount;

    @Column(name = "started") private OffsetDateTime started;
    @Column(name = "ended")   private OffsetDateTime ended;

    /** 1 medan instansen lever, null nar den ar terminal. Bar unikhetsconstrainten. */
    @Column(name = "active_marker")
    private Byte activeMarker;

    @Column(name = "created")  private OffsetDateTime created;
    @Column(name = "modified") private OffsetDateTime modified;

    /** Enda stallet som far satta status - haller de tva harledda falten i synk. */
    public void applyStatus(final ProcessStatus status, final Clock clock) {
        this.processStatus = status;
        this.activeMarker  = status.isTerminal() ? null : (byte) 1;
        if (status.isTerminal()) {
            this.ended = OffsetDateTime.now(clock);
            releaseLock();
        } else if (!status.holdsLock()) {
            releaseLock();
        }
    }

    public boolean isLocked(final Clock clock) {
        return lockExpires != null && lockExpires.isAfter(OffsetDateTime.now(clock));
    }

    private void releaseLock() {
        this.lockToken = null;
        this.lockExpires = null;
        this.lockReason = null;
        this.lockOwnerTaskId = null;
    }
}
```

**`applyStatus` är enda vägen att sätta status.** Sätts `processStatus` direkt någon annanstans hamnar `active_marker` och låset ur synk — och det är exakt buggen i §4.1. Gör settern privat/paketprivat.

---

## 5. API

### 5.1 SM — skrivning (enda skrivendpointen)

```http
PUT /{municipalityId}/{namespace}/errands/{errandId}/process-instances/{processInstanceId}
X-Sent-By: pw-alkt; type=processEngine
X-Process-Lock-Token: b41f0d5a-9c2e-4b77-8f10-3a6d5e1c2f44     (utom vid forsta RUNNING)
X-Request-Group-Id: 8f1c2b6e-1f4a-4d61-9a0e-2b7c1f0a5e33
Content-Type: application/json
```

```json
{
  "processService": "pw-alkt",
  "processKey": "alkt-ansokan",
  "processStatus": "RUNNING",
  "currentActivityId": "investigation_phase",
  "currentActivityName": "Utredning",
  "externalTaskId": "a91c7f30-4d2b-11f0-9e21-0242ac120004",
  "lockReason": "Hämtar beslutsunderlag",
  "ttlSeconds": 1200,
  "started": "2026-08-19T08:55:11.004+02:00",
  "ended": null,
  "error": null,
  "activities": [
    {
      "activityType": "PHASE",
      "activityId": "review_phase",
      "activityName": "Granskning",
      "severity": "INFO",
      "occurredAt": "2026-08-19T09:02:44.910+02:00"
    }
  ]
}
```

Svar `201 Created` (första gången, med `Location`) eller `200 OK`:

```json
{
  "id": "1f0e4c21-...",
  "processInstanceId": "8f1c2b6e-...",
  "processStatus": "RUNNING",
  "lockToken": "b41f0d5a-9c2e-4b77-8f10-3a6d5e1c2f44",
  "lockExpires": "2026-08-19T09:32:03.221+02:00"
}
```

`lockToken` returneras **endast** när låset hålls efter anropet.

**Felrapport** — samma endpoint:

```json
{ "processStatus": "FAILED",
  "currentActivityId": "investigation_fetch_decision",
  "externalTaskId": "a91c...",
  "error": { "code": "INCIDENT", "message": "Timeout mot Employee efter 30 s" } }
```

**Startfel** (Operaton svarade aldrig med ett id) rapporteras utan `processInstanceId` i path — använd `POST .../process-instances` i stället:

```json
{ "processKey": "alkt-ansokan", "processStatus": "FAILED",
  "error": { "code": "START_FAILED", "message": "..." } }
```

### 5.2 SM — läsning

```
GET .../errands/{errandId}/process-instances                        -> 200 List<ProcessInstance>
GET .../errands/{errandId}/process-instances/{piid}/activities      -> 200 Page<ProcessActivity>
                                                                       ?page=&size=50 (max 200)&sort=occurredAt,desc
GET .../errands/{errandId}                                          -> 200 Errand med process-objektet
GET .../process-labels                                              -> 200 List<ProcessLabelMapping> (diagnostik)
```

### 5.3 API-modeller (SM)

```java
@Schema(description = "Process instance driving an errand")
public class ProcessInstance {
    @Schema(accessMode = READ_ONLY) private String id;
    private String processService;                 // required on write
    private String processKey;                     // required on write
    @Schema(accessMode = READ_ONLY) private String processInstanceId;
    private ProcessStatus processStatus;           // required on write
    private String currentActivityId;
    private String currentActivityName;
    private String externalTaskId;                 // idempotensnyckel for activities
    private String lockReason;
    private Integer ttlSeconds;                    // onskad TTL; kaps av max-ttl
    private OffsetDateTime started;
    private OffsetDateTime ended;
    private ProcessError error;
    private List<ProcessActivity> activities;      // skrivs in, lases via egen endpoint
    @Schema(accessMode = READ_ONLY) private String lockToken;
    @Schema(accessMode = READ_ONLY) private OffsetDateTime lockExpires;
    @Schema(accessMode = READ_ONLY) private OffsetDateTime created;
    @Schema(accessMode = READ_ONLY) private OffsetDateTime modified;
}

public class ProcessActivity {
    @Schema(accessMode = READ_ONLY) private String id;
    private String activityType;                   // fri strang
    private String activityId;
    private String activityName;
    private ActivitySeverity severity;
    private String message;
    private String errorCode;
    private OffsetDateTime occurredAt;             // required
    @Schema(accessMode = READ_ONLY) private OffsetDateTime created;
}

public class ProcessError {
    private String code;
    private String message;
}

/** Lasprojektion pa Errand. Null for namespace utan processmodell. */
@Schema(accessMode = READ_ONLY, description = "Process state driving this errand")
public class ErrandProcess {
    private String processService;
    private String processKey;
    private String processInstanceId;
    private ProcessStatus processStatus;
    private String currentActivityId;
    private String currentActivityName;
    private OffsetDateTime started;
    private OffsetDateTime ended;
    private ProcessError error;
    private boolean locked;
    private ProcessLock lock;                      // null nar locked == false
}

public class ProcessLock {
    private String lockedBy;
    private String reason;
    private OffsetDateTime expires;
}

/** Diagnostik: vilka etiketter startar vilken process. */
public class ProcessLabelMapping {
    private String metadataLabelId;
    private String resourcePath;                   // for lasbarhet
    private String processKey;
    private boolean deprecated;
}
```

`Errand` utökas med:

```java
@Schema(accessMode = READ_ONLY, description = "Process state driving this errand; null for namespaces without a process model")
private ErrandProcess process;
```

### 5.4 pw-alkt — inkommande

```http
POST /{municipalityId}/{namespace}/process/errand-events
```

```json
{
  "eventId": "3f2b91c4-7d5e-4a10-9c33-8e6b2f0a1d77",
  "eventType": "UPDATE",
  "eventSubType": "MESSAGE",
  "errandId": "f0882f1d-06bc-47fd-b017-1d8307f5ce95",
  "processKey": "alkt-ansokan",
  "occurredAt": "2026-08-19T09:12:03.221+02:00"
}
```

**Ingen ärendedata i nyttolasten.** pw hämtar `GET /errands/{errandId}` när det behöver innehåll — håller PII utanför outbox, loggar och en eventuell broker, och gör en fördröjd leverans ofarlig.

| Svar | När |
|---|---|
| `202 Accepted` | Hanterat, eller medvetet ignorerat (okänt ärende, mismatch, DELETE utan instans) |
| `422 Unprocessable Entity` | `processKey` matchar ingen deployad modell. **Permanent** |
| `5xx` | Transient |

```java
public class ErrandEvent {
    @NotBlank private String eventId;
    @NotNull  private EventType eventType;      // CREATE | UPDATE | DELETE
    private String eventSubType;
    @ValidUuid private String errandId;
    private String processKey;                  // kravs vid CREATE
    private OffsetDateTime occurredAt;
}
```

### 5.5 pw-alkt — workerns rapport

```java
/** Returneras av varje worker. Basklassen skickar den till SM. */
public record ProcessStateReport(
        ProcessStatus status,
        String currentActivityId,
        String currentActivityName,
        String lockReason,
        ProcessError error,
        List<ProcessActivity> activities) {

    public static ProcessStateReport running(String activityId, String activityName, String reason) { ... }
    public static ProcessStateReport waiting(String activityId, String activityName) { ... }
    public static ProcessStateReport completed() { ... }
    public static ProcessStateReport failed(String code, String message) { ... }
    public static ProcessStateReport retrying(String code, String message) { ... }
}
```

Fabriksmetoderna finns för att en worker inte ska behöva komma ihåg vilken status som släpper låset.

### 5.6 Statuskoder i SM

| Kod | När |
|---|---|
| `400` | `activePhaseId` sätts på ärende med processinstans; etikettändring som byter `processKey` |
| `403` | `X-Sent-By` saknas; `processService` matchar inte namespacets `PROCESS_CONSUMER` |
| `404` | Ärendet finns inte eller ligger i annat namespace |
| `409` | Andra levande instans för ärendet; instans med annat `process_key` än ärendets befintliga; `max-total` passerat |
| `423` | Låst av annan instans/task, eller fel/saknad token |

### 5.7 Beteendeförändring på befintliga endpoints

**`423 Locked` tillkommer på:** PATCH och DELETE errand, parametrar, JSON-parametrar, bilagor (API-vägen), `sendEmail`/`sendSms`/`sendWebMessage`, konversationer, handover.

Frontend måste hantera `423` som eget fall. Bättre väg: läs `errand.process.locked` **innan** användaren försöker skriva, disabla redigering och visa *"Processen arbetar med ärendet — klart om ca N minuter"*. `Retry-After` och `lock.expires` ger tiden.

`423` uppstår bara i namespace med `PROCESS_CONSUMER`. Övriga verksamheter ser ingen skillnad.

### 5.8 Borttaget i pw-alkt

`POST /process/start/{errandId}` och `POST /process/update/{processInstanceId}` **tas bort** i samma steg som `errand-events` införs. Kaskad: `ProcessService.updateProcess:42-55`, `StartProcessResponse` + test, `AbstractTaskWorker.clearUpdateAvailable:38-44` (död kod), `Constants.PROCESS_VARIABLE_UPDATE_AVAILABLE`, `Constants.FALSE`, `OperatonClient.setProcessInstanceVariable(s)`, `AbstractTaskWorker.setProcessInstanceVariable`.

Ingen escape hatch går förlorad: `POST errand-events` fungerar som manuell trigger via samma kodväg som produktionsflödet.

---

## 6. Lås och loop-skydd

### 6.1 Livscykeln bärs av rapporten

| Rapporterad status | Låset | `active_marker` |
|---|---|---|
| `RUNNING` | tas eller förnyas | 1 |
| `WAITING` | **släpps** | 1 |
| `RETRYING` | **släpps** | 1 |
| `COMPLETED` | **släpps** | NULL |
| `FAILED` | **släpps** | NULL |

Två kolumner, två olika mängder — se §4.1. Ett tabelldrivet test måste räkna upp **varje** enumvärde mot **båda** kolumnerna.

**Idempotent acquire:** samma `processInstanceId` som rapporterar `RUNNING` igen får samma token, förnyad `lock_expires` och `lock_renew_count++`. En **annan** instans får `423`.

### 6.2 Crash-säkerhet

**Ett utgånget lås är inget lås** — guarden är `lock_expires > now()`, så självläkning kräver ingen schemaläggare.

- TTL **20 min**, konfigurerbart. pw kan begära kortare via `ttlSeconds`; `max-ttl` kapar.
- Förnyelse via `RUNNING`-rapport. Ingen heartbeat-endpoint.
- `max-total` (4 h) räknat från `lock_first_acquired`; därefter `409` på förnyelse.
- **Ingen force-unlock.** Nödutgång vid verklig incident: `update errand_process_instance set lock_expires = null where ...`, vilket kräver DB-åtkomst och loggas där.

**Svepningen rör inte `process_status`.** `ProcessLockWorker` nollar utgångna låsfält och skriver en `errand_process_activity` med `severity = WARN`. Den sätter **inte** `FAILED`: Operaton retryar själv task:en när dess eget lås löper ut, så processen återhämtar sig — och `FAILED` vore terminalt, vilket skulle nolla `active_marker` och kollidera med nästa rapport.

### 6.3 Guarden

```java
public enum ProcessLockPolicy { ENFORCE, BYPASS }
```

Ny överlagring på `AccessControlService.getErrand(...)`. **Härledd default: `ENFORCE` när accessfiltret är exakt `{RW}`, annars `BYPASS`.** Medvetna undantag skickar `BYPASS` explicit — `ErrandNoteService:54, 91, 111`, `NotificationService:82, 114, 123, 135`, `CommunicationService:122`, `ConversationService:239` — så varje undantag syns i diffen.

**Kortslut först:** saknas `PROCESS_CONSUMER` för namespacet, returnera utan att röra databasen. Annars betalar alla övriga namespace en fråga per skrivning.

**Guardens villkor:**

```sql
select 1 from errand_process_instance
 where errand_id = :errandId
   and active_marker = 1
   and lock_expires > :now
```

Träff + (token saknas eller `token != lock_token`) ⇒ `423`.

Ordningen faller ut gratis: `updateErrand` anropar `getErrand:178` före `validateIfMatch:183`, så lås före ETag. Guarden körs efter accesskontrollen, så obehöriga får `401`/`404` — inte en `423` som avslöjar att en process finns.

**Explicit guard** vid ingången av `HandoverService.handover:97` (§1.3).

**Ärligt om gränsen:** att `RW` betyder skrivning är en observerad egenskap, inte en invariant. En endpoint som inte går via `AccessControlService` är osynlig för guarden. Motmedel: härledd default + explicita `BYPASS` + tabelldrivet test + regel i `CLAUDE.md`.

### 6.4 Vad som blockeras

**Nekas:** PATCH/DELETE errand, parametrar, JSON-parametrar, bilagor via API-vägen (`ErrandAttachmentService:77, 132` — inte överlagringarna `:84`/`:159` som används av intag/handover), utgående kommunikation, konversationer, handover.

**Tillåts:** all läsning, anteckningar, notiser, läsmarkering, prenumerationer — och **allt intag** (`EmailReaderWorker`, `MessageExchangeSyncService`, `WebMessageCollectorWorker`, `SuspensionWorker`), som går förbi chokepointen av konstruktion. Extern parts data får aldrig tappas för att en process arbetar.

### 6.5 Parallella grenar — modelleringsregel, inte kod

**Brist som måste hanteras:** Operaton kan ha två external tasks aktiva samtidigt i samma processinstans (parallell gateway). Båda rapporterar `RUNNING` → båda får samma token (idempotent acquire) → den som blir klar först rapporterar `WAITING` och **släpper låset medan den andra fortfarande arbetar**. Handläggaren kan då editera mitt i.

**Beslut: en modelleringsregel.** BPMN-modellerna får inte ha parallella grenar där mer än en gren muterar ärendet. För myndighetsprocesser är parallell mutation av samma ärende ändå en modelleringssmell.

**Regeln görs synlig i stället för att förutsättas:** `lock_owner_task_id` lagrar den `externalTaskId` som tog låset. Kommer en `RUNNING`-rapport från en **annan** task medan låset hålls, skriver SM en `errand_process_activity` med `severity = WARN` och texten *"concurrent external tasks detected"* — och låset behålls av den ursprungliga ägaren. En terminal rapport **släpper bara låset om den kommer från `lock_owner_task_id`**; annars uppdateras tillstånd och aktiviteter men låset lämnas orört.

Det gör att regelbrott (a) inte tyst öppnar ärendet mitt i en körning och (b) syns i ärendets processlogg.

**Eskaleringsväg om parallell mutation blir nödvändig:** referensräkning på låset. Det är en kontenerad ändring — låset är en rad — men den bär en egen felmodell (en kraschad gren lämnar räknaren fel; TTL:n får rädda den). Bygg det inte i förväg.

### 6.6 Loop-skydd

**Lager 1 — strukturellt:** *finns ett aktivt processlås ⇒ skriv ingen outbox-rad.* Bygger på SM:s eget tillstånd, inte en klientheader. Korrekt eftersom en skrivning som *lyckas* under lås måste komma från låshållaren — alla andra får `423`.
**Invariant:** varje pw-skrivning mot ärendet sker under lås. Därför **ingen lås-opt-out** i `AbstractTaskWorker` för workers som skriver.

**Lager 2 — origin-filter** (`X-Sent-By` mot `PROCESS_CONSUMER`-identiteten) vid dispatch, som djupförsvar.

**Lager 3 — trigger-filter** på `(EventType, EventSubType)`. Ortogonalt: ser på *vad* som hände, inte *vem*.

**Lager 4 — orsaksagnostisk nödbroms.** Räkna rader med `errand_id = ? and created > now() - window`. Över tröskeln (20 / 10 min): ingen rad, ERROR-aktivitet, hälsoindikator unhealthy.

> Räkningen kräver att outbox-rader **soft-deletas** (`delivered_at`). Raderades de direkt skulle en snabb loop aldrig lämna mer än en rad och bromsen vore verkningslös precis när den behövs.

**Inte** ett loop-skydd: versionsbaserad idempotens — versionen stiger varje varv.

---

## 7. Konfiguration och processval

### 7.1 `namespace_config`

| Nyckel | Typ | Antal | Värde |
|---|---|---|---|
| `PROCESS_CONSUMER` | STRING | 1 | `pw-alkt` |
| `PROCESS_TRIGGER` | STRING | N | `ERRAND`, `MESSAGE`, `ATTACHMENT` |

```java
// ConfigPropertyExtractor - nya konstanter
public static final String PROPERTY_PROCESS_CONSUMER = "PROCESS_CONSUMER";
public static final String PROPERTY_PROCESS_TRIGGER  = "PROCESS_TRIGGER";

/** Flervardad motsvarighet till getNullableValue - befintlig gor .findFirst() och tappar resten. */
public static <T> List<T> getValues(NamespaceConfigEntity entity, String key) { ... }
```

Exempel på data:

```sql
insert into namespace_config_value (namespace_config_id, `key`, `value`, `type`) values
  (42, 'PROCESS_CONSUMER', 'pw-alkt',    'STRING'),
  (42, 'PROCESS_TRIGGER',  'ERRAND',     'STRING'),
  (42, 'PROCESS_TRIGGER',  'MESSAGE',    'STRING'),
  (42, 'PROCESS_TRIGGER',  'ATTACHMENT', 'STRING');
```

**`ERRAND` är obligatorisk** — utan den startar aldrig ett ärende som får sin etikett i ett andra anrop.

### 7.2 `application.yml`

```yaml
spring.security.oauth2.client:
  registration:
    pw-alkt: { authorization-grant-type: client_credentials, provider: pw-alkt }
  provider:
    pw-alkt: { token-uri: "${...}" }
integration:
  pw-alkt: { url: "${...}", connect-timeout: 5, read-timeout: 30 }
process-engine:
  lock:       { default-ttl: PT20M, max-ttl: PT30M, max-total: PT4H }
  loop-guard: { max-events-per-errand: 20, window: PT10M }
  consumers:
    pw-alkt: { identifier: pw-alkt }        # matchas mot X-Sent-By
scheduler:
  process-event:   { cron: "0 * * * * *", name: processEvent,   lockAtMostFor: PT2M }
  process-lock:    { cron: "0 */5 * * * *", name: processLock,  lockAtMostFor: PT2M }
  process-cleanup: { cron: "0 30 2 * * *", name: processCleanup, lockAtMostFor: PT10M }
```

I `application-it.yml` sätts samtliga cron till `"-"`.

### 7.3 Etikett → process

Attributet `processKey` på labeln. **Ingen trädtraversering** — bara ärendets egna etiketter.

```sql
insert into metadata_label_attribute (metadata_label_id, `key`, `value`) values
  ('9c1a...', 'processKey', 'alkt-ansokan'),
  ('4f8b...', 'processKey', 'alkt-tillsyn');
```

| Utfall | Resultat |
|---|---|
| Exakt en nyckel | Den processen startas |
| Noll | Ingen process. Inte ett fel |
| Två eller fler | **Fel.** Ingen start, ERROR-aktivitet som namnger båda |

`deprecated`-labels används inte. **SM validerar inte nyckeln** — bara pw vet vad som är deployat; felet fångas som `422` (§5.4).

### 7.4 1-1-invarianten

1. Etikettändring som skulle byta upplöst `processKey` avvisas med `400` så snart ärendet har en instans — även avslutad.
2. Högst en **levande** instans per ärende (`active_marker`, §4.1).
3. Alla instanser för ett ärende delar samma `process_key` — service-kontroll under radlås, ej uttryckbart i DB.

Constraint-violation på `uq_epi_one_active_per_errand` **måste översättas** till `409` med `detail` som pekar ut den blockerande instansen.

---

## 8. Drift och förvaltning

### 8.1 Mätvärden som måste finnas

Utan dessa är loop-skyddet och låset otestbara i drift.

| Mätvärde | Varför |
|---|---|
| `process_event.suppressed{reason=LOCK\|ORIGIN\|TRIGGER\|GUARD}` | Ett tyst loop-skydd som slutar fungera märks annars först när loopen är där |
| `process_event.published`, `.delivered`, `.dead_lettered` | Leveranshälsa |
| `process_event.nudge_rejected` | Executorn mättad ⇒ latensen faller tillbaka på cron |
| `process_lock.expired_without_report` | **Viktigaste driftindikatorn.** Stiger den kraschar workers, eller finns ett arbetssteg längre än TTL |
| `process_lock.concurrent_task_detected` | Brott mot modelleringsregeln i §6.5 |
| `process_instance.start_failed` | Feltaggade etiketter |

Logga alltid `eventId`, `errandId`, `processInstanceId` och `X-Request-Group-Id` — dubbelleveranser blir då spårbara i efterhand.

### 8.2 Vanliga driftfrågor och svaren

| Fråga | Svar |
|---|---|
| Varför startade ingen process för ärendet? | `GET .../process-instances` tom + `errand.process` null. Kontrollera etikett-tagg via `GET /process-labels`, och att `PROCESS_CONSUMER` finns för namespacet |
| Varför är ärendet låst? | `errand.process.lock` visar `lockedBy`, `reason`, `expires` |
| Varför väcks inte processen av inkommande e-post? | `MESSAGE` saknas i `PROCESS_TRIGGER` |
| Varför får handläggaren `423` fast processen ser klar ut? | Instansen har `WAITING` men låset inte släppt ⇒ terminal rapport uteblev. TTL löser inom 20 min; se `process_lock.expired_without_report` |

### 8.3 Dead letters — bygg en väg tillbaka

`NotificationDispatchWorker.cleanUpDeadLetters:48-49` **raderar** dead letters efter 7 dagar. Ärv inte det rakt av: en dead-letterad processhändelse betyder att en process aldrig fick veta något, och att bara radera den gör felet permanent och osynligt.

**Krav:**

- Dead letters röjs **inte** automatiskt inom retentionstiden; en unhealthy hälsoindikator hålls så länge det finns odelivererade.
- Administrativ endpoint `POST /{municipalityId}/{namespace}/process-events/{id}/redrive` som nollar `retry_count`, `dead_letter` och `next_retry_at`.
- Röjning först efter konfigurerad retention (default 30 d), med logg på vad som togs bort.

### 8.4 Verifiera lokalt

1. Skapa namespace-config med `PROCESS_CONSUMER=pw-alkt` och `PROCESS_TRIGGER=ERRAND,MESSAGE`.
2. Tagga en label med `processKey=alkt-ansokan`.
3. `POST /2281/ALKT/errands` med den labeln ⇒ rad i `process_event_outbox` inom en sekund, `delivered_at` satt när stubben svarat.
4. `GET /2281/ALKT/errands/{id}` ⇒ `process.processStatus = RUNNING`.
5. `PATCH` samma ärende under lås ⇒ `423` med `Retry-After`.

---

## 9. pw-alkt

### 9.1 En BPMN-fil per processdefinition

`alkt-ansokan.bpmn`, `alkt-tillsyn.bpmn`. `TenantAwareAutoDeployment:59-84` deployar en deployment per fil — gemensam fil hade betytt att en ändring i tillsyn versionerar upp ansökan och drar in pågående instanser.

Processens `id` måste matcha `Constants.PROCESS_KEY_*`. **`ProcessWithoutDeviationIT:72-76` väntar på `size() == 1`** och måste justeras när fil två läggs till.

### 9.2 Modelleringskrav

Dessa är förutsättningar för att designen ska hålla — de är inte råd.

1. **Wait states ska omvärdera sitt villkor vid inträde.** Läs aktuellt ärendetillstånd och avgör om villkoret redan är uppfyllt innan väntan börjar. En sväljd `MismatchingMessageCorrelation` kan vara en legitim väckning som kom medan processen var mellan två wait states; att det ändå är ofarligt vilar helt på detta.
2. **Inga parallella grenar som muterar ärendet** (§6.5).
3. **Inga processvariabler för idempotens eller signalering.** Skälet står i koden som tas bort — `"Clearing process variable has to be a blocking operation. Using ExternalTaskService.setVariables() will not work without creating race conditions."` Bevara resonemanget även när metoden är borta; det är det första någon återinför när ett dubblettproblem dyker upp.
4. **Inga call activities eller delade subprocesser** nu — framtida processer kan skilja sig avsevärt.

### 9.3 Start, fortsättning och radering

```
handleErrandEvent(municipalityId, namespace, event):
    om event.eventType == DELETE:
        instans = findProcessInstances(businessKey = errandId, tenantIdIn = ALKT)
        finns -> deleteProcessInstance(id, reason = "errand deleted in SM")
        202                                        // arendet ar borta i SM; ingen rapport tillbaka

    instans = findProcessInstances(errandId, event.processKey, "ALKT")
    om tom:
        om event.eventType == CREATE:
            om processKey inte deployad -> 422
            start med businessKey = errandId
            POST .../process-instances {RUNNING}   // 409 -> avbryt instansen
        annars: logga, 202
    annars:
        correlateMessage(messageName = "errandUpdated", businessKey = errandId, tenantId = "ALKT")
```

**`MismatchingMessageCorrelationException` (400) ⇒ INFO + `202`, ingen retry.** Annars fylls dead letter-kön av normala händelser.

**DELETE måste hanteras** — annars lever en processinstans vidare i Operaton för ett ärende som inte finns, och SM har inget spår kvar eftersom `errand_process_instance` kaskaderats bort.

`OperatonClient` behöver: `correlateMessage`, `findProcessInstances(businessKey, processDefinitionKey, tenantIdIn)`, `deleteProcessInstance`, och `businessKey` i `OperatonMapper.toStartProcessInstanceDto`.

### 9.4 Workerstruktur

```java
public abstract class AbstractTaskWorker implements ExternalTaskHandler {

    protected abstract ProcessStateReport executeBusinessLogic(ExternalTask task, ExternalTaskService service);

    @Override
    public void execute(final ExternalTask task, final ExternalTaskService service) {
        RequestId.init(task.getVariable(PROCESS_VARIABLE_REQUEST_ID));
        try {
            supportManagement.report(task, ProcessStateReport.running(
                    activityId(task), activityName(task), lockReason()));
            final var report = executeBusinessLogic(task, service);
            supportManagement.report(task, report);
            service.complete(task);
        } catch (final Exception e) {
            logException(task, e);
            failureHandler.handleException(service, task, e.getMessage());   // rapporterar RETRYING/FAILED
        } finally {
            RequestId.reset();
        }
    }
}
```

Returtypen gör rapporten **strukturellt obligatorisk** — en worker kan inte kompilera utan att producera en. **Ingen lås-opt-out** för workers som skriver till SM.

`FailureHandler` rapporterar till SM **före** `handleFailure`: `RETRYING` när `calculateRetries:52-56` ger fler försök, annars `FAILED`. Det är vad som får felmeddelandet till ärendets UI *och* släpper låset. Följd: mellan två försök (`retry.timeout: 10000`) är ärendet redigerbart i ~10 s — korrekt enligt "låst endast under exekvering", men en medveten skillnad.

`SupportManagementConfiguration` får en `RequestInterceptor` som sätter `X-Sent-By: pw-alkt; type=processEngine` och `X-Request-Group-Id` på **alla** utgående anrop.

---

## 10. Implementationsuppgifter

Varje uppgift är mergbar för sig. Acceptanskriterierna är avsedda att kunna klistras in i en task.

### T1 — Datamodell och domänenums (SM)

**Bygg:** `V1_47`-migrering (§3.1); `ProcessStatus` med `holdsLock()`/`isTerminal()` (§4.1); `ActivitySeverity`; entiteterna `ProcessEventOutboxEntity`, `ErrandProcessInstanceEntity` (§4.3), `ErrandProcessActivityEntity`; repositories med `Pageable` på **alla** sökfrågor; tabellerna i `truncate.sql`.

**Acceptans:**
- Ingen av entiteterna är mappad som relation på `ErrandEntity`.
- `ErrandProcessInstanceEntity.applyStatus` är enda vägen att sätta status; settern är inte publik.
- Tabelldrivet test räknar upp **varje** `ProcessStatus` mot både `holdsLock()` och `isTerminal()`, med `WAITING` explicit verifierad som *icke*-terminal men *icke*-låshållande.
- Någon IT startar grönt ⇒ `schema-generation: validate` bekräftar DDL mot entiteter.

### T2 — Konfigurationsläsning (SM)

**Bygg:** `PROPERTY_PROCESS_CONSUMER`, `PROPERTY_PROCESS_TRIGGER`, `getValues(...)` (§7.1); `@ConfigurationProperties` för `process-engine.*`; validering av `PROCESS_CONSUMER` mot konfigurerade konsumenter vid skrivning.

**Acceptans:**
- `getValues` returnerar **alla** rader för en nyckel (regression mot `.findFirst()`).
- Skrivning av okänd `PROCESS_CONSUMER` ger `400`.
- Verifierat att `namespaceConfigCache` evikteras vid skrivning — annars är runtime-redigerbarheten en illusion.

### T3 — Process-instance-API (SM)

**Bygg:** `ProcessInstanceResource` (`PUT`, `POST`, `GET`, `GET activities`); `ProcessInstanceService`; API-modellerna (§5.3); `Errand.process` + batchberikning i `readErrand`/`findErrands`; regenerera `openapi.yaml`.

**Acceptans:**
- `PUT` två gånger ⇒ `revision`-tabellen oförändrad (skyddar mot framtida `@OneToMany` på `ErrandEntity`).
- Aktiviteter idempotenta på `(processInstanceId, externalTaskId, activityId)`; batch > 100 ⇒ `400`.
- Andra levande instans ⇒ `409`; instans med annat `process_key` ⇒ `409`; constraint-violation översatt, aldrig `500`.
- `findErrands` gör **en** fråga för berikningen (verifieras med query-räkning, inte ögonmått).
- Beslut fattat och dokumenterat om `process` ska strippas av `limitedMappingPredicateByLabel`.

### T4 — Processlås (SM)

**Bygg:** låshanteringen i `ProcessInstanceService` (§6.1–6.2); `ProcessLockPolicy` + överlagring i `AccessControlService` med härledd default och explicita `BYPASS`; kortslutning för namespace utan `PROCESS_CONSUMER`; explicit guard i `HandoverService.handover:97`; `ProcessLockWorker`.

**Acceptans:**
- `ProcessLockService` tar en injicerad `Clock`. **Inget test använder `Thread.sleep`.**
- Tabelldrivet test över varje skrivväg i §6.4 med förväntat utfall.
- `@Sql`-laddad rad med `lock_expires` i det förflutna ⇒ PATCH ger `200` **utan** att svepningen körts.
- Svepningen ändrar **inte** `process_status`; den skriver WARN-aktivitet.
- `RUNNING` från annan `externalTaskId` medan låset hålls ⇒ WARN-aktivitet, låset behålls; terminal rapport från icke-ägare släpper **inte** låset (§6.5).
- `423` bär `Retry-After` och låsdetaljer.

### T5 — Publicering (SM)

**Bygg:** `ProcessEventPublisher` anropad från `EventService.createErrandEvent`; `ProcessKeySelector` (§7.3); nödbromsen; `GET /process-labels`.

**Acceptans:**
- **IT som verifierar att ett e-postintag ger en outbox-rad** — regressionsskydd för den allvarligaste bristen som hittades.
- Enhetstest per gren i §2.2, inklusive: aktivt lås ⇒ ingen rad; icke-triggad subtyp ⇒ ingen rad; namespace utan konsument ⇒ ingen rad.
- `ProcessKeySelectorTest`: en tagg ⇒ en nyckel; två med samma ⇒ en; två med olika ⇒ ERROR-aktivitet och ingen rad; `deprecated` ignoreras; **namnbyte och omflyttning av labeln lämnar upplösningen oförändrad**.
- Nödbromsen slår över tröskeln med rader som har `delivered_at` satt.

### T6 — Relay och leverans (SM)

**Bygg:** `service/scheduler/processevent/` (scheduler, worker, relay); AFTER_COMMIT-nudge + bounded executor; `ProcessEngineClient` + felnormaliserande wrapper; `422` permanent vs `5xx` transient; redrive-endpoint och retention (§8.3); mätvärden (§8.1).

**Acceptans:**
- WireMock svarar `202` / `422` / `503` / timeout — samtliga fyra vägar verifierade, inklusive att `422` **inte** retryas och skriver `FAILED` + ERROR-aktivitet.
- Ordning per ärende hålls när flera rader finns.
- Mättad executor ⇒ nudgen släpps och cron levererar; ingen HTTP-tråd blockeras.
- Dead letters raderas inte inom retentionstiden; redrive nollar räknarna.

### T7 — Skyddsräcken (SM)

**Bygg:** `400` på `activePhaseId` när ärendet har processinstans; `400` på etikettändring som byter `processKey`.

**Acceptans:** båda täckta av enhetstest och ett IT-fall vardera.

### T8 — `ProcessLoopGuardIT` (SM)

**Det viktigaste enskilda testet.** Kör hela varvet: ärende skapas ⇒ outbox-rad; stub agerar pw, rapporterar `RUNNING` (tar låset), PATCHar ärendet, rapporterar `WAITING`. **Assertera att ingen ny odelivererad outbox-rad uppstod.** Kör om utan lock-token och verifiera att lager 2 fångar.

Utan detta test är loop-skyddet en hypotes.

### P1 — Operaton-klienten (pw)

`correlateMessage`, `findProcessInstances`, `deleteProcessInstance`; `businessKey` i mappern; nya konstanter. **Acceptans:** `ProcessWithoutDeviationIT` fortsatt grön.

### P2 — Event-endpoint och borttagning (pw)

`POST /process/errand-events` med logiken i §9.3, **och borttagningen i §5.8 i samma steg**. Regenerera `openapi.yaml`.

**Acceptans:** start / korrelera / okänt ärende / DELETE / okänd nyckel (`422`) täckta; inga referenser kvar till `updateAvailable`, `StartProcessResponse` eller `setProcessInstanceVariable`.

### P3 — SM-klienten (pw)

`patchErrand`, `report(...)`, `getErrand`; `RequestInterceptor` för `X-Sent-By`/`X-Request-Group-Id`; uppdatera `support-management.yaml` och regenerera; WireMock-stubbar.

**Acceptans:** test som verifierar att headern sätts på **alla** utgående anrop, inte bara ett.

### P4 — Workerstruktur (pw)

`AbstractTaskWorker` enligt §9.4; `ProcessStateReport` med fabriksmetoder; `FailureHandler` rapporterar `RETRYING`/`FAILED`.

**Acceptans:** IT verifierar ordningen `RUNNING` → PATCH → terminal rapport; ett fall där `executeBusinessLogic` kastar asserterar att `RETRYING` rapporterats **och** låset släppts.

### P5 — Tillsynsprocessen (pw)

`alkt-tillsyn.bpmn`; justera `ProcessWithoutDeviationIT:75` till 2; egen `ProcessPathway`.

### P6 — Incidentåterkoppling (pw)

Schemalagd kontroll som skriver `FAILED` + `error` till SM när Operaton rest en incident.

---

## 11. Risker och kända begränsningar

| Risk | Hantering |
|---|---|
| **RabbitMQ-mognad** (öppen fråga) | §2.4. Varje REST-konsument byggd innan bytet är kastat arbete |
| **`WAITING` felaktigt behandlad som terminal** | Skulle bryta 1-1-invarianten tyst. Skyddas av `applyStatus` som enda väg + tabelldrivet test (T1) |
| **Parallella grenar släpper låset i förtid** | Modelleringsregel + `lock_owner_task_id` gör brottet synligt (§6.5). Eskalering: refcount |
| **Loop SM ↔ pw** | Fyra lager (§6.6), varav lager 1 vilar på SM:s eget tillstånd och lager 4 är orsaksagnostiskt. **Kräver mätvärdena i §8.1** |
| **`RW` ⇒ skrivning är observerad, inte garanterad** | Härledd default + explicita `BYPASS` + tabelldrivet test + regel i `CLAUDE.md`. Inte vattentätt |
| **Ny endpoint utan låskontroll** | Fångas om den går via `AccessControlService`. Annars osynlig — enda motmedlet är kodgranskning |
| **`423` manglas av WSO2** | Verifiera i miljö; fallback `409` med särskiljande `type` |
| **Handläggare blockerad efter krasch** | Upp till 20 min. Ingen force-unlock; kortare tak är spaken |
| **Felstavat `processKey`** | Upptäcks vid första ärendet. `422` ⇒ ingen retry, `FAILED` + ERROR-aktivitet direkt på ärendet |
| **Dead letter raderas och felet blir permanent** | §8.3 — redrive-endpoint och retention före röjning |
| **Runtime-redigerbar routing utan granskning** | Priset för att slippa release. Validering av `PROCESS_CONSUMER`; överväg ändringslogg |
| **PII i aktivitetsloggen** | `message` är fri text från processen. **pw måste instrueras att inte lägga PII där** — en regel, inte en spärr |
| **Dubbla processinstanser** | ShedLock-serialiserad leverans + businessKey-kontroll + `409` + DB-constraint. Restrisk i Operaton, som saknar unikhet på business key — men SM kan inte registrera resultatet |
| **Delas Operaton-tenanten `ALKT`?** | Påverkar `getDeployments`-assertions och `historyTimeToLive`. Bekräfta mot driftmiljön |

### Vad som inte går att verifiera automatiskt

- Att WSO2 släpper igenom med rätt scope och inte manglar `423`.
- Verklig samtidighet mellan poddar — ShedLock täcks indirekt av `ShedlockConfigurationIT`.
- Långtidsbeteende hos outbox och aktivitetslogg. Kompensation: mätvärdena i §8.1.
