# Överlämning: release av Telia ACE (SupportManagement)

> Miljöspecifika värden (Telia-URL, inloggning) finns inte i repot och står som `<…>`. De behöver fyllas i.

## Vad som släpps

När EmailReader skapar eller uppdaterar ett ärende läggs en work item i kö. Ett schemalagt jobb skickar den till Telia ACE med en länk till ärendet i Draken. Första gången skapas en work item (`POST /workitem/`) och därefter uppdateras den (`PUT /workitem/emailUid/{emailUid}`).

**Releasestatus:** ACE-koden ligger i `main` sedan 2026-10-14 (PR #783 och #787, version 18.5). Den är avstängd som standard, så den kan redan finnas ute utan att göra något.

---

## 1. Databas

Flyway-migreringen `V1_66__add_telia_ace_tables.sql` skapar tabellerna `telia_ace_work_item` och `telia_ace_contact`. Den körs automatiskt vid deploy och kräver inget manuellt steg.

## 2. Miljökonfiguration (config server / env), per miljö

| Property | Värde | Kommentar |
|---|---|---|
| `integration.telia-ace.url` | `<Telia ACE WorkItem proxy-URL>` | Saknas den används `https://telia-ace.invalid`. Tjänsten startar ändå, men alla anrop misslyckas. |
| `integration.telia-ace.username` | `<secret>` | Basic auth. Saknas den loggas en WARN och anropen misslyckas. |
| `integration.telia-ace.password` | `<secret>` | Samma som ovan. |
| `scheduler.telia-ace-work-item.enabled` | **`true`** | **Global flagga.** Standard är `false`. Utan den köas inget, oavsett namespace-inställningen. |

Följande har bra standardvärden och behöver normalt inte ändras: `connect-timeout: 5`, `read-timeout: 30`, `cron: 0 */2 * * * *` (varannan minut) och `max-age: P3D` (work items som legat i kö längre än 3 dagar kastas).

## 3. Namespace-flaggor (per namespace, via API)

Sätts i namespace-konfigurationen för Kontakt Sundsvalls namespace:

| Fält | Värde |
|---|---|
| `teliaAceWorkItemEnabled` | `true` |
| `teliaAceWorkItemBaseUrl` | `https://<draken-host>/kontaktsundsvall` (prod t.ex. `https://draken.sundsvall.se/kontaktsundsvall`) |

Länken till ACE byggs som `<baseUrl>/arende/<ärendenummer>`. Den måste börja med http(s) och får vara högst 230 tecken.

> ⚠️ `PUT /{municipalityId}/{namespace}/namespace-config` **ersätter hela konfigurationen**. Hämta den befintliga med GET först, lägg till de två fälten och skicka tillbaka allt. Annars nollställs övriga inställningar. Namespace-config cachas i upp till 10 minuter, så ändringen syns inte direkt.

Båda flaggorna måste vara på: den globala (`scheduler.telia-ace-work-item.enabled`) **och** namespace-flaggan (`teliaAceWorkItemEnabled`).

## 4. Vilka ärenden som skickas till ACE

Bara ärenden som skapas eller uppdateras via EmailReader skickas. Undantag är utkast, inaktiva ärenden, leveransrapporter och autosvar.

---

## Miljöer och ordning

| Steg | Miljö | Miljökonfiguration | Namespace-flaggor |
|---|---|---|---|
| 1 | **Test** | Sätt URL, användarnamn och lösenord för Telias testmiljö. `enabled: true` | På, `baseUrl` = test-Draken |
| 2 | Verifiera i test | Se checklistan nedan | |
| 3 | **Prod** | Sätt prod-URL och prod-inloggning. `enabled: true` | På, `baseUrl` = `https://draken.sundsvall.se/kontaktsundsvall` |

Sätt integrationen (URL och inloggning) **innan** du slår på flaggorna. Då hamnar inga work items i kö som bara misslyckas.

## Verifiering

- [ ] Skicka ett mejl till Kontakt Sundsvalls inkorg. En work item ska dyka upp i ACE inom cirka 2 minuter, och länken ska öppna rätt ärende.
- [ ] Svara på samma ärende via mejl. Den befintliga work itemen ska uppdateras ("Nytt meddelande mottaget från …"), inte dupliceras.
- [ ] Tabellen `telia_ace_work_item` ska tömmas och `telia_ace_contact` ska få en rad per ärende.
- [ ] Health-indikatorn `deliver_telia_ace_work_items` ska vara grön.

## Felsökning och rollback

- **401, 403 eller 429 från Telia:** work itemen ligger kvar i kön och health-indikatorn blir röd. Kontrollera inloggningen. Kön skickas automatiskt när felet är åtgärdat, så länge itemen är yngre än 3 dagar.
- **Andra 4xx:** work itemen kastas och ett ERROR loggas.
- **Rollback:** sätt `scheduler.telia-ace-work-item.enabled: false` (stänger av för alla namespaces) eller `teliaAceWorkItemEnabled: false` på namespacet. Ingen deploy krävs för namespace-flaggan.
