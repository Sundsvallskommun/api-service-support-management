-- -----------------------------------------------------------------------------------------------
-- Turns PURGE-NAMESPACE into a namespace that runs a process, on top of testdata-it.sql and
-- testdata-it-purge.sql, so that a run has a process to tell of every errand it removes.
--
-- Kept apart from testdata-it-purge.sql: a process consumer on the namespace would give every other
-- purge test outbox rows to account for.
-- -----------------------------------------------------------------------------------------------

INSERT INTO namespace_config(id, municipality_id, namespace, created, modified)
VALUES (81, '2281', 'PURGE-NAMESPACE', '2026-01-01 10:00:00.000', null);

INSERT INTO namespace_config_value(namespace_config_id, `key`, `value`, `type`)
VALUES (81, 'DISPLAY_NAME', 'Purge namespace', 'STRING'),
       (81, 'SHORT_CODE', 'PU', 'STRING'),
       (81, 'NOTIFICATION_TTL_IN_DAYS', '10', 'INTEGER'),
       (81, 'ACCESS_CONTROL', 'false', 'BOOLEAN'),
       (81, 'NOTIFY_REPORTER', 'false', 'BOOLEAN'),
       (81, 'ROLE_BASED_MAPPING', 'false', 'BOOLEAN'),
       (81, 'RESOURCE_ACCESS_CONTROL', 'false', 'BOOLEAN'),
       (81, 'PROCESS_CONSUMER', 'pw-alkt', 'STRING');
