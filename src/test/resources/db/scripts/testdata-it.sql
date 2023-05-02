-------------------------------------
-- Category and Type
-------------------------------------
INSERT INTO category(id, created, display_name, modified, municipality_id, name, namespace) VALUES
	(100, '2023-01-01 12:00:00.000', 'CATEGORY-DISPLAY-NAME-1', null, '2281', 'CATEGORY-1', 'NAMESPACE.1'),
	(101, '2023-01-01 12:00:00.000', 'CATEGORY-DISPLAY-NAME-2', null, '2281', 'CATEGORY-2', 'NAMESPACE.1'),
	(102, '2023-01-01 12:00:00.000', 'CATEGORY-DISPLAY-NAME-3', null, '2281', 'CATEGORY-3', 'NAMESPACE.1');

INSERT INTO type(id, created, display_name, escalation_email, modified, name, category_id)  VALUES
	(100, '2023-01-01 12:00:00.000', 'TYPE-DISPLAY-NAME-1', 'ESCALATION-EMAIL-1', null, 'TYPE-1', 100),
	(101, '2023-01-01 12:00:00.000', 'TYPE-DISPLAY-NAME-2', 'ESCALATION-EMAIL-2', null, 'TYPE-2', 100),
	(102, '2023-01-01 12:00:00.000', 'TYPE-DISPLAY-NAME-3', 'ESCALATION-EMAIL-3', null, 'TYPE-3', 100),
	(103, '2023-01-01 12:00:00.000', 'TYPE-DISPLAY-NAME-1', 'ESCALATION-EMAIL-1', null, 'TYPE-1', 101),
	(104, '2023-01-01 12:00:00.000', 'TYPE-DISPLAY-NAME-2', 'ESCALATION-EMAIL-2', null, 'TYPE-2', 101),
	(105, '2023-01-01 12:00:00.000', 'TYPE-DISPLAY-NAME-1', 'ESCALATION-EMAIL-1', null, 'TYPE-1', 102);
       
-------------------------------------
-- ExternalIdType
-------------------------------------
INSERT INTO external_id_type(id, created, modified, municipality_id, name, namespace)  VALUES
	(100, '2023-01-01 12:00:00.000', null, '2281', 'EXTERNAL-ID-TYPE-1', 'NAMESPACE.1'),
	(101, '2023-01-01 12:00:00.000', null, '2281', 'EXTERNAL-ID-TYPE-2', 'NAMESPACE.1'),
	(102, '2023-01-01 12:00:00.000', null, '2281', 'EXTERNAL-ID-TYPE-3', 'NAMESPACE.1'),
	(104, '2023-01-01 12:00:00.000', null, '2281', 'EXTERNAL-ID-TYPE-1', 'NAMESPACE.2'),
	(105, '2023-01-01 12:00:00.000', null, '2305', 'EXTERNAL-ID-TYPE-1', 'NAMESPACE.1'),
	(106, '2023-01-01 12:00:00.000', null, '2305', 'EXTERNAL-ID-TYPE-2', 'NAMESPACE.1');

-------------------------------------
-- Status
-------------------------------------
INSERT INTO status(id, created, modified, municipality_id, name, namespace) VALUES
	(100, '2023-01-01 12:00:00.000', null, '2281', 'STATUS-1', 'NAMESPACE.1'),
	(101, '2023-01-01 12:00:00.000', null, '2281', 'STATUS-2', 'NAMESPACE.1'),
	(102, '2023-01-01 12:00:00.000', null, '2281', 'STATUS-3', 'NAMESPACE.1'),
	(104, '2023-01-01 12:00:00.000', null, '2281', 'STATUS-1', 'NAMESPACE.2'),
	(105, '2023-01-01 12:00:00.000', null, '2305', 'STATUS-1', 'NAMESPACE.1'),
	(106, '2023-01-01 12:00:00.000', null, '2305', 'STATUS-2', 'NAMESPACE.1');

