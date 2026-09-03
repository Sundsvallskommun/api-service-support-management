# Measures

A measure belongs to an errand. Its `type` references `MeasureType.name` within the errand's municipality and namespace. The catalogue owns the display name, group, order and deprecation state.

## Catalogue changes

- Treat `name` as an immutable key. Rename the displayed label through `displayName`. Attempting to change the key returns `409 Conflict`.
- A referenced type cannot be deleted (`409 Conflict`). Deprecate it instead. Unused types can still be deleted.
- Deprecated types cannot be selected for new measures or when changing an existing measure's type. An existing measure may retain its historical type and remain editable.
- Catalogue validation and metadata writes lock the same type row for the transaction, so a type cannot be deleted between validation and saving a new reference.
- `measureGroup` categorizes types. It does not implicitly identify a role or grant permission to select a type.

## Creating and updating

`POST /{municipalityId}/{namespace}/errands/{errandId}/measures` requires `type`, `addedByUser` and `addedByRole`. Its OpenAPI input is `CreateMeasureRequest`; partial updates use `Measure`.

`accept` is one of the strings `TRUE`, `FALSE`, `REWORK`, or null. It is a decision field, not a complete workflow state machine.

For PATCH, an omitted field keeps its current value. Explicit null clears nullable fields, including planned dates, `executed`, `accept`, goal and description. `type` cannot be cleared. The same semantics apply when measures are sent within an errand update.

`addedByUser` and `addedByRole` cannot be changed after creation. In namespaces with access control enabled, creation also requires `addedByUser` to match the requesting AD account and `addedByRole` to be one of that user's roles from Access Mapper. Namespaces without access control retain their existing trusted-client policy; these fields must never be used as authorization grants.

## Concurrency and access

Before POST, PATCH or DELETE on measures, read the parent errand and send its current ETag in `If-Match`. A successful write changes the parent version, so read it again before the next write.

- Missing or blank `If-Match`: `428 Precondition Required`.
- Stale, weak or wildcard ETag: `412 Precondition Failed`.
- The generic errand PATCH also requires a version and measure write access whenever `measures` is supplied, including an empty list.

This strengthens the existing API contract: clients previously omitting `If-Match` must start sending it. No database migration is required. Rolling back the code restores the previous API behavior without reversing stored data.

## Validation

The reference and attribution rules live in `MeasureValidator`, used before mutation by both write paths. `MetadataService` owns catalogue changes. `AccessControlService` owns authorization, and `ErrandMeasureMapper` owns applying supplied fields.

Run unit tests with `mvn test`. Run the HTTP/database and OpenAPI checks with `mvn -Dit.test=ErrandMeasuresIT,MetadataMeasureTypeIT,OpenApiSpecificationIT integration-test failsafe:verify`.

Tests cover preserving historical references, rejecting new deprecated selections, scoped deletion, immutable keys and creator fields, explicit-null clearing, stale writes, and the generated OpenAPI contract.
