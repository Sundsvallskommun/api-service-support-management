# SupportManagement ↔ pw-alkt — processintegration

Så här ska SupportManagement och den nya tjänsten [pw-alkt](https://github.com/Sundsvallskommun/pw-alkt)
samarbeta när ALKT-namespacets myndighetsprocess körs i Operaton. Dokumentet går igenom datamodellen och
API:erna, hur handläggarens och processens ändringar hålls isär, hur vi hindrar att de två tjänsterna
väcker varandra i all oändlighet — och sist en uppdelning i uppgifter som går att bygga en i taget.

**Jira:** DRAKEN-4733 (story) med DRAKEN-4734…4750 som deluppgifter. Avsnittsnumren nedan refereras från
respektive deluppgift, och kopplingen mellan uppgifterna i §10 och Jira-nycklarna står i tabellen där.
T12 — automatisk och manuell start — ligger på DRAKEN-4811.

**Öppen fråga:** när RabbitMQ blir produktionsklar (§2.4). Den blockerar inte T1–T8.

---

## 0. Beslutslogg

| #  |                                                             Beslut                                                              |                                                                 Valdes bort                                                                 |                                                                                                                                                                                                     Skäl                                                                                                                                                                                                     |
|----|---------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1  | Outbox + REST-anrop via WSO2, med direktkörning av relayet så fort ärendet sparats                                              | Direkt mot RabbitMQ; SSE; att processen frågar efter ändringar                                                                              | Outboxen behövs ändå, annars kan ärendet sparas utan att händelsen skickas. **AMQP är målbilden**                                                                                                                                                                                                                                                                                                            |
| 2  | Publiceringen hängs in i `EventService.createErrandEvent`                                                                       | Att hänga in den i `ErrandService` och jämföra revisioner                                                                                   | Intaget skapar inga revisioner, så processen hade varit blind för kompletteringar                                                                                                                                                                                                                                                                                                                            |
| 3  | Filtrera på händelsens typ och subtyp                                                                                           | Jämföra JSON-fält mellan versioner                                                                                                          | Fungerar oavsett hur ändringen kom in, och mekanismen finns redan för notisprenumeranter                                                                                                                                                                                                                                                                                                                     |
| 4  | Konfiguration i befintlig `namespace_config`                                                                                    | Ny tabell; `application.yml`                                                                                                                | Nycklarna driver redan beteende. Flervärd per nyckel. Cache + CRUD finns                                                                                                                                                                                                                                                                                                                                     |
| 5  | Etikett → process via `metadata_label_attribute[processKey]`, utan trädtraversering                                             | Matcha på etikettens sökväg; ärva nyckeln nedåt i trädet                                                                                    | Med arv nedåt kan en process börja gälla för ett ärende bara för att någon flyttat om i metadatan                                                                                                                                                                                                                                                                                                            |
| 6  | Ett ärende hör till en processtyp                                                                                               | Flera processtyper på samma ärende                                                                                                          | En tillsyn är ett nytt ärende. Då kan databasen hålla regeln i stället för att vi ska komma ihåg den                                                                                                                                                                                                                                                                                                         |
| 7  | **Optimistisk kontroll med `errand.version` och `If-Match`** — krocken upptäcks när någon skriver                               | Pessimistiskt lås på ärendet med utgångstid, spärrar i skrivvägarna och `423`                                                               | Målet är att ändringar inte ska tappas, inte att handläggaren ska hindras. Och maskineriet finns redan i drift — §6.2                                                                                                                                                                                                                                                                                        |
| 8  | **En** skrivning bär både tillstånd och aktiviteter                                                                             | Var sin endpoint                                                                                                                            | Färre anrop, en transaktion och en rapport per arbetssteg                                                                                                                                                                                                                                                                                                                                                    |
| 9  | Inga processvariabler för att hålla reda på vad som redan gjorts                                                                | `updateAvailable`, versionsräknare                                                                                                          | Läs–ändra–skriv med inbyggd kapplöpning. Skyddet ligger i strukturen i stället                                                                                                                                                                                                                                                                                                                               |
| 10 | `errand.status` frikopplat från processens faser                                                                                | Process äger status                                                                                                                         | Skilda begrepp. Listvyn läser processläget ur `errand.process`                                                                                                                                                                                                                                                                                                                                               |
| 11 | pw-alkt:s `start`- och `update`-endpoints tas bort                                                                              | Att låta dem ligga kvar som utfasade                                                                                                        | Ingen använder dem, och med två sätt att starta en process kommer någon förr eller senare att välja fel                                                                                                                                                                                                                                                                                                      |
| 12 | **`ProcessStatus.isTerminal()` som metod på enumet**                                                                            | En separat lista över vilka statusar som räknas som avslutade                                                                               | Den styr `active_marker` och därmed regeln om en process per ärende. `WAITING` är fällan — se §4.1                                                                                                                                                                                                                                                                                                           |
| 13 | **Fem statusvärden**, där `START_FAILED` blir `FAILED` med en felkod                                                            | Åtta värden                                                                                                                                 | Skillnaden syns redan på att `processInstanceId` saknas                                                                                                                                                                                                                                                                                                                                                      |
| 14 | **Modelleringsregel: inga parallella grenar som ändrar ärendet**                                                                | Ingen regel alls                                                                                                                            | Två grenar som skriver till samma ärende slår ut varandra med `412` och kommer aldrig i mål — §6.4                                                                                                                                                                                                                                                                                                           |
| 15 | **Loop-skyddets lager 1 styrs av headern `X-Trigger-Process`, inte av vem som anropar.** Headern hedras inte för AD-identiteter | Att jämföra `X-Sent-By` mot namespacets `PROCESS_CONSUMER`                                                                                  | Avsikten *"den här skrivningen ska inte väcka processen"* är det vi vill uttrycka; identiteten var bara en gissning på den. SM slipper känna igen varje ny pw-tjänst, och en namnmiss slutar tyst loopa — §6.5                                                                                                                                                                                               |
| 16 | **Ett publiceringsfel märker transaktionen som `rollback-only`**                                                                | Lita på att anroparen för felet vidare                                                                                                      | Alla anropsställen sväljer undantag från `createErrandEvent` (§1.7). Utan det här är outboxen inte transaktionell                                                                                                                                                                                                                                                                                            |
| 17 | **Start styrs av `processKey`, inte av `eventType`**                                                                            | Start endast på `CREATE`                                                                                                                    | Ärendet som får sin etikett i ett andra anrop hade annars aldrig startat — §7.1 motiverar triggern, §9.3 utför den                                                                                                                                                                                                                                                                                           |
| 18 | Aktiviteter får sakna processinstans: `errand_process_id` är nullbar och läsningen sker per ärende                              | `not null`                                                                                                                                  | Tvetydiga etiketter och nödbromsen slår till innan någon instans finns. Posten hade helt enkelt inte gått att spara                                                                                                                                                                                                                                                                                          |
| 19 | Arbetssteg som bara läser skickar med `errandVersion`; SM svarar `412` om ärendet hunnit ändras                                 | Inget skydd alls för läsande steg                                                                                                           | Ett steg som aldrig skriver har annars ingenting att krocka på — §6.3                                                                                                                                                                                                                                                                                                                                        |
| 20 | **`POST .../processes` skapar, men uppdaterar aldrig**                                                                          | `409` så snart det finns en rad                                                                                                             | Ett arbetssteg kan hinna rapportera före pw:s `POST`. När `POST` bara skapar spelar ankomstordningen ingen roll, och `409` betyder bara en enda sak — §5.1                                                                                                                                                                                                                                                   |
| 21 | **Ett avslutat processliv startas aldrig om.** `FAILED` får startas om                                                          | Starta på nästa triggande händelse oavsett historik                                                                                         | `findProcessInstances` ser bara det som kör i Operaton just nu, så en `COMPLETED` process ser ut som ingen process alls. Nästa process är ett nytt ärende — §7.4                                                                                                                                                                                                                                             |
| 22 | `errand.process` visar den **senaste** instansen                                                                                | Den som lever just nu                                                                                                                       | En misslyckad start lämnar ingen levande instans efter sig, och då hade handläggaren inte sett någonting alls — §5.3                                                                                                                                                                                                                                                                                         |
| 23 | **Beslutet är en egen ärendescopad resurs, `errand_decision`, med fasta fält**                                                  | `json_parameter` med registrerat schema; kolumner på `errand_process`; vanliga parametrar; aktivitetsloggen                                 | Ett ärende, ett beslut — och `uq_ed_errand_id` *är* den regeln. Ett myndighetsbeslut har dessutom en form som följer av förvaltningslagen och förtjänar riktiga fält, och det är ärendedata som ska gå att läsa utan att man känner till processen. Se §7.5                                                                                                                                                  |
| 24 | **Både handläggare och process får fatta beslutet; `method` skiljer dem åt**                                                    | Bara handläggaren; bara processen                                                                                                           | Ett delegationsbeslut kan vara automatiserat, men vilket det var måste gå att svara på i efterhand (FL 28 §). Följden: `DECISION` måste vara `PROCESS_TRIGGER`, och `method` valideras mot identiteten — §7.5                                                                                                                                                                                                |
| 25 | `processKey` hämtas från instansen först och från etiketterna i andra hand; `DELETE` skickas även utan nyckel                   | Att alltid läsa nyckeln ur etiketterna                                                                                                      | En borttagen etikett skulle annars lämna en processinstans kvar i Operaton för ett ärende som inte längre finns — §2.2                                                                                                                                                                                                                                                                                       |
| 26 | **Resursen heter `processes` och modellen `ErrandProcess`**                                                                     | `process-instances`; `process-info`                                                                                                         | Modellen ska kunna bära även processer som inte körs i Operaton, och kodbasens övriga subresurser heter något i plural. `process-info` går inte att böja i plural och hade dessutom låst oss vid en rad per ärende                                                                                                                                                                                           |
| 27 | **`GET /process-labels` byggs inte**                                                                                            | En egen endpoint som visar vilken etikett som startar vilken process                                                                        | `GET /metadata/labels` lämnar redan tillbaka `attributes` med `id` och `resourcePath`. Den fråga man faktiskt ställer i drift gäller dessutom ett enskilt ärende och besvaras av aktivitetsloggen — §5.2. Indexet `idx_metadata_label_attribute_key` behövs därmed inte heller                                                                                                                               |
| 28 | **Manuell stegning sker med namngivna signaler, och valet manuellt eller automatiskt ligger i processmodellen**                 | En inställning per namespace; att handläggaren sätter processens läge direkt                                                                | En inställning i SM kan säga en sak medan modellen gör en annan. Signalen är dessutom en begäran, inte ett kommando — processen avgör, så lagstadgade steg går inte att kliva förbi (§5.9)                                                                                                                                                                                                                   |
| 29 | **Signalen bär bara ett namn, ingen fritext**                                                                                   | Ett kommentarsfält på signalen                                                                                                              | Aktivitetsloggen gallras efter 365 dagar medan ärendet lever längre, och `message` får inte innehålla personuppgifter. Motiveringen hör hemma i ärendeanteckningar (§5.9)                                                                                                                                                                                                                                    |
| 30 | **Ingen retry-räknare och ingen dead letter. Raden ligger kvar tills den gått igenom**                                          | Egen backoff med `retry_count`/`next_retry_at`/`dead_letter`, som `notification_dispatch` hade före `V1_48__simplify_notification_dispatch` | Leverans och radering i samma transaktion ger samma sak utan bokföring, och den bokföringen har kodbasen medvetet gjort sig av med. Kvar blir `delivered_at`, som nödbromsen behöver — §8.3                                                                                                                                                                                                                  |
| 31 | **Outbox-raden bär sitt eget mål i `process_service`, satt vid publicering**                                                    | Att relayet slår upp `PROCESS_CONSUMER` på nytt vid leverans                                                                                | Ett namespace har exakt en processkonsument, men konfigurationen kan ändras mellan publicering och leverans. Raden ska gå dit den var adresserad. Kolumnen är dessutom det relayet grupperar på för att en långsam konsument inte ska svälta de andra — §7.6                                                                                                                                                 |
| 32 | **`process` och `decision` är `ErrandField`-värden**                                                                            | Att låta dem stå utanför den rollbaserade fältfiltreringen                                                                                  | `justification` är fritext med personuppgifter, och alla andra känsliga fält på ärendet går genom `roleBasedFieldResolver`. Att ALKT kör utan åtkomstkontroll döljer bara problemet till nästa namespace — §5.3                                                                                                                                                                                              |
| 33 | **AoT använder inte AccessMapper, och ett namespace med `PROCESS_CONSUMER` får inte ha aktiv `access_control`**                 | Att lita på att ingen slår på den; att låta `AccessControlService` gå förbi kontrollen för konsumenten utpekad med `X-Sent-By`              | AccessMapper svarar bara på AD-konton, och pw är ingen människa. Slås kontrollen på får pw `401` på allt, och det syns som ärenden som står stilla. En header som anroparen sätter själv duger inte som behörighetsgrund — §7.1                                                                                                                                                                              |
| 34 | **Tre nya `ProtectedResource`: `PROCESS`, `PROCESS_ACTIVITY`, `DECISION`**                                                      | Att återanvända `ERRAND`                                                                                                                    | `getErrand` och `verifyExistingErrandAndAuthorization` kräver en resurs, så valet går inte att skjuta upp. `ERRAND` hade gett processens rapporter samma behörighet som ärendet självt — §5.6                                                                                                                                                                                                                |
| 35 | **Startläget bor på etiketten: `processStartMode` bredvid `processKey`**                                                        | En inställning per namespace; en manuell grind först i processmodellen                                                                      | Ansökan och tillsyn ligger i samma namespace och vill ha olika svar. En grind i modellen hade dessutom gett varje ärende en levande instans, och att avbryta vid grinden avslutar processlivet enligt §7.4 regel 4 — ärendet hade aldrig gått att starta igen. Avgränsat mot beslut 28: läget styr instansens **födelse**, modellen styr stegningen — §7.7                                                   |
| 36 | **SM räknar ut startlovet och skickar det med händelsen som `startAllowed`**                                                    | Att pw avgör själv och frågar SM om ärendet har en avslutad process                                                                         | Manuell start går annars inte att uttrycka: den skiljer sig från en vanlig ärendeändring bara genom att den får starta. På köpet försvinner pw:s återanrop till SM för `COMPLETED`-kontrollen — lovet är redan uträknat när händelsen kommer fram — §7.7, §9.3                                                                                                                                               |
| 37 | **Kommandon filtreras inte av `PROCESS_TRIGGER` och kräver AD-identitet**                                                       | Ett `PROCESS`-värde i triggern; att släppa in maskinidentiteter och i stället undanta kommandon från loop-skyddets lager 1                  | Ett kommando är ingen ärendeändring, och en människa som trycker på en knapp är ingen loop. Kommandon passerar därför **alla tre** lagren: AD-kravet gör lager 1 verkningslöst av sig självt, medan lager 2 och 3 undantar dem uttryckligen. Utan undantaget för nödbromsen sväljs startkommandot tyst på just de ärenden som har mest trafik. `SIGNAL` utgår därmed ur `PROCESS_TRIGGER` — §6.5, §7.1, §7.7 |
| 38 | **`GET .../processes` svarar med ett kuvert: `startable` + `processes`**                                                        | En naken lista; ett fält på ärendeprojektionen                                                                                              | Det intressanta fallet är när listan är tom, och en tom lista kan inte bära *varför*. Ärendeprojektionen är tjänstens varmaste läsväg och hade dragit med sig en uppslagning per ärende i listsvar — §5.10                                                                                                                                                                                                   |

---

## 1. Vad vi vet om koden i dag

Allt i det här avsnittet är efterkontrollerat i kodbasen, inte antaget. Resten av dokumentet vilar på det.

### 1.1 `EventService` är vägen allt går igenom

`EventService.createErrandEvent` anropas från 12 ställen:

|           Väg           |                                                        Anropsställe                                                         |
|-------------------------|-----------------------------------------------------------------------------------------------------------------------------|
| PATCH / create / delete | `ErrandService.createErrand/updateErrand/deleteErrand`                                                                      |
| Bilagor                 | `ErrandAttachmentService.createErrandAttachmentInternal/deleteErrandAttachment/createErrandAttachment(AttachmentEntity, …)` |
| Handover                | `HandoverService.logHandoverEvents`                                                                                         |
| Konversationer          | `MessageExchangeSyncService.syncConversation`                                                                               |
| E-postintag             | `EmailReaderWorker.processErrand`                                                                                           |
| Webbmeddelanden         | `WebMessageCollectorWorker.saveMessage`                                                                                     |
| Suspension              | `SuspensionWorker.processExpiredSuspensions`                                                                                |

**Revisioner skapas bara på fem ställen** — `ErrandService.createErrand/updateErrand`, `ErrandAttachmentService.createErrandAttachmentInternal/deleteErrandAttachment/createErrandAttachment(AttachmentEntity, …)`. Intaget (`EmailReaderWorker.processErrand`, `MessageExchangeSyncService.applyStatusChange`) sparar direkt via repository. Därför kan trigger-filtret inte bygga på revisionsdiff.

`EventSubType` (befintlig enum): `ATTACHMENT, DECISION, ERRAND, HANDOVER_IN, HANDOVER_OUT, MESSAGE, NOTE, SYSTEM, SUSPENSION`. `EventType` kommer från eventlog-specen. Enumet utökas med `SIGNAL` för manuell stegning (§5.9) och `PROCESS` för manuell start (§5.10) — rena tillägg som inte rör befintliga värden.

### 1.2 Halva leveransmaskineriet finns redan

`EventService.saveDispatchEntry` (outbox-rad), `NotificationDispatchScheduler.processDispatch` (ShedLock, hälsoindikator, gruppering per ärende), `NotificationDispatchWorker.processGroup` (leverans och radering i **en** transaktion), `.wantsEvent`/`.matches` (filter på typ och subtyp), `.isExecutingUser` (hoppa över upphovet), `NotificationDispatchRepository.findProcessable` (transaktionsfönstret, konfigurerat med `scheduler.notification-dispatch.transaction-buffer`, `PT10S`).

**`V1_48__simplify_notification_dispatch` förenklade mönstret, och det är den formen vi tar efter.** Retry-räknare, backoff och dead letter är borta ur `notification_dispatch`: en misslyckad leverans rullas tillbaka i sin helhet och raden ligger kvar tills den lyckas. Det som stoppar en rad som aldrig kan gå igenom är i stället `max-age` — när raderna åldrats ur filtreras de bort ur leveransen, gruppen blir tom, ingenting kan kasta, och de raderas oskickade. Migreringen säger det rakt ut: *retry bookkeeping and dead-lettering obsolete*. §8.3 beskriver vad det betyder för processhändelser.

Men ta inte med allt rakt av: `NotificationDispatchRepository.findProcessable` saknar `LIMIT`.

### 1.3 Var ärendeskrivningar passerar

Allt går via `AccessControlService`, och **skrivvägar skickar `RW` ensamt medan läsvägar skickar `R, RW`**. Det finns två ingångar dit. Eftersom vi valt optimistisk samtidighetskontroll (§6.2) finns det inget lås som måste sättas på båda ställena, men kartan är ändå värd att ha — den visar vilka vägar som faktiskt ändrar ärendet:

`getErrand(...)` — hämtar entiteten och filtrerar:
`ErrandService.updateErrand/deleteErrand`, `ErrandParameterService.updateErrandParameters/updateErrandParameter/deleteErrandParameter`, `ErrandJsonParameterService.updateJsonParameter/deleteJsonParameter`, `ErrandAttachmentService.createErrandAttachment/deleteErrandAttachment`, `CommunicationService.sendEmail/sendSms/sendWebMessage`, `ConversationService.markAsRead`, `ErrandNoteService.createErrandNote/updateErrandNote/deleteErrandNote`, `NotificationService.createNotification`.

`verifyExistingErrandAndAuthorization(...)` — kontrollerar **utan** att hämta entiteten:
`CommunicationService.updateViewedStatus`, `ConversationService.createConversation/updateConversationById/createMessage`, `NotificationService.globalAcknowledgeNotificationsByErrandId/updateNotification/deleteNotification`.

Ett undantag finns: **`HandoverService.handover`** hämtar utan filter men ändrar ändå källärendet i `handleSourceErrand`. De schemalagda jobben går förbi hela vägen och skriver rakt på repositoryt (`EmailReaderWorker.processErrand`, `MessageExchangeSyncService.applyStatusChange`), och därför höjer de inte heller `errand.version` (§6.2).

### 1.4 En revision är en kopia av hela ärendet

`RevisionMapper.toSerializedSnapshot` serialiserar hela `ErrandEntity` med Gson, och `CircularReferenceExclusionStrategy` plockar bara bort bakåtreferenser. Allt som hängs på entiteten hamnar alltså i varje revision. Därav två hårda regler: **inga nya kolumner på `errand` och inga nya `@OneToMany` på `ErrandEntity`** — annars växer varje snapshot, och eftersom `ErrandService.updateErrand` kör `OPTIMISTIC_FORCE_INCREMENT` hade en ny kolumn dessutom gjort varje utestående ETag ogiltig.

### 1.5 `namespace_config` driver redan beteende

`ConfigPropertyExtractor` har `PROPERTY_ACCESS_CONTROL` och `PROPERTY_NOTIFY_REPORTER` — beteendeflaggor. `namespace_config_value` (`V1_19`): `key`, `value text`, `type` (`BOOLEAN|STRING|INTEGER`), unikt på **`(namespace_config_id, key, value)`** ⇒ flervärd per nyckel. `@ElementCollection(EAGER)`, cachad via `namespaceConfigCache`.

**Fallgrop:** `ConfigPropertyExtractor.getNullableValue` tar `.findFirst()` och kastar tyst bort resten av värdena.

### 1.6 Övrigt

- `metadata_label_attribute` (`V1_37`): fri key/value, unik på `(metadata_label_id, key)`. Nycklar **inte** whitelistade (`ValidLabelAttributesConstraintValidator.hasUniqueAttributeKeys`).
- `errand.id` är `varchar(255)` (`V1_0`).
- pw-alkt är stateless. `AbstractTaskWorker.clearUpdateAvailable` varnar för races vid skrivning av processvariabler. `alkt-ansokan.bpmn` innehåller **inget** `updateAvailable`; `clearUpdateAvailable` har **inga anropare**.
- Varje PW-tjänst har eget API i WSO2 ⇒ en OAuth2-registrering per PW-tjänst.
- **SM har Testcontainers.** `application-it.yml` (`spring.datasource`): `driver-class-name: org.testcontainers.jdbc.ContainerDatabaseDriver`, `url: jdbc:tc:mariadb:10.6:///ittest`. Uppsättningen syns inte i `src/integration-test/java` eftersom den sker via JDBC-URL, inte via en `@Container`-deklaration. `schema-generation: validate` är påslaget. MariaDB **10.6** ⇒ `SKIP LOCKED` är tillgängligt.
- `json_parameter` (`V1_43`–`V1_45`): `errand_id`, `parameter_key`, `schema_id`, `value longtext`, egen `version`. Unik på `(errand_id, parameter_key)`, `@OneToMany` på `ErrandEntity`, validerad mot JSON-schema via `ValidJsonParameterConstraintValidator` och `JsonSchemaClient`. Vanliga parametrar duger inte till fritext: `parameter_values.value` är `varchar(255)`.
- **Varken `ErrandJsonParameterService` eller `ErrandParameterService` skapar revision eller event.** En parameterskrivning höjer `errand.version` och gör i övrigt ingenting. Det är därför beslutet inte lagras som parameter (§7.5) — den vägen hade krävt att en delad, redan använd tjänst börjar skapa event och revision. `EventSubType.DECISION` finns i enumet men används ingenstans i kodbasen; §7.5 tar den i bruk.
- `ErrandService.createErrand` tar `referredFrom` och skapar en relation via `RelationClient` — den befintliga vägen att koppla ihop två ärenden.
- `truncate.sql` måste utökas med varje ny tabell.

### 1.7 Alla som anropar `createErrandEvent` sväljer undantag

`ErrandService.createErrand/updateErrand/deleteErrand`, `ErrandAttachmentService.createErrandAttachmentInternal/deleteErrandAttachment/createErrandAttachment(AttachmentEntity, …)`,
`EmailReaderWorker.processErrand` och `MessageExchangeSyncService.syncConversation` har alla `try { … } catch (final Exception e) { LOG.warn(…) }` runt anropet.

Metoden har redan ett eget `try/catch` runt anropet till eventloggen, så de yttre fångsterna fyller
egentligen ingen funktion. Men de finns, och de fångar `Exception`. Hänger vi in publiceringen sist i
`createErrandEvent` och låter den kasta vid fel, blir resultatet en varningsrad i loggen medan
ärendeändringen sparas som vanligt. Då är vi tillbaka i precis det problem outboxen skulle lösa: ändringen
finns i SM men processen får aldrig veta om den. Motmedlet står i §2.2.

### 1.8 Identiteten i `X-Sent-By`

Identiteten avgör **inte** om en outbox-rad skrivs — det gör `X-Trigger-Process` (§6.5). Men den styr
fortfarande om ett beslut får stämplas `AUTOMATIC` (§7.5), vem aktivitetsloggen pekar ut och vem som står
som avsändare på notisen. Därför är det värt att veta precis hur den läses. Kontrollerat mot
dept44-starter 8.0.8 (`se.sundsvall.dept44.support.Identifier`):

- **En okänd typ i headern avvisas inte.** `Identifier.parse` känner igen `partyId` och `adAccount`, och
  gör om allt annat till `CUSTOM` med typsträngen kvar. `X-Sent-By: pw-alkt; type=processEngine` ger
  alltså värdet `pw-alkt`.
- **Däremot måste headern ha exakt två delar** — värdet och `type=`, åtskilda med semikolon. Är den
  felskriven lämnar `parse` tillbaka `null` och identiteten är borta. Loop-skyddet tar ingen skada av det,
  eftersom det inte läser identiteten — men ett `AUTOMATIC`-beslut avvisas med `403` och aktivitetsposten
  står utan avsändare. Det är därför T8 provar just det fallet.
- `ServiceUtil.getExecutingUser()` ger hela identiteten. `getAdUser()` ger `null` för allt som inte är ett
  AD-konto.

Det sista märks på ett ställe: `EventService.createNotification` hämtar avsändaren med `getAdUser()`.
När pw skriver blir den `null` och notisen till handläggaren står utan avsändare. Inget går sönder —
`NotificationService` kollar med `hasText` först — men fältet blir tomt, och det är lätt gjort att låta det
falla tillbaka på identitetens värde i stället (T3).

**ALKT använder inte AccessMapper.** Det är ett beslut för hela AoT-området, och det är också en
förutsättning för att pw ska komma åt ärendena över huvud taget: `AccessMapperService.getAccessibleLabels`,
`.getAccessibleRoles` och `.getAccessibleResources` filtrerar alla på `Identifier.Type.AD_ACCOUNT` och
lämnar tillbaka tomt för allt annat. pw är ingen människa och har inget AD-konto. Slås åtkomstkontrollen på
för namespacet får pw därför `401` på `getErrand`, `patchErrand`, processrapporten och beslutsskrivningen —
och det syns inte som ett fel i processen, utan som ett ärende som står stilla. Motmedlet är spärren
i §7.1.

---

## 2. Arkitektur

### 2.1 Flödet

Kortversionen: när något händer med ett ärende skriver SM ner en rad om det i en egen tabell — outboxen —
i samma transaktion som ändringen. Ett bakgrundsjobb plockar sedan raden och skickar den vidare till
pw-alkt, som antingen startar processen eller väcker den. När processen sedan gör något av
betydelse rapporterar den tillbaka till SM.

```
Handläggare/intag -> SM -> EventService -> ProcessEventPublisher -> process_event_outbox
                                                                          |
                              direktkörning efter commit / cronjobb -> ProcessEventRelay
                                                                          |
                                                                    POST errand-events (WSO2)
                                                                          |
                                                                       pw-alkt
                                                                     /          \
                                                        start process        correlate message
                                                                     \          /
                                                                       Operaton
                                                                          |
                                                                   arbetssteg hamtas -> pw kor det
                                                                          |
                                                    PUT processes (tillstånd + aktiviteter)
                                                                          |
                                                                         SM
```

### 2.2 När en händelse blir en outbox-rad

`ProcessEventPublisher.publish(errandEntity, eventType, eventSubType, executedBy, requestGroupId)` anropas sist i `EventService.createErrandEvent` och kör i samma transaktion. `sendNotification`-flaggan spelar ingen roll här — det här är inga notiser till handläggare, utan meddelanden till en process.

```
1. PROCESS_CONSUMER för (municipalityId, namespace)?      nej -> return
2. X-Trigger-Process: false, icke-AD-identitet?           ja  -> return        (loop-skydd, lager 1)
                       kommandon (PROCESS, SIGNAL) hoppar over steg 3 och 4, 6.5
3. Levererade event för ärendet i fönstret > tröskel?      ja  -> ERROR-aktivitet, return  (lager 3)
4. eventSubType i PROCESS_TRIGGER?                        nej -> return        (lager 2)
5. processKey: kommandots egen forst, sedan instansens, sist ur etiketterna
                       0 -> DELETE publiceras anda, ovriga return; >1 -> ERROR-aktivitet, return
6. startAllowed: kommando -> ja; annars ingen levande instans && ingen COMPLETED
                 && etikettens processStartMode == AUTOMATIC                    (7.7)
7. INSERT process_event_outbox (process_service = namespacets PROCESS_CONSUMER, 7.6)
```

Steg 1 är en uppslagning i en cachad map. Ett namespace utan process betalar alltså ingenting mer än så.

ERROR-aktiviteterna i steg 3 och 5 skrivs **utan processinstans** — de inträffar per definition när
ingen instans finns (§3.1). Båda skrivs dessutom **en gång per ärende och fönster**, inte en gång per
kastad händelse. Nödbromsens post har alltid haft det kravet (§6.5); tvetydighetsposten behöver det nu när
tvetydiga etiketter är ett läge ett ärende kan ligga och vänta i tills någon startar processen för hand
(§5.10) — annars lägger varje meddelande och varje bilaga en ny ERROR-rad i loggen, och felet dränker den
logg det rapporteras i. `uq_epa_idempotency` räddar oss inte: både `errand_process_id` och
`external_task_id` är NULL för de här posterna, och NULL är distinkt i unika index.

**Steg 6 är hela skillnaden mellan en händelse som väcker en process och en som startar den.** SM räknar ut
lovet en gång, vid publiceringen, och skickar med svaret. pw behöver därmed varken känna till etiketternas
startläge eller fråga tillbaka om ärendets processhistorik. Regeln och skälen till den står i §7.7.

**Steg 5 läser instansens `process_key` först, etiketterna bara i andra hand.** Så snart ärendet har en
processinstans är nyckeln fastnaglad, och en etikett som ändras eller tas bort kan inte längre ändra vad
som publiceras. Det spelar särskilt roll för `DELETE`: löstes nyckeln alltid ur etiketterna skulle ett
ärende vars etikett hunnit tas bort inte ge någon rad alls, och processinstansen leva vidare i Operaton
för ett ärende som inte finns (§9.3).

**Ett startkommando bär sin egen nyckel, och den går före allt annat.** Handläggaren har redan valt (§5.10),
och det valet får inte göras om vid publiceringen. Utan den regeln skulle upplösningen ur etiketterna hitta
två nycklar, falla ut i `>1 -> ERROR-aktivitet, return`, och kommandot vore verkningslöst i precis det fall
det finns till för — att låta en människa lösa upp en tvetydighet.

Därför publiceras `DELETE` **även utan nyckel**. pw matchar på `businessKey`, inte på `processKey`, när det
ska radera (§9.3), så fältet är informationsbärande för `DELETE` och obligatoriskt bara för `CREATE` och
`UPDATE`. Det gör också publiceringen okänslig för att instansraden redan kaskaderats bort när
`ErrandService.deleteErrand` anropar `createErrandEvent` efter `repository.deleteById`.

**Publiceringen får inte kunna svälja sitt eget fel.** Alla åtta anropsställen fångar `Exception` (§1.7),
och en överenskommelse om att inte lägga till ett nionde är ingen garanti. Därför:

```java
// ProcessEventPublisher, vid varje fel som inte är ett medvetet "return" enligt regeln ovan
if (TransactionSynchronizationManager.isActualTransactionActive()) {
    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
}
throw ...;   // anroparen far svalja detta; transaktionen gar anda inte att spara
```

Poängen är att en utebliven outbox-rad drar med sig hela ärendeändringen
(`UnexpectedRollbackException` när transaktionen ska sparas), oavsett vad anropsstället gör med
undantaget. Namespace utan `PROCESS_CONSUMER` vänder redan i steg 1 och märker aldrig något av det här.

Finns det ingen transaktion alls går det förstås inte att rulla tillbaka någonting — då är ärendet redan
sparat. Det loggas som ERROR och räknas i `process_event.publish_failed` (§8.1). I dag har varje väg in en
transaktion (`ErrandService`, `EmailReaderWorker.processEmail`, `MessageExchangeWorker.processConversation`),
så det ska aldrig hända.

Att i stället plocka bort de yttre fångsterna löser inte problemet ensamt, och skulle dessutom göra ett
misslyckat notisutskick dödligt för ärendeskrivningen. Vill man städa där är det ett eget arbete.

### 2.3 Hur snabbt det går — direktkörning och cronjobb

Relayet startas på två sätt, och det är värt att hålla isär dem.

> **Direktkörning** är en tom signal — den bär ingen data — som startar relayet så fort ärendets
> transaktion har sparats, i stället för att vänta på nästa cron-tick. Den *levererar ingenting själv*:
> den startar samma jobb som cronjobbet startar. Och den är frivillig i den meningen att den får tappas
> — då hämtar cronjobbet raden i stället, inom en minut.

Tre saker är lätta att blanda ihop:

|             |                  Direktkörning                   |             Cronjobb              |
|-------------|--------------------------------------------------|-----------------------------------|
| Startas av  | att ärendet just sparats                         | klockan, `0 * * * * *`            |
| Syfte       | latens: sekunder i stället för upp till en minut | att ingenting blir liggande       |
| Får utebli? | Ja, utan att något går förlorat                  | Nej — det är sanningen i systemet |

Direktkörningen **väcker relayet, inte processen**. Att väcka processen är något helt annat och sker längre
fram i kedjan, när pw korrelerar ett meddelande in i Operaton (§9.3).

Mekaniken: `@TransactionalEventListener(AFTER_COMMIT, fallbackExecution = true)` tillsammans med
`@Async("processEventExecutor")` (2 trådar normalt, 4 som mest, kö på 500). Samma mönster används redan i
`SubscriptionService.handleAutoSubscribeEvent`.

**Blir kön full ska direktkörningen hoppas över, inte anropet fällas.** Standardbeteendet `AbortPolicy`
kastar ett `RejectedExecutionException` i den tråd som just sparat ärendet, och det når hela vägen ut till
anroparen — en full kö hade alltså gett `500` på en ärendeskrivning som faktiskt gick bra. Kasta i stället
bort signalen, räkna upp `process_event.direct_run_rejected` och lägg try/catch runt lyssnaren, precis som
`handleAutoSubscribeEvent` redan gör. Cronjobbet är skyddsnätet: en missad direktkörning kostar upp till en
minut, aldrig ett fel.

Cronjobbet går genom samma kod och tar bara med rader som är minst fem sekunder gamla, så att det inte
krockar med en transaktion som håller på att sparas. Krockar de ändå — direktkörning och cronjobb på samma
rad — är det ofarligt: båda tar radgruppen med `@Lock(PESSIMISTIC_WRITE)` sorterad på `created, id` och
plockar bara rader utan `delivered_at`, så den som kommer sist hittar ingenting att göra. Samma lås är det
som håller ordningen inom ett ärende.

### 2.4 Hur händelserna tar sig över till pw

**Vi börjar med REST.** Outboxen behövs oavsett vilken transport vi väljer, och meddelandekön är ännu inte
bevisat driftklar. Det är det enda som saknas — testmässigt är det ingen tröskel alls, SM har redan
Testcontainers (§1.6) och en `RabbitMQContainer` är några rader kod.

**Men målbilden är AMQP.** Med REST kräver varje ny PW-tjänst en OAuth2-registrering, en url och ett
Feign-mål i SM — vad det innebär i praktiken står i §7.6. Själva bytet är däremot litet: allt utbyte sker
i en enda metod, `ProcessEventDelivery.deliver(row, client)`. Varje REST-konsument vi bygger innan bytet är arbete vi slänger.

Med *driftklar* menar vi: quorum queues på minst tre noder, DLX/DLQ med `x-delivery-limit`, egen vhost per
miljö, en användare per tjänst med rättighetsregler, TLS, övervakning av kölängd, obekräftade meddelanden,
DLQ-djup och nodstatus — samt en dokumenterad väg tillbaka när något gått fel.

---

## 3. Datamodell

### 3.1 Tabellerna

Fem nya tabeller i tre migreringar: `V1_53__add_process_integration_tables.sql` med de tre första (byggs i
T1), `V1_54__add_errand_decision.sql` med beslutet (T9) och `V1_55__add_errand_process_signal.sql` med de
väntade signalerna (T11). De ligger i var sin fil eftersom Flyway jämför checksumma — en migrering som
redan körts går inte att fylla på i efterhand.

```sql
-- 1. Outbox. Medvetet UTAN FK mot errand: ett DELETE-event maste overleva att arendet raderas.
create table if not exists process_event_outbox (
    id                varchar(36)  not null,
    municipality_id   varchar(8)   not null,
    namespace         varchar(32)  not null,
    errand_id         varchar(36)  not null,
    -- Radens mal, satt vid publicering ur namespacets PROCESS_CONSUMER. Relayet slar inte
    -- upp konfigurationen pa nytt, och grupperar pa den har kolumnen. Se 7.6.
    process_service   varchar(64)  not null,
    -- Nullbar: kravs for CREATE och UPDATE, irrelevant for DELETE dar pw matchar
    -- pa businessKey. Se 2.2 steg 5.
    process_key       varchar(128),
    event_type        varchar(64)  not null,   -- CREATE | UPDATE | DELETE
    event_sub_type    varchar(64)  not null,   -- ERRAND | MESSAGE | ATTACHMENT | ...
    -- Far handelsen starta en NY instans? Utraknat vid publicering, 7.7. Ett kommando satter
    -- den sjalv; en vanlig arendeandring far den bara i automatiskt lage.
    start_allowed     tinyint(1)   not null default 0,
    -- Meddelandenamnet ur BPMN, satt bara for rader med subtypen SIGNAL. Utan den kan pw inte
    -- veta VILKEN grind handlaggaren tryckte pa. Modelldata, inte arendedata. Se 5.9.
    signal_name       varchar(128),
    executed_by       varchar(255),            -- X-Sent-By-varde, for sparbarhet. Styr inte loop-filtret
    request_group_id  varchar(36),
    created           datetime(3)  not null,
    -- Soft delete, och den enda medvetna avvikelsen fran notification_dispatch: nodbromsen
    -- i 6.5 raknar rader i ett tidsfonster och behover dem kvar en stund. Ingen retry_count,
    -- ingen next_retry_at, ingen dead_letter - en oskickad rad ar sin egen kvittering. Se 8.3.
    delivered_at      datetime(3),
    primary key (id)
) engine=InnoDB;

create index if not exists idx_peo_dispatch on process_event_outbox (delivered_at, created);
-- Hamtningen: oskickade rader for EN konsument, aldst forst. Se 7.6.
create index if not exists idx_peo_consumer on process_event_outbox (process_service, delivered_at, created);
create index if not exists idx_peo_guard    on process_event_outbox (errand_id, delivered_at, created);

-- 2. Processinstans, inklusive lasets tillstand.
create table if not exists errand_process (
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
    constraint uq_ep_process_instance_id   unique (process_instance_id),
    constraint uq_ep_one_active_per_errand unique (errand_id, active_marker)
) engine=InnoDB;

create index if not exists idx_ep_errand_id  on errand_process (errand_id);

alter table if exists errand_process
    add constraint fk_ep_errand_id foreign key (errand_id) references errand (id) on delete cascade;

-- 3. Append-only faktalogg. Processagnostisk: inga FK mot SM-metadata, ingen validering.
create table if not exists errand_process_activity (
    id                         varchar(36)  not null,
    -- Nullbar med flit: SM skriver CONFIG/ERROR-poster (tvetydig etikett, nodbroms) innan
    -- nagon processinstans finns. Se 4.2. errand_id ar da enda kopplingen.
    errand_process_id          varchar(36)  null,
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
    constraint fk_epa_process foreign key (errand_process_id)
        references errand_process (id) on delete cascade,
    constraint fk_epa_errand foreign key (errand_id)
        references errand (id) on delete cascade,
    constraint uq_epa_idempotency unique (errand_process_id, external_task_id, activity_id)
) engine=InnoDB;

create index if not exists idx_epa_process_occurred on errand_process_activity (errand_process_id, occurred_at);
create index if not exists idx_epa_errand_occurred   on errand_process_activity (errand_id, occurred_at);
create index if not exists idx_epa_retention         on errand_process_activity (created);

-- V1_54: arendets beslut. Ett per arende - unikheten AR invarianten (7.5). Arendedata, inte
-- processmaskineri: darfor egen tabell och JPA-relation pa ErrandEntity, till skillnad fran
-- tabellerna ovan.
create table if not exists errand_decision (
    id                    varchar(36)  not null,
    errand_id             varchar(255) not null,
    municipality_id       varchar(8)   not null,
    namespace             varchar(32)  not null,
    outcome               varchar(32)  not null,   -- DecisionOutcome
    method                varchar(16)  not null,   -- MANUAL | AUTOMATIC
    decided_by            varchar(255) not null,   -- AD-konto vid MANUAL, konsumentnamn vid AUTOMATIC
    decided_at            datetime(3)  not null,
    legal_basis           varchar(255),
    delegation_reference  varchar(64),
    justification         text,                    -- motivering; innehaller personuppgifter
    appealable            bit,
    attachment_id         varchar(36),             -- beslutshandlingen, maste tillhora arendet
    -- Vilken processrad som fattade beslutet. Nullbar: manuella beslut, och arenden helt utan process.
    -- SET NULL, inte CASCADE - beslutet overlever att processraden stads bort.
    errand_process_id     varchar(36),
    version               int          default 0 not null,   -- barer ETag
    created               datetime(3)  not null,
    modified              datetime(3),
    primary key (id),
    constraint uq_ed_errand_id unique (errand_id),
    constraint fk_ed_errand  foreign key (errand_id)         references errand (id) on delete cascade,
    constraint fk_ed_process foreign key (errand_process_id) references errand_process (id) on delete set null
) engine=InnoDB;

-- V1_55: vad processen just nu vantar pa fran handlaggaren (5.9). Ersatts i sin helhet vid
-- varje rapport. Tom mangd = processen vantar inte pa nagon manniska.
create table if not exists errand_process_signal (
    id                varchar(36)  not null,
    errand_process_id varchar(36)  not null,
    name              varchar(128) not null,   -- meddelandenamnet i BPMN, t.ex. 'granskning-godkand'
    label             varchar(255),            -- visningstext, t.ex. 'Godkann granskning'
    sort_order        int          default 0 not null,
    created           datetime(3)  not null,
    primary key (id),
    constraint uq_eps_process_name unique (errand_process_id, name),
    constraint fk_eps_process foreign key (errand_process_id)
        references errand_process (id) on delete cascade
) engine=InnoDB;

create index if not exists idx_eps_process on errand_process_signal (errand_process_id);
```

### 3.2 Hur tabellerna hänger ihop

|                    Tabell                    |                              FK                               |                                                                                                            Motiv                                                                                                            |
|----------------------------------------------|---------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `process_event_outbox` → `errand`            | **Ingen, medvetet**                                           | Samma val som `notification_dispatch` (`V1_38`). Ett DELETE-event måste överleva att ärendet raderas                                                                                                                        |
| `errand_process` → `errand`                  | `ON DELETE CASCADE`, **ingen JPA-relation på `ErrandEntity`** | DB-kaskaden räcker för att undvika föräldralösa rader vid `repository.deleteById` (`ErrandService.deleteErrand`). JPA-mappning vore aktivt skadlig (§1.4)                                                                   |
| `errand_process_activity` → `errand_process` | `ON DELETE CASCADE`, **nullbar**                              | Poster utan instans måste kunna skrivas (§4.2)                                                                                                                                                                              |
| `errand_process_activity` → `errand`         | `ON DELETE CASCADE`                                           | Krävs när instans-FK:n är nullbar — annars överlever instanslösa poster ärendet. InnoDB tillåter båda kaskadvägarna parallellt                                                                                              |
| `errand_decision` → `errand`                 | `ON DELETE CASCADE`, **med JPA-relation på `ErrandEntity`**   | Motsatt val mot raderna ovan, och avsiktligt. Beslutet ska ligga i ärendets aggregat och därmed i revisionssnapshotten (§7.5). Det skrivs en handfull gånger per ärendes livstid, inte per arbetssteg — inget revisionsbrus |
| `errand_process_signal` → `errand_process`   | `ON DELETE CASCADE`                                           | Signalerna är processens tillstånd, inte ärendedata. Försvinner processraden ska de följa med                                                                                                                               |
| `errand_decision` → `errand_process`         | `ON DELETE SET NULL`, **nullbar**                             | Spårbarhet till processen som fattade beslutet. Nullbar för manuella beslut och för ärenden helt utan process; `SET NULL` för att beslutet inte får försvinna med processraden                                              |

**Retention:** aktiviteter röjs på `created` (default 365 d); **levererade** outbox-rader röjs när `delivered_at` är äldre än **max(loop-guard-fönstret × 6, 24 h)**; oskickade rader röjs aldrig av städningen utan ligger kvar tills de gått igenom eller åldrats ur (§8.3). Beslutet röjs aldrig separat — det följer ärendet.

---

## 4. Domänmodell

### 4.1 `ProcessStatus` — enumet vet själv vad som är slut

Statusen gör mer än att visas i gränssnittet: det är den som avgör om `active_marker` sätts, och därmed om
ärendet kan få en ny processinstans. Den kopplingen ska ligga på enumet självt och ingen annanstans — en
separat lista över "vilka statusar räknas som avslutade" är en lista någon glömmer att uppdatera.

```java
package se.sundsvall.supportmanagement.integration.db.model.enums;

public enum ProcessStatus {

    /** Ett arbetssteg kor just nu. */
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

> **Här är fällan:** `WAITING` betyder att processen inte arbetar just nu, och det är lätt att läsa som
> "klar". Behandlar man den som avslutad får raden `active_marker = NULL`, och då går det att starta en
> **andra** processinstans på samma ärende. Hela regeln om en process per ärende faller. Ett tabelldrivet
> test ska därför gå igenom **varje** värde i enumet mot `isTerminal()`, med `WAITING` uttryckligen
> kontrollerad som icke avslutad.

Motsvarande i pw-alkt (`se.sundsvall.alkt.api.model.ProcessStatus`) med samma fem värden.

### 4.2 De övriga enumen

```java
public enum ActivitySeverity { INFO, WARN, ERROR }

/** Beslutets utfall. Forvaltningsrattsliga varden, inte verksamhetsspecifika (7.5). */
public enum DecisionOutcome {
    APPROVAL,          // bifall
    PARTIAL_APPROVAL,  // delvis bifall
    REJECTION,         // avslag
    DISMISSAL,         // avvisning
    DISCONTINUATION,   // avskrivning
    OTHER
}

/** Hur beslutet fattades. Skillnaden maste ga att svara pa i efterhand - FL 28 §. */
public enum DecisionMethod { MANUAL, AUTOMATIC }
```

**`DecisionOutcome` är avsiktligt litet och gemensamt.** Värdena kommer ur förvaltningsrätten och gäller
lika i bygglov, försörjningsstöd och tillsyn. Verksamhetsspecifika utfall hör inte hemma i enumet — de
uttrycks i `legalBasis`, `delegationReference` och `justification`. Växer enumet per verksamhet är det ett
tecken på att beslutet egentligen behöver en egen modell för just den verksamheten, inte ett värde till här.

`activityType` och `activityId` är däremot **fria strängar** som SM inte tolkar alls. I pw används
`PHASE`, `TASK` och `INCIDENT`. SM skriver själv `START` när en handläggare startar processen för hand
(§5.10), `CONFIG` när etiketterna pekar åt två håll och
`CONCURRENCY` när två arbetssteg är igång samtidigt (§6.4).

**Vissa poster hör inte till någon processinstans.** `CONFIG`-posterna och nödbromsens felpost skrivs
just när etiketterna är tvetydiga eller när händelserna skenar — och då finns oftast ingen instans att
hänga posten på. Därför är `errand_process_id` nullbar (§3.1), och aktiviteterna läses per ärende i
stället för per instans (§5.2).

### 4.3 Entiteten `ErrandProcessEntity`

Byggd som `NamespaceConfigEntity`: `@PrePersist`/`@PreUpdate` sätter tidsstämplarna och
`@TimeZoneStorage(NORMALIZE)` håller tidszonerna i schack.

```java
@Entity
@Table(name = "errand_process",
    indexes = {
        @Index(name = "idx_ep_errand_id", columnList = "errand_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_ep_process_instance_id",   columnNames = "process_instance_id"),
        @UniqueConstraint(name = "uq_ep_one_active_per_errand",  columnNames = {"errand_id", "active_marker"})
    })
public class ErrandProcessEntity {

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

**`applyStatus` ska vara enda vägen att sätta status.** Sätter någon `processStatus` direkt hamnar `active_marker` ur synk, och då är vi tillbaka i fällan från §4.1. Håll settern privat eller paketprivat.

Lägg märke till att `ended` nollställs när statusen går tillbaka till något som lever. Det spelar roll den dag en incident löses för hand i Operaton och en `FAILED` instans börjar köra igen — raden ska inte bära en sluttid mitt under pågående körning. Samtidigt frigjordes platsen som `active_marker` håller när instansen blev avslutad, så hann ett annat flöde starta en instans under tiden får återupplivningen `409` på `uq_ep_one_active_per_errand`. Det är rätt svar, men felmeddelandet måste tala om vilken instans som står i vägen.

---

## 5. API

### 5.1 Så rapporterar processen in till SM

```http
PUT /{municipalityId}/{namespace}/errands/{errandId}/processes/{processInstanceId}
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

Rapporten kan också bära `awaitingSignals` — vad processen väntar på från handläggaren. Det hör till den
manuella stegningen och beskrivs i §5.9.

`errandVersion` är frivillig och talar om vilken version av ärendet arbetssteget läste. Är den med och
ärendet hunnit ändras svarar SM `412` och skriver ingenting alls — se §6.3. Ett arbetssteg som ändå
skriver tillbaka till ärendet behöver den inte, för då gör `If-Match` på själva PATCH-anropet samma jobb.

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

**`POST .../processes` — registrera ett startförsök.** Två fall:

| Utfall i Operaton  |                                     Kropp                                      |                                                                               SM svarar                                                                               |
|--------------------|--------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Start lyckades     | `processKey`, `processInstanceId`, `processStatus: RUNNING`                    | Raden saknas ⇒ `201` med `Location`. Raden finns redan med **samma** `processInstanceId` ⇒ `200`, och ingenting ändras. Annan **levande** instans för ärendet ⇒ `409` |
| Start misslyckades | `processKey`, `processStatus: FAILED`, `error` — **inget** `processInstanceId` | Terminal rad skapas ⇒ `201`                                                                                                                                           |

```json
{ "processKey": "alkt-ansokan", "processStatus": "FAILED",
  "error": { "code": "START_FAILED", "message": "..." } }
```

**En process som gått i mål startas inte om.** Har ärendet en instans som är `COMPLETED` svarar `POST`
`409`, även om ingen instans lever just nu. En `FAILED` instans står däremot inte i vägen — att försöka
igen efter ett misslyckat startförsök är återhämtning, inte en ny process (§7.4).

**Det som gör hela upplägget ofarligt: `POST` skapar, men uppdaterar aldrig.**

Operaton kan nämligen lämna ut det första arbetssteget innan `startProcessInstance` ens hunnit svara pw.
Ett arbetssteg — kanske i en helt annan pod — kan alltså hinna `PUT`:a sin `RUNNING`-rapport innan pw:s
`POST` kommer fram. Eftersom `PUT` skapar eller uppdaterar medan `POST` bara skapar spelar ordningen ingen
roll: den som kommer först skapar raden, och den andra gör antingen ingenting eller en helt vanlig
uppdatering. Utan den regeln hade pw fått `409` på en fullt frisk process och, enligt §9.3, avbrutit den.

Koden får inte heller förlita sig på vilken av de unika nycklarna som råkar slå till först —
`uq_ep_process_instance_id` och `uq_ep_one_active_per_errand` kan båda träffa på samma insert. Slå upp
`process_instance_id` i stället, både före och efter:

```
existing = findByProcessInstanceId(piid)
if existing: return 200 existing            // nagon hann fore, ror ingenting

if hasCompletedProcess(errandId):
    throw 409                               // processlivet ar over, se 7.4

try:
    insert(... RUNNING ...)
    return 201
catch ConstraintViolationException:
    existing = findByProcessInstanceId(piid)
    if existing: return 200 existing        // kapplopningen vanns av den andra
    throw 409                               // annan levande instans for arendet
```

Den misslyckade starten kan aldrig hamna i den kapplöpningen: utan `processInstanceId` finns det ingen
processinstans, och därmed varken något arbetssteg eller någon som kan rapportera.

### 5.2 Vad som går att läsa ut

```
GET .../errands/{errandId}/processes            -> 200 ErrandProcesses
                                                   { startable, processes }; nyast först i listan,
                                                   normalt exakt ett element (§5.10)
GET .../errands/{errandId}/process-activities   -> 200 Page<ProcessActivity>
                                                   ?processInstanceId= (valfritt filter)
                                                   &page=&size=50 (max 200)&sort=occurredAt,desc
GET .../errands/{errandId}/decision             -> 200 Decision | 404 (§7.5)
GET .../errands/{errandId}                      -> 200 Errand med process- och decision-objekten
```

Aktiviteterna läses **per ärende, inte per processinstans**. Annars går det inte att komma åt de poster som
saknar instans (§3.1) — och det är just de posterna som förklarar varför ingen process startade.

**Vi bygger ingen egen endpoint för att felsöka etiketterna.** Frågan "vilka etiketter startar en process?"
går redan att svara på med `GET /{municipalityId}/{namespace}/metadata/labels`, som lämnar tillbaka hela
etikettträdet med `id`, `resourcePath`, `deprecated` och `attributes` (se `MetadataMapper.toLabel`) —
klienten filtrerar själv på attributnyckeln `processKey`. Den fråga man faktiskt ställer i drift handlar
dessutom om ett enskilt ärende, inte om hela namespacet, och den besvaras av aktivitetsloggen (§8.2).

### 5.3 Modellerna i API:et

```java
/** Bade subresurs under /processes och projektion pa Errand - en modell, inte tva. */
@Schema(description = "A process attached to an errand, and its state")
public class ErrandProcess {
    @Schema(accessMode = READ_ONLY)  private String id;
    private String processService;                  // required on write
    private String processKey;                      // required on write
    private String processInstanceId;               // required i POST, tas ur pathen i PUT
    private ProcessStatus processStatus;            // required on write
    private String currentActivityId;
    private String currentActivityName;
    @Schema(accessMode = WRITE_ONLY) private String externalTaskId;  // idempotensnyckel for activities
    @Schema(accessMode = WRITE_ONLY) private Long errandVersion;     // valfri; versionen steget laste (6.3)
    private OffsetDateTime started;
    private OffsetDateTime ended;
    private ProcessError error;
    @Schema(accessMode = WRITE_ONLY) private List<ProcessActivity> activities;  // lases via egen endpoint
    private List<ProcessSignal> awaitingSignals;    // vad processen vantar pa fran handlaggaren (5.9)
    @Schema(accessMode = READ_ONLY)  private OffsetDateTime created;
    @Schema(accessMode = READ_ONLY)  private OffsetDateTime modified;
}

/** Ett val handlaggaren kan gora for att stega processen vidare. Namnen kommer ur BPMN, SM tolkar dem inte. */
public class ProcessSignal {
    @NotBlank private String name;     // meddelandenamnet i modellen, 'granskning-godkand'
    private String label;              // visningstext, 'Godkann granskning'
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

/** Arendets beslut. Agare ar arendet, inte processen - se 7.5. */
@Schema(description = "Decision made on an errand")
public class Decision {
    @Schema(accessMode = READ_ONLY) private String id;
    @NotNull  private DecisionOutcome outcome;
    @NotNull  private DecisionMethod method;                   // MANUAL | AUTOMATIC
    @NotBlank private String decidedBy;                        // AD-konto vid MANUAL, konsumentens namn vid AUTOMATIC
    @NotNull  private OffsetDateTime decidedAt;
    private String legalBasis;                                 // lagrum, "9 kap. 30 § PBL"
    private String delegationReference;                        // punkt i delegationsordningen, "3.2.1"
    private String justification;                              // motivering - innehaller personuppgifter, se 11
    private Boolean appealable;
    @ValidUuid(nullable = true) private String attachmentId;    // beslutshandlingen; maste tillhora arendet
    @Schema(accessMode = READ_ONLY) private String processId;   // errand_process.id, satts vid AUTOMATIC
    @Schema(accessMode = READ_ONLY) private OffsetDateTime created;
    @Schema(accessMode = READ_ONLY) private OffsetDateTime modified;
    @Schema(accessMode = READ_ONLY) private Integer version;    // barer ETag
}
```

`Errand` utökas med två läsprojektioner:

```java
@Schema(accessMode = READ_ONLY, description = "Process state driving this errand; null for namespaces without a process model")
private ErrandProcess process;

@Schema(accessMode = READ_ONLY, description = "Decision registered on this errand; null until a decision is made")
private Decision decision;
```

**Båda fälten är `ErrandField`-värden**, `PROCESS` och `DECISION`, och går därmed genom
`AccessControlService.roleBasedFieldResolver` som allt annat på ärendet. Skälet är `justification`: en
beslutsmotivering är fritext med personuppgifter, och den får inte vara det enda känsliga fältet på
ärendet som står utanför fältfiltreringen.

För ALKT gör det ingen skillnad — namespacet använder inte AccessMapper (§1.8), så resolvern vänder direkt.
Tillägget finns för nästa namespace. Designen är byggd för att bäras av fler (§7.6), och ett fält som
smiter förbi fältfiltreringen är svårt att upptäcka i efterhand just för att det fungerar i det första
namespacet som tar den i bruk.

Beteendet följer av hur resolvern redan fungerar, kontrollerat i koden:

|                   Läge                    |                                                           Utfall                                                           |
|-------------------------------------------|----------------------------------------------------------------------------------------------------------------------------|
| Namespace utan åtkomstkontroll — som ALKT | `roleBasedFieldResolver` returnerar `null` direkt och ingenting filtreras. Fälten syns, precis som i dag                   |
| Användare som ingen restriktion träffar   | Samma sak: `null`, hela ärendet                                                                                            |
| Begränsad läsning eller rollrestriktion   | Kartan är en **tillåtelselista**. `PROCESS` och `DECISION` saknas där tills namespacet räknar upp dem, alltså utelämnas de |

Tillägget är därför additivt och stängt som utgångsläge, vilket är rätt riktning för ett fält som bär
personuppgifter. Två saker följer: gränssnittet måste tåla att `process` och `decision` saknas för en
begränsad användare, och ett namespace som vill visa dem lägger till dem i `limitedReadAccess.fields`,
`roleFieldRestrictions` eller `reporterAccess`.

**Det som återstår att bestämma** är om `decision` ska vara reducerad i listsvar. `findErrands` returnerar
i dag hela beslutet per träff, `justification` inräknad, och det är både nyttolast och dataminimering. Ett
rimligt val är utfall, `method` och `decidedAt` i listan och hela beslutet vid enskild läsning — men det är
ett beslut som ska tas innan T3 byggs, inte efteråt.

**En modell, inte två.** `ErrandProcess` används både som svar från `/processes` och som fältet på ärendet.
De tre fälten som bara hör hemma i en rapport — `externalTaskId`, `errandVersion` och `activities` — är
`WRITE_ONLY` och syns aldrig när man läser, och modellen serialiseras utan null-fält. Alternativet, två
snarlika modeller som ska hållas i takt, glider isär första gången någon lägger till ett fält på bara det
ena hållet.

**`processInstanceId` går att skriva.** `POST` skickar den i kroppen — det är själva poängen med att
registrera en start (§5.1) — medan `PUT` tar den ur adressen. Skickas den ändå med i en `PUT` måste den
vara samma som i adressen, annars `400`. (I ett tidigare utkast stod fältet som `READ_ONLY` samtidigt som
`POST`-exemplet skickade det. Nu är det entydigt.)

**`process` visar den senaste instansen, inte den som lever.** Skillnaden syns i två lägen, och båda är
sådana handläggaren måste få se: en misslyckad start lämnar efter sig en `FAILED`-rad och ingen levande
instans, och en process som gått i mål lämnar en `COMPLETED`-rad och ingen levande instans. Visade vi bara
den levande skulle båda se ut som `null` — alltså precis som "ärendet har ingen process" — och ett felstavat
`processKey` hade varit osynligt trots att §11 lovar motsatsen.

Det ställer ett krav på hur fältet fylls i: frågan är "senaste raden per ärende", inte "raden där
`active_marker = 1`". Över en hel lista med ärenden kräver det antingen en fönsterfunktion (`ROW_NUMBER()`,
finns i MariaDB 10.6) eller en join mot `max(created)` — men det ska fortfarande bli **en** fråga, inte en
per ärende. `decision` hämtas i samma sväng och är en rak join på `errand_id`, eftersom det bara finns ett
beslut att välja på (§7.5).

`awaitingSignals` är en barnsamling och går inte att hämta i samma fråga utan att multiplicera raderna.
Hämta dem i **en** extra fråga för hela sidan, nycklad på de processrader man redan har — två frågor
totalt, inte en per ärende.

Kuvertet kring `GET .../processes` och modellerna för manuell start står i §5.10. De hör ihop med
startbeslutet och läses lättast i ett sammanhang.

### 5.4 Det SM skickar till pw-alkt

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
  "startAllowed": false,
  "signalName": null,
  "occurredAt": "2026-08-19T09:12:03.221+02:00"
}
```

**Meddelandet innehåller ingen ärendedata.** Behöver pw veta vad som står i ärendet hämtar det `GET /errands/{errandId}` självt. Det håller personuppgifter borta från outboxen, loggarna och en framtida meddelandekö — och det gör att en försenad leverans aldrig kan råka skicka ut en gammal bild av ärendet.

|            Svar            |                                                      När                                                       |
|----------------------------|----------------------------------------------------------------------------------------------------------------|
| `202 Accepted`             | Hanterat, eller medvetet ignorerat (okänt ärende, mismatch, DELETE utan instans)                               |
| `422 Unprocessable Entity` | `processKey` matchar ingen driftsatt processmodell. Felet är **permanent** — det hjälper inte att försöka igen |
| `5xx`                      | Tillfälligt fel, försök igen                                                                                   |

```java
public class ErrandEvent {
    @NotBlank private String eventId;
    @NotNull  private EventType eventType;      // CREATE | UPDATE | DELETE
    private String eventSubType;
    @ValidUuid private String errandId;
    private String processKey;                  // instansens nyckel, annars etiketternas. Null vid
                                                //  DELETE av arende utan losbar nyckel
    @NotNull private Boolean startAllowed;      // far handelsen foda en ny instans? SM raknar ut det
                                                //  vid publicering (7.7). Villkorar start (9.3)
    private String signalName;                  // bara vid subtyp SIGNAL: meddelandenamnet ur BPMN.
                                                //  Det pw korrelerar pa i stallet for errandUpdated (5.9)
    private OffsetDateTime occurredAt;
}
```

**`startAllowed` är ett lov, inte ett kommando.** pw kontrollerar fortfarande mot Operaton att ingen instans
redan kör innan den startar (§9.3) — lovet säger bara att SM:s regler inte står i vägen. Fältet är
obligatoriskt, och **saknas det ska pw läsa det som `false`**. Riktningen är medvetet den motsatta mot
tveksamma headervärden i §6.5, och asymmetrin är avsiktlig: en process som startar när den inte borde
arbetar på ett riktigt ärende och förbrukar dess enda processliv (§7.4), medan en start som uteblir syns
direkt i gränssnittet som en tänd knapp och rättas med ett klick (§5.10).

**`signalName` är det enda fält som pekar in i processmodellen.** Vid subtypen `SIGNAL` bär det
meddelandenamnet ur BPMN, och det är namnet pw korrelerar på i stället för det generiska `errandUpdated`
(§9.3). Utan fältet vet pw att någon tryckte på en knapp men inte på vilken, och manuell stegning fungerar
inte alls — grinden öppnas aldrig. Namnet är modelldata och inte ärendedata, precis som `processKey`, så
det bryter inte mot regeln att meddelandet inte bär något ur ärendet.

### 5.5 Det pw-alkt rapporterar tillbaka

```java
/** Returneras av varje arbetssteg. Basklassen skickar den vidare till SM. */
public record ProcessStateReport(
        ProcessStatus status,
        String currentActivityId,
        String currentActivityName,
        Long errandVersion,
        ProcessError error,
        List<ProcessActivity> activities,
        /** Resultatvarden som skrivs nar steget slutfors. Tom map = inga. Se 9.2 punkt 5. */
        Map<String, Object> variables) {

    public static ProcessStateReport running(String activityId, String activityName) { ... }
    public static ProcessStateReport waiting(String activityId, String activityName) { ... }
    public static ProcessStateReport completed() { ... }
    public static ProcessStateReport failed(String code, String message) { ... }
    public static ProcessStateReport retrying(String code, String message) { ... }

    /** Kopia med resultatvarden. Basklassen skickar dem till complete(task, variables). */
    public ProcessStateReport withVariables(Map<String, Object> variables) { ... }
}
```

Fabriksmetoderna finns just för att den som skriver ett arbetssteg inte ska behöva hålla reda på vilka statusar som räknas som avslutade.

**`variables` är den enda vägen ut för ett resultatvärde.** Ett arbetssteg kan inte anropa
`complete(task, variables)` självt, eftersom det är basklassen som slutför task:en (§9.4). Utan fältet
skulle ingen gateway i modellen kunna läsa något — och kontrollen framför varje väntläge bygger på just
det (§9.2 punkt 5). Fältet går aldrig vidare till SM: det hör till Operaton, inte till ärendet.

### 5.6 Vilka svar SM ger

De två skrivvägarna släpps in på olika sätt, och det är med flit. **Processrapporten** godtas bara från
namespacets `PROCESS_CONSUMER`, utpekad med `X-Sent-By` — den bär processens tillstånd och inget
ärendeinnehåll. Det är den enda plats vid sidan av beslutets `method` där `X-Sent-By` styr ett utfall;
loop-skyddet läser den inte (§6.5). **Beslutet** går den vanliga vägen för ärendeskrivningar, eftersom det *är* ärendedata.

#### Vilken `ProtectedResource` varje väg skyddas av

`AccessControlService.getErrand(...)` och `.verifyExistingErrandAndAuthorization(...)` **kräver** en
`ProtectedResource` och en lägsta nivå — det finns ingen överlagring utan. Varje ny endpoint måste alltså
peka ut en, och `ERRAND` är fel svar: den skulle ge processens rapporter samma behörighet som ärendet
självt. Tre nya värden tillkommer, alla under `errand/`-subträdet så att ett mönster som `errand/**`
täcker dem:

|                Väg                |              `ProtectedResource`               |  Nivå   |
|-----------------------------------|------------------------------------------------|---------|
| `GET .../processes`               | `PROCESS` — `errand/process`                   | `R, RW` |
| `PUT`/`POST .../processes`        | `PROCESS`                                      | `RW`    |
| `POST .../processes/{id}/signals` | `PROCESS`                                      | `RW`    |
| `POST .../processes/start`        | `PROCESS`                                      | `RW`    |
| `GET .../process-activities`      | `PROCESS_ACTIVITY` — `errand/process-activity` | `R, RW` |
| `GET .../decision`                | `DECISION` — `errand/decision`                 | `R, RW` |
| `PUT`/`DELETE .../decision`       | `DECISION`                                     | `RW`    |

Nivåerna följer regeln i §1.3: skrivvägar skickar `RW` ensamt, läsvägar `R, RW`. Signalen kunde ha varit en
egen resurs — att stega processen är något man kan vilja dela ut separat — men den ligger under `PROCESS`
tills behovet visar sig.

`verifyNamespaceAuthorization` används **inte** av de här vägarna. Den gäller resurser som hör till
namespacet självt, som konfiguration och metadata; våra ligger alla under ett ärende.

**`.../processes`**

|  Kod  |                                                                                                                  När                                                                                                                  |
|-------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `400` | Etikettändring som byter `processKey`; `processInstanceId` i `PUT`-kroppen skiljer sig från pathens                                                                                                                                   |
| `403` | `X-Sent-By` saknas; `processService` matchar inte namespacets `PROCESS_CONSUMER`                                                                                                                                                      |
| `404` | Ärendet finns inte eller ligger i annat namespace                                                                                                                                                                                     |
| `409` | Annan levande instans för ärendet **med ett annat `processInstanceId`**; ärendet har redan en `COMPLETED` instans (§7.4); instans med annat `process_key` än ärendets befintliga. Samma `processInstanceId` är aldrig `409` — se §5.1 |
| `412` | `errandVersion` i rapporten matchar inte ärendets aktuella version (§6.3)                                                                                                                                                             |

Statuskoderna för `.../signals` står i §5.9 och för `.../processes/start` i §5.10.

**`.../decision`**

|  Kod  |                                                            När                                                            |
|-------|---------------------------------------------------------------------------------------------------------------------------|
| `400` | Kroppen validerar inte; `attachmentId` pekar på en bilaga som inte tillhör ärendet                                        |
| `403` | `method: AUTOMATIC` från någon annan än namespacets `PROCESS_CONSUMER`; `method: MANUAL` från en icke-AD-identitet (§7.5) |
| `404` | Ärendet finns inte; eller, för `GET`/`DELETE`, inget beslut registrerat                                                   |
| `409` | Ärendet har en `COMPLETED` process — beslutet är låst (§7.5)                                                              |
| `412` | `If-Match` matchar inte beslutets `version`                                                                               |

### 5.7 Vad som ändras för dem som redan använder API:et

**Ingenting.**

Inga nya statuskoder, inga nya spärrar och inget nytt felfall att hantera i gränssnittet. Headern
`X-Trigger-Process` är valfri, och utelämnad betyder den precis det som gäller i dag (§6.5). Den optimistiska
samtidighetskontrollen (§6.2) använder `If-Match` och `412`, som redan finns på ärendet och dess parametrar
och redan står i specen. Verksamheter utanför ALKT märker ingenting alls.

Ärendet får två nya läsfält, `process` och `decision`, och `.../decision` är en ny adress. Båda är rena
tillägg — en klient som inte känner till dem påverkas inte.

Däremot finns det två saker gränssnittet **vinner på** att visa. Det ena är processläget:
`errand.process.processStatus` och `currentActivityName` berättar om en process arbetar med ärendet just
nu, och ett *"Processen arbetar med ärendet: hämtar beslutsunderlag"* räcker för att handläggaren ska
förstå varför ärendet plötsligt kan ändra sig. (Att skicka `If-Match` även från gränssnittet är också
en förbättring, för då upptäcks krocken i stället för att den sista skrivningen vinner — men det är en
fristående sak, inget krav härifrån.)

Det andra är beslutet. `errand.decision` är tom tills ett beslut finns, och när det väl finns räcker det
inte att visa utfallet: `method` talar om ifall det var en handläggare eller processen som fattade det,
och just den skillnaden har både handläggaren och den sökande rätt att se.

### 5.8 Det som tas bort i pw-alkt

`POST /process/start/{errandId}` och `POST /process/update/{processInstanceId}` **tas bort** i samma steg som `errand-events` införs. Med dem försvinner även: `ProcessService.updateProcess`, `StartProcessResponse` + test, `AbstractTaskWorker.clearUpdateAvailable` (död kod), `Constants.PROCESS_VARIABLE_UPDATE_AVAILABLE`, `Constants.FALSE`, `OperatonClient.setProcessInstanceVariable(s)`, `AbstractTaskWorker.setProcessInstanceVariable`.

Vi förlorar ingen nödutgång på kuppen: behöver någon starta en process för hand går det utmärkt att posta ett `errand-events` — samma kod som i skarp drift.

### 5.9 Manuell stegning — signaler

I ALKT ska de flesta övergångar styras av handläggaren, inte ske av sig själva. Andra processmodeller, och
andra namespace, kan vilja tvärtom. Båda ryms i samma maskineri, och **valet görs i processmodellen** —
inte i konfigurationen.

Ett väntläge som bara går vidare på ett *namngivet* meddelande är en manuell grind. Ett väntläge som går
vidare så snart villkoret är uppfyllt är automatiskt. En modell utan namngivna väntlägen beter sig precis
som innan det här avsnittet fanns, så automatiskt förblir default.

Att valet ligger i modellen och inte i `namespace_config` är avsiktligt: en inställning i SM skulle kunna
säga en sak medan modellen gör en annan, och då finns ingen instans som har rätt.

#### Processen berättar vad den väntar på

Rapporten (§5.1) utökas med `awaitingSignals`. Den fylls när processen går in i ett väntläge:

```json
{ "processStatus": "WAITING",
  "currentActivityId": "review_phase",
  "currentActivityName": "Granskning",
  "awaitingSignals": [
    { "name": "granskning-godkand", "label": "Godkänn granskning" },
    { "name": "granskning-avvisad", "label": "Skicka tillbaka för komplettering" }
  ] }
```

SM tolkar inte namnen. De kommer ur BPMN-modellen och relayas tillbaka precis som de kom, på samma sätt som
`activityType` (§4.2). Därmed finns ingen lista i SM som kan hamna ur synk med modellen — läggs en grind
till i BPMN dyker den upp i gränssnittet utan att någon rör SM.

Listan ersätts i sin helhet vid varje rapport. Ett tomt `awaitingSignals` betyder "processen väntar inte på
dig", vilket är det normala för ett automatiskt väntläge.

#### Handläggaren svarar

```http
POST /{municipalityId}/{namespace}/errands/{errandId}/processes/{processInstanceId}/signals
```

```json
{ "signal": "granskning-godkand" }
```

Svar `202 Accepted`: SM har registrerat avsikten och publicerat händelsen. Vad processen sedan gör avgör
processen.

|  Kod  |                                När                                |
|-------|-------------------------------------------------------------------|
| `202` | Signalen är registrerad och publicerad                            |
| `400` | `signal` saknas eller är tom                                      |
| `403` | Anroparen är ingen AD-identitet (§5.10)                           |
| `404` | Ärendet finns inte, eller har ingen levande processinstans        |
| `409` | Signalen finns inte bland de väntade, eller processen är avslutad |

**Signalen tvingar ingenting.** Den är en begäran, och väntläget avgör om den betyder något i sitt
nuvarande läge. Ett steg som lagen kräver går inte att kliva förbi genom att posta rätt sträng — och det
är hela skälet till att handläggaren inte får sätta processens läge direkt.

`409` när signalen inte står bland de väntade är dessutom ett naturligt dubbelklicksskydd: har processen
redan gått vidare rapporterar den en ny uppsättning signaler, och den gamla knappen slutar fungera.

#### Vad skrivningen gör

1. **En aktivitetspost** med `activityType = SIGNAL`, `activityId` = signalens namn, `severity = INFO` och
   en text som namnger avsändaren. Det är den posten som i efterhand svarar på frågan *vem stegade
   processen förbi granskningen, och när*.
2. **En händelse med subtypen `SIGNAL`**, som blir en outbox-rad och når pw. Den filtreras varken av
   `PROCESS_TRIGGER` eller av nödbromsen (§6.5): en signal är inte något som hänt med ärendet, utan ett
   kommando riktat till processen. Knappen går därför inte att konfigurera eller trafikera sönder. Radens
   `signal_name` bär signalens namn — utan det vet pw inte vilken grind som trycktes (§5.4).
3. Loop-skyddets första lager släpper igenom: headern hedras inte för AD-identiteter (§6.5). Att den
   passagen är säker är också skälet till att endpointen kräver ett AD-konto och svarar `403` för alla
   andra — resonemanget står i §5.10.

pw korrelerar på signalens namn i stället för det generiska `errandUpdated` när subtypen är `SIGNAL`
(§9.3).

#### Ingen fritext på signalen

Signalen bär bara ett namn. Handläggaren kan alltså inte skicka med en motivering, och det är medvetet:

- Aktivitetsloggen **gallras efter 365 dagar** medan ärendet lever längre. Dokumentation av varför ett steg
  hoppades över får inte försvinna före ärendet.
- `message` i aktivitetsloggen får enligt §11 inte innehålla personuppgifter, och en anteckning som *"kom
  per telefon från sökandens ombud"* är precis det.

Behöver handläggaren dokumentera varför finns ärendeanteckningar, som har rätt gallring och rätt
behörighet. Spårbarheten över *att* signalen skickades, av vem och när, skriver SM ändå automatiskt.

---

### 5.10 Manuell start — kommandot och startbarheten

Ett ärende vars etikett säger `MANUAL` startar ingen process av sig självt (§7.7). Det gör handläggaren,
med en knapp — "Starta handläggning". Knappen behöver två saker av API:et: ett sätt att veta om den ska
visas, och ett sätt att tryckas.

#### Får ärendet startas just nu?

`GET .../processes` svarar med ett kuvert i stället för en naken lista, eftersom det intressanta fallet är
när ärendet **inte** har någon process. En tom lista säger att ingen process kör, men inte om det beror på
att ärendet väntar på en knapptryckning, på att processen redan gått i mål och aldrig får startas om
(§7.4), eller på att ärendet saknar processetikett.

```json
{
  "startable": { "status": "AVAILABLE", "processKeys": ["alkt-tillsyn"] },
  "processes": []
}
```

```json
{
  "startable": { "status": "PROCESS_COMPLETED", "processKeys": [] },
  "processes": [
    { "id": "1f0e4c21-...", "processInstanceId": "8f1c2b6e-...", "processKey": "alkt-ansokan",
      "processStatus": "COMPLETED", "ended": "2026-08-20T14:03:11.882+02:00" }
  ]
}
```

**Två nycklar i `processKeys` betyder att någon måste välja.** Etiketterna pekar åt två håll (§7.3), och i
stället för att gissa lämnar SM över valet: gränssnittet frågar handläggaren och skickar den valda nyckeln
i kroppen. Det är samma tvetydighet som stoppar den automatiska starten — skillnaden är att här finns en
människa som kan lösa upp den.

#### Modellerna, och vad varje fält betyder

Gränssnittet ska kunna tända, släcka och förklara knappen utan att känna till en enda av reglerna i §7.4.
Beskrivningarna i specen är därför skrivna för den som läser dem i Swagger, inte för oss.

```java
/** Svaret fran GET .../processes. Kuvert, inte naken lista - se ovan. */
@Schema(description = """
    The processes attached to an errand, and whether a new one may be started right now.""")
public class ErrandProcesses {

    @Schema(description = """
        Whether a process may be started for this errand right now. Read this before offering a start
        action to the user. The same rules are enforced by POST .../processes/start, which answers 400 or
        409 when they are not met - so a client that ignores this field can never start something it
        should not. It can only show a button that fails.""")
    private ProcessStartable startable;

    @Schema(description = """
        Every process this errand has had, most recent first. Normally exactly one element. An empty list
        is not an error and does not mean the errand is broken: see startable for whether a process can be
        started, and why not if it cannot.""")
    private List<ErrandProcess> processes;
}

@Schema(description = "Whether a process may be started for an errand, and which one")
public class ProcessStartable {

    @Schema(description = """
        AVAILABLE means a process may be started right now; every other value says why one cannot be.
        LIVE_INSTANCE - a process is already running for this errand.
        PROCESS_COMPLETED - a process has already run to its end. An errand has one process life; a new
        process means a new errand.
        NO_PROCESS_KEY - no label on the errand carries a processKey attribute, so there is nothing to
        start. Setting the right label is the fix.
        NO_PROCESS_ENGINE - this namespace does not run processes at all.
        Treat any value you do not recognise as not startable - values may be added over time.""",
        examples = "AVAILABLE")
    private ProcessStartability status;

    @Schema(description = """
        The process keys that are eligible to start, taken from the processKey attribute on the labels of
        the errand. One element is the normal case: send it - or send nothing - to POST
        .../processes/start. Two or more elements mean the errand carries labels pointing at different
        processes and a person has to choose: ask the user and send the chosen key, otherwise the request
        is rejected with 400. Empty whenever status is not AVAILABLE.""")
    private List<String> processKeys;
}

public enum ProcessStartability { AVAILABLE, LIVE_INSTANCE, PROCESS_COMPLETED, NO_PROCESS_KEY, NO_PROCESS_ENGINE }

/** Kroppen i POST .../processes/start. Far utelamnas helt nar bara en nyckel ar mojlig. */
@Schema(description = "A request to start a process for an errand")
public class ProcessStartRequest {

    @Schema(description = """
        Which process to start. May be omitted when startable.processKeys holds exactly one key, and is
        required when it holds several. The value must be one of those keys: a request cannot name a
        process that the labels of the errand do not point at.""",
        examples = "alkt-tillsyn")
    private String processKey;
}
```

**Ett fält, inte två.** Ett tidigare utkast hade `available: boolean` vid sidan av orsaken, och då finns ett
läge som säger emot sig självt: `available: false` utan orsak ger en släckt knapp utan förklaring, och
`available: true` med en orsak är rena gissningsleken för klienten. Med `status` som enda fält går
motsägelsen inte att uttrycka, och gränssnittet skriver `status === "AVAILABLE"` i stället för att väga
samman två fält. Att värdena är ett enum och inte fri text betyder samtidigt att de står i specen, så
klienten formulerar sitt eget meddelande per fall i stället för att visa en sträng från servern.

#### Kommandot

```http
POST /{municipalityId}/{namespace}/errands/{errandId}/processes/start
X-Sent-By: abc12def; type=adAccount
```

```json
{ "processKey": "alkt-tillsyn" }
```

|  Kod  |                                                                                      När                                                                                      |
|-------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `202` | Avsikten är registrerad och publicerad                                                                                                                                        |
| `400` | Ärendet har ingen `processKey` på sina etiketter; flera är möjliga men kroppen pekar inte ut någon; nyckeln i kroppen finns inte bland ärendets; namespacet har ingen process |
| `403` | Anroparen är ingen AD-identitet                                                                                                                                               |
| `404` | Ärendet finns inte, eller ligger i ett annat namespace                                                                                                                        |
| `409` | Ärendet har en levande processinstans; ett avslutat processliv (§7.4); eller en oskickad start med en **annan** nyckel redan på väg                                           |

Felen är samma regler som `startable` redovisar, och det är med flit: gränssnittet tänder knappen efter
`status` utan att duplicera kontrollerna, och servern avvisar ändå det som hunnit ändras däremellan. Med ett
undantag: `AVAILABLE` med **två** nycklar i `processKeys` betyder att klienten måste välja, och trycker den
utan att skicka någon nyckel blir svaret `400`. Det är det enda fall där `startable` säger ja och kommandot
ändå säger nej.

**Kommandot läser inte `processStartMode`.** Läget avgör om SM startar processen åt er, inte om en människa
får göra det (§7.7). Därför fungerar knappen även i automatiskt läge — och det är den vägen man startar om
efter en misslyckad start, eftersom en `FAILED` instans varken är levande eller avslutad (§7.4 regel 4).

#### Vad skrivningen gör

1. **En aktivitetspost** med `activityType = START`, `activityId` = den valda nyckeln, `severity = INFO`
   och en text som namnger avsändaren. Posten skrivs **utan processinstans** — någon sådan finns ju inte
   än — och det är precis därför `errand_process_id` är nullbar (§4.2). Det är den posten som i efterhand
   svarar på *vem startade handläggningen på det här ärendet, och när*.
2. **En händelse med subtypen `PROCESS`** och `startAllowed = 1`, som blir en outbox-rad och når pw. Den
   filtreras varken av `PROCESS_TRIGGER` eller av nödbromsen (§6.5), och radens `process_key` är den
   **valda** nyckeln — den löses inte upp ur etiketterna på nytt vid publiceringen, eftersom valet redan är
   gjort.
3. **Ingen andra rad, om det redan ligger en oskickad startrad med samma nyckel.** Svaret blir `202` ändå.
   Det är dubbelklicksskyddet, och det är billigt — `idx_peo_guard` täcker frågan. Ligger den väntande
   raden på en **annan** nyckel är det inget dubbelklick utan ett ändrat val, och svaret är `409` som säger
   att en start med en annan process redan är på väg. Att tysta den tryckningen hade startat fel process.

Skyddet är värt sin kod trots att `409` från pw:s `POST .../processes` finns bakom: enligt §5.1 kan Operaton
lämna ut det första arbetssteget innan starten ens hunnit registreras, så en instans som avbryts kan redan
ha utfört ett steg. Billigare att inte starta den.

Svaret är `202` och inte `201` av samma skäl som signalen i §5.9: SM har registrerat avsikten, men det är
processen som avgör vad som blir av den. Instansen dyker upp i `GET .../processes` först när pw rapporterat
in den — normalt inom några sekunder tack vare direktkörningen (§2.3), i värsta fall vid nästa cron-tick.
**Gränssnittet ska visa att starten är på väg** under den tiden, inte "ingen process". Annars ser ett friskt
system trasigt ut i ett par sekunder.

#### Varför kommandot kräver ett AD-konto

Loop-skyddets lager 1 skriver ingen outbox-rad när en icke-AD-identitet skickar `X-Trigger-Process: false`
(§6.5) — och pw:s `RequestInterceptor` sätter just den headern på **alla** utgående skrivningar (P3). En
maskin som anropade kommandot skulle alltså få `202`, få en aktivitetspost skriven, och ingen process
skulle starta. Kravet på AD-konto gör det till ett `403` i stället.

Vinsten är att loop-skyddet lämnas orört: eftersom lager 1 ändå inte hedras för AD-identiteter passerar
kommandot av sig självt, och §6.5 behöver inget undantag för kommandon. Priset är att en e-tjänst inte kan
skapa ett ärende och starta processen i samma svep. I automatiskt läge behövs det inte, och i manuellt läge
är det själva poängen att en människa ska ta ställning först.

Det andra skälet är spårbarhet. En manuell start av en tillsyn är ett myndighetsbeslut i miniatyr, och
aktivitetsposten ska kunna svara på vem som fattade det. Med en maskinidentitet står det ett tjänstenamn
där, vilket inte besvarar frågan. Det är samma skillnad som §7.5 redan gör för beslutet, där `decidedBy`
är ett AD-konto när `method` är `MANUAL`.

---

## 6. Samtidighet och loopar

### 6.1 Rapporten styr livscykeln

| Rapporterad status | `active_marker` |
|--------------------|-----------------|
| `RUNNING`          | 1               |
| `WAITING`          | 1               |
| `RETRYING`         | 1               |
| `COMPLETED`        | NULL            |
| `FAILED`           | NULL            |

En **annan** levande instans kan inte finnas samtidigt — `uq_ep_one_active_per_errand` hindrar det (§7.4).

### 6.2 När handläggare och process krockar

**Målet är att handläggarens ändringar inte ska tappas bort — inte att handläggaren ska hindras från att
arbeta.** Den skillnaden avgör hur vi löser det, och lösningen finns redan i SM.

`ErrandEntity` har `@Version`, och de sex skrivvägar som rör ärendets beslutsunderlag —
`ErrandService.updateErrand`, `ErrandParameterService.updateErrandParameters`/`updateErrandParameter`/`deleteErrandParameter`
och `ErrandJsonParameterService.updateJsonParameter`/`deleteJsonParameter` — gör alla
`entityManager.lock(..., OPTIMISTIC_FORCE_INCREMENT)`. Ingen annan väg i SM höjer versionen med tvång:
bilagor, kommunikation, konversationer, anteckningar och notiser ligger i egna tabeller och rör inte
ärenderaden.

`errand.version` betyder alltså redan precis det vi behöver: **ärendets beslutsunderlag har ändrats**.
`GET /errands/{id}` lämnar tillbaka versionen både som `ETag`-header och i fältet `version`, `PATCH` tar
emot `If-Match` och svarar `412` när den inte stämmer, och `ETagUtil.validateIfMatch` hoppar över kontrollen
helt när headern saknas. Varje klient väljer alltså själv, och allt är redan i drift och testat.

**Så här gör pw:**

1. Arbetssteget läser ärendet och sparar undan `ETag`.
2. Det skriver tillbaka med `If-Match: "<version>"`.
3. Kommer `412` betyder det att handläggaren hann före. Steget rapporterar `RETRYING` och kastar vidare,
   Operaton kör om det, och andra gången läser det om ärendet och tar ställning på nytt.

Det som skyddas är alltså ett enda arbetssteg — läs, ta ställning, skriv — och det är exakt vad `If-Match`
täcker.

Kraschar ett arbetssteg mitt i finns det ingenting att städa upp, eftersom ingenting har låsts. Och som
sagt: inga befintliga endpoints ändrar beteende (§5.7).

**Priset** är att arbete ibland görs om. Det förutsätter att ett arbetssteg tål att köras två gånger — men
det kravet finns redan, eftersom Operaton kör om steg som fallerat (§9.4). Steg som gör något utåt, som att
skicka ett brev, bör lägga den delen sist så att en omkörning inte skickar två.

### 6.3 Arbetssteg som bara läser

Ett arbetssteg som bara läser ärendet och sedan gör något utanför SM — skickar ett brev, anropar någon
annan — har ingenting att krocka på. Det skriver ju aldrig tillbaka, så det finns inget `If-Match` att
skicka med.

Lösningen kostar ett fält. I rapporten (§5.1) finns `errandVersion`, alltså den version steget läste. SM
jämför den med `errand.version` och svarar **`412`** om ärendet hunnit ändras, utan att skriva vare sig
tillstånd eller aktiviteter. För steget är det ett `412` som alla andra: rapportera `RETRYING`, kasta, låt
Operaton köra om.

Eftersom varje arbetssteg ändå måste lämna en rapport (§5.5) tillkommer inget nytt anropsmönster. Fältet är
frivilligt — utelämnas det görs ingen kontroll alls, vilket är rätt för steg som varken läser eller skriver
ärendet.

### 6.4 Parallella grenar löser vi i modellen, inte i koden

Operaton kan ha två arbetssteg igång samtidigt i samma processinstans, om modellen har en parallell
gateway. Skriver båda till ärendet får det ena `412` och kör om — och slår då i sin tur ut det andra. De
två kommer aldrig i mål.

**Därför blir det en modelleringsregel:** BPMN-modellerna får inte ha parallella grenar där mer än en gren
ändrar ärendet. I en myndighetsprocess är det ändå tveksamt att ändra samma ärende på två håll samtidigt.

**Men regeln ska synas, inte bara antas.** Rapporterar två olika `externalTaskId` in `RUNNING` mot samma
instans utan att någon av dem hunnit bli klar däremellan, skriver SM en rad i aktivitetsloggen med
`severity = WARN` och texten *"concurrent external tasks detected"* och räknar upp
`process.concurrent_task_detected`. Båda rapporterna tas emot ändå — att avvisa den ena hade tystat just
den post som ska avslöja att modellen bryter mot regeln.

### 6.5 Så hindrar vi att tjänsterna väcker varandra i evighet

Risken är enkel att beskriva: processen ändrar ärendet, ändringen blir en händelse, händelsen väcker
processen, som ändrar ärendet igen. Tre lager håller emot, och de fångar olika saker.

**Lager 1 — ville anroparen väcka processen?** Bär skrivningen headern `X-Trigger-Process: false` skrivs
ingen outbox-rad. Processens egna ändringar väcker med andra ord inte processen — inte för att SM känner
igen pw, utan för att pw säger ifrån. Filtret sitter vid **publiceringen**, inte vid leveransen: raden ska
aldrig skrivas, för annars räknar nödbromsen i lager 3 fel.

Headern läses in i en ThreadLocal av ett `OncePerRequestFilter` och dokumenteras i OpenAPI-specen från
`OpenApiConfig`. Det är samma mönster som `X-Request-Group-Id` redan använder i kodbasen, så mekaniken är
byggd och beprövad — det som tillkommer är en konstant, en ThreadLocal, ett filter och en rad i specen.

|            Headerns värde            |                                      Utfall                                      |
|--------------------------------------|----------------------------------------------------------------------------------|
| Headern saknas                       | Raden skrivs. Normalfallet, och det som gäller för schemalagda jobb utan request |
| `false`, versaloberoende och trimmad | Raden skrivs inte                                                                |
| Vad som helst annat, skräp inräknat  | Raden skrivs                                                                     |

**Riktningen på tveksamma fall är vald med flit.** En rad för mycket blir en onödig väckning som lager 2
och 3 fångar och som syns i mätvärdena. En rad för lite blir en process som står och väntar för alltid utan
att någon märker det. Därför betyder allt utom exakt `false` *väck processen*.

**Headern hedras inte för AD-identiteter.** En handläggares skrivning väcker alltid processen, hur klienten
än sätter headern. Villkoret är ett enda (`ServiceUtil.getAdUser() == null`) och det stänger det enda hål
en fritt satt header annars öppnar: att någon annans integration råkar tysta äkta ärendeändringar.
Maskin-till-maskin-anropen — pw och kommande processmotorer — är just de som saknar AD-konto.

**Lager 2 — vad hände?** Bara de händelsetyper som står i `PROCESS_TRIGGER` går vidare. Det lagret bryr sig
inte om vem som skrev, bara om vad som ändrades, och kompletterar därför lager 1.

**Lager 3 — nödbromsen.** Den bryr sig varken om vem eller vad, utan bara om takten: räkna raderna med
`errand_id = ? and delivered_at is not null and created > now() - fönstret`. Går det över tröskeln
(20 stycken på 10 minuter) skrivs ingen rad, en felpost hamnar i aktivitetsloggen och hälsoindikatorn slår
om till unhealthy.

**Alla tre lagren gäller härledda händelser — kommandon passerar dem.** Handläggarens signal (§5.9) och
manuella start (§5.10) är inte något som hänt med ärendet, utan begäranden riktade rakt till processen.
Lager 1 släpper dem redan i dag eftersom headern inte hedras för AD-identiteter, och eftersom
kommandoendpointerna kräver ett AD-konto är det garanterat i stället för en tillfällighet. Lager 2 och 3
undantar dem uttryckligen: ett kommando är ingen ärendeändring, och en människa som trycker på en knapp är
ingen loop.

Undantaget för **lager 3** är det som är lätt att missa och dyrast att glömma. Ett ärende med livlig trafik
under intaget kan trippa bromsen, och eftersom bromsen ligger före triggerfiltret i §2.2 skulle
startkommandot då kastas medan endpointen svarar `202`. Handläggaren trycker, aktivitetsloggen säger
"startad manuellt", och ingen process startar — den tystaste felvägen i hela designen, och den slår till på
just de ärenden som har mest att göra. Bromsen ska mäta takten mellan tjänsterna, inte hindra en människa
från att komma igång.

**Att bara levererade rader räknas är inte en detalj.** Räknades även de oskickade skulle ett
leveransavbrott trippa bromsen av sig självt: raderna hopar sig därför att ingenting går fram, bromsen
läser hopen som en loop och börjar kasta nya händelser. Ett avbrott som bara kostade tid hade då blivit
permanent händelseförlust — och det i precis det läge då ingenting alls levererades. Bromsen mäter hur
fort vi *levererar* till processen, aldrig hur många rader som väntar. Det är också därför `idx_peo_guard`
bär `delivered_at`.

> Räkningen förutsätter att outbox-rader **markeras som levererade i stället för att raderas**
> (`delivered_at`). Städade vi bort dem direkt hade en snabb loop aldrig lämnat mer än en rad efter sig,
> och bromsen vore verkningslös precis när den behövs.
>
> Det är den **enda** punkt där process-outboxen medvetet avviker från `notification_dispatch`, som raderar
> sina rader så fort de gått igenom (§1.2). Avvikelsen finns för nödbromsens skull och ska inte städas bort
> nästa gång någon förenklar outboxarna.

Vad som *inte* fungerar som loopskydd: att jämföra versioner. Versionen stiger ju för varje varv.

**Nödbromsens felpost skrivs en gång per ärende och fönster, inte en gång per kastad händelse.** En loop
som producerar hundratals händelser skulle annars lägga hundratals ERROR-rader i aktivitetsloggen, och
posterna dedupas inte av `uq_epa_idempotency` eftersom både `errand_process_id` och `external_task_id` är
NULL för dem och NULL är distinkt i unika index (§3.1). Felet som ska rapporteras skulle alltså dränka
loggen det rapporteras i.

**Och en ärlighet om lager 1:** det bygger på en header som avsändaren själv sätter. Det gjorde det förr
också — `X-Sent-By` var precis lika fritt satt — skillnaden är att det nu är uttalat i stället för dolt
bakom en identitetsjämförelse. En pw-tjänst som glömmer headern loopar tills lager 2 eller 3 fångar den.
Motmedlen är att en `RequestInterceptor` sätter den på **alla** utgående skrivningar (P3), och att
`process_event.suppressed{reason=OPT_OUT}` ska visa värden skilda från noll i drift. Står den på noll
skriver processen antingen ingenting alls, eller så är headern fel.

**Vad SM inte längre behöver veta:** vad pw-tjänsten heter, för att kunna hålla loopen borta.
`PROCESS_CONSUMER` finns kvar, men bara till det den faktiskt behövs för — att peka ut vart raden ska
levereras, att säga att namespacet över huvud taget har en process, och att avgöra vem som får stämpla ett
beslut som `AUTOMATIC` (§7.5). Ingen av dem har den tysta felvägen som namnjämförelsen hade: pekar
`PROCESS_CONSUMER` på fel tjänst uteblir leveransen, och det syns direkt i stället för att loopen börjar
snurra. En ny pw-tjänst behöver därmed registreras som leveransadress, men aldrig kännas igen.

## 7. Konfiguration, processval och beslut

### 7.1 Vad som konfigureras per namespace

Två nycklar i den befintliga `namespace_config` styr det som gäller hela namespacet: vem som kör processen
och vilka händelser som är värda att skicka vidare. Vad som gäller per ärendetyp — vilken process, och om
den startar av sig själv — sitter i stället på etiketten (§7.3, §7.7).

|       Nyckel       |  Typ   | Antal |                     Värde                     |
|--------------------|--------|-------|-----------------------------------------------|
| `PROCESS_CONSUMER` | STRING | 1     | `pw-alkt`                                     |
| `PROCESS_TRIGGER`  | STRING | N     | `ERRAND`, `MESSAGE`, `ATTACHMENT`, `DECISION` |

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

**`ERRAND` måste vara med.** Utan den startar aldrig ett ärende som får sin etikett först i ett andra
anrop. Men triggern räcker inte ensam — pw måste också starta processen för att `processKey` finns med,
inte för att händelsen råkar vara ett `CREATE`. Annars faller samma fall bort på mottagarsidan i stället.
Se §9.3.

**`DECISION` är lika obligatorisk.** Beslutet skrivs till `.../errands/{errandId}/decision` (§7.5) och
processen väntar på det. Saknas triggern publiceras ingen outbox-rad för den skrivningen, processen får
aldrig veta att beslutet är fattat, och instansen står kvar i `WAITING` för alltid.

**Kommandon står utanför listan.** `PROCESS_TRIGGER` säger vilka *ärendeändringar* som är värda att berätta
om för processen. Handläggarens signal (§5.9) och manuella start (§5.10) är inga ärendeändringar utan
kommandon riktade rakt till processen, och de publiceras alltid. Det är därför `SIGNAL` inte längre står i
listan: en knapp som ser ut att fungera men inte gör något är precis den tysta felväg konfigurationen inte
ska kunna orsaka (§7.7).

**Startläget konfigureras inte här utan på etiketten.** `PROCESS_CONSUMER` säger att namespacet kör
processer; `processStartMode` säger om de startar av sig själva. Ansökan och tillsyn ligger i samma
namespace och vill ha olika svar, och därför sitter valet per etikett (§7.7).

#### Processmotor och åtkomstkontroll utesluter varandra

**Ett namespace får inte ha både `PROCESS_CONSUMER` och aktiv `access_control`.** Skrivningen avvisas med
`400`, åt båda hållen: att sätta konsumenten på ett namespace med åtkomstkontroll, och att slå på
åtkomstkontroll för ett namespace som har en konsument.

Spärren är ingen policy utan en inkodad teknisk begränsning. AccessMapper svarar bara på AD-konton (§1.8),
och en processmotor har inget. Utan spärren blir följden att pw får `401` på allt — och det upptäcks inte
som ett behörighetsfel någonstans, utan som ärenden som slutar röra sig. Felmeddelandet ska därför säga
*varför*, inte bara *att*: en processkonsument kan inte beviljas åtkomst av AccessMapper, eftersom den inte
är ett AD-konto.

Den dag AccessMapper kan bevilja åtkomst till maskinidentiteter är spärren det enda som behöver lyftas.
Tills dess är den skillnaden mellan ett högljutt konfigurationsfel och en tyst driftstörning.

### 7.2 `application.yml`

```yaml
spring.security.oauth2.client:
  registration:
    pw-alkt: { authorization-grant-type: client_credentials, provider: pw-alkt }
  provider:
    pw-alkt: { token-uri: "${...}" }
integration:
  # Kort read-timeout med flit: anropet halls inne i leveransens transaktion (8.3), och pw
  # svarar 202 sa fort handelsen tagits emot. Se 7.6.
  pw-alkt: { url: "${...}", connect-timeout: 5, read-timeout: 10 }
process-engine:
  loop-guard: { max-events-per-errand: 20, window: PT10M }
  consumers: [pw-alkt]                      # registret; namnet ar Feign-malet
scheduler:                                  # nyckelnamnen foljer notification-dispatch
  process-event:
    name: process_event_relay
    cron: "0 * * * * *"
    shedlock-lock-at-most-for: PT2M
    maximum-execution-time: PT1M
    max-age: P30D                           # sista utvagen for en rad som aldrig gar igenom, 8.3
    unhealthy-after: PT15M                  # aldern pa aldsta oskickade raden, 8.3
    batch-size: 200                         # tak per konsument och korning, 7.6
  process-cleanup:
    name: process_event_cleanup
    cron: "0 30 2 * * *"
    shedlock-lock-at-most-for: PT10M
    maximum-execution-time: PT5M
```

`consumers` är **registret över kända processkonsumenter**, och det är också uppslagningstabellen relayet
använder för att hitta rätt klient (§7.6). Namnet är adressen:
det är samma sträng som Feign-målet under `integration`, som OAuth2-registreringen och som
`PROCESS_CONSUMER` i `namespace_config` pekar ut. Loop-skyddet läser det inte (§6.5). Registret finns
för att en felstavad `PROCESS_CONSUMER` ska avvisas vid skrivning i stället för att tyst sluta fungera —
utan det går processkonsumenter inte att skilja från övriga poster under `integration`.

I `application-it.yml` sätts samtliga cron till `"-"`.

### 7.3 Så vet SM vilken process ett ärende hör till

Svaret ligger i attributet `processKey` på etiketten. Vi tittar bara på ärendets egna etiketter och går
alltså **inte** uppåt eller nedåt i etikettträdet.

```sql
insert into metadata_label_attribute (metadata_label_id, `key`, `value`) values
  ('9c1a...', 'processKey',       'alkt-ansokan'),
  ('9c1a...', 'processStartMode', 'AUTOMATIC'),
  ('4f8b...', 'processKey',       'alkt-tillsyn'),
  ('4f8b...', 'processStartMode', 'MANUAL');
```

Attributet `processStartMode` avgör om SM startar processen åt handläggaren eller om någon ska trycka på en
knapp. Det läses ur **samma etikett** som gav nyckeln, och beskrivs i §7.7.

|     Utfall      |                                                                 Resultat                                                                 |
|-----------------|------------------------------------------------------------------------------------------------------------------------------------------|
| Exakt en nyckel | Den processen startas                                                                                                                    |
| Noll            | Ingen process. Inte ett fel                                                                                                              |
| Två eller fler  | **Ingen automatisk start**, ERROR-aktivitet som namnger båda. En manuell start löser upp tvetydigheten genom att peka ut nyckeln (§5.10) |

Etiketter som är märkta `deprecated` räknas inte. **SM kontrollerar inte att nyckeln finns på riktigt** — det är bara pw som vet vilka processer som är driftsatta, och en nyckel som inte finns fångas som `422` (§5.4).

### 7.4 En process per ärende — och hur den regeln hålls

Fyra regler tillsammans:

1. En etikettändring som skulle peka ut en annan `processKey` avvisas med `400` så snart ärendet har en processrad — även om den processen är avslutad.
2. Högst en **levande** instans per ärende. Den regeln bär databasen själv via `active_marker` (§4.1).
3. Alla instanser på samma ärende har samma `process_key`. Den kontrollen får tjänstelagret göra under radlås; den går inte att uttrycka i databasen.
4. **När en instans blivit `COMPLETED` är ärendets processliv slut.** Ingen ny instans får startas — nästa process är ett nytt ärende (beslut 6). En `FAILED` instans stoppar däremot ingenting; att försöka igen efter en misslyckad start är återhämtning.

Regel 4 går inte heller att lägga i databasen, eftersom `active_marker` är NULL för både `COMPLETED` och `FAILED` och alltså inte skiljer dem åt. Kontrollen ligger därför i `POST .../processes` (§5.1), i `POST .../processes/start` (§5.10) och i startlovet som publiceraren räknar ut (§7.7) — tre ställen som ställer samma fråga, eftersom starten kan komma från tre håll. Att den ligger i SM och inte bara i pw är medvetet: pw frågar Operaton om vad som kör just nu, och där syns inte avslutade processer alls (§9.3).

Krockar med de unika nycklarna **måste översättas till begripliga svar** och aldrig bubbla upp som `500`. De två betyder dessutom olika saker och ska inte behandlas lika:

|          Constraint           |                             Betydelse                             |                 Utfall                 |
|-------------------------------|-------------------------------------------------------------------|----------------------------------------|
| `uq_ep_process_instance_id`   | Någon registrerade **samma** instans först — kapplöpningen i §5.1 | Läs om raden, returnera den. Inget fel |
| `uq_ep_one_active_per_errand` | En **annan** levande instans blockerar                            | `409` med `detail` som pekar ut den    |

Och eftersom en och samma insert kan träffa båda, avgörs svaret av en uppslagning på `process_instance_id` — inte av vilken nyckel som råkade slå till först (§5.1).

---

### 7.5 Beslutet

**Ett ärende, en processinstans, ett beslut.** Ska ett nytt beslut fattas skapas ett nytt ärende, kopplat
till det ursprungliga. Kopplingen finns redan: `POST /errands` tar `referredFrom` och `ErrandService.createErrand`
skapar relationen via `RelationClient` — samma väg handover använder.

Det är den yttersta av tre regler som säger ungefär samma sak, fast på olika nivåer:

|      Nivå      |            Invariant             |            Upprätthålls av            |
|----------------|----------------------------------|---------------------------------------|
| Processinstans | Högst en levande per ärende      | `uq_ep_one_active_per_errand`         |
| Processliv     | En `COMPLETED` startas aldrig om | `hasCompletedProcess` i `POST` (§7.4) |
| Beslut         | Ett per ärende                   | `uq_ed_errand_id`                     |

#### Var beslutet lagras

I en **egen ärendescopad tabell, `errand_decision`** (§3.1), med en typad modell `Decision` (§5.3) och en
egen endpoint:

```
GET    /{municipalityId}/{namespace}/errands/{errandId}/decision   -> 200 Decision | 404
PUT    /{municipalityId}/{namespace}/errands/{errandId}/decision   -> 200 | 201, If-Match
DELETE /{municipalityId}/{namespace}/errands/{errandId}/decision   -> 204
```

```json
{ "outcome": "APPROVAL",
  "method": "MANUAL",
  "decidedBy": "anna.andersson",
  "decidedAt": "2026-09-14T10:12:00+02:00",
  "legalBasis": "9 kap. 30 § PBL",
  "delegationReference": "3.2.1",
  "justification": "...",
  "appealable": true }
```

**Fasta fält, inte ett fritt dokument.** Ett beslut har en form som följer av förvaltningslagen, och den är
sig lik oavsett om det gäller bygglov, försörjningsstöd eller tillsyn: utfall, vem som fattade det, när,
med stöd av vilket lagrum eller vilken delegationspunkt, och varför. Den formen hör hemma i modellen — där
kontrolleras den när den kommer in, den syns i `openapi.yaml` och den går att söka i. I ett fritt
JSON-dokument får varje läsare tolka den på egen hand i stället.

Två saker är medvetet utelämnade: **överklagandetiden**, eftersom klockan börjar gå vid delgivningen och
den håller SM inte reda på, och en fri lista med egna nyckel/värde-par, eftersom den snabbt blir en
soptunna och återinför exakt det otypade vi just tagit bort.

Fyra andra lösningar övervägdes och valdes bort:

- **`json_parameter` med registrerat schema**, som var det tidigare valet i den här designen. Den kräver
  att ett JSON-schema registreras och förvaltas per verksamhet, och dessutom att
  `ErrandJsonParameterService.updateJsonParameter` börjar skapa event och revision — en beteendeändring i
  en tjänst som redan används av andra (§1.6). Otypad lagring för ärendets mest formbundna dokument.
- **Kolumner eller JSON på `errand_process`.** Ett myndighetsbeslut är ärendedata, inte processmaskineri.
  Det ska gå att läsa för e-tjänst och arkiv utan att man vet något om Operaton, det finns även på ärenden
  helt utan process, och vid en omstart efter `FAILED` hade det hamnat på fel rad (§7.4).
- **Vanliga parametrar.** `parameter_values.value` rymmer 255 tecken — en motivering får inte plats.
- **Aktivitetsloggen.** Den är en logg som SM inte tolkar, den har ingen unikhet, och enligt §11 får
  `message` inte innehålla personuppgifter — vilket en beslutsmotivering nästan alltid gör.

#### Vem som får skriva beslutet

Två kan fatta beslutet, men de går in samma väg:

|               Fall               |  `method`   |          Vem skriver           |                          Behörighet                           |
|----------------------------------|-------------|--------------------------------|---------------------------------------------------------------|
| Handläggaren fattar beslutet     | `MANUAL`    | ett AD-konto                   | RW på ärendet, som vid vilken annan ärendeskrivning som helst |
| Processen fattar beslutet självt | `AUTOMATIC` | namespacets `PROCESS_CONSUMER` | `X-Sent-By` pekar ut samma tjänst som `PROCESS_CONSUMER`      |

**Regeln kontrolleras när beslutet kommer in:** `AUTOMATIC` godtas bara från namespacets
`PROCESS_CONSUMER`, `MANUAL` bara från ett AD-konto. Allt annat ger `403`. Utan den kontrollen skulle en
handläggare kunna stämpla sitt eget beslut som automatiskt, eller en process stämpla sitt som manuellt —
och det är just den skillnaden man måste kunna svara på i efterhand (förvaltningslagen 28 § och
dataskyddsförordningen artikel 22 om automatiserat beslutsfattande).

`processId` fyller SM i själv och tar aldrig emot den från klienten: ärendets levande processrad när
beslutet är automatiskt, annars ingenting.

#### Vad som händer när beslutet skrivs

1. **Det skapas en revision och en händelse med subtypen `DECISION`**, längs samma väg som alla andra
   ärendeskrivningar (§1.1). Beslutet hänger på `ErrandEntity` och följer därför med i revisionens
   ögonblicksbild (§3.2) — till skillnad från processtabellerna, som med flit står utanför (§1.4).
2. **`errand.version` höjs.** Ett arbetssteg som håller en äldre ETag får `412` och kör om sig (§6.2).
   Det är rätt: beslutet ändrade ju underlaget.
3. **`DECISION` måste finnas i `PROCESS_TRIGGER`** (§7.1), annars skrivs ingen outbox-rad och processen
   får aldrig veta att beslutet är fattat.
4. Är det **processen själv** som skriver beslutet stoppas outbox-raden av loop-skyddets första lager
   (pw sätter `X-Trigger-Process: false`, §6.5). Också rätt: processen behöver inte väckas av sitt
   eget beslut.

#### När beslutet låses

Så länge processen lever går beslutet att skriva om — steget som förbereder beslutet kan behöva rätta sig
självt, och en handläggare kan upptäcka ett stavfel. När ärendets process är `COMPLETED` är det låst, och
samma `hasCompletedProcess(errandId)` som hindrar att processen startas om (§7.4) ger `409` också här. En
kontroll, två användningar. `DELETE` finns för det felskrivna beslutet och lyder under samma spärr.

Ärenden **utan** process låses aldrig, helt enkelt för att det inte finns någon process som kan bli
`COMPLETED`. Där är revisionshistoriken spårbarheten. Och ska ett låst beslut ändras är vägen ett nytt
ärende, kopplat till det gamla.

#### Det här hänger på hur processen är modellerad

Väntläget som avvaktar beslutet **måste läsa om ärendet när det går in i väntan** (§9.2 punkt 1). Skrivs
beslutet medan processen är mitt i ett arbetssteg finns det ingen som lyssnar, väckningen sväljs som en
`MismatchingMessageCorrelation` och är sedan borta. Om processen då inte tar reda på hur ärendet faktiskt
ser ut när den börjar vänta, blir ärendet stående för alltid — med ett färdigt beslut liggande i databasen.
Det är det allvarligaste misstag man kan göra i den här lösningen, och ingen kod i SM kan rädda det.

---

### 7.6 Fler namespace och fler processmotorer

Designen är byggd för att ALKT ska vara det första namespacet, inte det enda. Det mesta av det som skiljer
ett namespace från ett annat är därför data: `PROCESS_CONSUMER` och `PROCESS_TRIGGER` i `namespace_config`
(§7.1), `processKey` på etiketterna (§7.3), och processmodellerna i pw. Ingenting av det kräver en release.

**Ett namespace har exakt en processkonsument.** `namespace_config_value` tillåter tekniskt flera värden
per nyckel (§1.5), men `PROCESS_CONSUMER` läses som ett. Ett namespace är en verksamhet, och en verksamhet
har en processmotor. Behövs två är det två namespace.

#### Vad en ny pw-tjänst kostar

|                          Steg                           |            Var             |      Release?      |
|---------------------------------------------------------|----------------------------|--------------------|
| `PROCESS_CONSUMER` och `PROCESS_TRIGGER` för namespacet | `namespace_config`         | Nej                |
| `processKey` på etiketterna                             | `metadata_label_attribute` | Nej                |
| OAuth2-registrering och provider                        | `application.yml`          | **Ja**             |
| Feign-mål under `integration`                           | `application.yml`          | **Ja**             |
| Namnet i `process-engine.consumers`                     | `application.yml`          | **Ja**             |
| Eget API i WSO2                                         | WSO2                       | **Ja**, utanför SM |

Att halva listan kräver en release är inte ett fel, men det ska sägas rakt ut: **en ny processkonsument är
en driftsättning av SM, inte en konfigurationsändring.** Det som är konfiguration är att *koppla ett
namespace* till en konsument som redan finns.

#### Hur raden hittar rätt tjänst

`process_service` sätts på outbox-raden vid publicering, ur namespacets `PROCESS_CONSUMER` (§2.2 steg 6).
Relayet läser alltså aldrig om konfigurationen — raden går dit den var adresserad när den skrevs. Ändras
`PROCESS_CONSUMER` medan rader ligger oskickade går de till den gamla tjänsten, vilket är rätt: de
adresserades dit, och den nya konsumenten känner inte till dem.

Namnet blir en klient genom en uppslagningstabell byggd vid uppstart ur `process-engine.consumers`
(§7.2) — inte en injicerad `ProcessEngineClient`, för det finns fler än en:

```java
/** Namn -> klient. Byggd vid uppstart; ett namn utan klient ar ett konfigurationsfel som ska smalla da. */
private final Map<String, ProcessEngineClient> clientsByConsumer;
```

Varje klient bär sin egen url, sina egna timeouts och sin egen `clientRegistrationId` för OAuth2. Ett namn
i `namespace_config` som inte finns i registret avvisas redan vid skrivning med `400` (§7.2), så det fallet
ska aldrig nå relayet — men uppslagningen ska ändå smälla högt i stället för att tyst hoppa över raden.

#### Att en konsument är nere får inte stoppa de andra

Det här är den punkt där flera konsumenter skiljer sig mest från en. Leveransen håller ett HTTP-anrop inne
i transaktionen (§8.3), hämtningen sorterar deterministiskt, och jobbet har en `maximum-execution-time`.
Utan motmedel räcker det med att en pw-tjänst timeoutar för att körningen ska ta slut innan den hunnit
fram till de andra namespacens rader — och eftersom ordningen är densamma nästa körning hamnar samma
stockade rader först igen. Fyra saker håller isär dem:

1. **Hämtningen är per konsument och begränsad.** `batch-size` (§7.2) är ett tak per konsument och körning.
   Det är också svaret på fallgropen i §1.2: `findProcessable` saknar `LIMIT`, och den bristen får inte
   ärvas hit.
2. **Konsumenterna levereras oberoende av varandra**, med `idx_peo_consumer` som stöd. En konsument som
   inte svarar förbrukar sin egen andel av körningen, inte hela.
3. **Circuit breaker per konsument.** `@CircuitBreaker` används redan på ett fyrtiotal ställen i kodbasen.
   En konsument som är nere ska sluta anropas en stund i stället för att äta upp timeout efter timeout.
4. **Kort read-timeout för relayet** (§7.2). pw svarar `202` så snart händelsen tagits emot, så anropet är
   kort i alla normala fall — och ett långt anrop håller en databastransaktion öppen.

Ordningen inom ett ärende hålls fortfarande, eftersom grupperingen sker per ärende inne i varje konsuments
andel (§8.3).

#### Det som inte är delat, men borde diskuteras

pw-sidan i §9 är skriven som pw-alkt: `AbstractTaskWorker`, `ProcessStateReport`, `FailureHandler`,
SM-klienten med sina tre headers och `If-Match`-hanteringen, och de sju modelleringskraven i §9.2. Allt det
är kontrakt mot SM, inte ALKT-logik. Tjänst nummer två börjar därför med att kopiera det, och femte gången
har kopiorna glidit isär.

Det är inte löst här, och ska inte lösas i förbifarten. Men beslutet — delad starter eller medveten
kopiering — bör fattas innan den andra pw-tjänsten byggs, inte efter. Kraven i §9.2 är den del som gör mest
skada om de glöms bort, eftersom de inte syns i något API.

---

### 7.7 Automatisk och manuell start

Processen ska kunna startas på två sätt, och skillnaden mellan dem är bara vem som ger lovet.

|                      Läge                       |                                             Vad som händer                                              |
|-------------------------------------------------|---------------------------------------------------------------------------------------------------------|
| `processStartMode: AUTOMATIC`, eller inget alls | `POST /errands` ⇒ `CREATE`-händelse ⇒ SM sätter `startAllowed = 1` ⇒ pw startar processen               |
| `processStartMode: MANUAL`                      | Ingenting startar av sig självt. Handläggaren trycker "Starta handläggning", och kommandot sätter lovet |

**Läget styr inte om knappen finns — det styr om SM trycker på den åt handläggaren.** Villkoret för att en
start över huvud taget är möjlig är detsamma i båda lägena: en `processKey` går att lösa ut, ingen levande
instans finns, och inget processliv är avslutat (§7.4). Läget avgör bara om SM själv sätter lovet när en
ärendehändelse passerar publiceringen.

#### Attributet

Startläget är ett andra attribut på samma etikett som bär `processKey` (§7.3):

```sql
insert into metadata_label_attribute (metadata_label_id, `key`, `value`) values
  ('4f8b...', 'processKey',       'alkt-tillsyn'),
  ('4f8b...', 'processStartMode', 'MANUAL');
```

|    Värde    |                                  Betydelse                                   |
|-------------|------------------------------------------------------------------------------|
| `AUTOMATIC` | SM sätter startlovet på den första ärendehändelse som kan starta processen   |
| `MANUAL`    | Bara kommandot i §5.10 sätter lovet                                          |
| Saknas      | Som `AUTOMATIC`. Den som inte rör attributet märker ingen skillnad mot i dag |

**Två kontroller vid skrivning av etiketten, båda `400`:** värdet måste vara exakt `AUTOMATIC` eller
`MANUAL`, och `processStartMode` utan `processKey` på samma etikett avvisas eftersom attributet är
meningslöst ensamt.

Skälet till att kontrollerna ligger vid skrivningen och inte vid läsningen är att attributnycklar **inte**
är whitelistade (§1.6). En etikett med `processstartmode` — litet s — skulle annars tyst betyda
`AUTOMATIC`, och båda tänkbara fallbacks vid läsning är sämre än att avvisa: faller vi tillbaka på `MANUAL`
slutar processer startas utan att någon får veta det, och faller vi tillbaka på `AUTOMATIC` startar
processer som inte skulle ha startat. Avvisar vi vid skrivning uppstår tvetydigheten aldrig. Det är samma
val som spärren mellan `PROCESS_CONSUMER` och `access_control` i §7.1: hellre ett högljutt
konfigurationsfel än en tyst driftstörning.

**Läget läses ur samma etikett som gav nyckeln.** Det är inte en detalj: har ärendet två etiketter med
varsin `processKey` skulle två skilda uppslagningar kunna hämta nyckeln från den ena och läget från den
andra. `ProcessKeySelector` (§7.3, T5) ska därför lämna tillbaka paret — nyckel och läge tillsammans — och
aldrig läget för sig.

#### Varför på etiketten och ingen annanstans

Era två processer vill ha olika svar. En ansökan startar när medborgaren ansöker; en tillsyn initieras av
myndigheten och ska inte dra igång för att någon råkar registrera ett ärende. Båda ligger i ALKT, så en
inställning per namespace kan inte uttrycka skillnaden.

Alternativet — en manuell grind först i processmodellen, så att valet stannar i BPMN som i §5.9 — ser
frestande ut, men varje ärende skulle då få en levande Operaton-instans direkt: också felregistrerade och
uppenbart felaktiga. Värre är att den grinden är svår att backa ur. Att överge den avslutar instansen, och
en `COMPLETED` instans betyder att ärendets processliv är slut (§7.4 regel 4) — ett ärende som *inte* skulle
ha startats hade därmed aldrig kunnat startas senare. Det är fel sorts oåterkallelighet.

**Följden att acceptera: det finns ingen global nödbroms.** Ska alla automatiska starter i ALKT stoppas görs
det genom att sätta `MANUAL` på de etiketter som bär en nyckel — två rader i dag. Att i stället plocka bort
`PROCESS_CONSUMER` är ingen ersättning: då publiceras inga händelser alls, och processer som redan kör blir
blinda.

#### Hur lovet räknas ut

Steg 6 i publiceringen (§2.2):

```
startAllowed = kommando (subtyp PROCESS)
            || ( ingen levande instans for arendet
              && ingen COMPLETED instans for arendet          // 7.4 regel 4
              && etikettens processStartMode == AUTOMATIC )
```

Det kostar inga extra frågor. Steg 5 slår redan upp ärendets processrad för att kunna läsa `process_key`
från instansen i första hand. Låt den hämtningen ta ärendets **rader** och inte bara den levande, så svarar
samma fråga på båda kontrollerna. Läget kommer ur den etikettuppslagning som ändå gjordes för att lösa ut
nyckeln.

**Lovet är optimistiskt, registreringen är auktoritativ.** Det räknas ut vid publiceringen och används vid
leveransen, och däremellan kan tillståndet ha hunnit ändras — en process kan ha gått i mål under tiden. Då
startar pw något den inte borde, och `POST .../processes` svarar `409` varpå pw avbryter instansen den just
startat (§5.1, §9.3). Skyddsnätet fanns redan; det som är nytt är att det numera sällan behöver användas.

#### Vad läget inte styr

- **Stegningen.** När instansen väl finns är det modellen som avgör vad som går vidare av sig självt och vad
  som väntar på en människa (§5.9). Beslut 28 står kvar oförändrat, och gränsen mot det här avsnittet är
  skarp: läget styr instansens födelse, modellen styr dess steg. Modellen kan omöjligt äga startbeslutet —
  före starten finns ingen instans att fråga.
- **Knappen.** Kommandot i §5.10 läser inte `processStartMode`. Därför fungerar det även i automatiskt läge,
  och det är den vägen man startar om efter en misslyckad start.
- **Om händelser når en process som redan kör.** `startAllowed` grindar bara startgrenen i §9.3;
  korrelationen är orörd. I manuellt läge fortsätter ärendets händelser alltså att publiceras och levereras
  hela tiden — pw loggar och svarar `202` tills någon trycker.

#### En automatisk start sker inte bara vid `CREATE`

Beslut 17 står fast: start villkoras av att en `processKey` finns, inte av händelsetypen. Ett ärende som
skapas utan etikett och får den i ett andra anrop startar när etiketten kommer, inte aldrig.

Följden är värd att säga rakt ut: **ett gammalt ärende som får rätt etikett — eller som redan har den och
blir ändrat — startar en process.** Vid driftsättning kan det bli många på en gång.

Motmedlet följer av att läget sitter på etiketten. **Sätt `MANUAL` från början**, låt handläggarna starta de
första ärendena för hand och se att kedjan beter sig, och byt sedan till `AUTOMATIC` genom att ändra ett
attribut. Ingen kodändring, ingen driftsättning, och vägen tillbaka är lika kort om något ser fel ut.

---

## 8. Drift och förvaltning

### 8.1 Mätvärden som måste finnas

Utan dem går varken loop-skyddet eller samtidigheten att följa i drift — man märker att något är fel
först när någon hör av sig.

|                          Mätvärde                           |                                                                                                                     Varför                                                                                                                      |
|-------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `process_event.suppressed{reason=OPT_OUT\|TRIGGER\|GUARD}`  | Ett tyst loop-skydd som slutar fungera märks annars först när loopen är där. `OPT_OUT` ska vara **skild från noll** — står den på noll sätter pw inte `X-Trigger-Process`. Tagga även med anropande identitet                                   |
| `process_event.published`, `.delivered`, `.delivery_failed` | Leveranshälsa                                                                                                                                                                                                                                   |
| `process_event.oldest_undelivered_age`                      | Åldern på den äldsta oskickade raden. Den är larmklockan nu när det inte finns någon dead letter-flagga att räkna på: stiger den stadigt är det något som aldrig går igenom                                                                     |
| `process_event.aged_out`                                    | Rader som släppts oskickade vid `max-age`. **Ska vara noll.** Varje sådan rad betyder att en process aldrig fick veta något                                                                                                                     |
| `process_event.publish_failed`                              | Publicering som inte kunde rullas tillbaka (ingen aktiv transaktion, §2.2). Ska vara noll                                                                                                                                                       |
| `process_event.direct_run_rejected`                         | Trådpoolen är full, så direktkörningen hoppades över och leveransen får vänta på cronjobbet (§2.3)                                                                                                                                              |
| `process.errand_conflict`                                   | **Viktigaste driftindikatorn.** Hur ofta process och handläggare krockar (`412`). Stiger den arbetar processen på ärenden som redigeras samtidigt, och arbete görs om i onödan                                                                  |
| `process.concurrent_task_detected`                          | Brott mot modelleringsregeln i §6.4                                                                                                                                                                                                             |
| `process.start_failed`                                      | Feltaggade etiketter                                                                                                                                                                                                                            |
| `process.start_requested{method=MANUAL\|AUTOMATIC}`         | Hur processer faktiskt startas. Ligger `MANUAL` på noll i ett namespace vars etiketter säger `MANUAL` betyder det att knappen inte syns eller inte fungerar (§7.7)                                                                              |
| `process.start_rejected{reason=...}`                        | Avvisade startkommandon, taggat med `status`-värdet eller `NOT_AD`. Återkommande `NOT_AD` betyder att en integration försöker starta processer maskinellt (§5.10); återkommande `LIVE_INSTANCE` att gränssnittet visar en knapp som inte gäller |
| `decision.written{method=MANUAL\|AUTOMATIC}`                | Hur många beslut som fattas av maskin respektive människa. Krävs för att kunna svara på frågan i efterhand (§7.5), och en oväntad rörelse i `AUTOMATIC` är den tidigaste signalen på att en process fattar beslut den inte borde                |
| `process.signal_sent{signal=...}`                           | Hur ofta handläggaren stegar processen manuellt, och vid vilken grind. Står en grind still trots att ärenden köar där, är det ofta gränssnittet som inte visar knappen                                                                          |
| `process.signal_rejected{reason=UNKNOWN\|TERMINAL}`         | Signaler som avvisats med `409`. En återkommande `UNKNOWN` betyder att gränssnittet visar en knapp processen inte längre väntar på                                                                                                              |
| `decision.rejected{reason=METHOD\|LOCKED}`                  | Avvisade beslutsskrivningar: fel `method` för identiteten (`403`) eller låst av `COMPLETED` process (`409`)                                                                                                                                     |

Logga alltid `eventId`, `errandId`, `processInstanceId` och `X-Request-Group-Id` — dubbelleveranser blir då spårbara i efterhand. **Logga aldrig `justification`** — den innehåller personuppgifter (§11).

### 8.2 Vanliga frågor i drift — och svaren

|                                 Fråga                                 |                                                                                                                                                                                                                                                  Svar                                                                                                                                                                                                                                                   |
|-----------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Varför startade ingen process för ärendet?                            | Börja i `startable.status` från `GET .../processes` — den svarar direkt i de vanliga fallen (§5.10). Står den på `AVAILABLE` väntar ärendet bara på en knapptryckning, alltså `processStartMode: MANUAL` på etiketten (§7.7). Annars: läs `GET .../process-activities`, där tvetydig etikett och nödbroms ligger som `CONFIG`/`ERROR`-poster, och kontrollera etikettens `processKey`-attribut via `GET /{municipalityId}/{namespace}/metadata/labels` samt att `PROCESS_CONSUMER` finns för namespacet |
| Varför syns ingen knapp för att starta handläggningen?                | `startable.status` säger vilket hinder det är. `PROCESS_COMPLETED` betyder att ärendets processliv är slut — nästa process är ett nytt ärende (§7.4)                                                                                                                                                                                                                                                                                                                                                    |
| Varför går processen inte vidare fast handläggaren tryckt på knappen? | Kontrollera att signalen står bland `errand.process.awaitingSignals` och att väntläget i modellen lyssnar på just det namnet. Signaler filtreras inte av `PROCESS_TRIGGER` (§7.7), så där finns ingenting att felkonfigurera. Se `process.signal_rejected`                                                                                                                                                                                                                                              |
| Varför syns ingen knapp för att gå vidare?                            | `errand.process.awaitingSignals` är tom. Antingen är väntläget automatiskt, eller så rapporterar pw inte in signalerna (§9.3)                                                                                                                                                                                                                                                                                                                                                                           |
| Varför avslutas inte processen fast beslutet är fattat?               | Kontrollera att `DECISION` ligger i `PROCESS_TRIGGER` (§7.1), att outbox-raden finns för beslutsskrivningen, och att väntläget läser om ärendet när det går in i väntan (§9.2 punkt 1)                                                                                                                                                                                                                                                                                                                  |
| Vem fattade beslutet på ärendet?                                      | `errand.decision.method` och `.decidedBy`. `AUTOMATIC` betyder att processen fattade det; `processId` pekar ut vilken processrad                                                                                                                                                                                                                                                                                                                                                                        |
| Varför kör processen om samma steg gång på gång?                      | Handläggaren ändrar ärendet mitt i steget ⇒ `412` (§6.2). Se `process.errand_conflict` och aktivitetsloggen                                                                                                                                                                                                                                                                                                                                                                                             |
| Varför väcks inte processen av inkommande e-post?                     | `MESSAGE` saknas i `PROCESS_TRIGGER`                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| Varför väcks processen inte av sina egna ändringar?                   | Det är meningen — lager 1 i §6.5                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| Varför står instansen kvar som `RUNNING` fast inget händer?           | Workern kraschade utan att rapportera. Operaton kör om task:en när dess eget lås löper ut; instansen uppdateras vid nästa rapport                                                                                                                                                                                                                                                                                                                                                                       |

### 8.3 Misslyckade leveranser

Mönstret är det som `V1_48__simplify_notification_dispatch` införde: **leverans och radering delar transaktion.** Går leveransen inte igenom rullas allt
tillbaka, raden ligger kvar precis som den var, och nästa körning gör om försöket. Det finns alltså ingen
retry-räknare, ingen backoff och ingen dead letter-flagga — den oskickade raden *är* kvitteringen på att
arbetet återstår.

Två följder är värda att skriva ut:

- **Leveransen måste tåla att göras om.** Rullar transaktionen tillbaka efter att pw redan tagit emot
  händelsen kommer samma händelse en gång till. pw:s event-endpoint måste därför vara idempotent — den
  korrelerar på ärendet och startar eller väcker, den räknar inte (§9.3).
- **En rad som fastnar håller sitt eget ärende, inte de andra.** Rader levereras grupperade per ärende med
  en transaktion per grupp, precis som `processDispatch` gör. Ordningen inom ärendet hålls därför även när
  något går fel, utan att andra ärenden stoppas.

**De två felen behandlas olika:**

|                                      Fel                                       |                                                                                                      Utfall                                                                                                      |
|--------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `422` från pw — permanent, till exempel ett `processKey` som inte är driftsatt | Inget nytt försök. Raden konsumeras (`delivered_at` sätts) och felet skrivs som `FAILED` + ERROR-aktivitet på ärendet (§5.4). Ett permanent fel hör hemma i ärendets historik, inte i en flagga på en outbox-rad |
| `5xx`, timeout, nätfel — tillfälligt                                           | Transaktionen rullas tillbaka och raden görs om vid nästa körning, hur många gånger som helst, tills den går igenom eller åldras ur                                                                              |

**`max-age` är sista utvägen, inte en väg ut.** Som i `NotificationDispatchWorker` släpps en rad som
passerat åldersgränsen oskickad. Skillnaden mot en notis är att det här betyder att en process aldrig fick
veta något, så gränsen sätts högt (30 dagar) och varje sådan rad **loggas som ERROR och räknas i
`process_event.aged_out`, som ska stå på noll**.

**Hälsan mäts i ålder, inte i antal.** Det finns alltid oskickade rader — varje publicering lägger en, och
den ligger kvar tills nästa körning tar den. Villkoret är därför att den **äldsta** oskickade raden
passerat `unhealthy-after` (§7.2, `PT15M` som standard), inte att det över huvud taget finns oskickade
rader. Det är samma tal som `process_event.oldest_undelivered_age` mäter. Sätts villkoret på existens i
stället står tjänsten unhealthy under normal drift, och då slutar någon titta på indikatorn — vilket är
värre än att inte ha den.

Någon redrive-endpoint behövs inte längre: en rad som inte gått igenom försöker redan igen av sig själv.
Det som behövs är att någon märker att den ligger kvar, och det är vad de två mätvärdena är till för.

### 8.4 Prova själv, lokalt

1. Skapa namespace-config med `PROCESS_CONSUMER=pw-alkt` och `PROCESS_TRIGGER=ERRAND,MESSAGE`.
2. Tagga en label med `processKey=alkt-ansokan`.
3. `POST /2281/ALKT/errands` med den labeln ⇒ rad i `process_event_outbox` inom en sekund, `delivered_at` satt när stubben svarat.
4. `GET /2281/ALKT/errands/{id}` ⇒ `process.processStatus = RUNNING`, och `ETag` i svarshuvudet.
5. `PATCH` samma ärende med den ETag:en ⇒ `200`. `PATCH` igen med **samma** ETag ⇒ `412`.
6. Låt stubben rapportera med ett `errandVersion` som ligger efter ⇒ `412` på rapporten, inget tillstånd skrivet.
7. `PUT /2281/ALKT/errands/{id}/decision` med `method: MANUAL` ⇒ revision, `DECISION`-event och en outbox-rad (kräver `DECISION` i `PROCESS_TRIGGER`). Samma skrivning med `X-Sent-By: pw-alkt; type=processEngine`, `X-Trigger-Process: false` och `method: AUTOMATIC` ⇒ ingen outbox-rad, men beslutet skrivet.
8. Låt stubben rapportera `COMPLETED` och skriv beslutet igen ⇒ `409`.

## 9. pw-alkt

### 9.1 En BPMN-fil per process

Alltså `alkt-ansokan.bpmn` och `alkt-tillsyn.bpmn` var för sig. `TenantAwareAutoDeployment.deployResources` rullar ut en driftsättning per fil, och låg båda processerna i samma fil skulle en ändring i tillsynsprocessen versionera upp ansökningsprocessen och rycka undan mattan för de instanser som redan kör.

Processens `id` i filen måste stämma med `Constants.PROCESS_KEY_*`. Och tänk på att **`ProcessWithoutDeviationIT.setup` väntar på `getDeployments(...).size() == 1`** — det villkoret måste ändras när den andra filen läggs till.

**Modellen är i dag ett skelett, och det får konsekvenser.** `alkt-ansokan.bpmn` innehåller sex tomma
subprocesser på rad — Registrera, Granska, Utreda, Beslut, Uppföljning, Avsluta — med bara ett start- och
ett slutevent i varje. Inga arbetssteg, inga väntlägen, inga meddelanden, inga gateways. Kontrollerat i
filen, inte antaget.

Startas den modellen som den ser ut nu händer följande:

```
pw startar instansen  ->  POST .../processes {RUNNING}  ->  SM: raden lever
Operaton kor rakt igenom alla sex tomma subprocesser  ->  instansen ar slut pa millisekunder
ingen external task finns  ->  ingen rapporterar COMPLETED  ->  SM star kvar pa RUNNING

nasta handelse for arendet:
  pw: findProcessInstances -> tom (instansen ar borta ur runtime)
  pw: har SM en COMPLETED instans? -> nej, SM sager RUNNING
  pw: startar en NY instans -> POST -> 409 (raden lever redan) -> pw avbryter den
  ... och sa for varje handelse, i all evighet
```

Ärendet fastnar alltså i ett läge där ingenting går framåt och ingenting går sönder synligt. Två saker
följer:

1. **Ett minimum av modellarbete hör ihop med P2.** Varje fas behöver minst ett väntläge som håller
   processen vid liv tills det fasen väntar på har hänt — annars finns det heller ingenting för
   `correlateMessage` att träffa, och hela eventkedjan saknar mottagare.
2. **Driftsätt inte modellen mot skarpa ärenden innan dess.** En avslutad process avslutar ärendets
   processliv (§7.4), så ett ärende som fått springa igenom skelettet kan aldrig få en riktig process.

### 9.2 Sju krav på hur processerna modelleras

Det här är inte råd. Håller inte modellerna sig till dem faller delar av designen.

1. **Ett väntläge måste läsa om ärendet när det går in i väntan** och avgöra om det den väntar på redan har hänt. En väckning som sväljs som `MismatchingMessageCorrelation` kan mycket väl vara äkta — den kom bara medan processen råkade befinna sig mellan två väntlägen. Att det ändå är ofarligt vilar helt på den här punkten. **Tydligast blir det med beslutet** (§7.5): skrivs det medan processen arbetar och väntläget inte läser om, står ärendet stilla för alltid med ett fattat beslut i databasen.
2. **Manuella grindar modelleras som namngivna väntlägen** (§5.9). Ska handläggaren avgöra när processen går vidare räcker det inte att villkoret är uppfyllt — väntläget ska lyssna på ett *namngivet* meddelande, till exempel `granskning-godkand`. Finns flera vägar framåt används en event-based gateway med ett catch event per alternativ, så att handläggarens val också blir processens vägval.

   **Tidsgränsen läggs som ett timer catch event i samma event-based gateway** — inte som en boundary timer på väntläget. En boundary event fäster bara på en *aktivitet* (`BoundaryEvent.attachedToRef` pekar på `Activity`), och ett intermediate catch event är ingen aktivitet. Vill man ändå ha en boundary timer får väntläget modelleras som en **receive task**, som är en aktivitet. Kontrollerat mot Operatons dokumentation: en event-based gateway får bara följas av intermediate catch events, måste ha minst två utgående flöden, och message plus timer som alternativ är det dokumenterade exemplet. Ett väntläge som ingen någonsin klickar på står annars kvar för alltid.

3. **Inga user tasks.** Det är lätt att tro att ett väntläge på en människa ska vara en user task — det är BPMN-lärobokens svar. Här är det fel: handläggaren arbetar i SM och loggar aldrig in i Operaton. En user task skulle skapa en uppgiftslista som ingen tittar i, och grinden skulle aldrig öppnas. Manuella grindar är meddelandehändelser, ingenting annat.

4. **Inga parallella grenar som ändrar ärendet** (§6.4).

5. **Inga processvariabler som minne mellan väckningar.** Tillståndet bor i ärendet, och kontrollen läser om det varje gång — annars börjar processen tro saker som inte längre är sanna. Skälet står i den kod som nu tas bort: *"Clearing process variable has to be a blocking operation. Using ExternalTaskService.setVariables() will not work without creating race conditions."* Behåll resonemanget även när metoden är borta; det är det första någon återinför nästa gång ett dubblettproblem dyker upp.

   Ett **resultatvärde** är något annat och fullt tillåtet: det som ett arbetssteg sätter när det slutförs, `complete(task, variables)`, och som nästa gateway läser. Det skrivs atomiskt med slutförandet och har ingen kapplöpning i sig. Utan det gick det inte att ha gateways över huvud taget. Skillnaden är alltså: *resultat i ett steg är i sin ordning, minne mellan väckningar är det inte.*

   Vägen dit går genom `ProcessStateReport.variables` (§5.5). Arbetssteget slutför inte task:en självt — det gör basklassen (§9.4) — så värdet måste följa med rapporten tillbaka.

6. **Steget före varje slutevent är ett arbetssteg som rapporterar `completed()`.** SM får bara veta att en process är klar genom en rapport, och rapporter kommer från arbetssteg. Slutar en gren utan att ett arbetssteg kört sist står SM:s rad kvar som `RUNNING` för alltid medan instansen är borta ur Operaton — och då kan ärendet varken gå vidare eller få en ny process.

   Kravet är **ovillkorligt**, och det är värt att säga varför: en execution listener på sluteventet vore det naturliga alternativet, men den kan inte användas här. Operaton kör som en **separat server** som pw pollar via external task-klienten, så pw:s klasser finns inte i motorn — en `camunda:executionListener` med `class` eller `delegateExpression` har ingenting att peka på. Kvar vore ett inline-skript som gör ett HTTP-anrop inifrån motorn, och det bygger vi inte. P6 stämmer av det som ändå glider isär.

7. **Inga call activities eller delade subprocesser** tills vidare. Kommande processer kan se helt annorlunda ut, och då är det lättare att ha hållit dem isär.

#### Så ser en fas ut som uppfyller kraven

Kraven ovan hänger ihop, och det är lättare att se hur i en bild än i löptext. Varje fas följer samma form:

```
start_<fas>
    |
    v
[arbetssteg]         external tasks som gor jobbet. Kors EN gang.
[arbetssteg]         pw rapporterar RUNNING, aktiviteter och resultat.
    |
    v
[kontroll] <-----------------+   external task: laser om arendet och avgor om fasen
    |                        |   ar klar. Satter ett resultatvarde nar den slutfors.
    v                        |
 <klar?> -- ja --> end_<fas> |
    |                        |
   nej                       |
    |                        |
    v                        |
<event-based gateway>        |
    |                        |
    +--> (message catch) ----+   manuell grind: "granskning-godkand"
    |                        |   automatiskt vantlage: "errandUpdated"
    |                        |
    +--> (timer catch) ------+   paminnelse eller eskalering, t.ex. PT14D
```

**Tidsgränsen är en gren i gatewayen, inte en boundary timer.** Boundary events fäster bara på
aktiviteter, och ett catch event är ingen aktivitet — se krav 2. Båda grenarna leder tillbaka till
kontrollen, som gör om sin bedömning: väcktes processen av handläggaren är fasen kanske klar, väcktes den
av timern är den det förmodligen inte, och då är det påminnelsen som är arbetet.

**Slingan tillbaka är det bärande.** Varje väckning leder till en omläsning av ärendet, aldrig till ett
antagande om att villkoret nu är uppfyllt — det är krav 1, ritat. Kommer väckningen för tidigt, eller kommer
den två gånger, gör kontrollen samma sak som förra gången och processen står kvar där den ska.

Därav den enda hårda regeln om formen: **det slingan går tillbaka till måste tåla att köras om**. Är
arbetssteget rent — läser och bedömer, utan sidoeffekter utåt — får det gärna slås ihop med kontrollen till
ett enda steg. Skickar det ett brev, skapar en post i ett annat system eller aviserar sökanden måste de
hållas isär, annars skickas brevet om varje gång någon lägger en bilaga på ärendet.

**Kontrollen hör till väntläget, inte till arbetsstegen.** En fas med fem arbetssteg i rad behöver bara en
kontroll: den som sitter före grinden. Det är där processen tar ställning, och det är dit slingan går.

**Kontrollerna får dela topic.** Villkoren skiljer sig mellan faser — *är granskningen klar?* mot *finns ett
beslut?* — men mekaniken är identisk: hämta ärendet, pröva ett predikat, returnera ett resultat. En gemensam
topic som tar villkorets namn som input-parameter räcker, med en worker i pw som slår upp predikatet. Då
finns ett ställe att ändra hämtningen och felhanteringen på, och predikaten blir små rena funktioner som är
enkla att testa. Priset är att villkorsnamnen blir ett kontrakt mellan BPMN-filen och koden — så låt workern
**kasta hårt på okänt villkor** i stället för att tyst svara "inte klar". Då blir ett stavfel en incident som
syns på ärendet, inte en process som står still utan förklaring.

**En ren manuell grind behöver ingen kontroll alls.** Är villkoret bara *"handläggaren tryckte"* är signalen
svaret. Det fungerar därför att SM bara accepterar signaler som står i `awaitingSignals` (§5.9) — en
handläggare kan alltså inte skicka en signal medan processen är upptagen någon annanstans, för då visas
ingen knapp, och en skrivning ändå ger `409`.

Krav 6 gäller processens sista fas: steget före `end_process` ska vara ett arbetssteg som rapporterar
`completed()`, annars får SM aldrig veta att processen är slut.

### 9.3 Starta, fortsätta och radera

```
handleErrandEvent(municipalityId, namespace, event):
    om event.eventType == DELETE:
        instans = findProcessInstances(businessKey = errandId, tenantIdIn = ALKT)
        finns -> deleteProcessInstance(id, failIfNotExists = false,
                                       reason = "errand deleted in SM")
        202                                        // arendet ar borta i SM; ingen rapport tillbaka

    instans = findProcessInstances(errandId, event.processKey, "ALKT")
    om tom:
        om event.processKey saknas -> logga, 202       // arendet har ingen processetikett
        om inte event.startAllowed -> logga, 202       // manuellt lage utan knapptryckning, eller
                                                       //  processlivet ar over. SM har avgjort, 7.7
        om processKey inte ar driftsatt -> 422
        start med businessKey = errandId
        POST .../processes {RUNNING}                   // 200 = nagon hann fore, ok
                                                       // 409 = avslutad eller annan levande
                                                       //       instans -> avbryt den nystartade
    annars:
        messageName = (event.eventSubType == SIGNAL) ? event.signalName : "errandUpdated"
        om messageName saknas -> logga ERROR, 202       // en retry kan aldrig hjalpa
        correlateMessage(messageName, businessKey = errandId, tenantId = "ALKT",
                         all = false)
```

**`findProcessInstances` ser bara det som kör just nu.** En avslutad process finns inte där utan ligger i
Operatons historik. Utan `startAllowed` skulle därför varje ny händelse efter `COMPLETED` starta en helt ny
process på samma ärende — ett meddelande eller en bilaga som kommer in efter beslutet skulle dra igång
ansökningsprocessen från början igen. Det är SM som håller reda på det och skickar med svaret (§7.7), så pw
behöver inte längre fråga tillbaka. Blir lovet inaktuellt under transporten är `409` från `POST`
skyddsnätet bakom (§7.4).

**Det är `processKey` som avgör om en process ska startas, inte händelsetypen.** Ett ärende kan skapas utan
etikett och få den först i ett andra anrop, och då kommer nyckeln med ett `UPDATE` och inte ett `CREATE`.
Startade vi bara på `CREATE` skulle det ärendet aldrig få någon process, och `PROCESS_TRIGGER=ERRAND`
(§7.1) vore verkningslös för precis det fall den finns till för. Villkoret är alltså: ingen levande instans,
ett `processKey` satt **och** `startAllowed`. De två första ser pw själv i Operaton; det tredje kommer från
SM och bär både startläget och processhistoriken (§7.7).

**Ett `MismatchingMessageCorrelationException` (400) ska ge en INFO-rad och `202`, inte en ny leverans.**
Annars fylls kön av misslyckade leveranser med händelser som var helt normala.

`POST /message` svarar `400` av två skilda skäl, och de betyder helt olika saker. *Ingen* träff är
normalfallet ovan. *Flera* träffar — vilket `all = false` också gör till ett `400` — betyder att
modelleringsregeln om parallella grenar i §6.4 har brutits. Behåll därför `all = false`: då blir brottet
ett synligt fel i stället för en tyst fan-out, och felposten ska skilja de två fallen åt.

`failIfNotExists = false` på raderingen gör `DELETE`-vägen idempotent. Kommer samma händelse två gånger
(§8.3) är instansen redan borta vid andra försöket, och utan flaggan hade det blivit ett fel av något som
gick precis som det skulle.

**Glöm inte `DELETE`.** Utan den lever processinstansen vidare i Operaton för ett ärende som inte längre
finns — och SM har inget spår kvar av den, eftersom `errand_process` städats bort med ärendet.

**Signaler korrelerar på sitt eget namn.** Är händelsens subtyp `SIGNAL` är det handläggaren som stegar
processen vidare (§5.9), och då är det signalens namn som ska korreleras — inte det generiska
`errandUpdated`. Matchar namnet inget väntläge är det samma sak som vilken missad korrelation som helst:
en informationsrad och `202`.

**Rapportera tillbaka vad processen väntar på.** När pw har startat eller väckt en instans, och när ett
arbetssteg är klart, ska nästa rapport innehålla `awaitingSignals` för det väntläge processen hamnat i.
Namnen behöver inte underhållas för hand — Operaton vet vilka meddelanden en instans prenumererar på just
nu, så pw kan fråga och rapportera vidare. Tomt fält betyder att processen inte väntar på någon människa.

`OperatonClient` behöver: `correlateMessage`, `findProcessInstances(businessKey, processDefinitionKey, tenantIdIn)`, `deleteProcessInstance(id, failIfNotExists)`, och `businessKey` i `OperatonMapper.toStartProcessInstanceDto`. Den befintliga `getEventSubscriptions()` behöver dessutom parametrar — den hämtar i dag hela motorn (§9.5).

### 9.4 Hur arbetsstegen är byggda

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
            service.complete(task, report.variables());   // resultatvarden, 9.2 punkt 5
        } catch (final Exception e) {
            logException(task, e);
            failureHandler.handleException(service, task, e.getMessage());   // rapporterar RETRYING/FAILED
        } finally {
            RequestId.reset();
        }
    }
}
```

Returtypen gör rapporten **obligatorisk på riktigt** — ett arbetssteg som inte lämnar någon rapport kompilerar helt enkelt inte.

**Om handläggaren hinner före.** `SupportManagementClient.getErrand` lämnar tillbaka ärendet tillsammans
med dess `ETag`, och `patchErrand` skickar med den som `If-Match`. Kommer det `412` betyder det att
handläggaren hunnit ändra under tiden: låt undantaget gå hela vägen upp till `execute`, så rapporterar
`FailureHandler` in `RETRYING` och Operaton kör om steget. Ett steg som bara läser skickar i stället med
`errandVersion` i sin rapport (§6.3).

`FailureHandler` rapporterar till SM **innan** den anropar `handleFailure`: `RETRYING` så länge
`calculateRetries` ger fler försök, annars `FAILED`. Det är den ordningen som gör att felmeddelandet
hamnar på ärendet där handläggaren ser det.

**När processen fattar beslutet själv** skriver arbetssteget det till
`PUT .../errands/{errandId}/decision` med `method: AUTOMATIC` — alltså inte i rapporten och inte som en
processvariabel. Rapporten handlar om processens tillstånd, medan beslutet är ärendedata med egen
livslängd, egen behörighet och egen revision (§7.5). `processId` fyller SM i själv, så steget behöver inte
veta något om sin egen rad där. Kommer beslutet i stället från en handläggare gör steget ingenting alls —
processen väcks av `DECISION`-händelsen och läser beslutet ur `errand.decision`.

### 9.5 Var `awaitingSignals` kommer ifrån

Namnen finns i Operaton. `GET /event-subscription` filtrerat på `processInstanceId` och
`eventType = message` lämnar tillbaka en rad per meddelande instansen prenumererar på just nu, med
`eventName` — meddelandets namn i modellen — och `activityId`. Ingen lista behöver alltså underhållas för
hand, vare sig i SM eller i pw. Läggs en grind till i BPMN dyker den upp av sig själv.

**Etiketten finns däremot inte där.** `EventSubscriptionDto` bär `eventName`, `eventType`, `activityId`,
`executionId`, `processInstanceId` och `tenantId` — ingen läsbar text. `ProcessSignal.label`, alltså
*"Godkänn granskning"*, måste hämtas ur modellen: `GET /process-definition/{id}/xml` och en uppslagning av
elementets `name` på `activityId`.

Två saker gör det ofarligt att göra så:

- **Svaret cachas per processdefinition.** En definition är oföränderlig — en ändrad modell ger en ny
  version med ett nytt id — så cachen kan aldrig bli inaktuell.
- **Ett `activityId` som inte hittas ger namnet som etikett.** Gränssnittet får då en knapp som heter
  `granskning-godkand` i stället för ingen knapp alls, och rapporten går igenom.

Alternativet — en map i pw från meddelandenamn till etikett — vore samma fel som designen undviker på
SM-sidan: en lista som glider isär från modellen utan att någon märker det.

---

## 10. Uppgifter att bygga

Varje uppgift går att slå ihop för sig, och acceptanskriterierna är skrivna så att de kan klistras rakt in
i en Jira-task.

Tabellen visar **i vilken ordning de bör göras**. Numreringen längre ner (T för SupportManagement, P för
pw-alkt) följer tjänst i stället för ordning.

| Steg |    Jira     |                    Uppgift                     |
|------|-------------|------------------------------------------------|
| 1    | DRAKEN-4734 | T1 — Datamodell och domänenums                 |
| 2    | DRAKEN-4735 | T2 — Konfigurationsläsning                     |
| 3    | DRAKEN-4736 | T3 — Process-API                               |
| 4    | DRAKEN-4737 | T4 — Optimistisk samtidighetskontroll          |
| 5    | DRAKEN-4738 | T5 — Publicering                               |
| 6    | DRAKEN-4739 | T6 — Relay och leverans                        |
| 7    | DRAKEN-4740 | T7 — Skyddsräcken                              |
| 8    | DRAKEN-4741 | T9 — Beslutet: modell, endpoint och spårbarhet |
| 9    | DRAKEN-4749 | T11 — Manuell stegning med signaler            |
| 10   | DRAKEN-4811 | T12 — Automatisk och manuell start             |
| 11   | DRAKEN-4742 | P1 — Operaton-klienten                         |
| 12   | DRAKEN-4743 | P2 — Event-endpoint och borttagning            |
| 13   | DRAKEN-4744 | P3 — SM-klienten                               |
| 14   | DRAKEN-4745 | P4 — Workerstruktur                            |
| 15   | DRAKEN-4750 | P7 — Manuella grindar och väntade signaler     |
| 16   | DRAKEN-4746 | T8 — `ProcessLoopGuardIT`                      |
| 17   | DRAKEN-4747 | P5 — Tillsynsprocessen                         |
| 18   | DRAKEN-4748 | P6 — Incidentåterkoppling                      |

### T1 — Datamodell och domänenums (SM)

**Bygg:** `V1_53`-migrering (§3.1); `ProcessStatus` med `isTerminal()` (§4.1); `ActivitySeverity`; entiteterna `ProcessEventOutboxEntity`, `ErrandProcessEntity` (§4.3), `ErrandProcessActivityEntity`; repositories med `Pageable` på **alla** sökfrågor; tabellerna i `truncate.sql`.

**Acceptans:**
- Ingen av T1:s entiteter är mappad som relation på `ErrandEntity`. (Beslutet i T9 är det enda undantaget, och det är avsiktligt — §3.2.)
- `ErrandProcessEntity.applyStatus` är enda vägen att sätta status; settern är inte publik.
- Tabelldrivet test räknar upp **varje** `ProcessStatus` mot `isTerminal()`, med `WAITING` explicit verifierad som *icke*-terminal.
- `applyStatus` med icke-terminal status nollar `ended`.
- Aktivitet **utan** processinstans går att spara, och kaskaderas bort när ärendet raderas (`fk_epa_errand`).
- Någon IT startar grönt ⇒ `schema-generation: validate` bekräftar DDL mot entiteter.

### T2 — Konfigurationsläsning (SM)

**Bygg:** `PROPERTY_PROCESS_CONSUMER`, `PROPERTY_PROCESS_TRIGGER`, `getValues(...)` (§7.1); `@ConfigurationProperties` för `process-engine.*`; validering av `PROCESS_CONSUMER` mot konfigurerade konsumenter vid skrivning.

**Acceptans:**
- `getValues` returnerar **alla** rader för en nyckel (regression mot `.findFirst()`).
- Skrivning av okänd `PROCESS_CONSUMER` ger `400`. Registret är `process-engine.consumers` (§7.2), och namnet i det är leveransadressen — ingen separat `identifier`-egenskap som kan drifta från sin nyckel.
- Verifierat att `namespaceConfigCache` evikteras vid skrivning — annars går konfigurationen inte att ändra i drift, hur mycket den än ser ut att göra det.

### T3 — Process-API (SM)

**Bygg:** `ErrandProcessResource` (`PUT`, `POST`, `GET` under `.../errands/{errandId}/processes`) och ärendescopad `GET .../process-activities` med valfritt `processInstanceId`-filter (§5.2); `ErrandProcessService`; API-modellerna (§5.3); `Errand.process` + batchberikning i `readErrand`/`findErrands`; regenerera `openapi.yaml`. Fältet `awaitingSignals` på samma modell hör till T11 — bygg det inte här. `GET .../processes` ska däremot svara med kuvertet `ErrandProcesses` (§5.10) redan här; fältet `startable` fylls i T12.

**Acceptans:**
- `PUT` två gånger ⇒ `revision`-tabellen oförändrad (skyddar mot framtida `@OneToMany` på `ErrandEntity`).
- Aktiviteter idempotenta på `(processInstanceId, externalTaskId, activityId)`; batch > 100 ⇒ `400`.
- `GET .../process-activities` returnerar även poster utan processinstans; filtret `processInstanceId` utesluter dem.
- **Kapplöpningen i §5.1 körd i båda ordningarna:** `PUT` först (skapar raden) följt av `POST` med samma `processInstanceId` ⇒ `200` och orört tillstånd; `POST` först följt av arbetsstegets `PUT` ⇒ tillståndet uppdateras. Ingen av ordningarna ger `409`.
- Annan levande instans med **annat** `processInstanceId` ⇒ `409`; instans med annat `process_key` ⇒ `409`; constraint-violation översatt enligt §7.4, aldrig `500`.
- `POST` mot ärende som redan har en `COMPLETED` instans ⇒ `409`. `POST` mot ärende som bara har en `FAILED` instans ⇒ `201`.
- `errand.process` projicerar **senaste** instansen: ärende med misslyckad start visar `FAILED` med sitt felmeddelande, inte `null`.
- Batchberikningen är "senaste per ärende" och fortfarande **en** fråga — verifieras med query-räkning.
- `GET .../processes` sorterar nyast först.
- `findErrands` gör **en** fråga för berikningen (verifieras med query-räkning, inte ögonmått).
- `ErrandProcess` används som **en** modell för både subresursen och `errand.process`; `externalTaskId`, `errandVersion` och `activities` syns aldrig i ett lässvar (§5.3).
- `PUT` med ett `processInstanceId` i kroppen som skiljer sig från pathens ⇒ `400`.
- Notis som skapas av en processkrivning har en avsändare i `createdBy`, inte tom sträng: `EventService.createNotification` faller tillbaka på identitetens värde när `getAdUser()` är null (§1.8).
- `PROCESS` tillagt i `ErrandField` och filtrerat av `roleBasedFieldResolver` (§5.3). Test för båda riktningarna: namespace **utan** åtkomstkontroll ⇒ fältet syns; begränsad användare i ett namespace **med** åtkomstkontroll som inte räknat upp `PROCESS` ⇒ fältet utelämnas.
- Beslut fattat och dokumenterat om `decision` ska vara reducerad i listsvar (§5.3).

### T4 — Optimistisk samtidighetskontroll (SM)

**Bygg:** `errandVersion` i rapportmodellen och kontrollen mot `errand.version` i `ErrandProcessService` (§6.3); WARN-aktivitet och `process.concurrent_task_detected` när två skilda `externalTaskId` rapporterar `RUNNING` mot samma instans utan terminal rapport emellan (§6.4); `process.errand_conflict`.

**Acceptans:**
- Rapport med `errandVersion` som glidit ⇒ `412`, och **varken** tillstånd eller aktiviteter skrivs.
- Rapport utan `errandVersion` ⇒ ingen kontroll, `200`.
- Två skilda `externalTaskId` med `RUNNING` mot samma instans ⇒ WARN-aktivitet skriven, **båda** rapporterna tas emot.
- `ErrandProcessService` tar en injicerad `Clock`. **Inget test använder `Thread.sleep`.**
- Ett test bekräftar att `PATCH /errands/{id}` med föråldrad `If-Match` ger `412` — regressionsskydd för att hela samtidighetsmodellen vilar på befintligt beteende.

### T5 — Publicering (SM)

**Bygg:** `ProcessEventPublisher` anropad från `EventService.createErrandEvent`, med `setRollbackOnly` före kast (§2.2); `TriggerProcessFilter` med ThreadLocal i `ServiceUtil` och headern dokumenterad i `OpenApiConfig`, efter mönstret från `X-Request-Group-Id` (§6.5); `ProcessKeySelector` (§7.3); nödbromsen.

**Acceptans:**
- **IT som verifierar att ett e-postintag ger en outbox-rad.** Intaget skapar inga revisioner och går förbi den gemensamma passagen (§1.1) — tappas det där märks det inte av något annat test.
- Enhetstest per gren i §2.2, inklusive: `X-Trigger-Process: false` ⇒ ingen rad; icke-triggad subtyp ⇒ ingen rad; namespace utan konsument ⇒ ingen rad.
- **Utan header ⇒ raden skrivs**, och **headern satt av ett AD-konto ⇒ raden skrivs ändå** (§6.5). Filtret får inte vara bredare än sitt syfte.
- Tabelldrivet test över headervärdena: bara exakt `false`, versaloberoende och trimmad, tystar raden. `true`, tom sträng och skräp gör det inte.
- Skrivning helt utan request-kontext — ett schemalagt jobb — ⇒ raden skrivs.
- **`DELETE` av ett ärende vars etikett tagits bort ⇒ raden publiceras ändå**, med `process_key` null. Utan det blir processinstansen föräldralös i Operaton.
- Ärende med processinstans: `process_key` i raden kommer från instansen, inte från etiketterna. Verifieras genom att ändra etiketten i testdata och se att nyckeln står still.
- `ProcessKeySelectorTest`: en tagg ⇒ en nyckel; två med samma ⇒ en; två med olika ⇒ ERROR-aktivitet och ingen rad; `deprecated` ignoreras; **namnbyte och omflyttning av labeln lämnar upplösningen oförändrad**.
- Selektorn lämnar nyckel **och** startläge som ett par, ur samma etikett. Lovet räknas ut först i T12, men paret ska inte behöva byggas om då (§7.7).
- **Publisher kastar ⇒ ärendeskrivningen är inte committad**, trots att anropsstället sväljer undantaget (§1.7). Verifieras genom att PATCH:a och sedan läsa tillbaka ärendet — inte genom att inspektera loggen.
- Utan aktiv transaktion: ERROR-logg och `process_event.publish_failed` ökar, inget kast som spräcker anropet.
- Nödbromsen slår över tröskeln med rader som har `delivered_at` satt, och dess ERROR-aktivitet skrivs **utan** instans.
- `ProcessKeySelector` med två skilda nycklar skriver ERROR-aktivitet **utan** instans — testet får inte förutsätta att en instansrad finns — och **en gång per ärende och fönster**: tio händelser på ett tvetydigt ärende ger en post, inte tio (§2.2).
- Kommandon (subtyp `PROCESS`, `SIGNAL`) publiceras även när nödbromsen slagit till för ärendet och även när `PROCESS_TRIGGER` är tom (§6.5).

### T6 — Relay och leverans (SM)

**Bygg:** paketet `service/scheduler/processevent/` med schemaläggare, jobb och relay efter mönstret i `service/scheduler/notificationdispatch/` — leverans och kvittering i samma transaktion, ingen retry-bokföring; direktkörningen efter commit tillsammans med en trådpool med tak; `ProcessEngineClient` med ett lager som översätter felen; `422` som permanent fel och `5xx` som tillfälligt; `max-age` och röjningen av levererade rader (§8.3); mätvärdena i §8.1.

**Acceptans:**
- WireMock svarar `202` / `422` / `503` / timeout — samtliga fyra vägar verifierade, inklusive att `422` **inte** görs om, utan konsumerar raden och skriver `FAILED` + ERROR-aktivitet.
- **`503` ⇒ raden ligger kvar orörd** och nästa körning levererar den. Verifieras genom att läsa raden ur databasen, inte genom loggen.
- Rad som passerat `max-age` släpps oskickad, loggas som ERROR och räknas i `process_event.aged_out`.
- Röjningen tar levererade rader på `delivered_at` (§4) och lämnar **oskickade** rader i fred.
- **Hälsoindikatorn är grön direkt efter en publicering** och slår om först när äldsta oskickade rad passerat `unhealthy-after` (§8.3). Ett test som bara skriver en rad och läser indikatorn får inte se unhealthy.
- **Två konsumenter, en nere:** den friska konsumentens rader levereras i samma körning (§7.6). Utan det svälter ett namespace ett annat.
- `batch-size` respekteras per konsument och körning; hämtningen har ett `LIMIT` (§1.2).
- Raden levereras till den tjänst som står i `process_service`, även om `PROCESS_CONSUMER` hunnit ändras efter publiceringen.
- Ordning per ärende hålls när flera rader finns, och en grupp som fallerar rullas tillbaka i sin helhet.
- Full trådpool ⇒ direktkörningen hoppas över, `process_event.direct_run_rejected` ökar och cronjobbet levererar i stället. **Inget undantag når anroparen** — testet ska fylla kön och kontrollera att ärendeskrivningen ändå svarar `200` (§2.3).
- Samma händelse levererad två gånger, efter en återrullad transaktion, ger inte två processinstanser — idempotensen ligger hos pw (§8.3, §9.3).

### T7 — Skyddsräcken (SM)

**Bygg:** `400` på etikettändring som byter `processKey` — **både i `ErrandService.updateErrand` och i `AddLabelAction.executeAction`**, som körs schemalagt och aldrig passerar API:t.

**Acceptans:**
- `400`-fallet täckt av enhetstest och ett IT-fall.
- `AddLabelAction` som skulle byta upplöst `processKey` på ett ärende med levande instans ⇒ etiketten läggs inte till, ERROR-aktivitet skrivs. Utan detta slutar processen tyst få väckningar (§11).

### T8 — `ProcessLoopGuardIT` (SM)

**Det viktigaste enskilda testet.** Kör hela varvet: ärende skapas ⇒ outbox-rad; stub agerar pw, rapporterar `RUNNING`, PATCHar ärendet med `X-Trigger-Process: false` och rapporterar `WAITING`.

- **Kontrollera att pw:s egen PATCH inte gav någon ny outbox-rad** (lager 1).
- Kör sedan **samma** PATCH med en handläggaridentitet — **med headern kvar** — och kontrollera att den **ger** en rad. Filtret får inte vara så brett att äkta ändringar tystas, och AD-undantaget i §6.5 är det som håller emot. Det felet är osynligt i drift tills någon undrar varför processen aldrig vaknar.
- Kör pw:s PATCH **utan** headern och verifiera att lager 2 eller 3 fångar den.

Utan detta test är loop-skyddet en hypotes.

### T9 — Beslutet: modell, endpoint och spårbarhet (SM)

**Bygg:** `V1_54`-migreringen med `errand_decision` (§3.1) och `DecisionEntity` som `@OneToOne` på `ErrandEntity`; enums `DecisionOutcome` och `DecisionMethod` (§4.2); `Decision`-modellen (§5.3); `ErrandDecisionResource` (`GET`, `PUT`, `DELETE` med `If-Match`) och `ErrandDecisionService`; `method`-regeln mot identiteten; `EventSubType.DECISION`-event och revision från beslutsskrivningen; låsning mot `COMPLETED` process; `Errand.decision`; `DECISION` i `PROCESS_TRIGGER` för ALKT; mätvärdena i §8.1; regenerera `openapi.yaml`; tabellen i `truncate.sql`.

**Acceptans:**
- Beslutsskrivning ger en eventlogg-post med subtyp `DECISION` **och** en revision, och beslutet ingår i revisionssnapshotten. Utan eventet publiceras ingen outbox-rad och processen vaknar aldrig.
- Skrivningen höjer `errand.version`; ett arbetssteg med äldre ETag får `412`.
- **`method: AUTOMATIC` från någon annan än namespacets `PROCESS_CONSUMER` ⇒ `403`. `method: MANUAL` från en icke-AD-identitet ⇒ `403`.** Båda riktningarna testade — det är den skillnaden som ska hålla i efterhand (§7.5).
- `processId` sätts av tjänsten, inte av kroppen: en klient som skickar `processId` får det ignorerat, och vid `AUTOMATIC` pekar det på ärendets levande processrad.
- Andra skrivning på samma ärende går igenom medan processen lever, men ger `409` när ärendets process är `COMPLETED`. Samma spärr gäller `DELETE`.
- Ärende **utan** process: beslut går att skriva och ändra, och `409`-spärren slår aldrig till.
- `attachmentId` som pekar på en bilaga i ett annat ärende ⇒ `400`.
- Två samtidiga skrivningar med samma `If-Match` ⇒ den andra får `412` (`@Version` på entiteten).
- Radering av ärendet kaskaderar bort beslutet; radering av processraden gör det **inte** (`SET NULL`, §3.2).
- **IT: handläggaren skriver beslutet ⇒ outbox-rad med subtyp `DECISION`.** Det är hela kedjan som gör att processen kan avslutas.
- Enhetstest som verifierar att en beslutsskrivning med `X-Trigger-Process: false` **inte** ger en outbox-rad — lager 1 gäller även här.
- `justification` förekommer inte i någon loggrad (§8.1).
- `DECISION` tillagt i `ErrandField` (§5.3). Begränsad användare i ett namespace med åtkomstkontroll som inte räknat upp `DECISION` får ärendet **utan** beslutet — testat, eftersom det är `justification` som annars läcker.

### T11 — Manuell stegning med signaler (SM)

**Bygg:** `V1_55`-migreringen med `errand_process_signal` (§3.1), entitet och repository; `ProcessSignal` i API:et och `awaitingSignals` på `ErrandProcess` (§5.3); `POST .../processes/{processInstanceId}/signals` (§5.9); aktivitetspost med `activityType = SIGNAL`; värdet `SIGNAL` i `EventSubType`; signalnamnet i outbox-raden och i händelsemodellen — kolumnen `signal_name` skapas redan i T1:s `V1_53` (§3.1), här fylls den i; mätvärdena `process.signal_sent` och `process.signal_rejected`; regenerera `openapi.yaml`; tabellen i `truncate.sql`.

**Acceptans:**
- **Outbox-raden bär signalens namn i `signal_name`, och det följer med ut i `signalName` på händelsen.** Ett test som bara kontrollerar att en rad skrevs missar poängen — det är namnet pw korrelerar på (§5.4).
- En rapport med `awaitingSignals` ersätter tidigare rader helt — tas en signal bort ur rapporten försvinner den ur `errand.process`.
- Tomt `awaitingSignals` tömmer listan, och betyder att processen inte väntar på någon människa.
- Signal som står bland de väntade ⇒ `202`, aktivitetspost och outbox-rad med subtyp `SIGNAL`.
- Signal som **inte** står bland de väntade ⇒ `409`, och ingenting skrivs. Det är samtidigt dubbelklicksskyddet.
- Signal mot ärende utan levande process ⇒ `404`; mot avslutad process ⇒ `409`.
- Aktivitetsposten namnger avsändaren, så att "vem stegade processen förbi granskningen" går att besvara i efterhand.
- **Signalen publiceras även när `PROCESS_TRIGGER` är tom** för namespacet — kommandon filtreras inte (§7.7). Täck det med ett test, annars kryper filtret tillbaka nästa gång någon förenklar publiceraren.
- Signal från en icke-AD-identitet ⇒ `403`, och ingenting skrivs (§5.10).
- Berikningen av `awaitingSignals` gör **en** extra fråga för hela sidan, verifierat med frågeräkning.

### T12 — Automatisk och manuell start (SM)

**Bygg:** `start_allowed` i outbox-raden och `startAllowed` i händelsemodellen — kolumnen skapas i T1:s `V1_53` (§3.1), här fylls den i (§5.4); attributet `processStartMode` med validering vid etikettskrivning (§7.7); `ProcessKeySelector` som lämnar nyckel och läge som ett par; startlovet i publiceringens steg 6 och kommandonas undantag från triggerfiltret (§2.2); `startable` i kuvertet runt `GET .../processes` (§5.10); `POST .../processes/start` med `ProcessStartRequest`; värdet `PROCESS` i `EventSubType`; aktivitetspost med `activityType = START`; mätvärdena `process.start_requested` och `process.start_rejected`; regenerera `openapi.yaml`.

**Acceptans:**
- Etikett med `processStartMode: MANUAL` ⇒ `POST /errands` skapar ärendet, publicerar en rad med `start_allowed = 0`, och ingen process startar. Samma etikett med `AUTOMATIC` ⇒ `start_allowed = 1` och processen startar.
- Attributet saknas helt ⇒ beter sig som `AUTOMATIC`. Regressionen är hela poängen: den som inte rör attributet ska inte märka något.
- Etikettskrivning med annat värde än `AUTOMATIC`/`MANUAL` ⇒ `400`. `processStartMode` på en etikett utan `processKey` ⇒ `400`.
- **Nyckel och läge kommer ur samma etikett.** Ärende med två etiketter som bär varsin nyckel och olika lägen — testet får inte kunna passera genom att plocka läget från fel etikett (§7.7).
- Ärende med `COMPLETED` instans ⇒ `start_allowed = 0` på nästa händelse, utan att pw behöver fråga. Ärende med bara en `FAILED` instans ⇒ `start_allowed = 1`.
- `POST .../processes/start` ⇒ `202`, aktivitetspost **utan** processinstans, och en outbox-rad med subtyp `PROCESS`, `start_allowed = 1` och den **valda** nyckeln i `process_key`.
- Kommandot publiceras **även när `PROCESS_TRIGGER` är tom** för namespacet (§7.7).
- Kommandot från en icke-AD-identitet ⇒ `403`, och ingenting skrivs: varken aktivitetspost eller outbox-rad.
- Två snabba tryckningar med **samma** nyckel ⇒ en outbox-rad, `202` på båda. Med **olika** nyckel ⇒ `409` på den andra, och den första raden står orörd.
- Kommandot publiceras **även när nödbromsen har slagit till** för ärendet (§6.5). Utan det testet blir knappen tyst verkningslös på just de ärenden som har mest trafik.
- Tvetydiga etiketter: utan `processKey` i kroppen ⇒ `400`; med en av ärendets nycklar ⇒ `202` och just den nyckeln i raden; med en nyckel som inte hör till ärendets etiketter ⇒ `400`.
- `409` för levande instans och för avslutat processliv; `400` för ärende utan nyckel och för namespace utan `PROCESS_CONSUMER`.
- Kommandot fungerar i **automatiskt** läge också, och startar om ett ärende vars enda instans är `FAILED` (§5.10).
- `GET .../processes` svarar med kuvertet: ärende utan process ⇒ `processes: []` och `startable.status` enligt läget; ärende med `COMPLETED` ⇒ `status: PROCESS_COMPLETED` och tom `processKeys`.
- `processKeys` är tom så snart `status` inte är `AVAILABLE` — det finns inget läge där ett hinder redovisas tillsammans med nycklar att starta.
- `startable` kostar ingen extra fråga per ärende i listsvar — fältet finns bara på processendpointen, inte på ärendeprojektionen (§5.10). Verifieras med frågeräkning.
- **Den genererade specen granskas, inte bara annotationerna:** varje fält i `startable` ska gå att förstå av en klientutvecklare som inte läst det här dokumentet, och `status` ska visa sina värden som ett enum.

### P1 — Operaton-klienten (pw)

**Bygg:** `correlateMessage` (`POST /message`, `all = false`), `findProcessInstances` (`GET /process-instance` med `businessKey`, `processDefinitionKey`, `tenantIdIn`), `deleteProcessInstance` (`DELETE /process-instance/{id}` med `failIfNotExists = false`); parametrar på befintliga `getEventSubscriptions` (`processInstanceId`, `eventType`); `getProcessDefinitionXml` för etiketterna i §9.5; `businessKey` i `OperatonMapper.toStartProcessInstanceDto`; nya konstanter.

**Byt också ut `src/main/resources/integrations/operaton-openapi.json`.** Filen heter operaton men *är*
Camunda Platform REST API `7.19.4-ee` — den innehåller 1057 förekomster av "camunda" och noll av
"operaton". DTO:erna i bygget genereras alltså ur fel spec. Skillnaden är i praktiken liten, men den finns:
en jämförelse mot Operaton REST API `2.1.3` ger 305 paths mot 304, där Operaton *lägger till*
`/process-instance/{id}/comment/{commentId}`, och `CorrelationMessageDto` har fått
`processVariablesToTriggeredScope`. Inget pw anropar saknas. Hämta den riktiga från
`docs.operaton.org/reference/latest/rest-api/operaton-rest-api.json`.

**Acceptans:**
- `ProcessWithoutDeviationIT` fortsatt grön efter specbytet.
- `info.title` i den incheckade specen säger `Operaton REST API`.
- Radering av en instans som inte finns ger inget fel.

### P2 — Event-endpoint och borttagning (pw)

`POST /process/errand-events` med logiken i §9.3, **och borttagningen i §5.8 i samma steg**. Regenerera `openapi.yaml`.

**Acceptans:** start / korrelera / okänt ärende / DELETE / okänd nyckel (`422`) täckta; inga referenser kvar till `updateAvailable`, `StartProcessResponse` eller `setProcessInstanceVariable`.
- **`UPDATE`-event utan levande instans, med `processKey` och `startAllowed: true` ⇒ processen startas.** Det är fallet där etiketten sattes i ett andra anrop (§7.1); startas bara på `CREATE` faller det tyst bort.
- `UPDATE`-event utan `processKey` ⇒ `202`, ingen start.
- **Event med `startAllowed: false` ⇒ `202`, ingen start** — även när ingen instans kör och `processKey` är satt. Det täcker både manuellt läge och ett avslutat processliv; att fråga Operaton vad som kör just nu räcker inte som villkor, den ser inte avslutade instanser (§9.3).
- **Fältet saknas i kroppen ⇒ läses som `false`**, inte som `true` (§5.4).

### P3 — SM-klienten (pw)

`patchErrand`, `report(...)`, `getErrand`; `RequestInterceptor` för `X-Sent-By`/`X-Request-Group-Id`/`X-Trigger-Process`; uppdatera `support-management.yaml` och regenerera; WireMock-stubbar. `getErrand` måste returnera ärendets `ETag` tillsammans med kroppen, och `patchErrand` skicka den som `If-Match` (§6.2).

**Acceptans:**
- Test som verifierar att `X-Sent-By` sätts på **alla** utgående anrop, inte bara ett, och att `X-Trigger-Process: false` följer med alla skrivande anrop. Ett steg som medvetet vill väcka processen igen utelämnar headern för just det anropet.
- `getErrand` följt av `patchErrand` skickar den ETag servern gav; stub som svarar `412` ger ett undantag som når `execute`.

### P4 — Workerstruktur (pw)

`AbstractTaskWorker` enligt §9.4; `ProcessStateReport` med fabriksmetoder; `FailureHandler` rapporterar `RETRYING`/`FAILED`.

**Acceptans:** IT verifierar ordningen `RUNNING` → PATCH → terminal rapport; ett fall där `executeBusinessLogic` kastar kontrollerar att `RETRYING` rapporterats; ett fall där SM svarar `412` på PATCH kontrollerar att steget körs om och att andra försöket läser om ärendet.

### P7 — Manuella grindar och väntade signaler (pw)

**Bygg:** korrelation på signalens namn när händelsens subtyp är `SIGNAL` (§9.3); rapportering av `awaitingSignals` när processen går in i ett väntläge, hämtat ur Operatons event subscriptions (§9.5); manuella grindar i modellen enligt §9.2 punkt 2.

**Acceptans:**
- Händelse med subtyp `SIGNAL` korreleras på `signalName`; alla andra subtyper på `errandUpdated`.
- Subtyp `SIGNAL` **utan** `signalName` ⇒ ERROR-logg och `202`. Ingen retry, eftersom en sådan händelse aldrig kan bli korrelerbar.
- Signal som inte matchar något väntläge ⇒ informationsrad och `202`, ingen ny leverans.
- Efter varje avslutat arbetssteg innehåller rapporten de signaler instansen nu väntar på — och en tom lista när väntläget är automatiskt.
- `awaitingSignals` fylls ur `GET /event-subscription` filtrerat på instansen och `eventType = message`, och etiketten slås upp ur modellen enligt §9.5 — inte ur en lista i pw.
- Etikettuppslagningen cachas per processdefinition, och ett okänt `activityId` ger namnet som etikett i stället för att spränga rapporten.
- IT som kör hela varvet: process i manuell grind, signal från SM, processen går vidare och rapporterar nästa väntläge.

### P5 — Tillsynsprocessen (pw)

`alkt-tillsyn.bpmn`; justera väntevillkoret i `ProcessWithoutDeviationIT.setup` till 2; egen `ProcessPathway`.

### P6 — Incidentåterkoppling (pw)

Schemalagd kontroll som skriver `FAILED` + `error` till SM när Operaton rest en incident.

Samma kontroll stämmer av instanser som **försvunnit ur Operatons runtime utan att någon rapporterat ett
slut**: står SM:s rad kvar som levande medan varken runtime eller historik visar en pågående instans, ska
den skrivas som `COMPLETED` eller `FAILED` beroende på hur instansen slutade. Utan den avstämningen kan
ärendet varken gå vidare eller få en ny process (§9.2 punkt 6).

---

## 11. Vad som kan gå fel, och vad vi gör åt det

|                              Risk                               |                                                                                                                                                Hantering                                                                                                                                                 |
|-----------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **RabbitMQ-mognad** (öppen fråga)                               | §2.4. Varje REST-konsument byggd innan bytet är kastat arbete                                                                                                                                                                                                                                            |
| **`WAITING` felaktigt behandlad som terminal**                  | Skulle bryta 1-1-invarianten tyst. Skyddas av `applyStatus` som enda väg + tabelldrivet test (T1)                                                                                                                                                                                                        |
| **Handläggaren ändrar mitt i ett arbetssteg**                   | `412`, och steget körs om (§6.2). Kostar en omkörning. Mäts av `process.errand_conflict`                                                                                                                                                                                                                 |
| **Steg med extern sidoeffekt körs om**                          | Sidoeffekten kan dubbleras. Samma krav som när ett steg kraschar (§9.4) — lägg sidoeffekten sist i steget, eller gör den idempotent                                                                                                                                                                      |
| **Läsande steg utan `errandVersion`**                           | Då finns inget skydd alls (§6.3). Det får vara ett medvetet val för varje steg — men ett val som faktiskt måste göras                                                                                                                                                                                    |
| **Underresurser fångas inte av versionen**                      | En bilaga som raderas mitt i en körning höjer inte `errand.version`. Processen måste läsa om bilagor när den behöver dem                                                                                                                                                                                 |
| **Parallella grenar**                                           | Modelleringsregel + WARN-aktivitet gör brottet synligt (§6.4)                                                                                                                                                                                                                                            |
| **Loop SM ↔ pw**                                                | Tre lager (§6.5). Lager 1 vilar på en klientsatt header — därför är `process_event.suppressed{reason=OPT_OUT}` ett mätvärde som **ska** vara skilt från noll                                                                                                                                             |
| **Klient som tystar sina egna skrivningar**                     | `X-Trigger-Process: false` är fritt satt, så en integration som härmar pw kan göra sina ändringar osynliga för processen. Headern hedras inte för AD-identiteter (§6.5), och varje undertryckt rad räknas med anropande identitet i `process_event.suppressed`                                           |
| **Beslut skrivet medan processen arbetar**                      | Korrelationen sväljs och väckningen är borta. Fångas bara av modelleringskravet i §9.2 punkt 1 — väntläget måste läsa om ärendet när det går in i väntan. Ingen kod i SM kan rädda ett väntläge som inte gör det                                                                                         |
| **`DECISION` saknas i `PROCESS_TRIGGER`**                       | Processen vaknar aldrig av beslutet och står i `WAITING` för alltid. Validering av triggervärden och ett IT-fall i T9                                                                                                                                                                                    |
| **Personuppgifter i beslutets motivering**                      | `justification` innehåller nästan alltid personuppgifter. Får aldrig loggas (§8.1), aldrig kopieras till aktivitetsloggen och aldrig läggas i outboxens nyttolast — den bär medvetet ingen ärendedata alls (§5.4). Beslutet kaskaderas bort med ärendet                                                  |
| **Automatiskt beslut felstämplat som manuellt, eller tvärtom**  | `method` valideras mot identiteten vid systemgränsen och räknas i `decision.written` (§8.1). Utan bådadera går frågan "vilka beslut fattades av en maskin?" inte att svara på i efterhand — och det är en fråga som kommer att ställas                                                                   |
| **Instans som försvinner utan slutrapport**                     | SM står kvar på `RUNNING` medan instansen är borta ur Operaton, och ärendet kan varken gå vidare eller få en ny process. Modelleringskravet i §9.2 punkt 5 ska hindra det; P6:s schemalagda kontroll stämmer av det som ändå glider isär                                                                 |
| **Skelettmodellen driftsatt för tidigt**                        | Sex tomma subprocesser springer igenom på millisekunder. Startas den mot ett skarpt ärende är ärendets processliv förbrukat (§9.1). Driftsätt inte förrän väntlägena finns                                                                                                                               |
| **Manuell grind som ingen klickar på**                          | Processen står i `WAITING` för alltid. Modelleringsregeln i §9.2 punkt 2 kräver en tidsgräns på grindar som kan glömmas bort, och `process.signal_sent` visar vilka grindar som faktiskt används                                                                                                         |
| **Signal som accepteras men aldrig konsumeras**                 | Går processen vidare på en timer i samma stund som handläggaren trycker, hinner SM svara `202` innan den nya bilden rapporterats. Signalen når då inget väntläge och är borta. Handläggaren ser det vid nästa omläsning, men vi räknar det inte någonstans — överväg ett mätvärde om det visar sig hända |
| **Knapp som inte längre gäller**                                | Handläggaren ser en signal processen hunnit lämna. Skrivningen ger `409` och räknas i `process.signal_rejected` — gränssnittet ska läsa om ärendet, inte försöka igen                                                                                                                                    |
| **Beslut på ärende helt utan process**                          | Tillåtet och olåst: det finns ingen `COMPLETED` process att låsa mot. Spårbarheten bärs då av revisionen, inte av spärren (§7.5)                                                                                                                                                                         |
| **Föräldralös processinstans efter radering**                   | `DELETE` publiceras även utan `processKey` (§2.2), och pw raderar på `businessKey` (§9.3). Restrisk kvarstår om leveransen aldrig går igenom och raden åldras ur — därför `process_event.aged_out` och hälsoindikatorn (§8.3)                                                                            |
| **Etikettändring utanför API:t**                                | `AddLabelAction.executeAction` körs schemalagt och lägger till etiketter utan att passera någon endpoint. Byter den upplöst `processKey` slutar processen tyst få väckningar — T7:s kontroll måste ligga även där                                                                                        |
| **Publiceringsfel sväljs av anropsstället**                     | `setRollbackOnly` före kast (§2.2) gör svälj-fångsten ofarlig. Kvarstående hål: anropsväg helt utan transaktion — mäts av `process_event.publish_failed`                                                                                                                                                 |
| **Start uteblir när etiketten sätts sent**                      | Start villkoras av `processKey`, inte `eventType` (§9.3). Täckt av ett P2-fall                                                                                                                                                                                                                           |
| **Startlov som hunnit bli inaktuellt**                          | Lovet räknas ut vid publicering och används vid leverans. Hinner processen gå i mål däremellan startar pw något den inte borde — `409` från `POST .../processes` och pw:s avbrytande av den nystartade instansen är skyddsnätet (§7.7)                                                                   |
| **Felstavat `processStartMode`**                                | Attributnycklar är inte whitelistade (§1.6), så `processstartmode` hade tyst betytt `AUTOMATIC`. Värdet valideras därför när etiketten skrivs, inte när den läses (§7.7)                                                                                                                                 |
| **Automatisk start på gamla ärenden vid driftsättning**         | Ett ärende som redan bär etiketten startar vid nästa ändring, inte bara vid `CREATE` (beslut 17). Rulla ut med `MANUAL` på etiketterna och byt till `AUTOMATIC` när kedjan är sedd i drift (§7.7)                                                                                                        |
| **Signal utan namn i händelsen**                                | pw vet att någon tryckte men inte på vad, och grinden öppnas aldrig. `signal_name` i outbox-raden och `signalName` på händelsen bär namnet hela vägen (§5.4); pw svarar `202` med ERROR-logg om det ändå saknas, eftersom en retry inte kan hjälpa                                                       |
| **Kommando som tystas av ett filter**                           | Ett `202` utan verkan är knappen som ser ut att fungera. Kommandon undantas därför från nödbromsen och triggerfiltret, och de tre lagren gäller bara härledda händelser (§6.5). Täckt av testfall i T5 och T12                                                                                           |
| **Maskinidentitet som trycker på startknappen**                 | Skulle ge `202` och ingen start, eftersom lager 1 filtrerar bort raden när pw:s `X-Trigger-Process: false` följer med (§6.5). Kommandot kräver AD-konto och svarar `403` (§5.10)                                                                                                                         |
| **Felstavat `processKey`**                                      | Upptäcks vid första ärendet. `422` ⇒ ingen retry, `FAILED` + ERROR-aktivitet direkt på ärendet                                                                                                                                                                                                           |
| **Oskickad rad som ingen upptäcker**                            | Utan dead letter-flagga finns ingenting att larma på i tabellen. Hälsoindikatorn, `process_event.oldest_undelivered_age` och `process_event.aged_out` är det som gör raden synlig (§8.3)                                                                                                                 |
| **Återlevererad händelse efter återrullad transaktion**         | Leverans och kvittering delar transaktion, så ett fel efter att pw tagit emot händelsen ger en till. pw:s event-endpoint måste vara idempotent — annars blir följden dubbla processinstanser (§8.3)                                                                                                      |
| **Routingen går att ändra i drift, utan granskning**            | Priset för att slippa en release varje gång. Validering av `PROCESS_CONSUMER`; överväg ändringslogg                                                                                                                                                                                                      |
| **En långsam pw svälter de andra**                              | Ett jobb levererar åt alla namespace, och hämtningen sorterar deterministiskt. Motmedlen är `batch-size` per konsument, oberoende leverans, circuit breaker per konsument och kort read-timeout (§7.6)                                                                                                   |
| **Nödbromsen tolkar ett leveransavbrott som en loop**           | Skulle förvandla en fördröjning till permanent händelseförlust. Bromsen räknar därför bara rader med `delivered_at` satt (§6.5)                                                                                                                                                                          |
| **Hälsoindikatorn på existens i stället för ålder**             | Tjänsten står unhealthy under normal drift och indikatorn slutar betyda något. Villkoret är `unhealthy-after` (§8.3)                                                                                                                                                                                     |
| **`justification` utanför fältfiltreringen**                    | Beslutsmotiveringen är fritext med personuppgifter. `PROCESS` och `DECISION` är `ErrandField`-värden och stängda som utgångsläge för begränsade användare (§5.3). Att ALKT saknar åtkomstkontroll döljer bara felet till nästa namespace                                                                 |
| **Åtkomstkontroll påslagen för ett namespace med processmotor** | pw får `401` på allt, och felet visar sig som ärenden som står stilla — inte som ett behörighetsfel. AccessMapper svarar bara på AD-konton (§1.8). Spärren i §7.1 gör det till ett konfigurationsfel i stället, och lyfts först när AccessMapper kan bevilja maskinidentiteter                           |
| **Personuppgifter i aktivitetsloggen**                          | `message` är fri text som kommer från processen. **pw måste instrueras att inte skriva personuppgifter där** — det är en regel, inte en spärr                                                                                                                                                            |
| **Dubbla processinstanser**                                     | ShedLock-serialiserad leverans + businessKey-kontroll + `409` + DB-constraint. Restrisk i Operaton, som saknar unikhet på business key — men SM kan inte registrera resultatet                                                                                                                           |
| **Delas Operaton-tenanten `ALKT`?**                             | Påverkar `getDeployments`-assertions och `historyTimeToLive`. Bekräfta mot driftmiljön                                                                                                                                                                                                                   |

### Vad som inte går att verifiera automatiskt

- Att WSO2 släpper igenom med rätt scope, och att `If-Match`/`ETag` passerar oförvanskade.
- Verklig samtidighet mellan poddar — ShedLock täcks indirekt av `ShedlockConfigurationIT`.
- Långtidsbeteende hos outbox och aktivitetslogg. Kompensation: mätvärdena i §8.1.