-------------------------------------
-- Role
-------------------------------------
INSERT INTO role(id, created, modified, municipality_id, name, namespace) VALUES
	(100, '2023-01-01 12:00:00.000', null, '2281', 'ROLE-1', 'NAMESPACE.1'),
	(101, '2023-01-01 12:00:00.000', null, '2281', 'ROLE-2', 'NAMESPACE.1'),
	(102, '2023-01-01 12:00:00.000', null, '2281', 'ROLE-3', 'NAMESPACE.1'),
	(104, '2023-01-01 12:00:00.000', null, '2281', 'ROLE-1', 'NAMESPACE.2'),
	(105, '2023-01-01 12:00:00.000', null, '2305', 'ROLE-1', 'NAMESPACE.1'),
	(106, '2023-01-01 12:00:00.000', null, '2305', 'ROLE-2', 'NAMESPACE.1');
       
-------------------------------------
-- Validation
-------------------------------------
INSERT INTO validation(id, municipality_id, namespace, `type`, created, modified, validated) VALUES
	(100, '2281', 'NAMESPACE.1', 'CATEGORY', '2023-01-01 12:00:00.000', null, true),
	(101, '2281', 'NAMESPACE.1', 'TYPE', '2023-01-01 12:00:00.000', null, false),
	(102, '2281', 'NAMESPACE.2', 'CATEGORY', '2023-01-01 12:00:00.000', null, false),
	(103, '2281', 'NAMESPACE.2', 'STATUS', '2023-01-01 12:00:00.000', null, true);

-------------------------------------
-- Errand
-------------------------------------
INSERT INTO errand(municipality_id, id, assigned_group_id, assigned_user_id, category, namespace, priority, reporter_user_id, status, title, type, created, modified, resolution, description, escalation_email) VALUES
	('2281', 'ec677eb3-604c-4935-bff7-f8f0b500c8f4', 'ASSIGNED_GROUP_ID-1', 'ASSIGNED_USER_ID-1', 'CATEGORY-1', 'NAMESPACE.1', 'LOW', 'REPORTER_USER_ID-1', 'STATUS-1', 'TITLE-1', 'TYPE-1', '2022-01-01 12:00:00.000', null, null, null, "ESCALATION_EMAIL_1"),
	('2281', 'cc236cf1-c00f-4479-8341-ecf5dd90b5b9', 'ASSIGNED_GROUP_ID-1', 'ASSIGNED_USER_ID-1', 'CATEGORY-1', 'NAMESPACE.1', 'LOW', 'REPORTER_USER_ID-1', 'STATUS-1', 'TITLE-1', 'TYPE-1', '2022-02-01 12:00:00.000', '2022-04-01 12:00:00.000', null, null, null),
	('2281', '1be673c0-6ba3-4fb0-af4a-43acf23389f6', 'ASSIGNED_GROUP_ID-3', 'ASSIGNED_USER_ID-3', 'CATEGORY-3', 'NAMESPACE.1', 'HIGH', 'REPORTER_USER_ID-3', 'STATUS-3', 'TITLE-3', 'TYPE-3', '2022-03-01 12:00:00.000', null, "RESOLUTION", "DESCRIPTION", null),
	('2281', 'f4a7a771-bb75-487b-b7d8-2684a0c3512c', 'ASSIGNED_GROUP_ID-3', 'ASSIGNED_USER_ID-3', 'CATEGORY-3', 'NAMESPACE.2', 'HIGH', 'REPORTER_USER_ID-3', 'STATUS-3', 'TITLE-3', 'TYPE-3', '2022-03-01 12:00:00.000', null, "RESOLUTION", "DESCRIPTION", null),
	('2305', 'e29906af-3083-4dcf-bb8a-d787ccf2dcc4', 'ASSIGNED_GROUP_ID-3', 'ASSIGNED_USER_ID-3', 'CATEGORY-3', 'NAMESPACE.1', 'HIGH', 'REPORTER_USER_ID-1', 'STATUS-3', 'TITLE-3', 'TYPE-3', '2022-03-01 12:00:00.000', null, null, null, null),
    ('2281', '147d355f-dc94-4fde-a4cb-9ddd16cb1946', 'hardware support', 'jane11dane', 'CATEGORY-1', 'NAMESPACE.1', 'HIGH', 'joe01doe', 'STATUS-1', 'It is my birthday', 'TYPE-1', '2023-04-26 15:48:17.124', '2023-04-26 16:05:08.806', 'FIXED', 'Order cake for everyone', 'joe.doe@email.com');

