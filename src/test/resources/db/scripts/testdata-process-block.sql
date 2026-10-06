-- -----------------------------------------------------------------------------------------------
-- A label blocking processes in the process namespace, on top of testdata-it.sql and
-- testdata-process-loop-guard.sql, for the tests of the block.
--
-- Kept apart from the shared test data: a blocking label on the process errands would hold back
-- every event the other process tests expect to see published.
-- -----------------------------------------------------------------------------------------------

-- Attachments and decisions wake the process as well as a changed errand does, so that what holds
-- their events back is the block and not the triggers
INSERT INTO namespace_config_value(namespace_config_id, `key`, `value`, `type`)
VALUES (8, 'PROCESS_TRIGGER', 'ATTACHMENT', 'STRING'),
       (8, 'PROCESS_TRIGGER', 'DECISION', 'STRING');

INSERT INTO decision_outcome(id, name, display_name, sort_order, deprecated, namespace, municipality_id, created)
VALUES ('cb000000-0000-0000-0000-0000000000f1', 'APPROVAL', 'Bifall', 1, false, 'PROCESS-NAMESPACE', '2281',
        '2026-01-01 10:00:00.000');

-- The blocking label, and a label under it that says nothing about a block itself
INSERT INTO metadata_label (created, modified, municipality_id, namespace, classification, display_name, id, parent_id,
                            resource_name, resource_path, deprecated)
VALUES ('2026-01-01 10:00:00.000', NULL, '2281', 'PROCESS-NAMESPACE', 'CATEGORY', 'Sparrad',
        'cb000000-0000-0000-0000-0000000000c1', NULL, 'SPARRAD', 'SPARRAD', false),
       ('2026-01-01 10:00:00.000', NULL, '2281', 'PROCESS-NAMESPACE', 'TYPE', 'Manuell hantering',
        'cb000000-0000-0000-0000-0000000000c2', 'cb000000-0000-0000-0000-0000000000c1', 'MANUELL', 'SPARRAD/MANUELL', false);

INSERT INTO metadata_label_attribute (metadata_label_id, `key`, `value`)
VALUES ('cb000000-0000-0000-0000-0000000000c1', 'processBlocked', 'true');

-- The errand running a process and the errand that has never had one both wear the application label and the label
-- under the blocking one, the blocking one among them since labels are stored with their ancestors
INSERT INTO errand_labels(errand_id, metadata_label_id)
VALUES ('aa000000-0000-0000-0000-0000000000a1', 'dd000000-0000-0000-0000-0000000000d2'),
       ('aa000000-0000-0000-0000-0000000000a1', 'cb000000-0000-0000-0000-0000000000c1'),
       ('aa000000-0000-0000-0000-0000000000a1', 'cb000000-0000-0000-0000-0000000000c2'),
       ('aa000000-0000-0000-0000-0000000000a2', 'dd000000-0000-0000-0000-0000000000d2'),
       ('aa000000-0000-0000-0000-0000000000a2', 'cb000000-0000-0000-0000-0000000000c1'),
       ('aa000000-0000-0000-0000-0000000000a2', 'cb000000-0000-0000-0000-0000000000c2');

INSERT INTO errand_access_labels(errand_id, metadata_label_id)
VALUES ('aa000000-0000-0000-0000-0000000000a1', 'dd000000-0000-0000-0000-0000000000d2'),
       ('aa000000-0000-0000-0000-0000000000a1', 'cb000000-0000-0000-0000-0000000000c2'),
       ('aa000000-0000-0000-0000-0000000000a2', 'dd000000-0000-0000-0000-0000000000d2'),
       ('aa000000-0000-0000-0000-0000000000a2', 'cb000000-0000-0000-0000-0000000000c2');

-- The gate the process of the errand running one waits at
INSERT INTO errand_process_signal(sort_order, created, errand_process_id, id, name, label)
VALUES (0, '2026-01-01 11:00:00.000', 'ep-it-live', 'cb000000-0000-0000-0000-0000000000e1', 'granskning-godkand',
        'Godkänn granskning');
