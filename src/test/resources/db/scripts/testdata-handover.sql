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

-- A handover target whose classification labels sit under a root of their own (NAMESPACE-1 has none), used to preview
-- the label suggestions. CATEGORY-1/TYPE-1 matches the source exactly on the classification path; TYPE-2 of the source
-- is found under two other categories and, as CATEGORY-1, in the middle of the source path, all equally good.
INSERT INTO namespace_config(id, municipality_id, namespace, created, modified)
VALUES (98, '2281', 'NAMESPACE-HANDOVER', '2021-12-31 23:59:59.999', null);

INSERT INTO namespace_config_value(namespace_config_id, `key`, `value`, `type`)
VALUES (98, 'DISPLAY_NAME', 'Namespace handover', 'STRING'),
       (98, 'SHORT_CODE', 'NSH', 'STRING'),
       (98, 'NOTIFICATION_TTL_IN_DAYS', '10', 'INTEGER'),
       (98, 'ACCESS_CONTROL', 'false', 'BOOLEAN'),
       (98, 'NOTIFY_REPORTER', 'false', 'BOOLEAN');

INSERT INTO metadata_label (created, modified, municipality_id, namespace, classification, display_name, id, parent_id, resource_name, resource_path, deprecated) VALUES
    ('2025-10-16 09:45:49.815000', NULL, '2281', 'NAMESPACE-HANDOVER', 'CLASS', 'Rot', 'aa980000-0000-0000-0000-000000000001', NULL, 'ROOT-HANDOVER', 'ROOT-HANDOVER', false),
    ('2025-10-16 09:45:49.815000', NULL, '2281', 'NAMESPACE-HANDOVER', 'CATEGORY', 'Kategori 1', 'aa980000-0000-0000-0000-000000000002', 'aa980000-0000-0000-0000-000000000001', 'CATEGORY-1', 'ROOT-HANDOVER/CATEGORY-1', false),
    ('2025-10-16 09:45:49.815000', NULL, '2281', 'NAMESPACE-HANDOVER', 'TYPE', 'Typ 1', 'aa980000-0000-0000-0000-000000000003', 'aa980000-0000-0000-0000-000000000002', 'TYPE-1', 'ROOT-HANDOVER/CATEGORY-1/TYPE-1', false),
    ('2025-10-16 09:45:49.815000', NULL, '2281', 'NAMESPACE-HANDOVER', 'CATEGORY', 'Kategori A', 'aa980000-0000-0000-0000-000000000004', 'aa980000-0000-0000-0000-000000000001', 'CATEGORY-A', 'ROOT-HANDOVER/CATEGORY-A', false),
    ('2025-10-16 09:45:49.815000', NULL, '2281', 'NAMESPACE-HANDOVER', 'TYPE', 'Typ A2', 'aa980000-0000-0000-0000-000000000005', 'aa980000-0000-0000-0000-000000000004', 'TYPE-2', 'ROOT-HANDOVER/CATEGORY-A/TYPE-2', false),
    ('2025-10-16 09:45:49.815000', NULL, '2281', 'NAMESPACE-HANDOVER', 'CATEGORY', 'Kategori B', 'aa980000-0000-0000-0000-000000000006', 'aa980000-0000-0000-0000-000000000001', 'CATEGORY-B', 'ROOT-HANDOVER/CATEGORY-B', false),
    ('2025-10-16 09:45:49.815000', NULL, '2281', 'NAMESPACE-HANDOVER', 'TYPE', 'Typ B2', 'aa980000-0000-0000-0000-000000000007', 'aa980000-0000-0000-0000-000000000006', 'TYPE-2', 'ROOT-HANDOVER/CATEGORY-B/TYPE-2', false);

-- A source errand in NAMESPACE-1 classified by the labels CATEGORY-1/TYPE-1 and CATEGORY-1/TYPE-2, carrying a label on
-- every level of its classification
INSERT INTO errand(municipality_id, id, assigned_group_id, assigned_user_id, category, namespace,
                   priority, reporter_user_id, status, title, type, created, modified, resolution,
                   description, escalation_email, errand_number, business_related, previous_status, channel, touched)
VALUES ('2281', 'aa980000-0000-0000-0000-0000000000e1', null, null, 'CATEGORY-1', 'NAMESPACE-1', 'LOW', 'REPORTER_USER_ID-1',
        'STATUS-1', 'Labels to hand over', 'TYPE-1', '2024-01-01 12:00:00.000', null, null, null, null, 'KC-HANDOVER-1', false,
        null, null, '2024-01-01 12:00:00.000');

INSERT INTO errand_labels(errand_id, metadata_label_id)
VALUES ('aa980000-0000-0000-0000-0000000000e1', 'a8fe832f-77a7-4906-9a97-ac5cbd73dbe7'),
       ('aa980000-0000-0000-0000-0000000000e1', 'cd99569f-6b6d-4d7a-b04c-9ae528be8258'),
       ('aa980000-0000-0000-0000-0000000000e1', '3273e374-855c-4525-b8fc-aeaa710b83c5');
