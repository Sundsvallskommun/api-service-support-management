-------------------------------------
-- Category and Type
-------------------------------------
INSERT INTO category(id, created, display_name, modified, municipality_id, name, namespace) 
VALUES (100, now(), 'category-display-name-1', null, '2281', 'category-1', 'namespace-1'),
       (101, now(), 'category-display-name-2', null, '2281', 'category-2', 'namespace-1'),
       (102, now(), 'category-display-name-3', null, '2281', 'category-3', 'namespace-1'),
       (104, now(), 'category-display-name-1', null, '2281', 'category-1', 'namespace-2'),
       (105, now(), 'category-display-name-1', null, '2282', 'category-1', 'namespace-1');

INSERT INTO type(id, created, display_name, escalation_email, modified, name, category_id) 
VALUES (100, now(), 'type-display-name-1', 'escalation-email-1', null, 'type-1', 100),
       (101, now(), 'type-display-name-2', 'escalation-email-2', null, 'type-2', 100),
       (102, now(), 'type-display-name-3', 'escalation-email-3', null, 'type-3', 100),
       (103, now(), 'type-display-name-1', 'escalation-email-1', null, 'type-1', 101),
       (104, now(), 'type-display-name-2', 'escalation-email-2', null, 'type-2', 101),
       (105, now(), 'type-display-name-1', 'escalation-email-1', null, 'type-1', 102);
       
-------------------------------------
-- ExternalIdType
-------------------------------------
INSERT INTO external_id_type(id, created, modified, municipality_id, name, namespace) 
VALUES (100, now(), null, '2281', 'external-id-type-1', 'namespace-1'),
       (101, now(), null, '2281', 'external-id-type-2', 'namespace-1'),
       (102, now(), null, '2281', 'external-id-type-3', 'namespace-1'),
       (104, now(), null, '2281', 'external-id-type-1', 'namespace-2'),
       (105, now(), null, '2282', 'external-id-type-1', 'namespace-1'),
       (106, now(), null, '2282', 'external-id-type-2', 'namespace-1');
 
-------------------------------------
-- Status
-------------------------------------
INSERT INTO status(id, created, modified, municipality_id, name, namespace) 
VALUES (100, now(), null, '2281', 'status-1', 'namespace-1'),
       (101, now(), null, '2281', 'status-2', 'namespace-1'),
       (102, now(), null, '2281', 'status-3', 'namespace-1'),
       (104, now(), null, '2281', 'status-1', 'namespace-2'),
       (105, now(), null, '2282', 'status-1', 'namespace-1'),
       (106, now(), null, '2282', 'status-2', 'namespace-1');

-------------------------------------
-- Role
-------------------------------------
INSERT INTO role(id, created, modified, municipality_id, name, namespace) 
VALUES (100, now(), null, '2281', 'role-1', 'namespace-1'),
       (101, now(), null, '2281', 'role-2', 'namespace-1'),
       (102, now(), null, '2281', 'role-3', 'namespace-1'),
       (104, now(), null, '2281', 'role-1', 'namespace-2'),
       (105, now(), null, '2282', 'role-1', 'namespace-1'),
       (106, now(), null, '2282', 'role-2', 'namespace-1');
       
-------------------------------------
-- Validation
-------------------------------------
INSERT INTO validation(id, municipality_id, namespace, `type`, created, modified, validated)
VALUES (100, '2281', 'namespace-1', 'CATEGORY', now(), null, true),
       (101, '2281', 'namespace-1', 'TYPE', now(), null, false),
       (102, '2281', 'namespace-2', 'CATEGORY', now(), null, false),
       (103, '2281', 'namespace-2', 'STATUS', now(), null, true);
       
