-- Errands for the search integration test, in a namespace of their own so that the expected hits do not shift when
-- the shared test data changes. Loaded on top of testdata-it.sql, which holds the namespace configuration.
INSERT INTO errand(municipality_id, id, namespace, priority, status, category, type, title, description, reporter_user_id,
                   assigned_user_id, created, touched, errand_number, business_related)
VALUES ('2281', 'ee000000-0000-0000-0000-00000000se01', 'NAMESPACE-3', 'HIGH', 'NEW', 'VATTEN', 'LÄCKA',
        'Vattenläcka på Storgatan', 'Vattnet forsar ut ur en spricka i gatan utanför nummer 12', 'rep01ort', 'han01dle',
        '2025-01-10 08:00:00.000', '2025-01-10 08:00:00.000', 'NS3-25010001', false),
       ('2281', 'ee000000-0000-0000-0000-00000000se02', 'NAMESPACE-3', 'LOW', 'ONGOING', 'VATTEN', 'FAKTURA',
        'Fråga om vattenfaktura', 'Kunden undrar varför fakturan blev högre än vanligt', 'rep02ort', 'han01dle',
        '2025-02-10 08:00:00.000', '2025-02-10 08:00:00.000', 'NS3-25020001', false),
       ('2281', 'ee000000-0000-0000-0000-00000000se03', 'NAMESPACE-3', 'MEDIUM', 'NEW', 'GATA', 'BELYSNING',
        'Trasig gatubelysning', 'Lampan vid busshållplatsen är släckt sedan en vecka', 'rep03ort', 'han02dle',
        '2025-03-10 08:00:00.000', '2025-03-10 08:00:00.000', 'NS3-25030001', false),
       -- Same words as the first errand, but another namespace: never a hit in NAMESPACE-3
       ('2281', 'ee000000-0000-0000-0000-00000000se04', 'NAMESPACE-1', 'HIGH', 'NEW', 'VATTEN', 'LÄCKA',
        'Vattenläcka på Storgatan', 'Vattnet forsar ut ur en spricka i gatan', 'rep01ort', 'han01dle',
        '2025-01-10 08:00:00.000', '2025-01-10 08:00:00.000', 'NS1-25010099', false);

INSERT INTO stakeholder(id, external_id, external_id_type, errand_id, first_name, last_name, role, address, zip_code, city)
VALUES ('3901', 'aa000000-0000-0000-0000-000000000901', 'PRIVATE', 'ee000000-0000-0000-0000-00000000se01', 'Anna', 'Bergström', 'APPLICANT', 'Storgatan 12', '85230', 'Sundsvall'),
       ('3902', 'aa000000-0000-0000-0000-000000000902', 'PRIVATE', 'ee000000-0000-0000-0000-00000000se02', 'Erik', 'Lindqvist', 'APPLICANT', 'Skolgatan 3', '85231', 'Sundsvall'),
       ('3903', 'aa000000-0000-0000-0000-000000000903', 'ENTERPRISE', 'ee000000-0000-0000-0000-00000000se03', null, null, 'CONTACT', 'Industrivägen 1', '85240', 'Timrå');

INSERT INTO contact_channel(stakeholder_id, type, value)
VALUES ('3901', 'EMAIL', 'anna.bergstrom@example.com'),
       ('3901', 'PHONE', '+46701234567'),
       ('3902', 'EMAIL', 'erik.lindqvist@example.com');

INSERT INTO parameter(errand_id, id, parameters_key, display_name, parameter_group)
VALUES ('ee000000-0000-0000-0000-00000000se01', 'pp000000-0000-0000-0000-000000000901', 'location', 'Plats', 'Ärende'),
       ('ee000000-0000-0000-0000-00000000se02', 'pp000000-0000-0000-0000-000000000902', 'invoiceNumber', 'Fakturanummer', 'Faktura');

INSERT INTO parameter_values(parameter_id, value, value_order)
VALUES ('pp000000-0000-0000-0000-000000000901', 'Storgatan', 0),
       ('pp000000-0000-0000-0000-000000000901', 'Centrum', 1),
       ('pp000000-0000-0000-0000-000000000902', 'INV-778899', 0);

INSERT INTO json_parameter(id, errand_id, parameter_key, schema_id, value, version)
VALUES ('jp000000-0000-0000-0000-000000000901', 'ee000000-0000-0000-0000-00000000se01', 'vehicle', 'vehicle-1.0',
        '{"regNo":"ABC123","owner":{"name":"Anna Bergström"},"tags":["tjänstebil","diesel"]}', 0),
       ('jp000000-0000-0000-0000-000000000902', 'ee000000-0000-0000-0000-00000000se03', 'vehicle', 'vehicle-1.0',
        '{"regNo":"XYZ789","owner":{"name":"Timrå Industri AB"}}', 0);

