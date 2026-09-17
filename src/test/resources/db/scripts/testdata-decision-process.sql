-- -----------------------------------------------------------------------------------------------
-- The process namespace of testdata-it.sql told about decisions, for the test of the decision chain.
--
-- Kept apart from the shared test data: triggers on PROCESS-NAMESPACE would make every errand write
-- of the other process tests publish.
--
-- Three errands, one in each state the decision rules tell apart: aa..a1 runs its process (ep-it-live
-- in testdata-it.sql), aa..a2 has never had one, and aa..a5 has a process that has run to its end.
-- -----------------------------------------------------------------------------------------------

INSERT INTO namespace_config_value(namespace_config_id, `key`, `value`, `type`)
VALUES (7, 'PROCESS_TRIGGER', 'ERRAND', 'STRING'),
       (7, 'PROCESS_TRIGGER', 'DECISION', 'STRING');

INSERT INTO decision_outcome(id, name, display_name, sort_order, deprecated, namespace, municipality_id, created)
VALUES ('d0000000-0000-0000-0000-0000000000f1', 'APPROVAL', 'Bifall', 1, false, 'PROCESS-NAMESPACE', '2281',
        '2026-01-01 10:00:00.000');

INSERT INTO errand(municipality_id, id, assigned_group_id, assigned_user_id, category, namespace,
                   priority, reporter_user_id, status, title, type, created, modified, resolution,
                   description, escalation_email, errand_number, business_related, previous_status, channel, touched)
VALUES ('2281', 'aa000000-0000-0000-0000-0000000000a5', null, null, 'CATEGORY-1', 'PROCESS-NAMESPACE',
        'MEDIUM', 'joe01doe', 'STATUS-1', 'Errand whose process has ended', 'TYPE-1',
        '2026-01-01 10:00:00.000', null, null, null, null, 'PN-26010005', false, null, null,
        '2026-01-01 10:00:00.000');

INSERT INTO errand_process(id, errand_id, municipality_id, namespace, process_service, process_key,
                           process_instance_id, process_status, current_activity_id, current_activity_name,
                           started, ended, active_marker, created, modified)
VALUES ('ep-it-completed', 'aa000000-0000-0000-0000-0000000000a5', '2281', 'PROCESS-NAMESPACE', 'pw-alkt',
        'alkt-ansokan', 'pi-it-completed', 'COMPLETED', null, null,
        '2026-01-01 10:00:00.000', '2026-01-02 10:00:00.000', null, '2026-01-01 10:00:00.000',
        '2026-01-02 10:00:00.000');

INSERT INTO attachment_data(id, file)
VALUES ('120', 'utkast'),
       ('121', 'beslut'),
       ('122', 'beslut'),
       ('123', 'underlag');

-- One attachment for each decision below, and on the ended errand one more that is linked to nothing
INSERT INTO attachment(id, attachment_data_id, file_name, mime_type, errand_id, namespace, municipality_id, file_size)
VALUES ('ad000000-0000-0000-0000-000000000001', '120', 'utkast.txt', 'text/plain',
        'aa000000-0000-0000-0000-0000000000a1', 'PROCESS-NAMESPACE', '2281', 6),
       ('ad000000-0000-0000-0000-000000000002', '121', 'beslut.txt', 'text/plain',
        'aa000000-0000-0000-0000-0000000000a1', 'PROCESS-NAMESPACE', '2281', 6),
       ('ad000000-0000-0000-0000-000000000003', '122', 'beslut.txt', 'text/plain',
        'aa000000-0000-0000-0000-0000000000a5', 'PROCESS-NAMESPACE', '2281', 6),
       ('ad000000-0000-0000-0000-000000000004', '123', 'underlag.txt', 'text/plain',
        'aa000000-0000-0000-0000-0000000000a5', 'PROCESS-NAMESPACE', '2281', 8);

-- The investigations the draft decision and the decision of the ended errand rest on
INSERT INTO investigation(id, errand_id, municipality_id, namespace, type, status, title, created_by, created, version)
VALUES ('e1000000-0000-0000-0000-000000000001', 'aa000000-0000-0000-0000-0000000000a1', '2281', 'PROCESS-NAMESPACE',
        'SUITABILITY', 'COMPLETED', 'Lämplighetsprövning', 'joe01doe', '2026-01-02 10:00:00.000', 0),
       ('e1000000-0000-0000-0000-000000000002', 'aa000000-0000-0000-0000-0000000000a5', '2281', 'PROCESS-NAMESPACE',
        'SUITABILITY', 'COMPLETED', 'Lämplighetsprövning', 'pw-alkt', '2026-01-02 08:00:00.000', 0);

-- On the running errand a draft and a completed decision, on the errand without a process a completed one, and on
-- the ended errand one the process made and never completed
INSERT INTO decision(id, errand_id, municipality_id, namespace, type, status, outcome, method, decided_by, decided_at,
                     justification, errand_process_id, investigation_id, created_by, created, version)
VALUES ('de000000-0000-0000-0000-000000000001', 'aa000000-0000-0000-0000-0000000000a1', '2281', 'PROCESS-NAMESPACE',
        'PERMIT', 'DRAFT', 'APPROVAL', 'MANUAL', 'joe01doe', '2026-01-03 10:00:00.000', 'Utkast till motivering',
        null, 'e1000000-0000-0000-0000-000000000001', 'joe01doe', '2026-01-03 10:00:00.000', 0),
       ('de000000-0000-0000-0000-000000000002', 'aa000000-0000-0000-0000-0000000000a1', '2281', 'PROCESS-NAMESPACE',
        'PERMIT', 'COMPLETED', 'APPROVAL', 'MANUAL', 'joe01doe', '2026-01-03 10:00:00.000', 'Motivering',
        null, null, 'joe01doe', '2026-01-03 10:00:00.000', 0),
       ('de000000-0000-0000-0000-000000000003', 'aa000000-0000-0000-0000-0000000000a2', '2281', 'PROCESS-NAMESPACE',
        'PERMIT', 'COMPLETED', 'APPROVAL', 'MANUAL', 'joe01doe', '2026-01-03 10:00:00.000', 'Motivering',
        null, null, 'joe01doe', '2026-01-03 10:00:00.000', 0),
       ('de000000-0000-0000-0000-000000000004', 'aa000000-0000-0000-0000-0000000000a5', '2281', 'PROCESS-NAMESPACE',
        'PERMIT', 'ACTIVE', 'APPROVAL', 'AUTOMATIC', 'pw-alkt', '2026-01-02 09:00:00.000', 'Motivering',
        'ep-it-completed', 'e1000000-0000-0000-0000-000000000002', 'pw-alkt', '2026-01-02 09:00:00.000', 0);

INSERT INTO decision_term(id, decision_id, sort_order, category, text)
VALUES ('df000000-0000-0000-0000-000000000001', 'de000000-0000-0000-0000-000000000004', 1, 'serveringstid',
        'Servering får ske mellan 11.00 och 01.00.');

INSERT INTO decision_attachment(decision_id, attachment_id)
VALUES ('de000000-0000-0000-0000-000000000001', 'ad000000-0000-0000-0000-000000000001'),
       ('de000000-0000-0000-0000-000000000002', 'ad000000-0000-0000-0000-000000000002'),
       ('de000000-0000-0000-0000-000000000004', 'ad000000-0000-0000-0000-000000000003');

INSERT INTO decision_json_parameter(id, decision_id, parameter_key, schema_id, value, version)
VALUES ('df100000-0000-0000-0000-000000000001', 'de000000-0000-0000-0000-000000000004', 'legalForce',
        'test-schema-1.0', '{"date":"2026-02-01"}', 0);
