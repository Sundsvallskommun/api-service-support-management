-- -----------------------------------------------------------------------------------------------
-- A label under the one naming the application process, on top of testdata-it.sql and
-- testdata-process-key-guard.sql, worn as the access label of both the errand running that process
-- and the errand that has never had one, so that moving it under the label naming the supervision
-- process is asked of both.
--
-- Kept apart from testdata-process-key-guard.sql: the label would show on those errands in every other
-- test that loads that script.
-- -----------------------------------------------------------------------------------------------

INSERT INTO metadata_label (created, modified, municipality_id, namespace, classification, display_name, id, parent_id,
                            resource_name, resource_path, deprecated)
VALUES ('2026-01-01 10:00:00.000', NULL, '2281', 'PROCESS-NAMESPACE', 'TYPE', 'Forstagangsansokan',
        'bb000000-0000-0000-0000-0000000000b6', 'bb000000-0000-0000-0000-0000000000b1', 'FORSTAGANG', 'ANSOKAN/FORSTAGANG', false);

INSERT INTO errand_labels(errand_id, metadata_label_id)
VALUES ('aa000000-0000-0000-0000-0000000000a1', 'bb000000-0000-0000-0000-0000000000b6'),
       ('aa000000-0000-0000-0000-0000000000a2', 'bb000000-0000-0000-0000-0000000000b6');

INSERT INTO errand_access_labels(errand_id, metadata_label_id)
VALUES ('aa000000-0000-0000-0000-0000000000a1', 'bb000000-0000-0000-0000-0000000000b6'),
       ('aa000000-0000-0000-0000-0000000000a2', 'bb000000-0000-0000-0000-0000000000b6');
