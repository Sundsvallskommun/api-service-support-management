-- -----------------------------------------------------------------------------------------------
-- A label blocking processes on the errand a run of testdata-it-purge-process.sql reaches, on top of
-- that script, so that the deletion the run publishes is held back.
-- -----------------------------------------------------------------------------------------------

INSERT INTO metadata_label (created, modified, municipality_id, namespace, classification, display_name, id, parent_id,
                            resource_name, resource_path, deprecated)
VALUES ('2026-01-01 10:00:00.000', NULL, '2281', 'PURGE-NAMESPACE', 'CATEGORY', 'Sparrad',
        'cb000000-0000-0000-0000-0000000000c4', NULL, 'SPARRAD', 'SPARRAD', false);

INSERT INTO metadata_label_attribute (metadata_label_id, `key`, `value`)
VALUES ('cb000000-0000-0000-0000-0000000000c4', 'processBlocked', 'true');

INSERT INTO errand_labels(errand_id, metadata_label_id)
VALUES ('aaaa1111-0000-0000-0000-000000000001', 'cb000000-0000-0000-0000-0000000000c4');
