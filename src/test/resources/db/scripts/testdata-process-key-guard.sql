-- -----------------------------------------------------------------------------------------------
-- Labels naming a process, on top of testdata-it.sql, for the guard that keeps an errand from being
-- relabelled into another process.
--
-- Kept apart from the shared test data: a label carrying a processKey on the errands of
-- PROCESS-NAMESPACE would change what every other test of that namespace publishes and resolves.
-- -----------------------------------------------------------------------------------------------

-- Two labels naming different processes, and one naming none at all
INSERT INTO metadata_label (created, modified, municipality_id, namespace, classification, display_name, id, parent_id,
                            resource_name, resource_path, deprecated)
VALUES ('2026-01-01 10:00:00.000', NULL, '2281', 'PROCESS-NAMESPACE', 'CATEGORY', 'Ansokan',
        'bb000000-0000-0000-0000-0000000000b1', NULL, 'ANSOKAN', 'ANSOKAN', false),
       ('2026-01-01 10:00:00.000', NULL, '2281', 'PROCESS-NAMESPACE', 'CATEGORY', 'Tillsyn',
        'bb000000-0000-0000-0000-0000000000b2', NULL, 'TILLSYN', 'TILLSYN', false),
       ('2026-01-01 10:00:00.000', NULL, '2281', 'PROCESS-NAMESPACE', 'CATEGORY', 'Bradskande',
        'bb000000-0000-0000-0000-0000000000b3', NULL, 'BRADSKANDE', 'BRADSKANDE', false);

INSERT INTO metadata_label_attribute (metadata_label_id, `key`, `value`)
VALUES ('bb000000-0000-0000-0000-0000000000b1', 'processKey', 'alkt-ansokan'),
       ('bb000000-0000-0000-0000-0000000000b2', 'processKey', 'alkt-tillsyn');

-- A third errand, whose process has run to its end: its process life is over, and its labels are held just as still
-- as those of the errand running one. Its instance id differs from the live one, which uq_ep_process_instance_id holds.
INSERT INTO errand(municipality_id, id, assigned_group_id, assigned_user_id, category, namespace,
                   priority, reporter_user_id, status, title, type, created, modified, resolution,
                   description, escalation_email, errand_number, business_related, previous_status, channel, touched)
VALUES ('2281', 'aa000000-0000-0000-0000-0000000000a3', null, null, 'CATEGORY-1', 'PROCESS-NAMESPACE',
        'MEDIUM', 'joe01doe', 'STATUS-1', 'Errand whose process has finished', 'TYPE-1',
        '2026-01-01 10:00:00.000', null, null, null, null, 'PN-26010003', false, null, null,
        '2026-01-01 10:00:00.000');

INSERT INTO errand_process(id, errand_id, municipality_id, namespace, process_service, process_key,
                           process_instance_id, process_status, current_activity_id, current_activity_name,
                           started, ended, active_marker, created, modified)
VALUES ('ep-it-done', 'aa000000-0000-0000-0000-0000000000a3', '2281', 'PROCESS-NAMESPACE', 'pw-alkt', 'alkt-ansokan',
        'pi-it-done', 'COMPLETED', null, null,
        '2026-01-01 10:00:00.000', '2026-01-01 12:00:00.000', null, '2026-01-01 10:00:00.000', null);

-- The errand running a process, the one whose process has finished and the one that has never had a process all wear
-- the label naming the application process, so that the same relabelling can be asked of each of them.
INSERT INTO errand_labels(errand_id, metadata_label_id)
VALUES ('aa000000-0000-0000-0000-0000000000a1', 'bb000000-0000-0000-0000-0000000000b1'),
       ('aa000000-0000-0000-0000-0000000000a2', 'bb000000-0000-0000-0000-0000000000b1'),
       ('aa000000-0000-0000-0000-0000000000a3', 'bb000000-0000-0000-0000-0000000000b1');