-------------------------------------
-- Errand
-------------------------------------
INSERT INTO errand(municipality_id, id, assigned_group_id, assigned_user_id, category, namespace, priority, reporter_user_id, status, title, type, escalation_email, errand_number)
VALUES('2281', 'ERRAND_ID-1', 'ASSIGNED_GROUP_ID-1', 'ASSIGNED_USER_ID-1', 'CATEGORY-1', 'NAMESPACE.1', 'PRIORITY-1', 'REPORTER_USER_ID-1', 'STATUS-1', 'TITLE-1', 'TYPE-1', 'ESCALATION_EMAIL_1', 'KC-23020001'),
	  ('2281', 'ERRAND_ID-2', 'ASSIGNED_GROUP_ID-1', 'ASSIGNED_USER_ID-1', 'CATEGORY-1', 'NAMESPACE.1', 'PRIORITY-1', 'REPORTER_USER_ID-1', 'STATUS-1', 'TITLE-1', 'TYPE-1', 'ESCALATION_EMAIL_2','KC-23020002'),
	  ('2281', 'ERRAND_ID-3', 'ASSIGNED_GROUP_ID-3', 'ASSIGNED_USER_ID-3', 'CATEGORY-3', 'NAMESPACE.3', 'PRIORITY-3', 'REPORTER_USER_ID-3', 'STATUS-3', 'TITLE-3', 'TYPE-3', 'ESCALATION_EMAIL_3','KC-23020003'),
	  ('2305', 'ERRAND_ID-4', 'ASSIGNED_GROUP_ID-3', 'ASSIGNED_USER_ID-3', 'CATEGORY-3', 'NAMESPACE.3', 'PRIORITY-3', 'REPORTER_USER_ID-3', 'STATUS-3', 'TITLE-3', 'TYPE-3', 'ESCALATION_EMAIL_4','KC-23020004');

-------------------------------------
-- Stakeholder
------------------------------------
INSERT INTO stakeholder(id, external_id, external_id_type, errand_id, address, care_of, country, first_name, last_name, zip_code, role) VALUES
	('3001', 'EXTERNAL_ID-1', 'EMPLOYEE', 'ERRAND_ID-1', 'ADDRESS-1', 'CARE_OF-1','COUNTRY-1','FIRST_NAME-1', 'LAST_NAME-1', 'ZIP_CODE-1', 'ROLE-1');
INSERT INTO stakeholder(id, external_id, external_id_type, errand_id) VALUES
	('3002', 'EXTERNAL_ID-2', 'PRIVATE', 'ERRAND_ID-2'),
	('3003', 'EXTERNAL_ID-3', 'ENTERPRISE', 'ERRAND_ID-3'),
	('3004', 'EXTERNAL_ID-3', 'ENTERPRISE', 'ERRAND_ID-4');

-------------------------------------
-- ContactChannel
------------------------------------
INSERT INTO contact_channel(stakeholder_id, type, value) VALUES('3001', 'TYPE-1', 'VALUE-1');

-------------------------------------
-- StakeholderParameter
------------------------------------
INSERT INTO stakeholder_parameter(id, stakeholder_id, display_name, parameters_key) VALUES(201, 3001, 'Parameter X', 'parameterX');
INSERT INTO stakeholder_parameter_values(stakeholder_parameter_id, `value`) VALUES(201, 'value-x1');
INSERT INTO stakeholder_parameter_values(stakeholder_parameter_id, `value`) VALUES(201, 'value-x2');

-------------------------------------
-- ExternalTag
-------------------------------------
INSERT INTO external_tag(errand_id, `key`, `value`) VALUES ('ERRAND_ID-1', 'KEY-1', 'VALUE-1');
INSERT INTO external_tag(errand_id, `key`, `value`) VALUES ('ERRAND_ID-1', 'KEY-2', 'VALUE-2');
INSERT INTO external_tag(errand_id, `key`, `value`) VALUES ('ERRAND_ID-2', 'KEY-2', 'VALUE-2');
INSERT INTO external_tag(errand_id, `key`, `value`) VALUES ('ERRAND_ID-2', 'KEY-3', 'VALUE-3');
INSERT INTO external_tag(errand_id, `key`, `value`) VALUES ('ERRAND_ID-3', 'KEY-4', 'VALUE-4');
INSERT INTO external_tag(errand_id, `key`, `value`) VALUES ('ERRAND_ID-3', 'KEY-5', 'VALUE-5');