-------------------------------------
-- Stakeholder
------------------------------------
INSERT INTO stakeholder(id, external_id, external_id_type, errand_id, address, care_of, country, first_name, last_name, zip_code, role) VALUES
	('3001', 'USER_ID', 'EMPLOYEE', 'ec677eb3-604c-4935-bff7-f8f0b500c8f4', 'ADDRESS-1', 'CARE_OF-1','COUNTRY-1','FIRST_NAME-1', 'LAST_NAME-1', 'ZIP_CODE-1', 'ROLE-1'),
	('3002', '01e159b4-724d-40f9-91a4-9a7d021b8563', 'PRIVATE', 'ec677eb3-604c-4935-bff7-f8f0b500c8f4', 'ADDRESS-2', 'CARE_OF-2','COUNTRY-2','FIRST_NAME-1', 'LAST_NAME-1', 'ZIP_CODE-2', 'ROLE-2');

INSERT INTO stakeholder(id, external_id, external_id_type, errand_id) VALUES
	('3003', 'def57969-9b83-4e54-9351-667dc896a19d', 'PRIVATE', 'cc236cf1-c00f-4479-8341-ecf5dd90b5b9'),
	('3004', '7fba994b-e99e-4beb-8ab5-63cc74483b66', 'ENTERPRISE', '1be673c0-6ba3-4fb0-af4a-43acf23389f6'),
	('3005', '83793ee4-4b33-467a-ac56-570d6babcc5b', 'ENTERPRISE', 'f4a7a771-bb75-487b-b7d8-2684a0c3512c'),
	('3006', '76bf1aa0-6596-4dca-88ee-25d8a1a47e60', 'PRIVATE', 'e29906af-3083-4dcf-bb8a-d787ccf2dcc4');

INSERT INTO stakeholder(id, address, care_of, country, first_name, last_name, external_id, external_id_type, zip_code, errand_id, `role`) VALUES 
	('3008', '155 Country Lane, Cottington', 'Ford Prefect', 'United Kingdom', 'Aurthur', 'Dent', 'cb20c51f-fcf3-42c0-b613-de563634a8ec', 'PRIVATE', '12345', '147d355f-dc94-4fde-a4cb-9ddd16cb1946', 'CAKE-BAKER'),
	('3009', 'Northern skies', NULL, 'Norway', 'Slartibartfast', 'Magrathea', 'cb20c51f-fcf3-42c0-b613-de563634a8ec', 'PRIVATE', '23456', '147d355f-dc94-4fde-a4cb-9ddd16cb1946', 'CAKE-EATER');

-------------------------------------
-- ContactChannel
------------------------------------

INSERT INTO contact_channel(stakeholder_id, type, value) VALUES
	('3001', 'TYPE-1', 'VALUE-1');

-------------------------------------
-- ExternalTag
-------------------------------------
INSERT INTO external_tag(errand_id, `key`, `value`) VALUES 
	('ec677eb3-604c-4935-bff7-f8f0b500c8f4', 'KEY-1', 'VALUE-1'),
	('ec677eb3-604c-4935-bff7-f8f0b500c8f4', 'KEY-2', 'VALUE-2'),
	('cc236cf1-c00f-4479-8341-ecf5dd90b5b9', 'KEY-3', 'VALUE-3'),
	('1be673c0-6ba3-4fb0-af4a-43acf23389f6', 'KEY-4', 'VALUE-4'),
	('1be673c0-6ba3-4fb0-af4a-43acf23389f6', 'KEY-5', 'VALUE-5');

