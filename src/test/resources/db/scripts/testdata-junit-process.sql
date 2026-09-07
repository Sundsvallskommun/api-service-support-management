-- Fixed wall-clock literals rather than offsets from NOW(): the queries here work in minutes, and NOW() is read from
-- the clock of the database while the moments a test passes in are read from the clock of the JVM. The tests convert
-- these literals in their own default zone, which is the same conversion the entities make when they store one.
INSERT INTO errand_process(id, errand_id, municipality_id, namespace, process_service, process_key, process_instance_id,
                           process_status, current_activity_id, current_activity_name, started, ended, active_marker,
                           created, modified)
VALUES ('ep-live-1', 'ERRAND_ID-1', '2281', 'NAMESPACE.1', 'pw-alkt', 'alkt-ansokan', 'pi-live-1',
        'WAITING', 'granska-ansokan', 'Granska ansokan', '2026-01-01 10:00:00.000', null, 1,
        '2026-01-01 10:00:00.000', null),
       ('ep-done-1', 'ERRAND_ID-1', '2281', 'NAMESPACE.1', 'pw-alkt', 'alkt-ansokan', 'pi-done-1',
        'COMPLETED', null, null, '2025-12-20 08:00:00.000', '2025-12-21 08:00:00.000', null,
        '2025-12-20 08:00:00.000', '2025-12-21 08:00:00.000'),
       ('ep-failed-2', 'ERRAND_ID-2', '2281', 'NAMESPACE.1', 'pw-alkt', 'alkt-tillsyn', 'pi-failed-2',
        'FAILED', null, null, '2025-12-28 08:00:00.000', '2025-12-28 09:00:00.000', null,
        '2025-12-28 08:00:00.000', '2025-12-28 09:00:00.000');

INSERT INTO errand_process_activity(id, errand_process_id, errand_id, external_task_id, activity_type, activity_id,
                                    activity_name, severity, message, error_code, occurred_at, created)
VALUES ('epa-task-1', 'ep-live-1', 'ERRAND_ID-1', 'task-1', 'TASK', 'granska-ansokan', 'Granska ansokan',
        'INFO', null, null, '2026-01-01 10:00:00.000', '2026-01-01 10:00:00.000'),
       ('epa-phase-1', 'ep-live-1', 'ERRAND_ID-1', null, 'PHASE', 'utredning', 'Utredning',
        'INFO', null, null, '2026-01-01 11:00:00.000', '2026-01-01 11:00:00.000'),
       -- Written before any instance existed, which is why the instance column is nullable.
       ('epa-config-1', null, 'ERRAND_ID-1', null, 'CONFIG', null, null,
        'ERROR', 'Two labels resolve to different process keys', 'AMBIGUOUS_PROCESS_KEY',
        '2026-01-01 11:55:00.000', '2026-01-01 11:55:00.000'),
       -- Aged out of both the window and the retention.
       ('epa-config-2', null, 'ERRAND_ID-2', null, 'CONFIG', null, null,
        'ERROR', 'Two labels resolve to different process keys', 'AMBIGUOUS_PROCESS_KEY',
        '2025-01-01 08:00:00.000', '2025-01-01 08:00:00.000');

INSERT INTO process_event_outbox(id, municipality_id, namespace, errand_id, process_service, process_key, event_type,
                                 event_sub_type, start_allowed, signal_name, executed_by, request_group_id, created,
                                 delivered_at)
VALUES ('peo-waiting', '2281', 'NAMESPACE.1', 'ERRAND_ID-1', 'pw-alkt', 'alkt-ansokan', 'UPDATE',
        'MESSAGE', 0, null, 'joe01doe', null, '2026-01-01 11:50:00.000', null),
       -- Too young to be picked up: the transaction that wrote it may still be committing.
       ('peo-just-written', '2281', 'NAMESPACE.1', 'ERRAND_ID-1', 'pw-alkt', 'alkt-ansokan', 'UPDATE',
        'ERRAND', 0, null, 'joe01doe', null, '2026-01-01 11:59:59.000', null),
       ('peo-delivered-in-window', '2281', 'NAMESPACE.1', 'ERRAND_ID-1', 'pw-alkt', 'alkt-ansokan', 'UPDATE',
        'ERRAND', 0, null, 'joe01doe', null, '2026-01-01 11:59:00.000', '2026-01-01 11:59:30.000'),
       ('peo-delivered-long-ago', '2281', 'NAMESPACE.1', 'ERRAND_ID-1', 'pw-alkt', 'alkt-ansokan', 'CREATE',
        'ERRAND', 1, null, 'joe01doe', null, '2026-01-01 10:00:00.000', '2026-01-01 10:00:05.000'),
       -- Addressed to another process engine.
       ('peo-other-consumer', '2281', 'NAMESPACE.1', 'ERRAND_ID-2', 'pw-other', 'other-process', 'UPDATE',
        'ERRAND', 0, null, 'joe01doe', null, '2026-01-01 11:40:00.000', null);
