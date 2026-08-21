# SupportManagement ↔ pw-alkt — processintegration

Så här ska SupportManagement och den nya tjänsten [pw-alkt](https://github.com/Sundsvallskommun/pw-alkt)
samarbeta när ALKT-namespacets myndighetsprocess körs i Operaton. Dokumentet går igenom datamodellen och
API:erna, hur handläggarens och processens ändringar hålls isär, hur vi hindrar att de två tjänsterna
väcker varandra i all oändlighet — och sist en uppdelning i uppgifter som går att bygga en i taget.

**Jira:** DRAKEN-4733 (story) med DRAKEN-4734…4750 som deluppgifter. Avsnittsnumren nedan refereras från
respektive deluppgift, och kopplingen mellan uppgifterna i §10 och Jira-nycklarna står i tabellen där.

**Öppen fråga:** när RabbitMQ blir produktionsklar (§2.4). Den blockerar inte T1–T8.

---

## 0. Beslutslogg

| # | Beslut | Valdes bort | Skäl |
|---|---|---|---|
| 1 | Outbox + REST-anrop via WSO2, med direktkörning av relayet så fort ärendet sparats | Direkt mot RabbitMQ; SSE; att processen frågar efter ändringar | Outboxen behövs ändå, annars kan ärendet sparas utan att händelsen skickas. **AMQP är målbilden** |
| 2 | Publiceringen hängs in i `EventService.createErrandEvent` | Att hänga in den i `ErrandService` och jämföra revisioner | Intaget skapar inga revisioner, så processen hade varit blind för kompletteringar |
| 3 | Filtrera på händelsens typ och subtyp | Jämföra JSON-fält mellan versioner | Fungerar oavsett hur ändringen kom in, och mekanismen finns redan för notisprenumeranter |
| 4 | Konfiguration i befintlig `namespace_config` | Ny tabell; `application.yml` | Nycklarna driver redan beteende. Flervärd per nyckel. Cache + CRUD finns |
| 5 | Etikett → process via `metadata_label_attribute[processKey]`, utan trädtraversering | Matcha på etikettens sökväg; ärva nyckeln nedåt i trädet | Med arv nedåt kan en process börja gälla för ett ärende bara för att någon flyttat om i metadatan |
| 6 | Ett ärende hör till en processtyp | Flera processtyper på samma ärende | En tillsyn är ett nytt ärende. Då kan databasen hålla regeln i stället för att vi ska komma ihåg den |
| 7 | **Optimistisk kontroll med `errand.version` och `If-Match`** — krocken upptäcks när någon skriver | Pessimistiskt lås på ärendet med utgångstid, spärrar i skrivvägarna och `423` | Målet är att ändringar inte ska tappas, inte att handläggaren ska hindras. Och maskineriet finns redan i drift — §6.2 |
| 8 | **En** skrivning bär både tillstånd och aktiviteter | Var sin endpoint | Färre anrop, en transaktion och en rapport per arbetssteg |
| 9 | Inga processvariabler för att hålla reda på vad som redan gjorts | `updateAvailable`, versionsräknare | Läs–ändra–skriv med inbyggd kapplöpning. Skyddet ligger i strukturen i stället |
| 10 | `errand.status` frikopplat från processens faser | Process äger status | Skilda begrepp. Listvyn läser processläget ur `errand.process` |
| 11 | SM:s `phase`/`errand_phase` används inte för ALKT | Återanvänd fasmodellen | Processmodellen ägs av BPMN |
| 12 | pw-alkt:s `start`- och `update`-endpoints tas bort | Att låta dem ligga kvar som utfasade | Ingen använder dem, och med två sätt att starta en process kommer någon förr eller senare att välja fel |
| 13 | **`ProcessStatus.isTerminal()` som metod på enumet** | En separat lista över vilka statusar som räknas som avslutade | Den styr `active_marker` och därmed regeln om en process per ärende. `WAITING` är fällan — se §4.1 |
| 14 | **Fem statusvärden**, där `START_FAILED` blir `FAILED` med en felkod | Åtta värden | Skillnaden syns redan på att `processInstanceId` saknas |
| 15 | **Modelleringsregel: inga parallella grenar som ändrar ärendet** | Ingen regel alls | Två grenar som skriver till samma ärende slår ut varandra med `412` och kommer aldrig i mål — §6.4 |
| 16 | `X-Sent-By` används för loop-skyddet, inte för behörighet | En skrivspärr baserad på vem som anropar | Med optimistisk kontroll finns ingen spärr att sätta — `@Version` fångar de krockar som faktiskt uppstår |
| 17 | **Ett publiceringsfel märker transaktionen som `rollback-only`** | Lita på att anroparen för felet vidare | Alla anropsställen sväljer undantag från `createErrandEvent` (§1.7). Utan det här är outboxen inte transaktionell |
| 18 | **Start styrs av `processKey`, inte av `eventType`** | Start endast på `CREATE` | Ärendet som får sin etikett i ett andra anrop hade annars aldrig startat — §7.1 motiverar triggern, §9.3 utför den |
| 19 | Aktiviteter får sakna processinstans: `errand_process_id` är nullbar och läsningen sker per ärende | `not null` | Tvetydiga etiketter och nödbromsen slår till innan någon instans finns. Posten hade helt enkelt inte gått att spara |
| 20 | Arbetssteg som bara läser skickar med `errandVersion`; SM svarar `412` om ärendet hunnit ändras | Inget skydd alls för läsande steg | Ett steg som aldrig skriver har annars ingenting att krocka på — §6.3 |
| 21 | **`POST .../processes` skapar, men uppdaterar aldrig** | `409` så snart det finns en rad | Ett arbetssteg kan hinna rapportera före pw:s `POST`. När `POST` bara skapar spelar ankomstordningen ingen roll, och `409` betyder bara en enda sak — §5.1 |
| 22 | **Ett avslutat processliv startas aldrig om.** `FAILED` får startas om | Starta på nästa triggande händelse oavsett historik | `findProcessInstances` ser bara det som kör i Operaton just nu, så en `COMPLETED` process ser ut som ingen process alls. Nästa process är ett nytt ärende — §7.4 |
| 23 | `errand.process` visar den **senaste** instansen | Den som lever just nu | En misslyckad start lämnar ingen levande instans efter sig, och då hade handläggaren inte sett någonting alls — §5.3 |
| 24 | **Beslutet är en egen ärendescopad resurs, `errand_decision`, med fasta fält** | `json_parameter` med registrerat schema; kolumner på `errand_process`; vanliga parametrar; aktivitetsloggen | Ett ärende, ett beslut — och `uq_ed_errand_id` *är* den regeln. Ett myndighetsbeslut har dessutom en form som följer av förvaltningslagen och förtjänar riktiga fält, och det är ärendedata som ska gå att läsa utan att man känner till processen. Se §7.5 |
| 25 | **Både handläggare och process får fatta beslutet; `method` skiljer dem åt** | Bara handläggaren; bara processen | Ett delegationsbeslut kan vara automatiserat, men vilket det var måste gå att svara på i efterhand (FL 28 §). Följden: `DECISION` måste vara `PROCESS_TRIGGER`, och `method` valideras mot identiteten — §7.5 |
| 26 | `processKey` hämtas från instansen först och från etiketterna i andra hand; `DELETE` skickas även utan nyckel | Att alltid läsa nyckeln ur etiketterna | En borttagen etikett skulle annars lämna en processinstans kvar i Operaton för ett ärende som inte längre finns — §2.2 |
| 27 | **Resursen heter `processes` och modellen `ErrandProcess`** | `process-instances`; `process-info` | Modellen ska kunna bära även processer som inte körs i Operaton, och kodbasens övriga subresurser heter något i plural. `process-info` går inte att böja i plural och hade dessutom låst oss vid en rad per ärende |
| 28 | **`GET /process-labels` byggs inte** | En egen endpoint som visar vilken etikett som startar vilken process | `GET /metadata/labels` lämnar redan tillbaka `attributes` med `id` och `resourcePath`. Den fråga man faktiskt ställer i drift gäller dessutom ett enskilt ärende och besvaras av aktivitetsloggen — §5.2. Indexet `idx_metadata_label_attribute_key` behövs därmed inte heller |
| 29 | **Manuell stegning sker med namngivna signaler, och valet manuellt eller automatiskt ligger i processmodellen** | En inställning per namespace; att handläggaren sätter processens läge direkt | En inställning i SM kan säga en sak medan modellen gör en annan. Signalen är dessutom en begäran, inte ett kommando — processen avgör, så lagstadgade steg går inte att kliva förbi (§5.9) |
| 30 | **Signalen bär bara ett namn, ingen fritext** | Ett kommentarsfält på signalen | Aktivitetsloggen gallras efter 365 dagar medan ärendet lever längre, och `message` får inte innehålla personuppgifter. Motiveringen hör hemma i ärendeanteckningar (§5.9) |

---

## 1. Vad vi vet om koden i dag

Allt i det här avsnittet är efterkontrollerat i kodbasen, inte antaget. Resten av dokumentet vilar på det.

### 1.1 `EventService` är vägen allt går igenom

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

`EventSubType` (befintlig enum): `ATTACHMENT, DECISION, ERRAND, HANDOVER_IN, HANDOVER_OUT, MESSAGE, NOTE, SYSTEM, SUSPENSION`. `EventType` kommer från eventlog-specen. Enumet utökas med `SIGNAL` för manuell stegning (§5.9) — ett rent tillägg som inte rör befintliga värden.

### 1.2 Halva leveransmaskineriet finns redan

`EventService.saveDispatchEntry` (outbox-rad), `NotificationDispatchEntity` (fälten `retryCount`/`nextRetryAt`/`deadLetter`, indexet `idx_dispatch_dead_letter_retry`), `NotificationDispatchScheduler.processDispatch` (ShedLock + hälsoindikator), `NotificationDispatchWorker.processGroup`/`handleFailure` (retry/backoff/dead letter), `.subscriberWantsEventType` (eventtypfilter), `.isExecutingUser` (hoppa över upphovet).

Men ta inte med allt rakt av. `NotificationDispatchRepository.findProcessable` saknar `LIMIT`, och `NotificationDispatchWorker.TRANSACTION_BUFFER_SECONDS` är satt till 10 sekunder. Värst är att `cleanUpDeadLetters` **raderar** de misslyckade leveranserna efter sju dagar, utan någon chans att köra om dem — se §8.3.

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
- `ErrandPhaseService.processPhaseChange` och `.validateStatusAgainstActivePhase` returnerar direkt när ingen fas finns ⇒ oanvänd fasmodell kostar noll.
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

Loop-skyddet bygger på att SM kan se vem som skrev, så det är värt att veta precis hur den identiteten
läses. Kontrollerat mot dept44-starter 8.0.8 (`se.sundsvall.dept44.support.Identifier`):

- **En okänd typ i headern avvisas inte.** `Identifier.parse` känner igen `partyId` och `adAccount`, och
  gör om allt annat till `CUSTOM` med typsträngen kvar. `X-Sent-By: pw-alkt; type=processEngine` ger
  alltså värdet `pw-alkt`, och det är den strängen loop-skyddet jämför mot (§6.5).
- **Däremot måste headern ha exakt två delar** — värdet och `type=`, åtskilda med semikolon. Är den
  felskriven lämnar `parse` tillbaka `null`, identiteten är borta, och loop-skyddets första lager slutar
  fungera utan att något annat märks. Det är därför T8 provar just det fallet.
- `ServiceUtil.getExecutingUser()` ger hela identiteten. `getAdUser()` ger `null` för allt som inte är ett
  AD-konto.

Det sista märks på ett ställe: `EventService.createNotification` hämtar avsändaren med `getAdUser()`.
När pw skriver blir den `null` och notisen till handläggaren står utan avsändare. Inget går sönder —
`NotificationService` kollar med `hasText` först — men fältet blir tomt, och det är lätt gjort att låta det
falla tillbaka på identitetens värde i stället (T3).

ALKT-namespacet körs utan etikettbaserad åtkomstkontroll, så pw:s identitet filtreras inte mot AccessMapper.

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
2. executedBy == PROCESS_CONSUMER?                        ja  -> return        (loop-skydd, lager 1)
3. Levererade event för ärendet i fönstret > tröskel?      ja  -> ERROR-aktivitet, return  (lager 3)
4. eventSubType i PROCESS_TRIGGER?                        nej -> return        (lager 2)
5. processKey: instansens om den finns, annars ur etiketterna
                       0 -> DELETE publiceras anda, ovriga return; >1 -> ERROR-aktivitet, return
6. INSERT process_event_outbox
```

Steg 1 är en uppslagning i en cachad map. Ett namespace utan process betalar alltså ingenting mer än så.

ERROR-aktiviteterna i steg 3 och 5 skrivs **utan processinstans** — de inträffar per definition när
ingen instans finns (§3.1).

**Steg 5 läser instansens `process_key` först, etiketterna bara i andra hand.** Så snart ärendet har en
processinstans är nyckeln fastnaglad, och en etikett som ändras eller tas bort kan inte längre ändra vad
som publiceras. Det spelar särskilt roll för `DELETE`: löstes nyckeln alltid ur etiketterna skulle ett
ärende vars etikett hunnit tas bort inte ge någon rad alls, och processinstansen leva vidare i Operaton
för ett ärende som inte finns (§9.3).

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

| | Direktkörning | Cronjobb |
|---|---|---|
| Startas av | att ärendet just sparats | klockan, `0 * * * * *` |
| Syfte | latens: sekunder i stället för upp till en minut | att ingenting blir liggande |
| Får utebli? | Ja, utan att något går förlorat | Nej — det är sanningen i systemet |

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
Feign-mål i SM. Själva bytet är däremot litet: allt utbyte sker i en enda metod,
`ProcessEventDelivery.deliver(row)`. Varje REST-konsument vi bygger innan bytet är arbete vi slänger.

Med *driftklar* menar vi: quorum queues på minst tre noder, DLX/DLQ med `x-delivery-limit`, egen vhost per
miljö, en användare per tjänst med rättighetsregler, TLS, övervakning av kölängd, obekräftade meddelanden,
DLQ-djup och nodstatus — samt en dokumenterad väg tillbaka när något gått fel.

---

## 3. Datamodell

### 3.1 Tabellerna

Fem nya tabeller i tre migreringar: `V1_47__add_process_integration_tables.sql` med de tre första (byggs i
T1), `V1_48__add_errand_decision.sql` med beslutet (T9) och `V1_49__add_errand_process_signal.sql` med de
väntade signalerna (T11). De ligger i var sin fil eftersom Flyway jämför checksumma — en migrering som
redan körts går inte att fylla på i efterhand.

```sql
-- 1. Outbox. Medvetet UTAN FK mot errand: ett DELETE-event maste overleva att arendet raderas.
create table if not exists process_event_outbox (
    id                varchar(36)  not null,
    municipality_id   varchar(8)   not null,
    namespace         varchar(32)  not null,
    errand_id         varchar(36)  not null,
    -- Nullbar: kravs for CREATE och UPDATE, irrelevant for DELETE dar pw matchar
    -- pa businessKey. Se 2.2 steg 5.
    process_key       varchar(128),
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

-- V1_48: arendets beslut. Ett per arende - unikheten AR invarianten (7.5). Arendedata, inte
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

-- V1_49: vad processen just nu vantar pa fran handlaggaren (5.9). Ersatts i sin helhet vid
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

| Tabell | FK | Motiv |
|---|---|---|
| `process_event_outbox` → `errand` | **Ingen, medvetet** | Samma val som `notification_dispatch` (`V1_38`). Ett DELETE-event måste överleva att ärendet raderas |
| `errand_process` → `errand` | `ON DELETE CASCADE`, **ingen JPA-relation på `ErrandEntity`** | DB-kaskaden räcker för att undvika föräldralösa rader vid `repository.deleteById` (`ErrandService.deleteErrand`). JPA-mappning vore aktivt skadlig (§1.4) |
| `errand_process_activity` → `errand_process` | `ON DELETE CASCADE`, **nullbar** | Poster utan instans måste kunna skrivas (§4.2) |
| `errand_process_activity` → `errand` | `ON DELETE CASCADE` | Krävs när instans-FK:n är nullbar — annars överlever instanslösa poster ärendet. InnoDB tillåter båda kaskadvägarna parallellt |
| `errand_decision` → `errand` | `ON DELETE CASCADE`, **med JPA-relation på `ErrandEntity`** | Motsatt val mot raderna ovan, och avsiktligt. Beslutet ska ligga i ärendets aggregat och därmed i revisionssnapshotten (§7.5). Det skrivs en handfull gånger per ärendes livstid, inte per arbetssteg — inget revisionsbrus |
| `errand_process_signal` → `errand_process` | `ON DELETE CASCADE` | Signalerna är processens tillstånd, inte ärendedata. Försvinner processraden ska de följa med |
| `errand_decision` → `errand_process` | `ON DELETE SET NULL`, **nullbar** | Spårbarhet till processen som fattade beslutet. Nullbar för manuella beslut och för ärenden helt utan process; `SET NULL` för att beslutet inte får försvinna med processraden |

**Retention:** aktiviteter röjs på `created` (default 365 d); outbox-rader röjs när `delivered_at` är äldre än **max(loop-guard-fönstret × 6, 24 h)**; dead letters enligt §8.3. Beslutet röjs aldrig separat — det följer ärendet.

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
`PHASE`, `TASK` och `INCIDENT`. SM skriver själv `CONFIG` när etiketterna pekar åt två håll och
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

| Utfall i Operaton | Kropp | SM svarar |
|---|---|---|
| Start lyckades | `processKey`, `processInstanceId`, `processStatus: RUNNING` | Raden saknas ⇒ `201` med `Location`. Raden finns redan med **samma** `processInstanceId` ⇒ `200`, och ingenting ändras. Annan **levande** instans för ärendet ⇒ `409` |
| Start misslyckades | `processKey`, `processStatus: FAILED`, `error` — **inget** `processInstanceId` | Terminal rad skapas ⇒ `201` |

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
GET .../errands/{errandId}/processes            -> 200 List<ErrandProcess>
                                                   nyast först; normalt exakt ett element
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
  "occurredAt": "2026-08-19T09:12:03.221+02:00"
}
```

**Meddelandet innehåller ingen ärendedata.** Behöver pw veta vad som står i ärendet hämtar det `GET /errands/{errandId}` självt. Det håller personuppgifter borta från outboxen, loggarna och en framtida meddelandekö — och det gör att en försenad leverans aldrig kan råka skicka ut en gammal bild av ärendet.

| Svar | När |
|---|---|
| `202 Accepted` | Hanterat, eller medvetet ignorerat (okänt ärende, mismatch, DELETE utan instans) |
| `422 Unprocessable Entity` | `processKey` matchar ingen driftsatt processmodell. Felet är **permanent** — det hjälper inte att försöka igen |
| `5xx` | Tillfälligt fel, försök igen |

```java
public class ErrandEvent {
    @NotBlank private String eventId;
    @NotNull  private EventType eventType;      // CREATE | UPDATE | DELETE
    private String eventSubType;
    @ValidUuid private String errandId;
    private String processKey;                  // instansens nyckel, annars etiketternas. Null vid
                                                //  DELETE av arende utan losbar nyckel. Styr start (9.3)
    private OffsetDateTime occurredAt;
}
```

### 5.5 Det pw-alkt rapporterar tillbaka

```java
/** Returneras av varje arbetssteg. Basklassen skickar den vidare till SM. */
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

Fabriksmetoderna finns just för att den som skriver ett arbetssteg inte ska behöva hålla reda på vilka statusar som räknas som avslutade.

### 5.6 Vilka svar SM ger

De två skrivvägarna släpps in på olika sätt, och det är med flit. **Processrapporten** godtas bara från
namespacets `PROCESS_CONSUMER`, utpekad med `X-Sent-By` — den bär processens tillstånd och inget
ärendeinnehåll. **Beslutet** går den vanliga vägen för ärendeskrivningar, eftersom det *är* ärendedata.

**`.../processes`**

| Kod | När |
|---|---|
| `400` | `activePhaseId` sätts på ärende med processinstans; etikettändring som byter `processKey`; `processInstanceId` i `PUT`-kroppen skiljer sig från pathens |
| `403` | `X-Sent-By` saknas; `processService` matchar inte namespacets `PROCESS_CONSUMER` |
| `404` | Ärendet finns inte eller ligger i annat namespace |
| `409` | Annan levande instans för ärendet **med ett annat `processInstanceId`**; ärendet har redan en `COMPLETED` instans (§7.4); instans med annat `process_key` än ärendets befintliga. Samma `processInstanceId` är aldrig `409` — se §5.1 |
| `412` | `errandVersion` i rapporten matchar inte ärendets aktuella version (§6.3) |

Statuskoderna för `.../signals` står i §5.9.

**`.../decision`**

| Kod | När |
|---|---|
| `400` | Kroppen validerar inte; `attachmentId` pekar på en bilaga som inte tillhör ärendet |
| `403` | `method: AUTOMATIC` från någon annan än namespacets `PROCESS_CONSUMER`; `method: MANUAL` från en icke-AD-identitet (§7.5) |
| `404` | Ärendet finns inte; eller, för `GET`/`DELETE`, inget beslut registrerat |
| `409` | Ärendet har en `COMPLETED` process — beslutet är låst (§7.5) |
| `412` | `If-Match` matchar inte beslutets `version` |

### 5.7 Vad som ändras för dem som redan använder API:et

**Ingenting.**

Inga nya statuskoder, inga nya spärrar och inget nytt felfall att hantera i gränssnittet. Den optimistiska
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

| Kod | När |
|---|---|
| `202` | Signalen är registrerad och publicerad |
| `400` | `signal` saknas eller är tom |
| `404` | Ärendet finns inte, eller har ingen levande processinstans |
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
2. **En händelse med subtypen `SIGNAL`**, som blir en outbox-rad och når pw. Det kräver att `SIGNAL` ligger
   i `PROCESS_TRIGGER` för namespacet (§7.1) — utan den publiceras ingenting och knappen blir en attrapp.
3. Loop-skyddets första lager släpper igenom, eftersom avsändaren är en handläggare och inte namespacets
   `PROCESS_CONSUMER` (§6.5).

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

## 6. Samtidighet och loopar

### 6.1 Rapporten styr livscykeln

| Rapporterad status | `active_marker` |
|---|---|
| `RUNNING` | 1 |
| `WAITING` | 1 |
| `RETRYING` | 1 |
| `COMPLETED` | NULL |
| `FAILED` | NULL |

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

**Lager 1 — vem gjorde det?** Är `executedBy` (alltså `X-Sent-By`) samma som namespacets
`PROCESS_CONSUMER` skrivs ingen outbox-rad. Processens egna ändringar väcker med andra ord inte processen.
Filtret sitter vid **publiceringen**, inte vid leveransen: raden ska aldrig skrivas, för annars räknar
nödbromsen i lager 3 fel.

**Lager 2 — vad hände?** Bara de händelsetyper som står i `PROCESS_TRIGGER` går vidare. Det lagret bryr sig
inte om vem som skrev, bara om vad som ändrades, och kompletterar därför lager 1.

**Lager 3 — nödbromsen.** Den bryr sig varken om vem eller vad, utan bara om takten: räkna raderna med
`errand_id = ? and created > now() - fönstret`. Går det över tröskeln (20 stycken på 10 minuter) skrivs
ingen rad, en felpost hamnar i aktivitetsloggen och hälsoindikatorn slår om till unhealthy.

> Räkningen förutsätter att outbox-rader **markeras som levererade i stället för att raderas**
> (`delivered_at`). Städade vi bort dem direkt hade en snabb loop aldrig lämnat mer än en rad efter sig,
> och bromsen vore verkningslös precis när den behövs.

Vad som *inte* fungerar som loopskydd: att jämföra versioner. Versionen stiger ju för varje varv.

**Och en ärlighet om lager 1:** det bygger på en header som avsändaren själv sätter. En pw-tjänst som
glömmer `X-Sent-By` loopar tills lager 2 eller 3 fångar den. Motmedlen är att en `RequestInterceptor` sätter
headern på **alla** utgående anrop (P3), och att `process_event.suppressed{reason=ORIGIN}` ska visa värden
skilda från noll i drift. Står den på noll skriver processen antingen ingenting alls, eller så är headern fel.

## 7. Konfiguration, processval och beslut

### 7.1 Vad som konfigureras per namespace

Två nycklar i den befintliga `namespace_config` styr allt: vem som kör processen och vilka händelser som
är värda att skicka vidare.

| Nyckel | Typ | Antal | Värde |
|---|---|---|---|
| `PROCESS_CONSUMER` | STRING | 1 | `pw-alkt` |
| `PROCESS_TRIGGER` | STRING | N | `ERRAND`, `MESSAGE`, `ATTACHMENT`, `DECISION`, `SIGNAL` |

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
  (42, 'PROCESS_TRIGGER',  'DECISION',   'STRING'),
  (42, 'PROCESS_TRIGGER',  'SIGNAL',     'STRING');
```

**`ERRAND` måste vara med.** Utan den startar aldrig ett ärende som får sin etikett först i ett andra
anrop. Men triggern räcker inte ensam — pw måste också starta processen för att `processKey` finns med,
inte för att händelsen råkar vara ett `CREATE`. Annars faller samma fall bort på mottagarsidan i stället.
Se §9.3.

**`DECISION` är lika obligatorisk.** Beslutet skrivs till `.../errands/{errandId}/decision` (§7.5) och
processen väntar på det. Saknas triggern publiceras ingen outbox-rad för den skrivningen, processen får
aldrig veta att beslutet är fattat, och instansen står kvar i `WAITING` för alltid.

**`SIGNAL` behövs så snart modellen har manuella grindar** (§5.9). Utan den publiceras handläggarens
knapptryck aldrig, och knappen blir en attrapp som ser ut att fungera. Ett namespace vars processer stegar
helt automatiskt behöver den inte.

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
  consumers: [pw-alkt]                      # registret; namnet matchas mot X-Sent-By
scheduler:
  process-event:   { cron: "0 * * * * *", name: processEvent,   lockAtMostFor: PT2M }
  process-cleanup: { cron: "0 30 2 * * *", name: processCleanup, lockAtMostFor: PT10M }
```

`consumers` är **registret över kända processkonsumenter**, inte en mappning. Namnet är identiteten:
det är samma sträng som Feign-målet under `integration`, som OAuth2-registreringen, som `X-Sent-By`-värdet
loop-skyddet jämför mot (§6.5) och som `PROCESS_CONSUMER` i `namespace_config` pekar ut. Registret finns
för att en felstavad `PROCESS_CONSUMER` ska avvisas vid skrivning i stället för att tyst sluta fungera —
utan det går processkonsumenter inte att skilja från övriga poster under `integration`.

I `application-it.yml` sätts samtliga cron till `"-"`.

### 7.3 Så vet SM vilken process ett ärende hör till

Svaret ligger i attributet `processKey` på etiketten. Vi tittar bara på ärendets egna etiketter och går
alltså **inte** uppåt eller nedåt i etikettträdet.

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

Etiketter som är märkta `deprecated` räknas inte. **SM kontrollerar inte att nyckeln finns på riktigt** — det är bara pw som vet vilka processer som är driftsatta, och en nyckel som inte finns fångas som `422` (§5.4).

### 7.4 En process per ärende — och hur den regeln hålls

Fyra regler tillsammans:

1. En etikettändring som skulle peka ut en annan `processKey` avvisas med `400` så snart ärendet har en processrad — även om den processen är avslutad.
2. Högst en **levande** instans per ärende. Den regeln bär databasen själv via `active_marker` (§4.1).
3. Alla instanser på samma ärende har samma `process_key`. Den kontrollen får tjänstelagret göra under radlås; den går inte att uttrycka i databasen.
4. **När en instans blivit `COMPLETED` är ärendets processliv slut.** Ingen ny instans får startas — nästa process är ett nytt ärende (beslut 6). En `FAILED` instans stoppar däremot ingenting; att försöka igen efter en misslyckad start är återhämtning.

Regel 4 går inte heller att lägga i databasen, eftersom `active_marker` är NULL för både `COMPLETED` och `FAILED` och alltså inte skiljer dem åt. Kontrollen ligger därför i `POST .../processes` (§5.1). Att den ligger i SM och inte bara i pw är medvetet: pw frågar Operaton om vad som kör just nu, och där syns inte avslutade processer alls (§9.3).

Krockar med de unika nycklarna **måste översättas till begripliga svar** och aldrig bubbla upp som `500`. De två betyder dessutom olika saker och ska inte behandlas lika:

| Constraint | Betydelse | Utfall |
|---|---|---|
| `uq_ep_process_instance_id` | Någon registrerade **samma** instans först — kapplöpningen i §5.1 | Läs om raden, returnera den. Inget fel |
| `uq_ep_one_active_per_errand` | En **annan** levande instans blockerar | `409` med `detail` som pekar ut den |

Och eftersom en och samma insert kan träffa båda, avgörs svaret av en uppslagning på `process_instance_id` — inte av vilken nyckel som råkade slå till först (§5.1).

---

### 7.5 Beslutet

**Ett ärende, en processinstans, ett beslut.** Ska ett nytt beslut fattas skapas ett nytt ärende, kopplat
till det ursprungliga. Kopplingen finns redan: `POST /errands` tar `referredFrom` och `ErrandService.createErrand`
skapar relationen via `RelationClient` — samma väg handover använder.

Det är den yttersta av tre regler som säger ungefär samma sak, fast på olika nivåer:

| Nivå | Invariant | Upprätthålls av |
|---|---|---|
| Processinstans | Högst en levande per ärende | `uq_ep_one_active_per_errand` |
| Processliv | En `COMPLETED` startas aldrig om | `hasCompletedProcess` i `POST` (§7.4) |
| Beslut | Ett per ärende | `uq_ed_errand_id` |

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

| Fall | `method` | Vem skriver | Behörighet |
|---|---|---|---|
| Handläggaren fattar beslutet | `MANUAL` | ett AD-konto | RW på ärendet, som vid vilken annan ärendeskrivning som helst |
| Processen fattar beslutet självt | `AUTOMATIC` | namespacets `PROCESS_CONSUMER` | `X-Sent-By` pekar ut samma tjänst som `PROCESS_CONSUMER` |

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
   (`executedBy` är samma som `PROCESS_CONSUMER`, §6.5). Också rätt: processen behöver inte väckas av sitt
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

## 8. Drift och förvaltning

### 8.1 Mätvärden som måste finnas

Utan dem går varken loop-skyddet eller samtidigheten att följa i drift — man märker att något är fel
först när någon hör av sig.

| Mätvärde | Varför |
|---|---|
| `process_event.suppressed{reason=ORIGIN\|TRIGGER\|GUARD}` | Ett tyst loop-skydd som slutar fungera märks annars först när loopen är där. `ORIGIN` ska vara **skild från noll** |
| `process_event.published`, `.delivered`, `.dead_lettered` | Leveranshälsa |
| `process_event.publish_failed` | Publicering som inte kunde rullas tillbaka (ingen aktiv transaktion, §2.2). Ska vara noll |
| `process_event.direct_run_rejected` | Trådpoolen är full, så direktkörningen hoppades över och leveransen får vänta på cronjobbet (§2.3) |
| `process.errand_conflict` | **Viktigaste driftindikatorn.** Hur ofta process och handläggare krockar (`412`). Stiger den arbetar processen på ärenden som redigeras samtidigt, och arbete görs om i onödan |
| `process.concurrent_task_detected` | Brott mot modelleringsregeln i §6.4 |
| `process.start_failed` | Feltaggade etiketter |
| `decision.written{method=MANUAL\|AUTOMATIC}` | Hur många beslut som fattas av maskin respektive människa. Krävs för att kunna svara på frågan i efterhand (§7.5), och en oväntad rörelse i `AUTOMATIC` är den tidigaste signalen på att en process fattar beslut den inte borde |
| `process.signal_sent{signal=...}` | Hur ofta handläggaren stegar processen manuellt, och vid vilken grind. Står en grind still trots att ärenden köar där, är det ofta gränssnittet som inte visar knappen |
| `process.signal_rejected{reason=UNKNOWN\|TERMINAL}` | Signaler som avvisats med `409`. En återkommande `UNKNOWN` betyder att gränssnittet visar en knapp processen inte längre väntar på |
| `decision.rejected{reason=METHOD\|LOCKED}` | Avvisade beslutsskrivningar: fel `method` för identiteten (`403`) eller låst av `COMPLETED` process (`409`) |

Logga alltid `eventId`, `errandId`, `processInstanceId` och `X-Request-Group-Id` — dubbelleveranser blir då spårbara i efterhand. **Logga aldrig `justification`** — den innehåller personuppgifter (§11).

### 8.2 Vanliga frågor i drift — och svaren

| Fråga | Svar |
|---|---|
| Varför startade ingen process för ärendet? | `GET .../processes` tom + `errand.process` null. Läs `GET .../process-activities` — tvetydig etikett och nödbroms ligger där som `CONFIG`/`ERROR`-poster. Kontrollera sedan etikettens `processKey`-attribut via `GET /{municipalityId}/{namespace}/metadata/labels`, och att `PROCESS_CONSUMER` finns för namespacet |
| Varför går processen inte vidare fast handläggaren tryckt på knappen? | Kontrollera att `SIGNAL` ligger i `PROCESS_TRIGGER` (§7.1), att signalen står bland `errand.process.awaitingSignals`, och att väntläget i modellen lyssnar på just det namnet. Se `process.signal_rejected` |
| Varför syns ingen knapp för att gå vidare? | `errand.process.awaitingSignals` är tom. Antingen är väntläget automatiskt, eller så rapporterar pw inte in signalerna (§9.3) |
| Varför avslutas inte processen fast beslutet är fattat? | Kontrollera att `DECISION` ligger i `PROCESS_TRIGGER` (§7.1), att outbox-raden finns för beslutsskrivningen, och att väntläget läser om ärendet när det går in i väntan (§9.2 punkt 1) |
| Vem fattade beslutet på ärendet? | `errand.decision.method` och `.decidedBy`. `AUTOMATIC` betyder att processen fattade det; `processId` pekar ut vilken processrad |
| Varför kör processen om samma steg gång på gång? | Handläggaren ändrar ärendet mitt i steget ⇒ `412` (§6.2). Se `process.errand_conflict` och aktivitetsloggen |
| Varför väcks inte processen av inkommande e-post? | `MESSAGE` saknas i `PROCESS_TRIGGER` |
| Varför väcks processen inte av sina egna ändringar? | Det är meningen — lager 1 i §6.5 |
| Varför står instansen kvar som `RUNNING` fast inget händer? | Workern kraschade utan att rapportera. Operaton kör om task:en när dess eget lås löper ut; instansen uppdateras vid nästa rapport |

### 8.3 Misslyckade leveranser måste gå att köra om

`NotificationDispatchWorker.cleanUpDeadLetters` **raderar** sina misslyckade leveranser efter sju dagar. Ta inte efter det. En processhändelse som hamnat i dead letter betyder att en process aldrig fick veta något — raderar vi den blir felet både permanent och osynligt.

Därför:

- Misslyckade leveranser städas **inte** bort automatiskt inom bevarandetiden, och hälsoindikatorn står kvar på unhealthy så länge det finns oskickade rader.
- Det ska finnas en administrativ väg att försöka igen: `POST /{municipalityId}/{namespace}/process-events/{id}/redrive`, som nollställer `retry_count`, `dead_letter` och `next_retry_at`.
- Först efter den konfigurerade bevarandetiden (30 dagar som standard) städas de bort, och då med en loggrad om vad som försvann.

### 8.4 Prova själv, lokalt

1. Skapa namespace-config med `PROCESS_CONSUMER=pw-alkt` och `PROCESS_TRIGGER=ERRAND,MESSAGE`.
2. Tagga en label med `processKey=alkt-ansokan`.
3. `POST /2281/ALKT/errands` med den labeln ⇒ rad i `process_event_outbox` inom en sekund, `delivered_at` satt när stubben svarat.
4. `GET /2281/ALKT/errands/{id}` ⇒ `process.processStatus = RUNNING`, och `ETag` i svarshuvudet.
5. `PATCH` samma ärende med den ETag:en ⇒ `200`. `PATCH` igen med **samma** ETag ⇒ `412`.
6. Låt stubben rapportera med ett `errandVersion` som ligger efter ⇒ `412` på rapporten, inget tillstånd skrivet.
7. `PUT /2281/ALKT/errands/{id}/decision` med `method: MANUAL` ⇒ revision, `DECISION`-event och en outbox-rad (kräver `DECISION` i `PROCESS_TRIGGER`). Samma skrivning med `X-Sent-By: pw-alkt; type=processEngine` och `method: AUTOMATIC` ⇒ ingen outbox-rad, men beslutet skrivet.
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
2. **Manuella grindar modelleras som namngivna väntlägen** (§5.9). Ska handläggaren avgöra när processen går vidare räcker det inte att villkoret är uppfyllt — väntläget ska lyssna på ett *namngivet* meddelande, till exempel `granskning-godkand`. Finns flera vägar framåt används en event-based gateway med ett catch event per alternativ, så att handläggarens val också blir processens vägval. Sätt en tidsgräns på grindar som kan glömmas bort: ett väntläge som ingen någonsin klickar på står kvar för alltid.
3. **Inga user tasks.** Det är lätt att tro att ett väntläge på en människa ska vara en user task — det är BPMN-lärobokens svar. Här är det fel: handläggaren arbetar i SM och loggar aldrig in i Operaton. En user task skulle skapa en uppgiftslista som ingen tittar i, och grinden skulle aldrig öppnas. Manuella grindar är meddelandehändelser, ingenting annat.
4. **Inga parallella grenar som ändrar ärendet** (§6.4).
5. **Inga processvariabler som minne mellan väckningar.** Tillståndet bor i ärendet, och kontrollen läser om det varje gång — annars börjar processen tro saker som inte längre är sanna. Skälet står i den kod som nu tas bort: *"Clearing process variable has to be a blocking operation. Using ExternalTaskService.setVariables() will not work without creating race conditions."* Behåll resonemanget även när metoden är borta; det är det första någon återinför nästa gång ett dubblettproblem dyker upp.

    Ett **resultatvärde** är något annat och fullt tillåtet: det som ett arbetssteg sätter när det slutförs, `complete(task, variables)`, och som nästa gateway läser. Det skrivs atomiskt med slutförandet och har ingen kapplöpning i sig. Utan det gick det inte att ha gateways över huvud taget. Skillnaden är alltså: *resultat i ett steg är i sin ordning, minne mellan väckningar är det inte.*
6. **Processens slut måste rapporteras.** SM får bara veta att en process är klar genom en rapport, och rapporter kommer från arbetssteg. Slutar en gren utan att ett arbetssteg kört sist står SM:s rad kvar som `RUNNING` för alltid medan instansen är borta ur Operaton — och då kan ärendet varken gå vidare eller få en ny process. Antingen är sista steget före varje slutevent ett arbetssteg som rapporterar `completed()`, eller så hängs en execution listener på sluteventet som gör det. P6 stämmer av det som ändå glider isär.
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
(vantlage) -- vackt ---------+   manuell grind: message catch "granskning-godkand"
                                 automatisk:    message catch "errandUpdated"
                                 + boundary timer for paminnelse eller eskalering
```

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
        finns -> deleteProcessInstance(id, reason = "errand deleted in SM")
        202                                        // arendet ar borta i SM; ingen rapport tillbaka

    instans = findProcessInstances(errandId, event.processKey, "ALKT")
    om tom:
        om event.processKey saknas -> logga, 202       // arendet har ingen processetikett
        om SM har en COMPLETED instans -> logga, 202   // processlivet ar over, se 7.4
        om processKey inte ar driftsatt -> 422
        start med businessKey = errandId
        POST .../processes {RUNNING}                   // 200 = nagon hann fore, ok
                                                       // 409 = avslutad eller annan levande
                                                       //       instans -> avbryt den nystartade
    annars:
        messageName = (event.eventSubType == SIGNAL) ? event.signal : "errandUpdated"
        correlateMessage(messageName, businessKey = errandId, tenantId = "ALKT")
```

**`findProcessInstances` ser bara det som kör just nu.** En avslutad process finns inte där utan ligger i
Operatons historik. Utan kontrollen mot SM skulle därför varje ny händelse efter `COMPLETED` starta en helt
ny process på samma ärende — ett meddelande eller en bilaga som kommer in efter beslutet skulle dra igång
ansökningsprocessen från början igen. Kontrollen mot SM är den som ska fånga det; `409` från `POST` är
skyddsnätet bakom (§7.4).

**Det är `processKey` som avgör om en process ska startas, inte händelsetypen.** Ett ärende kan skapas utan
etikett och få den först i ett andra anrop, och då kommer nyckeln med ett `UPDATE` och inte ett `CREATE`.
Startade vi bara på `CREATE` skulle det ärendet aldrig få någon process, och `PROCESS_TRIGGER=ERRAND`
(§7.1) vore verkningslös för precis det fall den finns till för. Villkoret är alltså: ingen levande instans
**och** ett `processKey` satt.

**Ett `MismatchingMessageCorrelationException` (400) ska ge en INFO-rad och `202`, inte en ny leverans.**
Annars fylls kön av misslyckade leveranser med händelser som var helt normala.

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

`OperatonClient` behöver: `correlateMessage`, `findProcessInstances(businessKey, processDefinitionKey, tenantIdIn)`, `deleteProcessInstance`, och `businessKey` i `OperatonMapper.toStartProcessInstanceDto`.

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

---

## 10. Uppgifter att bygga

Varje uppgift går att slå ihop för sig, och acceptanskriterierna är skrivna så att de kan klistras rakt in
i en Jira-task.

Tabellen visar **i vilken ordning de bör göras**. Numreringen längre ner (T för SupportManagement, P för
pw-alkt) följer tjänst i stället för ordning.

| Steg | Jira | Uppgift |
|---|---|---|
| 1 | DRAKEN-4734 | T1 — Datamodell och domänenums |
| 2 | DRAKEN-4735 | T2 — Konfigurationsläsning |
| 3 | DRAKEN-4736 | T3 — Process-API |
| 4 | DRAKEN-4737 | T4 — Optimistisk samtidighetskontroll |
| 5 | DRAKEN-4738 | T5 — Publicering |
| 6 | DRAKEN-4739 | T6 — Relay och leverans |
| 7 | DRAKEN-4740 | T7 — Skyddsräcken |
| 8 | DRAKEN-4741 | T9 — Beslutet: modell, endpoint och spårbarhet |
| 9 | DRAKEN-4749 | T11 — Manuell stegning med signaler |
| 10 | DRAKEN-4742 | P1 — Operaton-klienten |
| 11 | DRAKEN-4743 | P2 — Event-endpoint och borttagning |
| 12 | DRAKEN-4744 | P3 — SM-klienten |
| 13 | DRAKEN-4745 | P4 — Workerstruktur |
| 14 | DRAKEN-4750 | P7 — Manuella grindar och väntade signaler |
| 15 | DRAKEN-4746 | T8 — `ProcessLoopGuardIT` |
| 16 | DRAKEN-4747 | P5 — Tillsynsprocessen |
| 17 | DRAKEN-4748 | P6 — Incidentåterkoppling |

### T1 — Datamodell och domänenums (SM)

**Bygg:** `V1_47`-migrering (§3.1); `ProcessStatus` med `isTerminal()` (§4.1); `ActivitySeverity`; entiteterna `ProcessEventOutboxEntity`, `ErrandProcessEntity` (§4.3), `ErrandProcessActivityEntity`; repositories med `Pageable` på **alla** sökfrågor; tabellerna i `truncate.sql`.

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
- Skrivning av okänd `PROCESS_CONSUMER` ger `400`. Registret är `process-engine.consumers` (§7.2), och namnet i det är identiteten — ingen separat `identifier`-egenskap som kan drifta från sin nyckel.
- Verifierat att `namespaceConfigCache` evikteras vid skrivning — annars går konfigurationen inte att ändra i drift, hur mycket den än ser ut att göra det.

### T3 — Process-API (SM)

**Bygg:** `ErrandProcessResource` (`PUT`, `POST`, `GET` under `.../errands/{errandId}/processes`) och ärendescopad `GET .../process-activities` med valfritt `processInstanceId`-filter (§5.2); `ErrandProcessService`; API-modellerna (§5.3); `Errand.process` + batchberikning i `readErrand`/`findErrands`; regenerera `openapi.yaml`. Fältet `awaitingSignals` på samma modell hör till T11 — bygg det inte här.

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
- Beslut fattat och dokumenterat om `process` och `decision` ska strippas av `limitedMappingPredicateByLabel`.

### T4 — Optimistisk samtidighetskontroll (SM)

**Bygg:** `errandVersion` i rapportmodellen och kontrollen mot `errand.version` i `ErrandProcessService` (§6.3); WARN-aktivitet och `process.concurrent_task_detected` när två skilda `externalTaskId` rapporterar `RUNNING` mot samma instans utan terminal rapport emellan (§6.4); `process.errand_conflict`.

**Acceptans:**
- Rapport med `errandVersion` som glidit ⇒ `412`, och **varken** tillstånd eller aktiviteter skrivs.
- Rapport utan `errandVersion` ⇒ ingen kontroll, `200`.
- Två skilda `externalTaskId` med `RUNNING` mot samma instans ⇒ WARN-aktivitet skriven, **båda** rapporterna tas emot.
- `ErrandProcessService` tar en injicerad `Clock`. **Inget test använder `Thread.sleep`.**
- Ett test bekräftar att `PATCH /errands/{id}` med föråldrad `If-Match` ger `412` — regressionsskydd för att hela samtidighetsmodellen vilar på befintligt beteende.

### T5 — Publicering (SM)

**Bygg:** `ProcessEventPublisher` anropad från `EventService.createErrandEvent`, med `setRollbackOnly` före kast (§2.2); `ProcessKeySelector` (§7.3); nödbromsen.

**Acceptans:**
- **IT som verifierar att ett e-postintag ger en outbox-rad.** Intaget skapar inga revisioner och går förbi den gemensamma passagen (§1.1) — tappas det där märks det inte av något annat test.
- Enhetstest per gren i §2.2, inklusive: `executedBy` == konsumenten ⇒ ingen rad; icke-triggad subtyp ⇒ ingen rad; namespace utan konsument ⇒ ingen rad.
- **`executedBy` == en handläggare ⇒ raden skrivs.** Origin-filtret får inte vara bredare än sitt syfte.
- **`DELETE` av ett ärende vars etikett tagits bort ⇒ raden publiceras ändå**, med `process_key` null. Utan det blir processinstansen föräldralös i Operaton.
- Ärende med processinstans: `process_key` i raden kommer från instansen, inte från etiketterna. Verifieras genom att ändra etiketten i testdata och se att nyckeln står still.
- `ProcessKeySelectorTest`: en tagg ⇒ en nyckel; två med samma ⇒ en; två med olika ⇒ ERROR-aktivitet och ingen rad; `deprecated` ignoreras; **namnbyte och omflyttning av labeln lämnar upplösningen oförändrad**.
- **Publisher kastar ⇒ ärendeskrivningen är inte committad**, trots att anropsstället sväljer undantaget (§1.7). Verifieras genom att PATCH:a och sedan läsa tillbaka ärendet — inte genom att inspektera loggen.
- Utan aktiv transaktion: ERROR-logg och `process_event.publish_failed` ökar, inget kast som spräcker anropet.
- Nödbromsen slår över tröskeln med rader som har `delivered_at` satt, och dess ERROR-aktivitet skrivs **utan** instans.
- `ProcessKeySelector` med två skilda nycklar skriver ERROR-aktivitet **utan** instans — testet får inte förutsätta att en instansrad finns.

### T6 — Relay och leverans (SM)

**Bygg:** paketet `service/scheduler/processevent/` med schemaläggare, jobb och relay; direktkörningen efter commit tillsammans med en trådpool med tak; `ProcessEngineClient` med ett lager som översätter felen; `422` som permanent fel och `5xx` som tillfälligt; endpointen för att köra om och reglerna för hur länge rader sparas (§8.3); mätvärdena i §8.1.

**Acceptans:**
- WireMock svarar `202` / `422` / `503` / timeout — samtliga fyra vägar verifierade, inklusive att `422` **inte** retryas och skriver `FAILED` + ERROR-aktivitet.
- Ordning per ärende hålls när flera rader finns.
- Full trådpool ⇒ direktkörningen hoppas över, `process_event.direct_run_rejected` ökar och cronjobbet levererar i stället. **Inget undantag når anroparen** — testet ska fylla kön och kontrollera att ärendeskrivningen ändå svarar `200` (§2.3).
- Dead letters raderas inte inom retentionstiden; redrive nollar räknarna.

### T7 — Skyddsräcken (SM)

**Bygg:** `400` på `activePhaseId` när ärendet har processinstans; `400` på etikettändring som byter `processKey` — **både i `ErrandService.updateErrand` och i `AddLabelAction.executeAction`**, som körs schemalagt och aldrig passerar API:t.

**Acceptans:**
- Båda `400`-fallen täckta av enhetstest och ett IT-fall vardera.
- `AddLabelAction` som skulle byta upplöst `processKey` på ett ärende med levande instans ⇒ etiketten läggs inte till, ERROR-aktivitet skrivs. Utan detta slutar processen tyst få väckningar (§11).

### T8 — `ProcessLoopGuardIT` (SM)

**Det viktigaste enskilda testet.** Kör hela varvet: ärende skapas ⇒ outbox-rad; stub agerar pw, rapporterar `RUNNING`, PATCHar ärendet med `X-Sent-By: pw-alkt` och rapporterar `WAITING`.

- **Kontrollera att pw:s egen PATCH inte gav någon ny outbox-rad** (lager 1).
- Kör sedan **samma** PATCH med en handläggaridentitet och kontrollera att den **ger** en rad. Filtret får inte vara så brett att äkta ändringar tystas — det felet är osynligt i drift tills någon undrar varför processen aldrig vaknar.
- Kör pw:s PATCH **utan** `X-Sent-By` och verifiera att lager 2 eller 3 fångar den.

Utan detta test är loop-skyddet en hypotes.

### T9 — Beslutet: modell, endpoint och spårbarhet (SM)

**Bygg:** `V1_48`-migreringen med `errand_decision` (§3.1) och `DecisionEntity` som `@OneToOne` på `ErrandEntity`; enums `DecisionOutcome` och `DecisionMethod` (§4.2); `Decision`-modellen (§5.3); `ErrandDecisionResource` (`GET`, `PUT`, `DELETE` med `If-Match`) och `ErrandDecisionService`; `method`-regeln mot identiteten; `EventSubType.DECISION`-event och revision från beslutsskrivningen; låsning mot `COMPLETED` process; `Errand.decision`; `DECISION` i `PROCESS_TRIGGER` för ALKT; mätvärdena i §8.1; regenerera `openapi.yaml`; tabellen i `truncate.sql`.

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
- Enhetstest som verifierar att beslutsskrivning från namespacets `PROCESS_CONSUMER` **inte** ger en outbox-rad — origin-filtret gäller även här.
- `justification` förekommer inte i någon loggrad (§8.1).

### T11 — Manuell stegning med signaler (SM)

**Bygg:** `V1_49`-migreringen med `errand_process_signal` (§3.1), entitet och repository; `ProcessSignal` i API:et och `awaitingSignals` på `ErrandProcess` (§5.3); `POST .../processes/{processInstanceId}/signals` (§5.9); aktivitetspost med `activityType = SIGNAL`; värdet `SIGNAL` i `EventSubType` och i `PROCESS_TRIGGER` för ALKT; mätvärdena `process.signal_sent` och `process.signal_rejected`; regenerera `openapi.yaml`; tabellen i `truncate.sql`.

**Acceptans:**
- En rapport med `awaitingSignals` ersätter tidigare rader helt — tas en signal bort ur rapporten försvinner den ur `errand.process`.
- Tomt `awaitingSignals` tömmer listan, och betyder att processen inte väntar på någon människa.
- Signal som står bland de väntade ⇒ `202`, aktivitetspost och outbox-rad med subtyp `SIGNAL`.
- Signal som **inte** står bland de väntade ⇒ `409`, och ingenting skrivs. Det är samtidigt dubbelklicksskyddet.
- Signal mot ärende utan levande process ⇒ `404`; mot avslutad process ⇒ `409`.
- Aktivitetsposten namnger avsändaren, så att "vem stegade processen förbi granskningen" går att besvara i efterhand.
- **Utan `SIGNAL` i `PROCESS_TRIGGER` publiceras ingen outbox-rad** — täck det med ett test, annars upptäcks felkonfigurationen först i drift.
- Berikningen av `awaitingSignals` gör **en** extra fråga för hela sidan, verifierat med frågeräkning.

### P1 — Operaton-klienten (pw)

`correlateMessage`, `findProcessInstances`, `deleteProcessInstance`; `businessKey` i mappern; nya konstanter. **Acceptans:** `ProcessWithoutDeviationIT` fortsatt grön.

### P2 — Event-endpoint och borttagning (pw)

`POST /process/errand-events` med logiken i §9.3, **och borttagningen i §5.8 i samma steg**. Regenerera `openapi.yaml`.

**Acceptans:** start / korrelera / okänt ärende / DELETE / okänd nyckel (`422`) täckta; inga referenser kvar till `updateAvailable`, `StartProcessResponse` eller `setProcessInstanceVariable`.
- **`UPDATE`-event utan levande instans men med `processKey` ⇒ processen startas.** Det är fallet där etiketten sattes i ett andra anrop (§7.1); startas bara på `CREATE` faller det tyst bort.
- `UPDATE`-event utan `processKey` ⇒ `202`, ingen start.
- **Event mot ärende vars process är `COMPLETED` ⇒ `202`, ingen ny start.** att fråga Operaton vad som kör just nu räcker inte som villkor — den ser inte avslutade instanser (§9.3).

### P3 — SM-klienten (pw)

`patchErrand`, `report(...)`, `getErrand`; `RequestInterceptor` för `X-Sent-By`/`X-Request-Group-Id`; uppdatera `support-management.yaml` och regenerera; WireMock-stubbar. `getErrand` måste returnera ärendets `ETag` tillsammans med kroppen, och `patchErrand` skicka den som `If-Match` (§6.2).

**Acceptans:**
- Test som verifierar att `X-Sent-By` sätts på **alla** utgående anrop, inte bara ett.
- `getErrand` följt av `patchErrand` skickar den ETag servern gav; stub som svarar `412` ger ett undantag som når `execute`.

### P4 — Workerstruktur (pw)

`AbstractTaskWorker` enligt §9.4; `ProcessStateReport` med fabriksmetoder; `FailureHandler` rapporterar `RETRYING`/`FAILED`.

**Acceptans:** IT verifierar ordningen `RUNNING` → PATCH → terminal rapport; ett fall där `executeBusinessLogic` kastar kontrollerar att `RETRYING` rapporterats; ett fall där SM svarar `412` på PATCH kontrollerar att steget körs om och att andra försöket läser om ärendet.

### P7 — Manuella grindar och väntade signaler (pw)

**Bygg:** korrelation på signalens namn när händelsens subtyp är `SIGNAL` (§9.3); rapportering av `awaitingSignals` när processen går in i ett väntläge, hämtat från Operatons event subscriptions om API:et tillåter det; manuella grindar i modellen enligt §9.2 punkt 2.

**Acceptans:**
- Händelse med subtyp `SIGNAL` korreleras på signalens namn; alla andra subtyper på `errandUpdated`.
- Signal som inte matchar något väntläge ⇒ informationsrad och `202`, ingen ny leverans.
- Efter varje avslutat arbetssteg innehåller rapporten de signaler instansen nu väntar på — och en tom lista när väntläget är automatiskt.
- **Verifierat och dokumenterat om Operatons REST-API kan lista event subscriptions.** Kan det inte det: vald väg för att få fram namnen är beskriven i uppgiften. Ta reda på det innan resten byggs.
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

| Risk | Hantering |
|---|---|
| **RabbitMQ-mognad** (öppen fråga) | §2.4. Varje REST-konsument byggd innan bytet är kastat arbete |
| **`WAITING` felaktigt behandlad som terminal** | Skulle bryta 1-1-invarianten tyst. Skyddas av `applyStatus` som enda väg + tabelldrivet test (T1) |
| **Handläggaren ändrar mitt i ett arbetssteg** | `412`, och steget körs om (§6.2). Kostar en omkörning. Mäts av `process.errand_conflict` |
| **Steg med extern sidoeffekt körs om** | Sidoeffekten kan dubbleras. Samma krav som när ett steg kraschar (§9.4) — lägg sidoeffekten sist i steget, eller gör den idempotent |
| **Läsande steg utan `errandVersion`** | Då finns inget skydd alls (§6.3). Det får vara ett medvetet val för varje steg — men ett val som faktiskt måste göras |
| **Underresurser fångas inte av versionen** | En bilaga som raderas mitt i en körning höjer inte `errand.version`. Processen måste läsa om bilagor när den behöver dem |
| **Parallella grenar** | Modelleringsregel + WARN-aktivitet gör brottet synligt (§6.4) |
| **Loop SM ↔ pw** | Tre lager (§6.5). Lager 1 vilar på en klientsatt header — därför är `process_event.suppressed{reason=ORIGIN}` ett mätvärde som **ska** vara skilt från noll |
| **Beslut skrivet medan processen arbetar** | Korrelationen sväljs och väckningen är borta. Fångas bara av modelleringskravet i §9.2 punkt 1 — väntläget måste läsa om ärendet när det går in i väntan. Ingen kod i SM kan rädda ett väntläge som inte gör det |
| **`DECISION` saknas i `PROCESS_TRIGGER`** | Processen vaknar aldrig av beslutet och står i `WAITING` för alltid. Validering av triggervärden och ett IT-fall i T9 |
| **Personuppgifter i beslutets motivering** | `justification` innehåller nästan alltid personuppgifter. Får aldrig loggas (§8.1), aldrig kopieras till aktivitetsloggen och aldrig läggas i outboxens nyttolast — den bär medvetet ingen ärendedata alls (§5.4). Beslutet kaskaderas bort med ärendet |
| **Automatiskt beslut felstämplat som manuellt, eller tvärtom** | `method` valideras mot identiteten vid systemgränsen och räknas i `decision.written` (§8.1). Utan bådadera går frågan "vilka beslut fattades av en maskin?" inte att svara på i efterhand — och det är en fråga som kommer att ställas |
| **Instans som försvinner utan slutrapport** | SM står kvar på `RUNNING` medan instansen är borta ur Operaton, och ärendet kan varken gå vidare eller få en ny process. Modelleringskravet i §9.2 punkt 5 ska hindra det; P6:s schemalagda kontroll stämmer av det som ändå glider isär |
| **Skelettmodellen driftsatt för tidigt** | Sex tomma subprocesser springer igenom på millisekunder. Startas den mot ett skarpt ärende är ärendets processliv förbrukat (§9.1). Driftsätt inte förrän väntlägena finns |
| **Manuell grind som ingen klickar på** | Processen står i `WAITING` för alltid. Modelleringsregeln i §9.2 punkt 2 kräver en tidsgräns på grindar som kan glömmas bort, och `process.signal_sent` visar vilka grindar som faktiskt används |
| **Signal som accepteras men aldrig konsumeras** | Går processen vidare på en timer i samma stund som handläggaren trycker, hinner SM svara `202` innan den nya bilden rapporterats. Signalen når då inget väntläge och är borta. Handläggaren ser det vid nästa omläsning, men vi räknar det inte någonstans — överväg ett mätvärde om det visar sig hända |
| **Knapp som inte längre gäller** | Handläggaren ser en signal processen hunnit lämna. Skrivningen ger `409` och räknas i `process.signal_rejected` — gränssnittet ska läsa om ärendet, inte försöka igen |
| **Beslut på ärende helt utan process** | Tillåtet och olåst: det finns ingen `COMPLETED` process att låsa mot. Spårbarheten bärs då av revisionen, inte av spärren (§7.5) |
| **Föräldralös processinstans efter radering** | `DELETE` publiceras även utan `processKey` (§2.2), och pw raderar på `businessKey` (§9.3). Restrisk kvarstår om leveransen dead-letteras — därför retention och redrive före röjning (§8.3) |
| **Etikettändring utanför API:t** | `AddLabelAction.executeAction` körs schemalagt och lägger till etiketter utan att passera någon endpoint. Byter den upplöst `processKey` slutar processen tyst få väckningar — T7:s kontroll måste ligga även där |
| **Publiceringsfel sväljs av anropsstället** | `setRollbackOnly` före kast (§2.2) gör svälj-fångsten ofarlig. Kvarstående hål: anropsväg helt utan transaktion — mäts av `process_event.publish_failed` |
| **Start uteblir när etiketten sätts sent** | Start villkoras av `processKey`, inte `eventType` (§9.3). Täckt av ett P2-fall |
| **Felstavat `processKey`** | Upptäcks vid första ärendet. `422` ⇒ ingen retry, `FAILED` + ERROR-aktivitet direkt på ärendet |
| **Dead letter raderas och felet blir permanent** | §8.3 — redrive-endpoint och retention före röjning |
| **Routingen går att ändra i drift, utan granskning** | Priset för att slippa en release varje gång. Validering av `PROCESS_CONSUMER`; överväg ändringslogg |
| **Personuppgifter i aktivitetsloggen** | `message` är fri text som kommer från processen. **pw måste instrueras att inte skriva personuppgifter där** — det är en regel, inte en spärr |
| **Dubbla processinstanser** | ShedLock-serialiserad leverans + businessKey-kontroll + `409` + DB-constraint. Restrisk i Operaton, som saknar unikhet på business key — men SM kan inte registrera resultatet |
| **Delas Operaton-tenanten `ALKT`?** | Påverkar `getDeployments`-assertions och `historyTimeToLive`. Bekräfta mot driftmiljön |

### Vad som inte går att verifiera automatiskt

- Att WSO2 släpper igenom med rätt scope, och att `If-Match`/`ETag` passerar oförvanskade.
- Verklig samtidighet mellan poddar — ShedLock täcks indirekt av `ShedlockConfigurationIT`.
- Långtidsbeteende hos outbox och aktivitetslogg. Kompensation: mätvärdena i §8.1.