-------------------------------------
-- Attachment
-------------------------------------
INSERT INTO attachment(id, file, file_name, mime_type, errand_id) VALUES
	('25d266a7-1ff2-4bf4-b6f3-0473b2b86fcd', '89504E470D0A1A0A0000000D494844520000001000000010080200000090916836000000017352474200AECE1CE90000000467414D410000B18F0BFC6105000000097048597300000EC300000EC301C76FA8640000001E49444154384F6350DAE843126220493550F1A80662426C349406472801006AC91F1040F796BD0000000049454E44AE426082', 'Test_image.jpg', 'image/jpeg', 'ec677eb3-604c-4935-bff7-f8f0b500c8f4'),
	('c697642d-4d8d-4b07-8816-025a2734b09a', '89504E470D0A1A0A0000000D494844520000001000000010080200000090916836000000017352474200AECE1CE90000000467414D410000B18F0BFC6105000000097048597300000EC300000EC301C76FA8640000001E49444154384F6350DAE843126220493550F1A80662426C349406472801006AC91F1040F796BD0000000049454E44AE426082', 'Test.txt', 'text/plain', 'cc236cf1-c00f-4479-8341-ecf5dd90b5b9'),
	('c8d88089-5136-4a1a-aa10-5f435cb6e69f', '89504E470D0A1A0A0000000D494844520000001000000010080200000090916836000000017352474200AECE1CE90000000467414D410000B18F0BFC6105000000097048597300000EC300000EC301C76FA8640000001E49444154384F6350DAE843126220493550F1A80662426C349406472801006AC91F1040F796BD0000000049454E44AE426082', 'Test2.txt', 'text/plain', 'cc236cf1-c00f-4479-8341-ecf5dd90b5b9'),
	('99fa4dd0-9308-4d45-bb8e-4bb881a9a536', '89504E470D0A1A0A0000000D494844520000001000000010080200000090916836000000017352474200AECE1CE90000000467414D410000B18F0BFC6105000000097048597300000EC300000EC301C76FA8640000001E49444154384F6350DAE843126220493550F1A80662426C349406472801006AC91F1040F796BD0000000049454E44AE426082', 'Test3.txt', 'text/plain', '1be673c0-6ba3-4fb0-af4a-43acf23389f6'),
	('95ea267a-28ec-4636-922c-a717d79bd029', 'happybirthday', 'birthday-card.txt', 'text/plain', '147d355f-dc94-4fde-a4cb-9ddd16cb1946');

