-- -----------------------------------
-- Namespace config (required for display name lookup)
-- -----------------------------------
INSERT INTO namespace_config(id, municipality_id, namespace, created, modified)
VALUES (1, '2281', 'NAMESPACE-1', '2024-01-01 00:00:00.000', null);

INSERT INTO namespace_config_value(namespace_config_id, `key`, `value`, `type`)
VALUES (1, 'DISPLAY_NAME', 'Namespace 1', 'STRING'),
       (1, 'SHORT_CODE', 'NS1', 'STRING'),
       (1, 'NOTIFICATION_TTL_IN_DAYS', '10', 'INTEGER'),
       (1, 'ACCESS_CONTROL', 'false', 'BOOLEAN'),
       (1, 'NOTIFY_REPORTER', 'false', 'BOOLEAN'),
       (1, 'ROLE_BASED_MAPPING', 'false', 'BOOLEAN'),
       (1, 'RESOURCE_ACCESS_CONTROL', 'false', 'BOOLEAN'),
       (1, 'BASE_URL', 'https://draken.test.local', 'STRING');

-- -----------------------------------
-- Errand
-- -----------------------------------
INSERT INTO errand(id, assigned_group_id, assigned_user_id, errand_number, municipality_id, namespace, priority, status, title, created, modified, version)
VALUES ('aaaaaaaa-bbbb-cccc-dddd-eeeeeeee0001', 'GROUP-1', 'adm01adm', 'NS1-2024-000001', '2281', 'NAMESPACE-1', 'MEDIUM', 'OPEN', 'Test errand', '2024-06-01 12:00:00.000', null, 0);

-- -----------------------------------
-- Subscribers
-- -----------------------------------
-- Subscriber with EMAIL channel (enhetschef) — active, not paused
INSERT INTO subscriber(id, municipality_id, namespace, name, identifier_type, identifier_value, paused_from, paused_until, created, modified, created_by_type, created_by_value)
VALUES ('11111111-0000-0000-0000-000000000001', '2281', 'NAMESPACE-1', 'Enhetschef', 'adAccount', 'chef01mgr', null, null, '2024-01-01 12:00:00.000', null, 'adAccount', 'adm01adm'),
       ('11111111-0000-0000-0000-000000000002', '2281', 'NAMESPACE-1', 'Internal only', 'adAccount', 'int01usr', null, null, '2024-01-01 12:00:00.000', null, 'adAccount', 'adm01adm');

INSERT INTO subscriber_channel(subscriber_id, sort_order, type, destination)
VALUES ('11111111-0000-0000-0000-000000000001', 0, 'EMAIL', 'chef@example.com'),
       ('11111111-0000-0000-0000-000000000001', 1, 'INTERNAL', null),
       ('11111111-0000-0000-0000-000000000002', 0, 'INTERNAL', null);

-- Event filters: subscribe to UPDATE/MESSAGE and CREATE (any subtype)
INSERT INTO subscriber_event_filter(subscriber_id, sort_order, type, subtype)
VALUES ('11111111-0000-0000-0000-000000000001', 0, 'UPDATE', 'MESSAGE'),
       ('11111111-0000-0000-0000-000000000001', 1, 'CREATE', null);

-- -----------------------------------
-- Subscriptions — NAMESPACE level
-- -----------------------------------
INSERT INTO subscription(id, subscriber_id, target_type, errand_id, expires_at, created, created_by_type, created_by_value)
VALUES ('22222222-0000-0000-0000-000000000001', '11111111-0000-0000-0000-000000000001', 'NAMESPACE', null, null, '2024-01-10 12:00:00.000', 'adAccount', 'adm01adm'),
       ('22222222-0000-0000-0000-000000000002', '11111111-0000-0000-0000-000000000002', 'NAMESPACE', null, null, '2024-01-10 12:00:00.000', 'adAccount', 'adm01adm');

-- -----------------------------------
-- Notification dispatch entries
-- -----------------------------------
-- test01/test02: UPDATE/MESSAGE event (existing)
INSERT INTO notification_dispatch(id, errand_id, municipality_id, namespace, event_id, request_group_id, event_type, sub_type, description, executing_user_id, created)
VALUES ('33333333-0000-0000-0000-000000000001', 'aaaaaaaa-bbbb-cccc-dddd-eeeeeeee0001', '2281', 'NAMESPACE-1', 'evt-001', 'grp-001', 'UPDATE', 'MESSAGE', 'Nytt meddelande', 'other01usr', DATE_SUB(NOW(), INTERVAL 1 MINUTE));

-- test03: CREATE/ERRAND event
INSERT INTO notification_dispatch(id, errand_id, municipality_id, namespace, event_id, request_group_id, event_type, sub_type, description, executing_user_id, created)
VALUES ('33333333-0000-0000-0000-000000000002', 'aaaaaaaa-bbbb-cccc-dddd-eeeeeeee0001', '2281', 'NAMESPACE-1', 'evt-002', 'grp-002', 'CREATE', 'ERRAND', 'Nytt arende', 'other01usr', DATE_SUB(NOW(), INTERVAL 1 MINUTE));
