-- -----------------------------------------------------------------------------------------------
-- The process namespace of testdata-it.sql made to wake its process, for the test that runs the whole
-- round between SupportManagement and pw-alkt.
--
-- Kept apart from the shared test data: a trigger on PROCESS-NAMESPACE would make every errand write
-- of the other process tests publish.
-- -----------------------------------------------------------------------------------------------

-- The namespace already delivers to pw-alkt (PROCESS_CONSUMER in testdata-it.sql). A changed errand is
-- what wakes the process, and nothing else: SIGNAL and PROCESS are left out on purpose, so that a
-- command reaching the process shows that the triggers have no say over commands.
INSERT INTO namespace_config_value(namespace_config_id, `key`, `value`, `type`)
VALUES (8, 'PROCESS_TRIGGER', 'ERRAND', 'STRING');

-- A status is always validated against the namespace, and an errand cannot be created without one
INSERT INTO status(id, created, modified, municipality_id, name, display_name, external_display_name, sort_order, namespace, deprecated)
VALUES ('dd000000-0000-0000-0000-0000000000d1', '2026-01-01 10:00:00.000', null, '2281', 'STATUS-1', 'Status 1',
        'External Status 1', 1, 'PROCESS-NAMESPACE', false);

-- The label naming the process. It says nothing about the start mode, which reads as AUTOMATIC, so an
-- errand created wearing it carries the permission to start its process.
INSERT INTO metadata_label (created, modified, municipality_id, namespace, classification, display_name, id, parent_id,
                            resource_name, resource_path, deprecated)
VALUES ('2026-01-01 10:00:00.000', NULL, '2281', 'PROCESS-NAMESPACE', 'CATEGORY', 'Ansokan',
        'dd000000-0000-0000-0000-0000000000d2', NULL, 'ANSOKAN', 'ANSOKAN', false);

INSERT INTO metadata_label_attribute (metadata_label_id, `key`, `value`)
VALUES ('dd000000-0000-0000-0000-0000000000d2', 'processKey', 'alkt-ansokan');

-- An errand already wearing the label and never given a process, for the tests that do not start
-- from a creation
INSERT INTO errand(municipality_id, id, assigned_group_id, assigned_user_id, category, namespace,
                   priority, reporter_user_id, status, title, type, created, modified, resolution,
                   description, escalation_email, errand_number, business_related, previous_status, channel, touched)
VALUES ('2281', 'aa000000-0000-0000-0000-0000000000a4', null, null, 'CATEGORY-1', 'PROCESS-NAMESPACE',
        'MEDIUM', 'joe01doe', 'STATUS-1', 'Errand wearing the process label', 'TYPE-1',
        '2026-01-01 10:00:00.000', null, null, null, null, 'PN-26010004', false, null, null,
        '2026-01-01 10:00:00.000');

INSERT INTO errand_labels(errand_id, metadata_label_id)
VALUES ('aa000000-0000-0000-0000-0000000000a4', 'dd000000-0000-0000-0000-0000000000d2');