-------------------------------------
-- Attachment Data
-------------------------------------
INSERT INTO attachment_data(id, file) VALUES
    ('1', '89504E470D0A1A0A0000000D494844520000001000000010080200000090916836000000017352474200AECE1CE90000000467414D410000B18F0BFC6105000000097048597300000EC300000EC301C76FA8640000001E49444154384F6350DAE843126220493550F1A80662426C349406472801006AC91F1040F796BD0000000049454E44AE426082'),
    ('2', '89504E470D0A1A0A0000000D494844520000001000000010080200000090916836000000017352474200AECE1CE90000000467414D410000B18F0BFC6105000000097048597300000EC300000EC301C76FA8640000001E49444154384F6350DAE843126220493550F1A80662426C349406472801006AC91F1040F796BD0000000049454E44AE4260822'),
    ('3', '89504E470D0A1A0A0000000D494844520000001000000010080200000090916836000000017352474200AECE1CE90000000467414D410000B18F0BFC6105000000097048597300000EC300000EC301C76FA8640000001E49444154384F6350DAE843126220493550F1A80662426C349406472801006AC91F1040F796BD0000000049454E44AE426082');

-------------------------------------
-- Attachment
-------------------------------------
INSERT INTO attachment(id, attachment_data_id, file_name, mime_type, errand_id) VALUES
    ('ATTACHMENT_ID-1', '1', 'Test_image.jpg', 'image/jpeg', 'ERRAND_ID-1'),
    ('ATTACHMENT_ID-2', '2', 'Test.txt', 'text/plain', 'ERRAND_ID-2'),
    ('ATTACHMENT_ID-3', '3', 'Test2.txt', 'text/plain', 'ERRAND_ID-2');

-------------------------------------
-- Revision
-------------------------------------
INSERT INTO revision(id, entity_id, entity_type, serialized_snapshot, version, created, namespace, municipality_id)
VALUES ('59328e70-4297-4bb5-ba69-cb17f2d15a17', '9791682e-4ba8-4f3a-857a-54e14836a53b', 'ErrandEntity', '{}', 1, '2022-01-01 12:14:32.234','namespace', '2281'),
       ('5ac0398d-67d7-4267-b7b1-d9983b51758b', '9791682e-4ba8-4f3a-857a-54e14836a53b', 'ErrandEntity', '{}', 2, '2022-02-02 12:14:32.234','namespace', '2281'),
       ('207ef370-607b-4502-9d16-bf38defb1dfd', '9791682e-4ba8-4f3a-857a-54e14836a53b', 'ErrandEntity', '{}', 3, '2022-02-03 12:14:32.234','namespace', '2281'),
       ('f9e222f3-2476-4ead-bb1a-3e7e25f9c6ee', '9791682e-4ba8-4f3a-857a-54e14836a53b', 'ErrandEntity', '{}', 4, '2022-02-04 12:14:32.234','namespace', '2281'),
       ('203c924b-dd67-4802-b99f-256ef6f2de69', '9791682e-4ba8-4f3a-857a-54e14836a53b', 'ErrandEntity', '{}', 5, '2022-02-05 12:14:32.234','namespace', '2281');

