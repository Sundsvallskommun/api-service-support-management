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
   required level, or the caller is the reporter and `reporterAccess` grants the resource at that level.
4. **Layer C** — on the way out the payload is trimmed: a limited errand uses `limitedReadAccess.fields` whatever the
   `roleBasedMapping` flag says, and otherwise the matched roles' `roleFieldRestrictions` apply if that flag is on.
   Reporter fields union on top of whichever restriction applied, and a caller no restriction applied to receives the
   full errand.

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
