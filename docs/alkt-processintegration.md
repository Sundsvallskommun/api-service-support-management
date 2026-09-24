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

| #  |                                                                                                                                                                                             Beslut                                                                                                                                                                                              |                                                                             Valdes bort                                                                             |                                                                                                                                                                                                         Skäl                                                                                                                                                                                                         |
|----|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1  | Outbox + REST-anrop via WSO2, med direktkörning av relayet så fort ärendet sparats                                                                                                                                                                                                                                                                                                              | Direkt mot RabbitMQ; SSE; att processen frågar efter ändringar                                                                                                      | Outboxen behövs ändå, annars kan ärendet sparas utan att händelsen skickas. **AMQP är målbilden**                                                                                                                                                                                                                                                                                                                    |
| 2  | Publiceringen hängs in i `EventService.createErrandEvent`                                                                                                                                                                                                                                                                                                                                       | Att hänga in den i `ErrandService` och jämföra revisioner                                                                                                           | Intaget skapar inga revisioner, så processen hade varit blind för kompletteringar                                                                                                                                                                                                                                                                                                                                    |
| 3  | Filtrera på händelsens typ och subtyp                                                                                                                                                                                                                                                                                                                                                           | Jämföra JSON-fält mellan versioner                                                                                                                                  | Fungerar oavsett hur ändringen kom in, och mekanismen finns redan för notisprenumeranter                                                                                                                                                                                                                                                                                                                             |
| 4  | Konfiguration i befintlig `namespace_config`                                                                                                                                                                                                                                                                                                                                                    | Ny tabell; `application.yml`                                                                                                                                        | Nycklarna driver redan beteende. Flervärd per nyckel. Cache + CRUD finns                                                                                                                                                                                                                                                                                                                                             |
| 5  | Etikett → process via `metadata_label_attribute[processKey]`, utan trädtraversering                                                                                                                                                                                                                                                                                                             | Matcha på etikettens sökväg; ärva nyckeln nedåt i trädet                                                                                                            | Med arv nedåt kan en process börja gälla för ett ärende bara för att någon flyttat om i metadatan                                                                                                                                                                                                                                                                                                                    |
| 6  | Ett ärende hör till en processtyp                                                                                                                                                                                                                                                                                                                                                               | Flera processtyper på samma ärende                                                                                                                                  | En tillsyn är ett nytt ärende. Då kan databasen hålla regeln i stället för att vi ska komma ihåg den                                                                                                                                                                                                                                                                                                                 |
| 7  | **Optimistisk kontroll med `errand.version` och `If-Match`** — krocken upptäcks när någon skriver                                                                                                                                                                                                                                                                                               | Pessimistiskt lås på ärendet med utgångstid, spärrar i skrivvägarna och `423`                                                                                       | Målet är att ändringar inte ska tappas, inte att handläggaren ska hindras. Och maskineriet finns redan i drift — §6.2                                                                                                                                                                                                                                                                                                |
| 8  | **En** skrivning bär både tillstånd och aktiviteter                                                                                                                                                                                                                                                                                                                                             | Var sin endpoint                                                                                                                                                    | Färre anrop, en transaktion och en rapport per arbetssteg                                                                                                                                                                                                                                                                                                                                                            |
| 9  | Inga processvariabler för att hålla reda på vad som redan gjorts                                                                                                                                                                                                                                                                                                                                | `updateAvailable`, versionsräknare                                                                                                                                  | Läs–ändra–skriv med inbyggd kapplöpning. Skyddet ligger i strukturen i stället                                                                                                                                                                                                                                                                                                                                       |
| 10 | `errand.status` frikopplat från processens faser                                                                                                                                                                                                                                                                                                                                                | Process äger status                                                                                                                                                 | Skilda begrepp. Listvyn läser processläget ur `errand.process`                                                                                                                                                                                                                                                                                                                                                       |
| 11 | pw-alkt:s `start`- och `update`-endpoints tas bort                                                                                                                                                                                                                                                                                                                                              | Att låta dem ligga kvar som utfasade                                                                                                                                | Ingen använder dem, och med två sätt att starta en process kommer någon förr eller senare att välja fel                                                                                                                                                                                                                                                                                                              |
| 12 | **`ProcessStatus.isTerminal()` som metod på enumet**                                                                                                                                                                                                                                                                                                                                            | En separat lista över vilka statusar som räknas som avslutade                                                                                                       | Den styr `active_marker` och därmed regeln om en process per ärende. `WAITING` är fällan — se §4.1                                                                                                                                                                                                                                                                                                                   |
| 13 | **Fem statusvärden**, där `START_FAILED` blir `FAILED` med en felkod                                                                                                                                                                                                                                                                                                                            | Åtta värden                                                                                                                                                         | Skillnaden syns redan på att `processInstanceId` saknas                                                                                                                                                                                                                                                                                                                                                              |
| 14 | **Modelleringsregel: inga parallella grenar som ändrar ärendet**                                                                                                                                                                                                                                                                                                                                | Ingen regel alls                                                                                                                                                    | Två grenar som skriver till samma ärende slår ut varandra med `412` och kommer aldrig i mål — §6.4                                                                                                                                                                                                                                                                                                                   |
| 15 | **Loop-skyddets lager 1 styrs av headern `X-Trigger-Process`, inte av vem som anropar.** Headern hedras inte för AD-identiteter                                                                                                                                                                                                                                                                 | Att jämföra `X-Sent-By` mot namespacets `PROCESS_CONSUMER`                                                                                                          | Avsikten *"den här skrivningen ska inte väcka processen"* är det vi vill uttrycka; identiteten var bara en gissning på den. SM slipper känna igen varje ny pw-tjänst, och en namnmiss slutar tyst loopa — §6.5                                                                                                                                                                                                       |
| 16 | **Ett publiceringsfel märker transaktionen som `rollback-only`**                                                                                                                                                                                                                                                                                                                                | Lita på att anroparen för felet vidare                                                                                                                              | Alla anropsställen sväljer undantag från `createErrandEvent` (§1.7). Utan det här är outboxen inte transaktionell                                                                                                                                                                                                                                                                                                    |
| 17 | **Start styrs av `processKey`, inte av `eventType`**                                                                                                                                                                                                                                                                                                                                            | Start endast på `CREATE`                                                                                                                                            | Ärendet som får sin etikett i ett andra anrop hade annars aldrig startat — §7.1 motiverar triggern, §9.3 utför den                                                                                                                                                                                                                                                                                                   |
| 18 | Aktiviteter får sakna processinstans: `errand_process_id` är nullbar och läsningen sker per ärende                                                                                                                                                                                                                                                                                              | `not null`                                                                                                                                                          | Tvetydiga etiketter och nödbromsen slår till innan någon instans finns. Posten hade helt enkelt inte gått att spara                                                                                                                                                                                                                                                                                                  |
| 19 | Arbetssteg som bara läser skickar med `errandVersion`; SM svarar `412` om ärendet hunnit ändras                                                                                                                                                                                                                                                                                                 | Inget skydd alls för läsande steg                                                                                                                                   | Ett steg som aldrig skriver har annars ingenting att krocka på — §6.3                                                                                                                                                                                                                                                                                                                                                |
| 20 | **`POST .../processes` skapar, men uppdaterar aldrig**                                                                                                                                                                                                                                                                                                                                          | `409` så snart det finns en rad                                                                                                                                     | Ett arbetssteg kan hinna rapportera före pw:s `POST`. När `POST` bara skapar spelar ankomstordningen ingen roll, och `409` betyder bara en enda sak — §5.1                                                                                                                                                                                                                                                           |
| 21 | **Ett avslutat processliv startas aldrig om.** `FAILED` får startas om                                                                                                                                                                                                                                                                                                                          | Starta på nästa triggande händelse oavsett historik                                                                                                                 | `findProcessInstances` ser bara det som kör i Operaton just nu, så en `COMPLETED` process ser ut som ingen process alls. Nästa process är ett nytt ärende — §7.4                                                                                                                                                                                                                                                     |
| 22 | `errand.process` visar den **senaste** instansen när ingen lever (ändrat 2026-09-22, beslut 82)                                                                                                                                                                                                                                                                                                 | Den som lever just nu                                                                                                                                               | En misslyckad start lämnar ingen levande instans efter sig, och då hade handläggaren inte sett någonting alls — §5.3                                                                                                                                                                                                                                                                                                 |
| 23 | **Beslutet är en resurs med fasta fält — mains `.../decisions`, inte en egen `errand_decision`** (ändrat 2026-09-15, beslut 51)                                                                                                                                                                                                                                                                 | `json_parameter` med registrerat schema; kolumner på `errand_process`; vanliga parametrar; aktivitetsloggen; en egen `errand_decision`                              | Ett myndighetsbeslut har en form som följer av förvaltningslagen och förtjänar riktiga fält, och det är ärendedata som ska gå att läsa utan att man känner till processen. Den formen fanns redan i mains handläggningsmodell när ALKT kom dit. Se §7.5                                                                                                                                                              |
| 24 | **Både handläggare och process får fatta beslutet; `method` skiljer dem åt**                                                                                                                                                                                                                                                                                                                    | Bara handläggaren; bara processen                                                                                                                                   | Ett delegationsbeslut kan vara automatiserat, men vilket det var måste gå att svara på i efterhand (FL 28 §). Följden: `DECISION` måste vara `PROCESS_TRIGGER`, och `method` valideras mot identiteten — §7.5                                                                                                                                                                                                        |
| 25 | `processKey` hämtas från instansen först och från etiketterna i andra hand; `DELETE` skickas även utan nyckel                                                                                                                                                                                                                                                                                   | Att alltid läsa nyckeln ur etiketterna                                                                                                                              | En borttagen etikett skulle annars lämna en processinstans kvar i Operaton för ett ärende som inte längre finns — §2.2                                                                                                                                                                                                                                                                                               |
| 26 | **Resursen heter `processes` och modellen `ErrandProcess`**                                                                                                                                                                                                                                                                                                                                     | `process-instances`; `process-info`                                                                                                                                 | Modellen ska kunna bära även processer som inte körs i Operaton, och kodbasens övriga subresurser heter något i plural. `process-info` går inte att böja i plural och hade dessutom låst oss vid en rad per ärende                                                                                                                                                                                                   |
| 27 | **`GET /process-labels` byggs inte**                                                                                                                                                                                                                                                                                                                                                            | En egen endpoint som visar vilken etikett som startar vilken process                                                                                                | `GET /metadata/labels` lämnar redan tillbaka `attributes` med `id` och `resourcePath`. Den fråga man faktiskt ställer i drift gäller dessutom ett enskilt ärende och besvaras av aktivitetsloggen — §5.2. Indexet `idx_metadata_label_attribute_key` behövs därmed inte heller                                                                                                                                       |
| 28 | **Manuell stegning sker med namngivna signaler, och valet manuellt eller automatiskt ligger i processmodellen**                                                                                                                                                                                                                                                                                 | En inställning per namespace; att handläggaren sätter processens läge direkt                                                                                        | En inställning i SM kan säga en sak medan modellen gör en annan. Signalen är dessutom en begäran, inte ett kommando — processen avgör, så lagstadgade steg går inte att kliva förbi (§5.9)                                                                                                                                                                                                                           |
| 29 | **Signalen bär bara ett namn, ingen fritext**                                                                                                                                                                                                                                                                                                                                                   | Ett kommentarsfält på signalen                                                                                                                                      | Aktivitetsloggen gallras efter 365 dagar medan ärendet lever längre, och `message` får inte innehålla personuppgifter. Motiveringen hör hemma i ärendeanteckningar (§5.9)                                                                                                                                                                                                                                            |
| 30 | **Ingen retry-räknare och ingen dead letter. Raden ligger kvar tills den gått igenom**                                                                                                                                                                                                                                                                                                          | Egen backoff med `retry_count`/`next_retry_at`/`dead_letter`, som `notification_dispatch` hade före `V1_48__simplify_notification_dispatch`                         | Leverans och radering i samma transaktion ger samma sak utan bokföring, och den bokföringen har kodbasen medvetet gjort sig av med. Kvar blir `delivered_at`, som nödbromsen behöver — §8.3                                                                                                                                                                                                                          |
| 31 | **Outbox-raden bär sitt eget mål i `process_service`, satt vid publicering**                                                                                                                                                                                                                                                                                                                    | Att relayet slår upp `PROCESS_CONSUMER` på nytt vid leverans                                                                                                        | Ett namespace har exakt en processkonsument, men konfigurationen kan ändras mellan publicering och leverans. Raden ska gå dit den var adresserad. Relayet hämtar på kolumnen, och en rad adresserad någon annanstans än pw-alkt syns i hälsoindikatorn — §7.6                                                                                                                                                        |
| 32 | **`process` är ett `ErrandField`-värde. `decision` är det inte** (ändrat 2026-09-15)                                                                                                                                                                                                                                                                                                            | Att låta `process` stå utanför den rollbaserade fältfiltreringen                                                                                                    | Ärendet bär inget beslut (beslut 51). Beslutet läses på `.../decisions`, och `justification` skyddas där av resursen `DECISION` i stället för av fältfiltreringen — §5.3, §7.5                                                                                                                                                                                                                                       |
| 33 | **AoT använder inte AccessMapper, och ett namespace med `PROCESS_CONSUMER` får inte ha aktiv `access_control`**                                                                                                                                                                                                                                                                                 | Att lita på att ingen slår på den; att låta `AccessControlService` gå förbi kontrollen för konsumenten utpekad med `X-Sent-By`                                      | AccessMapper svarar bara på AD-konton, och pw är ingen människa. Slås kontrollen på får pw `401` på allt, och det syns som ärenden som står stilla. En header som anroparen sätter själv duger inte som behörighetsgrund — §7.1                                                                                                                                                                                      |
| 34 | **Två nya `ProtectedResource`: `PROCESS` och `PROCESS_ACTIVITY`. `DECISION` fanns redan**                                                                                                                                                                                                                                                                                                       | Att återanvända `ERRAND`                                                                                                                                            | `getErrand` och `verifyExistingErrandAndAuthorization` kräver en resurs, så valet går inte att skjuta upp. `ERRAND` hade gett processens rapporter samma behörighet som ärendet självt. `DECISION` kom med mains handläggningsmodell — §5.6                                                                                                                                                                          |
| 35 | **Startläget bor på etiketten: `processStartMode` bredvid `processKey`**                                                                                                                                                                                                                                                                                                                        | En inställning per namespace; en manuell grind först i processmodellen                                                                                              | Ansökan och tillsyn ligger i samma namespace och vill ha olika svar. En grind i modellen hade dessutom gett varje ärende en levande instans, och att avbryta vid grinden avslutar processlivet enligt §7.4 regel 4 — ärendet hade aldrig gått att starta igen. Avgränsat mot beslut 28: läget styr instansens **födelse**, modellen styr stegningen — §7.7                                                           |
| 36 | **SM räknar ut startlovet och skickar det med händelsen som `startAllowed`**                                                                                                                                                                                                                                                                                                                    | Att pw avgör själv och frågar SM om ärendet har en avslutad process                                                                                                 | Manuell start går annars inte att uttrycka: den skiljer sig från en vanlig ärendeändring bara genom att den får starta. På köpet försvinner pw:s återanrop till SM för `COMPLETED`-kontrollen — lovet är redan uträknat när händelsen kommer fram — §7.7, §9.3                                                                                                                                                       |
| 37 | **Kommandon filtreras inte av `PROCESS_TRIGGER` och kräver AD-identitet**                                                                                                                                                                                                                                                                                                                       | Ett `PROCESS`-värde i triggern; att släppa in maskinidentiteter och i stället undanta kommandon från loop-skyddets lager 1                                          | Ett kommando är ingen ärendeändring, och en människa som trycker på en knapp är ingen loop. Kommandon passerar därför **alla tre** lagren: AD-kravet gör lager 1 verkningslöst av sig självt, medan lager 2 och 3 undantar dem uttryckligen. Utan undantaget för nödbromsen sväljs startkommandot tyst på just de ärenden som har mest trafik. `SIGNAL` utgår därmed ur `PROCESS_TRIGGER` — §6.5, §7.1, §7.7         |
| 38 | **`GET .../processes` svarar med ett kuvert: `startable` + `processes`**                                                                                                                                                                                                                                                                                                                        | En naken lista; ett fält på ärendeprojektionen                                                                                                                      | Det intressanta fallet är när listan är tom, och en tom lista kan inte bära *varför*. Ärendeprojektionen är tjänstens varmaste läsväg och hade dragit med sig en uppslagning per ärende i listsvar — §5.10                                                                                                                                                                                                           |
| 39 | **Utgår (2026-09-15).** `decision` skulle ha reducerats i listsvar till `outcome`, `method` och `decidedAt`                                                                                                                                                                                                                                                                                     | —                                                                                                                                                                   | Ärendet bär inget beslut (beslut 51), så det finns inget listsvar att reducera                                                                                                                                                                                                                                                                                                                                       |
| 40 | **Kontrollen av processrapportens avsändare är validering och svarar `400`, inte `403`**                                                                                                                                                                                                                                                                                                        | `403` enligt §5.6:s ursprungliga tabell                                                                                                                             | SM autentiserar ingenting inkommande och `X-Sent-By` sätts av anroparen själv, så ett `403` hade påstått en behörighetsprövning som aldrig gjordes och skickat felsökningen till WSO2 i stället för till fältet i kroppen. Reglerna gör `process_service` garanterad, hindrar rader i namespace utan processmotor och ger loggen en avsändare — §5.6, beslut 33                                                      |
| 41 | **Relayet levererar bara till pw-alkt, med en statisk Feign-klient byggd som tjänstens övriga**                                                                                                                                                                                                                                                                                                 | En uppslagningstabell namn → klient byggd ur `process-engine.consumers` med `FeignClientBuilder`; ett register över konsumenter att validera `PROCESS_CONSUMER` mot | Det finns en processmotor, och REST ska ersättas av RabbitMQ. En ny konsument kräver en release ändå, och en statisk klient ser ut som resten av tjänsten. `PROCESS_CONSUMER` valideras mot klientens namn, så ett register med fler namn än relayet kan leverera till behövs inte. En rad adresserad någon annanstans syns i hälsoindikatorn — §7.2, §7.6                                                           |
| 42 | **Statusfälten är strängar i API:et, och enumen hålls på SM-sidan** — `processStatus`, `severity` och `startable.status`, kontrollerade med `@ValidEnumValue`                                                                                                                                                                                                                                   | Enum i specen                                                                                                                                                       | Ett enum i specen gör varje nytt värde till en ny API-version, och en klient som genererat enumet kastar på värdet i stället för att bortse från det. Mängden är stängd där värdet skrivs och öppen där det läses (PR #737) — §5.3, §5.10                                                                                                                                                                            |
| 43 | **`DELETE` passerar loop-skyddets tre lager, precis som kommandon**                                                                                                                                                                                                                                                                                                                             | Att låta nödbromsen, triggerfiltret och headern gälla raderingar                                                                                                    | En radering kan inte loopa, eftersom ärendet är borta, och en radering som hålls tillbaka lämnar processinstansen levande i Operaton för ett ärende som inte finns — §2.2, §6.5                                                                                                                                                                                                                                      |
| 44 | **Aktivitetsloggen gallras av städjobbet efter `activity-retention`, 365 dagar**                                                                                                                                                                                                                                                                                                                | Ingen gallring; gallring som en egen uppgift                                                                                                                        | §5.9 och §11 vilar på att loggen gallras, och frågan och indexet fanns redan — §3.2, §7.2                                                                                                                                                                                                                                                                                                                            |
| 45 | **Ett ärendes etiketter får peka ut högst en `processKey`, och det hålls när etiketterna skrivs** — vid skapande, `PATCH` och `ADD_LABEL`                                                                                                                                                                                                                                                       | Att bara hantera tvetydigheten när händelsen publiceras: ingen start och en ERROR-aktivitet (§7.3)                                                                  | Ett ärende som pekar ut två processer är fel innan något har startats, och vid publiceringen syns det först när processen borde ha vaknat. Läs-sidan behövs ändå, eftersom en ändring i etikettens metadata kan göra ett ärende tvetydigt utan att ärendet skrivs — §7.3, §7.4                                                                                                                                       |
| 46 | **Etikettspärren jämför vad etiketterna löser ut till före och efter ändringen**, och släpper alltid igenom en ändring som pekar på den process ärendet kör                                                                                                                                                                                                                                     | Att jämföra de nya etiketterna med nyckeln på processraden                                                                                                          | Jämförelsen fångar att nyckeln försvinner lika väl som att den byts. Undantaget är enda vägen tillbaka för ett ärende vars etikett redan tappat nyckeln — §7.4                                                                                                                                                                                                                                                       |
| 47 | **Etiketter som inte lästs från databasen slås upp på id när nyckeln löses ut**, i `ProcessKeySelector.select`, i en fråga och bara när sådana finns                                                                                                                                                                                                                                            | Att bara fylla i metadataetiketten där etiketterna byggs; att alltid slå upp alla etiketter                                                                         | Selektorn äger frågan om vad etiketterna säger. Fyra vägar bygger etiketter, och en femte hade annars tyst tappat nyckeln igen. Att alltid slå upp hade kostat en fråga per publicering även för inlästa ärenden. `ErrandLabelService` fyller ändå i metadataetiketten när en skrivning sätter etiketterna, för svarets skull — §7.3                                                                                 |
| 48 | **En schemalagd åtgärd som ändrat ärendet ger revision och ärendehändelse, men ingen notis**                                                                                                                                                                                                                                                                                                    | Ingen händelse, som tidigare; en händelse med notis till handläggaren                                                                                               | Utan händelse fick varken historiken, eventloggen eller processen veta att en etikett kommit till, och en processetikett startade ingenting. Ändringen är namespacets konfiguration i arbete, inte någon handläggaren väntar sig ett besked från — §7.3                                                                                                                                                              |
| 49 | **Ett ärende ska ge samma snapshot nyss skrivet som nyss läst.** Etiketternas metadata och den inlästa statusen skrivs inte, äldre snapshots jämförs och diffas utan dem, samlingar utan egen ordning sorteras, en tom samling räknas som ingen när revisionen avgörs, och den första tidsmätningens start avrundas som ärendets egen tid                                                       | Att fylla i fälten överallt; att även låta diffen bortse från tomma samlingar                                                                                       | Skillnaderna fanns bara mellan skrivet och läst, så en `PATCH` utan ändring gav revision, händelse och en onödig väckning. En ändring av etikettens metadata hade dessutom sett ut som en ändring av varje ärende som bär etiketten. Diffen behåller sina sökvägar, eftersom den är ett API — §11                                                                                                                    |
| 50 | **En etikett i `POST` och `PATCH` måste höra till ärendets namespace och kommun**, annars `400` med samma svar som för en etikett som inte finns                                                                                                                                                                                                                                                | Att bara lita på främmande nyckel i databasen                                                                                                                       | Ett id når etiketter i alla namespace, och med etiketten följer åtkomstregler och processnyckel. Samma svar för okänd och främmande etikett, så att svaret inte avslöjar något om andra namespace — §11                                                                                                                                                                                                              |
| 51 | **Beslutet byggs inte i ALKT-planen. Mains handläggningsmodell används som den är: `.../decisions`, `DecisionEntity`, `DecisionValidator` och utfallen som metadata** (2026-09-15)                                                                                                                                                                                                              | En egen `errand_decision` med `@OneToOne` på ärendet, `Errand.decision`, `ErrandField.DECISION` och revision av beslutet                                            | Två beslutsmodeller hade behövt hållas i takt, och den som läser beslut hade fått fråga sig vilken som gäller. Processen läser beslutet på `GET .../decisions` — §7.5                                                                                                                                                                                                                                                |
| 52 | **Att skapa, ändra och radera ett beslut ger en ärendehändelse med subtypen `DECISION` och höjer `errand.version`. Villkoren, bilagelänkarna och JSON-parametrarna gör ingetdera**                                                                                                                                                                                                              | Händelser även för underresurserna; revision av beslutet                                                                                                            | Processen väntar på att beslutet blir färdigt. Varje extra händelse räknas av nödbromsen, och ärendets revision bär inte beslutet — §7.5                                                                                                                                                                                                                                                                             |
| 53 | **Beslutet låses på ärenden med process: alla skrivvägar när processen är `COMPLETED`, och beslutet självt när det är `COMPLETED`. JSON-parametrarna undantas. En ärendebilaga som ett låst beslut länkar och en utredning ett låst beslut vilar på går inte att radera**                                                                                                                       | Lås bara mot processen; lås även JSON-parametrarna                                                                                                                  | Processen har gått vidare från ett fattat beslut. Laga kraft och delgivning blir kända först efteråt och hör hemma i JSON-parametrarna. Bilagan och utredningen hade annars ändrat beslutet genom databasens kaskad — §7.5                                                                                                                                                                                           |
| 54 | **Händelsen när en handläggare gör ett beslut `COMPLETED` passerar nödbromsen, men inte lager 1 och 2**                                                                                                                                                                                                                                                                                         | Att låta bromsen hålla tillbaka den som andra händelser; att släppa förbi även processens egna avslut                                                               | Det är den enda händelse väntläget behöver, och en människa loopar inte. Processen skapar ett nytt beslut varje gång den avslutar ett, så dess egna avslut hade kunnat loopa förbi bromsen — §6.5                                                                                                                                                                                                                    |
| 55 | **`PROCESS_TRIGGER` kontrolleras när konfigurationen skrivs: med `PROCESS_CONSUMER` krävs `ERRAND` och `DECISION`, och `PROCESS` och `SIGNAL` får aldrig stå med**                                                                                                                                                                                                                              | Bara dokumentation; att alltid publicera beslutshändelsen oavsett listan                                                                                            | En saknad trigger märks bara som ärenden som står stilla. En uppräknad kommandotyp ser ut att styra något den inte styr — §7.1                                                                                                                                                                                                                                                                                       |
| 56 | **`AUTOMATIC` godtas bara från namespacets `PROCESS_CONSUMER`, och SM sätter `errandProcessId` till ärendets levande processrad**                                                                                                                                                                                                                                                               | `AUTOMATIC` från vilken identitet som helst som inte är ett AD-konto, som handläggningsmodellen först tillät                                                        | Ett namespace utan processmotor har ingen som kan fatta ett automatiskt beslut. Ingen FK mot processraden, eftersom processrader bara försvinner med ärendet — §7.5                                                                                                                                                                                                                                                  |
| 57 | **`justification` maskas i payloadloggen med ett filter i `logbook.body-filters`**                                                                                                                                                                                                                                                                                                              | Att lita på att tjänstens egna loggrader bara loggar id:n                                                                                                           | dept44 loggar hela request- och svarskroppen som standard — §8.1                                                                                                                                                                                                                                                                                                                                                     |
| 58 | **De väntade signalerna ligger i `V1_60` tillsammans med de övriga processtabellerna** (2026-09-21)                                                                                                                                                                                                                                                                                             | En egen migrering för `errand_process_signal`                                                                                                                       | Användarens beslut. Filen skrivs defensivt, men en miljö som redan kört den tidigare versionen stoppar på checksumman tills den repareras — §3.1                                                                                                                                                                                                                                                                     |
| 59 | **En signal skickar ingen notis** (2026-09-21)                                                                                                                                                                                                                                                                                                                                                  | Notis till ärendets handläggare och prenumeranter, som vid andra handläggarskrivningar                                                                              | Användarens beslut. Signalen är ett kommando till processen; aktivitetsposten och eventloggen ger spårbarheten — §5.9                                                                                                                                                                                                                                                                                                |
| 60 | **Signalnamn jämförs exakt överallt: när handläggaren skickar ett, när en rapport lagras och i kolumnen, som har kollationen `utf8mb4_nopad_bin`**                                                                                                                                                                                                                                              | Kolumnens standardkollation, med en nyckel i Java som efterliknar den                                                                                               | Operaton skiljer på versaler, accenter och blanksteg. En efterliknad kollation täcker aldrig alla likheter databasen gör, och en rapport som nyckeln avvisar avvisas vid varje omförsök — §5.9                                                                                                                                                                                                                       |
| 61 | **En process som avslutats visar inga väntade signaler, oavsett vilka rader den lämnat efter sig.** Regeln hålls i läsningen                                                                                                                                                                                                                                                                    | Att tömma listan när rapporten skrivs                                                                                                                               | Även reläet avslutar processer (`422`), och en regel på varje skrivväg glöms på nästa. En knapp som alltid avvisas är sämre än ingen — §5.9                                                                                                                                                                                                                                                                          |
| 62 | **Kommandona har en egen tjänst, `ProcessCommandService`**, bredvid `ErrandProcessService`                                                                                                                                                                                                                                                                                                      | Signalen som metod i `ErrandProcessService`                                                                                                                         | `ErrandProcessService` äger det processen rapporterar och läsningen av det; ett kommando är ingetdera och behöver `EventService`. Startkommandot i T12 hör hemma där också — §5.9                                                                                                                                                                                                                                    |
| 63 | **En signal i ett namespace utan `PROCESS_CONSUMER` ⇒ `400`**, som för processrapporten                                                                                                                                                                                                                                                                                                         | Att låta publiceringen avgöra                                                                                                                                       | Publiceringen skriver ingen rad där, och knappen hade sett ut att fungera utan att något hände — §5.9                                                                                                                                                                                                                                                                                                                |
| 64 | **En signal förbrukar ingenting: skyddet mot dubbelklick är processens nästa rapport** (2026-09-21)                                                                                                                                                                                                                                                                                             | Att tömma instansens väntade signaler när en godtas; att förbruka bara den tryckta                                                                                  | Användarens beslut. SM vet inte vad en signal besvarar, och en gissning döljer knappar som gäller eller skyddar bara halvt. Ett andra klick före rapporten tas emot av pw som en signal ingen grind väntar på — §5.9                                                                                                                                                                                                 |
| 65 | **Rapporten har en egen modell, `ErrandProcessReport`, och `ErrandProcess` är bara läsmodellen** (2026-09-21)                                                                                                                                                                                                                                                                                   | En modell för båda, med rapportens egna fält märkta `WRITE_ONLY`                                                                                                    | Användarens beslut. Fält som syns i lässchemat men aldrig fylls förvirrar, och en genererad klient bär dem i sin typ — §5.3                                                                                                                                                                                                                                                                                          |
| 66 | **Kuvertet från `GET .../processes` heter `ErrandProcessOverview`** (2026-09-21)                                                                                                                                                                                                                                                                                                                | `ErrandProcesses`                                                                                                                                                   | Användarens beslut: pluralformen förväxlades med `ErrandProcess`. JSON på tråden är oförändrad, bara schemanamnet byts — pw-alkts genererade klient följer med nästa gång dess kopia av specen uppdateras — §5.10                                                                                                                                                                                                    |
| 67 | **När ärendet har en processrad, också en misslyckad start, erbjuds och godtas bara den processens nyckel**                                                                                                                                                                                                                                                                                     | Att erbjuda alla nycklar som etiketterna pekar ut                                                                                                                   | Registreringen avvisar en instans av en annan process med `409` (alla instanser av ett ärende kör samma process). Pekar etiketterna efter en metadataändring på en annan nyckel hade knappen tänts för en start som pw sedan måste avbryta. Etiketterna utan den nyckeln ger `NO_PROCESS_KEY` och `400` — §5.10                                                                                                      |
| 68 | **Etikettskrivningen avvisar också nycklar som stavas som `processKey` eller `processStartMode` på annat sätt** (versaler, blanksteg runt)                                                                                                                                                                                                                                                      | Bara de två kontrollerna i §7.7                                                                                                                                     | Utan den tredje regeln går `processstartmode: MANUAL` igenom båda kontrollerna och läses som inget läge alls, alltså `AUTOMATIC` — exakt den tysta feltolkning kontrollerna finns för — §7.7                                                                                                                                                                                                                         |
| 69 | **En start "på väg" är varje olevererad rad för ärendet med `start_allowed = 1`**, oavsett subtyp                                                                                                                                                                                                                                                                                               | Bara rader med subtypen `PROCESS`                                                                                                                                   | En automatisk start som ännu inte levererats är lika mycket en start på väg; en knapptryckning ovanpå den hade gett pw två startlov för samma ärende — §5.10                                                                                                                                                                                                                                                         |
| 70 | **`startable.status` hålls till `ProcessStartability` av `withStatus(enum)`, inte av `@ValidEnumValue`**                                                                                                                                                                                                                                                                                        | Annotationen, som beslut 42 anger                                                                                                                                   | Fältet är `READ_ONLY` och valideras aldrig; annotationen hade inte haft någon verkan. Samma mönster som `ErrandProcess.processStatus` i läsmodellen                                                                                                                                                                                                                                                                  |
| 71 | **I ett namespace utan `PROCESS_CONSUMER` godtas `AUTOMATIC` från varje anropare som inte är ett AD-konto, som på main** (2026-09-21). Beslut 56 gäller bara namespace med processkonsument                                                                                                                                                                                                     | `403` för `AUTOMATIC` i varje namespace utan processkonsument                                                                                                       | Användarens beslut. Main tillät det, och en tjänst som redan skriver automatiska beslut hade slutat fungera när alkt-sprint driftsattes — §7.5                                                                                                                                                                                                                                                                       |
| 72 | **Varje starttryckning skrivs, också en som möter en start med samma nyckel på väg: aktivitetsposten och händelsen i eventloggen skrivs, bara publiceringen till pw uteblir** (2026-09-22)                                                                                                                                                                                                      | Att inte skriva något alls när en start redan är på väg                                                                                                             | Granskningen av PR #754. Med beslut 69 räknas också en automatisk start som ännu inte levererats, så i `AUTOMATIC`-läge hade en handläggares första tryckning inte lämnat något spår av vem som tryckte. Ett dubbelklick ger nu två poster och två händelser, men fortfarande en startrad — §5.10                                                                                                                    |
| 73 | **En tom eller blank `processKey` i startkroppen är ingen nyckel alls, precis som en utelämnad** (2026-09-22)                                                                                                                                                                                                                                                                                   | `@NotBlank` på fältet; att låta den falla igenom till "inte bland ärendets nycklar"                                                                                 | Granskningen av PR #754. Samma regel som publiceringen redan har (`resolveProcessKey`), och `@NotBlank` hade avvisat också den utelämnade nyckeln, som kroppen får sakna — §5.10                                                                                                                                                                                                                                     |
| 74 | **Cronjobbet hämtar vidare förbi ärenden som fallerat under körningen, och ett ärende som fallerar räknas med de rader det försökt, minst en** (2026-09-22)                                                                                                                                                                                                                                     | Att alltid hämta samma första sida; att räkna varje hämtad rad; att räkna ett ärende som fallerar som ett försök oavsett hur många rader det skickat                | Helhetsgranskningen. Med samma första sida kunde ett ärende vars äldsta rad aldrig går igenom samla `batch-size` rader bakom sig, och då levererade cronjobbet inget annat. Att räkna hämtade rader hade låtit samma rader fylla körningen igen, och att räkna ett ärende som ett försök hade låtit ärenden som skickar många rader före felet dra körningen långt över taket — §8.3                                 |
| 75 | **En rad som fallerar avslutar sin grupp: raderna före kvitteras, och felet förs vidare när kvitteringen är sparad.** Det gäller också en rad som inte går att göra till en händelse (2026-09-22)                                                                                                                                                                                               | Att rulla tillbaka hela gruppen, som `notification_dispatch` gör                                                                                                    | Raderna före är redan hos pw. Tillbakarullade hade de getts igen varje minut i upp till 30 dagar, signaler och startkommandon inräknade. Ordningen kräver bara att inget efter den misslyckade raden levereras. Ett fel när en avvisning skrivs rullar däremot tillbaka hela gruppen, som ett misslyckat commit gör — §8.3                                                                                           |
| 76 | **En rapport om något annat än `COMPLETED` på en `COMPLETED` instans svarar `200` och lämnar instansen som den är, och likadant en rapport om något annat än `FAILED` på en `FAILED` instans när en annan instans av ärendet är `COMPLETED`.** Svaret är `200` vilken `errandVersion` rapporten än bär. Aktiviteterna lagras, och en upprepad avslutad status behåller sin sluttid (2026-09-22) | `409`; att låta rapporten uppdatera raden                                                                                                                           | Användarens beslut. Operaton återupptar aldrig en avslutad instans, och en återupplivad rad hade hävt beslutslåsen och brutit §7.4 regel 4. Rapporten kom bara sent, och inget omförsök kan rätta den — §5.1                                                                                                                                                                                                         |
| 77 | **En olevererad rad med `start_allowed = 1` räknas som ärendets process** — i etikettspärren, och i startlovet, som stängs när en start med en annan nyckel är på väg (2026-09-22)                                                                                                                                                                                                              | Att bara räkna processrader                                                                                                                                         | Ett ärende som bytte etikett innan pw hunnit registrera starten hade annars kört den första processen för alltid medan etiketterna pekade på den andra. Glappet mellan leverans och registrering stängs av pw och `409` — §5.10, §7.4, §7.7                                                                                                                                                                          |
| 78 | **Gallringen publicerar en `DELETE` för varje ärende den tar bort, utan händelse i eventloggen och utan notis** (2026-09-22)                                                                                                                                                                                                                                                                    | Att undanta ärenden med levande process från gallringen                                                                                                             | Användarens beslut. Ett ärende som stått orört i en grind i två år har en instans som lever vidare i Operaton. Bevarandet avgör när ärendet får försvinna, och processen ska få veta det — §7.5                                                                                                                                                                                                                      |
| 79 | **Etikettspärrens regel 1 jämför mängden nycklar före och efter** (2026-09-22)                                                                                                                                                                                                                                                                                                                  | Att jämföra den enda nyckel etiketterna löser ut till                                                                                                               | Ett tvetydigt ärende och ett utan nycklar löser båda ut till ingen, så jämförelsen hade släppt igenom att ett tvetydigt ärende med process tappar alla sina — §7.4                                                                                                                                                                                                                                                   |
| 80 | **`startable` svarar `START_PENDING` när en start är på väg, och en `processKey` längre än 128 tecken avvisas redan när etiketten skrivs** (2026-09-22)                                                                                                                                                                                                                                         | `AVAILABLE` tills pw registrerat instansen; längdkontrollen bara vid publicering                                                                                    | Användarens beslut. `startable` hade annars sagt ja där kommandot svarar `409` eller `400`, och gränssnittet behöver ett värde för att visa att starten är på väg. Publiceringens `CONFIG`-post står kvar för nycklar som kommit in förbi API:t — §5.10, §7.7                                                                                                                                                        |
| 81 | **En tom plats i rapportens listor, en tom `processInstanceId` och en sortering av aktivitetsloggen på ett okänt fält avvisas med `400`** (2026-09-22)                                                                                                                                                                                                                                          | Att låta dem nå tjänsten                                                                                                                                            | De gav `500`, och `processInstanceId: ""` i `POST` gav en levande rad som ingen `PUT` kan nå — §5.3                                                                                                                                                                                                                                                                                                                  |
| 82 | **`errand.process` visar den levande instansen före en nyare avslutad, och en rapport får inte göra en instans levande eller `COMPLETED` medan en annan lever** (2026-09-22). Ändrar beslut 22                                                                                                                                                                                                  | Den senaste raden oavsett status; att bara vägra en andra levande instans                                                                                           | En `FAILED` från en avbruten dubbelstart hade skymt den process som kör, och en `COMPLETED` för fel instans hade avslutat processlivet under den som kör. `FAILED` tas emot bredvid den levande — §5.1, §5.3                                                                                                                                                                                                         |
| 83 | **`ErrandService.persistLabelUpdate` frågar etikettspärren och skriver revision och ärendehändelse utan notis** (2026-09-22)                                                                                                                                                                                                                                                                    | Att låta `LabelMoveWorker` skriva förbi spärren; att låta även `updateErrand` gå genom metoden                                                                      | En flyttad etikett kan byta vilken process ett ärende löser ut till. Den fjärde skrivaren behöver samma kontroll som de tre andra, och processen ska få veta om en flytt som går igenom — §7.4. `updateErrand` delar bara spärren med metoden: en patch sparas, kör åtgärderna och ger notis i en enda skrivning och avvisas med `400`, medan en vägrad flytt lämnar ärendet orört och skriver en felpost            |
| 84 | **Relayets `422`-post råder bara ett ärende utan processrad att rätta etiketten, och etikettvalideringen jämför namespace utan hänsyn till versaler, som databasen, medan etikettens id jämförs exakt** (2026-09-22)                                                                                                                                                                            | Samma råd för alla ärenden; exakt jämförelse av namespace; id utan hänsyn till versaler                                                                             | Ett ärende med processrad kör processens nyckel hela sitt liv, så en rättad etikett hjälper inte där. Ett id med andra versaler hade sparats som det skickades, och sedan missat etiketten i allt som jämför id exakt, åtkomstkontrollen inräknad. `V1_60` öppnas inte (användarens beslut), så kollationen för `activity_id`, kolumnen `process_service` och hälsokontrollen för andra konsumenter står kvar — §8.3 |
| 85 | **Berikningen av `errand.process` frågar ingenting i ett namespace utan `PROCESS_CONSUMER`** (2026-09-22)                                                                                                                                                                                                                                                                                       | En fråga mot processtabellen i varje läsning, i alla namespace                                                                                                      | `readErrand`, `updateErrand` och varje sida i `findErrands` kostar där det de gjorde innan fältet fanns — §5.3                                                                                                                                                                                                                                                                                                       |
| 86 | **Triggerfiltret frågas före nödbromsen, och direktkörningen signaleras en gång per ärende och transaktion** (2026-09-22)                                                                                                                                                                                                                                                                       | Bromsen först; en signal per outbox-rad                                                                                                                             | Utfallet är detsamma, men en händelse som ändå filtreras bort kostar ingen `COUNT` och ger ingen `LOOP_GUARD`-post. Direktkörningen tar alla ärendets rader, så fler signaler gav bara körningar som inte hittade något — §2.2, §2.3                                                                                                                                                                                 |
| 87 | **`DELETE /errands/{errandId}` svarar `409` när ärendet bär ett beslut som inte längre får ändras** (2026-09-22)                                                                                                                                                                                                                                                                                | Att låta kaskaden ta beslutet och skriva in det i §7.5                                                                                                              | Användarens beslut. Samma lås som för bilagan och utredningen: beslutet hade annars försvunnit förbi beslutets egna regler. Gallringen omfattas inte — §7.5                                                                                                                                                                                                                                                          |
| 88 | **Varje beslutsskrivning, i alla namespace, ger händelse, notis, `notification_dispatch`-rad och höjd `errand.version`** (2026-09-22)                                                                                                                                                                                                                                                           | Att begränsa versionshöjningen och notisen till namespace med `PROCESS_CONSUMER`                                                                                    | Användarens beslut: beteendet står kvar, och dokumentet rättas. En klient som håller ärendets `ETag` får `412` efter en beslutsskrivning, också utan process — §5.7                                                                                                                                                                                                                                                  |
| 89 | **Dokumentet hålls samlat** (2026-09-22)                                                                                                                                                                                                                                                                                                                                                        | En del för gällande design och en för beslutslogg och historik                                                                                                      | Användarens beslut. Det har börjat glida isär med koden, och rättelserna görs i stället i det befintliga dokumentet                                                                                                                                                                                                                                                                                                  |

---

## 1. Vad vi vet om koden i dag

Allt i det här avsnittet är efterkontrollerat i kodbasen, inte antaget. Resten av dokumentet vilar på det.

### 1.1 `EventService` är vägen allt går igenom

`EventService.createErrandEvent` anropas från 14 ställen i koden, fördelade på nio vägar:

|           Väg           |                                                               Anropsställe                                                               |
|-------------------------|------------------------------------------------------------------------------------------------------------------------------------------|
| PATCH / create / delete | `ErrandService.createErrand/updateErrand/deleteErrand`                                                                                   |
| Flyttad etikett         | `ErrandService.persistLabelUpdate`, genom samma anrop som `updateErrand`                                                                 |
| Bilagor                 | `ErrandAttachmentService.createErrandAttachmentInternal/deleteErrandAttachment/createErrandAttachment(AttachmentEntity, …)/recordChange` |
| Handover                | `HandoverService.logHandoverEvents` (två anrop, ett per ärende)                                                                          |
| Konversationer          | `MessageExchangeSyncService.syncConversation`                                                                                            |
| Schemalagda åtgärder    | `ActionWorker.logChange`                                                                                                                 |
| E-postintag             | `EmailReaderWorker.processErrand`                                                                                                        |
| Webbmeddelanden         | `WebMessageCollectorWorker.saveMessage`                                                                                                  |
| Suspension              | `SuspensionWorker.processExpiredSuspensions`                                                                                             |

Beslutet går inte den vägen utan genom `EventService.createDecisionEvent` (§7.5), och kommandona genom `EventService.createProcessCommandEvent` (§2.2). Båda publicerar på samma sätt.

**Revisioner skapas på åtta ställen** — `ErrandService.createErrand/updateErrand/persistLabelUpdate`, `ErrandAttachmentService.createErrandAttachmentInternal/deleteErrandAttachment/createErrandAttachment(AttachmentEntity, …)/recordChange` och `ActionWorker.logChange`. Intaget (`EmailReaderWorker.processErrand`, `MessageExchangeSyncService.applyStatusChange`) sparar direkt via repository. Därför kan trigger-filtret inte bygga på revisionsdiff.

`EventSubType`: `ATTACHMENT, DECISION, ERRAND, HANDOVER_IN, HANDOVER_OUT, MESSAGE, NOTE, PROCESS, SIGNAL, SYSTEM, SUSPENSION`. `EventType` kommer från eventlog-specen. `SIGNAL` bär manuell stegning (§5.9) och `PROCESS` manuell start (§5.10) — de två kommandona, som `EventSubType.isCommand` känner igen.

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
- **Varken `ErrandJsonParameterService` eller `ErrandParameterService` skapar revision eller event.** En parameterskrivning höjer `errand.version` och gör i övrigt ingenting. Det är därför beslutet inte lagras som parameter (§7.5) — den vägen hade krävt att en delad, redan använd tjänst börjar skapa event och revision. `EventSubType.DECISION` används av beslutsskrivningen, genom `EventService.createDecisionEvent` (§7.5).
- `ErrandService.createErrand` tar `referredFrom` och skapar en relation via `RelationClient` — den befintliga vägen att koppla ihop två ärenden.
- `truncate.sql` måste utökas med varje ny tabell.

### 1.7 Alla som anropar `createErrandEvent` sväljer undantag

Alla 14 anropsställen i §1.1 har `try { … } catch (final Exception e) { LOG.warn(…) }` runt anropet, och
beslutsvägens `ErrandDecisionService.recordChange` likaså.

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

`ProcessEventPublisher.publish(errandEntity, eventType, eventSubType, executedBy, requestGroupId, command, concludesDecision)` anropas sist i `EventService.createErrandEvent` och kör i samma transaktion. Tre vägar går in bredvid:

- **Kommandona** i §5.9 och §5.10, genom `EventService.createProcessCommandEvent`: ett kommando ändrar inte ärendet, så händelsen pekar inte på någon revision, och det som kommandot bär — den valda nyckeln eller signalens namn — följer med till publiceringen. Utan kommando skrivs bara händelsen, för en start som redan är på väg (beslut 72).
- **Beslutet**, genom `EventService.createDecisionEvent`. `concludesDecision` säger om skrivningen är den som gör beslutet `COMPLETED`, vilket är det enda som släpps förbi nödbromsen (steg 4, beslut 54).
- **Gallringen**, genom `EventService.publishDeletionToProcess`: en `DELETE` utan händelse i eventloggen och utan notis, eftersom gallringen inte ska lämna något spår av ärendet efter sig (§7.5).

`sendNotification`-flaggan spelar ingen roll här — det här är inga notiser till handläggare, utan meddelanden till en process.

```
1. PROCESS_CONSUMER för (municipalityId, namespace)?      nej -> return
   eventType CREATE, UPDATE eller DELETE?                 nej -> kast, arendeandringen rullas tillbaka
2. X-Trigger-Process: false, icke-AD-identitet?           ja  -> return        (loop-skydd, lager 1)
                       kommandon (PROCESS, SIGNAL) och DELETE hoppar over steg 2, 3 och 4, 6.5
3. eventSubType i PROCESS_TRIGGER?                        nej -> return        (lager 2)
4. Levererade event för ärendet i fönstret >= tröskel?     ja  -> ERROR-aktivitet, return  (lager 3)
                       ett beslut som ett AD-konto avslutar hoppar over steget
5. processKey: kommandots egen forst, sedan instansens, sist ur etiketterna
                       0 -> DELETE publiceras anda, ovriga return
                       >1 -> ERROR-aktivitet, return
                       langre an kolumnen -> ERROR-aktivitet, return
6. startAllowed: startkommando (subtyp PROCESS) -> ja; annars ingen levande instans
                 && ingen COMPLETED && ingen start av en annan process pa vag
                 && etikettens processStartMode == AUTOMATIC
                 && etiketten pekar ut samma nyckel som raden bar (7.7)
7. INSERT process_event_outbox (process_service = namespacets PROCESS_CONSUMER, 7.6)
   och signalera direktkorningen - en gang per arende och transaktion (2.3)
```

Steg 1 är en uppslagning i en cachad map. Ett namespace utan process betalar alltså ingenting mer än så.

**Triggerfiltret ligger före nödbromsen.** Filtret är en uppslagning i samma cachade konfiguration som steg 1, medan
bromsen är en `COUNT` mot outboxen. En händelse som ändå inte ska till processen kostar då ingen fråga, och den kan
inte heller ge en LOOP_GUARD-post för en händelse som aldrig hade publicerats. Utfallet är detsamma i vilken ordning
de två än frågas.

Typkontrollen hör också till steg 1. pw-alkt tar bara emot `CREATE`, `UPDATE` och `DELETE`. En rad med någon annan typ kan relayet aldrig leverera, men läser den ändå först vid varje körning, och den håller tillbaka ärendets senare händelser tills den åldras ut. Publiceringen kastar därför i stället, medan anroparen finns kvar, och ärendeändringen rullas tillbaka. Inget anropsställe skickar någon annan typ idag, så kontrollen är till för nästa. Den ligger före loop-skyddet, så att ett sådant anrop fallerar i första testet och inte i produktion den dag någon lägger subtypen i `PROCESS_TRIGGER`. Ett namespace utan process når aldrig kontrollen.

ERROR-aktiviteterna i steg 4 och 5 skrivs **utan processinstans** — de inträffar per definition när
ingen instans finns (§3.1). Båda skrivs dessutom **en gång per ärende, felkod och fönster**, inte en gång per
kastad händelse. Nödbromsens post har alltid haft det kravet (§6.5); tvetydighetsposten behöver det nu när
tvetydiga etiketter är ett läge ett ärende kan ligga och vänta i tills någon startar processen för hand
(§5.10) — annars lägger varje meddelande och varje bilaga en ny ERROR-rad i loggen, och felet dränker den
logg det rapporteras i. `uq_epa_idempotency` räddar oss inte: både `errand_process_id` och
`external_task_id` är NULL för de här posterna, och NULL är distinkt i unika index.

**"Fönstret" är nödbromsens fönster, också för posterna som inte har med nödbromsen att göra.**
`process-engine.loop-guard.window` är alltså en inställning för två saker: höjs den till en timme börjar
ett tvetydigt ärende rapportera sig en gång i timmen. Det är avsiktligt — det finns ingen anledning att
tvinga fram två tal som ändå ska betyda samma sak — men kopplingen är dold i konfigurationen och står
därför utskriven här och i `ProcessActivityLog.windowStart`, som är den enda punkt där de skulle behöva
skiljas åt.

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

**En nyckel som inte får plats i kolumnen är ett konfigurationsfel, inte ett publiceringsfel.**
`metadata_label_attribute.value` är `text` och alltså obegränsad, medan `process_event_outbox.process_key`
är `varchar(128)`. Överlämnas den längdkontrollen till databasen blir en felskriven etikett en misslyckad
publicering — och en misslyckad publicering drar med sig ärendeändringen. Följden vore att **varje**
skrivning till **varje** ärende som bär etiketten svarar `500` för alltid, med ett felmeddelande som inte
pekar ut etiketten. Steg 5 mäter därför nyckeln själv och skriver en ERROR-aktivitet som namnger längden
och nyckelns början, precis som vid tvetydighet. Ärendet förblir skrivbart och processen går att starta för
hand. Samma resonemang gäller inte `signal_name`: ett kommando kan inte göra ett ärende oskrivbart, och en
avkortad signal skulle korrelera fel grind, så där får skrivningen fallera högljutt i stället.

**Steg 6 säger ja bara till startkommandot.** En signal (§5.9) riktas mot en instans som redan kör och bär
därför aldrig lovet att föda en ny — formeln i §7.7 är den som gäller.

**Publiceringen får inte kunna svälja sitt eget fel.** Alla anropsställen fångar `Exception` (§1.7),
och en överenskommelse om att inte lägga till ett till är ingen garanti. Därför:

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

**`publish` måste bära `@Transactional(propagation = SUPPORTS)` för att raden ovan ska fungera.**
`currentTransactionStatus()` läser den status som *transaktionsaspekten* lagt undan, inte den som finns.
Nås publiceringen från en anropare som kör sin egen `TransactionTemplate` — som `ErrandProcessService`
redan gör för sin kollisionsåterhämtning, och som kommandona i §5.9 och §5.10 därför kommer att göra —
finns ingen sådan status, och raden kastar `NoTransactionException` i stället för att märka transaktionen.
Då är skyddet borta, och det ursprungliga felet är dessutom överskrivet. Med annotationen binder aspekten
en status även när publiceringen bara hakar på någon annans transaktion. `SUPPORTS` och inte `REQUIRED`,
eftersom en skrivning utan transaktion ska rapporteras och inte tilldelas en.

Finns det ingen transaktion alls går det förstås inte att rulla tillbaka någonting — då är ärendet redan
sparat. Det loggas som ERROR (§8.1). I dag har varje väg in en
transaktion — tjänsterna bakom API:et och de schemalagda arbetarna (`EmailReaderWorker.processEmail`,
`WebMessageCollectorWorker.processMessage`, `SuspensionWorker.processExpiredSuspensions`,
`MessageExchangeWorker.processConversation`, `ActionWorker.processAction`) — så det ska aldrig hända.

Att i stället plocka bort de yttre fångsterna löser inte problemet ensamt, och skulle dessutom göra ett
misslyckat notisutskick dödligt för ärendeskrivningen. Vill man städa där är det ett eget arbete.

**En känd brist i intaget, som fanns före processintegrationen.** `EmailReaderWorker.processEmail` och
`WebMessageCollectorWorker.processMessage` raderar meddelandet i källsystemet — och e-postintaget skickar kvittensen
— inne i transaktionen, före commit. Rullas transaktionen tillbaka, av publiceringen eller av något annat databasfel,
är meddelandet borta ur källsystemet utan att ha sparats i SM. Publiceringen gör inte felet troligare än andra
databasfel, men den är ytterligare en väg till en återrullning. Rättelsen — radering och kvittens efter commit, och
kvittensen i en egen transaktion — rör intaget för alla namespace och hanteras som en egen uppgift (§11).

### 2.3 Hur snabbt det går — direktkörning och cronjobb

Relayet startas på två sätt, och det är värt att hålla isär dem.

> **Direktkörning** är en signal som bara bär ärendets id, och som levererar ärendets olevererade rader
> så fort transaktionen som skrev dem har sparats, i stället för att vänta på nästa cron-tick. Den är
> frivillig i den meningen att den får tappas — då hämtar cronjobbet raderna i stället, inom en minut.

Signalen skickas en gång per ärende och transaktion, av den första raden som skrivs för ärendet. Direktkörningen
tar ändå alla ärendets olevererade rader, så fler signaler hade bara gett fler körningar som hittar ingenting.

Direktkörningen och cronjobbet levererar på samma sätt, grupp för grupp (§8.3), men är inte samma jobb:

|                          |                  Direktkörning                   |                     Cronjobb                      |
|--------------------------|--------------------------------------------------|---------------------------------------------------|
| Startas av               | att ärendets transaktion just sparats            | klockan, `0 * * * * *`                            |
| Syfte                    | latens: sekunder i stället för upp till en minut | att ingenting blir liggande                       |
| Tar                      | ett ärendes rader, högst `batch-size`            | de äldsta raderna för pw-alkt, högst `batch-size` |
| `transaction-buffer`     | nej                                              | ja                                                |
| Rader äldre än `max-age` | lämnas åt cronjobbet                             | raderas oskickade, med ERROR-logg                 |
| Hälsokontrollen          | nej                                              | ja, efter körningen                               |
| Får utebli?              | Ja, utan att något går förlorat                  | Nej — det är sanningen i systemet                 |

Direktkörningen **väcker relayet, inte processen**. Att väcka processen är något helt annat och sker längre
fram i kedjan, när pw korrelerar ett meddelande in i Operaton (§9.3).

Mekaniken: `@TransactionalEventListener(AFTER_COMMIT, fallbackExecution = true)` i `ProcessEventDirectRun`, som
lägger körningen på trådpoolen `processEventExecutor` (2 trådar normalt, 4 som mest, kö på 500 —
`process-engine.direct-run`). Lyssnaren följer mönstret i `SubscriptionService.handleAutoSubscribeEvent`, men
körningen lämnas direkt till poolen i stället för genom `@Async`, som utan `@EnableAsync` tyst hade kört leveransen
i den tråd som just sparat ärendet.

**Blir kön full ska direktkörningen hoppas över, inte anropet fällas.** Standardbeteendet `AbortPolicy`
kastar ett `RejectedExecutionException` i den tråd som just sparat ärendet, och det når hela vägen ut till
anroparen — en full kö hade alltså gett `500` på en ärendeskrivning som faktiskt gick bra. Poolens
`RejectedExecutionHandler` kastar i stället bort signalen och loggar att det hände, så inget undantag kan nå den tråd
som sparat ärendet och lyssnaren behöver ingen egen fångst. Cronjobbet är skyddsnätet: en missad direktkörning kostar upp till en
minut, aldrig ett fel.

Cronjobbet levererar genom samma grupphantering och tar bara med rader som är minst fem sekunder gamla
(`scheduler.process-event.transaction-buffer`), så att det inte krockar med en transaktion som håller på att
sparas. Krockar de ändå — direktkörning och cronjobb på samma rad — är det ofarligt: båda läser om radgruppen med
`@Lock(PESSIMISTIC_WRITE)` och tar bara rader utan `delivered_at`, så den som kommer sist väntar in den första och
hittar sedan ingenting att göra. Samma lås är det som håller ordningen inom ett ärende, bland de rader som hunnit sparas:
direktkörningen väntar inte ut `transaction-buffer`, så en rad från en längre transaktion kan levereras efter en
senare. Det är ofarligt, eftersom händelsen inte bär någon ärendedata och pw läser ärendet självt (§5.4). Att vänta är avsiktligt:
`SKIP LOCKED` hade släppt förbi en senare rad medan en tidigare fortfarande levereras.

**Låset tas på id, i läsnivån `READ COMMITTED`, och raderna sorteras i Java.** Under MariaDB:s `REPEATABLE READ`
låser en låsande läsning även glappen mellan de indexposter den passerar. Gick den via indexet över oskickade rader
skulle den låsa just det glapp där publiceringen skriver sin nya rad, och varje ärendeskrivning stå och vänta tills
pw-alkt svarat. En uppslagning på primärnyckeln utan `ORDER BY` ger optimeraren inget skäl att välja den vägen, och
`READ COMMITTED` tar inga glapplås om den ändå skulle göra det. Rader som åldrats ur hittas på samma sätt: utan lås
först, sedan låsta på id.

### 2.4 Hur händelserna tar sig över till pw

**Vi börjar med REST.** Outboxen behövs oavsett vilken transport vi väljer, och meddelandekön är ännu inte
bevisat driftklar. Det är det enda som saknas — testmässigt är det ingen tröskel alls, SM har redan
Testcontainers (§1.6) och en `RabbitMQContainer` är några rader kod.

**Men målbilden är AMQP.** Med REST kräver varje ny PW-tjänst en OAuth2-registrering, en url och ett
Feign-mål i SM — vad det innebär i praktiken står i §7.6. Själva bytet är däremot litet: allt utbyte med pw-alkt
sker i `PwAlktIntegration`, och det är den klassen som byts ut. Relayet, leveransen och kvitteringen står kvar.

Med *driftklar* menar vi: quorum queues på minst tre noder, DLX/DLQ med `x-delivery-limit`, egen vhost per
miljö, en användare per tjänst med rättighetsregler, TLS, övervakning av kölängd, obekräftade meddelanden,
DLQ-djup och nodstatus — samt en dokumenterad väg tillbaka när något gått fel.

---

## 3. Datamodell

### 3.1 Tabellerna

Fyra nya tabeller, alla i `V1_60__add_process_integration_tables.sql`. De tre första byggdes i T1 och de
väntade signalerna i T11, som fyllde på samma fil i stället för att lägga en egen migrering (beslut 58). Det
fungerar bara där den tidigare versionen av filen aldrig körts: Flyway jämför checksumma, och en miljö som
redan kört den stoppar vid uppstart tills den repareras.

Beslutet har ingen tabell här. Det ligger i `decision` från mains gemensamma handläggningsmodell
(`V1_56__add_errand_item_model.sql`), tillsammans med sina villkor, bilagelänkar och JSON-parametrar (§7.5,
beslut 51). Kolumnen `decision.errand_process_id` fanns redan där, för just det här ändamålet.

**Tre regler gäller alla migreringar här, och de står i förväg eftersom de kostar mest när de upptäcks
sent.**

- **Versionsnumret sätts efter det högsta som redan finns i repot, inte efter det som stod i ett dokument.**
  Numren ovan är de lediga när det här skrivs; `V1_53`–`V1_59` togs av annat arbete medan designen låg
  färdig. Det är därför numren i det här avsnittet ska läsas som "nästa lediga", och kontrolleras mot
  `src/main/resources/db/migration` när migreringen faktiskt skrivs. Ett återanvänt nummer stoppar Flyway
  vid uppstart i varje miljö som redan kört den andra filen.
- **Skripten skrivs defensivt: `create table if not exists`, `create index if not exists`, `add column if
  not exists`.** En migrering ska kunna köras om mot ett schema där delar av den redan finns, utan att
  falla på att objektet är på plats. Av samma skäl ligger indexen nedan **inne i** sin `create table` i
  stället för i egna satser: hela skriptet hamnar då bakom ett enda `if not exists`, och InnoDB slipper
  lägga ett eget index bredvid varje främmande nyckel — en constraint återanvänder ett index som deklareras
  på samma sats.
- **Inga kommentarer i själva skripten.** Vad en kolumn är till för hör hemma i entitetens javadoc och i det
  annoterade schemat här nedan, där det går att hitta utan att öppna en migrering som ändå aldrig ska röras
  igen. En kommentar i migreringen blir dessutom osann med tiden: filen är låst av sin checksumma medan
  kolumnen den beskriver lever vidare.

```sql
-- 1. Outbox. Medvetet UTAN FK mot errand: ett DELETE-event maste overleva att arendet raderas.
create table if not exists process_event_outbox (
    id                varchar(36)  not null,
    municipality_id   varchar(8)   not null,
    namespace         varchar(32)  not null,
    errand_id         varchar(36)  not null,
    -- Radens mal, satt vid publicering ur namespacets PROCESS_CONSUMER. Relayet slar inte
    -- upp konfigurationen pa nytt, och hamtar bara rader for pw-alkt. Se 7.6.
    process_service   varchar(64)  not null,
    -- Nullbar: kravs for CREATE och UPDATE, irrelevant for DELETE dar pw matchar
    -- pa businessKey. Se 2.2 steg 5.
    process_key       varchar(128),
    event_type        varchar(64)  not null,   -- CREATE | UPDATE | DELETE
    event_sub_type    varchar(64)  not null,   -- ERRAND | MESSAGE | ATTACHMENT | ...
    -- Far handelsen starta en NY instans? Utraknat vid publicering, 7.7. Ett kommando satter
    -- den sjalv; en vanlig arendeandring far den bara i automatiskt lage.
    start_allowed     bit          not null default 0,
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
    primary key (id),
    key idx_peo_dispatch (delivered_at, created),
    -- Hamtningen: oskickade rader for EN konsument, aldst forst. Se 7.6.
    key idx_peo_consumer (process_service, delivered_at, created),
    key idx_peo_guard    (errand_id, delivered_at, created)
) engine=InnoDB;

-- 2. Processinstans, inklusive lasets tillstand.
create table if not exists errand_process (
    id                    varchar(36)  not null,
    errand_id             varchar(255) not null,
    municipality_id       varchar(8)   not null,
    namespace             varchar(32)  not null,
    process_service       varchar(64)  not null,   -- 'pw-alkt'
    process_key           varchar(128) not null,   -- 'alcohol-serving'
    process_instance_id   varchar(64),             -- null nar starten aldrig lyckades
    process_status        varchar(32)  not null,   -- RUNNING|WAITING|RETRYING|COMPLETED|FAILED
    current_activity_id   varchar(255),
    current_activity_name varchar(255),
    -- Det arbetssteg som rapporterat RUNNING och inte hort av sig sedan dess. Tomt igen sa
    -- snart samma steg rapporterar en andra gang. Ett annat externalTaskId som rapporterar RUNNING
    -- medan den har ar upptagen ar tva parallella grenar i samma instans (6.4).
    outstanding_external_task_id varchar(64),
    error_code            varchar(64),
    error_message         varchar(2048),
    started               datetime(3),
    ended                 datetime(3),
    -- TRUE medan instansen lever, NULL nar den ar terminal. NULL ar distinkt i unika index
    -- -> godtyckligt manga historiska instanser, hogst EN levande per arende.
    active_marker         bit          null,
    created               datetime(3)  not null,
    modified              datetime(3),
    primary key (id),
    key idx_ep_errand_id (errand_id),
    constraint uq_ep_process_instance_id   unique (process_instance_id),
    constraint uq_ep_one_active_per_errand unique (errand_id, active_marker),
    constraint fk_ep_errand_id foreign key (errand_id)
        references errand (id) on delete cascade
) engine=InnoDB;

-- 3. Append-only faktalogg. Processagnostisk: inga FK mot SM-metadata, ingen validering.
create table if not exists errand_process_activity (
    id                         varchar(36)  not null,
    -- Nullbar med flit: SM skriver CONFIG- och LOOP_GUARD-poster (tvetydig etikett, nodbroms) innan
    -- nagon processinstans finns. Se 4.2. errand_id ar da enda kopplingen.
    errand_process_id          varchar(36)  null,
    errand_id                  varchar(255) not null,
    external_task_id           varchar(64),             -- idempotensnyckel, stabil over retries
    activity_type              varchar(64)  not null,   -- fri strang: PHASE | TASK | INCIDENT | CONFIG | LOOP_GUARD | DELIVERY | CONCURRENCY
    activity_id                varchar(255),
    activity_name              varchar(255),
    severity                   varchar(16)  default 'INFO' not null,   -- INFO | WARN | ERROR
    message                    varchar(2048),
    error_code                 varchar(64),
    occurred_at                datetime(3)  not null,   -- processens klocka
    created                    datetime(3)  not null,   -- SM:s klocka
    primary key (id),
    key idx_epa_process_occurred (errand_process_id, occurred_at),
    key idx_epa_errand_occurred  (errand_id, occurred_at),
    key idx_epa_retention        (created),
    constraint uq_epa_idempotency unique (errand_process_id, external_task_id, activity_id),
    constraint fk_epa_process foreign key (errand_process_id)
        references errand_process (id) on delete cascade,
    -- Behovs eftersom instansnyckeln ar nullbar: utan den skulle de instanslosa posterna overleva arendet.
    constraint fk_epa_errand foreign key (errand_id)
        references errand (id) on delete cascade
) engine=InnoDB;

-- 4. Vad processen just nu vantar pa fran handlaggaren (5.9), byggd i T11 i samma fil. Ersatts i sin helhet
-- vid varje rapport. Tom mangd = processen vantar inte pa nagon manniska.
create table if not exists errand_process_signal (
    id                varchar(36)  not null,
    errand_process_id varchar(36)  not null,
    name              varchar(128) character set utf8mb4 collate utf8mb4_nopad_bin not null,
                                               -- meddelandenamnet i BPMN, t.ex. 'granskning-godkand'
    label             varchar(255),            -- visningstext, t.ex. 'Godkann granskning'
    sort_order        int          default 0 not null,   -- ordningen i rapporten, som knapparna visas i
    created           datetime(3)  not null,
    primary key (id),
    -- Jamfor namn exakt, eftersom name har kollationen utf8mb4_nopad_bin (beslut 60). Nyckelns forsta kolumn
    -- bar ocksa FK:n, sa nagot eget index pa errand_process_id behovs inte.
    constraint uq_eps_process_name unique (errand_process_id, name),
    constraint fk_eps_process foreign key (errand_process_id)
        references errand_process (id) on delete cascade
) engine=InnoDB;
```

### 3.2 Hur tabellerna hänger ihop

|                     Tabell                      |                                    FK                                    |                                                                           Motiv                                                                           |
|-------------------------------------------------|--------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------|
| `process_event_outbox` → `errand`               | **Ingen, medvetet**                                                      | Samma val som `notification_dispatch` (`V1_38`). Ett DELETE-event måste överleva att ärendet raderas                                                      |
| `errand_process` → `errand`                     | `ON DELETE CASCADE`, **ingen JPA-relation på `ErrandEntity`**            | DB-kaskaden räcker för att undvika föräldralösa rader vid `repository.deleteById` (`ErrandService.deleteErrand`). JPA-mappning vore aktivt skadlig (§1.4) |
| `errand_process_activity` → `errand_process`    | `ON DELETE CASCADE`, **nullbar**                                         | Poster utan instans måste kunna skrivas (§4.2)                                                                                                            |
| `errand_process_activity` → `errand`            | `ON DELETE CASCADE`                                                      | Krävs när instans-FK:n är nullbar — annars överlever instanslösa poster ärendet. InnoDB tillåter båda kaskadvägarna parallellt                            |
| `decision` → `errand`                           | `ON DELETE CASCADE` (mains `V1_56`), **ingen samling på `ErrandEntity`** | Beslutet följer ärendet, men det ingår inte i ärendets revision. Spårbarheten är händelseloggen och beslutets egen version (§7.5)                         |
| `errand_process_signal` → `errand_process`      | `ON DELETE CASCADE`                                                      | Signalerna är processens tillstånd, inte ärendedata. Försvinner processraden ska de följa med                                                             |
| `decision.errand_process_id` → `errand_process` | **Ingen FK**, nullbar                                                    | Processraden som fattade ett automatiskt beslut. Processrader försvinner bara tillsammans med ärendet, så referensen kan inte bli hängande (beslut 56)    |

**Retention:** aktiviteter röjs på `created` av städjobbet, efter `scheduler.process-cleanup.activity-retention` (365 d); **levererade** outbox-rader röjs när `delivered_at` är äldre än **max(loop-guard-fönstret × 6, 24 h)**; oskickade rader röjs aldrig av städningen utan ligger kvar tills de gått igenom eller åldrats ur (§8.3). Beslutet röjs aldrig separat — det följer ärendet.

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

/** Hur beslutet fattades. Skillnaden maste ga att svara pa i efterhand - FL 28 §. */
public enum DecisionMethod { MANUAL, AUTOMATIC }
```

**Beslutets utfall är inget enum.** Det kommer från mains handläggningsmodell (beslut 51) och är metadata per
namespace: `.../metadata/decisionoutcomes` registrerar vilka utfall som finns, och `DecisionValidator` avvisar ett
utfall namespacet inte har registrerat. Verksamhetsspecifika detaljer uttrycks i `legalBasis`,
`delegationReference` och `justification`.

`DecisionMethod` används på entiteten. I API:et är `method` en sträng som `@OneOf` håller till `MANUAL` och
`AUTOMATIC`, så att ett nytt värde inte blir en ny API-version (beslut 42).

`activityType` och `activityId` är däremot **fria strängar** som SM inte tolkar alls. I pw används
`PHASE`, `TASK` och `INCIDENT`. SM skriver själv `CONFIG` när etiketterna pekar åt två håll eller nyckeln är för lång för sin kolumn (§2.2)
och när en schemalagd åtgärd eller en flyttad etikett hade flyttat ärendet till en annan process (§7.4),
`LOOP_GUARD` när nödbromsen slår till (§6.5), `DELIVERY` när pw svarar `422` (§8.3), `CONCURRENCY` när två arbetssteg
är igång samtidigt (§6.4) och — med T11 och T12 — `SIGNAL` och `START` (§5.9, §5.10). Typerna står som konstanter i
`ProcessActivityLog`, som också är den enda som skriver SM:s egna poster. Det som skiljer posterna åt är felkoden:
`AMBIGUOUS_PROCESS_KEY`, `OVERSIZED_PROCESS_KEY`, `LABELS_NAME_TWO_PROCESSES`, `LABEL_MOVES_PROCESS_KEY`,
`EVENT_RATE_EXCEEDED`, `PROCESS_KEY_NOT_DEPLOYED` och `CONCURRENT_EXTERNAL_TASKS`.

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
    @UuidGenerator
    @Column(name = "id", length = 36)
    private String id;

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
    @Column(name = "outstanding_external_task_id", length = 64) private String outstandingExternalTaskId;   // 6.4
    @Column(name = "error_code",    length = 64)   private String errorCode;
    @Column(name = "error_message", length = 2048) private String errorMessage;

    @Column(name = "started") private OffsetDateTime started;
    @Column(name = "ended")   private OffsetDateTime ended;

    /** TRUE medan instansen lever, null nar den ar terminal - aldrig FALSE. Bar unikhetsconstrainten. */
    @Column(name = "active_marker")
    private Boolean activeMarker;

    @Column(name = "created")  private OffsetDateTime created;
    @Column(name = "modified") private OffsetDateTime modified;

    /** Enda stallet som far satta status - haller active_marker och ended i synk. */
    public void applyStatus(final ProcessStatus status, final Clock clock) {
        final var terminal = status.isTerminal();
        final var repeated = status == processStatus && nonNull(ended);

        this.processStatus = status;
        this.activeMarker  = terminal ? null : TRUE;

        if (!terminal) {
            this.ended = null;
        } else if (!repeated) {
            this.ended = OffsetDateTime.now(clock).truncatedTo(MILLIS);
        }
    }

    /** Lever instansen? Det ar vad active_marker sager. */
    public boolean isLive() { return activeMarker != null; }
}
```

**`applyStatus` ska vara enda vägen att sätta status.** Sätter någon `processStatus` direkt hamnar `active_marker` ur synk, och då är vi tillbaka i fällan från §4.1. Håll settern privat eller paketprivat. Frågan om en instans lever ställs till `isLive()`, och reglerna som bygger på den — levande instans, avslutat processliv, vad som går att starta — står samlade i `ProcessRules`.

Lägg märke till att `ended` nollställs när statusen går tillbaka till något som lever. Det spelar roll den dag en incident löses för hand i Operaton och en `FAILED` instans börjar köra igen — raden ska inte bära en sluttid mitt under pågående körning. Samtidigt frigjordes platsen som `active_marker` håller när instansen blev avslutad, så hann ett annat flöde starta en instans under tiden får återupplivningen `409` på `uq_ep_one_active_per_errand`. Det är rätt svar, men felmeddelandet måste tala om vilken instans som står i vägen.

En avslutad status som rapporteras igen behåller sin första sluttid. En `COMPLETED` instans väcks däremot aldrig till liv: en rapport om något annat än `COMPLETED` på en sådan instans stoppas redan i rapportvägen och lämnar raden som den är (§5.1).

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
  "processKey": "alcohol-serving",
  "processStatus": "RUNNING",
  "currentActivityId": "investigation_phase",
  "currentActivityName": "Utredning",
  "externalTaskId": "a91c7f30-4d2b-11f0-9e21-0242ac120004",
  "errandVersion": 7,
  "started": "2026-08-19T08:55:11.004+02:00",
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

**En `COMPLETED` instans förblir `COMPLETED`.** En sen eller omskickad rapport om något annat — `RUNNING`,
`RETRYING`, `WAITING` eller `FAILED` — på en instans som redan gått i mål svarar `200` med instansen som den står.
Aktiviteterna i rapporten lagras, men status, sluttid, `active_marker` och väntade signaler rörs inte. Operaton kan
aldrig återuppta en avslutad instans, och en återupplivad rad hade hävt beslutslåsen (§7.5) och brutit regel 4 (§7.4).
`200` och inte `409`, eftersom rapporten inte är fel på något sätt som ett omförsök kan rätta — den kom bara sent.
Av samma skäl är svaret `200` vilken `errandVersion` rapporten än bär.

**En `FAILED` instans förblir `FAILED` när en annan instans av ärendet är `COMPLETED`.** Reläet kan sätta en instans
som fortfarande kör i Operaton till `FAILED` (§8.3), och hinner en ny instans starta och gå i mål innan den gamla
hör av sig igen, är ärendets processliv redan slut. En rapport om något annat än `FAILED` på den gamla instansen
svarar då `200` på samma sätt som ovan och gör den inte levande igen.

**En rapport får inte göra en instans levande eller `COMPLETED` medan en annan instans av ärendet lever.** Det gäller
både raden som skapas och raden som uppdateras, och svaret är `409` som namnger den levande instansen. En andra
levande instans är det `uq_ep_one_active_per_errand` utesluter, och en `COMPLETED` för en annan instans hade avslutat
ärendets processliv mitt under den som kör. `FAILED` — till exempel en dubbelstart som pw avbryter — tas emot bredvid
den levande.

**`POST .../processes` — registrera ett startförsök.** Två fall:

| Utfall i Operaton  |                                     Kropp                                      |                                                                               SM svarar                                                                               |
|--------------------|--------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Start lyckades     | `processKey`, `processInstanceId`, `processStatus: RUNNING`                    | Raden saknas ⇒ `201` med `Location`. Raden finns redan med **samma** `processInstanceId` ⇒ `200`, och ingenting ändras. Annan **levande** instans för ärendet ⇒ `409` |
| Start misslyckades | `processKey`, `processStatus: FAILED`, `error` — **inget** `processInstanceId` | Terminal rad skapas ⇒ `201`                                                                                                                                           |

```json
{ "processKey": "alcohol-serving", "processStatus": "FAILED",
  "error": { "code": "START_FAILED", "message": "..." } }
```

**En process som gått i mål startas inte om.** Har ärendet en instans som är `COMPLETED` svarar `POST`
`409`, även om ingen instans lever just nu — och det gör även den `PUT` som skulle skapa raden för en ny instans. Ett
arbetssteg kan rapportera före pw:s `POST`, så en kontroll som bara `POST` gjorde skulle släppa in en instans startad på
ett inaktuellt lov: `PUT` skapade raden, och `POST` svarade `200` i stället för det `409` som får pw att avbryta
instansen. En `FAILED` instans står däremot inte i vägen — att försöka
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
GET .../errands/{errandId}/processes            -> 200 ErrandProcessOverview
                                                   { startable, processes }; nyast först i listan,
                                                   normalt exakt ett element (§5.10)
GET .../errands/{errandId}/process-activities   -> 200 Page<ProcessActivity>
                                                   ?processInstanceId= (valfritt filter)
                                                   &page=&size=50&sort=occurredAt,desc
GET .../errands/{errandId}/decisions            -> 200 [Decision] (§7.5)
GET .../errands/{errandId}                      -> 200 Errand med process-objektet
```

Aktiviteterna läses **per ärende, inte per processinstans**. Annars går det inte att komma åt de poster som
saknar instans (§3.1) — och det är just de posterna som förklarar varför ingen process startade.

Loggen går att sortera på `id`, `activityType`, `activityId`, `activityName`, `severity`, `message`, `errorCode`,
`occurredAt` och `created`. Något annat svarar `400` innan något läses.

**Vi bygger ingen egen endpoint för att felsöka etiketterna.** Frågan "vilka etiketter startar en process?"
går redan att svara på med `GET /{municipalityId}/{namespace}/metadata/labels`, som lämnar tillbaka hela
etikettträdet med `id`, `resourcePath`, `deprecated` och `attributes` (se `MetadataMapper.toLabel`) —
klienten filtrerar själv på attributnyckeln `processKey`. Den fråga man faktiskt ställer i drift handlar
dessutom om ett enskilt ärende, inte om hela namespacet, och den besvaras av aktivitetsloggen (§8.2).

### 5.3 Modellerna i API:et

```java
/** Kroppen i PUT och POST under /processes - det processen rapporterar om sig sjalv (beslut 65). */
@Schema(description = "What a process reports about itself: the state it is in, what it did and what it waits for")
public class ErrandProcessReport {
    @NotBlank private String processService;
    @NotBlank private String processKey;
    @Pattern(regexp = "\\S+")
    private String processInstanceId;               // required i POST, tas ur pathen i PUT; inga blanksteg
    @NotBlank private String processStatus;         // ett av ProcessStatus, @ValidEnumValue (beslut 42)
    private String currentActivityId;
    private String currentActivityName;
    private String externalTaskId;                  // idempotensnyckel for activities
    private Long errandVersion;                     // valfri; versionen steget laste (6.3)
    private OffsetDateTime started;
    @Valid private ProcessError error;
    @Valid @Size(max = 100)
    private List<@NotNull ProcessActivity> activities;     // lases via egen endpoint
    @Valid @Size(max = 50)
    private List<@NotNull ProcessSignal> awaitingSignals;  // vad processen vantar pa fran handlaggaren (5.9)
}

/** Processen som den lases - bade under /processes och som Errand.process. */
@Schema(description = "A process attached to an errand, and its state")
public class ErrandProcess {
    private String id;
    private String processService;
    private String processKey;
    private String processInstanceId;               // saknas for en start som misslyckades
    private String processStatus;
    private String currentActivityId;
    private String currentActivityName;
    private OffsetDateTime started;
    private OffsetDateTime ended;                   // satt av SM ur statusen
    private ProcessError error;
    private List<ProcessSignal> awaitingSignals;    // alltid en lista, tom nar ingen vantas eller processen avslutats
    private OffsetDateTime created;
    private OffsetDateTime modified;
}

/** Ett val handlaggaren kan gora for att stega processen vidare. Namnen kommer ur BPMN, SM tolkar dem inte. */
public class ProcessSignal {
    @NotBlank @Size(max = 128) private String name;   // meddelandenamnet i modellen, 'granskning-godkand'
    @Size(max = 255)           private String label;  // visningstext, 'Godkann granskning'
}

/** Kroppen i POST .../processes/{processInstanceId}/signals. Bara ett namn, ingen fritext (5.9). */
public class ProcessSignalRequest {
    @NotBlank @Size(max = 128) private String signal;
}

public class ProcessActivity {
    @Schema(accessMode = READ_ONLY) private String id;
    @Schema(accessMode = READ_ONLY) private String processInstanceId;   // null for poster SM skrivit utan instans
    private String activityType;                   // fri strang
    private String activityId;
    private String activityName;
    private String severity;                       // ett av ActivitySeverity, default INFO
    private String message;
    private String errorCode;
    private OffsetDateTime occurredAt;             // required
    @Schema(accessMode = READ_ONLY) private OffsetDateTime created;
}

public class ProcessError {
    @Size(max = 64)   private String code;
    @Size(max = 2048) private String message;
}
```

Beslutet har ingen modell här. Det är mains `Decision` från den gemensamma handläggningsmodellen, och det
läses på sin egen resurs (§7.5).

`Errand` utökas med en läsprojektion:

```java
@Schema(accessMode = READ_ONLY, description = "Process state driving this errand; null for namespaces without a process model")
private ErrandProcess process;
```

**Fältet är ett `ErrandField`-värde**, `PROCESS`, och går därmed genom
`AccessControlService.roleBasedFieldResolver` som allt annat på ärendet.

För ALKT gör det ingen skillnad — namespacet använder inte AccessMapper (§1.8), så resolvern vänder direkt.
Tillägget finns för nästa namespace. Designen är byggd för att bäras av fler (§7.6), och ett fält som
smiter förbi fältfiltreringen är svårt att upptäcka i efterhand just för att det fungerar i det första
namespacet som tar den i bruk.

Beteendet följer av hur resolvern redan fungerar, kontrollerat i koden:

|                   Läge                    |                                                    Utfall                                                    |
|-------------------------------------------|--------------------------------------------------------------------------------------------------------------|
| Namespace utan åtkomstkontroll — som ALKT | `roleBasedFieldResolver` returnerar `null` direkt och ingenting filtreras. Fälten syns, precis som i dag     |
| Användare som ingen restriktion träffar   | Samma sak: `null`, hela ärendet                                                                              |
| Begränsad läsning eller rollrestriktion   | Kartan är en **tillåtelselista**. `PROCESS` saknas där tills namespacet räknar upp det, alltså utelämnas det |

Tillägget är därför additivt och stängt som utgångsläge. Två saker följer: gränssnittet måste tåla att
`process` saknas för en begränsad användare, och ett namespace som vill visa det lägger till det i
`limitedReadAccess.fields`, `roleFieldRestrictions` eller `reporterAccess`.

**Beslutet står utanför fältfiltreringen, och det är med flit** (beslut 32): ärendet bär det inte. Den som
läser `.../decisions` måste i stället ha resursen `DECISION`, på nivån `LR`, och begränsad läsning når bara
resursen om namespacet räknat upp `DECISION` bland `limitedReadAccess.resources`. `justification`, som är
fritext med personuppgifter, är därmed lika stängd som utgångsläge som den hade varit som ärendefält — och
eftersom ärendet inte bär beslutet finns det heller ingen träfflista som bär hundra motiveringar (beslut 39
utgår).

**`process` reduceras inte i listsvar.** Processens tillstånd är just det listvyn ska visa (§2.1), och ingen
del av `ErrandProcess` är fritext om en person — `error.message` beskriver ett tekniskt fel och får enligt
§11 inte bära personuppgifter.

**En modell för rapporten och en för läsningen** (beslut 65). `ErrandProcessReport` är kroppen i
`PUT`/`POST .../processes`, och `ErrandProcess` är det som läses, både från `/processes` och som fältet på
ärendet. Ett tidigare utkast använde en enda modell med rapportens tre egna fält — `externalTaskId`,
`errandVersion` och `activities` — märkta `WRITE_ONLY`. De syntes då i lässchemat utan att någonsin fyllas,
och en genererad klient bar dem i sin typ för processen. Priset för två modeller är att ett fält som hör till
båda måste läggas till på båda ställena.

**`processInstanceId` går att skriva.** `POST` skickar den i kroppen — det är själva poängen med att
registrera en start (§5.1) — medan `PUT` tar den ur adressen. Skickas den ändå med i en `PUT` måste den
vara samma som i adressen, annars `400`. Den får inte vara tom eller innehålla blanksteg: en tom
`processInstanceId` i `POST` hade gett en levande rad som ingen `PUT` kan nå.

**En tom plats i en lista är ett fel i kroppen.** `activities: [null]` och `awaitingSignals: [null]` svarar `400`,
och det gör även en sortering av `.../process-activities` på ett fält loggen inte kan sorteras på — svaret räknar upp
de fält som går (§5.2).

**`process` visar den levande instansen när ärendet har en, och annars den senaste.** Utan levande instans
syns skillnaden i två lägen, och båda är sådana handläggaren måste få se: en misslyckad start lämnar efter sig
en `FAILED`-rad, och en process som gått i mål lämnar en `COMPLETED`-rad. Visade vi bara den levande skulle
båda se ut som `null` — alltså precis som "ärendet har ingen process" — och ett felstavat `processKey` hade
varit osynligt trots att §11 lovar motsatsen. Den levande går före en nyare avslutad rad, eftersom en
`FAILED` från en avbruten dubbelstart annars skymmer den process som faktiskt kör.

Det ställer ett krav på hur fältet fylls i: det ska fortfarande bli **en** fråga för hela listan, inte en
per ärende. `ErrandProcessService.findLatestProcesses` läser ärendenas alla processrader, nyast först, och
väljer per ärende i Java.

`awaitingSignals` är en barnsamling och går inte att hämta i samma fråga utan att multiplicera raderna.
Hämta dem i **en** extra fråga för hela sidan, nycklad på de processrader man redan har — två frågor
totalt, inte en per ärende. Signalerna läses bara för den valda raden per ärende, och en sida där inget
ärende har en process frågar inte efter signaler alls. **Ett namespace utan `PROCESS_CONSUMER` frågar
ingenting:** berikningen vänder på den cachade konfigurationen innan processtabellen läses, så `readErrand`,
`updateErrand` och varje sida i `findErrands` kostar där exakt det de gjorde innan fältet fanns.
`GET .../processes` läser ärendets alla processrader i en fråga.

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
  "processKey": "alcohol-serving",
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
| Allt annat                 | Tillfälligt: `5xx`, timeout, nätfel och `4xx` från WSO2. Raden ligger kvar och försöks igen                    |

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
loop-skyddet läser den inte (§6.5). **Beslutet** går den vanliga vägen för ärendeskrivningar, eftersom det *är*
ärendedata, och `method: AUTOMATIC` från fel avsändare svarar `403` — beslutets egen regel från
handläggningsmodellen, som bara pekar ut vem som får göra anspråket (§7.5). I ett namespace med
`PROCESS_CONSUMER` är det bara processkonsumenten som får det; i ett namespace utan godtas varje anropare som
inte är ett AD-konto, som på main (beslut 71).

**Kontrollen är validering, inte behörighetsprövning, och svaret är `400`** (beslut 40). SM autentiserar
ingenting inkommande — det gör WSO2 — och `X-Sent-By` sätts av anroparen själv utan att något bakom den
kontrolleras (§1.8). Ett `403` hade därför påstått en prövning som aldrig gjordes, och skickat den som
felsöker till WSO2 efter credentials när felet sitter i ett fält i kroppen. Vad reglerna faktiskt ger är
två saker: `process_service` blir en kolumn någon garanterar i stället för fritext, och ett namespace utan
processmotor kan inte samla på sig processrader. Rapportvägen skriver varken notis eller aktivitetspost med
avsändare — de aktiviteter rapporten bär är processens egna — så `X-Sent-By` krävs för att kontrollen ska ha
något att läsa, inte för att värdet sparas.
Skyddet mot den som *vill* åt skrivvägen ligger i vilka klienter WSO2 ger scope på sökvägarna — inte i en
statuskod.

#### Vilken `ProtectedResource` varje väg skyddas av

`AccessControlService.getErrand(...)` och `.verifyExistingErrandAndAuthorization(...)` **kräver** en
`ProtectedResource` och en lägsta nivå — det finns ingen överlagring utan. Varje ny endpoint måste alltså
peka ut en, och `ERRAND` är fel svar: den skulle ge processens rapporter samma behörighet som ärendet
självt. Två nya värden tillkommer, båda under `errand/`-subträdet så att ett mönster som `errand/**`
täcker dem. Beslutets `DECISION` fanns redan, från mains handläggningsmodell:

|                Väg                 |                    `ProtectedResource`                     |    Nivå     |
|------------------------------------|------------------------------------------------------------|-------------|
| `GET .../processes`                | `PROCESS` — `errand/process`                               | `R, RW`     |
| `PUT`/`POST .../processes`         | `PROCESS`                                                  | `RW`        |
| `POST .../processes/{id}/signals`  | `PROCESS`                                                  | `RW`        |
| `POST .../processes/start`         | `PROCESS`                                                  | `RW`        |
| `GET .../process-activities`       | `PROCESS_ACTIVITY` — `errand/process-activity`             | `R, RW`     |
| `GET .../decisions/**`             | `DECISION` — `errand/decision`, från handläggningsmodellen | `LR, R, RW` |
| Skrivvägarna under `.../decisions` | `DECISION`                                                 | `RW`        |

Nivåerna följer regeln i §1.3: skrivvägar kräver `RW`, läsvägar lägst `R`. `AccessControlService` tar en lägsta
nivå, så `R` släpper även igenom den som har `RW`. Beslutets läsvägar kräver lägst `LR`, som handläggningsmodellens
alla artefakter, och begränsad läsning når dem bara när namespacet räknat upp `DECISION` (§5.3). Signalen kunde ha varit en
egen resurs — att stega processen är något man kan vilja dela ut separat — men den ligger under `PROCESS`
tills behovet visar sig.

`verifyNamespaceAuthorization` används **inte** av de här vägarna. Den gäller resurser som hör till
namespacet självt, som konfiguration och metadata; våra ligger alla under ett ärende.

**`.../processes`**

|  Kod  |                                                                                                                                                                                                                   När                                                                                                                                                                                                                    |
|-------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `400` | Etikettändring som byter `processKey`; `processInstanceId` i `PUT`-kroppen skiljer sig från pathens; `POST` utan `processInstanceId` med en annan status än `FAILED`; en `processInstanceId` som är tom eller har blanksteg; en tom plats i `activities` eller `awaitingSignals`; ett fält längre än sin kolumn; sortering av `.../process-activities` på ett fält som inte går att sortera på                                           |
| `400` | `X-Sent-By` saknas; `processService` matchar inte namespacets `PROCESS_CONSUMER`; namespacet har ingen `PROCESS_CONSUMER` (beslut 40)                                                                                                                                                                                                                                                                                                    |
| `403` | Anroparen når inte ärendet — bara i namespace med åtkomstkontroll, vilket ett namespace med `PROCESS_CONSUMER` inte får ha (§7.1)                                                                                                                                                                                                                                                                                                        |
| `404` | Ärendet finns inte eller ligger i annat namespace                                                                                                                                                                                                                                                                                                                                                                                        |
| `409` | Annan levande instans för ärendet **med ett annat `processInstanceId`**, när rapporten skulle göra sin rad levande eller `COMPLETED`; ärendet har redan en `COMPLETED` instans (§7.4), vid `POST` och vid den `PUT` som skapar raden; instans med annat `process_key` än ärendets befintliga; `processInstanceId` registrerad på ett annat ärende, som inte namnges. Samma `processInstanceId` på samma ärende är aldrig `409` — se §5.1 |
| `412` | `errandVersion` i rapporten matchar inte ärendets aktuella version (§6.3)                                                                                                                                                                                                                                                                                                                                                                |

En rapport om något annat än `COMPLETED` på en instans som redan är `COMPLETED`, eller om något annat än `FAILED` på
en `FAILED` instans när en annan instans av ärendet är `COMPLETED`, är varken `409` eller `412`, utan `200` med
instansen oförändrad (§5.1).

Statuskoderna för `.../signals` står i §5.9 och för `.../processes/start` i §5.10.

**`.../decisions`** — det som tillkommer för ärenden med process

|  Kod  |                                                                                                                                                     När                                                                                                                                                     |
|-------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `403` | `method: AUTOMATIC` från någon annan än namespacets `PROCESS_CONSUMER`, eller från ett AD-konto i ett namespace utan processkonsument (beslut 71); `method: MANUAL` från en identitet som inte är ett AD-konto (§7.5)                                                                                       |
| `409` | Ärendets process är `COMPLETED`, eller beslutet är `COMPLETED` på ett ärende med process — beslutet är låst. Gäller också villkoren och bilagelänkarna, radering av en ärendebilaga som ett låst beslut länkar, radering av en utredning som ett låst beslut vilar på och radering av själva ärendet (§7.5) |
| `412` | `If-Match` matchar inte beslutets `version`, eller ett arbetssteg skriver ärendet med en `ETag` från före beslutsskrivningen                                                                                                                                                                                |

Övriga svar på `.../decisions` är handläggningsmodellens och beskrivs i `openapi.yaml`.

### 5.7 Vad som ändras för dem som redan använder API:et

**Nästan ingenting.**

Inga nya statuskoder, och tre nya spärrar som bara slår till kring processer. `POST` och `PATCH` på ärendet
svarar `400` när etiketterna skulle peka ut två processer, eller flytta ett ärende med process till en annan
(§7.4 regel 1 och 5). En etikettskrivning i metadata svarar `400` när en `processKey` är längre än 128 tecken
(§7.7). Ett låst beslut ger `409` på beslutet och på radering av ärendet (nedan). Spärrarna slår bara till i
namespace vars etiketter bär `processKey` eller vars ärenden har en process, så verksamheter utanför ALKT
märker dem inte. I
gränssnittet räcker det att visa `detail` — den säger vad som är fel och vad man gör åt det. Headern
`X-Trigger-Process` är valfri, och utelämnad betyder den precis det som gäller i dag (§6.5). Den optimistiska
samtidighetskontrollen (§6.2) använder `If-Match` och `412`, som redan finns på ärendet och dess parametrar
och redan står i specen. Verksamheter utanför ALKT märker två saker. En notis som skapas av en skrivning från en
identitet som inte är ett AD-konto — en e-tjänst med `partyId`, en integration — får identitetens värde i
`createdBy` i stället för ett tomt fält (§1.8, T3). Och en beslutsskrivning höjer ärendets version (nedan).

Ärendet får ett nytt läsfält, `process`. Det är ett rent tillägg — en klient som inte känner till det påverkas
inte.

Beslutsresursen `.../decisions` får ett nytt svar på ärenden som har en process: `409` när beslutet är låst.
På samma ärenden går varken en ärendebilaga som ett låst beslut länkar eller själva ärendet att radera (`409`).

**Varje skrivning på ett beslut, i alla namespace, ger fyra saker:** en post i händelseloggen med subtypen
`DECISION`, en notis till ärendets handläggare, en `notification_dispatch`-rad till prenumeranterna och en höjd
`errand.version`. En klient som håller ärendets `ETag` och skriver ärendet efter en beslutsskrivning får alltså
`412`, också i ett namespace utan process. Det är samma svar som när någon annan ändrat ärendet, och
det rättas på samma sätt: läs ärendet igen. Publiceringen till processen sker däremot bara där det finns en
`PROCESS_CONSUMER`.

Två skärpningar gäller namespace med `PROCESS_CONSUMER`: `method: AUTOMATIC` godtas bara från
processkonsumenten, och konfigurationen måste räkna upp `ERRAND` och `DECISION` bland triggerna (§7.1, §7.5).
I ett namespace utan processkonsument godtas `AUTOMATIC` som på main, från varje anropare som inte är ett
AD-konto (beslut 71).

Däremot finns det två saker gränssnittet **vinner på** att visa. Det ena är processläget:
`errand.process.processStatus` och `currentActivityName` berättar om en process arbetar med ärendet just
nu, och ett *"Processen arbetar med ärendet: hämtar beslutsunderlag"* räcker för att handläggaren ska
förstå varför ärendet plötsligt kan ändra sig. (Att skicka `If-Match` även från gränssnittet är också
en förbättring, för då upptäcks krocken i stället för att den sista skrivningen vinner — men det är en
fristående sak, inget krav härifrån.)

Det andra är beslutet. När det väl finns på `.../decisions` räcker det inte att visa utfallet: `method`
talar om ifall det var en handläggare eller processen som fattade det, och just den skillnaden har både
handläggaren och den sökande rätt att se. Gränssnittet bör också visa att ett beslut är låst, så att
handläggaren förstår varför en rättelse kräver ett nytt ärende.

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
dig", vilket är det normala för ett automatiskt väntläge, och en rapport som utelämnar fältet säger samma sak
— rapporten beskriver hela processens läge, precis som för `error` och `currentActivityId`. Vid läsning är
fältet alltid en lista, tom när ingen väntas, så att gränssnittet aldrig behöver skilja på "inget svar" och
"ingen knapp".

Tre saker gör SM ändå med listan, och ingen av dem är att tolka namnen:

- **En process som avslutats visar inga väntade signaler** (beslut 61). Regeln hålls i läsningen, inte när
  rapporten skrivs, eftersom mer än rapporten avslutar en process: reläet sätter den levande instansen `FAILED`
  när pw-alkt svarar `422` (§8.3), och de rader den lämnar efter sig får inte bli knappar. En signal till en
  avslutad process avvisas ändå med `409`, och en knapp som alltid avvisas är sämre än ingen.
- **En signal som står kvar behåller sin rad**, med ny etikett och plats. En flush kör inserts före deletes, så
  en rad som raderades och lades in på nytt i samma rapport hade krockat med sig själv i `uq_eps_process_name`.
- **Namnen jämförs exakt, också i databasen** (beslut 60). Kolumnen har kollationen `utf8mb4_nopad_bin`, så
  namn som skiljer i versaler, accenter eller ett avslutande blanksteg är olika signaler — precis som i Operaton.
  Samma namn två gånger i en rapport lagras en gång, det första.

#### Handläggaren svarar

```http
POST /{municipalityId}/{namespace}/errands/{errandId}/processes/{processInstanceId}/signals
```

```json
{ "signal": "granskning-godkand" }
```

Svar `202 Accepted`: SM har registrerat avsikten och publicerat händelsen. Vad processen sedan gör avgör
processen.

|  Kod  |                                        När                                        |
|-------|-----------------------------------------------------------------------------------|
| `202` | Signalen är registrerad och publicerad                                            |
| `400` | `signal` saknas eller är tom; namespacet har ingen `PROCESS_CONSUMER` (beslut 63) |
| `403` | Anroparen är ingen AD-identitet (§5.10)                                           |
| `404` | Ärendet finns inte, eller har ingen processinstans med det id:t                   |
| `409` | Signalen finns inte bland de väntade, eller processen är avslutad                 |

Kroppen valideras först (`400` för tom `signal`). Sedan kommer AD-kravet (`403`), och därefter låses ärendet
(`404` om det inte finns) innan något annat läses. Först efter låset kontrolleras processkonsumenten (`400`),
instansen (`404`), om den avslutats (`409`) och om signalen väntas (`409`). En avvisad signal skriver ingenting.

Låset kommer före alla andra läsningar av en anledning. Under repeatable read tas transaktionens ögonblicksbild
vid den första vanliga läsningen, och en läsning före låset hade låst fast signalen vid läget före den rapport
låset väntade in — den hade då kunnat godtas för en grind som rapporten just stängt.

**Namnet matchas exakt**, versaler inräknade (beslut 60). Det är namnet Operaton korrelerar på, och ett namn
som matchats utan hänsyn till versaler hade nått processen som ett namn ingen grind lyssnar på — och
försvunnit där utan spår.

**Signalen tvingar ingenting.** Den är en begäran, och väntläget avgör om den betyder något i sitt
nuvarande läge. Ett steg som lagen kräver går inte att kliva förbi genom att posta rätt sträng — och det
är hela skälet till att handläggaren inte får sätta processens läge direkt.

`409` när signalen inte står bland de väntade skyddar mot en knapp som inte längre gäller: har processen
rapporterat att den gått vidare väntar den på något annat, och den gamla knappen slutar fungera.

**Men signalen förbrukar ingenting** (beslut 64). Fram till processens nästa rapport godtas samma signal igen,
och ett dubbelklick ger två aktivitetsposter, två händelser och två utkorgsrader. pw korrelerar den första och
tar emot den andra som en signal ingen grind väntar på — informationsrad och `202`, enligt P7. Två handläggare
som trycker på olika knappar i samma stund godtas på samma sätt båda, och den som kom först vinner i processen.
Gränssnittet ska därför inte erbjuda en knapp igen efter att den tryckts, förrän ärendet lästs om.

Att SM inte tömmer listan själv är ett medvetet val. Nästa rapport är det som stänger grinden, och SM vet inte
vad en signal besvarar — en händelsebaserad gateway förbrukar alla alternativ, en avbrytknapp på en
underprocess inget av de andra. Att gissa hade antingen dolt knappar som fortfarande gäller, eller bara
skyddat mot det ena av de två fallen.

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
4. **Ingen notis** (beslut 59). Händelsen skrivs till eventloggen, men varken ärendets handläggare eller
   prenumeranterna meddelas. Signalen är riktad till processen, och det processen sedan gör med ärendet ger
   sina egna händelser.

Ingenting på ärendet ändras: ingen revision, ingen höjd `errand.version` — ett arbetssteg som läst ärendet
får alltså inte `412` av att någon tryckt på en knapp. Kommandot ligger i `ProcessCommandService`, där
startkommandot i T12 också hör hemma (beslut 62).

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
  "startable": { "status": "AVAILABLE", "processKeys": ["supervision"] },
  "processes": []
}
```

```json
{
  "startable": { "status": "PROCESS_COMPLETED", "processKeys": [] },
  "processes": [
    { "id": "1f0e4c21-...", "processInstanceId": "8f1c2b6e-...", "processKey": "alcohol-serving",
      "processStatus": "COMPLETED", "ended": "2026-08-20T14:03:11.882+02:00" }
  ]
}
```

**Två nycklar i `processKeys` betyder att någon måste välja.** Etiketterna pekar åt två håll (§7.3), och i
stället för att gissa lämnar SM över valet: gränssnittet frågar handläggaren och skickar den valda nyckeln
i kroppen. Det är samma tvetydighet som stoppar den automatiska starten — skillnaden är att här finns en
människa som kan lösa upp den. En skrivning kan inte ge ärendet två nycklar (§7.4 regel 5), men en ändring i
etikettens metadata kan, och då är det här vägen framåt.

#### Modellerna, och vad varje fält betyder

Gränssnittet ska kunna tända, släcka och förklara knappen utan att känna till en enda av reglerna i §7.4.
Beskrivningarna i specen är därför skrivna för den som läser dem i Swagger, inte för oss.

```java
/** Svaret fran GET .../processes. Kuvert, inte naken lista - se ovan. */
@Schema(description = """
    The processes attached to an errand, and whether a new one may be started right now.""")
public class ErrandProcessOverview {

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
        START_PENDING - a start is already on its way to the process engine, and the process shows up
        among the processes once the process engine has registered it, normally within seconds. Show that
        the start is on its way rather than offering it again.
        NO_PROCESS_KEY - no label on the errand carries a processKey attribute the errand can be started
        with, so there is nothing to start. Setting the right label is the fix.
        NO_PROCESS_ENGINE - this namespace does not run processes at all.
        The answer is the same whether or not the labels start the process on their own: an errand whose
        process starts by itself is AVAILABLE too, and starting it by hand is how a start that failed is
        tried again.
        Treat any value you do not recognise as not startable - values may be added over time.""",
        examples = "AVAILABLE")
    private String status;          // skrivs med ProcessStartability (beslut 42)

    @Schema(description = """
        The process keys that are eligible to start, taken from the processKey attribute on the labels of
        the errand. One element is the normal case: send it - or send nothing - to POST
        .../processes/start. Two or more elements mean the errand carries labels pointing at different
        processes and a person has to choose: ask the user and send the chosen key, otherwise the request
        is rejected with 400. An errand runs one process for the whole of its life, so once it has had one
        - a start that failed included - only the key of that process is offered. Empty whenever status is
        not AVAILABLE.""")
    private List<String> processKeys;
}

public enum ProcessStartability { AVAILABLE, LIVE_INSTANCE, PROCESS_COMPLETED, START_PENDING, NO_PROCESS_KEY, NO_PROCESS_ENGINE }

/** Kroppen i POST .../processes/start. Far utelamnas helt, eller nyckeln lamnas tom, nar bara en nyckel ar mojlig. */
@Schema(description = "A request to start a process for an errand")
public class ProcessStartRequest {

    @Schema(description = """
        Which process to start. May be omitted, or left blank, when startable.processKeys holds exactly
        one key, and is required when it holds several. The value must be one of those keys, exactly as
        given there: a request cannot name a process that the labels of the errand do not point at.""",
        examples = "supervision")
    @Size(max = 128)
    private String processKey;
}
```

**Ett fält, inte två.** Ett tidigare utkast hade `available: boolean` vid sidan av orsaken, och då finns ett
läge som säger emot sig självt: `available: false` utan orsak ger en släckt knapp utan förklaring, och
`available: true` med en orsak är rena gissningsleken för klienten. Med `status` som enda fält går
motsägelsen inte att uttrycka, och gränssnittet skriver `status === "AVAILABLE"` i stället för att väga
samman två fält. Värdena är en stängd mängd på SM-sidan och står uppräknade i fältets beskrivning, så
klienten formulerar sitt eget meddelande per fall i stället för att visa en sträng från servern. I specen är fältet
en sträng, så att ett nytt värde inte blir en ny version av API:et (beslut 42).

#### Kommandot

```http
POST /{municipalityId}/{namespace}/errands/{errandId}/processes/start
X-Sent-By: abc12def; type=adAccount
```

```json
{ "processKey": "supervision" }
```

|  Kod  |                                                                                                                När                                                                                                                |
|-------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `202` | Avsikten är registrerad och publicerad                                                                                                                                                                                            |
| `400` | Ärendet har ingen `processKey` som går att starta på sina etiketter; flera är möjliga men kroppen pekar inte ut någon; nyckeln i kroppen finns inte bland ärendets; nyckeln är längre än 128 tecken; namespacet har ingen process |
| `403` | Anroparen är ingen AD-identitet                                                                                                                                                                                                   |
| `404` | Ärendet finns inte, eller ligger i ett annat namespace                                                                                                                                                                            |
| `409` | Ärendet har en levande processinstans; ett avslutat processliv (§7.4); eller en oskickad start med en **annan** nyckel redan på väg                                                                                               |

Felen är samma regler som `startable` redovisar, och det är med flit: gränssnittet tänder knappen efter
`status` utan att duplicera kontrollerna, och servern avvisar ändå det som hunnit ändras däremellan. Med ett
undantag: `AVAILABLE` med **två** nycklar i `processKeys` betyder att klienten måste välja, och trycker den
utan att skicka någon nyckel blir svaret `400`. Bortsett från det som hinner ändras mellan läsningen och
tryckningen är det det enda fall där `startable` säger ja och kommandot ändå säger nej.

Två regler håller det så:

- **En start på väg ger `START_PENDING`**, oavsett nyckel. `startable` läser outboxen bara när en start annars
  hade varit `AVAILABLE`, med en `exists`-fråga på olevererade rader med `start_allowed = 1`. En tryckning ovanpå
  en väntande start med samma nyckel ger inget fel (steg 3 nedan), men det finns ingen anledning att erbjuda den,
  och med en annan nyckel hade kommandot svarat `409`. Värdet är också det gränssnittet visar medan starten är på
  väg (nedan).
- **En nyckel längre än 128 tecken erbjuds aldrig.** Etikettskrivningen avvisar den redan (§7.7), så en sådan
  nyckel kan bara ha kommit in förbi API:et, men `startable` hade annars tänt en knapp som kommandot avvisar med
  `400`.

**Kommandot läser inte `processStartMode`.** Läget avgör om SM startar processen åt er, inte om en människa
får göra det (§7.7). Därför fungerar knappen även i automatiskt läge — och det är den vägen man startar om
efter en misslyckad start, eftersom en `FAILED` instans varken är levande eller avslutad (§7.4 regel 4).

**Har ärendet en processrad erbjuds bara den processens nyckel** (beslut 67). Registreringen avvisar en
instans av en annan process med `409`, eftersom alla instanser av ett ärende kör samma process, och
etiketterna hålls fast vid den nyckeln (§7.4 regel 5). Men en ändring i etikettens metadata går förbi
spärren, och då hade knappen tänts för en start som pw fått avbryta. Etiketter som inte längre namnger
ärendets process ger därför `NO_PROCESS_KEY`, och kommandot svarar `400` med processens nyckel i texten —
rätt etikett tillbaka är åtgärden. Samma filter gör att ett tvetydigt ärende med en misslyckad start bara
erbjuder den nyckel som redan är vald.

#### Vad skrivningen gör

1. **En aktivitetspost** med `activityType = START`, `activityId` = den valda nyckeln, `severity = INFO`
   och en text som namnger avsändaren. Posten skrivs **utan processinstans** — någon sådan finns ju inte
   än — och det är precis därför `errand_process_id` är nullbar (§4.2). Det är den posten som i efterhand
   svarar på *vem startade handläggningen på det här ärendet, och när*.
2. **En händelse med subtypen `PROCESS`** och `startAllowed = 1`, som blir en outbox-rad och når pw. Den
   filtreras varken av `PROCESS_TRIGGER` eller av nödbromsen (§6.5), och radens `process_key` är den
   **valda** nyckeln — den löses inte upp ur etiketterna på nytt vid publiceringen, eftersom valet redan är
   gjort.
3. **Ingen andra rad, om det redan ligger en oskickad startrad med samma nyckel.** Svaret blir `202` ändå,
   och aktivitetsposten och händelsen i eventloggen skrivs som vid varje tryckning — bara publiceringen till
   pw uteblir (beslut 72). Annars hade en handläggare som trycker medan en automatisk start väntar inte
   lämnat något spår efter sig. En startrad är varje olevererad rad för ärendet med
   `start_allowed = 1`, också en automatisk start som ännu inte levererats (beslut 69). Det är
   dubbelklicksskyddet, och det är billigt — `idx_peo_guard` täcker frågan. Ligger den väntande raden på en
   **annan** nyckel är det inget dubbelklick utan ett ändrat val, och svaret är `409` som säger att en start
   med en annan process redan är på väg. Att tysta den tryckningen hade startat fel process. Ärendet låses
   före alla andra läsningar, så två samtidiga tryckningar bedöms efter varandra och den andra ser raden
   den första skrev.

Skyddet är värt sin kod trots att `409` från pw:s `POST .../processes` finns bakom: enligt §5.1 kan Operaton
lämna ut det första arbetssteget innan starten ens hunnit registreras, så en instans som avbryts kan redan
ha utfört ett steg. Billigare att inte starta den.

Svaret är `202` och inte `201` av samma skäl som signalen i §5.9: SM har registrerat avsikten, men det är
processen som avgör vad som blir av den. Instansen dyker upp i `GET .../processes` först när pw rapporterat
in den — normalt inom några sekunder tack vare direktkörningen (§2.3), i värsta fall vid nästa cron-tick.
**Gränssnittet ska visa att starten är på väg** under den tiden, inte "ingen process" — det är vad
`startable.status: START_PENDING` säger. Annars ser ett friskt system trasigt ut i ett par sekunder.

`START_PENDING` gäller så länge startraden är olevererad, inte tills pw registrerat instansen. Mellan leveransen
och pw:s `POST .../processes` visar `startable` därför `AVAILABLE` igen, och en tryckning i det glappet ger en
andra startrad. Glappet ger ingen andra levande instans — pw kontrollerar mot Operaton innan den startar, och
registreringen avvisar en andra levande instans med `409` (§5.1, §9.3) — men det är pw och registreringen, inte
`startable`, som stänger det.

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
| `RUNNING`          | TRUE            |
| `WAITING`          | TRUE            |
| `RETRYING`         | TRUE            |
| `COMPLETED`        | NULL            |
| `FAILED`           | NULL            |

En **annan** levande instans kan inte finnas samtidigt — `uq_ep_one_active_per_errand` hindrar det (§7.4).

### 6.2 När handläggare och process krockar

**Målet är att handläggarens ändringar inte ska tappas bort — inte att handläggaren ska hindras från att
arbeta.** Den skillnaden avgör hur vi löser det, och lösningen finns redan i SM.

`ErrandEntity` har `@Version`, och de sex skrivvägar som rör ärendets beslutsunderlag —
`ErrandService.updateErrand`, `ErrandParameterService.updateErrandParameters`/`updateErrandParameter`/`deleteErrandParameter`
och `ErrandJsonParameterService.updateJsonParameter`/`deleteJsonParameter` — gör alla
`entityManager.lock(..., OPTIMISTIC_FORCE_INCREMENT)`, och det gör även beslutsskrivningen i
`ErrandDecisionService`, i alla namespace (§7.5). Ingen annan väg i SM höjer versionen med tvång:
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
`severity = WARN` och texten *"concurrent external tasks detected"*. Båda rapporterna tas emot ändå — att
avvisa den ena hade tystat just den post som ska avslöja att modellen bryter mot regeln.

**Posten säger också vad man gör åt saken.** Efter inledningen namnger den båda arbetsstegen — *"task 'B'
reported RUNNING while task 'A' was still working"* — och pekar ut åtgärden: ta bort den parallella
gatewayen ur modellen, eller låt bara en gren skriva till ärendet. Den som läser posten står i ett ärende
medan felet sitter i en BPMN-fil hen inte når därifrån, så en varning som bara konstaterar problemet läses
en gång och lämnas därhän. `error_code` sätts till `CONCURRENT_EXTERNAL_TASKS`, och det är den larmet byggs
på — meddelandet bär de två task-id:na och ser därför olika ut varje gång. Posten skrivs en gång per instans, medan
varje förekomst loggas som WARN i applikationsloggen.

**Så vet SM att ett steg är klart.** Kolumnen `errand_process.outstanding_external_task_id` bär det
arbetssteg som rapporterat `RUNNING` och inte hört av sig sedan dess, och töms så fort samma
`externalTaskId` rapporterar en andra gång. Basklassen rapporterar `RUNNING` när steget börjar och lämnar
sin egentliga rapport när det slutar (§9.4), så platsen är upptagen just mellan de två — och bara då. Steg
som körs efter varandra möts därför aldrig där: Operaton slutför en task innan den delar ut nästa, så
rapporten som tömmer platsen har alltid hunnit fram innan nästa steg anmäler sig. Utan den regeln hade
varningen gått igång på varenda sekventiell process, och posten inte sagt någonting alls.

Bara steget som står på platsen tömmer den. En rapport från ett annat arbetssteg som inte själv anmäler sig, eller en
rapport som inte namnger något arbetssteg alls, lämnar platsen som den fann den. Den säger ingenting om steget
som står där, och registreringen av en start — som inte bär något `externalTaskId` — får inte läsas som att
ett steg blivit klart.

Varningsposten skrivs **utan `activity_id`**, eftersom `uq_epa_idempotency` räknar null som distinkt. En
andra varning på samma instans ska inte kunna fällas av unikhetsnyckeln — då hade den tagit rapporten den
hittades i med sig i fallet, vilket är raka motsatsen till poängen.

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
och 3 fångar. En rad för lite blir en process som står och väntar för alltid utan
att någon märker det. Därför betyder allt utom exakt `false` *väck processen*.

**Headern hedras inte för AD-identiteter.** En handläggares skrivning väcker alltid processen, hur klienten
än sätter headern. Villkoret är ett enda (`ServiceUtil.getAdUser() == null`) och det stänger det enda hål
en fritt satt header annars öppnar: att någon annans integration råkar tysta äkta ärendeändringar.
Maskin-till-maskin-anropen — pw och kommande processmotorer — är just de som saknar AD-konto.

**Lager 2 — vad hände?** Bara de händelsetyper som står i `PROCESS_TRIGGER` går vidare. Det lagret bryr sig
inte om vem som skrev, bara om vad som ändrades, och kompletterar därför lager 1. Det frågas före lager 3,
eftersom det är en uppslagning i cachad konfiguration medan bromsen är en fråga mot databasen (§2.2).

**Lager 3 — nödbromsen.** Den bryr sig varken om vem eller vad, utan bara om takten: räkna raderna med
`errand_id = ? and delivered_at is not null and created > now() - fönstret`. Når det tröskeln
(20 stycken på 10 minuter) skrivs ingen rad, en felpost (`LOOP_GUARD`, `EVENT_RATE_EXCEEDED`) hamnar i
aktivitetsloggen och en ERROR-rad i applikationsloggen. Hälsoindikatorn rörs inte: bromsen gäller ett ärende, inte
tjänsten.

**Alla tre lagren gäller härledda händelser — kommandon passerar dem.** Handläggarens signal (§5.9) och
manuella start (§5.10) är inte något som hänt med ärendet, utan begäranden riktade rakt till processen.
**Alla tre lagren undantar dem uttryckligen**, lager 1 inräknat: ett kommando är ingen ärendeändring, och en
människa som trycker på en knapp är ingen loop. Lager 1 hade visserligen släppt dem ändå — headern hedras
inte för AD-identiteter, och kommandoendpointerna kräver ett AD-konto — men att förlita sig på den kedjan
gör knappen beroende av att `403`-kontrollen i §5.10 finns kvar. Glömdes den skulle kommandot tystas i
stället för att avvisas, alltså den tystaste av felvägar. Undantaget i publiceraren kostar ingenting och
tar bort beroendet.

Undantaget för **lager 3** är det som är lätt att missa och dyrast att glömma. Ett ärende med livlig trafik
under intaget kan trippa bromsen, och utan undantaget skulle startkommandot då kastas medan endpointen
svarar `202`. Handläggaren trycker, aktivitetsloggen säger
"startad manuellt", och ingen process startar — den tystaste felvägen i hela designen, och den slår till på
just de ärenden som har mest att göra. Bromsen ska mäta takten mellan tjänsterna, inte hindra en människa
från att komma igång.

**`DELETE` passerar också alla tre lagren** (beslut 43). En radering kan inte loopa — ärendet finns inte längre — och
en radering som hålls tillbaka lämnar processinstansen levande i Operaton för ett ärende som inte finns. Det gäller
även en radering från en maskinidentitet med `X-Trigger-Process: false`, och även när `ERRAND` saknas i
`PROCESS_TRIGGER`.

**Skrivningen där en handläggare gör ett beslut `COMPLETED` passerar bromsen, men bara den** (beslut 54). Det
är den händelse ett väntläge väntar på, och kastas den står ärendet still hur mycket annan trafik som än orsakade
att bromsen slog till. En människa som fattar ett beslut är ingen loop, och på ett ärende med process är beslutet
låst när det väl är färdigt (§7.5), så övergången sker en gång. Processens egna avslut omfattas inte: varje
beslut processen skapar färdigt är ett nytt, så en process som glömt att be om att slippa väckas hade kunnat
skapa beslut efter beslut förbi bromsen — och den behöver aldrig väckas av sitt eget beslut. Lager 1 och 2
gäller som vanligt, och ett namespace utan `DECISION` bland triggerna får ingen rad.
`EventService.createDecisionEvent` talar om för publiceringen att händelsen är en sådan övergång, och
publiceringen avgör själv om skrivaren är ett AD-konto.

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
processens egen identitet aldrig ska förekomma bland outbox-radernas `executed_by` (§8.1). Gör den det
skrivs rader den skulle ha tystat, och headern är fel eller borta.

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

**`DECISION` är lika obligatorisk.** Beslutet skrivs till `.../errands/{errandId}/decisions` (§7.5) och
processen väntar på det. Saknas triggern publiceras ingen outbox-rad för den skrivningen, processen får
aldrig veta att beslutet är fattat, och instansen står kvar i `WAITING` för alltid.

**Båda kontrolleras när konfigurationen skrivs** (beslut 55). Har namespacet en `PROCESS_CONSUMER` svarar
`NamespaceConfigService` `400` om `ERRAND` eller `DECISION` saknas bland triggerna, och felmeddelandet säger
vad som går förlorat utan dem. Det gör en tyst driftstörning till ett högljutt konfigurationsfel, på samma sätt
som spärren mot åtkomstkontroll nedan. En konfiguration som sparades före kontrollen prövas först när den
skrivs nästa gång, så ett namespace som redan kör bör kontrolleras för hand. Triggers utan konsument prövas
inte — de väcker ingen, och står kvar till den dag en konsument läggs till.

**Kommandon står utanför listan.** `PROCESS_TRIGGER` säger vilka *ärendeändringar* som är värda att berätta
om för processen. Handläggarens signal (§5.9) och manuella start (§5.10) är inga ärendeändringar utan
kommandon riktade rakt till processen, och de publiceras alltid. Det är därför `SIGNAL` inte längre står i
listan: en knapp som ser ut att fungera men inte gör något är precis den tysta felväg konfigurationen inte
ska kunna orsaka (§7.7). Konfigurationen avvisar dem numera med `400`, eftersom en uppräknad kommandotyp ser
ut att styra något den aldrig styr.

**Startläget konfigureras inte här utan på etiketten.** `PROCESS_CONSUMER` säger att namespacet kör
processer; `processStartMode` säger om de startar av sig själva. Ansökan och tillsyn ligger i samma
namespace och vill ha olika svar, och därför sitter valet per etikett (§7.7).

**Båda nycklarna läses ur `namespaceConfigCache`, som är per podd.** Cachen är en Caffeine-cache i minnet med
`expireAfterWrite=10m`, och en skrivning av konfigurationen evikterar bara nycklarna i den podd som tog emot
anropet. En ändrad `PROCESS_CONSUMER` eller triggerlista slår därför igenom direkt i den podden, men först inom
tio minuter i de andra. Under den tiden kan podderna publicera olika: en ny trigger ger rader bara från den podd
som evikterat, och en borttagen konsument ger rader tills cachen gått ut. Det är ofarligt för en ändring som görs
i förväg, men en konfigurationsändring som ska gälla från en viss tidpunkt behöver tio minuters marginal.

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
  direct-run: { enabled: true, core-pool-size: 2, max-pool-size: 4, queue-capacity: 500 }   # 2.3
scheduler:                                  # nyckelnamnen foljer notification-dispatch
  process-event:
    name: process_event_relay
    cron: "0 * * * * *"
    shedlock-lock-at-most-for: PT2M
    maximum-execution-time: PT1M
    transaction-buffer: PT5S                # cronjobbet tar inga yngre rader an sa, 2.3
    max-age: P30D                           # sista utvagen for en rad som aldrig gar igenom, 8.3
    unhealthy-after: PT15M                  # aldern pa aldsta oskickade raden, 8.3
    batch-size: 200                         # tak per korning, och per arende i direktkorningen, 7.6
  process-cleanup:
    name: process_event_cleanup
    cron: "0 30 2 * * *"
    shedlock-lock-at-most-for: PT10M
    maximum-execution-time: PT5M
    batch-size: 1000                        # rader per borttagning, var sin transaktion
    activity-retention: P365D               # aktivitetsloggen, 3.2
resilience4j.circuitbreaker.instances:
  pw-alkt:                                  # raknar bara anrop utan svar, 7.6
    ignoreExceptions:                       # svar om en enskild handelse raknas inte
      - se.sundsvall.dept44.exception.ClientProblem
      - se.sundsvall.dept44.exception.ServerProblem
    slidingWindowSize: 5
    minimumNumberOfCalls: 3
    waitDurationInOpenState: PT30S
```

Relayets och städjobbets egna inställningar läses av `ProcessEventRelayProperties` (`scheduler.process-event`) och
`ProcessEventCleanupProperties` (`scheduler.process-cleanup`), där också standardvärdena står. `name`, `cron`,
`shedlock-lock-at-most-for` och `maximum-execution-time` läses av schemaläggaren, som för tjänstens övriga jobb.
Städjobbet loggar hur många levererade rader och aktivitetsposter det tog bort.

`PROCESS_CONSUMER` valideras vid skrivning mot namnet på den enda klient relayet har, `pw-alkt` (beslut 41). Namnet
är adressen: det är samma sträng som Feign-målet under `integration`, som OAuth2-registreringen och som
`PROCESS_CONSUMER` i `namespace_config` pekar ut. En felstavad konsument avvisas därför med `400` i stället för att
tyst sluta fungera. Loop-skyddet läser inte namnet (§6.5).

I `application-it.yml` och `application-junit.yml` sätts samtliga cron till `"-"` och
`process-engine.direct-run.enabled` till `false`. `ProcessEventRelayIT` slår på direktkörningen igen.

### 7.3 Så vet SM vilken process ett ärende hör till

Svaret ligger i attributet `processKey` på etiketten. Vi tittar bara på ärendets egna etiketter och går
alltså **inte** uppåt eller nedåt i etikettträdet.

```sql
insert into metadata_label_attribute (metadata_label_id, `key`, `value`) values
  ('9c1a...', 'processKey',       'alcohol-serving'),
  ('9c1a...', 'processStartMode', 'AUTOMATIC'),
  ('4f8b...', 'processKey',       'supervision'),
  ('4f8b...', 'processStartMode', 'MANUAL');
```

Attributet `processStartMode` avgör om SM startar processen åt handläggaren eller om någon ska trycka på en
knapp. Det läses ur **samma etikett** som gav nyckeln, och beskrivs i §7.7.

|     Utfall      |                                                                 Resultat                                                                 |
|-----------------|------------------------------------------------------------------------------------------------------------------------------------------|
| Exakt en nyckel | Den processen startas                                                                                                                    |
| Noll            | Ingen process. Inte ett fel                                                                                                              |
| Två eller fler  | **Ingen automatisk start**, ERROR-aktivitet som namnger båda. En manuell start löser upp tvetydigheten genom att peka ut nyckeln (§5.10) |

Tabellen beskriver vad som händer när ärendet **redan** pekar ut två nycklar. En skrivning som skulle leda dit avvisas (§7.4 regel 5), men ett ärende kan ändå hamna där om någon lägger `processKey` på en etikett som ärendet redan bär — därför behövs raden.

Etiketter som är märkta `deprecated` räknas inte. Nycklarna i exemplen är pw-alkts egna: `Constants.PROCESS_KEYS` i
pw-alkt räknar upp de tio processer som finns. **SM kontrollerar inte att nyckeln finns på riktigt** — det är bara pw som vet vilka processer som är driftsatta, och en nyckel som inte finns fångas som `422` (§5.4).

**Etiketterna läses som ärendet bär dem, inte bara som Hibernate fyllt i dem.** En etikett på ärendet känner sin
metadataetikett först när ärendet lästs från databasen. Ett ärende som skapas, genom API:t eller e-postintaget, en
`PATCH` som sätter etiketterna och en `ADD_LABEL`-åtgärd som körs direkt i samma skrivning bär därför etiketter med bara
ett id under resten av transaktionen — och det är just de skrivningar som ger ärendet dess processetikett.
`ProcessKeySelector.select` slår upp sådana etiketter på id, i en fråga och bara när det finns någon (beslut 47). Innan
dess fick ett ärende som skapades med rätt etikett ingen nyckel vid publiceringen, alltså ingen rad och ingen process,
och ingenting syntes. T8 hittade felet.

`ErrandLabelService` fyller dessutom i metadataetiketten på de etiketter en skrivning sätter, så att svaret på en
`PATCH` visar hela etiketter och inte bara id. Uppslagningen i selektorn behövs ändå, eftersom `ADD_LABEL` lägger till
etiketter utan att gå den vägen.

En `ADD_LABEL` som körs **schemalagt** publicerade först ingenting alls, eftersom `ActionWorker` lade till etiketten
utan att skapa revision eller ärendehändelse. Nu skapar den båda när åtgärden ändrat ärendet, men ingen notis (beslut
48). Ett ärende som får sin processetikett den vägen startar alltså processen direkt, inte först vid nästa skrivning.

### 7.4 En process per ärende — och hur den regeln hålls

Fem regler tillsammans:

1. **En etikettändring får inte flytta ett ärende till en annan process** så snart ärendet har en processrad — även om den processen är avslutad — eller en start på väg till pw. Etiketterna ska fortsätta peka ut samma `processKey` som före ändringen.
2. Högst en **levande** instans per ärende. Den regeln bär databasen själv via `active_marker` (§4.1).
3. Alla instanser på samma ärende har samma `process_key`. Den kontrollen får tjänstelagret göra under radlås; den går inte att uttrycka i databasen.
4. **När en instans blivit `COMPLETED` är ärendets processliv slut.** Ingen ny instans får startas — nästa process är ett nytt ärende (beslut 6). En `FAILED` instans stoppar däremot ingenting; att försöka igen efter en misslyckad start är återhämtning.
5. **Ett ärendes etiketter får peka ut högst en `processKey`.** Det gäller alla ärenden, med eller utan process, och hålls redan när ärendet skapas (beslut 45).

#### Regel 1 och 5 — etiketterna

Båda reglerna handlar om samma sak: att etiketterna tyst slutar svara på frågan vilken process ärendet hör till. Det kan ske på två sätt, och inget av dem ger något fel någonstans — ärendet står bara still tills någon undrar varför:

```
två etiketter pekar ut två processer -> de löser ut till ingen, och ingen process startas eller får veta något
nyckeln byts mot en annan            -> instansen som kör får aldrig veta något, och ingenting fallerar
```

Därför avvisas ändringen när den görs, inte när händelsen publiceras. Det är samma val som i §7.7: hellre ett högljutt konfigurationsfel än en tyst driftstörning.

**Regel 1 jämför före med efter.** Frågan är vilka nycklar etiketterna löser ut till före ändringen och vilka de skulle lösa ut till efter — inte vad de löser ut till jämfört med nyckeln på processraden. Då fångas både att nyckeln byts och att den försvinner: ett ärende som inte längre pekar ut någon process slutar få väckningar lika tyst som ett som pekar fel. Det är **mängden** nycklar som jämförs, inte den enda nyckel de löser ut till. Ett tvetydigt ärende löser ut till ingen nyckel alls, precis som ett ärende utan nycklar, och en jämförelse av den enda nyckeln hade därför släppt igenom att ett tvetydigt ärende tappar alla sina.

**En start på väg räknas som den process ärendet kör.** Har ärendet ingen processrad men en olevererad outbox-rad med `start_allowed = 1`, är det den radens nyckel regeln håller etiketterna till. Utan det hade ett ärende som skapats med etikett A och bytt till B innan pw hunnit registrera starten släppts igenom, fått ett nytt startlov för B, och kört A för alltid medan etiketterna sagt B. Publiceringen ger av samma skäl inget startlov för en nyckel när en start med en annan nyckel redan är på väg (§2.2 steg 6).

**En ändring som pekar tillbaka på den process ärendet faktiskt kör släpps alltid igenom**, oavsett vad etiketterna pekade på innan. Utan det undantaget finns ingen väg tillbaka för ett ärende vars etikett redan har tappat nyckeln — att sätta tillbaka den är också en ändring. Och det är den enda etikettändring som inte kan peka ärendet någon annanstans än dit det redan pekar.

**Bara nyckeln hålls still.** `processStartMode` läses ur samma etiketter men får bytas fritt, även på ett ärende med process. Attributet säger om SM startar processen åt handläggaren (§7.7), inte vilken process ärendet kör, och att byta läge är just så automatisk start rullas ut.

**Regel 5 frågas först.** Pekar etiketterna ut två nycklar spelar det ingen roll vad ärendet kör — det är fel redan innan något har startats. Processraderna läses därför först när regel 5 har passerat.

**Ett ärende som redan pekar ut två nycklar får `400` på varje etikettändring** tills tvetydigheten är löst. Vägen ut finns alltid: ändringen som tar bort den ena etiketten löser ut till en nyckel och släpps igenom. Ett ärende kan hamna där trots regel 5, om någon lägger `processKey` på en etikett som ärendet redan bär — ärendet rörs inte, så ingen skrivning fångar det. Därför finns läs-sidans hantering i §7.3 kvar, och därför kan en manuell start fortfarande behöva peka ut nyckeln (§5.10).

**Fyra vägar skriver etiketter, och kontrollen finns på alla** — en kontroll på bara några av dem är ingen kontroll:

|                        Väg                        | Regler  |                          Utfall                           |
|---------------------------------------------------|---------|-----------------------------------------------------------|
| `POST /errands` — även handover och e-postintaget | 5       | `400`. Ett ärende som skapas har ingen process än         |
| `PATCH /errands/{errandId}`                       | 1 och 5 | `400`                                                     |
| `AddLabelAction.executeAction`                    | 1 och 5 | Etiketten läggs inte till, och en ERROR-post skrivs       |
| `ErrandService.persistLabelUpdate`                | 1 och 5 | Ärendet behåller sina etiketter, och en ERROR-post skrivs |

`AddLabelAction` körs av ett schemalagt jobb och passerar ingen endpoint, så där finns ingen anropare att svara. Etiketten läggs inte till, och felposten i aktivitetsloggen är det enda som syns på ärendet. Utan den slutar processen tyst att få väckningar. Posten skrivs en gång per ärende, fel och fönster, som övriga poster SM skriver själv (§2.2). En uppgift som stoppats skapas om vid nästa ändring av ärendet, eftersom etiketten aldrig kom på plats.

`ErrandService.persistLabelUpdate` används av `LabelMoveWorker`, som bygger om etiketterna på varje ärende som bär en etikett som flyttats i metadatan. Inte heller där finns någon anropare, så en flytt som skulle byta ärendets process lämnar ärendet som det är och skriver en felpost på det. En flytt som går igenom ger revision och ärendehändelse utan notis, som en schemalagd åtgärd (beslut 48), och väcker därmed processen.

Posten från de två schemalagda vägarna har felkoden `LABELS_NAME_TWO_PROCESSES` eller `LABEL_MOVES_PROCESS_KEY`, och texten säger efter orsaken vad skrivaren gjorde åt det.

Kontrollen frågas mot de etiketter ärendet faktiskt skulle bära, alltså **efter** att förfäderna lagts till. En förälder kan vara etiketten som pekar ut processen.

#### Regel 4 och de unika nycklarna

Regel 4 går, precis som regel 3, inte att lägga i databasen, eftersom `active_marker` är NULL för både `COMPLETED` och `FAILED` och alltså inte skiljer dem åt. Kontrollen ligger därför där en processrad skapas — både i `POST .../processes` och i den `PUT` som rapporterar en instans SM inte sett (§5.1) — i den `PUT` som skulle göra en `FAILED` instans levande igen (§5.1), i `POST .../processes/start` (§5.10) och i startlovet som publiceraren räknar ut (§7.7). Flera ställen ställer samma fråga, eftersom starten kan komma från flera håll. Att den ligger i SM och inte bara i pw är medvetet: pw frågar Operaton om vad som kör just nu, och där syns inte avslutade processer alls (§9.3).

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

|      Nivå      |            Invariant             |                                         Upprätthålls av                                         |
|----------------|----------------------------------|-------------------------------------------------------------------------------------------------|
| Processinstans | Högst en levande per ärende      | `uq_ep_one_active_per_errand`                                                                   |
| Processliv     | En `COMPLETED` startas aldrig om | `ErrandProcessService`, när en processrad skapas och när en `FAILED` rapporteras levande (§7.4) |
| Beslut         | Ett per ärende                   | `SINGLE_DECISION_PER_ERRAND` i namespacets konfiguration, `DecisionValidator` (`409`)           |

Den tredje regeln är en inställning och ingen unik nyckel, eftersom beslutsmodellen delas med verksamheter där
interimistiska beslut, delbeslut och omprövning är vardag. **ALKT måste alltså slå på
`singleDecisionPerErrand`** för att regeln ska hålla där. Inställningen läses cachad, som namespacets övriga
inställningar (§7.1): en ändring gäller direkt i den podd som tog emot den och i de andra inom tio minuter.

#### Var beslutet lagras

**I mains gemensamma handläggningsmodell, och ingen annanstans** (beslut 51). Tabellen `decision`
(`V1_56`), modellen `Decision` och resursen nedan byggdes för alla verksamheter — bygglov, miljötillsyn,
individärenden — och ALKT använder dem som de är:

```
POST   /{municipalityId}/{namespace}/errands/{errandId}/decisions                -> 201
GET    /{municipalityId}/{namespace}/errands/{errandId}/decisions                -> 200 [Decision]
GET    /{municipalityId}/{namespace}/errands/{errandId}/decisions/{decisionId}   -> 200 Decision, ETag
PATCH  /{municipalityId}/{namespace}/errands/{errandId}/decisions/{decisionId}   -> 200, If-Match
DELETE /{municipalityId}/{namespace}/errands/{errandId}/decisions/{decisionId}   -> 204, If-Match
       .../decisions/{decisionId}/terms, /attachments, /json-parameters
```

```json
{ "type": "PERMIT",
  "status": "COMPLETED",
  "outcome": "APPROVAL",
  "method": "MANUAL",
  "decidedBy": "anna.andersson",
  "decidedAt": "2026-09-14T10:12:00+02:00",
  "legalBasis": "8 kap. 12 § alkohollagen",
  "delegationReference": "3.2.1",
  "justification": "...",
  "appealable": true }
```

Planen var länge en egen `errand_decision` med `@OneToOne` på ärendet, `Errand.decision` och ett beslut i
ärendets revision. Den skrinlades när handläggningsmodellen kom till main först: två beslutsmodeller hade
behövt hållas i takt, och den som läser beslut — e-tjänst, arkiv, processen — hade fått fråga sig vilken av
dem som gäller.

**Fasta fält, inte ett fritt dokument.** Ett beslut har en form som följer av förvaltningslagen, och den är
sig lik oavsett om det gäller bygglov, försörjningsstöd eller tillsyn: utfall, vem som fattade det, när,
med stöd av vilket lagrum eller vilken delegationspunkt, och varför. Den formen hör hemma i modellen — där
kontrolleras den när den kommer in, den syns i `openapi.yaml` och den går att söka i. Två saker skiljer
modellen från den ursprungliga planen:

- **`outcome` är metadata, inte ett enum.** Utfallen registreras per namespace på
  `/metadata/decisionoutcomes`, och ett utfall namespacet inte känner till ger `400`. ALKT registrerar sina
  på samma sätt som alla andra.
- **`status` är beslutets livscykel** — `DRAFT`, `ACTIVE`, `COMPLETED`, `CANCELLED` — och det är den som
  säger när beslutet är fattat. `outcome` och `decidedAt` krävs redan när beslutet skapas, så ett
  `COMPLETED` beslut har alltid båda.

**Uppgifter som blir kända efter beslutet**, som laga kraft och delgivning, hör hemma i beslutets
JSON-parametrar (`decision_json_parameter`, med registrerat schema). Det är därför de står utanför låsen
nedan.

Fyra andra lösningar övervägdes och valdes bort redan innan handläggningsmodellen fanns, och skälen gäller
fortfarande:

- **`json_parameter` på ärendet med registrerat schema.** Kräver att ett JSON-schema förvaltas per
  verksamhet för ärendets mest formbundna dokument, och otypad lagring för det.
- **Kolumner eller JSON på `errand_process`.** Ett myndighetsbeslut är ärendedata, inte processmaskineri.
  Det ska gå att läsa för e-tjänst och arkiv utan att man vet något om Operaton, det finns även på ärenden
  helt utan process, och vid en omstart efter `FAILED` hade det hamnat på fel rad (§7.4).
- **Vanliga parametrar.** `parameter_values.value` rymmer 255 tecken — en motivering får inte plats.
- **Aktivitetsloggen.** Den är en logg som SM inte tolkar, den har ingen unikhet, och enligt §11 får
  `message` inte innehålla personuppgifter — vilket en beslutsmotivering nästan alltid gör.

#### Vem som får skriva beslutet

Två kan fatta beslutet, men de går in samma väg:

|                             Fall                             |  `method`   |          Vem skriver           |                               Känns igen på                               |
|--------------------------------------------------------------|-------------|--------------------------------|---------------------------------------------------------------------------|
| Handläggaren fattar beslutet                                 | `MANUAL`    | ett AD-konto                   | `X-Sent-By` med `type=adAccount`, och RW på beslutet                      |
| Processen fattar beslutet självt                             | `AUTOMATIC` | namespacets `PROCESS_CONSUMER` | `X-Sent-By` har samma värde som `PROCESS_CONSUMER`, och är inget AD-konto |
| En tjänst fattar beslutet, i ett namespace utan processmotor | `AUTOMATIC` | vilken tjänst som helst        | `X-Sent-By` är inget AD-konto (beslut 71)                                 |

**Regeln kontrolleras när beslutet kommer in** (beslut 56 och 71): `MANUAL` godtas bara från ett AD-konto och
`AUTOMATIC` bara från en anropare som inte är det. Har namespacet en `PROCESS_CONSUMER` godtas `AUTOMATIC` bara
från den. Allt annat ger `403`. I ett namespace utan processmotor gäller alltså handläggningsmodellens regel från
main oförändrad, så att en tjänst som redan skriver automatiska beslut där inte slutar fungera när
processintegrationen driftsätts. Utan den kontrollen skulle en handläggare kunna stämpla sitt eget beslut som
automatiskt, eller en process stämpla sitt som manuellt — och det är just den skillnaden man måste kunna svara
på i efterhand (förvaltningslagen 28 § och dataskyddsförordningen artikel 22 om automatiserat
beslutsfattande). Metoden som prövas är den beslutet får efter skrivningen, så en `PATCH` som utelämnar
`method` prövas mot den lagrade.

`X-Sent-By` sätts av anroparen, så kontrollen håller stämpeln till anroparens avsikt snarare än bevisar vem
den är (§1.8). Det räcker för det den ska göra: ett beslut kan inte få fel metod av misstag. Handläggningsmodellen
svarade redan `403` på regeln, och det står sig — till skillnad från processrapportens avsändarkontroll
(beslut 40) handlar den om vem som får göra ett anspråk, inte om ett fält som är fel.

`errandProcessId` fyller SM i själv och tar aldrig emot från klienten: vid `AUTOMATIC` ärendets levande
processrad, och den rad beslutet redan pekar ut om ingen lever; vid `MANUAL` ingenting. Kolumnen har ingen
främmande nyckel, eftersom processrader bara försvinner tillsammans med ärendet.

#### Vad som händer när beslutet skrivs

1. **Att skapa, ändra eller radera ett beslut ger en ärendehändelse med subtypen `DECISION`** (beslut 52),
   av typen `UPDATE` och längs samma väg som alla andra ärendehändelser (§1.1). Händelsen pekar inte ut
   någon revision: beslutet ingår inte i ärendets ögonblicksbild. Spårbarheten är händelseloggen, beslutets
   `createdBy`, `modifiedBy` och `version`, och `method` och `decidedBy` på beslutet självt.
2. **`errand.version` höjs.** Ett arbetssteg som håller en äldre ETag får `412` och kör om sig (§6.2). Det är
   rätt: beslutet ändrade ju underlaget.
3. **Villkoren, bilagelänkarna och JSON-parametrarna gör ingetdera.** Processen väntar på att beslutet blir
   färdigt, och varje extra händelse räknas av nödbromsen. En fil som laddas upp via beslutet blir ändå en
   `ATTACHMENT`-händelse, precis som all uppladdning.
4. **`DECISION` måste finnas i `PROCESS_TRIGGER`** (§7.1), annars skrivs ingen outbox-rad och processen
   får aldrig veta att beslutet är fattat. Konfigurationen avvisas utan den.
5. Är det **processen själv** som skriver beslutet stoppas outbox-raden av loop-skyddets första lager
   (pw sätter `X-Trigger-Process: false`, §6.5). Också rätt: processen behöver inte väckas av sitt eget
   beslut.
6. **Skrivningen där en handläggare gör beslutet `COMPLETED` passerar nödbromsen** (beslut 54), men inte
   lager 1 och 2. Den är den enda händelse väntläget behöver, och en broms som trippats av annan trafik på
   ärendet hade annars lämnat det stående. Processens egna avslut bromsas som andra skrivningar från processen
   (§6.5). I händelseloggen heter skrivningen "Ett beslut i ärendet har fattats.", också när beslutet skapas
   färdigt.

#### När beslutet låses

Låsen gäller bara ärenden som har en process — minst en processrad (beslut 53). Svaret är `409`.

- **När ärendets process är `COMPLETED`** kan inget av ärendets beslut skapas, ändras eller raderas. Det är
  samma `ProcessRules.hasCompletedProcess` som hindrar att processen startas om (§7.4). En kontroll,
  två användningar.
- **När beslutet är `COMPLETED`** är det låst som det står, också innan processen hunnit gå i mål. Att backa
  statusen och att radera beslutet ingår, och det är det lagrade beslutet som prövas: en `PATCH` som gör
  beslutet färdigt släpps igenom, men inte nästa.
- **Villkoren och bilagelänkarna** låses med beslutet.
- **En bilaga på ärendet som ett låst beslut länkar går inte att radera**, och inte heller **en utredning
  som ett låst beslut vilar på**. Länken respektive beslutets hänvisning till utredningen hade annars
  försvunnit genom databasens kaskad, förbi beslutets regler — och hänvisningen hade inte gått att sätta
  tillbaka, eftersom beslutet är låst. Spärrarna gäller API-vägen; gallringen tar allt.
- **Ärendet självt går inte att radera** med `DELETE /errands/{errandId}` när ett av dess beslut är låst — när
  processen är `COMPLETED`, eller när ärendet har ett `COMPLETED` beslut. Beslutet hade annars försvunnit genom
  kaskaden. Spärren prövas efter `If-Match` och innan något tas bort, och ett ärende utan beslut eller utan
  processrad raderas som vanligt.
- **JSON-parametrarna låses inte.** Laga kraft och delgivning blir kända först efter beslutet.

**Gallringen går förbi alla spärrarna.** Den tar ärenden som inte rörts sedan den tidpunkt körningen pekas på,
som måste ligga minst `errand.purge.minimum-age` (två år) bakåt, låsta beslut inräknade, och det är bevarandet och inte beslutsregeln som avgör när
ett ärende får försvinna. I ett namespace med `PROCESS_CONSUMER` publicerar den en `DELETE` för varje ärende den
tar bort, så att en instans som fortfarande lever i Operaton — ett ärende som stått orört i en grind i två år —
får veta att ärendet är borta. Publiceringen skriver bara outbox-raden, varken händelse i eventloggen eller notis,
eftersom gallringen inte ska lämna något spår av ärendet efter sig.

Så länge processen lever och beslutet inte är färdigt går det att skriva om — steget som förbereder beslutet
kan behöva rätta sig självt, och en handläggare kan upptäcka ett stavfel. Ärenden **utan** process låses
aldrig: där är händelseloggen spårbarheten. Och ska ett låst beslut ändras är vägen ett nytt ärende,
kopplat till det gamla.

#### Det här hänger på hur processen är modellerad

Väntläget som avvaktar beslutet **väntar på ett beslut med `status = COMPLETED`**, läst på
`GET .../decisions`, och **måste läsa om ärendet när det går in i väntan** (§9.2 punkt 1). Skrivs beslutet
medan processen är mitt i ett arbetssteg finns det ingen som lyssnar, väckningen sväljs som en
`MismatchingMessageCorrelation` och är sedan borta. Om processen då inte tar reda på hur ärendet faktiskt ser
ut när den börjar vänta, blir ärendet stående för alltid — med ett färdigt beslut liggande i databasen.
Det är det allvarligaste misstag man kan göra i den här lösningen, och ingen kod i SM kan rädda det.

---

### 7.6 Fler namespace och fler processmotorer

Det mesta av det som skiljer ett namespace från ett annat är data: `PROCESS_CONSUMER` och `PROCESS_TRIGGER` i
`namespace_config` (§7.1), `processKey` på etiketterna (§7.3), och processmodellerna i pw. Ingenting av det kräver
en release.

**Ett namespace har exakt en processkonsument.** `namespace_config_value` tillåter tekniskt flera värden
per nyckel (§1.5), men `PROCESS_CONSUMER` läses som ett. Ett namespace är en verksamhet, och en verksamhet
har en processmotor. Behövs två är det två namespace.

#### Relayet levererar bara till pw-alkt

Relayet har **en** klient, `PwAlktClient`, byggd som tjänstens övriga Feign-klienter, och ingen uppslagning från
namn till klient (beslut 41). Det finns en processmotor, och REST-transporten ska ersättas av RabbitMQ (§2.4). En
uppslagningstabell byggd ur en lista över konsumenter i konfigurationen hade gett en klient som inte ser ut som de andra, för att spara ett arbete som
ändå kräver en release — se tabellen nedan.

`process_service` sätts fortfarande på outbox-raden vid publicering, ur namespacets `PROCESS_CONSUMER` (§2.2 steg
7). Relayet hämtar bara rader adresserade till pw-alkt, och **en oskickad rad adresserad någon annanstans slår om
hälsoindikatorn direkt** (§8.3), eftersom ingen körning någonsin kommer att ta den. En okänd konsument avvisas
redan vid skrivning (§7.2), så en sådan rad kan bara uppstå om någon skriver konfigurationen direkt i databasen.

#### Vad en ny pw-tjänst skulle kosta

|                                  Steg                                  |            Var             |      Release?      |
|------------------------------------------------------------------------|----------------------------|--------------------|
| `PROCESS_CONSUMER` och `PROCESS_TRIGGER` för namespacet                | `namespace_config`         | Nej                |
| `processKey` på etiketterna                                            | `metadata_label_attribute` | Nej                |
| OAuth2-registrering och provider                                       | `application.yml`          | **Ja**             |
| Feign-klient med konfiguration och inställningar                       | kod, `application.yml`     | **Ja**             |
| Relayet och valideringen av `PROCESS_CONSUMER` känner igen konsumenten | kod                        | **Ja**             |
| Eget API i WSO2                                                        | WSO2                       | **Ja**, utanför SM |

**En ny processkonsument är en driftsättning av SM, inte en konfigurationsändring** — och med AMQP som målbild är
det bytet som bör göras först. Det som är konfiguration är att *koppla ett namespace* till pw-alkt.

#### Att pw-alkt är nere får inte hålla körningen

Leveransen håller ett HTTP-anrop inne i transaktionen (§8.3), och jobbet har en `maximum-execution-time`. Tre saker
håller körningen kort när pw-alkt inte svarar:

1. **Hämtningen är begränsad.** `batch-size` (§7.2) är ett tak per körning. Det är också svaret på fallgropen i
   §1.2: `findProcessable` saknar `LIMIT`, och den bristen får inte ärvas hit.
2. **Circuit breaker.** `@CircuitBreaker` på klienten, som på tjänstens övriga klienter, men den räknar bara anrop
   som aldrig fick svar — timeout, anslutningsfel, en token som inte går att hämta, och en `401` som står sig efter
   token-retryerns omförsök — och öppnar när minst tre anrop räknats och hälften av de senaste fem saknat svar. Öppnar den avbryts körningen,
   eftersom varje ärende efter det skulle få samma svar. Ett felsvar om en enskild händelse, 5xx inräknat, betyder att
   pw-alkt finns där. Räknades det skulle några händelser som pw-alkt aldrig tar kunna hålla breakern öppen för alla
   ärenden, eftersom hämtningen börjar med samma rader varje körning. Varje timeout görs två gånger av den
   token-retryer som tjänstens alla klienter har, så tre timeouts kostar körningen en minut.
3. **Kort read-timeout** (§7.2). pw svarar `202` så snart händelsen tagits emot, så anropet är kort i alla normala
   fall — och ett långt anrop håller en databastransaktion öppen.

En pw-alkt som svarar långsamt men ändå svarar räknas inte av breakern. Då är det `batch-size` som begränsar
körningen, och en körning som drar över `maximum-execution-time` slår om hälsoindikatorn för just den körningen — tre
timeouts kostar redan en minut. Hinner en annan podd starta en körning när `shedlock-lock-at-most-for` passerats är det
ofarligt, eftersom leveransen låser raderna (§2.3).

Ett ärende vars leverans fallerar håller inte tillbaka de andra, inom den gräns `batch-size` sätter. Raderna
levereras grupperade per ärende med en transaktion per grupp, ett ärende som fallerar lämnas åt nästa körning, och
körningen hämtar vidare förbi det (§8.3). Varje sådant ärende räknas som ett försök, så en körning räcker till
`batch-size` ärenden som alla fallerar — först då får resten vänta till nästa minut.

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
  ('4f8b...', 'processKey',       'supervision'),
  ('4f8b...', 'processStartMode', 'MANUAL');
```

|    Värde    |                                  Betydelse                                   |
|-------------|------------------------------------------------------------------------------|
| `AUTOMATIC` | SM sätter startlovet på den första ärendehändelse som kan starta processen   |
| `MANUAL`    | Bara kommandot i §5.10 sätter lovet                                          |
| Saknas      | Som `AUTOMATIC`. Den som inte rör attributet märker ingen skillnad mot i dag |

**Tre kontroller vid skrivning av etiketten, alla `400`:** värdet måste vara exakt `AUTOMATIC` eller
`MANUAL`, `processStartMode` utan `processKey` på samma etikett avvisas eftersom attributet är
meningslöst ensamt, och en nyckel som stavas som `processKey` eller `processStartMode` på annat sätt —
andra versaler, blanksteg runt — avvisas (beslut 68). Utan den tredje går `processstartmode: MANUAL`
igenom de två första och läses som inget läge alls. Kontrollerna gäller `POST` och `PUT` av
`/metadata/labels`, de enda vägarna som skriver etikettattribut, och varje fel namnger etiketten med dess
sökväg av resursnamn.

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

Tre följder av det syns i koden. Har ärendet en instans vars nyckel etiketterna inte längre pekar ut ges inget
startlov alls, eftersom nyckeln då kommer från instansen och läget annars skulle komma från en etikett som inte har med
den att göra. Bär två etiketter samma nyckel men olika lägen vinner `MANUAL`, eftersom det är en uttalad önskan om att
ingenting ska starta av sig självt. Och ett oläsbart värde, som efter T12 bara kan komma in förbi API:t — direkt i
databasen, som i exemplet ovan, eller från före valideringen — läses som `MANUAL`, med en WARN-rad i loggen.

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
            || ( etikettens processStartMode == AUTOMATIC
              && etiketten pekar ut samma nyckel som raden bar
              && ingen levande instans for arendet
              && ingen COMPLETED instans for arendet          // 7.4 regel 4
              && ingen olevererad startrad med en annan nyckel )   // 7.4 regel 1
```

Processraderna kostar inga extra frågor. Steg 5 slår redan upp ärendets processrader för att kunna läsa
`process_key` från instansen i första hand, och samma rader svarar på båda instanskontrollerna. Läget kommer ur
den etikettuppslagning som ändå gjordes för att lösa ut nyckeln. Outboxen läses för det sista villkoret, och
bara när de andra redan håller — alltså bara för de händelser som annars hade fått lovet.

**En start på väg med en annan nyckel stänger lovet.** Byts etiketten från A till B innan startraden för A hunnit
levereras, hade raden för B annars också fått lov att starta, och det som kom fram först hade avgjort vilken
process ärendet kör för resten av sitt liv. Spärren i §7.4 avvisar dessutom själva etikettbytet, så villkoret
behövs bara för ändringar som når etiketterna förbi den, som en ändring i etikettens metadata. En start på väg med
**samma** nyckel stänger inte lovet: pw startar bara en instans per ärende (§9.3).

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

### 8.1 Fel som inte syns, och vad som gör dem synliga

Det finns ingen mätvärdesinsamling, och det är ett medvetet val. Utan larm är en räknare bara ett tal som
någon skulle behöva komma ihåg att titta på. Micrometer-räknare ligger dessutom i varje podds minne, så med
två poddar i klustret svarar `/actuator/metrics` med en slumpvis delsumma som kan sjunka mellan två anrop —
sämre än ingen siffra alls.

Kvar står den fråga som listan egentligen besvarade, och den är värd mer än namnen var: **vilka fel märks
inte av sig själva?** Ett fel som ger ett felsvar, en rad i outboxen eller en post i aktivitetsloggen hittar
den som letar. De nedan gör det inte — de yttrar sig i att ingenting händer.

|                                      Fel                                       |                                                 Varför det är tyst                                                  |                                                                           Så syns det ändå                                                                            |
|--------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Loop-skyddets lager 1 ur funktion — pw slutar sätta `X-Trigger-Process: false` | Skyddet verkar genom att *inte* skriva en rad, så att det upphört yttrar sig i extra rader som ser fullt normala ut | Gruppera outbox-rader på `executed_by` i ett tidsfönster. Processens egen identitet ska inte förekomma där; gör den det väcker processen sig själv (§6.5)             |
| Handläggare och process krockar (`412`)                                        | Rapporten avvisas innan något skrivs — varken tillstånd eller aktivitet, med flit (§6.3)                            | `ErrandProcessService` loggar en INFO-rad per avvisad rapport med ärende, läst version och aktuell. Räkna dem i loggverktyget när ett arbetssteg körs om gång på gång |
| Startknappen visas inte i ett namespace vars etiketter säger `MANUAL`          | Ingen trycker, alltså händer ingenting alls (§7.7)                                                                  | Ärenden vars etikett bär ett `processKey` men som saknar processrad, äldre än ett dygn                                                                                |
| Manuell grind som ingen klickar på                                             | Processen står kvar i `WAITING` för alltid (§9.2 punkt 2)                                                           | Processrader i `WAITING` med samma `current_activity_id` längre än fasen rimligen tar                                                                                 |
| Oskickad outbox-rad som aldrig går igenom                                      | Det finns ingen dead letter-flagga att räkna på (§8.3)                                                              | **Hälsoindikatorn**, som inte är ett mätvärde: den slår om när äldsta oskickade rad passerat `unhealthy-after`                                                        |
| Rad som släppts vid `max-age`, och publicering utan aktiv transaktion          | Anropet lyckas, men något gick ändå förlorat                                                                        | ERROR-loggar, båda två. Ska aldrig förekomma                                                                                                                          |

Logga alltid `eventId`, `errandId`, `processInstanceId` och `X-Request-Group-Id` — dubbelleveranser blir då
spårbara i efterhand. **Logga aldrig `justification`** — den innehåller personuppgifter (§11). Tjänstens egna
loggrader räcker inte för det: dept44 skriver hela request- och svarskroppen till loggern
`se.sundsvall.dept44.payload` som standard. Fältet maskas därför med ett filter i `logbook.body-filters`
(`$..justification`), och `ErrandDecisionProcessIT` kontrollerar att motiveringen inte syns i loggen men att
maskeringen gör det (beslut 57).

Behöver någon senare veta hur ofta något sker snarare än om det skett, är svaret oftast en fråga till
databasen: `decision` bär `method`, `decided_by` och `errand_process_id`, `process_event_outbox` bär `delivered_at`, och
aktivitetsloggen bär en post per instans där modellen bryter mot §6.4. Ett mätvärde blir motiverat den dag
något faktiskt larmar på det — och då är beroendet `micrometer-registry-prometheus` det som saknas, inte
räknarna.

### 8.2 Vanliga frågor i drift — och svaren

|                                  Fråga                                  |                                                                                                                                                                                                                                                                                                                       Svar                                                                                                                                                                                                                                                                                                                        |
|-------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Varför startade ingen process för ärendet?                              | Börja i `startable.status` från `GET .../processes` — den svarar direkt i de vanliga fallen (§5.10). Står den på `AVAILABLE` väntar ärendet bara på en knapptryckning, alltså `processStartMode: MANUAL` på etiketten (§7.7). `START_PENDING` betyder att startraden ännu inte levererats — se hälsoindikatorn och ärendets olevererade rader i outboxen. Annars: läs `GET .../process-activities`, där tvetydig etikett ligger som `CONFIG`-post och nödbromsen som `LOOP_GUARD`-post, och kontrollera etikettens `processKey`-attribut via `GET /{municipalityId}/{namespace}/metadata/labels` samt att `PROCESS_CONSUMER` finns för namespacet |
| Varför syns ingen knapp för att starta handläggningen?                  | `startable.status` säger vilket hinder det är. `PROCESS_COMPLETED` betyder att ärendets processliv är slut — nästa process är ett nytt ärende (§7.4)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| Varför går processen inte vidare fast handläggaren tryckt på knappen?   | Kontrollera att signalen står bland `errand.process.awaitingSignals` och att väntläget i modellen lyssnar på just det namnet. Signaler filtreras inte av `PROCESS_TRIGGER` (§7.7), så där finns ingenting att felkonfigurera. En avvisad signal svarar `409`                                                                                                                                                                                                                                                                                                                                                                                      |
| Varför syns ingen knapp för att gå vidare?                              | `errand.process.awaitingSignals` är tom. Antingen är väntläget automatiskt, eller så rapporterar pw inte in signalerna (§9.3)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| Varför avslutas inte processen fast beslutet är fattat?                 | Kontrollera att beslutet har `status = COMPLETED` på `GET .../decisions`, att outbox-raden med subtypen `DECISION` finns för skrivningen, och att väntläget läser om ärendet när det går in i väntan (§9.2 punkt 1). `DECISION` i `PROCESS_TRIGGER` kontrolleras när konfigurationen skrivs, men en konfiguration från före kontrollen kan sakna den (§7.1)                                                                                                                                                                                                                                                                                       |
| Vem fattade beslutet på ärendet?                                        | `method` och `decidedBy` på `GET .../decisions`. `AUTOMATIC` betyder att processen fattade det; `errandProcessId` pekar ut vilken processrad                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| Varför går beslutet inte att ändra?                                     | Ärendets process är `COMPLETED`, eller beslutet är det, och ärendet har en process (§7.5). En rättelse görs i ett nytt ärende, kopplat via `referredFrom`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| Varför går ärendet inte att radera?                                     | Ärendet bär ett beslut som inte längre får ändras, och `DELETE` svarar `409` för att beslutet inte ska följa med i kaskaden (§7.5). Gallringen tar ärendet när det är tillräckligt gammalt                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| Varför fick en schemalagd åtgärd eller en flyttad etikett ingen verkan? | Läs `GET .../process-activities`: en `CONFIG`-post med `LABELS_NAME_TWO_PROCESSES` eller `LABEL_MOVES_PROCESS_KEY` betyder att ändringen hade flyttat ärendet till en annan process, och att etiketterna lämnades som de var (§7.4)                                                                                                                                                                                                                                                                                                                                                                                                               |
| Varför kör processen om samma steg gång på gång?                        | Handläggaren ändrar ärendet mitt i steget ⇒ `412` (§6.2). Se INFO-raden per avvisad rapport (§8.1) och aktivitetsloggen                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| Varför väcks inte processen av inkommande e-post?                       | `MESSAGE` saknas i `PROCESS_TRIGGER`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| Varför väcks processen inte av sina egna ändringar?                     | Det är meningen — lager 1 i §6.5                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| Varför står instansen kvar som `RUNNING` fast inget händer?             | Workern kraschade utan att rapportera. Operaton kör om task:en när dess eget lås löper ut; instansen uppdateras vid nästa rapport                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |

### 8.3 Misslyckade leveranser

Mönstret bygger på det som `V1_48__simplify_notification_dispatch` införde: **leverans och kvittering delar
transaktion.** En rad som inte går igenom ligger kvar precis som den var, och nästa körning gör om försöket. Det
finns alltså ingen retry-räknare, ingen backoff och ingen dead letter-flagga — den oskickade raden *är*
kvitteringen på att arbetet återstår.

**Principen är att ingenting efter den misslyckade raden levereras — inte att en grupp levereras helt eller inte
alls.** Rader levereras grupperade per ärende, äldst först, med en transaktion per grupp. Den första raden som
inte går igenom avslutar gruppen: raderna före den är redan hos pw och kvitteras när transaktionen sparas, medan
den raden och ärendets senare ligger kvar. Felet förs vidare först när kvitteringen är sparad. Ordningen inom
ärendet hålls, eftersom inget som kom efter den misslyckade raden når pw före den, och det som redan tagits emot
skickas inte om — också signaler och startkommandon, som annars hade gått en gång i minuten i upp till 30 dagar.

Tre följder är värda att skriva ut:

- **Leveransen måste tåla att göras om.** Går transaktionen inte att spara efter att pw redan tagit emot
  händelsen kommer samma händelse en gång till. Detsamma gäller när en avvisning inte går att skriva i
  ärendets historik (`422`, nedan): hela gruppen rullas då tillbaka och görs om. pw:s event-endpoint måste därför vara idempotent — den
  korrelerar på ärendet och startar eller väcker, den räknar inte (§9.3).
- **En rad som fastnar håller sitt eget ärende, inte de andra.** Cronjobbet lämnar ett ärende som fallerat åt
  nästa körning och hämtar vidare med `...AndErrandIdNotIn`, så raderna som hämtas efter det tillhör andra
  ärenden. Ett ärende som fallerar räknas mot `batch-size` med de rader det försökt, den misslyckade inräknad
  och minst en, och körningen slutar när en hämtning ger färre rader än den bad om. Rader som aldrig går igenom kan därmed inte fylla körningen, så länge färre än
  `batch-size` ärenden fallerar samtidigt (§7.6).
- **En öppen circuit breaker avslutar körningen.** Varje ärende efter det hade fått samma svar, och resten tas
  vid nästa körning.

**De två felen behandlas olika:**

|                                                        Fel                                                        |                                                                                                                                                                                                                                                                                      Utfall                                                                                                                                                                                                                                                                                      |
|-------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `422` från pw — permanent, till exempel ett `processKey` som inte är driftsatt                                    | Inget nytt försök. Raden konsumeras (`delivered_at` sätts) och avvisningen loggas som ERROR. Ärendets levande instans, om den finns, sätts `FAILED`, och en ERROR-post (`DELIVERY`, `PROCESS_KEY_NOT_DEPLOYED`) skrivs en gång per ärende och fönster. För en `DELETE`, eller ett ärende som inte längre finns, är loggraden allt (§5.4). Posten råder bara ett ärende utan processrad att rätta etiketten; ett ärende med processrad kör processens nyckel och får rådet att driftsätta den. Ett permanent fel hör hemma i ärendets historik, inte i en flagga på en outbox-rad |
| Allt annat — `5xx`, `4xx` från WSO2, timeout, nätfel, en rad som inte går att göra till en händelse — tillfälligt | Gruppen avslutas vid raden: raderna före kvitteras, och raden och ärendets senare görs om vid nästa körning, hur många gånger som helst, tills den går igenom eller åldras ur                                                                                                                                                                                                                                                                                                                                                                                                    |

**`max-age` är sista utvägen, inte en väg ut.** Som i `NotificationDispatchWorker` släpps en rad som
passerat åldersgränsen oskickad. Det gör cronjobbet i början av varje körning; direktkörningen lämnar sådana rader
åt det. Skillnaden mot en notis är att det här betyder att en process aldrig fick
veta något, så gränsen sätts högt (30 dagar) och varje sådan rad **loggas som ERROR**. Det ska aldrig
hända.

**Hälsan mäts i ålder, inte i antal.** Det finns alltid oskickade rader — varje publicering lägger en, och
den ligger kvar tills nästa körning tar den. Villkoret är därför att den **äldsta** oskickade raden
passerat `unhealthy-after` (§7.2, `PT15M` som standard), inte att det över huvud taget finns oskickade
rader. Sätts villkoret på existens i
stället står tjänsten unhealthy under normal drift, och då slutar någon titta på indikatorn — vilket är
värre än att inte ha den.

Någon redrive-endpoint behövs inte längre: en rad som inte gått igenom försöker redan igen av sig själv.
Det som behövs är att någon märker att den ligger kvar, och det är vad hälsoindikatorn är till för.

### 8.4 Prova själv, lokalt

1. Skapa namespace-config med `PROCESS_CONSUMER=pw-alkt` och `PROCESS_TRIGGER=ERRAND,MESSAGE,DECISION` — utan `ERRAND` eller `DECISION` svarar SM `400`. Registrera ett beslutsutfall på `/metadata/decisionoutcomes`.
2. Tagga en label med `processKey=alcohol-serving`.
3. `POST /2281/ALKT/errands` med den labeln ⇒ rad i `process_event_outbox` inom en sekund, `delivered_at` satt när stubben svarat.
4. `GET /2281/ALKT/errands/{id}` ⇒ `process.processStatus = RUNNING`, och `ETag` i svarshuvudet.
5. `PATCH` samma ärende med den ETag:en ⇒ `200`. `PATCH` igen med **samma** ETag ⇒ `412`.
6. Låt stubben rapportera med ett `errandVersion` som ligger efter ⇒ `412` på rapporten, inget tillstånd skrivet.
7. `POST /2281/ALKT/errands/{id}/decisions` med `method: MANUAL` ⇒ en `DECISION`-händelse, en outbox-rad och ett ärende vars `ETag` flyttat sig (kräver `DECISION` i `PROCESS_TRIGGER`). Samma skrivning med `X-Sent-By: pw-alkt; type=processEngine`, `X-Trigger-Process: false` och `method: AUTOMATIC` ⇒ ingen outbox-rad, men beslutet skrivet och `errandProcessId` satt.
8. `PATCH` beslutet till `status: COMPLETED` ⇒ en outbox-rad, som skulle ha passerat nödbromsen även om den slagit till för ärendet. `PATCH` det igen ⇒ `409`.
9. Låt stubben rapportera `COMPLETED` och skapa ett nytt beslut på ärendet ⇒ `409`.

## 9. pw-alkt

### 9.1 En BPMN-fil per process

Alltså `alkt-ansokan.bpmn` och `alkt-tillsyn.bpmn` var för sig. (Skrivet när pw-alkt hade två processer. I dag har den tio, en fil per process, med nycklarna i `Constants.PROCESS_KEYS`.) `TenantAwareAutoDeployment.deployResources` rullar ut en driftsättning per fil, och låg båda processerna i samma fil skulle en ändring i tillsynsprocessen versionera upp ansökningsprocessen och rycka undan mattan för de instanser som redan kör.

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
`POST .../errands/{errandId}/decisions` med `method: AUTOMATIC`, och `X-Sent-By` med samma värde som
namespacets `PROCESS_CONSUMER` — alltså inte i rapporten och inte som en processvariabel. Rapporten handlar
om processens tillstånd, medan beslutet är ärendedata med egen livslängd, egen behörighet och egen
händelsehistorik (§7.5). `errandProcessId` fyller SM i själv, så steget behöver inte veta något om sin egen
rad där. Kommer beslutet i stället från en handläggare gör steget ingenting alls — processen väcks av
`DECISION`-händelsen och läser beslutet på `GET .../decisions`. Ett `409` på beslutet är permanent: beslutet
är låst, och steget ska inte köras om för det.

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

| Steg |    Jira     |                   Uppgift                   |
|------|-------------|---------------------------------------------|
| 1    | DRAKEN-4734 | T1 — Datamodell och domänenums              |
| 2    | DRAKEN-4735 | T2 — Konfigurationsläsning                  |
| 3    | DRAKEN-4736 | T3 — Process-API                            |
| 4    | DRAKEN-4737 | T4 — Optimistisk samtidighetskontroll       |
| 5    | DRAKEN-4738 | T5 — Publicering                            |
| 6    | DRAKEN-4739 | T6 — Relay och leverans                     |
| 7    | DRAKEN-4740 | T7 — Skyddsräcken                           |
| 8    | DRAKEN-4741 | T9 — Beslutet: händelse, lås och spårbarhet |
| 9    | DRAKEN-4749 | T11 — Manuell stegning med signaler         |
| 10   | DRAKEN-4811 | T12 — Automatisk och manuell start          |
| 11   | DRAKEN-4742 | P1 — Operaton-klienten                      |
| 12   | DRAKEN-4743 | P2 — Event-endpoint och borttagning         |
| 13   | DRAKEN-4744 | P3 — SM-klienten                            |
| 14   | DRAKEN-4745 | P4 — Workerstruktur                         |
| 15   | DRAKEN-4750 | P7 — Manuella grindar och väntade signaler  |
| 16   | DRAKEN-4746 | T8 — `ProcessLoopGuardIT`                   |
| 17   | DRAKEN-4747 | P5 — Tillsynsprocessen                      |
| 18   | DRAKEN-4748 | P6 — Incidentåterkoppling                   |

### T1 — Datamodell och domänenums (SM)

**Bygg:** `V1_60`-migrering (§3.1); `ProcessStatus` med `isTerminal()` (§4.1); `ActivitySeverity`; entiteterna `ProcessEventOutboxEntity`, `ErrandProcessEntity` (§4.3), `ErrandProcessActivityEntity`; repositories med `Pageable` på de sökfrågor som kan växa utan tak; tabellerna i `truncate.sql`.

**Acceptans:**
- Ingen av T1:s entiteter är mappad som relation på `ErrandEntity`.
- `ErrandProcessEntity.applyStatus` är enda vägen att sätta status; settern är inte publik.
- Tabelldrivet test räknar upp **varje** `ProcessStatus` mot `isTerminal()`, med `WAITING` explicit verifierad som *icke*-terminal.
- `applyStatus` med icke-terminal status nollar `ended`.
- Aktivitet **utan** processinstans går att spara, och kaskaderas bort när ärendet raderas (`fk_epa_errand`).
- Någon IT startar grönt ⇒ `schema-generation: validate` bekräftar DDL mot entiteter.

### T2 — Konfigurationsläsning (SM)

**Bygg:** `PROPERTY_PROCESS_CONSUMER`, `PROPERTY_PROCESS_TRIGGER`, `getValues(...)` (§7.1); `@ConfigurationProperties` för `process-engine.*`; validering av `PROCESS_CONSUMER` mot relayets klientnamn vid skrivning.

**Acceptans:**
- `getValues` returnerar **alla** rader för en nyckel (regression mot `.findFirst()`).
- Skrivning av okänd `PROCESS_CONSUMER` ger `400`. Den enda kända är `pw-alkt`, relayets klientnamn (§7.2, beslut 41), och namnet är leveransadressen — ingen separat `identifier`-egenskap som kan drifta från sin nyckel.
- Verifierat att `namespaceConfigCache` evikteras vid skrivning — annars går konfigurationen inte att ändra i drift, hur mycket den än ser ut att göra det.
- Evikteringen gäller den podd som tog emot skrivningen. De andra podderna läser den gamla konfigurationen tills cachen gått ut, högst tio minuter (§7.1). Det är dokumenterat, inte åtgärdat.

### T3 — Process-API (SM)

**Bygg:** `ErrandProcessResource` (`PUT`, `POST`, `GET` under `.../errands/{errandId}/processes`) och ärendescopad `GET .../process-activities` med valfritt `processInstanceId`-filter (§5.2); `ErrandProcessService`; API-modellerna (§5.3); `Errand.process` + batchberikning i `readErrand`/`findErrands`; regenerera `openapi.yaml`. Fältet `awaitingSignals` på samma modell hör till T11 — bygg det inte här. `GET .../processes` ska däremot svara med kuvertet `ErrandProcessOverview` (§5.10) redan här; fältet `startable` fylls i T12.

**Acceptans:**
- `PUT` två gånger ⇒ `revision`-tabellen oförändrad (skyddar mot framtida `@OneToMany` på `ErrandEntity`).
- Aktiviteter idempotenta på `(processInstanceId, externalTaskId, activityId)`; batch > 100 ⇒ `400`.
- `GET .../process-activities` returnerar även poster utan processinstans; filtret `processInstanceId` utesluter dem.
- **Kapplöpningen i §5.1 körd i båda ordningarna:** `PUT` först (skapar raden) följt av `POST` med samma `processInstanceId` ⇒ `200` och orört tillstånd; `POST` först följt av arbetsstegets `PUT` ⇒ tillståndet uppdateras. Ingen av ordningarna ger `409`.
- Annan levande instans med **annat** `processInstanceId` ⇒ `409`; instans med annat `process_key` ⇒ `409`; constraint-violation översatt enligt §7.4, aldrig `500`.
- `POST` mot ärende som redan har en `COMPLETED` instans ⇒ `409`. `POST` mot ärende som bara har en `FAILED` instans ⇒ `201`.
- `errand.process` projicerar den **levande** instansen när ärendet har en, annars den **senaste**: ärende med misslyckad start visar `FAILED` med sitt felmeddelande, inte `null`, och en nyare `FAILED` bredvid en levande instans skymmer inte den levande.
- Rapport om något annat än `COMPLETED` på en `COMPLETED` instans ⇒ `200`, instansen oförändrad och `ended` kvar, aktiviteterna lagrade. `COMPLETED` för en ny instans medan en annan lever ⇒ `409`.
- `processInstanceId` som är tom eller har blanksteg, `activities: [null]` och sortering av loggen på ett okänt fält ⇒ `400`, aldrig `500`.
- Namespace utan `PROCESS_CONSUMER` ⇒ berikningen gör ingen fråga alls.
- Batchberikningen är "senaste per ärende" och fortfarande **en** fråga — verifieras med query-räkning.
- `GET .../processes` sorterar nyast först.
- `findErrands` gör **en** fråga för berikningen (verifieras med query-räkning, inte ögonmått).
- `ErrandProcess` är samma modell för subresursen och `errand.process`. Rapporten har en egen modell, `ErrandProcessReport`, så `externalTaskId`, `errandVersion` och `activities` finns inte i lässchemat (§5.3, beslut 65).
- `PUT` med ett `processInstanceId` i kroppen som skiljer sig från pathens ⇒ `400`.
- Notis som skapas av en processkrivning har en avsändare i `createdBy`, inte tom sträng: `EventService.createNotification` faller tillbaka på identitetens värde när `getAdUser()` är null (§1.8).
- `PROCESS` tillagt i `ErrandField` och filtrerat av `roleBasedFieldResolver` (§5.3). Test för båda riktningarna: namespace **utan** åtkomstkontroll ⇒ fältet syns; begränsad användare i ett namespace **med** åtkomstkontroll som inte räknat upp `PROCESS` ⇒ fältet utelämnas.
- Beslut fattat och dokumenterat om `decision` ska vara reducerad i listsvar (§5.3). Utgick 2026-09-15, eftersom ärendet inte bär beslutet (beslut 51).

### T4 — Optimistisk samtidighetskontroll (SM)

**Bygg:** `errandVersion` i rapportmodellen och kontrollen mot `errand.version` i `ErrandProcessService` (§6.3); kolumnen `outstanding_external_task_id` i T1:s `V1_60` (§3.1) med WARN-aktivitet när ett annat `externalTaskId` rapporterar `RUNNING` medan ett steg fortfarande står där (§6.4); INFO-raden som är enda spåret av ett `412` (§8.1).

**Acceptans:**
- Rapport med `errandVersion` som glidit ⇒ `412`, och **varken** tillstånd eller aktiviteter skrivs.
- Rapport utan `errandVersion` ⇒ ingen kontroll, `200`.
- Två skilda `externalTaskId` med `RUNNING` mot samma instans ⇒ WARN-aktivitet skriven, **båda** rapporterna tas emot.
- `ErrandProcessService` tar en injicerad `Clock`. **Inget test använder `Thread.sleep`.**
- Ett test bekräftar att `PATCH /errands/{id}` med föråldrad `If-Match` ger `412` — regressionsskydd för att hela samtidighetsmodellen vilar på befintligt beteende.

### T5 — Publicering (SM)

**Bygg:** `ProcessEventPublisher` anropad från `EventService.createErrandEvent`, med `setRollbackOnly` före kast (§2.2); `TriggerProcessFilter` med ThreadLocal i `ServiceUtil` och headern dokumenterad i `OpenApiConfig`, efter mönstret från `X-Request-Group-Id` (§6.5); `ProcessKeySelector` (§7.3); nödbromsen; uträkningen av `startAllowed` och kolumnen `start_allowed` (§7.7); kommandonas undantag från triggerfiltret och nödbromsen samt `signal_name` på raden (§6.5); värdena `PROCESS` och `SIGNAL` i `EventSubType`; regenerera `openapi.yaml`.

**Acceptans:**
- **IT som verifierar att ett e-postintag ger en outbox-rad.** Intaget skapar inga revisioner och går förbi den gemensamma passagen (§1.1) — tappas det där märks det inte av något annat test.
- Enhetstest per gren i §2.2, inklusive: `X-Trigger-Process: false` ⇒ ingen rad; icke-triggad subtyp ⇒ ingen rad; namespace utan konsument ⇒ ingen rad.
- **Utan header ⇒ raden skrivs**, och **headern satt av ett AD-konto ⇒ raden skrivs ändå** (§6.5). Filtret får inte vara bredare än sitt syfte.
- Tabelldrivet test över headervärdena: bara exakt `false`, versaloberoende och trimmad, tystar raden. `true`, tom sträng och skräp gör det inte.
- Skrivning helt utan request-kontext — ett schemalagt jobb — ⇒ raden skrivs.
- **`DELETE` av ett ärende vars etikett tagits bort ⇒ raden publiceras ändå**, med `process_key` null. Utan det blir processinstansen föräldralös i Operaton. Gallringen publicerar på samma sätt en `DELETE` per borttaget ärende, utan händelse i eventloggen.
- Triggerfiltret frågas före nödbromsen: en händelse som inte står i `PROCESS_TRIGGER` räknar inte bromsen och ger ingen `LOOP_GUARD`-post.
- Flera rader för samma ärende i en transaktion ger **en** signal till direktkörningen.
- `startAllowed` blir falskt när en olevererad startrad med en annan nyckel ligger för ärendet (§7.7).
- Ärende med processinstans: `process_key` i raden kommer från instansen, inte från etiketterna. Verifieras genom att ändra etiketten i testdata och se att nyckeln står still.
- `ProcessKeySelectorTest`: en tagg ⇒ en nyckel; två med samma ⇒ en; två med olika ⇒ ERROR-aktivitet och ingen rad; `deprecated` ignoreras; **namnbyte och omflyttning av labeln lämnar upplösningen oförändrad**.
- Selektorn lämnar nyckel **och** startläge som ett par, ur samma etikett (§7.7).
- `startAllowed` blir falskt för ett ärende med avslutad instans och sant för ett vars enda instans är misslyckad. Verifierat på raden i databasen.
- **Publisher kastar ⇒ ärendeskrivningen är inte committad**, trots att anropsstället sväljer undantaget (§1.7). Verifieras genom att PATCH:a och sedan läsa tillbaka ärendet — inte genom att inspektera loggen.
- Utan aktiv transaktion: ERROR-logg, inget kast som spräcker anropet.
- Nödbromsen slår till när tröskeln nås med rader som har `delivered_at` satt, och dess ERROR-aktivitet skrivs **utan** instans.
- `ProcessKeySelector` med två skilda nycklar skriver ERROR-aktivitet **utan** instans — testet får inte förutsätta att en instansrad finns — och **en gång per ärende och fönster**: tio händelser på ett tvetydigt ärende ger en post, inte tio (§2.2).
- Kommandon (subtyp `PROCESS`, `SIGNAL`) publiceras även när nödbromsen slagit till för ärendet och även när `PROCESS_TRIGGER` är tom (§6.5).
- Kommandon publiceras även med `X-Trigger-Process: false` från en maskinidentitet. Undantaget från lager 1 är uttryckligt, så att knappen inte hänger på `403`-kontrollen (§6.5).
- Etiketter som inte lästs från databasen, alltså på ett ärende som skapas eller i en `PATCH` som sätter etiketterna, slås upp på id. Annars tappas nyckeln just när ärendet får sin processetikett (§7.3, tillagt när T8 hittade felet).

### T6 — Relay och leverans (SM)

**Bygg:** paketet `service/scheduler/processevent/` med schemaläggare, jobb och relay efter mönstret i `service/scheduler/notificationdispatch/` — leverans och kvittering i samma transaktion, ingen retry-bokföring; direktkörningen efter commit tillsammans med en trådpool med tak; `PwAlktClient` med ett lager som översätter felen (`PwAlktIntegration`); `422` som permanent fel och `5xx` som tillfälligt; `max-age`, röjningen av levererade rader och av aktivitetsloggen (§3.2) och hälsoindikatorn (§8.3).

**Acceptans:**
- WireMock svarar `202` / `422` / `503` / timeout — samtliga fyra vägar verifierade, inklusive att `422` **inte** görs om, utan konsumerar raden och skriver `FAILED` + ERROR-aktivitet.
- **`503` ⇒ raden ligger kvar orörd** och nästa körning levererar den. Verifieras genom att läsa raden ur databasen, inte genom loggen.
- Rad som passerat `max-age` släpps oskickad och loggas som ERROR.
- Röjningen tar levererade rader på `delivered_at` (§4) och lämnar **oskickade** rader i fred.
- Röjningen tar aktivitetsposter äldre än `activity-retention` på `created`.
- **Hälsoindikatorn är grön direkt efter en publicering** och slår om först när äldsta oskickade rad passerat `unhealthy-after` (§8.3). Ett test som bara skriver en rad och läser indikatorn får inte se unhealthy.
- **En oskickad rad adresserad till annat än pw-alkt slår om hälsoindikatorn direkt** (§7.6). Ingen körning tar den, och utan det ligger den kvar osynlig.
- `batch-size` respekteras per körning; hämtningen har ett `LIMIT` (§1.2).
- Ordning per ärende hålls när flera rader finns. En rad som fallerar avslutar gruppen: raderna före kvitteras, och bara den och de senare ges igen vid nästa körning (§8.3).
- Ett ärende som fallerar hindrar inte körningen från att leverera andra ärendens rader, också när dess rader fyller en hel sida: körningen hämtar vidare förbi det.
- Full trådpool ⇒ direktkörningen hoppas över och cronjobbet levererar i stället. **Inget undantag når anroparen** — testet ska fylla kön och kontrollera att ärendeskrivningen ändå svarar `200` (§2.3).
- Samma händelse levererad två gånger, efter en återrullad transaktion, ger inte två processinstanser — idempotensen ligger hos pw (§8.3, §9.3).

### T7 — Skyddsräcken (SM)

**Bygg:** `ProcessKeyGuard` med §7.4 regel 1 och 5, anropad från alla fyra vägar som skriver etiketter: `ErrandService.createErrand` (`400`, bara regel 5), `ErrandService.updateErrand` (`400`), `AddLabelAction.executeAction`, som körs schemalagt och aldrig passerar API:t (etiketten läggs inte till, ERROR-post via `ProcessActivityLog`), och `ErrandService.persistLabelUpdate`, som `LabelMoveWorker` använder när en etikett flyttats (ärendet behåller sina etiketter, ERROR-post). Ingen ny action — `AddLabelAction` får bara ett anrop. `ProcessKeySelector.select` löser ut nyckeln ur ärendets etiketter och slår upp dem som saknar sin metadataetikett på id, eftersom en etikett som mappern just byggt inte har någon.

**Acceptans:**
- `400`-fallet täckt av enhetstest och ett IT-fall — både för en levande och för en **avslutad** process, mot riktiga rader i databasen.
- `AddLabelAction` som skulle byta upplöst `processKey` på ett ärende med levande instans ⇒ etiketten läggs inte till, ERROR-aktivitet skrivs. Utan detta slutar processen tyst få väckningar (§11).
- Ett ärende utan processrad påverkas inte av regel 1.
- En etikettändring som inte rör `processKey` går igenom som vanligt, även på ett ärende med levande process.
- Att byta `processStartMode` är alltid tillåtet, även på ett ärende med process (§7.7).
- Etiketter som pekar ut två nycklar ⇒ `400`, även på ett ärende utan process och även när ärendet skapas.
- En ändring som pekar tillbaka på den process ärendet kör går igenom, även om etiketterna inte pekade ut någon nyckel före ändringen.
- Kontrollen görs mot etiketterna **efter** att förfäderna lagts till, så en förälder som bär `processKey` räknas.
- Etiketter som redan bär sin metadataetikett slås inte upp igen, och en ändring som inte rör etiketterna läser ingenting alls.
- Att ta bort alla nycklar från ett ärende med process vars etiketter pekade ut två ⇒ avvisas: regel 1 jämför mängden nycklar.
- Ett ärende utan processrad men med en olevererad startrad hålls till startradens nyckel.
- En etikettflytt som skulle byta ärendets process lämnar ärendet orört och skriver en ERROR-post; en flytt som går igenom ger revision och ärendehändelse.

### T8 — `ProcessLoopGuardIT` (SM)

**Det viktigaste enskilda testet.** Kör hela varvet med WireMock i pw-alkts ställe och direktkörningen påslagen, och väntar på tillstånd, aldrig på tid.

**Bygg:** `ProcessLoopGuardIT` med testdata i `testdata-process-loop-guard.sql`: `PROCESS-NAMESPACE` får triggern `ERRAND` och ingen annan, och en etikett med `processKey` men utan startläge. Varvet i åtta steg:

1. Handläggaren skapar ärendet med etiketten ⇒ en outbox-rad, `CREATE`, `start_allowed = 1`.
2. Stubben registrerar starten och arbetssteget rapporterar `RUNNING` ⇒ en processrad, och `errand.process` visar `RUNNING`.
3. Stubben PATCH:ar ärendet med `X-Trigger-Process: false` ⇒ ingen ny rad (lager 1), och processens identitet finns inte bland `executed_by`.
4. Arbetssteget rapporterar `WAITING` ⇒ instansen lever, `active_marker = TRUE`.
5. Samma PATCH, headern kvar, men med handläggarens identitet ⇒ en rad.
6. Stubben glömmer headern och PATCH:ar på nytt för varje händelse den får ⇒ loopen stannar när nödbromsen släppt igenom `max-events-per-errand` levererade händelser, handläggarens inräknade. Varven är begränsade, så en broms som aldrig slår till fäller testet i stället för att hänga det.
7. Med bromsen utlöst ger även handläggarens ändring ingen rad, felposten `LOOP_GUARD`/`EVENT_RATE_EXCEEDED` finns en gång, och relayets hälsoindikator är `UP`, både direkt och efter en schemalagd körning. Det är kontrollen före körningen som räknas, eftersom den schemalagda körningen själv återställer indikatorn.
8. Handläggaren skickar en signal till den väntande processen, och startar den sedan igen när den rapporterats `FAILED` ⇒ en rad var, med subtyp `SIGNAL` respektive `PROCESS`, trots bromsen och trots att triggern inte nämner dem.

Varje PATCH skriver ett nytt värde och kontrolleras mot eventloggen. En PATCH som inte ändrar något ger normalt varken revision eller händelse, och "ingen rad" hade då inte sagt något om loop-skyddet. Kommandona går genom `EventService.createProcessCommandEvent`, eftersom endpointerna i T11 och T12 inte är byggda. När de är det ska steg 8 gå över tråden, och steget kräver redan i dag en process som inte lever, precis som knappen gör.

Två fall till: ett kommando från en maskinidentitet med `X-Trigger-Process: false` publiceras, medan en vanlig ändring i samma sammanhang tystas. Kommandona undantas alltså från lager 1 av egen kraft, inte bara genom `403`. Dessutom lämnar en `X-Sent-By` som inte går att tolka lager 1 verksamt (§1.8).

**Acceptans:**
- **Kontrollera att pw:s egen PATCH inte gav någon ny outbox-rad** (lager 1).
- Kör sedan **samma** PATCH med en handläggaridentitet — **med headern kvar** — och kontrollera att den **ger** en rad. Filtret får inte vara så brett att äkta ändringar tystas, och AD-undantaget i §6.5 är det som håller emot. Det felet är osynligt i drift tills någon undrar varför processen aldrig vaknar.
- Kör pw:s PATCH **utan** headern och verifiera att lager 2 eller 3 fångar den.
- Med nödbromsen utlöst för ärendet ger en vanlig ärendeändring ingen rad, men ett startkommando ger en. Det är skillnaden mellan ett loop-skydd och en spärr mot handläggaren.
- Samma sak för en signal: den publiceras även när `PROCESS_TRIGGER` inte innehåller `SIGNAL`.
- Testet använder inte `Thread.sleep`.

**Testet hittade ett fel i publiceringen.** Etiketter som inte lästs från databasen saknar sin metadataetikett, så ett ärende som skapades med processetiketten publicerades aldrig. Felet rättades i `ProcessKeySelector` (§7.3, beslut 47). `ProcessEventPublisherDatabaseTest` täcker samma fel för en skrivning som ger ett befintligt ärende etiketten. En granskning efteråt hittade fyra fel till av samma slag, som också är rättade: ett `PATCH`-svar utan etiketternas metadata, en schemalagd `ADD_LABEL` som inte gav någon händelse, skenrevisioner som väckte processen, och etiketter som kunde hämtas från andra namespace (beslut 47–50).

Utan detta test är loop-skyddet en hypotes.

### T9 — Beslutet: händelse, lås och spårbarhet (SM)

Modellen, tabellen och resursen finns redan i mains handläggningsmodell (beslut 51). Det som återstår är
kedjan som gör att processen vaknar när beslutet är fattat, och reglerna runt den (beslut 52–57).

**Bygg:** en händelse med subtypen `DECISION` och höjd `errand.version` när ett beslut skapas, ändras eller raderas (`EventService.createDecisionEvent`); `method`-regeln mot namespacets `PROCESS_CONSUMER`; `errandProcessId` satt av SM; låsen i `DecisionValidator.validateChangeable` på alla skrivvägar utom JSON-parametrarna, och spärrarna i `ErrandAttachmentService.deleteErrandAttachment`, `ErrandInvestigationService.deleteErrandInvestigation` och `ErrandService.deleteErrand`; `ProcessRules.hasCompletedProcess` delad med processtjänsterna; undantaget från nödbromsen för ett beslut som en handläggare gör `COMPLETED` i `ProcessEventPublisher`; kontrollen av `PROCESS_TRIGGER` i `NamespaceConfigService`; `$..justification` i `logbook.body-filters`; `403` och `409` i `openapi.yaml`; README.

**Acceptans:**
- Att skapa, ändra och radera ett beslut ger en eventloggpost med subtypen `DECISION`, utan revision. Villkor, bilagelänkar och JSON-parametrar ger ingen.
- Skrivningen höjer `errand.version`, i alla namespace; ett arbetssteg med äldre ETag får `412`.
- **`method: AUTOMATIC` från någon annan än namespacets `PROCESS_CONSUMER` ⇒ `403`. I ett namespace utan processkonsument godtas `AUTOMATIC` från varje anropare som inte är ett AD-konto (beslut 71). `method: MANUAL` från en identitet som inte är ett AD-konto ⇒ `403`.** Båda riktningarna testade — det är den skillnaden som ska hålla i efterhand (§7.5).
- `errandProcessId` sätts av tjänsten, inte av kroppen: en klient som skickar det får det ignorerat, och vid `AUTOMATIC` pekar det på ärendets levande processrad.
- Ärende med levande process: beslutet går att skriva om tills det är `COMPLETED`, därefter `409` — också för att backa statusen, för `DELETE`, för villkoren och för bilagelänkarna. Ärende med `COMPLETED` process: `409` på alla skrivvägar, nytt beslut inräknat. JSON-parametrarna låses aldrig.
- Ärende **utan** process: beslutet går att skriva, ändra och radera, och `409`-spärren slår aldrig till.
- Att radera en ärendebilaga som ett låst beslut länkar, eller en utredning som ett låst beslut vilar på ⇒ `409`.
- `DELETE /errands/{errandId}` på ett ärende med ett låst beslut ⇒ `409`, och ärendet och beslutet står kvar. Gallringen tar ärendet ändå.
- Två skrivningar med samma `If-Match` ⇒ den andra får `412`.
- Radering av ärendet, där den tillåts, tar beslutet med sig; att processraden försvinner gör det inte.
- **IT: handläggaren skriver beslutet ⇒ outbox-rad med subtyp `DECISION`.** Det är hela kedjan som gör att processen kan avslutas (`ErrandDecisionProcessIT`).
- En beslutsskrivning från processen med `X-Trigger-Process: false` ger **ingen** outbox-rad — lager 1 gäller även här.
- Skrivningen där en handläggare gör beslutet `COMPLETED` ger en outbox-rad även när nödbromsen slagit till för ärendet; en vanlig beslutsändring ger ingen, och inte heller ett beslut som processen själv skapar färdigt.
- `PROCESS_CONSUMER` utan `ERRAND` eller `DECISION` bland triggerna ⇒ `400`; `PROCESS` eller `SIGNAL` bland triggerna ⇒ `400`.
- `justification` förekommer inte i någon loggrad, men den maskerade platshållaren gör det (§8.1).
- Läs- och skrivvägarna anropar åtkomstkontrollen med `ProtectedResource.DECISION`: läsvägarna på `LR`, skrivvägarna på `RW`.

**Utgår jämfört med den ursprungliga planen:** `errand_decision` och dess migrering, `@OneToOne` på `ErrandEntity`, enumet `DecisionOutcome`, `ErrandDecisionResource` i singular, `Errand.decision`, `ErrandField.DECISION`, reduceringen i listsvar (beslut 39), revision av beslutet, `attachmentId` på beslutet och mätvärdena `decision.written` och `decision.rejected` (§8.1).

### T11 — Manuell stegning med signaler (SM)

Byggd på `sm-a10` (2026-09-21).

**Bygg:** `errand_process_signal` i `V1_60` (§3.1, beslut 58), `ErrandProcessSignalEntity` och `ErrandProcessSignalRepository`; `ProcessSignal` och `ProcessSignalRequest` i API:et och `awaitingSignals` på `ErrandProcess` (§5.3); ersättningen av signalerna i båda rapportvägarna och berikningen i `findLatestProcesses` och `readProcesses`; `POST .../processes/{processInstanceId}/signals` i `ProcessCommandService` (§5.9, beslut 62); aktivitetspost med `activityType = SIGNAL`; signalnamnet i händelsemodellen — värdet `SIGNAL` i `EventSubType` och kolumnen `signal_name` fylls redan i T5; regenerera `openapi.yaml`; tabellen i `truncate.sql` och `schema.sql`; README.

**Acceptans:**
- **Outbox-raden bär signalens namn i `signal_name`, och det följer med ut i `signalName` på händelsen.** Ett test som bara kontrollerar att en rad skrevs missar poängen — det är namnet pw korrelerar på (§5.4).
- En rapport med `awaitingSignals` ersätter tidigare rader helt — tas en signal bort ur rapporten försvinner den ur `errand.process`.
- Tomt `awaitingSignals` tömmer listan, och betyder att processen inte väntar på någon människa.
- Signal som står bland de väntade ⇒ `202`, aktivitetspost och outbox-rad med subtyp `SIGNAL`.
- Signal som **inte** står bland de väntade ⇒ `409`, och ingenting skrivs. Före processens nästa rapport godtas samma signal igen (beslut 64).
- Signal mot ärende utan levande process ⇒ `404`; mot avslutad process ⇒ `409`.
- Aktivitetsposten namnger avsändaren, så att "vem stegade processen förbi granskningen" går att besvara i efterhand.
- **Signalen publiceras även när `PROCESS_TRIGGER` är tom** för namespacet — kommandon filtreras inte (§7.7). Täck det med ett test, annars kryper filtret tillbaka nästa gång någon förenklar publiceraren.
- Signal från en icke-AD-identitet ⇒ `403`, och ingenting skrivs (§5.10).
- Berikningen av `awaitingSignals` gör **en** extra fråga för hela sidan, verifierat med frågeräkning.
- Signalen publiceras även när nödbromsen har slagit till för ärendet (§6.5).
- Signalen ger ingen notis, varken till ärendets handläggare eller till prenumeranterna (beslut 59).
- Namnet matchas exakt: samma namn med andra versaler ⇒ `409`. Kolumnen jämför också exakt, så namn som skiljer i versaler, accenter eller ett avslutande blanksteg lagras som olika signaler (beslut 60).
- En avslutad process visar en tom lista, vad dess senaste rapport än skickade och även när reläet avslutat den (beslut 61).
- Ärendet låses före alla andra läsningar i signalvägen, så att signalen bedöms mot den senaste rapporten.
- Signal i ett namespace utan `PROCESS_CONSUMER` ⇒ `400` (beslut 63).

Testerna: `ProcessSignalIT` kör varvet över tråden — rapport, ärendet, signalen, raden och händelsen pw-alkt tar emot — mot ett namespace utan triggers, och med nödbromsen utlöst. `ErrandProcessPersistenceTest` räknar frågorna och visar att en kvarstående signal behåller sin rad, och `ProcessIntegrationDataModelTest` att kolumnen jämför namn exakt, som tjänsten gör.

### T12 — Automatisk och manuell start (SM)

**Bygg:** `startAllowed` i händelsemodellen (§5.4) — kolumnen skapas i T1:s `V1_60` och fylls redan i T5; attributet `processStartMode` med validering vid etikettskrivning (§7.7); `startable` i kuvertet runt `GET .../processes` (§5.10); `POST .../processes/start` med `ProcessStartRequest`; aktivitetspost med `activityType = START`; regenerera `openapi.yaml`.

`ProcessKeySelector` med paret nyckel och läge, uträkningen av startlovet i publiceringens steg 6 och kommandonas undantag från triggerfiltret och nödbromsen byggdes i T5. Kvar här är reglerna runt attributet och vägen in för handläggaren.

Byggd på `sm-a11` (2026-09-21). Kontrollerna vid etikettskrivning ligger i `@ValidProcessLabelAttributes` på `POST` och `PUT` av `/metadata/labels`, med en tredje regel om felstavade nycklar (beslut 68). Reglerna för startbarheten finns på ett ställe, `ProcessRules.startOptionsOf`, som både `startable` och kommandot läser, och en start med en annan process än den ärendet redan kört erbjuds aldrig (beslut 67). `startable` läser dem genom `ProcessRules.startableOf`, som svarar `START_PENDING` när en start redan är på väg, och en nyckel längre än 128 tecken erbjuds aldrig. Dubbelklicksskyddet räknar varje olevererad rad med `start_allowed = 1` (beslut 69), och hindrar bara publiceringen: varje tryckning skrivs i aktivitetsloggen och eventloggen (beslut 72). En tom nyckel i startkroppen är ingen nyckel alls (beslut 73). Kuvertet heter `ErrandProcessOverview` (beslut 66), och `startable.status` hålls till `ProcessStartability` av byggaren i stället för av `@ValidEnumValue` (beslut 70). Testerna: `ProcessStartIT` kör kommandot och `startable` över tråden mot ett namespace utan triggers, med nödbromsen utlöst och hela vägen till pw-alkt. `ProcessStartModeIT` kör startlovet på ärendehändelser och etikettskrivningens kontroller. `ErrandProcessPersistenceTest` räknar frågorna i listsvaret.

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
- `processKey` i `ProcessStartRequest` får högst 128 tecken, annars `400`. Publiceringen tystar en för lång nyckel med en `CONFIG`-post, och för ett kommando vore det den tystaste felvägen (§6.5).
- `GET .../processes` svarar med kuvertet: ärende utan process ⇒ `processes: []` och `startable.status` enligt läget; ärende med `COMPLETED` ⇒ `status: PROCESS_COMPLETED` och tom `processKeys`.
- Ärende med en olevererad startrad ⇒ `status: START_PENDING` och tom `processKeys`, tills raden levererats — när en start annars hade varit `AVAILABLE`. Ett hinder som står före, till exempel att etiketten inte längre bär någon nyckel, visas i stället.
- Etikettskrivning med en `processKey` längre än 128 tecken ⇒ `400`, och en sådan nyckel som ändå kommit in förbi API:t erbjuds inte i `processKeys`.
- `processKeys` är tom så snart `status` inte är `AVAILABLE` — det finns inget läge där ett hinder redovisas tillsammans med nycklar att starta.
- `startable` kostar ingen extra fråga per ärende i listsvar — fältet finns bara på processendpointen, inte på ärendeprojektionen (§5.10). Verifieras med frågeräkning.
- **Den genererade specen granskas, inte bara annotationerna:** varje fält i `startable` ska gå att förstå av en klientutvecklare som inte läst det här dokumentet, och `status` ska räkna upp sina värden i beskrivningen (fältet är en sträng, beslut 42).

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

**P6 är en förutsättning för produktion, inte en förbättring.** Processlivet (§7.4 regel 4) och beslutslåsen
(§7.5) vilar på att SM:s processrad blir `COMPLETED` när instansen nått sitt slut. Det sker bara om pw rapporterar
`completed()` från sista arbetssteget, eller om den här avstämningen gör det i efterhand. Utan P6 kan en rad stå
som `RUNNING` för alltid: beslutet låses aldrig, `startable` säger `LIVE_INSTANCE` och ärendet går inte att starta
om.

---

## 11. Vad som kan gå fel, och vad vi gör åt det

|                              Risk                               |                                                                                                                                                                                             Hantering                                                                                                                                                                                              |
|-----------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **RabbitMQ-mognad** (öppen fråga)                               | §2.4. Varje REST-konsument byggd innan bytet är kastat arbete                                                                                                                                                                                                                                                                                                                                      |
| **`WAITING` felaktigt behandlad som terminal**                  | Skulle bryta 1-1-invarianten tyst. Skyddas av `applyStatus` som enda väg + tabelldrivet test (T1)                                                                                                                                                                                                                                                                                                  |
| **Handläggaren ändrar mitt i ett arbetssteg**                   | `412`, och steget körs om (§6.2). Kostar en omkörning. Syns som en INFO-rad per avvisad rapport (§8.1)                                                                                                                                                                                                                                                                                             |
| **Steg med extern sidoeffekt körs om**                          | Sidoeffekten kan dubbleras. Samma krav som när ett steg kraschar (§9.4) — lägg sidoeffekten sist i steget, eller gör den idempotent                                                                                                                                                                                                                                                                |
| **Läsande steg utan `errandVersion`**                           | Då finns inget skydd alls (§6.3). Det får vara ett medvetet val för varje steg — men ett val som faktiskt måste göras                                                                                                                                                                                                                                                                              |
| **Underresurser fångas inte av versionen**                      | En bilaga som raderas mitt i en körning höjer inte `errand.version`. Processen måste läsa om bilagor när den behöver dem                                                                                                                                                                                                                                                                           |
| **Parallella grenar**                                           | Modelleringsregel + WARN-aktivitet gör brottet synligt (§6.4)                                                                                                                                                                                                                                                                                                                                      |
| **Loop SM ↔ pw**                                                | Tre lager (§6.5). Lager 1 vilar på en klientsatt header — därför ska processens egen identitet aldrig förekomma bland outbox-radernas `executed_by` (§8.1)                                                                                                                                                                                                                                         |
| **Klient som tystar sina egna skrivningar**                     | `X-Trigger-Process: false` är fritt satt, så en integration som härmar pw kan göra sina ändringar osynliga för processen. Headern hedras inte för AD-identiteter (§6.5), men en maskinell integration som härmar pw lämnar inga spår alls — en undertryckt rad skrivs per definition inte                                                                                                          |
| **Beslut skrivet medan processen arbetar**                      | Korrelationen sväljs och väckningen är borta. Fångas bara av modelleringskravet i §9.2 punkt 1 — väntläget måste läsa om ärendet när det går in i väntan. Ingen kod i SM kan rädda ett väntläge som inte gör det                                                                                                                                                                                   |
| **`DECISION` saknas i `PROCESS_TRIGGER`**                       | Processen vaknar aldrig av beslutet och står i `WAITING` för alltid. Konfigurationen avvisas utan den (beslut 55), men en konfiguration från före kontrollen prövas först när den skrivs igen                                                                                                                                                                                                      |
| **Personuppgifter i beslutets motivering**                      | `justification` innehåller nästan alltid personuppgifter. Den maskas i payloadloggen (§8.1), kopieras aldrig till aktivitetsloggen och läggs aldrig i outboxens nyttolast — den bär medvetet ingen ärendedata alls (§5.4). Beslutet kaskaderas bort med ärendet                                                                                                                                    |
| **Automatiskt beslut felstämplat som manuellt, eller tvärtom**  | `method` valideras mot identiteten vid systemgränsen och står kvar i `decision.method`, `.decided_by` och `.errand_process_id` (§7.5). Utan dem går frågan "vilka beslut fattades av en maskin?" inte att svara på i efterhand — och det är en fråga som kommer att ställas                                                                                                                        |
| **Instans som försvinner utan slutrapport**                     | SM står kvar på `RUNNING` medan instansen är borta ur Operaton, och ärendet kan varken gå vidare eller få en ny process. Modelleringskravet i §9.2 punkt 5 ska hindra det; P6:s schemalagda kontroll stämmer av det som ändå glider isär                                                                                                                                                           |
| **Skelettmodellen driftsatt för tidigt**                        | Sex tomma subprocesser springer igenom på millisekunder. Startas den mot ett skarpt ärende är ärendets processliv förbrukat (§9.1). Driftsätt inte förrän väntlägena finns                                                                                                                                                                                                                         |
| **Manuell grind som ingen klickar på**                          | Processen står i `WAITING` för alltid. Modelleringsregeln i §9.2 punkt 2 kräver en tidsgräns på grindar som kan glömmas bort, och §8.1 visar hur en grind som står still hittas                                                                                                                                                                                                                    |
| **Signal som accepteras men aldrig konsumeras**                 | Går processen vidare på en timer i samma stund som handläggaren trycker, hinner SM svara `202` innan den nya bilden rapporterats. Signalen når då inget väntläge och är borta. Handläggaren ser det vid nästa omläsning, men ingenstans syns att det hände                                                                                                                                         |
| **Knapp som inte längre gäller**                                | Handläggaren ser en signal processen hunnit lämna. Skrivningen ger `409` — gränssnittet ska läsa om ärendet, inte försöka igen                                                                                                                                                                                                                                                                     |
| **Dubbelklick före processens nästa rapport**                   | Signalen förbrukar ingenting, så ett andra klick godtas och ger en andra aktivitetspost och händelse. pw tar emot den som en signal ingen grind väntar på. Värre blir det bara om modellen hamnar i en ny grind som väntar på samma namn innan den andra händelsen når fram — då stegar den processen en gång till. Gränssnittet ska inte erbjuda knappen igen förrän ärendet lästs om (beslut 64) |
| **Beslut på ärende helt utan process**                          | Tillåtet och olåst: det finns ingen process att låsa mot. Spårbarheten bärs då av händelseloggen, inte av spärren (§7.5)                                                                                                                                                                                                                                                                           |
| **Föräldralös processinstans efter radering**                   | `DELETE` publiceras även utan `processKey` (§2.2), också från gallringen (§7.5), och passerar loop-skyddets tre lager (§6.5), och pw raderar på `businessKey` (§9.3). Restrisk kvarstår om leveransen aldrig går igenom och raden åldras ur — därför ERROR-loggen när en rad åldras ur, och hälsoindikatorn (§8.3)                                                                                 |
| **Etikettändring utanför API:t**                                | `AddLabelAction.executeAction` körs schemalagt och `LabelMoveWorker` bygger om etiketterna när en etikett flyttats, båda utan att passera någon endpoint. Byter de upplöst `processKey` slutar processen tyst få väckningar. Kontrollen ligger därför även där: etiketterna lämnas som de var, och en ERROR-post syns på ärendet (§7.4)                                                            |
| **Etikettbyte medan starten är på väg**                         | Ett ärende som skapas med etikett A och byts till B innan pw registrerat starten hade kunnat köra A för alltid medan etiketterna säger B. En olevererad startrad räknas därför som ärendets process, både i spärren och i startlovet (§7.4, §7.7). Kvar är glappet mellan leveransen och registreringen, som pw och `409` från registreringen stänger (§5.10)                                      |
| **Sen rapport på en avslutad instans**                          | En omskickad `RUNNING` eller `FAILED` på en `COMPLETED` instans hade väckt raden till liv och hävt beslutslåsen. Rapporten svarar `200` och lämnar instansen som den är (§5.1)                                                                                                                                                                                                                     |
| **Konfigurationsändring som slår igenom i en podd i taget**     | `namespaceConfigCache` är per podd och evikteras bara där skrivningen tas emot. En ändrad `PROCESS_CONSUMER` eller triggerlista kan gälla olika i olika poddar i upp till tio minuter (§7.1)                                                                                                                                                                                                       |
| **Processrad som aldrig blir `COMPLETED`**                      | Processlivet och beslutslåsen vilar på att pw rapporterar `completed()` och på avstämningen i P6. Utan dem står raden som `RUNNING` för alltid. P6 är en förutsättning för produktion (§10)                                                                                                                                                                                                        |
| **Etiketter som ännu inte lästs från databasen**                | En etikett känner sin metadataetikett först när ärendet lästs in. Vid skapande, `PATCH` med etiketter, `ADD_LABEL` och e-postintag hittade publiceringen därför ingen nyckel, och processen startade aldrig. `ProcessKeySelector` slår upp sådana etiketter på id (§7.3). Täckt av T8 och `ProcessEventPublisherDatabaseTest`                                                                      |
| **Schemalagd åtgärd som ändrar ärendet i tysthet**              | `ActionWorker.processAction` lade till etiketten utan revision eller ärendehändelse, så processen fick aldrig veta det. Åtgärden ger nu båda när den ändrat ärendet, men ingen notis (beslut 48). Täckt av `ProcessStartModeIT`                                                                                                                                                                    |
| **Skenrevisioner som väcker processen**                         | Samma ärende gav olika snapshots nyss skrivet och nyss läst: etiketternas metadata, den inlästa statusen, tomma samlingar mot inga, etiketternas ordning och en starttid med fler decimaler än kolumnen. En `PATCH` utan ändring direkt efter skapande eller statusbyte gav därför revision, händelse och väckning. Rättat i beslut 49 och täckt av `ErrandsIT`                                    |
| **Etikett från ett annat namespace**                            | Etiketter anges med id, och ett id når etiketter i alla namespace. Ett ärende kunde därför få en annan verksamhets åtkomstregler och processnyckel. `POST` och `PATCH` avvisar nu med `400` en etikett som inte hör till ärendets namespace och kommun (beslut 50)                                                                                                                                 |
| **Ärende som pekar ut två processer**                           | Löser ut till ingen av dem, så ingen process startas och ingenting syns. Avvisas när etiketterna skrivs (§7.4 regel 5). Kan ändå uppstå om `processKey` läggs på en etikett ärendet redan bär — då fångar läs-sidan det (§7.3), och en manuell start kan peka ut nyckeln (§5.10)                                                                                                                   |
| **Ärende som redan är tvetydigt**                               | Varje etikettändring får `400` tills tvetydigheten är löst. Vägen ut är att ta bort den ena etiketten, vilket alltid går igenom (§7.4)                                                                                                                                                                                                                                                             |
| **Publiceringsfel sväljs av anropsstället**                     | `setRollbackOnly` före kast (§2.2) gör svälj-fångsten ofarlig. Kvarstående hål: anropsväg helt utan transaktion — syns som ERROR-logg                                                                                                                                                                                                                                                              |
| **E-post eller webbmeddelande tappas vid återrullning**         | Befintlig brist i intaget: meddelandet raderas i källsystemet, och e-postkvittensen skickas, före commit. Publiceringens `setRollbackOnly` är ytterligare en väg till en återrullning bland databasfelen. Rättas i en egen uppgift: radering och kvittens efter commit, kvittensen i en egen transaktion (§2.2)                                                                                    |
| **Start uteblir när etiketten sätts sent**                      | Start villkoras av `processKey`, inte `eventType` (§9.3). Täckt av ett P2-fall                                                                                                                                                                                                                                                                                                                     |
| **Startlov som hunnit bli inaktuellt**                          | Lovet räknas ut vid publicering och används vid leverans. Hinner processen gå i mål däremellan startar pw något den inte borde — `409` från `POST .../processes` och pw:s avbrytande av den nystartade instansen är skyddsnätet (§7.7)                                                                                                                                                             |
| **Felstavat `processStartMode`**                                | Attributnycklar är inte whitelistade (§1.6), så `processstartmode` hade tyst betytt `AUTOMATIC`. Värdet valideras därför när etiketten skrivs, inte när den läses (§7.7)                                                                                                                                                                                                                           |
| **Automatisk start på gamla ärenden vid driftsättning**         | Ett ärende som redan bär etiketten startar vid nästa ändring, inte bara vid `CREATE` (beslut 17). Rulla ut med `MANUAL` på etiketterna och byt till `AUTOMATIC` när kedjan är sedd i drift (§7.7)                                                                                                                                                                                                  |
| **Signal utan namn i händelsen**                                | pw vet att någon tryckte men inte på vad, och grinden öppnas aldrig. `signal_name` i outbox-raden och `signalName` på händelsen bär namnet hela vägen (§5.4); pw svarar `202` med ERROR-logg om det ändå saknas, eftersom en retry inte kan hjälpa                                                                                                                                                 |
| **Kommando som tystas av ett filter**                           | Ett `202` utan verkan är knappen som ser ut att fungera. Kommandon undantas därför från nödbromsen och triggerfiltret, och de tre lagren gäller bara härledda händelser (§6.5). Täckt av testfall i T5 och T12                                                                                                                                                                                     |
| **Maskinidentitet som trycker på startknappen**                 | Skulle ge `202` och ingen start, eftersom lager 1 filtrerar bort raden när pw:s `X-Trigger-Process: false` följer med (§6.5). Kommandot kräver AD-konto och svarar `403` (§5.10)                                                                                                                                                                                                                   |
| **Felstavat `processKey`**                                      | Upptäcks vid första ärendet. `422` ⇒ ingen retry, `FAILED` + ERROR-aktivitet direkt på ärendet                                                                                                                                                                                                                                                                                                     |
| **Oskickad rad som ingen upptäcker**                            | Utan dead letter-flagga finns ingenting att larma på i tabellen. Hälsoindikatorn och ERROR-loggen när en rad åldras ur är det som gör raden synlig (§8.3)                                                                                                                                                                                                                                          |
| **Återlevererad händelse efter återrullad transaktion**         | Leverans och kvittering delar transaktion, så ett fel efter att pw tagit emot händelsen ger en till. pw:s event-endpoint måste vara idempotent — annars blir följden dubbla processinstanser (§8.3)                                                                                                                                                                                                |
| **Routingen går att ändra i drift, utan granskning**            | Priset för att slippa en release varje gång. Validering av `PROCESS_CONSUMER`; överväg ändringslogg                                                                                                                                                                                                                                                                                                |
| **En långsam pw-alkt håller körningen**                         | Leveransen håller anropet inne i transaktionen. Motmedlen är `batch-size`, en circuit breaker som bara räknar anrop utan svar, och kort read-timeout (§7.6)                                                                                                                                                                                                                                        |
| **Händelser som pw-alkt aldrig tar fyller batchen**             | Ett ärende vars äldsta rad alltid fallerar samlar rader bakom sig. Cronjobbet hämtar vidare förbi ärenden som fallerat under körningen och räknar varje sådant ärende som ett försök, så raderna kan inte fylla körningen. Först när fler än `batch-size` ärenden fallerar samtidigt får resten vänta. Syns i hälsoindikatorn (§8.3)                                                               |
| **Rad som fallerar mitt i en grupp**                            | Raderna före den är redan hos pw. De kvitteras, och bara den och de senare ges igen — annars hade signaler och startkommandon skickats om varje minut i upp till 30 dagar (§8.3)                                                                                                                                                                                                                   |
| **Nödbromsen tolkar ett leveransavbrott som en loop**           | Skulle förvandla en fördröjning till permanent händelseförlust. Bromsen räknar därför bara rader med `delivered_at` satt (§6.5)                                                                                                                                                                                                                                                                    |
| **Hälsoindikatorn på existens i stället för ålder**             | Tjänsten står unhealthy under normal drift och indikatorn slutar betyda något. Villkoret är `unhealthy-after` (§8.3)                                                                                                                                                                                                                                                                               |
| **`justification` utanför fältfiltreringen**                    | Beslutsmotiveringen är fritext med personuppgifter. Ärendet bär inte beslutet, och `.../decisions` kräver resursen `DECISION`, som begränsad läsning bara når när namespacet räknat upp den (§5.3). Att ALKT saknar åtkomstkontroll döljer bara felet till nästa namespace                                                                                                                         |
| **Åtkomstkontroll påslagen för ett namespace med processmotor** | pw får `401` på allt, och felet visar sig som ärenden som står stilla — inte som ett behörighetsfel. AccessMapper svarar bara på AD-konton (§1.8). Spärren i §7.1 gör det till ett konfigurationsfel i stället, och lyfts först när AccessMapper kan bevilja maskinidentiteter                                                                                                                     |
| **Personuppgifter i aktivitetsloggen**                          | `message` är fri text som kommer från processen. **pw måste instrueras att inte skriva personuppgifter där** — det är en regel, inte en spärr                                                                                                                                                                                                                                                      |
| **Dubbla processinstanser**                                     | Radlås på outbox-raderna vid leverans + businessKey-kontroll + `409` + DB-constraint. Restrisk i Operaton, som saknar unikhet på business key — men SM kan inte registrera resultatet                                                                                                                                                                                                              |
| **Delas Operaton-tenanten `ALKT`?**                             | Påverkar `getDeployments`-assertions och `historyTimeToLive`. Bekräfta mot driftmiljön                                                                                                                                                                                                                                                                                                             |

### Vad som inte går att verifiera automatiskt

- Att WSO2 släpper igenom med rätt scope, och att `If-Match`/`ETag` passerar oförvanskade.
- Verklig samtidighet mellan poddar — ShedLock täcks indirekt av `ShedlockConfigurationTest`.
- Långtidsbeteende hos outbox och aktivitetslogg. Kompensation: hälsoindikatorn (§8.3).

