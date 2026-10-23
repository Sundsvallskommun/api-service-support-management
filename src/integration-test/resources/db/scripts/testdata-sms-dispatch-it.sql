-- -----------------------------------
-- Namespace configs: NAMESPACE-1 has enabled sms, NAMESPACE-2 has not
-- -----------------------------------
INSERT INTO namespace_config(id, municipality_id, namespace, created, modified)
VALUES (1, '2281', 'NAMESPACE-1', '2024-01-01 00:00:00.000', null),
       (2, '2281', 'NAMESPACE-2', '2024-01-01 00:00:00.000', null);

INSERT INTO namespace_config_value(namespace_config_id, `key`, `value`, `type`)
VALUES (1, 'DISPLAY_NAME', 'Namespace 1', 'STRING'),
       (1, 'SHORT_CODE', 'NS1', 'STRING'),
       (1, 'NOTIFICATION_TTL_IN_DAYS', '10', 'INTEGER'),
       (1, 'ACCESS_CONTROL', 'false', 'BOOLEAN'),
       (1, 'NOTIFY_REPORTER', 'false', 'BOOLEAN'),
       (1, 'ROLE_BASED_MAPPING', 'false', 'BOOLEAN'),
       (1, 'RESOURCE_ACCESS_CONTROL', 'false', 'BOOLEAN'),
       (1, 'SMS_NOTIFICATIONS_ENABLED', 'true', 'BOOLEAN'),
       (1, 'BASE_URL', 'https://draken.test.local/NS1/arende/', 'STRING'),
       (2, 'DISPLAY_NAME', 'Namespace 2', 'STRING'),
       (2, 'SHORT_CODE', 'NS2', 'STRING'),
       (2, 'NOTIFICATION_TTL_IN_DAYS', '10', 'INTEGER'),
       (2, 'ACCESS_CONTROL', 'false', 'BOOLEAN'),
       (2, 'NOTIFY_REPORTER', 'false', 'BOOLEAN'),
       (2, 'ROLE_BASED_MAPPING', 'false', 'BOOLEAN'),
       (2, 'RESOURCE_ACCESS_CONTROL', 'false', 'BOOLEAN'),
       (2, 'SMS_NOTIFICATIONS_ENABLED', 'false', 'BOOLEAN');

-- -----------------------------------
-- Errands
-- -----------------------------------
INSERT INTO errand(id, assigned_group_id, assigned_user_id, errand_number, municipality_id, namespace, priority, status, title, created, modified, version)
VALUES ('aaaaaaaa-bbbb-cccc-dddd-eeeeeeee0001', 'GROUP-1', 'adm01adm', 'NS1-2024-000001', '2281', 'NAMESPACE-1', 'MEDIUM', 'OPEN', 'Test errand', '2024-06-01 12:00:00.000', null, 0),
       ('aaaaaaaa-bbbb-cccc-dddd-eeeeeeee0002', 'GROUP-1', 'adm01adm', 'NS2-2024-000001', '2281', 'NAMESPACE-2', 'MEDIUM', 'OPEN', 'Test errand', '2024-06-01 12:00:00.000', null, 0);

-- -----------------------------------
-- Subscribers
-- -----------------------------------
-- An sms subscriber in each namespace, with a mobile number of its own
INSERT INTO subscriber(id, municipality_id, namespace, name, identifier_type, identifier_value, paused_from, paused_until, created, modified, created_by_type, created_by_value)
VALUES ('11111111-0000-0000-0000-000000000001', '2281', 'NAMESPACE-1', 'Jour', 'adAccount', 'jou01usr', null, null, '2024-01-01 12:00:00.000', null, 'adAccount', 'adm01adm'),
       ('11111111-0000-0000-0000-000000000002', '2281', 'NAMESPACE-2', 'Jour', 'adAccount', 'jou02usr', null, null, '2024-01-01 12:00:00.000', null, 'adAccount', 'adm01adm');

INSERT INTO subscriber_channel(subscriber_id, sort_order, type, destination)
VALUES ('11111111-0000-0000-0000-000000000001', 0, 'SMS', '+46701234567'),
       ('11111111-0000-0000-0000-000000000002', 0, 'SMS', '+46709876543');

