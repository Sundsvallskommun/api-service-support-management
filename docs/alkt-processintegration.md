# SupportManagement ↔ pw-alkt — processintegration

Designunderlag för ALKT-namespacets myndighetsprocess i Operaton, körd av tjänsten
[pw-alkt](https://github.com/Sundsvallskommun/pw-alkt). Innehåller datamodell, API-kontrakt,
regler för samtidighet och loop-skydd, samt uppgiftsnedbrytning med acceptanskriterier.

**Jira:** DRAKEN-4713 (story) med DRAKEN-4714…4727 som deluppgifter. Avsnittsnumren nedan
refereras från respektive deluppgift; kopplingen mellan uppgifterna i §10 och Jira-nycklarna
står i tabellen där.

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
| 7 | **Optimistisk kontroll via `errand.version` och `If-Match`** — konflikten upptäcks vid skrivning | Pessimistiskt ärendelås med TTL, guard och `423` | Kravet är att ändringar inte tappas, inte att handläggaren hindras. Maskineriet finns redan i drift — §6.2 |
| 8 | **Ett** skrivendpoint bär tillstånd och aktiviteter | Separata endpoints | Färre anrop, en transaktion, en rapport per arbetssteg |
| 9 | Inga processvariabler för idempotens | `updateAvailable`, versionsräknare | Racy read-modify-write. Skyddet finns strukturellt |
| 10 | `errand.status` frikopplat från processens faser | Process äger status | Skilda begrepp. Listvyn läser processläget ur `errand.process` |
| 11 | SM:s `phase`/`errand_phase` används inte för ALKT | Återanvänd fasmodellen | Processmodellen ägs av BPMN |
| 12 | pw-alkt:s `start`/`update`-endpoints tas bort | Deprecation | Inga konsumenter. Två startvägar ⇒ någon använder fel |
| 13 | **`ProcessStatus.isTerminal()` som metod på enumet** | Fristående mappningstabell | Styr `active_marker` och därmed 1-1-invarianten. `WAITING` är den fällan — se §4.1 |
| 14 | **Fem statusvärden**, `START_FAILED` blir `FAILED` + `errorCode` | Åtta värden | Skillnaden syns redan på `processInstanceId IS NULL` |
| 15 | **Modelleringsregel: inga parallella grenar som muterar ärendet** | Ingen regel | Två grenar som skriver samma ärende ger ömsesidiga `412` utan konvergens — §6.4 |
| 16 | `X-Sent-By` bär loop-filtret, inte auktorisation | Identitetsbaserad skrivspärr | Med optimistisk kontroll finns ingen skrivspärr att auktorisera mot. `@Version` fångar de verkliga konflikterna |
| 17 | **Publiceringsfel märker transaktionen `rollback-only`** | Lita på att anropsstället propagerar felet | Samtliga anropsställen sväljer undantag från `createErrandEvent` (§1.7). Utan detta finns ingen transaktionell outbox |
| 18 | **Start styrs av `processKey`, inte av `eventType`** | Start endast på `CREATE` | Ärendet som får sin etikett i ett andra anrop hade annars aldrig startat — §7.1 motiverar triggern, §9.3 utför den |
| 19 | Aktiviteter får sakna processinstans — `errand_process_instance_id` nullbar, aktivitetsläsning ärendescopad | `not null` | Tvetydig etikett och nödbroms inträffar innan någon instans finns. Posten hade inte gått att skriva |
| 20 | Rena läs-workers rapporterar `errandVersion`; SM svarar `412` om den glidit | Inget skydd alls för läs-workers | Workern har annars ingenting att kollidera på — §6.3 |
| 21 | **`POST .../process-instances` skapar men uppdaterar aldrig** | `409` så snart en rad finns | Workern kan `PUT`:a före pw:s `POST`. Med create-only är ankomstordningen likgiltig, och `409` betyder bara en sak — §5.1 |
| 22 | **Ett avslutat processliv startas aldrig om.** `FAILED` får startas om | Starta på nästa triggande händelse oavsett historik | `findProcessInstances` ser bara Operatons runtime, så en `COMPLETED` process ser ut som ingen process alls. Nästa process är ett nytt ärende — §7.4 |
| 23 | `errand.process` projicerar **senaste** instansen | Den levande instansen | En misslyckad start lämnar ingen levande instans. Projiceras bara den levande ser handläggaren ingenting alls — §5.3 |
| 24 | **Beslutet lagras som `json_parameter` med nyckeln `alkt.beslut`** | Kolumner på `errand_process_instance`; vanliga parametrar; aktivitetsloggen | Ett ärende, ett beslut — unikheten på `(errand_id, parameter_key)` *är* invarianten. Se §7.5 |
| 25 | **Handläggaren fattar beslutet; processen väntar på det** | Processen fattar beslutet | Delegationsbeslut fattas av människa. Följden: `DECISION` måste vara `PROCESS_TRIGGER`, och `updateJsonParameter` måste skapa event — §7.5 |

---

## 1. Verifierade fakta som designen vilar på

### 1.1 `EventService` är den universella kanalen

`EventService.createErrandEvent` anropas från 12 ställen:

| Väg | Anropsställe |
|---|---|
| PATCH / create / delete | `ErrandService.createErrand/updateErrand/deleteErrand` |
| Bilagor | `ErrandAttachmentService.createErrandAttachmentInternal/deleteErrandAttachment/createErrandAttachment(AttachmentEntity, …)` |
| Handover | `HandoverService.logHandoverEvents` |
| Konversationer | `MessageExchangeSyncService.syncConversation` |
| E-postintag | `EmailReaderWorker.processErrand` |
| Webbmeddelanden | `WebMessageCollectorWorker.saveMessage` |
| Suspension | `SuspensionWorker.processExpiredSuspensions` |

**Revisioner skapas bara på fem ställen** — `ErrandService.createErrand/updateErrand`, `ErrandAttachmentService.createErrandAttachmentInternal/deleteErrandAttachment/createErrandAttachment(AttachmentEntity, …)`. Intaget (`EmailReaderWorker.processErrand`, `MessageExchangeSyncService.applyStatusChange`) sparar direkt via repository. Därför kan trigger-filtret inte bygga på revisionsdiff.

`EventSubType` (befintlig enum): `ATTACHMENT, DECISION, ERRAND, HANDOVER_IN, HANDOVER_OUT, MESSAGE, NOTE, SYSTEM, SUSPENSION`. `EventType` kommer från eventlog-specen.

### 1.2 Leveransmaskineriet finns redan

`EventService.saveDispatchEntry` (outbox-rad), `NotificationDispatchEntity` (fälten `retryCount`/`nextRetryAt`/`deadLetter`, indexet `idx_dispatch_dead_letter_retry`), `NotificationDispatchScheduler.processDispatch` (ShedLock + hälsoindikator), `NotificationDispatchWorker.processGroup`/`handleFailure` (retry/backoff/dead letter), `.subscriberWantsEventType` (eventtypfilter), `.isExecutingUser` (hoppa över upphovet).

Ärv **inte**: `NotificationDispatchRepository.findProcessable` saknar `LIMIT`; `NotificationDispatchWorker.TRANSACTION_BUFFER_SECONDS` är 10 s. Och `NotificationDispatchWorker.cleanUpDeadLetters` **raderar** dead letters efter 7 dagar utan möjlighet att köra om dem — se §8.3.

### 1.3 Chokepoint för errand-skrivningar

Allt går via `AccessControlService`, och **skrivvägar skickar `RW` ensamt, läsvägar `R, RW`**. Det finns **två** ingångar. Med optimistisk kontroll (§6.2) finns ingen guard som måste täcka båda — men kartan behövs ändå för att veta vilka vägar som muterar ärendet:

`getErrand(...)` — hämtar entiteten och filtrerar:
`ErrandService.updateErrand/deleteErrand`, `ErrandParameterService.updateErrandParameters/updateErrandParameter/deleteErrandParameter`, `ErrandJsonParameterService.updateJsonParameter/deleteJsonParameter`, `ErrandAttachmentService.createErrandAttachment/deleteErrandAttachment`, `CommunicationService.sendEmail/sendSms/sendWebMessage`, `ConversationService.markAsRead`, `ErrandNoteService.createErrandNote/updateErrandNote/deleteErrandNote`, `NotificationService.createNotification`.

`verifyExistingErrandAndAuthorization(...)` — kontrollerar **utan** att hämta entiteten:
`CommunicationService.updateViewedStatus`, `ConversationService.createConversation/updateConversationById/createMessage`, `NotificationService.globalAcknowledgeNotificationsByErrandId/updateNotification/deleteNotification`.

Undantag: **`HandoverService.handover`** hämtar utan filter men muterar källärendet i `handleSourceErrand`. Schemaläggarna går förbi chokepointen helt — de skriver via repository direkt (`EmailReaderWorker.processErrand`, `MessageExchangeSyncService.applyStatusChange`) och höjer därmed inte heller `errand.version` (§6.2).

### 1.4 Revisionssnapshotten är hela entiteten

`RevisionMapper.toSerializedSnapshot` Gson-serialiserar hela `ErrandEntity`; `CircularReferenceExclusionStrategy` tar bara bakåtreferenser. **Två hårda regler:** inga nya kolumner på `errand`, inga nya `@OneToMany` på `ErrandEntity`. Dessutom gör `ErrandService.updateErrand` `OPTIMISTIC_FORCE_INCREMENT` — en kolumn på `errand` hade invaliderat varje ETag.

### 1.5 `namespace_config` driver redan beteende

`ConfigPropertyExtractor` har `PROPERTY_ACCESS_CONTROL` och `PROPERTY_NOTIFY_REPORTER` — beteendeflaggor. `namespace_config_value` (`V1_19`): `key`, `value text`, `type` (`BOOLEAN|STRING|INTEGER`), unikt på **`(namespace_config_id, key, value)`** ⇒ flervärd per nyckel. `@ElementCollection(EAGER)`, cachad via `namespaceConfigCache`.

**Fallgrop:** `ConfigPropertyExtractor.getNullableValue` gör `.findFirst()` och ignorerar tyst resten.

### 1.6 Övrigt

- `metadata_label_attribute` (`V1_37`): fri key/value, unik på `(metadata_label_id, key)`. Nycklar **inte** whitelistade (`ValidLabelAttributesConstraintValidator.hasUniqueAttributeKeys`).
- `ErrandPhaseService.processPhaseChange` och `.validateStatusAgainstActivePhase` returnerar direkt när ingen fas finns ⇒ oanvänd fasmodell kostar noll.
- `errand.id` är `varchar(255)` (`V1_0`).
- pw-alkt är stateless. `AbstractTaskWorker.clearUpdateAvailable` varnar för races vid skrivning av processvariabler. `alkt-ansokan.bpmn` innehåller **inget** `updateAvailable`; `clearUpdateAvailable` har **inga anropare**.
- Varje PW-tjänst har eget API i WSO2 ⇒ en OAuth2-registrering per PW-tjänst.
- **SM har Testcontainers.** `application-it.yml` (`spring.datasource`): `driver-class-name: org.testcontainers.jdbc.ContainerDatabaseDriver`, `url: jdbc:tc:mariadb:10.6:///ittest`. Uppsättningen syns inte i `src/integration-test/java` eftersom den sker via JDBC-URL, inte via en `@Container`-deklaration. `schema-generation: validate` är påslaget. MariaDB **10.6** ⇒ `SKIP LOCKED` är tillgängligt.
- `json_parameter` (`V1_43`–`V1_45`): `errand_id`, `parameter_key`, `schema_id`, `value longtext`, egen `version`. Unik på `(errand_id, parameter_key)`, `@OneToMany` på `ErrandEntity`, validerad mot JSON-schema via `ValidJsonParameterConstraintValidator` och `JsonSchemaClient`. Vanliga parametrar duger inte till fritext: `parameter_values.value` är `varchar(255)`.
- **Varken `ErrandJsonParameterService` eller `ErrandParameterService` skapar revision eller event.** En parameterskrivning höjer `errand.version` och gör i övrigt ingenting. `EventSubType.DECISION` finns i enumet men används ingenstans i kodbasen.
- `ErrandService.createErrand` tar `referredFrom` och skapar en relation via `RelationClient` — den befintliga vägen att koppla ihop två ärenden.
- `truncate.sql` måste utökas med varje ny tabell.

### 1.7 Varje anrop till `createErrandEvent` sväljer undantag

`ErrandService.createErrand/updateErrand/deleteErrand`, `ErrandAttachmentService.createErrandAttachmentInternal/deleteErrandAttachment/createErrandAttachment(AttachmentEntity, …)`,
`EmailReaderWorker.processErrand` och `MessageExchangeSyncService.syncConversation` har alla `try { … } catch (final Exception e) { LOG.warn(…) }` runt anropet.

`EventService.createErrandEvent` har **redan** en inre `try/catch` runt eventlog-anropet, så de yttre fångarna är
redundanta för sitt uttalade syfte. Men de finns, och de fångar `Exception`. En publicering som hängs in
sist i `createErrandEvent` och bara kastar blir därför en WARN-rad i loggen medan ärendeskrivningen
committar — dual-write-problemet outboxen skulle lösa, återinfört. Motmedlet står i §2.2.

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
                                                    PUT process-instances (tillstånd + aktiviteter)
                                                                          |
                                                                         SM
```

### 2.2 Publiceringsregeln

`ProcessEventPublisher.publish(errandEntity, eventType, eventSubType, executedBy, requestGroupId)` anropas sist i `EventService.createErrandEvent`, i samma transaktion. `sendNotification`-flaggan är irrelevant — processevent är inte notiser.

```
1. PROCESS_CONSUMER för (municipalityId, namespace)?      nej -> return
2. executedBy == PROCESS_CONSUMER?                        ja  -> return        (loop-skydd, lager 1)
3. Levererade event för ärendet i fönstret > tröskel?      ja  -> ERROR-aktivitet, return  (lager 3)
4. eventSubType i PROCESS_TRIGGER?                        nej -> return        (lager 2)
5. processKey ur etiketterna                              0 -> return; >1 -> ERROR-aktivitet, return
6. INSERT process_event_outbox
```

Steg 1 är en cachad map-uppslagning. Namespace utan process betalar inget mer.

ERROR-aktiviteterna i steg 3 och 5 skrivs **utan processinstans** — de inträffar per definition när
ingen instans finns (§3.1).

**Publiceringen får inte kunna svälja sitt eget fel.** Anropsställena fångar `Exception` (§1.7), och
regeln att inte lägga till en nionde svalgande fångst är inte en garanti. Därför:

```java
// ProcessEventPublisher, vid varje fel som inte är ett medvetet "return" enligt regeln ovan
if (TransactionSynchronizationManager.isActualTransactionActive()) {
    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
}
throw ...;   // anropsstället får svälja detta; transaktionen kan ändå inte committa
```

Effekten: en utebliven outbox-rad fäller ärendeskrivningen (`UnexpectedRollbackException` vid commit),
oavsett vad anropsstället gör med undantaget. Namespace utan `PROCESS_CONSUMER` returnerar i steg 1 och
påverkas aldrig.

Saknas transaktion helt går det inte att rulla tillbaka — då är ärendeskrivningen redan committad.
Det loggas som ERROR och räknas i `process_event.publish_failed` (§8.1); i dagsläget har varje
anropsväg en transaktion (`ErrandService`, `EmailReaderWorker.processEmail`, `MessageExchangeWorker.processConversation`).

Att ta bort de yttre fångarna löser inte problemet ensamt — det skulle också göra notisfel dödliga för
ärendeskrivningen. Gör det i så fall som separat städning.

### 2.3 Latens

`@TransactionalEventListener(AFTER_COMMIT, fallbackExecution = true)` + `@Async("processEventExecutor")` (core 2 / max 4 / kö 500, **`AbortPolicy`**). Mönstret finns i `SubscriptionService.handleAutoSubscribeEvent`.

Cron `0 * * * * *` som nät, samma kodväg, buffert 5 s **endast** där. Ordning per ärende via `@Lock(PESSIMISTIC_WRITE)` på radgruppen sorterad på `created, id`.

### 2.4 Transport

**Nu REST**, eftersom outboxen byggs ändå och brokern inte är bevisat driftklar. Det senare är enda grinden — SM har Testcontainers (§1.6), så en `RabbitMQContainer` är ingen tröskel.

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

alter table if exists errand_process_instance
    add constraint fk_epi_errand_id foreign key (errand_id) references errand (id) on delete cascade;

-- 3. Append-only faktalogg. Processagnostisk: inga FK mot SM-metadata, ingen validering.
create table if not exists errand_process_activity (
    id                         varchar(36)  not null,
    -- Nullbar med flit: SM skriver CONFIG/ERROR-poster (tvetydig etikett, nodbroms) innan
    -- nagon processinstans finns. Se 4.2. errand_id ar da enda kopplingen.
    errand_process_instance_id varchar(36)  null,
    errand_id                  varchar(255) not null,
    external_task_id           varchar(64),             -- idempotensnyckel, stabil over retries
    activity_type              varchar(64)  not null,   -- fri strang: PHASE | TASK | INCIDENT | CONFIG | CONCURRENCY
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
    constraint fk_epa_errand foreign key (errand_id)
        references errand (id) on delete cascade,
    constraint uq_epa_idempotency unique (errand_process_instance_id, external_task_id, activity_id)
) engine=InnoDB;

create index if not exists idx_epa_instance_occurred on errand_process_activity (errand_process_instance_id, occurred_at);
create index if not exists idx_epa_errand_occurred   on errand_process_activity (errand_id, occurred_at);
create index if not exists idx_epa_retention         on errand_process_activity (created);
```

### 3.2 FK och kaskad

| Tabell | FK | Motiv |
|---|---|---|
| `process_event_outbox` → `errand` | **Ingen, medvetet** | Samma val som `notification_dispatch` (`V1_38`). Ett DELETE-event måste överleva att ärendet raderas |
| `errand_process_instance` → `errand` | `ON DELETE CASCADE`, **ingen JPA-relation på `ErrandEntity`** | DB-kaskaden räcker för att undvika föräldralösa rader vid `repository.deleteById` (`ErrandService.deleteErrand`). JPA-mappning vore aktivt skadlig (§1.4) |
| `errand_process_activity` → instans | `ON DELETE CASCADE`, **nullbar** | Poster utan instans måste kunna skrivas (§4.2) |
| `errand_process_activity` → `errand` | `ON DELETE CASCADE` | Krävs när instans-FK:n är nullbar — annars överlever instanslösa poster ärendet. InnoDB tillåter båda kaskadvägarna parallellt |

**Retention:** aktiviteter röjs på `created` (default 365 d); outbox-rader röjs när `delivered_at` är äldre än **max(loop-guard-fönstret × 6, 24 h)**; dead letters enligt §8.3.

---

## 4. Domänmodell

### 4.1 `ProcessStatus` — beteendet bor på enumet

Statusen styr en sak till utöver att visas: `active_marker`, och därmed 1-1-invarianten. Den kopplingen
måste bo på enumet, inte i en mappningstabell någon annanstans.

```java
package se.sundsvall.supportmanagement.integration.db.model.enums;

public enum ProcessStatus {

    /** En worker exekverar just nu. */
    RUNNING   (false),

    /** Processen väntar på handläggare, timer eller extern part. Lever, men arbetar inte. */
    WAITING   (false),

    /** Ett försök fallerade, Operaton kommer att försöka igen. Lever, men arbetar inte. */
    RETRYING  (false),

    /** Processen nådde sitt slut. */
    COMPLETED (true),

    /** Retries uttömda, incident rest, eller starten misslyckades (errorCode säger vilket). */
    FAILED    (true);

    private final boolean terminal;

    ProcessStatus(final boolean terminal) {
        this.terminal = terminal;
    }

    /** Styr active_marker — och därmed hur många levande instanser ett ärende kan ha. */
    public boolean isTerminal() { return terminal; }
}
```

> **Fällan:** `WAITING` betyder att processen inte arbetar, och det är lätt att läsa som "klar".
> Behandlas den som terminal får instansen `active_marker = NULL`, och då kan en **andra**
> processinstans startas för samma ärende. Hela 1-1-invarianten faller. Ett tabelldrivet test måste
> räkna upp **varje** enumvärde mot `isTerminal()`, med `WAITING` explicit verifierad som icke-terminal.

Motsvarande i pw-alkt (`se.sundsvall.alkt.api.model.ProcessStatus`) med samma fem värden.

### 4.2 Övriga enums

```java
public enum ActivitySeverity { INFO, WARN, ERROR }
```

`activityType` och `activityId` är **fria strängar** — SM tolkar dem inte. Konvention i pw: `PHASE`, `TASK`, `INCIDENT`. SM skriver själv `CONFIG` (tvetydig etikett, okänd processKey) och `CONCURRENCY` (två external tasks samtidigt, §6.4).

**Poster utan processinstans.** `CONFIG`-posterna och nödbromsens ERROR-post uppstår när etiketterna är
tvetydiga eller när eventflödet skenar — i båda fallen finns typiskt **ingen** instans att hänga posten
på. Därför är `errand_process_instance_id` nullbar (§3.1) och aktiviteter läses ärendescopat (§5.2).

### 4.3 JPA-entitet — `ErrandProcessInstanceEntity`

Följer mönstret i `NamespaceConfigEntity`: `@PrePersist`/`@PreUpdate` för tidsstämplar, `@TimeZoneStorage(NORMALIZE)`.

```java
@Entity
@Table(name = "errand_process_instance",
    indexes = {
        @Index(name = "idx_epi_errand_id", columnList = "errand_id")
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

    @Column(name = "started") private OffsetDateTime started;
    @Column(name = "ended")   private OffsetDateTime ended;

    /** 1 medan instansen lever, null nar den ar terminal. Bar unikhetsconstrainten. */
    @Column(name = "active_marker")
    private Byte activeMarker;

    @Column(name = "created")  private OffsetDateTime created;
    @Column(name = "modified") private OffsetDateTime modified;

    /** Enda stallet som far satta status - haller active_marker och ended i synk. */
    public void applyStatus(final ProcessStatus status, final Clock clock) {
        this.processStatus = status;
        this.activeMarker  = status.isTerminal() ? null : (byte) 1;
        this.ended         = status.isTerminal() ? OffsetDateTime.now(clock) : null;
    }
}
```

**`applyStatus` är enda vägen att sätta status.** Sätts `processStatus` direkt någon annanstans hamnar `active_marker` ur synk — och det är exakt fällan i §4.1. Gör settern privat/paketprivat.

`ended` nollas när status blir icke-terminal. Det spelar roll när en incident löses manuellt i Operaton och en `FAILED` instans börjar köra igen: raden ska inte bära en sluttid mitt under körning. Notera att `active_marker`-platsen frigjordes när instansen blev terminal — hann ett nytt flöde starta en instans dessförinnan ger återupplivningen `409` på `uq_epi_one_active_per_errand`. Det är rätt utfall, men felmeddelandet måste peka ut den blockerande instansen.

---

## 5. API

### 5.1 SM — skrivning (enda skrivendpointen)

```http
PUT /{municipalityId}/{namespace}/errands/{errandId}/process-instances/{processInstanceId}
X-Sent-By: pw-alkt; type=processEngine
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
  "errandVersion": 7,
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

`errandVersion` är valfri och bär den version av ärendet workern läste. Är den satt och har glidit svarar
SM `412` och skriver ingenting — se §6.3. Workers som skriver tillbaka till ärendet behöver den inte;
för dem gör `If-Match` på själva PATCH:en samma jobb.

Svar `201 Created` (första gången, med `Location`) eller `200 OK`:

```json
{
  "id": "1f0e4c21-...",
  "processInstanceId": "8f1c2b6e-...",
  "processStatus": "RUNNING"
}
```

**Felrapport** — samma endpoint:

```json
{ "processStatus": "FAILED",
  "currentActivityId": "investigation_fetch_decision",
  "externalTaskId": "a91c...",
  "error": { "code": "INCIDENT", "message": "Timeout mot Employee efter 30 s" } }
```

**`POST .../process-instances` — registrera ett startförsök.** Två fall:

| Utfall i Operaton | Kropp | SM svarar |
|---|---|---|
| Start lyckades | `processKey`, `processInstanceId`, `processStatus: RUNNING` | Raden saknas ⇒ `201` med `Location`. Raden finns redan med **samma** `processInstanceId` ⇒ `200`, och ingenting ändras. Annan **levande** instans för ärendet ⇒ `409` |
| Start misslyckades | `processKey`, `processStatus: FAILED`, `error` — **inget** `processInstanceId` | Terminal rad skapas ⇒ `201` |

```json
{ "processKey": "alkt-ansokan", "processStatus": "FAILED",
  "error": { "code": "START_FAILED", "message": "..." } }
```

**Ett avslutat processliv startas inte om.** Har ärendet en instans med `COMPLETED` svarar `POST` `409`
även om ingen levande instans finns. En `FAILED` instans blockerar däremot inte — omstart efter ett
misslyckat startförsök är återhämtning, inte en ny process (§7.4).

**Regeln som gör det hela ofarligt: `POST` skapar, den uppdaterar aldrig.**

Operaton kan göra den första external task:en tillgänglig innan `startProcessInstance` ens returnerat till
pw. En worker — kanske i en annan pod — hinner alltså `PUT`:a sin `RUNNING`-rapport innan pw:s `POST`
landar. Eftersom `PUT` är upsert och `POST` är create-only spelar ordningen ingen roll: den som kommer
först skapar raden, den andra blir en no-op respektive en vanlig tillståndsuppdatering. Utan regeln hade
pw fått `409` på en fullt frisk process och, enligt §9.3, avbrutit den.

Implementationen får inte bero på vilken constraint som råkar fyra först — `uq_epi_process_instance_id`
och `uq_epi_one_active_per_errand` kan båda träffa på samma insert. Slå upp på `process_instance_id`
i stället, före och efter:

```
existing = findByProcessInstanceId(piid)
if existing: return 200 existing            // nagon hann fore, ror ingenting

if hasCompletedInstance(errandId):
    throw 409                               // processlivet ar over, se 7.4

try:
    insert(... RUNNING ...)
    return 201
catch ConstraintViolationException:
    existing = findByProcessInstanceId(piid)
    if existing: return 200 existing        // kapplopningen vanns av den andra
    throw 409                               // annan levande instans for arendet
```

Startfelsfallet kan inte kapplöpa: utan `processInstanceId` finns ingen processinstans, och därmed ingen
external task och ingen worker.

### 5.2 SM — läsning

```
GET .../errands/{errandId}/process-instances                        -> 200 List<ProcessInstance>
                                                                       nyast först; normalt exakt ett element
GET .../errands/{errandId}/process-activities                       -> 200 Page<ProcessActivity>
                                                                       ?processInstanceId= (valfritt filter)
                                                                       &page=&size=50 (max 200)&sort=occurredAt,desc
GET .../errands/{errandId}                                          -> 200 Errand med process-objektet
GET .../process-labels                                              -> 200 List<ProcessLabelMapping> (diagnostik)
```

Aktivitetsläsningen är **ärendescopad, inte instansscopad** — annars går poster utan instans (§3.1)
inte att läsa, och det är de posterna som förklarar varför ingen process startade.

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
    private Long errandVersion;                    // valfri; version workern laste (6.3)
    private OffsetDateTime started;
    private OffsetDateTime ended;
    private ProcessError error;
    private List<ProcessActivity> activities;      // skrivs in, lases via egen endpoint
    @Schema(accessMode = READ_ONLY) private OffsetDateTime created;
    @Schema(accessMode = READ_ONLY) private OffsetDateTime modified;
}

public class ProcessActivity {
    @Schema(accessMode = READ_ONLY) private String id;
    @Schema(accessMode = READ_ONLY) private String processInstanceId;   // null for poster SM skrivit utan instans
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

/** Lasprojektion pa Errand: SENASTE instansen, inte den levande. Null for namespace utan processmodell. */
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

**`process` projicerar den senaste instansen, inte den levande.** Skillnaden spelar roll i två lägen som
båda är sådana handläggaren måste se: en misslyckad start lämnar en terminal `FAILED`-rad och ingen levande
instans, och en avslutad process lämnar en `COMPLETED`-rad och ingen levande instans. Projicerades bara den
levande skulle båda visa `null`, alltså samma sak som "ärendet har ingen process" — och felstavat `processKey`
vore osynligt trots att §11 lovar motsatsen.

Följden för berikningen: frågan är "senaste per ärende", inte "där `active_marker = 1`". Över en mängd
ärende-id kräver det en window function (`ROW_NUMBER()`, finns i MariaDB 10.6) eller en join mot
`max(created)`. Det ska fortfarande vara **en** fråga.

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
    private String processKey;                  // satt nar arendets etiketter ger en nyckel; styr start (9.3)
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
        Long errandVersion,
        ProcessError error,
        List<ProcessActivity> activities) {

    public static ProcessStateReport running(String activityId, String activityName) { ... }
    public static ProcessStateReport waiting(String activityId, String activityName) { ... }
    public static ProcessStateReport completed() { ... }
    public static ProcessStateReport failed(String code, String message) { ... }
    public static ProcessStateReport retrying(String code, String message) { ... }
}
```

Fabriksmetoderna finns för att en worker inte ska behöva komma ihåg vilken status som är terminal.

### 5.6 Statuskoder i SM

| Kod | När |
|---|---|
| `400` | `activePhaseId` sätts på ärende med processinstans; etikettändring som byter `processKey` |
| `403` | `X-Sent-By` saknas; `processService` matchar inte namespacets `PROCESS_CONSUMER` |
| `404` | Ärendet finns inte eller ligger i annat namespace |
| `409` | Annan levande instans för ärendet **med ett annat `processInstanceId`**; ärendet har redan en `COMPLETED` instans (§7.4); instans med annat `process_key` än ärendets befintliga. Samma `processInstanceId` är aldrig `409` — se §5.1 |
| `412` | `errandVersion` i rapporten matchar inte ärendets aktuella version (§6.3) |

### 5.7 Beteendeförändring på befintliga endpoints

**Ingen.**

Inga nya statuskoder, inga nya spärrar, inget nytt felfall att hantera i frontend. Optimistisk kontroll
(§6.2) använder `If-Match` och `412`, som redan finns på ärende- och parameterendpointsen och redan är
dokumenterade i specen. Verksamheter utanför ALKT ser ingen skillnad alls.

Vad frontend däremot **bör** göra är att visa processläget. `errand.process.processStatus` och
`currentActivityName` säger om en process arbetar med ärendet just nu, och *"Processen arbetar med
ärendet: Hämtar beslutsunderlag"* räcker för att handläggaren ska förstå varför ärendet kan ändras
under fötterna. Att skicka `If-Match` från frontend är också en förbättring — då upptäcks kollisionen
i stället för att sista skrivningen vinner — men det är en fristående förbättring, inte ett krav
härifrån.

### 5.8 Borttaget i pw-alkt

`POST /process/start/{errandId}` och `POST /process/update/{processInstanceId}` **tas bort** i samma steg som `errand-events` införs. Kaskad: `ProcessService.updateProcess`, `StartProcessResponse` + test, `AbstractTaskWorker.clearUpdateAvailable` (död kod), `Constants.PROCESS_VARIABLE_UPDATE_AVAILABLE`, `Constants.FALSE`, `OperatonClient.setProcessInstanceVariable(s)`, `AbstractTaskWorker.setProcessInstanceVariable`.

Ingen escape hatch går förlorad: `POST errand-events` fungerar som manuell trigger via samma kodväg som produktionsflödet.

---

## 6. Samtidighet och loop-skydd

### 6.1 Livscykeln bärs av rapporten

| Rapporterad status | `active_marker` |
|---|---|
| `RUNNING` | 1 |
| `WAITING` | 1 |
| `RETRYING` | 1 |
| `COMPLETED` | NULL |
| `FAILED` | NULL |

En **annan** levande instans kan inte finnas samtidigt — `uq_epi_one_active_per_errand` hindrar det (§7.4).

### 6.2 Samtidighet — optimistisk kontroll, inget lås

**Kravet är att handläggarens ändringar inte tappas, inte att handläggaren hindras.** Det avgör
mekanismen, och SM har den redan.

`ErrandEntity` har `@Version`, och de sex skrivvägar som rör ärendets beslutsunderlag —
`ErrandService.updateErrand`, `ErrandParameterService.updateErrandParameters`/`updateErrandParameter`/`deleteErrandParameter`
och `ErrandJsonParameterService.updateJsonParameter`/`deleteJsonParameter` — gör alla
`entityManager.lock(..., OPTIMISTIC_FORCE_INCREMENT)`. Ingen annan väg i SM höjer versionen med tvång:
bilagor, kommunikation, konversationer, anteckningar och notiser ligger i egna tabeller och rör inte
ärenderaden.

`errand.version` betyder alltså redan exakt **"ärendets beslutsunderlag har ändrats"**.
`GET /errands/{id}` returnerar den både som `ETag`-header och i
`version`-fältet, `PATCH` tar `If-Match` och svarar `412`, och `ETagUtil.validateIfMatch` returnerar
direkt när headern saknas. Allt är opt-in per klient, i drift och testat.

**Regeln för pw:**

1. Workern läser ärendet och behåller `ETag`.
2. Workern skriver tillbaka med `If-Match: "<version>"`.
3. `412` ⇒ handläggaren hann före. Workern rapporterar `RETRYING` och kastar; Operaton kör om steget,
   och vid omkörningen läser workern om och beslutar om.

Skyddsfönstret är ett arbetssteg — läs, besluta, skriv — och det är exakt vad `If-Match` täcker.

En kraschad worker lämnar ingenting efter sig som måste städas: det finns inget tillstånd att städa. Inga
befintliga endpoints ändrar beteende (§5.7).

**Priset** är att arbete kan behöva göras om. Det förutsätter att ett arbetssteg tål omkörning — vilket
Operatons egen retry redan kräver (§9.4), så det är inget nytt krav. Steg med extern sidoeffekt bör
lägga sidoeffekten sist, så att en omkörning inte dubblerar den.

### 6.3 Workers som inte skriver tillbaka

En worker som bara läser ärendet och gör något externt — skickar ett brev, anropar en extern part — har
inget att kollidera på.

Lösningen kostar ett fält. Rapporten (§5.1) bär `errandVersion`: den version workern läste. SM jämför mot
`errand.version` och svarar **`412`** om den glidit, utan att skriva tillstånd eller aktiviteter. Workern
behandlar det som vilket `412` som helst — `RETRYING`, kasta, låt Operaton köra om.

Rapporten är strukturellt obligatorisk (§5.5), så inget nytt anropsmönster tillkommer. Fältet är valfritt:
utelämnas det görs ingen kontroll, vilket är rätt för workers som varken läser eller skriver ärendet.

### 6.4 Parallella grenar — modelleringsregel, inte kod

Operaton kan ha två external tasks aktiva samtidigt i samma processinstans (parallell gateway). Skriver
båda mot ärendet får den ena `412`, kör om, och slår i sin tur ut den andra. Det konvergerar inte av sig
självt.

**Beslut: en modelleringsregel.** BPMN-modellerna får inte ha parallella grenar där mer än en gren muterar
ärendet. För myndighetsprocesser är parallell mutation av samma ärende ändå en modelleringssmell.

**Regeln görs synlig i stället för att förutsättas.** Rapporterar två skilda `externalTaskId` `RUNNING` mot
samma instans utan en terminal rapport emellan, skriver SM en `errand_process_activity` med
`severity = WARN` och texten *"concurrent external tasks detected"*, och räknar upp
`process_instance.concurrent_task_detected`. Båda rapporterna tas emot — ett avslag här hade tystat just
den post som ska göra regelbrottet synligt.

### 6.5 Loop-skydd

**Lager 1 — origin-filter.** `executedBy` (`X-Sent-By`) lika med namespacets `PROCESS_CONSUMER` ⇒ ingen
outbox-rad. Processens egna skrivningar väcker inte processen. Filtret sitter vid **publicering**, inte vid
leverans: raden ska inte skrivas alls, annars räknar nödbromsen fel.

**Lager 2 — trigger-filter** på `(EventType, EventSubType)`. Ortogonalt: ser på *vad* som hände, inte *vem*.

**Lager 3 — orsaksagnostisk nödbroms.** Räkna rader med `errand_id = ? and created > now() - window`. Över
tröskeln (20 / 10 min): ingen rad, ERROR-aktivitet utan instans, hälsoindikator unhealthy.

> Räkningen kräver att outbox-rader **soft-deletas** (`delivered_at`). Raderades de direkt skulle en snabb
> loop aldrig lämna mer än en rad och bromsen vore verkningslös precis när den behövs.

**Inte** ett loop-skydd: versionsbaserad idempotens — versionen stiger varje varv.

**Ärligt om lager 1:** det vilar på en klientsatt header. En pw-tjänst som glömmer `X-Sent-By` loopar tills
lager 2 eller 3 fångar den. Motmedel: `RequestInterceptor` sätter headern på **alla** utgående anrop (P3),
och `process_event.suppressed{reason=ORIGIN}` ska vara skild från noll i drift — är den noll skriver
processen antingen inte alls, eller så är headern fel.

## 7. Konfiguration och processval

### 7.1 `namespace_config`

| Nyckel | Typ | Antal | Värde |
|---|---|---|---|
| `PROCESS_CONSUMER` | STRING | 1 | `pw-alkt` |
| `PROCESS_TRIGGER` | STRING | N | `ERRAND`, `MESSAGE`, `ATTACHMENT`, `DECISION` |

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
  (42, 'PROCESS_TRIGGER',  'ATTACHMENT', 'STRING'),
  (42, 'PROCESS_TRIGGER',  'DECISION',   'STRING');
```

**`ERRAND` är obligatorisk** — utan den startar aldrig ett ärende som får sin etikett i ett andra anrop.
Triggern räcker dock inte ensam: pw måste starta på förekomsten av `processKey`, inte på `eventType`,
annars faller samma fall ändå bort på mottagarsidan. Se §9.3.

**`DECISION` är lika obligatorisk.** Handläggaren fattar beslutet och processen väntar på det (§7.5).
Saknas triggern får processen aldrig veta att beslutet är fattat och instansen står kvar i `WAITING`
för alltid.

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
  loop-guard: { max-events-per-errand: 20, window: PT10M }
  consumers:
    pw-alkt: { identifier: pw-alkt }        # matchas mot X-Sent-By
scheduler:
  process-event:   { cron: "0 * * * * *", name: processEvent,   lockAtMostFor: PT2M }
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
4. **En `COMPLETED` instans avslutar ärendets processliv.** En ny instans får inte startas. Nästa process är ett nytt ärende (beslut 6). En `FAILED` instans får däremot startas om — det är återhämtning efter ett misslyckat startförsök.

Regel 4 är inte uttryckbar i DB: `active_marker` är NULL för både `COMPLETED` och `FAILED` och skiljer dem inte åt. Kontrollen ligger i `POST .../process-instances` (§5.1). Att lägga den där och inte bara i pw är avsiktligt — pw frågar Operatons runtime, som inte känner till avslutade instanser alls (§9.3).

Constraint-violation **måste översättas**, aldrig bubbla upp som `500`. Men de två unika indexen betyder olika saker och får inte behandlas lika:

| Constraint | Betydelse | Utfall |
|---|---|---|
| `uq_epi_process_instance_id` | Någon registrerade **samma** instans först — kapplöpningen i §5.1 | Läs om raden, returnera den. Inget fel |
| `uq_epi_one_active_per_errand` | En **annan** levande instans blockerar | `409` med `detail` som pekar ut den |

Eftersom en och samma insert kan träffa båda, avgörs utfallet av en uppslagning på `process_instance_id` — inte av vilket index som råkade fyra (§5.1).

---

### 7.5 Processens utfall — beslutet

**Ett ärende, en processinstans, ett beslut.** Ska ett nytt beslut fattas skapas ett nytt ärende, kopplat
till det ursprungliga. Kopplingen finns redan: `POST /errands` tar `referredFrom` och `ErrandService.createErrand`
skapar relationen via `RelationClient` — samma väg handover använder.

Regeln är den yttersta av tre som säger samma sak på olika nivåer:

| Nivå | Invariant | Upprätthålls av |
|---|---|---|
| Processinstans | Högst en levande per ärende | `uq_epi_one_active_per_errand` |
| Processliv | En `COMPLETED` startas aldrig om | `hasCompletedInstance` i `POST` (§7.4) |
| Beslut | Ett per ärende | `uq_json_parameter_errand_id_key` |

#### Var beslutet lagras

Som en **`json_parameter` med nyckeln `alkt.beslut`** och ett registrerat JSON-schema:

```json
{ "utfall": "BIFALL",
  "beslutsdatum": "2026-09-14",
  "beslutsfattare": "anna.andersson",
  "delegationspunkt": "3.2.1",
  "motivering": "..." }
```

Unikheten på `(errand_id, parameter_key)` **är** invarianten — ingen ny tabell, ingen ny endpoint och
ingen kod som upprätthåller regeln. Dokumentet valideras mot schemat vid systemgränsen, har egen
`version` för ETag, och ligger som `@OneToMany` på `ErrandEntity` och därmed i revisionssnapshotten.

Tre alternativ valdes bort. **Kolumner på `errand_process_instance`**: ett myndighetsbeslut är ärendedata,
inte processmaskineri — det måste kunna läsas av e-tjänst och arkiv utan kännedom om Operaton, och
tabellen ligger medvetet utanför ärendets aggregat (§1.4), alltså utan revision och event. **Vanliga
parametrar**: `parameter_values.value` är `varchar(255)`, en motivering får inte plats. **Aktivitetsloggen**:
den är en logg SM inte tolkar, utan unikhet, och `message` får enligt §11 inte innehålla PII — vilket en
beslutsmotivering nästan alltid gör.

#### Vem som skriver, och vad det kräver

**Handläggaren fattar beslutet**, baserat på det underlag som kommit in. Processen väntar på det. Två saker
följer, och båda är förutsättningar för att flödet ska fungera alls:

1. **`DECISION` måste ligga i `PROCESS_TRIGGER`** (§7.1).
2. **`ErrandJsonParameterService.updateJsonParameter` måste skapa event och revision** med
   `EventSubType.DECISION`. I dag skapar den ingetdera (§1.6), så utan den ändringen publiceras aldrig
   något processevent och processen vaknar aldrig. `EventSubType.DECISION` finns redan i enumet, oanvänd.

Beslutsskrivningen forcerar `errand.version` precis som andra parameterskrivningar, så en worker som
håller en äldre ETag får `412` och kör om sitt steg (§6.2). Det är rätt utfall: beslutet ändrade underlaget.

#### Låsning

Beslutet får skrivas medan processen lever — det steg som förbereder beslutet kan behöva korrigera sig
självt. När instansen är `COMPLETED` är det låst: samma `hasCompletedInstance(errandId)` som hindrar
omstart av processen (§7.4) hindrar då också ändring av beslutet. En kontroll, två användningar. Rättelse
eller omprövning sker genom ett nytt ärende.

#### Modelleringskravet detta hänger på

Wait state:t som väntar på beslutet **måste omvärdera sitt villkor vid inträde** (§9.2 punkt 1). Skrivs
beslutet medan processen är mitt i ett arbetssteg sväljs korrelationen som en `MismatchingMessageCorrelation`
och väckningen är borta. Läser processen inte om ärendet när den går in i väntan står ärendet stilla för
alltid — med ett fattat beslut liggande i databasen. Det är den enskilt allvarligaste modelleringsmissen
som går att göra i den här lösningen.

---

## 8. Drift och förvaltning

### 8.1 Mätvärden som måste finnas

Utan dessa är loop-skyddet och samtidighetsbeteendet otestbara i drift.

| Mätvärde | Varför |
|---|---|
| `process_event.suppressed{reason=ORIGIN\|TRIGGER\|GUARD}` | Ett tyst loop-skydd som slutar fungera märks annars först när loopen är där. `ORIGIN` ska vara **skild från noll** |
| `process_event.published`, `.delivered`, `.dead_lettered` | Leveranshälsa |
| `process_event.publish_failed` | Publicering som inte kunde rullas tillbaka (ingen aktiv transaktion, §2.2). Ska vara noll |
| `process_event.nudge_rejected` | Executorn mättad ⇒ latensen faller tillbaka på cron |
| `process_instance.errand_conflict` | **Viktigaste driftindikatorn.** Hur ofta process och handläggare krockar (`412`). Stiger den arbetar processen på ärenden som redigeras samtidigt, och arbete görs om i onödan |
| `process_instance.concurrent_task_detected` | Brott mot modelleringsregeln i §6.4 |
| `process_instance.start_failed` | Feltaggade etiketter |

Logga alltid `eventId`, `errandId`, `processInstanceId` och `X-Request-Group-Id` — dubbelleveranser blir då spårbara i efterhand.

### 8.2 Vanliga driftfrågor och svaren

| Fråga | Svar |
|---|---|
| Varför startade ingen process för ärendet? | `GET .../process-instances` tom + `errand.process` null. Kontrollera etikett-tagg via `GET /process-labels`, och att `PROCESS_CONSUMER` finns för namespacet |
| Varför kör processen om samma steg gång på gång? | Handläggaren ändrar ärendet mitt i steget ⇒ `412` (§6.2). Se `process_instance.errand_conflict` och aktivitetsloggen |
| Varför väcks inte processen av inkommande e-post? | `MESSAGE` saknas i `PROCESS_TRIGGER` |
| Varför väcks processen inte av sina egna ändringar? | Det är meningen — lager 1 i §6.5 |
| Varför står instansen kvar som `RUNNING` fast inget händer? | Workern kraschade utan att rapportera. Operaton kör om task:en när dess eget lås löper ut; instansen uppdateras vid nästa rapport |

### 8.3 Dead letters — bygg en väg tillbaka

`NotificationDispatchWorker.cleanUpDeadLetters` **raderar** dead letters efter 7 dagar. Ärv inte det rakt av: en dead-letterad processhändelse betyder att en process aldrig fick veta något, och att bara radera den gör felet permanent och osynligt.

**Krav:**

- Dead letters röjs **inte** automatiskt inom retentionstiden; en unhealthy hälsoindikator hålls så länge det finns odelivererade.
- Administrativ endpoint `POST /{municipalityId}/{namespace}/process-events/{id}/redrive` som nollar `retry_count`, `dead_letter` och `next_retry_at`.
- Röjning först efter konfigurerad retention (default 30 d), med logg på vad som togs bort.

### 8.4 Verifiera lokalt

1. Skapa namespace-config med `PROCESS_CONSUMER=pw-alkt` och `PROCESS_TRIGGER=ERRAND,MESSAGE`.
2. Tagga en label med `processKey=alkt-ansokan`.
3. `POST /2281/ALKT/errands` med den labeln ⇒ rad i `process_event_outbox` inom en sekund, `delivered_at` satt när stubben svarat.
4. `GET /2281/ALKT/errands/{id}` ⇒ `process.processStatus = RUNNING`, och `ETag` i svarshuvudet.
5. `PATCH` samma ärende med den ETag:en ⇒ `200`. `PATCH` igen med **samma** ETag ⇒ `412`.
6. Låt stubben rapportera med ett `errandVersion` som ligger efter ⇒ `412` på rapporten, inget tillstånd skrivet.

## 9. pw-alkt

### 9.1 En BPMN-fil per processdefinition

`alkt-ansokan.bpmn`, `alkt-tillsyn.bpmn`. `TenantAwareAutoDeployment.deployResources` deployar en deployment per fil — gemensam fil hade betytt att en ändring i tillsyn versionerar upp ansökan och drar in pågående instanser.

Processens `id` måste matcha `Constants.PROCESS_KEY_*`. **`ProcessWithoutDeviationIT.setup` väntar på `getDeployments(...).size() == 1`** och måste justeras när fil två läggs till.

### 9.2 Modelleringskrav

Dessa är förutsättningar för att designen ska hålla — de är inte råd.

1. **Wait states ska omvärdera sitt villkor vid inträde.** Läs aktuellt ärendetillstånd och avgör om villkoret redan är uppfyllt innan väntan börjar. En sväljd `MismatchingMessageCorrelation` kan vara en legitim väckning som kom medan processen var mellan två wait states; att det ändå är ofarligt vilar helt på detta. **Det skarpaste fallet är beslutet** (§7.5): skrivs det medan processen arbetar och väntan inte omvärderar, står ärendet stilla för alltid med ett fattat beslut i databasen.
2. **Inga parallella grenar som muterar ärendet** (§6.4).
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
        om event.processKey saknas -> logga, 202       // arendet har ingen processetikett
        om SM har en COMPLETED instans -> logga, 202   // processlivet ar over, se 7.4
        om processKey inte deployad -> 422
        start med businessKey = errandId
        POST .../process-instances {RUNNING}           // 200 = nagon hann fore, ok
                                                       // 409 = avslutad eller annan levande
                                                       //       instans -> avbryt den nystartade
    annars:
        correlateMessage(messageName = "errandUpdated", businessKey = errandId, tenantId = "ALKT")
```

**`findProcessInstances` ser bara Operatons runtime.** En avslutad process finns inte där — den ligger i
historiken. Utan kontrollen mot SM skulle alltså varje triggande händelse efter `COMPLETED` starta en helt
ny process på samma ärende: ett meddelande eller en bilaga som kommer in efter beslut skulle dra igång
ansökningsprocessen från början. Kontrollen mot SM är förstahandsspärren, `409` från `POST` är
backstoppet (§7.4).

**Starten styrs av `processKey`, inte av `eventType`.** Ett ärende kan skapas utan etikett och få den i
ett andra anrop — då kommer nyckeln med ett `UPDATE`-event, inte ett `CREATE`. Startades bara på `CREATE`
skulle det ärendet aldrig få någon process, och `PROCESS_TRIGGER=ERRAND` (§7.1) vore verkningslös för
just det fall den motiveras av. Villkoret är därför: ingen levande instans **och** `processKey` satt.

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
            supportManagement.report(task, ProcessStateReport.running(activityId(task), activityName(task)));
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

Returtypen gör rapporten **strukturellt obligatorisk** — en worker kan inte kompilera utan att producera en.

**Samtidighet.** `SupportManagementClient.getErrand` returnerar ärendet tillsammans med dess `ETag`, och
`patchErrand` skickar den som `If-Match`. Ett `412` betyder att handläggaren hann före: låt undantaget gå
upp till `execute`, så rapporterar `FailureHandler` `RETRYING` och Operaton kör om steget. En worker som
läser men inte skriver sätter i stället `errandVersion` i sin rapport (§6.3).

`FailureHandler` rapporterar till SM **före** `handleFailure`: `RETRYING` när `FailureHandler.calculateRetries`
ger fler försök, annars `FAILED`. Det är vad som får felmeddelandet till ärendets UI.

---

## 10. Implementationsuppgifter

Varje uppgift är mergbar för sig. Acceptanskriterierna är avsedda att kunna klistras in i en task.

Tabellen nedan är **utförandeordningen**. T- och P-numreringen längre ner följer tjänst, inte ordning.

| Steg | Jira | Uppgift |
|---|---|---|
| 1 | DRAKEN-4714 | T1 — Datamodell och domänenums |
| 2 | DRAKEN-4715 | T2 — Konfigurationsläsning |
| 3 | DRAKEN-4716 | T3 — Process-instance-API |
| 4 | DRAKEN-4717 | T4 — Optimistisk samtidighetskontroll |
| 5 | DRAKEN-4718 | T5 — Publicering |
| 6 | DRAKEN-4719 | T6 — Relay och leverans |
| 7 | DRAKEN-4720 | T7 — Skyddsräcken |
| 8 | DRAKEN-4728 | T9 — Beslutsdokument och spårbarhet |
| 9 | DRAKEN-4721 | P1 — Operaton-klienten |
| 10 | DRAKEN-4722 | P2 — Event-endpoint och borttagning |
| 11 | DRAKEN-4723 | P3 — SM-klienten |
| 12 | DRAKEN-4724 | P4 — Workerstruktur |
| 13 | DRAKEN-4725 | T8 — `ProcessLoopGuardIT` |
| 14 | DRAKEN-4726 | P5 — Tillsynsprocessen |
| 15 | DRAKEN-4727 | P6 — Incidentåterkoppling |

### T1 — Datamodell och domänenums (SM)

**Bygg:** `V1_47`-migrering (§3.1); `ProcessStatus` med `isTerminal()` (§4.1); `ActivitySeverity`; entiteterna `ProcessEventOutboxEntity`, `ErrandProcessInstanceEntity` (§4.3), `ErrandProcessActivityEntity`; repositories med `Pageable` på **alla** sökfrågor; tabellerna i `truncate.sql`.

**Acceptans:**
- Ingen av entiteterna är mappad som relation på `ErrandEntity`.
- `ErrandProcessInstanceEntity.applyStatus` är enda vägen att sätta status; settern är inte publik.
- Tabelldrivet test räknar upp **varje** `ProcessStatus` mot `isTerminal()`, med `WAITING` explicit verifierad som *icke*-terminal.
- `applyStatus` med icke-terminal status nollar `ended`.
- Aktivitet **utan** processinstans går att spara, och kaskaderas bort när ärendet raderas (`fk_epa_errand`).
- Någon IT startar grönt ⇒ `schema-generation: validate` bekräftar DDL mot entiteter.

### T2 — Konfigurationsläsning (SM)

**Bygg:** `PROPERTY_PROCESS_CONSUMER`, `PROPERTY_PROCESS_TRIGGER`, `getValues(...)` (§7.1); `@ConfigurationProperties` för `process-engine.*`; validering av `PROCESS_CONSUMER` mot konfigurerade konsumenter vid skrivning.

**Acceptans:**
- `getValues` returnerar **alla** rader för en nyckel (regression mot `.findFirst()`).
- Skrivning av okänd `PROCESS_CONSUMER` ger `400`.
- Verifierat att `namespaceConfigCache` evikteras vid skrivning — annars är runtime-redigerbarheten en illusion.

### T3 — Process-instance-API (SM)

**Bygg:** `ProcessInstanceResource` (`PUT`, `POST`, `GET`) och ärendescopad `GET .../process-activities` med valfritt `processInstanceId`-filter (§5.2); `ProcessInstanceService`; API-modellerna (§5.3); `Errand.process` + batchberikning i `readErrand`/`findErrands`; regenerera `openapi.yaml`.

**Acceptans:**
- `PUT` två gånger ⇒ `revision`-tabellen oförändrad (skyddar mot framtida `@OneToMany` på `ErrandEntity`).
- Aktiviteter idempotenta på `(processInstanceId, externalTaskId, activityId)`; batch > 100 ⇒ `400`.
- `GET .../process-activities` returnerar även poster utan processinstans; filtret `processInstanceId` utesluter dem.
- **Kapplöpningen i §5.1 körd i båda ordningarna:** `PUT` först (skapar raden) följt av `POST` med samma `processInstanceId` ⇒ `200` och orört tillstånd; `POST` först följt av workerns `PUT` ⇒ tillståndet uppdateras. Ingen av ordningarna ger `409`.
- Annan levande instans med **annat** `processInstanceId` ⇒ `409`; instans med annat `process_key` ⇒ `409`; constraint-violation översatt enligt §7.4, aldrig `500`.
- `POST` mot ärende som redan har en `COMPLETED` instans ⇒ `409`. `POST` mot ärende som bara har en `FAILED` instans ⇒ `201`.
- `errand.process` projicerar **senaste** instansen: ärende med misslyckad start visar `FAILED` med sitt felmeddelande, inte `null`.
- Batchberikningen är "senaste per ärende" och fortfarande **en** fråga — verifieras med query-räkning.
- `GET .../process-instances` sorterar nyast först.
- `findErrands` gör **en** fråga för berikningen (verifieras med query-räkning, inte ögonmått).
- Beslut fattat och dokumenterat om `process` ska strippas av `limitedMappingPredicateByLabel`.

### T4 — Optimistisk samtidighetskontroll (SM)

**Bygg:** `errandVersion` i rapportmodellen och kontrollen mot `errand.version` i `ProcessInstanceService` (§6.3); WARN-aktivitet och `process_instance.concurrent_task_detected` när två skilda `externalTaskId` rapporterar `RUNNING` mot samma instans utan terminal rapport emellan (§6.4); `process_instance.errand_conflict`.

**Acceptans:**
- Rapport med `errandVersion` som glidit ⇒ `412`, och **varken** tillstånd eller aktiviteter skrivs.
- Rapport utan `errandVersion` ⇒ ingen kontroll, `200`.
- Två skilda `externalTaskId` med `RUNNING` mot samma instans ⇒ WARN-aktivitet skriven, **båda** rapporterna tas emot.
- `ProcessInstanceService` tar en injicerad `Clock`. **Inget test använder `Thread.sleep`.**
- Ett test bekräftar att `PATCH /errands/{id}` med föråldrad `If-Match` ger `412` — regressionsskydd för att hela samtidighetsmodellen vilar på befintligt beteende.

### T5 — Publicering (SM)

**Bygg:** `ProcessEventPublisher` anropad från `EventService.createErrandEvent`, med `setRollbackOnly` före kast (§2.2); `ProcessKeySelector` (§7.3); nödbromsen; `GET /process-labels`.

**Acceptans:**
- **IT som verifierar att ett e-postintag ger en outbox-rad.** Intaget skapar inga revisioner och går förbi chokepointen (§1.1) — tappas det där märks det inte av något annat test.
- Enhetstest per gren i §2.2, inklusive: `executedBy` == konsumenten ⇒ ingen rad; icke-triggad subtyp ⇒ ingen rad; namespace utan konsument ⇒ ingen rad.
- **`executedBy` == en handläggare ⇒ raden skrivs.** Origin-filtret får inte vara bredare än sitt syfte.
- `ProcessKeySelectorTest`: en tagg ⇒ en nyckel; två med samma ⇒ en; två med olika ⇒ ERROR-aktivitet och ingen rad; `deprecated` ignoreras; **namnbyte och omflyttning av labeln lämnar upplösningen oförändrad**.
- **Publisher kastar ⇒ ärendeskrivningen är inte committad**, trots att anropsstället sväljer undantaget (§1.7). Verifieras genom att PATCH:a och sedan läsa tillbaka ärendet — inte genom att inspektera loggen.
- Utan aktiv transaktion: ERROR-logg och `process_event.publish_failed` ökar, inget kast som spräcker anropet.
- Nödbromsen slår över tröskeln med rader som har `delivered_at` satt, och dess ERROR-aktivitet skrivs **utan** instans.
- `ProcessKeySelector` med två skilda nycklar skriver ERROR-aktivitet **utan** instans — testet får inte förutsätta att en instansrad finns.

### T6 — Relay och leverans (SM)

**Bygg:** `service/scheduler/processevent/` (scheduler, worker, relay); AFTER_COMMIT-nudge + bounded executor; `ProcessEngineClient` + felnormaliserande wrapper; `422` permanent vs `5xx` transient; redrive-endpoint och retention (§8.3); mätvärden (§8.1).

**Acceptans:**
- WireMock svarar `202` / `422` / `503` / timeout — samtliga fyra vägar verifierade, inklusive att `422` **inte** retryas och skriver `FAILED` + ERROR-aktivitet.
- Ordning per ärende hålls när flera rader finns.
- Mättad executor ⇒ nudgen släpps och cron levererar; ingen HTTP-tråd blockeras.
- Dead letters raderas inte inom retentionstiden; redrive nollar räknarna.

### T7 — Skyddsräcken (SM)

**Bygg:** `400` på `activePhaseId` när ärendet har processinstans; `400` på etikettändring som byter `processKey` — **både i `ErrandService.updateErrand` och i `AddLabelAction.executeAction`**, som körs schemalagt och aldrig passerar API:t.

**Acceptans:**
- Båda `400`-fallen täckta av enhetstest och ett IT-fall vardera.
- `AddLabelAction` som skulle byta upplöst `processKey` på ett ärende med levande instans ⇒ etiketten läggs inte till, ERROR-aktivitet skrivs. Utan detta slutar processen tyst få väckningar (§11).

### T8 — `ProcessLoopGuardIT` (SM)

**Det viktigaste enskilda testet.** Kör hela varvet: ärende skapas ⇒ outbox-rad; stub agerar pw, rapporterar `RUNNING`, PATCHar ärendet med `X-Sent-By: pw-alkt` och rapporterar `WAITING`.

- **Assertera att pw:s egen PATCH inte gav någon ny outbox-rad** (lager 1).
- Kör sedan **samma** PATCH med en handläggaridentitet och assertera att den **ger** en rad. Filtret får inte vara så brett att äkta ändringar tystas — det felet är osynligt i drift tills någon undrar varför processen aldrig vaknar.
- Kör pw:s PATCH **utan** `X-Sent-By` och verifiera att lager 2 eller 3 fångar den.

Utan detta test är loop-skyddet en hypotes.

### T9 — Beslutsdokument och spårbarhet (SM)

**Bygg:** JSON-schema för `alkt.beslut` registrerat i JsonSchema-tjänsten (§7.5); `EventSubType.DECISION`-event och revision från `ErrandJsonParameterService.updateJsonParameter`; låsning av beslutet mot `COMPLETED` instans; `DECISION` i `PROCESS_TRIGGER` för ALKT.

**Acceptans:**
- Skrivning av `alkt.beslut` ger en eventlogg-post med subtyp `DECISION` **och** en revision. Utan detta publiceras inget processevent och processen vaknar aldrig.
- Skrivning som inte validerar mot schemat ger `400`.
- Andra skrivning av `alkt.beslut` på samma ärende går igenom medan instansen lever, men ger `409` när instansen är `COMPLETED`.
- Skrivningen höjer `errand.version`; en worker med äldre ETag får `412`.
- **IT: handläggaren skriver beslutet ⇒ outbox-rad med subtyp `DECISION`.** Det är hela kedjan som gör att processen kan avslutas.
- Enhetstest som verifierar att beslutsskrivning från namespacets `PROCESS_CONSUMER` **inte** ger en outbox-rad — origin-filtret gäller även här.

### P1 — Operaton-klienten (pw)

`correlateMessage`, `findProcessInstances`, `deleteProcessInstance`; `businessKey` i mappern; nya konstanter. **Acceptans:** `ProcessWithoutDeviationIT` fortsatt grön.

### P2 — Event-endpoint och borttagning (pw)

`POST /process/errand-events` med logiken i §9.3, **och borttagningen i §5.8 i samma steg**. Regenerera `openapi.yaml`.

**Acceptans:** start / korrelera / okänt ärende / DELETE / okänd nyckel (`422`) täckta; inga referenser kvar till `updateAvailable`, `StartProcessResponse` eller `setProcessInstanceVariable`.
- **`UPDATE`-event utan levande instans men med `processKey` ⇒ processen startas.** Det är fallet där etiketten sattes i ett andra anrop (§7.1); startas bara på `CREATE` faller det tyst bort.
- `UPDATE`-event utan `processKey` ⇒ `202`, ingen start.
- **Event mot ärende vars process är `COMPLETED` ⇒ `202`, ingen ny start.** `findProcessInstances` mot Operatons runtime räcker inte som villkor — den ser inte avslutade instanser (§9.3).

### P3 — SM-klienten (pw)

`patchErrand`, `report(...)`, `getErrand`; `RequestInterceptor` för `X-Sent-By`/`X-Request-Group-Id`; uppdatera `support-management.yaml` och regenerera; WireMock-stubbar. `getErrand` måste returnera ärendets `ETag` tillsammans med kroppen, och `patchErrand` skicka den som `If-Match` (§6.2).

**Acceptans:**
- Test som verifierar att `X-Sent-By` sätts på **alla** utgående anrop, inte bara ett.
- `getErrand` följt av `patchErrand` skickar den ETag servern gav; stub som svarar `412` ger ett undantag som når `execute`.

### P4 — Workerstruktur (pw)

`AbstractTaskWorker` enligt §9.4; `ProcessStateReport` med fabriksmetoder; `FailureHandler` rapporterar `RETRYING`/`FAILED`.

**Acceptans:** IT verifierar ordningen `RUNNING` → PATCH → terminal rapport; ett fall där `executeBusinessLogic` kastar asserterar att `RETRYING` rapporterats; ett fall där SM svarar `412` på PATCH asserterar att steget körs om och att andra försöket läser om ärendet.

### P5 — Tillsynsprocessen (pw)

`alkt-tillsyn.bpmn`; justera väntevillkoret i `ProcessWithoutDeviationIT.setup` till 2; egen `ProcessPathway`.

### P6 — Incidentåterkoppling (pw)

Schemalagd kontroll som skriver `FAILED` + `error` till SM när Operaton rest en incident.

---

## 11. Risker och kända begränsningar

| Risk | Hantering |
|---|---|
| **RabbitMQ-mognad** (öppen fråga) | §2.4. Varje REST-konsument byggd innan bytet är kastat arbete |
| **`WAITING` felaktigt behandlad som terminal** | Skulle bryta 1-1-invarianten tyst. Skyddas av `applyStatus` som enda väg + tabelldrivet test (T1) |
| **Handläggarändring mitt i ett arbetssteg** | `412`, workern kör om steget (§6.2). Kostar en omkörning. Mäts av `process_instance.errand_conflict` |
| **Steg med extern sidoeffekt körs om** | Sidoeffekten kan dubbleras. Samma krav som vid kraschad worker (§9.4) — lägg sidoeffekten sist i steget, eller gör den idempotent |
| **Rena läs-workers utan `errandVersion`** | Då finns inget skydd alls (§6.3). Medvetet val per worker, men ett val som måste göras aktivt |
| **Underresurser fångas inte av versionen** | En bilaga som raderas mitt i en körning höjer inte `errand.version`. Processen måste läsa om bilagor när den behöver dem |
| **Parallella grenar** | Modelleringsregel + WARN-aktivitet gör brottet synligt (§6.4) |
| **Loop SM ↔ pw** | Tre lager (§6.5). Lager 1 vilar på en klientsatt header — därför är `process_event.suppressed{reason=ORIGIN}` ett mätvärde som **ska** vara skilt från noll |
| **Beslut skrivet medan processen arbetar** | Korrelationen sväljs och väckningen är borta. Fångas bara av modelleringskravet i §9.2 punkt 1 — wait state:t måste läsa om ärendet vid inträde. Ingen kod i SM kan rädda ett wait state som inte omvärderar |
| **`DECISION` saknas i `PROCESS_TRIGGER`** | Processen vaknar aldrig av beslutet och står i `WAITING` för alltid. Validering av triggervärden och ett IT-fall i T9 |
| **Etikettändring utanför API:t** | `AddLabelAction.executeAction` körs schemalagt och lägger till etiketter utan att passera någon endpoint. Byter den upplöst `processKey` slutar processen tyst få väckningar — T7:s kontroll måste ligga även där |
| **Publiceringsfel sväljs av anropsstället** | `setRollbackOnly` före kast (§2.2) gör svälj-fångsten ofarlig. Kvarstående hål: anropsväg helt utan transaktion — mäts av `process_event.publish_failed` |
| **Start uteblir när etiketten sätts sent** | Start villkoras av `processKey`, inte `eventType` (§9.3). Täckt av ett P2-fall |
| **Felstavat `processKey`** | Upptäcks vid första ärendet. `422` ⇒ ingen retry, `FAILED` + ERROR-aktivitet direkt på ärendet |
| **Dead letter raderas och felet blir permanent** | §8.3 — redrive-endpoint och retention före röjning |
| **Runtime-redigerbar routing utan granskning** | Priset för att slippa release. Validering av `PROCESS_CONSUMER`; överväg ändringslogg |
| **PII i aktivitetsloggen** | `message` är fri text från processen. **pw måste instrueras att inte lägga PII där** — en regel, inte en spärr |
| **Dubbla processinstanser** | ShedLock-serialiserad leverans + businessKey-kontroll + `409` + DB-constraint. Restrisk i Operaton, som saknar unikhet på business key — men SM kan inte registrera resultatet |
| **Delas Operaton-tenanten `ALKT`?** | Påverkar `getDeployments`-assertions och `historyTimeToLive`. Bekräfta mot driftmiljön |

### Vad som inte går att verifiera automatiskt

- Att WSO2 släpper igenom med rätt scope, och att `If-Match`/`ETag` passerar oförvanskade.
- Verklig samtidighet mellan poddar — ShedLock täcks indirekt av `ShedlockConfigurationIT`.
- Långtidsbeteende hos outbox och aktivitetslogg. Kompensation: mätvärdena i §8.1.
