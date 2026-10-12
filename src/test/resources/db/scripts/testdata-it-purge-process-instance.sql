-- -----------------------------------------------------------------------------------------------
-- A process instance on the errand a run of testdata-it-purge-process.sql reaches, on top of that
-- script, so that the deletion the run publishes has a process to name.
-- -----------------------------------------------------------------------------------------------

INSERT INTO errand_process(id, errand_id, municipality_id, namespace, process_service, process_key,
                           process_instance_id, process_status, current_activity_id, current_activity_name,
                           started, ended, active_marker, created, modified)
VALUES ('ep-it-purge', 'aaaa1111-0000-0000-0000-000000000001', '2281', 'PURGE-NAMESPACE', 'pw-alkt', 'alkt-ansokan',
        'pi-it-purge', 'RUNNING', 'granska-ansokan', 'Granska ansokan',
        '2026-01-01 10:00:00.000', null, 1, '2026-01-01 10:00:00.000', null);