-- Only interested in messages, so the profile alone texts about new errands
INSERT INTO subscriber_event_filter(subscriber_id, sort_order, type, subtype)
VALUES ('11111111-0000-0000-0000-000000000001', 0, 'UPDATE', 'MESSAGE_EXTERNAL'),
       ('11111111-0000-0000-0000-000000000002', 0, 'UPDATE', 'MESSAGE_EXTERNAL');

INSERT INTO subscription(id, subscriber_id, target_type, errand_id, expires_at, created, created_by_type, created_by_value)
VALUES ('22222222-0000-0000-0000-000000000001', '11111111-0000-0000-0000-000000000001', 'NAMESPACE', null, null, '2024-01-10 12:00:00.000', 'adAccount', 'adm01adm'),
       ('22222222-0000-0000-0000-000000000002', '11111111-0000-0000-0000-000000000002', 'NAMESPACE', null, null, '2024-01-10 12:00:00.000', 'adAccount', 'adm01adm');

-- -----------------------------------
-- A profile texting about new errands, whose member only has the internal channel
-- -----------------------------------
INSERT INTO subscription_profile(id, municipality_id, namespace, name, description, created, modified)
VALUES ('55555555-0000-0000-0000-000000000001', '2281', 'NAMESPACE-1', 'Sms: nya arenden', null, '2024-01-01 12:00:00.000', null);

INSERT INTO subscription_profile_event_filter(profile_id, sort_order, type, subtype, label_id)
VALUES ('55555555-0000-0000-0000-000000000001', 0, 'CREATE', null, null);

INSERT INTO subscription_profile_channel(profile_id, sort_order, type)
VALUES ('55555555-0000-0000-0000-000000000001', 0, 'SMS');

INSERT INTO subscriber(id, municipality_id, namespace, name, identifier_type, identifier_value, paused_from, paused_until, created, modified, created_by_type, created_by_value)
VALUES ('11111111-0000-0000-0000-000000000003', '2281', 'NAMESPACE-1', null, 'adAccount', 'pro01usr', null, null, '2024-01-01 12:00:00.000', null, 'adAccount', 'job01');

INSERT INTO subscriber_channel(subscriber_id, sort_order, type, destination)
VALUES ('11111111-0000-0000-0000-000000000003', 0, 'INTERNAL', null);

INSERT INTO subscription(id, subscriber_id, target_type, errand_id, profile_id, expires_at, created, created_by_type, created_by_value)
VALUES ('22222222-0000-0000-0000-000000000003', '11111111-0000-0000-0000-000000000003', 'NAMESPACE', null, '55555555-0000-0000-0000-000000000001', null, '2024-01-10 12:00:00.000', 'adAccount', 'job01');

-- -----------------------------------
-- Notification dispatch entries
-- -----------------------------------
-- An UPDATE/MESSAGE_EXTERNAL event on each errand, which the profile does not route
INSERT INTO notification_dispatch(id, errand_id, municipality_id, namespace, event_id, request_group_id, event_type, sub_type, description, executing_user_id, created)
VALUES ('33333333-0000-0000-0000-000000000001', 'aaaaaaaa-bbbb-cccc-dddd-eeeeeeee0001', '2281', 'NAMESPACE-1', 'evt-001', 'grp-001', 'UPDATE', 'MESSAGE_EXTERNAL', 'Nytt meddelande', 'other01usr', DATE_SUB(NOW(), INTERVAL 1 MINUTE)),
       ('33333333-0000-0000-0000-000000000002', 'aaaaaaaa-bbbb-cccc-dddd-eeeeeeee0002', '2281', 'NAMESPACE-2', 'evt-002', 'grp-002', 'UPDATE', 'MESSAGE_EXTERNAL', 'Nytt meddelande', 'other01usr', DATE_SUB(NOW(), INTERVAL 1 MINUTE)),
-- A CREATE/ERRAND event, which the profile routes to sms
       ('33333333-0000-0000-0000-000000000003', 'aaaaaaaa-bbbb-cccc-dddd-eeeeeeee0001', '2281', 'NAMESPACE-1', 'evt-003', 'grp-003', 'CREATE', 'ERRAND', 'Nytt arende', 'other01usr', DATE_SUB(NOW(), INTERVAL 1 MINUTE));
