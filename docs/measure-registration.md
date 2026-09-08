# Registering measures by namespace role

This change starts from `avvikelse-sprint` (45cd25628 / API 16.0). It replaces
Draken's temporary all-roles demo policy with an API-owned contract. No application,
build, type check or test has been run during implementation at the user's request.

## Ownership and contract

Draken can filter roles itself if it has their authoritative mapping to the current
user. The new endpoint is a choice to reuse Support Management's existing
AccessMapper interpretation for both selection and write validation, rather than
maintain another mapping in the client. UI filtering alone does not validate a
submitted registration. Previously, the shared measure validator checked that the
type and role existed in the namespace; it did not check the user's membership or
an explicit assignment between that role and type.

- `AccessMapperService.getAccessSnapshot` remains the owner of actual role grants.
  `AccessControlService` intersects them with active role metadata, preserving the
  metadata role's exact `name`, `id` and `displayName`. Resource/errand permissions
  remain separate from role membership. Existing AccessMapper caching still applies.
- `GET /{municipalityId}/{namespace}/users/me/roles` returns the current user's
  active roles. It requires the trusted request identifier to be an AD account.
  It accepts no target username. No grants returns `[]`; a missing AD identifier
  returns `401`; upstream failures remain failures. The HTTP response is not cached.
- `MeasureType.allowedRoleIds` is a set of explicit `Role.id` references within the
  same municipality and namespace. A type can be shared by several roles. An empty
  set grants no roles. `measureGroup` remains descriptive grouping, not authorization.
- `MeasureValidator` enforces the same rules for dedicated measure writes and for
  measures embedded in errand POST/PATCH. New registrations require an active type
  assigned to the selected active role, an actual grant for that role and the
  existing resource write permission. Attribution checks apply even if namespace
  access filtering or role-based field mapping is disabled.
- `addedByRole` is the exact namespace `Role.name`. The API sets `addedByUser` from
  the request's AD identity. If a client still supplies `addedByUser`, it must match
  that identity; this covers existing clients already sending their own username.
  Both attribution fields are immutable after creation. An unchanged historical
  type/role remains editable; a type change must satisfy the current type assignment.
- Embedded errand updates also require MEASURE write access, including `measures: []`.
  Existing measure IDs must belong to that errand and may not be repeated.
- Metadata role names are immutable keys; presentation changes use `displayName`.
  Deleting a role referenced by a type or saved measure returns `409`. Deleting a
  type referenced by a saved measure also returns `409`; deprecate it instead.
- PATCH flushes and refreshes the measure before returning it, so the response ETag,
  version and derived type label reflect the saved change.

The request identifier follows the API's existing trusted caller model. An AD name
in a header is not a separate authentication mechanism; callers/gateways must only
forward the authenticated user's identity.

## Deployment and configuration order

1. Deploy this API change and the additive migration
   `V1_58__add_measure_type_allowed_roles.sql`. Existing measures are unchanged.
   The migration grants nothing automatically and does not reinterpret old group names.
2. Configure the intended exact namespace role names in AccessMapper for users.
   Confirm `GET /2281/{namespace}/users/me/roles` with the user's trusted identifier
   header, `X-Sent-By: username; type=adAccount` (`Identifier.HEADER_NAME` in Dept44).
   Draken's existing API transport supplies this header from the logged-in user.
3. Read role and measure-type metadata, then assign the intended **role UUIDs** to
   each type through `PATCH /2281/{namespace}/metadata/measuretypes/{typeId}`.
   Use the metadata resource path documented by this API (`measuretypes`). Example:

   ```json
   {
     "allowedRoleIds": ["cc000000-0000-0000-0000-000000000100"]
   }
   ```

   Only the list needs to be sent. Omitted/null leaves assignments unchanged;
   `[]` removes all assignments. Unknown or foreign-scope role IDs return `400`.
   This is metadata administration and uses the existing metadata write permission.
4. Deploy the accompanying Draken changes. Draken reads the role endpoint, asks for
   a role before type/details and filters types by that role's ID. There is no demo
   fallback, AD-to-role mapping or extra feature flag. Types without assignments
   cannot be registered. The previous API deployment does not implement this contract.

New registration needs these fields; creator attribution is intentionally omitted:

```json
{
  "measureTypeId": "dd000000-0000-0000-0000-000000000100",
  "addedByRole": "ROLE-1",
  "description": "Describe the measure",
  "goal": "Describe the intended result"
}
```

Existing clients registering measures need a valid AD identifier, a granted role and
configured type assignments before switching to this API version. No production
configuration, AccessMapper grants or remote data were changed during implementation.

## Verification and recovery

Source tests cover current-user role intersection, missing identity, ungranted roles,
creator spoofing, type/role assignments, historical edits, immutable attribution,
embedded-write permission and metadata reference integrity. Draken tests cover the
role prerequisite, filtering, role changes preserving draft text and protected writes.
The checked-in OpenAPI snapshot was edited alongside the code, not regenerated by
starting the application. It must be compared by `OpenApiSpecificationIT` in the
normal validation environment.

Suggested scoped checks for the maintainer/CI (not executed here):

```sh
mvn -Dtest=CurrentUserRolesResourceTest,MeasureValidatorTest,AccessControlServiceTest,MetadataServiceTest,ErrandMeasureServiceTest,ErrandServiceTest test
```

Also run the existing `ErrandMeasuresIT`, metadata integration tests and
`OpenApiSpecificationIT` in the project's integration-test environment. Check the
role/type flow with the intended real AccessMapper grants before enabling it for users.

Rollback is coordinated: revert the Draken/API application changes together, leaving
this additive table in place. Do not drop populated assignments as an application
rollback step. Draken's existing `useMeasures` flag can hide the tab while deployment
or metadata configuration is completed. Saved measures retain their creator and role.
