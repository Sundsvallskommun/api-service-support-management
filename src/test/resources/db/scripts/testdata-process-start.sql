-- -----------------------------------------------------------------------------------------------
-- Errands to start a process for by hand, on top of testdata-it.sql. PROCESS-NAMESPACE names no
-- process triggers, so every row written here is written by a start command.
-- -----------------------------------------------------------------------------------------------

-- A supervision label that leaves the start to a handler, and an application label that says nothing
-- about the start mode and therefore starts on its own
INSERT INTO metadata_label (created, modified, municipality_id, namespace, classification, display_name, id, parent_id,
                            resource_name, resource_path, deprecated)
VALUES ('2026-01-01 10:00:00.000', NULL, '2281', 'PROCESS-NAMESPACE', 'CATEGORY', 'Tillsyn',
        'ee000000-0000-0000-0000-0000000000e1', NULL, 'TILLSYN', 'TILLSYN', false),
       ('2026-01-01 10:00:00.000', NULL, '2281', 'PROCESS-NAMESPACE', 'CATEGORY', 'Ansokan',
        'ee000000-0000-0000-0000-0000000000e2', NULL, 'ANSOKAN', 'ANSOKAN', false);

INSERT INTO metadata_label_attribute (metadata_label_id, `key`, `value`)
VALUES ('ee000000-0000-0000-0000-0000000000e1', 'processKey', 'alkt-tillsyn'),
       ('ee000000-0000-0000-0000-0000000000e1', 'processStartMode', 'MANUAL'),
       ('ee000000-0000-0000-0000-0000000000e2', 'processKey', 'alkt-ansokan');

-- b1: supervision without a process. b2: both labels, so two processes to choose between. b3: an
-- application whose only start failed. b4: an application whose process ran to its end. b5: a
-- supervision for the emergency brake to be tripped on.
INSERT INTO errand(municipality_id, id, assigned_group_id, assigned_user_id, category, namespace,
                   priority, reporter_user_id, status, title, type, created, modified, resolution,
                   description, escalation_email, errand_number, business_related, previous_status, channel, touched)
VALUES ('2281', 'ab000000-0000-0000-0000-0000000000b1', null, null, 'CATEGORY-1', 'PROCESS-NAMESPACE',
        'MEDIUM', 'joe01doe', 'STATUS-1', 'Supervision without a process', 'TYPE-1',
        '2026-01-01 10:00:00.000', null, null, null, null, 'PN-26010011', false, null, null,
        '2026-01-01 10:00:00.000'),
       ('2281', 'ab000000-0000-0000-0000-0000000000b2', null, null, 'CATEGORY-1', 'PROCESS-NAMESPACE',
        'MEDIUM', 'joe01doe', 'STATUS-1', 'Errand pointing at two processes', 'TYPE-1',
        '2026-01-01 10:00:00.000', null, null, null, null, 'PN-26010012', false, null, null,
        '2026-01-01 10:00:00.000'),
       ('2281', 'ab000000-0000-0000-0000-0000000000b3', null, null, 'CATEGORY-1', 'PROCESS-NAMESPACE',
        'MEDIUM', 'joe01doe', 'STATUS-1', 'Application whose start failed', 'TYPE-1',
        '2026-01-01 10:00:00.000', null, null, null, null, 'PN-26010013', false, null, null,
        '2026-01-01 10:00:00.000'),
       ('2281', 'ab000000-0000-0000-0000-0000000000b4', null, null, 'CATEGORY-1', 'PROCESS-NAMESPACE',
        'MEDIUM', 'joe01doe', 'STATUS-1', 'Application whose process has finished', 'TYPE-1',
        '2026-01-01 10:00:00.000', null, null, null, null, 'PN-26010014', false, null, null,
        '2026-01-01 10:00:00.000'),
       ('2281', 'ab000000-0000-0000-0000-0000000000b5', null, null, 'CATEGORY-1', 'PROCESS-NAMESPACE',
        'MEDIUM', 'joe01doe', 'STATUS-1', 'Supervision with lively traffic', 'TYPE-1',
        '2026-01-01 10:00:00.000', null, null, null, null, 'PN-26010015', false, null, null,
        '2026-01-01 10:00:00.000');

INSERT INTO errand_labels(errand_id, metadata_label_id)
VALUES ('ab000000-0000-0000-0000-0000000000b1', 'ee000000-0000-0000-0000-0000000000e1'),
       ('ab000000-0000-0000-0000-0000000000b2', 'ee000000-0000-0000-0000-0000000000e1'),
       ('ab000000-0000-0000-0000-0000000000b2', 'ee000000-0000-0000-0000-0000000000e2'),
       ('ab000000-0000-0000-0000-0000000000b3', 'ee000000-0000-0000-0000-0000000000e2'),
       ('ab000000-0000-0000-0000-0000000000b4', 'ee000000-0000-0000-0000-0000000000e2'),
       ('ab000000-0000-0000-0000-0000000000b5', 'ee000000-0000-0000-0000-0000000000e1');

INSERT INTO errand_process(id, errand_id, municipality_id, namespace, process_service, process_key,
                           process_instance_id, process_status, current_activity_id, current_activity_name,
                           error_code, error_message, started, ended, active_marker, created, modified)
VALUES ('ep-start-failed', 'ab000000-0000-0000-0000-0000000000b3', '2281', 'PROCESS-NAMESPACE', 'pw-alkt', 'alkt-ansokan',
        null, 'FAILED', null, null, 'START_FAILED', 'The process engine did not answer',
        null, '2026-01-01 10:05:00.000', null, '2026-01-01 10:05:00.000', null),
       ('ep-start-done', 'ab000000-0000-0000-0000-0000000000b4', '2281', 'PROCESS-NAMESPACE', 'pw-alkt', 'alkt-ansokan',
        'pi-start-done', 'COMPLETED', null, null, null, null,
        '2026-01-01 10:00:00.000', '2026-01-01 12:00:00.000', null, '2026-01-01 10:00:00.000', null);
