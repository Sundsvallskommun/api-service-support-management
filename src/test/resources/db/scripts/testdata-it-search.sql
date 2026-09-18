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
