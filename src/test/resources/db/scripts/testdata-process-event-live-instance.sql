-- -----------------------------------------------------------------------------------------------
-- A live process for the errand of testdata-process-event.sql, on top of it, for the test of an event
-- pw-alkt refuses for good while the errand runs a process.
-- -----------------------------------------------------------------------------------------------
INSERT INTO errand_process(id, errand_id, municipality_id, namespace, process_service, process_key,
                           process_instance_id, process_status, current_activity_id, current_activity_name,
                           started, ended, active_marker, created, modified)
VALUES ('ep-relay-it', 'ec677eb3-604c-4935-bff7-f8f0b500c8f4', '2281', 'NAMESPACE-1', 'pw-alkt', 'alkt-ansokan',
        'pi-relay-it', 'RUNNING', null, null, '2026-01-01 10:00:00.000', null, 1, '2026-01-01 10:00:00.000', null);
