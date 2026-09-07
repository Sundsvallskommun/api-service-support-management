# Measures

A measure belongs to an errand. Its `type` references `MeasureType.name` within the errand's municipality and namespace. The catalogue owns the display name, group, order and deprecation state.

## Catalogue changes

- Treat `name` as an immutable key. Rename the displayed label through `displayName`. A PATCH may omit `name` or repeat the current one; sending another name returns `409 Conflict`. Creation uses `CreateMeasureTypeRequest`, where `name` and `measureGroup` are required.
- A referenced type cannot be deleted (`409 Conflict`). Deprecate it instead. Unused types can still be deleted.
- Deprecated types cannot be selected for new measures or when changing an existing measure's type. An existing measure may retain its historical type and remain editable.
- Measure validation reads the type under a shared lock and catalogue writes take an exclusive lock on the same row, both held for the transaction. A type therefore cannot be deleted between validation and the commit of a new reference, while measure writes referring to the same type do not block each other.
- `measureGroup` categorizes types. It does not implicitly identify a role or grant permission to select a type.

## Creating and updating

`POST /{municipalityId}/{namespace}/errands/{errandId}/measures` requires `type`, `addedByUser` and `addedByRole`. Its OpenAPI input is `CreateMeasureRequest`; partial updates use `Measure`.

`accept` is one of the strings `TRUE`, `FALSE`, `REWORK`, or null. It is a decision field, not a complete workflow state machine.

For PATCH, an omitted field keeps its current value. Explicit null clears nullable fields, including planned dates, `executed`, `accept`, goal and description. `type` cannot be cleared. The same semantics apply when measures are sent within an errand update.

`addedByUser` and `addedByRole` cannot be changed after creation. In namespaces with access control enabled, creation also requires `addedByUser` to match the requesting AD account and `addedByRole` to be one of that user's roles from Access Mapper. This applies to every path that creates a measure, including `POST /errands` with `measures`. Namespaces without access control retain their existing trusted-client policy; these fields must never be used as authorization grants.

## Concurrency and access

Each measure has its own database version, exposed as a read-only `version` property. POST returns the new measure's ETag together with its Location. GET and PATCH of a single measure return its current ETag; PATCH flushes the update before building the response so that the returned version can be used for the next write.

For PATCH or DELETE of a single measure, send that measure's ETag in `If-Match` to protect against stale edits. This header is optional, matching parameters and JSON parameters:

- Omitting `If-Match` skips the client version check.
- A stale or weak ETag returns `412 Precondition Failed`.
- `If-Match: *` accepts an existing measure without checking a specific version.
- POST creates a new measure without requiring an ETag from the parent errand.

Creating, changing or deleting a measure also increments the parent errand's version in the same transaction. Other errand fields and other measures do not invalidate this measure's ETag. The existing optimistic lock on the parent remains, so transactions that overlap in time can still conflict (`412 Precondition Failed`, from the shared optimistic-locking handler).

The generic errand PATCH continues to use the errand's optional `If-Match`, including when replacing the measure list. Clients should send it to protect the whole list against lost additions or removals. Whenever `measures` is supplied, including an empty list, the endpoint also checks measure write access. A client that reads an errand, changes an unrelated field and patches the whole representation back carries `measures` along and therefore needs measure write access; a client without it must leave `measures` out. Updates through this path increment the versions of changed measures; the mapper never copies a version supplied in the request into the database.

## Migration and client compatibility

Migration `V1_56__add_measure_version.sql` adds `measure.version` as a non-null bigint with default 0. Existing measures start at version 0 and retain their IDs and content. A rollback of the application behavior should retain the column and the applied migration in the release history.

Before deploying, check for measures whose `type` no longer exists in the catalogue. They predate the reference check and cannot be edited through the errand PATCH, where `type` is required, until the type is recreated or the measure is corrected:

```sql
SELECT m.id, m.type, e.namespace, e.municipality_id
FROM measure m
JOIN errand e ON e.id = m.errand_id
LEFT JOIN measure_type t ON t.name = m.type AND t.namespace = e.namespace AND t.municipality_id = e.municipality_id
WHERE t.id IS NULL;
```

Clients can continue omitting `If-Match`. Clients opting into concurrency protection use the single measure's ETag for its PATCH and DELETE endpoints, and the errand's ETag for the generic errand PATCH. Nullable fields sent as null are cleared; omit fields that should retain their current values.

## Validation

The reference and attribution rules live in `MeasureValidator`, used before mutation by both write paths. `MetadataService` owns catalogue changes. `AccessControlService` owns authorization, and `ErrandMeasureMapper` owns applying supplied fields.

Run unit tests with `mvn test`. Run the HTTP/database and OpenAPI checks with `mvn -Dit.test=ErrandMeasuresIT,ErrandsIT,ErrandTimeMeasurementsIT,MetadataMeasureTypeIT,OpenApiSpecificationIT integration-test failsafe:verify`.

Tests cover preserving historical references, rejecting new deprecated selections, scoped deletion, immutable keys and creator fields, explicit-null clearing, stale measure and parent writes, optional preconditions, independent measure versions, parent propagation, and the generated OpenAPI contract. `MeasureEntity` owns the version; the existing `ETagUtil` owns header matching.