INSERT INTO communication(internal, viewed, sender, sender_user_id, sent, id, errand_number, external_id,
                          message_body, target, subject, direction, type, namespace, municipality_id, html_message_body)
VALUES (0, 0, 'Anna Bergström', null, '2025-01-11 09:00:00.000', 'cc000000-0000-0000-0000-000000000901', 'NS3-25010001', null,
        'Hej, det rinner fortfarande vatten på gatan. Kan ni skicka någon?', 'anna.bergstrom@example.com', 'Uppföljning av vattenläckan', 'INBOUND', 'EMAIL', 'NAMESPACE-3', '2281', null);

INSERT INTO measure(id, errand_id, municipality_id, namespace, status, type, title, description, goal, responsible_user, created_by, created, version)
VALUES ('ee000000-0000-0000-0000-000000000901', 'ee000000-0000-0000-0000-00000000se03', '2281', 'NAMESPACE-3', 'ACTIVE', 'MEASURE-1',
        'Byte av armatur', 'Beställ ny armatur från leverantören', 'Fungerande belysning vid hållplatsen', 'han02dle', 'han02dle', '2025-03-11 10:00:00.000', 0);

INSERT INTO measure_json_parameter(id, measure_id, parameter_key, schema_id, value, version)
VALUES ('jp000000-0000-0000-0000-000000000903', 'ee000000-0000-0000-0000-000000000901', 'order', 'order-1.0', '{"supplier":"Ljusbolaget","orderNo":"ORD-4711"}', 0);

-- A communication on the errand of the namespace that weighs resource grants, for the search access tests
INSERT INTO communication(internal, viewed, sender, sender_user_id, sent, id, errand_number, external_id,
                          message_body, target, subject, direction, type, namespace, municipality_id, html_message_body)
VALUES (0, 0, 'Frida Frontline', null, '2025-01-11 09:00:00.000', 'cc000000-0000-0000-0000-000000000902', 'FL-23020001', null,
        'Ett hemligt meddelande', 'frida@example.com', 'Hemligt ärende', 'INBOUND', 'EMAIL', 'NAMESPACE-2507', '2506', null);

-- -----------------------------------------------------------------------------------------------
-- A namespace where the labels of a user reach one errand at read and another at limited read, for
-- the search of an errand held at limited read. Its own namespace, so that the errands the tests of
-- NAMESPACE-2506 count stay as they are.
-- -----------------------------------------------------------------------------------------------
INSERT INTO namespace_config(id, municipality_id, namespace, created, modified)
VALUES (8, '2506', 'NAMESPACE-2508', '2026-09-22 10:00:00.000', null);

INSERT INTO namespace_config_value(namespace_config_id, `key`, `value`, `type`)
VALUES (8, 'DISPLAY_NAME', 'Namespace 2508', 'STRING'),
       (8, 'SHORT_CODE', 'LR', 'STRING'),
       (8, 'NOTIFICATION_TTL_IN_DAYS', '40', 'INTEGER'),
       (8, 'ACCESS_CONTROL', 'true', 'BOOLEAN'),
       (8, 'NOTIFY_REPORTER', 'false', 'BOOLEAN'),
       (8, 'ROLE_BASED_MAPPING', 'false', 'BOOLEAN'),
       (8, 'RESOURCE_ACCESS_CONTROL', 'false', 'BOOLEAN');

-- What a limited read exposes here: the errand by its number, title and status, and nothing of the
-- resources hanging off it.
INSERT INTO namespace_config_access_grant(namespace_config_id, `scope`, `type`, `value`, access_level)
VALUES (8, 'LIMITED', 'FIELD', 'ID', null),
       (8, 'LIMITED', 'FIELD', 'ERRAND_NUMBER', null),
       (8, 'LIMITED', 'FIELD', 'TITLE', null),
       (8, 'LIMITED', 'FIELD', 'STATUS', null);

INSERT INTO metadata_label (created, modified, municipality_id, namespace, classification, display_name, id, parent_id, resource_name, resource_path, deprecated) VALUES
    ('2026-09-22 10:00:00.000', NULL, '2506', 'NAMESPACE-2508', 'CLASS', 'TEAM-A-DISPLAY-NAME', 'aa000000-0000-0000-0000-0000000008a1', NULL, 'TEAM-A', 'TEAM-A', false),
    ('2026-09-22 10:00:00.000', NULL, '2506', 'NAMESPACE-2508', 'CLASS', 'TEAM-B-DISPLAY-NAME', 'bb000000-0000-0000-0000-0000000008b1', NULL, 'TEAM-B', 'TEAM-B', false);

INSERT INTO errand(municipality_id, id, namespace, priority, status, category, type, title, description, reporter_user_id,
                   created, touched, errand_number, business_related)
