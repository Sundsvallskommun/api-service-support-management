-- -----------------------------------------------------------------------------------------------
-- Labels with each start mode, and errands whose process history decides the start permission, on
-- top of testdata-it.sql and testdata-process-loop-guard.sql (which make a changed errand wake the
-- process of PROCESS-NAMESPACE).
-- -----------------------------------------------------------------------------------------------

INSERT INTO metadata_label (created, modified, municipality_id, namespace, classification, display_name, id, parent_id,
                            resource_name, resource_path, deprecated)
VALUES ('2026-01-01 10:00:00.000', NULL, '2281', 'PROCESS-NAMESPACE', 'CATEGORY', 'Ansokan, manuell start',
        'ef000000-0000-0000-0000-0000000000f1', NULL, 'ANSOKAN_MANUELL', 'ANSOKAN_MANUELL', false),
       ('2026-01-01 10:00:00.000', NULL, '2281', 'PROCESS-NAMESPACE', 'CATEGORY', 'Tillsyn, automatisk start',
        'ef000000-0000-0000-0000-0000000000f2', NULL, 'TILLSYN_AUTOMATISK', 'TILLSYN_AUTOMATISK', false),
       ('2026-01-01 10:00:00.000', NULL, '2281', 'PROCESS-NAMESPACE', 'CATEGORY', 'Ansokan, automatisk start',
        'ef000000-0000-0000-0000-0000000000f3', NULL, 'ANSOKAN_AUTOMATISK', 'ANSOKAN_AUTOMATISK', false);

INSERT INTO metadata_label_attribute (metadata_label_id, `key`, `value`)
VALUES ('ef000000-0000-0000-0000-0000000000f1', 'processKey', 'alkt-ansokan'),
       ('ef000000-0000-0000-0000-0000000000f1', 'processStartMode', 'MANUAL'),
       ('ef000000-0000-0000-0000-0000000000f2', 'processKey', 'alkt-tillsyn'),
       ('ef000000-0000-0000-0000-0000000000f2', 'processStartMode', 'AUTOMATIC'),
       ('ef000000-0000-0000-0000-0000000000f3', 'processKey', 'alkt-ansokan'),
       ('ef000000-0000-0000-0000-0000000000f3', 'processStartMode', 'AUTOMATIC');

-- c1: a failed application wearing the manual application label and the automatic supervision label.
-- c2: an application whose process ran to its end. c3: an application whose only start failed.
INSERT INTO errand(municipality_id, id, assigned_group_id, assigned_user_id, category, namespace,
                   priority, reporter_user_id, status, title, type, created, modified, resolution,
                   description, escalation_email, errand_number, business_related, previous_status, channel, touched)
VALUES ('2281', 'ac000000-0000-0000-0000-0000000000c1', null, null, 'CATEGORY-1', 'PROCESS-NAMESPACE',
        'MEDIUM', 'joe01doe', 'STATUS-1', 'Failed application wearing two process labels', 'TYPE-1',
        '2026-01-01 10:00:00.000', null, null, null, null, 'PN-26010021', false, null, null,
        '2026-01-01 10:00:00.000'),
       ('2281', 'ac000000-0000-0000-0000-0000000000c2', null, null, 'CATEGORY-1', 'PROCESS-NAMESPACE',
        'MEDIUM', 'joe01doe', 'STATUS-1', 'Application whose process has finished', 'TYPE-1',
        '2026-01-01 10:00:00.000', null, null, null, null, 'PN-26010022', false, null, null,
        '2026-01-01 10:00:00.000'),
       ('2281', 'ac000000-0000-0000-0000-0000000000c3', null, null, 'CATEGORY-1', 'PROCESS-NAMESPACE',
        'MEDIUM', 'joe01doe', 'STATUS-1', 'Application whose start failed', 'TYPE-1',
        '2026-01-01 10:00:00.000', null, null, null, null, 'PN-26010023', false, null, null,
        '2026-01-01 10:00:00.000');

INSERT INTO errand_labels(errand_id, metadata_label_id)
VALUES ('ac000000-0000-0000-0000-0000000000c1', 'ef000000-0000-0000-0000-0000000000f1'),
       ('ac000000-0000-0000-0000-0000000000c1', 'ef000000-0000-0000-0000-0000000000f2'),
       ('ac000000-0000-0000-0000-0000000000c2', 'ef000000-0000-0000-0000-0000000000f3'),
       ('ac000000-0000-0000-0000-0000000000c3', 'ef000000-0000-0000-0000-0000000000f3');

INSERT INTO errand_process(id, errand_id, municipality_id, namespace, process_service, process_key,
                           process_instance_id, process_status, current_activity_id, current_activity_name,
                           error_code, error_message, started, ended, active_marker, created, modified)
VALUES ('ep-mode-failed-two', 'ac000000-0000-0000-0000-0000000000c1', '2281', 'PROCESS-NAMESPACE', 'pw-alkt', 'alkt-ansokan',
        null, 'FAILED', null, null, 'START_FAILED', 'The process engine did not answer',
        null, '2026-01-01 10:05:00.000', null, '2026-01-01 10:05:00.000', null),
       ('ep-mode-done', 'ac000000-0000-0000-0000-0000000000c2', '2281', 'PROCESS-NAMESPACE', 'pw-alkt', 'alkt-ansokan',
        'pi-mode-done', 'COMPLETED', null, null, null, null,
        '2026-01-01 10:00:00.000', '2026-01-01 12:00:00.000', null, '2026-01-01 10:00:00.000', null),
       ('ep-mode-failed', 'ac000000-0000-0000-0000-0000000000c3', '2281', 'PROCESS-NAMESPACE', 'pw-alkt', 'alkt-ansokan',
        null, 'FAILED', null, null, 'START_FAILED', 'The process engine did not answer',
        null, '2026-01-01 10:05:00.000', null, '2026-01-01 10:05:00.000', null);
