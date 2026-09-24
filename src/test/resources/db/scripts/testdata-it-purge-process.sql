-- -----------------------------------------------------------------------------------------------
-- Turns PURGE-NAMESPACE into a namespace that runs a process, on top of testdata-it.sql and
-- testdata-it-purge.sql, so that a run has a process to tell of every errand it removes.
--
-- Kept apart from testdata-it-purge.sql: a process consumer on the namespace would give every other
-- purge test outbox rows to account for.
-- -----------------------------------------------------------------------------------------------

INSERT INTO namespace_config(id, municipality_id, namespace, created, modified)
VALUES (9, '2281', 'PURGE-NAMESPACE', '2026-01-01 10:00:00.000', null);

INSERT INTO namespace_config_value(namespace_config_id, `key`, `value`, `type`)
VALUES (9, 'DISPLAY_NAME', 'Purge namespace', 'STRING'),
       (9, 'SHORT_CODE', 'PU', 'STRING'),
       (9, 'NOTIFICATION_TTL_IN_DAYS', '10', 'INTEGER'),
       (9, 'ACCESS_CONTROL', 'false', 'BOOLEAN'),
       (9, 'NOTIFY_REPORTER', 'false', 'BOOLEAN'),
       (9, 'ROLE_BASED_MAPPING', 'false', 'BOOLEAN'),
       (9, 'RESOURCE_ACCESS_CONTROL', 'false', 'BOOLEAN'),
       (9, 'PROCESS_CONSUMER', 'pw-alkt', 'STRING');