VALUES ('2506', 'ee000000-0000-0000-0000-0000000008a0', 'NAMESPACE-2508', 'HIGH', 'NEW', 'VATTEN', 'LÄCKA',
        'Vattenläcka i Team A', 'Det rinner vatten i källaren', 'rep08ort',
        '2026-01-10 08:00:00.000', '2026-01-10 08:00:00.000', 'LR-26010001', false),
       ('2506', 'ee000000-0000-0000-0000-0000000008b0', 'NAMESPACE-2508', 'HIGH', 'NEW', 'VATTEN', 'LÄCKA',
        'Vattenläcka i Team B', 'Det rinner vatten på vinden', 'rep08ort',
        '2026-02-10 08:00:00.000', '2026-02-10 08:00:00.000', 'LR-26020001', false);

INSERT INTO errand_access_labels(errand_id, metadata_label_id)
VALUES ('ee000000-0000-0000-0000-0000000008a0', 'aa000000-0000-0000-0000-0000000008a1'),
       ('ee000000-0000-0000-0000-0000000008b0', 'bb000000-0000-0000-0000-0000000008b1');

INSERT INTO communication(internal, viewed, sender, sender_user_id, sent, id, errand_number, external_id,
                          message_body, target, subject, direction, type, namespace, municipality_id, html_message_body)
VALUES (0, 0, 'Bo Bergman', null, '2026-02-11 09:00:00.000', 'cc000000-0000-0000-0000-0000000008b1', 'LR-26020001', null,
        'Vattnet står kvar på vinden', 'bo@example.com', 'Uppföljning från Team B', 'INBOUND', 'EMAIL', 'NAMESPACE-2508', '2506', null);

-- -----------------------------------------------------------------------------------------------
-- A namespace where a role sees one field that a search without a field never looks in: the status
-- is a value searched by name, not a word. Nothing is left for a word to look in there, which is an
-- empty answer, while a query naming the status is answered.
-- -----------------------------------------------------------------------------------------------
INSERT INTO namespace_config(id, municipality_id, namespace, created, modified)
VALUES (9, '2506', 'NAMESPACE-2509', '2026-09-24 10:00:00.000', null);

INSERT INTO namespace_config_value(namespace_config_id, `key`, `value`, `type`)
VALUES (9, 'DISPLAY_NAME', 'Namespace 2509', 'STRING'),
       (9, 'SHORT_CODE', 'SO', 'STRING'),
       (9, 'NOTIFICATION_TTL_IN_DAYS', '40', 'INTEGER'),
       (9, 'ACCESS_CONTROL', 'true', 'BOOLEAN'),
       (9, 'NOTIFY_REPORTER', 'false', 'BOOLEAN'),
       (9, 'ROLE_BASED_MAPPING', 'true', 'BOOLEAN'),
       (9, 'RESOURCE_ACCESS_CONTROL', 'true', 'BOOLEAN');

INSERT INTO namespace_config_access_grant(namespace_config_id, `scope`, `type`, `value`, access_level)
VALUES (9, 'STATUS_ONLY', 'FIELD', 'STATUS', null);

INSERT INTO metadata_label (created, modified, municipality_id, namespace, classification, display_name, id, parent_id, resource_name, resource_path, deprecated) VALUES
    ('2026-09-24 10:00:00.000', NULL, '2506', 'NAMESPACE-2509', 'CLASS', 'TEAM-S-DISPLAY-NAME', 'ss000000-0000-0000-0000-0000000009s1', NULL, 'TEAM-S', 'TEAM-S', false);

INSERT INTO errand(municipality_id, id, namespace, priority, status, category, type, title, description, reporter_user_id,
                   created, touched, errand_number, business_related)
VALUES ('2506', 'ee000000-0000-0000-0000-0000000009s0', 'NAMESPACE-2509', 'HIGH', 'NEW', 'VATTEN', 'LÄCKA',
        'Vattenläcka i Team S', 'Det rinner vatten i trapphuset', 'rep09ort',
        '2026-03-10 08:00:00.000', '2026-03-10 08:00:00.000', 'SO-26030001', false),
       ('2506', 'ee000000-0000-0000-0000-0000000009s2', 'NAMESPACE-2509', 'LOW', 'ONGOING', 'GATA', 'BELYSNING',
        'Trasig lampa i Team S', 'Lampan vid porten är släckt', 'rep09ort',
        '2026-03-11 08:00:00.000', '2026-03-11 08:00:00.000', 'SO-26030002', false);

INSERT INTO errand_access_labels(errand_id, metadata_label_id)
VALUES ('ee000000-0000-0000-0000-0000000009s0', 'ss000000-0000-0000-0000-0000000009s1'),
       ('ee000000-0000-0000-0000-0000000009s2', 'ss000000-0000-0000-0000-0000000009s1');