-------------------------------------
-- MetadataLabel
-------------------------------------
INSERT INTO metadata_label (id, created, municipality_id, namespace, resource_path, resource_name, parent_id)
VALUES 
  -- 2281 / namespace-1
  ('9b2a5a7d-3e8f-4a7c-8f9b-2f7d3c1a0b11', now(), '2281', 'namespace-1', 'parent', 'parent', NULL),
  ('f3a1c6d9-7b2e-4d5a-9c1f-8e2b2c3d4e5f', now(), '2281', 'namespace-1', 'parent/child', 'child', '9b2a5a7d-3e8f-4a7c-8f9b-2f7d3c1a0b11'),
  ('a0bb7b61-8d55-4857-b619-547572eed26f', now(), '2281', 'namespace-1', 'parent/child/resource1', 'resource1', 'f3a1c6d9-7b2e-4d5a-9c1f-8e2b2c3d4e5f'),
  ('86d459cd-4810-4b4a-b365-97aa0c2c0ff5', now(), '2281', 'namespace-1', 'parent/child/resource2', 'resource2', 'f3a1c6d9-7b2e-4d5a-9c1f-8e2b2c3d4e5f'),
  -- 2282 / namespace-1
  ('0c5d8a12-6b74-4e39-9f0a-1b2c3d4e5f60', now(), '2282', 'namespace-1', 'parent', 'parent', NULL),
  ('1a2b3c4d-5e6f-7081-92a3-b4c5d6e7f801', now(), '2282', 'namespace-1', 'parent/child', 'child', '0c5d8a12-6b74-4e39-9f0a-1b2c3d4e5f60'),
  ('821033d0-f059-4f2b-90f2-cc0562ac0560', now(), '2282', 'namespace-1', 'parent/child/resource3', 'resource3', '1a2b3c4d-5e6f-7081-92a3-b4c5d6e7f801'),
  -- 2283 / namespace-1
  ('2b4c6d8e-0f12-4a3b-9c5d-6e7f8012a3b4', now(), '2283', 'namespace-1', 'parent', 'parent', NULL),
  ('3c5d7e9f-1021-4b3c-8d6e-7f8012a3b4c5', now(), '2283', 'namespace-1', 'parent/child', 'child', '2b4c6d8e-0f12-4a3b-9c5d-6e7f8012a3b4'),
  ('4bee7529-b904-4559-97ae-0437f74de935', now(), '2283', 'namespace-1', 'parent/child/resource4', 'resource4', '3c5d7e9f-1021-4b3c-8d6e-7f8012a3b4c5'),
  -- 2285 / namespace-123 (egen rot)
  ('8fa9beb8-944d-41c1-884a-c0f6db4f7bf6', now(), '2285', 'namespace-123', 'parent', 'parent', NULL);
  
-------------------------------------
-- Communication
-------------------------------------
INSERT INTO communication(internal,viewed, sender, sender_user_id, sent, id, errand_number, external_id,
                          message_body, target, subject, direction, type)
VALUES (1, 0,'Test Testorsson', 'userId', '2023-01-01 12:00:00.000', 'comm1', 'errand1', 'case1',
        'message body 1', '1234567890', 'subject1', 'INBOUND', 'SMS'),
       (0,1, 'Test Testorsson','userId', '2023-01-02 12:00:00.000', 'comm2',  'errand2', 'case2',
        'message body 2', '0987654321', 'subject2', 'OUTBOUND', 'EMAIL');

-------------------------------------
-- Communication_attachment
-------------------------------------
INSERT INTO communication_attachment(attachment_data_id, id,
                                     communication_id, mime_type, file_name)
VALUES (1, 'attach1', 'comm1', 'text/plain', 'attachment1'),
       (2, 'attach2', 'comm2', 'image/png', 'attachment2');

-------------------------------------
-- Notification
-------------------------------------
INSERT INTO notification(acknowledged, global_acknowledged, created, expires, modified, content, created_by, description, errand_id, id, municipality_id, namespace, owner_full_name, owner_id, type)
VALUES	(0, 0, now(), '2023-12-31 23:59:59.999', now(), 'content-1', 'created_by-1', 'description-1', 'ERRAND_ID-1', '1', '2281', 'namespace-1', 'owner_full_name-1', 'owner_id-1', 'type-1'),
		(0, 0, now(), '2024-12-31 23:59:59.999', now(), 'content-2', 'created_by-2', 'description-2', 'ERRAND_ID-2', '2', '2281', 'namespace-1', 'owner_full_name-2', 'owner_id-2', 'type-2'),
		(1, 0, now(), '2024-12-31 23:59:59.999', now(), 'content-3', 'created_by-3', 'description-3', 'ERRAND_ID-3', '3', '2281', 'namespace-1', 'owner_full_name-3', 'owner_id-3', 'type-3');

-------------------------------------
-- EmailWorkerConfig
-------------------------------------
INSERT INTO email_worker_config(days_of_inactivity_before_reject, enabled, created, id, modified,
                                errand_closed_email_sender, errand_closed_email_template, inactive_status,
                                municipality_id, namespace, status_change_to, status_for_new, trigger_status_change_on,
                                add_sender_as_stakeholder, stakeholder_role, errand_channel)
