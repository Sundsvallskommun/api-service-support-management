-- -----------------------------------------------------------------------------------------------
-- A label blocking processes on the errand the email intake of testdata-process-event.sql writes to,
-- on top of that script, for the test of an incoming message on a blocked errand.
-- -----------------------------------------------------------------------------------------------

INSERT INTO metadata_label (created, modified, municipality_id, namespace, classification, display_name, id, parent_id,
                            resource_name, resource_path, deprecated)
VALUES ('2026-01-01 10:00:00.000', NULL, '2281', 'NAMESPACE-1', 'CATEGORY', 'Sparrad',
        'cb000000-0000-0000-0000-0000000000c3', NULL, 'SPARRAD', 'SPARRAD', false);

INSERT INTO metadata_label_attribute (metadata_label_id, `key`, `value`)
VALUES ('cb000000-0000-0000-0000-0000000000c3', 'processBlocked', 'true');

INSERT INTO errand_labels(errand_id, metadata_label_id)
VALUES ('ec677eb3-604c-4935-bff7-f8f0b500c8f4', 'cb000000-0000-0000-0000-0000000000c3');
