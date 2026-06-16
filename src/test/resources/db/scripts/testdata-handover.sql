-- Adds NAMESPACE-2 to namespace_config so it can be used as a handover target in validation tests.
-- testdata-it.sql has validation entries for NAMESPACE-2 (STATUS validated=true) but no namespace_config entry.
INSERT INTO namespace_config(id, municipality_id, namespace, created, modified)
VALUES (99, '2281', 'NAMESPACE-2', '2021-12-31 23:59:59.999', null);

INSERT INTO namespace_config_value(namespace_config_id, `key`, `value`, `type`)
VALUES (99, 'DISPLAY_NAME', 'Namespace 2', 'STRING'),
       (99, 'SHORT_CODE', 'NS2', 'STRING'),
       (99, 'NOTIFICATION_TTL_IN_DAYS', '10', 'INTEGER'),
       (99, 'ACCESS_CONTROL', 'false', 'BOOLEAN'),
       (99, 'NOTIFY_REPORTER', 'false', 'BOOLEAN');