VALUES (1, true, '2020-10-10 10:10:59.999', 1, '2021-11-11 11:11:59.999',
        'sender-1', 'template-1', 'inactive_status-1',
        '2281', 'namespace-1', 'status_change_to-1', 'status_for_new-1', 'trigger_status_change_on-1', true, 'role-1', 'channel-1'),
       (2, false, '2022-02-02 12:12:59.999', 2, '2023-03-03 13:13:59.999',
       'sender-2', 'template-2', 'inactive_status-2',
       '2282', 'namespace-2', 'status_change_to-2', 'status_for_new-2', 'trigger_status_change_on-2', true, 'role-2', 'channel-2');

-------------------------------------
-- NamespaceConfig
-------------------------------------

INSERT INTO namespace_config(id, municipality_id, namespace, created, modified)
VALUES (1, '2281', 'namespace-1', '2021-01-01 11:11:59.999', '2022-01-01 11:11:59.999'),
       (2, '2281', 'namespace-3', '2022-03-03 13:13:59.999', '2023-03-03 13:13:59.999'),
       (3, '2282', 'namespace-2', '2022-02-02 12:12:59.999', '2023-02-02 12:12:59.999');

INSERT INTO namespace_config_value(namespace_config_id, `key`, `value`, `type`)
VALUES (1, 'DISPLAY_NAME', 'display name 1', 'STRING'),
       (1, 'SHORT_CODE', 'short_code-1', 'STRING'),
       (1, 'NOTIFICATION_TTL_IN_DAYS', '10', 'INTEGER'),
       (1, 'ACCESS_CONTROL', 'false', 'BOOLEAN'),
       (2, 'DISPLAY_NAME', 'display name 3', 'STRING'),
       (2, 'SHORT_CODE', 'short_code-3', 'STRING'),
       (2, 'NOTIFICATION_TTL_IN_DAYS', '30', 'INTEGER'),
       (2, 'ACCESS_CONTROL', 'true', 'BOOLEAN'),
       (3, 'DISPLAY_NAME', 'display name 2', 'STRING'),
       (3, 'SHORT_CODE', 'short_code-2', 'STRING'),
       (3, 'NOTIFICATION_TTL_IN_DAYS', '20', 'INTEGER'),
       (3, 'ACCESS_CONTROL', 'false', 'BOOLEAN');

-------------------------------------
-- WebMessageCollect
-------------------------------------

INSERT INTO web_message_collect(id, municipality_id, namespace, instance)
VALUES (1, '2281', 'namespace-1', 'instance-1'),
       (2, '2281', 'namespace-1', 'instance-2'),
       (3, '2281', 'namespace-2', 'instance-1');

INSERT INTO web_message_collect_family_ids(web_message_collect_id, family_id)
VALUES (1, 'family_id-1'),
       (1, 'family_id-2'),
       (2, 'family_id-3'),
       (2, 'family_id-4');


-------------------------------------
-- Conversation
-------------------------------------

INSERT INTO conversation(id, message_exchange_id, errand_id, namespace, municipality_id, topic, type, latest_synced_sequence_number)
VALUES ('1', 'message_exchange_id-1', 'errand_id-1', 'namespace-1', '2281', 'topic-1', 'type-1', 1),
       ('2', 'message_exchange_id-2', 'errand_id-1', 'namespace-1', '2281', 'topic-2', 'type-2', 2),
       ('3', 'message_exchange_id-3', 'errand_id-3', 'namespace-1', '2281', 'topic-3', 'type-3', 4);

INSERT INTO conversation_relation_id(conversation_id, relation_id)
VALUES ('1', 'relation_id-1-1'),
       ('1', 'relation_id-1-2'),
       ('2', 'relation_id-2-1'),
       ('2', 'relation_id-2-2');

-------------------------------------
-- MessageExchangeSync
-------------------------------------
INSERT INTO message_exchange_sync(id, municipality_id, namespace, active)
VALUES ('1', '2281', 'namespace-1', true),
       ('2', '2281', 'namespace-2', false);