-------------------------------------
-- Revision
-------------------------------------
INSERT INTO revision(id, created, entity_id, entity_type, serialized_snapshot, version) VALUES 
	('59328e70-4297-4bb5-ba69-cb17f2d15a17', '2022-01-01 12:00:00.000', '1be673c0-6ba3-4fb0-af4a-43acf23389f6', 'ErrandEntity', '{"id":"1be673c0-6ba3-4fb0-af4a-43acf23389f6"}', 0),
	('84e0f78f-a857-4325-adff-04d2c0609a64', '2023-04-26 15:48:17.164', '147d355f-dc94-4fde-a4cb-9ddd16cb1946', 'ErrandEntity', '{"id":"147d355f-dc94-4fde-a4cb-9ddd16cb1946","externalTags":[{"key":"caseid","value":"2222-3333"}],"stakeholders":[{"id":3007,"externalId":"cb20c51f-fcf3-42c0-b613-de563634a8ec","externalIdType":"PRIVATE","role":"ADMINISTRATOR","firstName":"Aurthur","lastName":"Dent","address":"155 Country Lane, Cottington","careOf":"Ford Prefect","zipCode":"12345","country":"United Kingdom","contactChannels":[{"type":"Email","value":"arthur.dent@earth.com"}]}],"municipalityId":"2281","namespace":"NAMESPACE.1","title":"Title for the errand","category":"CATEGORY-1","type":"TYPE-1","status":"STATUS-1","resolution":"FIXED","description":"Order cake for everyone","priority":"LOW","reporterUserId":"joe01doe","assignedUserId":"joe01doe","assignedGroupId":"hardware support","escalationEmail":"joe.doe@email.com","created":"2023-04-26T15:48:17.124+02:00"}', 0),
	('b69b0c4a-4a43-4753-ab6d-f5b8eeca1dcd', '2023-04-26 16:05:08.805', '147d355f-dc94-4fde-a4cb-9ddd16cb1946', 'ErrandEntity', '{"id":"147d355f-dc94-4fde-a4cb-9ddd16cb1946","externalTags":[],"stakeholders":[{"id":3008,"externalId":"cb20c51f-fcf3-42c0-b613-de563634a8ec","externalIdType":"PRIVATE","role":"CAKE-BAKER","firstName":"Aurthur","lastName":"Dent","address":"155 Country Lane, Cottington","careOf":"Ford Prefect","zipCode":"12345","country":"United Kingdom","contactChannels":[{"type":"Email","value":"arthur.dent@earth.com"}]},{"id":3009,"externalId":"cb20c51f-fcf3-42c0-b613-de563634a8ec","externalIdType":"PRIVATE","role":"CAKE-EATER","firstName":"Slartibartfast","lastName":"Magrathea","address":"Northern skies","zipCode":"23456","country":"Norway","contactChannels":[{"type":"Email","value":"slartibartfast@earth.com"}]}],"municipalityId":"2281","namespace":"NAMESPACE.1","title":"It is my birthday","category":"CATEGORY-1","type":"TYPE-1","status":"STATUS-1","resolution":"FIXED","description":"Order cake for everyone","priority":"HIGH","reporterUserId":"joe01doe","assignedUserId":"jane11dane","assignedGroupId":"hardware support","escalationEmail":"joe.doe@email.com","attachments":[],"created":"2023-04-26T15:48:17.124+02:00","modified":"2023-04-26T16:05:08.795+02:00","touched":"2023-04-26T15:48:17.124+02:00"}', 1),
	('43a5b3c8-9010-4518-ab1b-d365bd7d6bb1', '2023-04-26 16:07:32.884', '147d355f-dc94-4fde-a4cb-9ddd16cb1946', 'ErrandEntity', '{"id":"147d355f-dc94-4fde-a4cb-9ddd16cb1946","externalTags":[],"stakeholders":[{"id":3008,"externalId":"cb20c51f-fcf3-42c0-b613-de563634a8ec","externalIdType":"PRIVATE","role":"CAKE-BAKER","firstName":"Aurthur","lastName":"Dent","address":"155 Country Lane, Cottington","careOf":"Ford Prefect","zipCode":"12345","country":"United Kingdom","contactChannels":[{"type":"Email","value":"arthur.dent@earth.com"}]},{"id":3009,"externalId":"cb20c51f-fcf3-42c0-b613-de563634a8ec","externalIdType":"PRIVATE","role":"CAKE-EATER","firstName":"Slartibartfast","lastName":"Magrathea","address":"Northern skies","zipCode":"23456","country":"Norway","contactChannels":[{"type":"Email","value":"slartibartfast@earth.com"}]}],"municipalityId":"2281","namespace":"NAMESPACE.1","title":"It is my birthday","category":"CATEGORY-1","type":"TYPE-1","status":"STATUS-1","resolution":"FIXED","description":"Order cake for everyone","priority":"HIGH","reporterUserId":"joe01doe","assignedUserId":"jane11dane","assignedGroupId":"hardware support","escalationEmail":"joe.doe@email.com","attachments":[{"id":"95ea267a-28ec-4636-922c-a717d79bd029","fileName":"birthday-card.txt","mimeType":"text/plain","file":[104,97,112,112,121,98,105,114,116,104,100,97,121],"created":"2023-04-26T16:07:32.874+02:00"}],"created":"2023-04-26T15:48:17.124+02:00","modified":"2023-04-26T16:05:08.806+02:00","touched":"2023-04-26T16:05:08.806+02:00"}', 2);

