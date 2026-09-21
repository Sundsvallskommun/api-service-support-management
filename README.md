# SupportManagement

_Provides features for managing cases related to support related functions. It includes functionalities such as
creating, updating, and tracking errand statuses and progress._

## Getting Started

### Prerequisites

- **Java 25 or higher**
- **Maven**
- **MariaDB**
- **Git**
- **[Dependent Microservices](#dependencies)**

### Installation

1. **Clone the repository:**

   ```bash
   git clone git@github.com:Sundsvallskommun/api-service-support-management.git
   cd api-service-support-management
   ```
2. **Configure the application:**

   Before running the application, you need to set up configuration settings.
   See [Configuration](#Configuration)

   **Note:** Ensure all required configurations are set; otherwise, the application may fail to start.

3. **Ensure dependent services are running:**

   If this microservice depends on other services, make sure they are up and accessible.
   See [Dependencies](#dependencies) for more details.

4. **Build and run the application:**

   ```bash
   mvn spring-boot:run
   ```

## Dependencies

This microservice depends on the following services:

- **EmailReader**
  - **Purpose:** Reads e-mails sent to mailboxes and provides them for processing by SupportManagement and other
    systems.
  - **Repository:** [Link to the repository](https://github.com/Sundsvallskommun/api-service-email-reader)
  - **Setup Instructions:** Refer to its documentation for installation and configuration steps.
- **Employee**
  - **Purpose:** Used for reading employee information.
  - **Repository:** Not available at this moment.
  - **Additional Notes:** Employee is a API serving data
    from [Metadatakatalogen](https://utveckling.sundsvall.se/digital-infrastruktur/metakatalogen).
- **Eventlog**
  - **Purpose:** Used for logging events
  - **Repository:** [Link to the repository](https://github.com/Sundsvallskommun/api-service-eventlog)
  - **Setup Instructions:** Refer to its documentation for installation and configuration steps.
- **Messaging**
  - **Purpose:** Used to send communications to stakeholders via E-mail, SMS or Open-E Webmessage
  - **Repository:** [Link to the repository](https://github.com/Sundsvallskommun/api-service-messaging)
  - **Setup Instructions:** Refer to its documentation for installation and configuration steps.
- **Notes**
  - **Purpose:** Provides functionality for storing and retrieving notes linked to an organization or a citizen.
  - **Repository:** [Link to the repository](https://github.com/Sundsvallskommun/api-service-notes)
  - **Setup Instructions:** Refer to its documentation for installation and configuration steps.
- **WebMessageCollector**
  - **Purpose:** Reads web messages sent to open-E and provides them for processing by SupportManagement and other
    systems.
  - **Repository:** [Link to the repository](https://github.com/Sundsvallskommun/api-service-web-message-collector)
  - **Setup Instructions:** Refer to its documentation for installation and configuration steps.
- **Relation**
  - **Purpose:** Used to fetch relation data (linked errands)
  - **Repository:** [Link to the repository](https://github.com/Sundsvallskommun/api-service-relations)
  - **Setup Instructions:** Refer to its documentation for installation and configuration steps.
- **AccessMapper**
  - **Purpose:** Provides functionality for fetching access restrictions per user (optional setting per namespace).
  - **Repository:** [Link to the repository](https://github.com/Sundsvallskommun/api-service-access-mapper)
  - **Setup Instructions:** Refer to its documentation for installation and configuration steps.
- **Citizen**
  - **Purpose:** Used for reading citizen information.
  - **Repository:** [Link to the repository](https://github.com/Sundsvallskommun/api-service-citizen)
  - **Setup Instructions:** Refer to its documentation for installation and configuration steps.
- **JsonSchema**
  - **Purpose:** Provides functionality for validating JSON data against predefined schemas.
  - **Repository:** [Link to the repository](https://github.com/Sundsvallskommun/api-service-json-schema)
  - **Setup Instructions:** Refer to its documentation for installation and configuration steps.
- **MessageExchange**
  - **Purpose:** Provides functionality for handling message conversations and messages.
  - **Repository:** [Link to the repository](https://github.com/Sundsvallskommun/api-service-message-exchange)
  - **Setup Instructions:** Refer to its documentation for installation and configuration steps.
- **MessagingSettings**
  - **Purpose:** Used for fetching messaging settings per municipality.
  - **Repository:** [Link to the repository](https://github.com/Sundsvallskommun/api-service-messaging-settings)
  - **Setup Instructions:** Refer to its documentation for installation and configuration steps.
- **pw-alkt**
  - **Purpose:** Runs the case handling processes (Operaton) for alcohol and tobacco permits. SupportManagement
    delivers errand events to it, and it reports the state of its processes back. Only needed for namespaces that run
    processes — see [Process Integration](#process-integration).
  - **Repository:** [Link to the repository](https://github.com/Sundsvallskommun/pw-alkt)
  - **Setup Instructions:** Refer to its documentation for installation and configuration steps.

Ensure that these services are running and properly configured before starting this microservice.

## API Documentation

Access the API documentation via Swagger UI:

- **Swagger UI:** [http://localhost:8080/api-docs](http://localhost:8080/api-docs)

Alternatively, refer to the `openapi.yaml` file located in `src/test/resources/api` for the OpenAPI specification.

## Usage

### API Endpoints

Refer to the [API Documentation](#api-documentation) for detailed information on available endpoints.

### Example Request

```bash
curl -X GET http://localhost:8080/2281/my.namespace/errands/b82bd8ac-1507-4d9a-958d-369261eecc15/communication
```

## Configuration

Configuration is crucial for the application to run successfully. Ensure all necessary settings are configured in
`application.yml`.

### Key Configuration Parameters

- **Server Port:**

  ```yaml
  server:
    port: 8080
  ```
- **Database Settings:**

  ```yaml
  spring:
    datasource:
      url: jdbc:mysql://localhost:3306/your_database
      username: your_db_username
      password: your_db_password
  ```
- **External Service URLs:**

  ```yaml
  integration:
    accessmapper:
      url: http://dependency_service_url
    citizen:
      url: http://dependency_service_url
    emailreader:
      url: http://dependency_service_url
    employee:
      url: http://dependency_service_url
    eventlog:
      url: http://dependency_service_url
    json-schema:
      url: http://dependency_service_url
    messageexchange:
      url: http://dependency_service_url
    messaging:
      url: http://dependency_service_url
    messaging-settings:
      url: http://dependency_service_url
    notes:
      url: http://dependency_service_url
    pw-alkt:
      url: http://dependency_service_url
    relation:
      url: http://dependency_service_url
    web-message-collector:
      url: http://dependency_service_url

  spring:
    security:
      oauth2:
        client:
          provider:
            accessmapper:
              token-uri: http://dependency_service_token_url
            citizen:
              token-uri: http://dependency_service_token_url
            emailreader:
              token-uri: http://dependency_service_token_url
            employee:
              token-uri: http://dependency_service_token_url
            eventlog:
              token-uri: http://dependency_service_token_url
            json-schema:
              token-uri: http://dependency_service_token_url
            messageexchange:
              token-uri: http://dependency_service_token_url
            messaging:
              token-uri: http://dependency_service_token_url
            messaging-settings:
              token-uri: http://dependency_service_token_url
            notes:
              token-uri: http://dependency_service_token_url
            pw-alkt:
              token-uri: http://dependency_service_token_url
            relation:
              token-uri: http://dependency_service_token_url
            web-message-collector:
              token-uri: http://dependency_service_token_url
          registration:
            accessmapper:
              client-id: the-client-id
              client-secret: the-client-secret
            citizen:
              client-id: the-client-id
              client-secret: the-client-secret
            emailreader:
              client-id: the-client-id
              client-secret: the-client-secret
            employee:
              client-id: the-client-id
              client-secret: the-client-secret
            eventlog:
              client-id: the-client-id
              client-secret: the-client-secret
            json-schema:
              client-id: the-client-id
              client-secret: the-client-secret
            messageexchange:
              client-id: the-client-id
              client-secret: the-client-secret
            messaging:
              client-id: the-client-id
              client-secret: the-client-secret
            messaging-settings:
              client-id: the-client-id
              client-secret: the-client-secret
            notes:
              client-id: the-client-id
              client-secret: the-client-secret
            pw-alkt:
              client-id: the-client-id
              client-secret: the-client-secret
            relation:
              client-id: the-client-id
              client-secret: the-client-secret
            web-message-collector:
              client-id: the-client-id
              client-secret: the-client-secret
  ```

### Database Initialization

The project is set up with [Flyway](https://github.com/flyway/flyway) for database migrations. Flyway is disabled by
default so you will have to enable it to automatically populate the database schema upon application startup.

```yaml
spring:
  flyway:
    enabled: true
```

- **No additional setup is required** for database initialization, as long as the database connection settings are
  correctly configured.

### Additional Notes

- **Application Profiles:**

  Use Spring profiles (`dev`, `prod`, etc.) to manage different configurations for different environments.

- **Logging Configuration:**

  Adjust logging levels if necessary.

## Access Control

Access control is **opt-in per namespace** and inert by default. The `accessControl` flag on the namespace config is
the master switch: while it is `false`, none of the machinery below applies and every caller sees every errand in the
namespace.

### The two authorities

The system answers two different questions from two different sources, and keeping them apart is the key to reading
the rest of this section:

> **The access mapper answers "what may this AD identity do".**
> **Namespace config answers "what may a non-AD role do on its own errand".**

A case officer exists in AD, so the access mapper supplies everything for them: which errands they see, which
sub-resources they may reach, and which role governs the payload they get back. A reporter is not in AD at all — no
group, no patterns — so none of it applies to them, and their grants live in namespace config instead.

The two never conflict. Namespace config only adds access where the access mapper is silent by construction, so a case
officer who also happens to have reported an errand keeps their officer entitlements untouched.

### Access levels

Three levels, ordered `LR < R < RW`:

| Level |                            Meaning                            |
|-------|---------------------------------------------------------------|
| `LR`  | Limited read — the errand is visible, but trimmed (see below) |
| `R`   | Read                                                          |
| `RW`  | Read and write                                                |

Every read guard in the service layer requires **at least `LR`**. That makes limited read a floor rather than a dead
end: a namespace can widen `LR` all the way up to `R` by extending the resources and fields it covers.

### Access mapper group types

The access mapper is queried per AD identity with three group types, each carrying ant-style patterns and a level:

|    Type    |    Patterns match against     |                    Governs                     |
|------------|-------------------------------|------------------------------------------------|
| `label`    | metadata label resource paths | Which errands are visible / writable (Layer A) |
| `resource` | `ProtectedResource` paths     | Which sub-resources are reachable (Layer B)    |
| `role`     | free-form role names          | Which fields come back (Layer C)               |

### The three layers

**Layer A — visibility and write filtering.** A JPA specification restricts which errands come back and whether a
write is allowed, based on the caller's label grants. An errand carrying no access labels is accessible to everyone.
The labels an errand is given must belong to its own namespace and municipality; `POST` and `PATCH` answer `400`
otherwise, since a label brings its access rules along.

The level a label grant must reach depends on *what* is being written. `RW` is demanded for the errand itself, which is
what the labels are held against. For a resource **of** the errand the resource grant carries the write and the labels
only have to reach the errand at `R` — so a caller holding a category at read, plus `errand/conversation/message` at
`RW`, may post a message while `PATCH /errands/{errandId}` stays refused. That split needs a second axis to be doing
the restricting, so it applies only while `resourceAccessControl` is on; with it off the labels are all there is and
carry the write themselves, exactly as before. A write is never permitted on an errand the caller only holds at `LR`.

**Layer B — resource entitlement.** Which sub-resources the caller may reach, from the access mapper's `resource`
groups. Gated by the `resourceAccessControl` flag: while it is `false`, resources are unrestricted and only labels
apply. This exists so a namespace can enable `accessControl` before the access mapper has any `resource` groups
configured, without every sub-resource turning into a 401.

**Layer C — field mapping.** Which fields of the errand payload are returned. `roleBasedMapping` governs the *role*
restrictions only: while it is `false` no role trims anything, but an errand the caller only has limited read for is
still trimmed, since limited read may never silently mean full read. The same grants bind writes, so a whole-errand
`PATCH` can neither set nor delete a key the caller may not read.

### Resource taxonomy

`ProtectedResource` gives every guarded resource a hierarchical path, which is what access-mapper `resource` patterns
are matched against with `AntPathMatcher`. Note that `errand/**` matches `errand` itself as well as everything beneath
it, so one pattern really does grant the whole errand tree. Where several patterns match, the most permissive wins.

|          Constant          |                                                                        Path                                                                         |
|----------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| `ERRAND`                   | `errand`                                                                                                                                            |
| `ATTACHMENT`               | `errand/attachment`                                                                                                                                 |
| `COMMUNICATION`            | `errand/communication`                                                                                                                              |
| `COMMUNICATION_ATTACHMENT` | `errand/communication/attachment`                                                                                                                   |
| `CONVERSATION`             | `errand/conversation`                                                                                                                               |
| `CONVERSATION_MESSAGE`     | `errand/conversation/message`                                                                                                                       |
| `CONVERSATION_ATTACHMENT`  | `errand/conversation/attachment`                                                                                                                    |
| `EVENT`                    | `errand/event`                                                                                                                                      |
| `NOTE`                     | `errand/note`                                                                                                                                       |
| `NOTE_REVISION`            | `errand/note/revision`                                                                                                                              |
| `PARAMETER`                | `errand/parameter`                                                                                                                                  |
| `JSON_PARAMETER`           | `errand/json-parameter`                                                                                                                             |
| `NOTIFICATION`             | `errand/notification`                                                                                                                               |
| `REVISION`                 | `errand/revision`                                                                                                                                   |
| `TIME_MEASURE`             | `errand/time-measure`                                                                                                                               |
| `NAMESPACE_CONFIG`         | `namespace-config`                                                                                                                                  |
| `METADATA_*`               | `metadata/category`, `metadata/contact-reason`, `metadata/external-id-type`, `metadata/label`, `metadata/phase`, `metadata/role`, `metadata/status` |
| `SUBSCRIBER`               | `subscriber`                                                                                                                                        |
| `SUBSCRIPTION`             | `subscriber/subscription`                                                                                                                           |
| `SUBSCRIBER_NOTIFICATION`  | `subscriber-notification`                                                                                                                           |

Everything from `NAMESPACE_CONFIG` down is **namespace-scoped** rather than errand-scoped — see below.

### Namespace config blocks

Three named blocks shape what a caller sees. They are named after their nature rather than made uniform, because they
do genuinely different things.

|          Block          |                   Applies when                    |                Carries                 |                Absence means                |
|-------------------------|---------------------------------------------------|----------------------------------------|---------------------------------------------|
| `limitedReadAccess`     | the caller's labels do not cover the errand fully | resources (no level) **and** fields    | the errand only, showing a built-in minimum |
| `reporterAccess`        | `reporterUserId` matches the caller               | resources (with levels) **and** fields | the reporter gets nothing                   |
| `roleFieldRestrictions` | the caller holds the role and mapping is on       | fields                                 | no restriction, so the full errand          |

`reporterAccess` **grants** — it is the only thing that lets a reporter in at all. `limitedReadAccess` is mixed: its
resources grant reach, its fields restrict the payload. `roleFieldRestrictions` can only ever narrow a payload, never
grant anything, since the caller has already cleared Layers A and B to reach the errand.

**Failing open is right for a restriction, wrong for limited read.** An unlisted role is genuinely unrestricted, which
keeps rollout incremental. Reporter fields therefore widen a restriction rather than introduce one: a caller nothing
else restricts stays unrestricted on an errand they reported, since reporting an errand may not reduce their access. But declaring an errand *limited* and then returning every field would be
self-contradictory, so limited read never resolves to empty — with no configured fields it falls back to `ID`,
`ERRAND_NUMBER`, `TITLE`, `STATUS`. Turning `roleBasedMapping` on can therefore never widen what a limited-read user
sees.

`limitedReadAccess.resources` deliberately carries **no level**, because whether an errand is limited is already
settled by the caller's labels, and within limited read a resource is simply readable or not — a resource cannot be
partially read the way a payload can be trimmed. `reporterAccess.resources` keeps levels, because a reporter may
legitimately be granted write, e.g. `CONVERSATION_MESSAGE: RW` so they can reply on their own errand.

`ERRAND` is implicitly reachable on the limited path — that *is* what limited read means — so a namespace that
configures nothing behaves exactly as it did before access control existed.

A role may not be named `LIMITED` or `REPORTER`. Those are the reserved scopes the two blocks above are stored under,
so such a restriction would be read back as the namespace's limited read or reporter configuration; the config
endpoints answer 400 instead.

### Reporter identity

A caller is treated as the reporter when the `X-Sent-By` identifier is of type `adAccount` and its value equals the
errand's `reporterUserId`. A `partyId` identifier never matches, and an errand with a null `reporterUserId` never
matches anyone.

### Key-level grants

Three fields are *keyed*, meaning a grant may name individual keys instead of the whole collection:
`PARAMETERS`, `JSON_PARAMETERS` and `EXTERNAL_TAGS`. A field granted with no keys exposes all of them.

These grants bind the dedicated sub-resource endpoints too, not just the errand payload — otherwise a role limited to
one parameter key could simply read every key through `/parameters`. Reads of an ungranted key return 401 and list
endpoints filter. Writes are covered on the same basis: a key you cannot read is a key you cannot write, so nobody can
overwrite or remove data they are not allowed to see.

### Namespace-scoped resources

Namespace config, metadata, subscribers, subscriptions and subscriber notifications are not errands, so they are
guarded differently: **write** endpoints require `RW` on the corresponding `ProtectedResource` from the access mapper's
`resource` groups. This exists so that a caller with broad errand rights cannot simply switch access control off. A
grant of `errand/**` deliberately does **not** cover `namespace-config`, `metadata/*` or `subscriber*`.

Creating an errand subscription is additionally checked against the errand itself, requiring at least `LR` on
`ERRAND` — subscribing reveals the errand's activity, and routing the lookup through the access control service also
stops the endpoint being used as an existence oracle.

These guards live in the resource layer rather than the service layer, because placing them in `MetadataService`
creates a circular dependency (`metadataService → accessControlService → accessMapperService → metadataService`).

Reads of namespace config and metadata are not currently guarded, and neither is **ownership** of subscribers: any
caller may currently list, read or modify another identity's subscriber, subscriptions and notifications. Resource
entitlement is the wrong tool for that — it needs an ownership rule — so it remains open.

### Worked example

```json
{
  "accessControl": true,
  "resourceAccessControl": true,
  "roleBasedMapping": true,
  "limitedReadAccess": {
    "resources": ["COMMUNICATION"],
    "fields": [
      { "field": "ID" }, { "field": "ERRAND_NUMBER" },
      { "field": "TITLE" }, { "field": "STATUS" }
    ]
  },
  "reporterAccess": {
    "resources": [
      { "resource": "ERRAND", "level": "R" },
      { "resource": "COMMUNICATION", "level": "R" },
      { "resource": "CONVERSATION_MESSAGE", "level": "RW" }
    ],
    "fields": [
      { "field": "TITLE" }, { "field": "STATUS" },
      { "field": "PARAMETERS", "keys": ["contactChannel"] }
    ]
  },
  "roleFieldRestrictions": [
    {
      "role": "FIRST_LINE_CASE_OFFICER",
      "fields": [{ "field": "TITLE" }, { "field": "STATUS" }, { "field": "STAKEHOLDERS" }]
    }
  ]
}
```

### How a request is evaluated

1. If `accessControl` is `false` for the namespace, stop — everything is permitted.
2. **Layer B** — if `resourceAccessControl` is on, the access mapper's `resource` grants must permit the target
   resource at the required level, otherwise 401.
3. **Layer A** — the errand must be reachable, which is true if either the caller's label grants cover it at the
   level that resource demands of them — `RW` for a write to the errand itself, `R` for a write the resource grant
   carries, `LR` where limited read is extended to the resource — or the caller is the reporter and `reporterAccess`
   grants the resource at the required level.
4. **Layer C** — on the way out the payload is trimmed: a limited errand uses `limitedReadAccess.fields` whatever the
   `roleBasedMapping` flag says, and otherwise the matched roles' `roleFieldRestrictions` apply if that flag is on.
   Reporter fields union on top of whichever restriction applied, and a caller no restriction applied to receives the
   full errand.

### Discovering what may be configured

The `field` and `resource` values above are published as **data**, not as an enum of the schema:

`GET /{municipalityId}/{namespace}/namespace-config/access-definition`

```json
{
  "fields": [{ "field": "PARAMETERS", "property": "parameters", "keyed": true }],
  "resources": [{ "resource": "COMMUNICATION", "path": "errand/communication", "errandScoped": true }]
}
```

Exposing a new errand field or guarding a new resource therefore leaves the published contract untouched — only this
response grows. A client configuring access reads the accepted values from here; `keyed` tells it whether `keys` and a
`level` may be given for a field, and `property` and `path` connect a configured value to how the access of an errand
reports it.

The values are still enforced: the request models remain typed by the enums internally, so an unknown value is refused
with a 400 exactly as before — only the published schema says `string` rather than listing the constants.

### Asking what a caller may do

`GET /{municipalityId}/{namespace}/errands/{errandId}/access` reports the outcome of the evaluation above for one
errand, so a client can render only the controls the caller's next request would be allowed to make — including whether
a jsonSchema driven form is editable before any data has been saved to it, which inspecting the errand cannot answer.

```json
{
  "level": "R",
  "fields": [
    { "field": "title", "level": "R" },
    { "field": "parameters", "level": "RW", "allKeys": false,
      "keys": [{ "key": "granted-key", "level": "RW" }, { "key": "readonly-key", "level": "R" }] },
    { "field": "jsonParameters", "level": "R", "allKeys": true, "keys": [] }
  ],
  "resources": [{ "resource": "errand/communication", "level": "R" },
                { "resource": "errand/parameter", "level": "RW" }]
}
```

- `level` is what the caller holds the errand itself at. Reaching the endpoint at all means at least `LR`. The example
  above is the shape this takes for a caller the errand is read-only to whose grant on `errand/parameter` carries the
  write: `title` follows the errand and `parameters` follows the resource serving it.
- `fields` lists the fields the caller reaches, each named as the property is written in the errand payload rather than
  as the `ErrandField` constant, so a client looks the answer up against what it renders and adding a field here leaves
  the published contract alone. One that is not listed is absent from the errand payload, which says nothing about an endpoint of its own serving it - `resources` answers that. Fields carry **no level of
  their own** — a namespace may only hold an individual key to read, never a whole field, so a field is writable exactly
  when what serves it is: the errand for most of them, and for `parameters` and `jsonParameters` the resource carrying
  their own write endpoint, so a field's `level` may be **wider than the errand's**. Every property a response carries
  is a field, `phases` and `actions` included — the one exception is `activePhaseId`, which is inbound only: a request
  names the phase to move the errand into, and the response carries the phases themselves, of which the active one is
  the phase not yet ended. Whether a property is writable *at all* is a separate question answered by `readOnly` in the
  schema.
- A key restriction is all or nothing. `allKeys: true` means the field carries no key restriction and every key of it
  follows the field's own `level`, including keys that may be added. `allKeys: false` means `keys` is exhaustive: those are the only
  keys the caller reaches, each with its own level, already held against the level of the errand.
- A granted key is listed whether or not the errand carries it yet, so a key shown as `RW` may be **created** as well as
  changed. That is what lets a form be rendered editable before anything is saved to it.
- `allKeys` and `keys` are only set for the keyed fields `parameters`, `jsonParameters` and `externalTags`.
- `resources` covers the errand scoped resources the caller reaches, each named by the path access is granted on rather
  than by the `ProtectedResource` constant, for the same reason the fields are. The access definition above maps the two
  onto each other. The errand itself is reported as `level` rather than repeated here.

A resource grant never stands in for the labels. Every endpoint needs both: the access mapper must grant the resource
at the level the operation asks for, *and* the caller's labels must reach the errand at the level that resource demands
of them. What the two are held against differs, though, which is why `resources` may report a level **above** `level`:
with `resourceAccessControl` on, `{errand: R, errand/parameter: RW}` and labels at read opens
`PATCH /errands/{errandId}/parameters/{key}` while leaving `PATCH /errands/{errandId}` refused, and the report says
exactly that — `level: "R"` with `errand/parameter` at `RW`.

The keys of a keyed field follow the same split, so a `parameters` key may be reported `RW` on an errand reported `R`.
Only `parameters` and `jsonParameters` have a write endpoint of their own; `externalTags` and every other field are
written through `PATCH /errands/{errandId}` alone and stay held against the errand.

One grant the response cannot express: a whole keyed collection granted *and* held to read
(`{"field": "PARAMETERS", "level": "R"}` with no keys — namespace configuration still names fields and resources by
constant, only the report uses property and path). It has no keys to carry the restriction and fields carry no level, so
it is reported as `allKeys: true` and overstates what the write paths accept. No namespace configures this.

`AccessControlSpecificationParityTest` holds the in-memory answer this endpoint reports to the JPA specification that
actually guards every endpoint, so the two cannot drift apart.

## Process Integration

A namespace can have its errands driven by a case handling process running in a process engine. Like access control,
this is **opt-in per namespace** and off by default: a namespace without a process consumer publishes nothing and is
unaffected by everything below. The full design, in Swedish, is in
[`docs/alkt-processintegration.md`](docs/alkt-processintegration.md).

### Onboarding a new process

How a new line of business gets its errands driven by a process. There are two cases, and they differ in what they
cost:

|                                 Case                                 |                                     What it takes                                      | Release of SupportManagement? |
|----------------------------------------------------------------------|----------------------------------------------------------------------------------------|-------------------------------|
| A new process in a process service that is already known (`pw-alkt`) | Configuration in SupportManagement, and the process deployed in the process service    | No                            |
| A new process service                                                | Code and configuration in SupportManagement, a new API in WSO2, and the service itself | Yes                           |

Every environment needs `integration.pw-alkt.url` and the OAuth2 client `pw-alkt` (`token-uri`, `client-id` and
`client-secret`) whether or not any namespace runs a process: the service does not start without them.

#### A new process in a known process service

1. **Model and deploy the process** in the process service. Its process definition key is what SupportManagement
   calls the process key, and it is matched exactly. Follow the modelling rules in §9.2 of the
   [design document](docs/alkt-processintegration.md): a wait
   state reads the errand again when it is entered, a manual gate is a named message event (never a user task), no
   parallel branches change the errand, and the last step before every end event is a work step reporting the process
   as completed. SupportManagement does not check that a key is deployed; the process service answers `422` for one it
   does not know, and the event is then dropped with an `ERROR` entry on the errand.
2. **Connect the namespace** in its namespace config (`POST` or `PUT /{municipalityId}/{namespace}/namespace-config`):
   - `processConsumer`: `pw-alkt`;
   - `processTriggers`: at least `ERRAND` and `DECISION`, plus every other sub type the process should be woken by, such
     as `MESSAGE` or `ATTACHMENT`;
   - `accessControl`: `false`, since a process consumer cannot be combined with access control.

   See [Turning it on](#turning-it-on).

3. **Point the labels at the process.** Give the label, or labels, of the errands the process is to run the attribute
   `processKey` with the process definition key, and `processStartMode` `MANUAL` to begin with
   (`PUT /{municipalityId}/{namespace}/metadata/labels`). An errand's labels may name one process key only. See
   [Which process an errand belongs to](#which-process-an-errand-belongs-to) and [Label rules](#label-rules).

4. **Try it by hand.** Create an errand wearing the label. `GET .../errands/{errandId}/processes` answers
   `startable.status` `AVAILABLE` with the key, `POST .../processes/start` starts the process, and within seconds the
   instance shows in the list and its entries in `GET .../process-activities`. See
   [Starting a process by hand](#starting-a-process-by-hand).

5. **Switch to automatic start** by setting `processStartMode` to `AUTOMATIC`, or removing it, once the chain behaves.
   From then on every errand event that passes the triggers starts the process for an errand that has neither a live
   nor a completed process — and that includes **existing errands already wearing the label, the next time they
   change**. Switching back is the same attribute change; neither needs a release.

Whether events are getting through is visible in the health indicator `process_event_relay`, which turns unhealthy
when the oldest undelivered event is older than `scheduler.process-event.unhealthy-after`.

#### A new process service

The relay delivers only to `pw-alkt`, through one Feign client. A process service of its own is therefore a release of
SupportManagement:

|                                              Step                                              |           Where            |
|------------------------------------------------------------------------------------------------|----------------------------|
| An OAuth2 client registration and provider for the service                                     | `application.yml`          |
| A Feign client with its configuration and properties, like `PwAlktClient`                      | code and `application.yml` |
| The relay delivering to the new consumer, and the validation of `processConsumer` accepting it | code                       |
| An API for the service in WSO2, which all REST traffic between the services passes             | WSO2                       |

After that the namespace is connected exactly as above, with the new consumer's name in `processConsumer`.

The process service in turn has to keep the contract `pw-alkt` keeps:

- **Take events** at `POST /{municipalityId}/{namespace}/process/errand-events` (see
  [`pw-alkt-api.yaml`](src/main/resources/integrations/pw-alkt-api.yaml)). Answer `202` when the event is taken, and
  `422` only for an event it can never take, such as an unknown process key — that event is dropped for good, while
  every other answer is retried until `scheduler.process-event.max-age`. The same event can arrive twice.
- **Start a process** only when the errand has no live instance, the event carries a `processKey` and `startAllowed`
  is `true`, with the errand id as business key. Register the start with `POST .../errands/{errandId}/processes`, a
  failed one included, and abort the new instance if the answer is `409`.
- **Wake the process** on every other event: correlate the message `errandUpdated`, or `signalName` when the sub type
  is `SIGNAL`. A correlation that finds no wait state is normal and answered `202`.
- **Delete the instance** when the event type is `DELETE`.
- **Report** the state of the instance from every work step with `PUT .../processes/{processInstanceId}`: the status,
  the current activity, the activities performed, `awaitingSignals` for the gate it waits at, and `errandVersion` for
  the version of the errand the step read. A step that changes the errand sends `If-Match` with that version. A `412`
  means the errand changed since; the step is retried against the errand as it now is.
- **Identify itself** in every call with `X-Sent-By: <consumer>; type=processEngine` and `processService` set to the
  consumer name, and send `X-Trigger-Process: false` on its writes, so that they do not wake the process again.

### Turning it on

Two namespace config values decide it:

|       Field       |     Config key     |                                                                          Meaning                                                                           |
|-------------------|--------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `processConsumer` | `PROCESS_CONSUMER` | The process engine running the namespace's processes, and the address its events go to. The only one known is `pw-alkt`. Leaving it out means no processes |
| `processTriggers` | `PROCESS_TRIGGER`  | The errand event sub types worth waking the process for                                                                                                    |

A process consumer **cannot be combined with access control**. The access mapper only grants access to AD accounts, so
every read and write the process makes would be denied. A namespace config with both is refused with `400`.

A namespace with a process consumer **must list `ERRAND` and `DECISION` among its triggers**, or the config is refused
with `400`. Without `ERRAND` an errand given its process label after it was created never starts its process, and
without `DECISION` a process waiting for its decision is never told that it has been made. The commands `PROCESS` and
`SIGNAL` may never be listed: they always reach the process, and listing them would suggest otherwise. A config stored
before these rules is not checked until it is written again.

### Which process an errand belongs to

The answer is read from the errand's **own** labels, through two label attributes:

|     Attribute      |             Values              |                                          Meaning                                           |
|--------------------|---------------------------------|--------------------------------------------------------------------------------------------|
| `processKey`       | the key of a process            | Which process the errand runs                                                              |
| `processStartMode` | `AUTOMATIC` (default), `MANUAL` | Whether SupportManagement starts the process itself. Read from the label that gave the key |

The label tree is not walked, so moving or renaming a label leaves the answer alone. Deprecated labels are not read.

A label write (`POST` or `PUT` of `/metadata/labels`) is refused with `400` when `processStartMode` is anything but
exactly `AUTOMATIC` or `MANUAL`, when it stands on a label without `processKey`, or when an attribute key is spelled
like `processKey` or `processStartMode` in any other way (another case, surrounding blanks).

| The labels resolve to |                          Result                          |
|-----------------------|----------------------------------------------------------|
| Exactly one key       | That process                                             |
| No key                | No process. Not an error                                 |
| Two or more keys      | No automatic start, and an `ERROR` entry naming the keys |

SupportManagement does not check that a key names a process that is actually deployed — only the process engine knows,
and it answers `422` for one it does not recognise.

### Label rules

Labels can silently stop answering which process an errand belongs to: two labels naming two processes resolve to
neither, and a key exchanged for another leaves the running process with nothing to wake it. Neither fails anywhere —
the errand just stops moving. Two rules therefore hold whenever labels are written:

1. **An errand's labels may name at most one `processKey`.** This holds for every errand, with or without a process.
2. **Once an errand has a process — live or completed — a label change may not move it to another.** What the labels
   resolve to after the change must be what they resolved to before. A change that leaves them naming the process the
   errand actually runs is always allowed, which is the way back for an errand whose label has lost its key.

Only the key is held still: `processStartMode` may be changed freely, even on an errand with a process.

|                      Writer                       |  Rules  |                              When refused                               |
|---------------------------------------------------|---------|-------------------------------------------------------------------------|
| `POST /errands` (also handover and e-mail intake) | 1       | `400` — a new errand has no process yet                                 |
| `PATCH /errands/{errandId}`                       | 1 and 2 | `400`                                                                   |
| The `ADD_LABEL` action (scheduled)                | 1 and 2 | The labels are not added, and an `ERROR` entry is written on the errand |

The rules are checked against the labels the errand would actually wear, including the ancestors added to them.

An errand can still end up naming two processes if `processKey` is added to a label the errand already wears, since no
write to the errand happens. Such an errand gets `400` on every label change until one of the labels is removed — which
is always allowed, as it resolves the errand to a single key.

### How events reach the process

- Every errand event that matches the triggers becomes a row in an outbox, **in the same transaction** as the change.
  If the row cannot be written, the change is rolled back.
- A relay delivers the rows: right after the commit, and on the schedule in `scheduler.process-event` for whatever the
  direct run did not reach.
- `422` from the process engine is permanent — the row is consumed, a live instance is marked `FAILED` and an `ERROR`
  entry is written. Anything else is retried until the row passes `scheduler.process-event.max-age`.
- The process engine sets `X-Trigger-Process: false` on its own writes, so that they do not wake it again. The header is
  **ignored for AD accounts**, so a handler's change always reaches the process.
- An emergency brake stops publishing for an errand once `process-engine.loop-guard.max-events-per-errand` events (20
  by default) have been delivered to its process within `window` (10 minutes). Further events are dropped and an
  `ERROR` entry is written. Only delivered events are counted, so a delivery outage never trips it.
- A deletion passes the trigger filter, the header and the brake alike: it cannot loop, and holding it back would leave
  the process running for an errand that no longer exists.
- So does a command, a handler's start or signal: it is a person pressing a button rather than something that
  happened to the errand.
- Every event carries `startAllowed`, which says whether the process engine may start a process for it. It is set for
  a start command, and otherwise only when the errand has no live and no completed process and the label naming the
  key of the event has `processStartMode` `AUTOMATIC`.
- A decision concluded by an AD account passes the brake, and nothing else. It is the event a waiting process needs,
  and a person is no loop. A decision the process concludes itself is held to the brake like any other write of the
  process.
- A scheduled action that changes the errand, such as `ADD_LABEL`, is recorded as a revision and an errand event
  without a notification, and reaches the process like any other change.

### Endpoints

All under `/{municipalityId}/{namespace}/errands/{errandId}`:

| Method |                   Path                   |                                       Purpose                                        |
|--------|------------------------------------------|--------------------------------------------------------------------------------------|
| `PUT`  | `/processes/{processInstanceId}`         | The process engine reports the state of an instance                                  |
| `POST` | `/processes`                             | The process engine registers a start, including one that failed                      |
| `GET`  | `/processes`                             | Every process the errand has had, newest first, and whether a new one may be started |
| `POST` | `/processes/start`                       | A handler starts the handling of the errand                                          |
| `POST` | `/processes/{processInstanceId}/signals` | A handler steps the process past the gate it waits at                                |
| `GET`  | `/process-activities`                    | The activity log of the errand, optionally narrowed to one instance                  |

The errand itself carries the latest process in `errand.process`.

### Starting a process by hand

`GET .../processes` answers with `startable` beside the list, which is what a start button is lit, dimmed and
explained by:

| `startable.status`  |                                Meaning                                |
|---------------------|-----------------------------------------------------------------------|
| `AVAILABLE`         | A process may be started now, with one of the keys in `processKeys`   |
| `LIVE_INSTANCE`     | A process is already running for the errand                           |
| `PROCESS_COMPLETED` | A process has run to its end. A new process means a new errand        |
| `NO_PROCESS_KEY`    | No label of the errand names a process the errand can be started with |
| `NO_PROCESS_ENGINE` | The namespace runs no processes                                       |

`processKeys` is empty whenever the status is not `AVAILABLE`, and holds two or more keys when the labels point at
different processes. Once an errand has had a process, a start that failed included, only the key of that process is
offered. The start mode of the labels does not affect the answer.

A handler starts the process with `POST .../processes/start`, and `{ "processKey": "<key>" }` when more than one key is
offered — the body may be left out otherwise:

| Code  |                                                                       When                                                                       |
|-------|--------------------------------------------------------------------------------------------------------------------------------------------------|
| `202` | The start is recorded and handed on to the process                                                                                               |
| `400` | No label names a process to start, several do and the request names none, the key is not among those offered, or the namespace runs no processes |
| `403` | The caller is not an AD account                                                                                                                  |
| `404` | The errand does not exist in the namespace                                                                                                       |
| `409` | The errand has a live process, its process has run to its end, or a start of another process is already on its way                               |

- An accepted start writes an entry of type `START` to the activity log, naming the sender and belonging to no process
  instance, and an errand event with the sub type `PROCESS` carrying the chosen key and `startAllowed`. It changes
  nothing on the errand and **sends no notification**.
- The command works in both start modes. In `AUTOMATIC` mode it is the way a start that failed is tried again.
- A start sent again while an earlier one with the same key is still undelivered writes nothing and is answered `202`.
- The process shows in `GET .../processes` once the process engine has registered it; until then `startable` still
  says `AVAILABLE`, and the user interface should show the start as on its way.

### Stepping a process by hand

Whether a step waits for a person is decided in the process model, not in SupportManagement. A gate that waits for a
named message is a manual one, and the process engine reports the names it waits for in `awaitingSignals` of its
report — a name and a display label each. SupportManagement stores them without interpreting them, replaces them as a
whole with every report, and shows them on `errand.process`. An empty list means the process waits for no person, and
a process that has ended always waits for no one.

A handler answers with `POST .../processes/{processInstanceId}/signals` and `{ "signal": "<name>" }`:

| Code  |                                           When                                           |
|-------|------------------------------------------------------------------------------------------|
| `202` | The signal is recorded and handed on to the process                                      |
| `400` | `signal` is missing or blank, or the namespace has no process consumer                   |
| `403` | The caller is not an AD account                                                          |
| `404` | The errand does not exist, or has no such process instance                               |
| `409` | The process does not wait for the signal right now, or has ended — read the errand again |

- The name has to match one of `awaitingSignals` **exactly**, case included, since that is what the process engine
  correlates on. Names are stored and compared exactly too, so names differing only in case are two signals. A button
  pressed after the process has reported that it moved on is a `409`, not a second step.
- **Sending a signal consumes nothing.** Until the process reports where it went, the same signal is accepted again: a
  double click writes two entries and two events, and the process engine correlates the first and ignores the second.
  The user interface should therefore not offer a button again once it has been pressed, until the errand is read anew.
- An accepted signal writes an entry of type `SIGNAL` to the activity log, naming the sender, and an errand event with
  the sub type `SIGNAL`, which carries the name to the process in `signalName`. It changes nothing on the errand,
  moves no version and **sends no notification**.
- Only AD accounts may send one: the entry it leaves has to say which person stepped the process on, and a service
  name answers nothing. Publication does not depend on the check — commands pass the loop guard of their own accord.
- The signal carries a name and nothing else. A reason for the step belongs in a note on the errand, which is kept as
  long as the errand, while the activity log is swept after a year.

### The decision

The decision is the ordinary `/decisions` resource of the errand, the same for errands with and without a process. What
a process adds:

- **Creating, changing and deleting a decision** is logged as an errand event with the sub type `DECISION` and moves
  the version of the errand, so that a work step holding an older `ETag` gets `412`. Its terms, attachment links and
  JSON parameters do neither.
- **Who may claim which method:** `MANUAL` is written by an AD account, and `AUTOMATIC` by a caller that is not one. In
  a namespace with a process consumer, `AUTOMATIC` is written only by that consumer, recognised by the value of
  `X-Sent-By`. Anything else is `403`. The process row that made an automatic
  decision is set by SupportManagement in `errandProcessId`, and is never taken from the request.
- **When a decision is locked**, and only on an errand that has a process:
  - once the process has run to its end (`COMPLETED`), no decision of the errand can be created, changed or deleted;
  - once a decision is `COMPLETED`, it can no longer be changed or deleted, nor can its terms or attachment links;
  - an attachment of the errand linked to a locked decision cannot be deleted, nor can an investigation a locked
    decision rests on.

  Every one of these is answered with `409`. The JSON parameters of a decision are never locked, since legal force and
  service of the decision are known only after it is made. An errand without a process is never locked. A decision
  that has to be corrected once it is locked is corrected in a new errand, referred from the first.

- **The justification** nearly always holds personal data, and is masked in the payload log (`logbook.body-filters`).

## Contributing

Contributions are welcome! Please
see [CONTRIBUTING.md](https://github.com/Sundsvallskommun/.github/blob/main/.github/CONTRIBUTING.md) for guidelines.

## License

This project is licensed under the [MIT License](LICENSE).

## Code status

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-support-management&metric=alert_status)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-support-management)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-support-management&metric=reliability_rating)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-support-management)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-support-management&metric=security_rating)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-support-management)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-support-management&metric=sqale_rating)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-support-management)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-support-management&metric=vulnerabilities)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-support-management)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-support-management&metric=bugs)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-support-management)

---

Copyright (c) 2026 Sundsvalls kommun
