-------------------------------------
-- Tag
-------------------------------------
INSERT INTO tag(id, type, name) VALUES(1, 'CATEGORY', 'CATEGORY-1');
INSERT INTO tag(id, type, name) VALUES(2, 'CATEGORY', 'CATEGORY-2');
INSERT INTO tag(id, type, name) VALUES(3, 'CATEGORY', 'CATEGORY-3');

INSERT INTO tag(id, type, name) VALUES(4, 'TYPE', 'TYPE-1');
INSERT INTO tag(id, type, name) VALUES(5, 'TYPE', 'TYPE-2');
INSERT INTO tag(id, type, name) VALUES(6, 'TYPE', 'TYPE-3');

INSERT INTO tag(id, type, name) VALUES(7, 'STATUS', 'STATUS-1');
INSERT INTO tag(id, type, name) VALUES(8, 'STATUS', 'STATUS-2');
INSERT INTO tag(id, type, name) VALUES(9, 'STATUS', 'STATUS-3');

-------------------------------------
-- Errand
-------------------------------------
INSERT INTO errand(municipality_id, id, assigned_group_id, assigned_user_id, category_tag, namespace, priority, reporter_user_id, status_tag, title, type_tag)
VALUES('2281', 'ERRAND_ID-1', 'ASSIGNED_GROUP_ID-1', 'ASSIGNED_USER_ID-1', 'CATEGORY-1', 'NAMESPACE.1', 'PRIORITY-1', 'REPORTER_USER_ID-1', 'STATUS-1', 'TITLE-1', 'TYPE-1'),
	  ('2281', 'ERRAND_ID-2', 'ASSIGNED_GROUP_ID-1', 'ASSIGNED_USER_ID-1', 'CATEGORY-1', 'NAMESPACE.1', 'PRIORITY-1', 'REPORTER_USER_ID-1', 'STATUS-1', 'TITLE-1', 'TYPE-1'),
	  ('2281', 'ERRAND_ID-3', 'ASSIGNED_GROUP_ID-3', 'ASSIGNED_USER_ID-3', 'CATEGORY-3', 'NAMESPACE.3', 'PRIORITY-3', 'REPORTER_USER_ID-3', 'STATUS-3', 'TITLE-3', 'TYPE-3'),
	  ('2305', 'ERRAND_ID-4', 'ASSIGNED_GROUP_ID-3', 'ASSIGNED_USER_ID-3', 'CATEGORY-3', 'NAMESPACE.3', 'PRIORITY-3', 'REPORTER_USER_ID-3', 'STATUS-3', 'TITLE-3', 'TYPE-3');

-------------------------------------
-- Stakeholder
------------------------------------
INSERT INTO stakeholder(id, external_id, external_id_type_tag, errand_id, address, care_of, country, first_name, last_name, zip_code) VALUES
	('3001', 'EXTERNAL_ID-1', 'EMPLOYEE', 'ERRAND_ID-1', 'ADDRESS-1', 'CARE_OF-1','COUNTRY-1','FIRST_NAME-1', 'LAST_NAME-1', 'ZIP_CODE-1');
INSERT INTO stakeholder(id, external_id, external_id_type_tag, errand_id) VALUES
	('3002', 'EXTERNAL_ID-2', 'PRIVATE', 'ERRAND_ID-2'),
	('3003', 'EXTERNAL_ID-3', 'ENTERPRISE', 'ERRAND_ID-3'),
	('3004', 'EXTERNAL_ID-3', 'ENTERPRISE', 'ERRAND_ID-4');

-------------------------------------
-- ContactChannel
------------------------------------

INSERT INTO contact_channel(stakeholder_id, type, value) VALUES('3001', 'TYPE-1', 'VALUE-1');

-------------------------------------
-- ExternalTag
-------------------------------------
INSERT INTO external_tag(errand_id, `key`, `value`) VALUES ('ERRAND_ID-1', 'KEY-1', 'VALUE-1');
INSERT INTO external_tag(errand_id, `key`, `value`) VALUES ('ERRAND_ID-1', 'KEY-2', 'VALUE-2');
INSERT INTO external_tag(errand_id, `key`, `value`) VALUES ('ERRAND_ID-2', 'KEY-3', 'VALUE-3');
INSERT INTO external_tag(errand_id, `key`, `value`) VALUES ('ERRAND_ID-3', 'KEY-4', 'VALUE-4');
INSERT INTO external_tag(errand_id, `key`, `value`) VALUES ('ERRAND_ID-3', 'KEY-5', 'VALUE-5');

-------------------------------------
-- Attachment
-------------------------------------
INSERT INTO attachment(id, file, file_name, mime_type, errand_id)
VALUES('ATTACHMENT_ID-1', '89504E470D0A1A0A0000000D494844520000001000000010080200000090916836000000017352474200AECE1CE90000000467414D410000B18F0BFC6105000000097048597300000EC300000EC301C76FA8640000001E49444154384F6350DAE843126220493550F1A80662426C349406472801006AC91F1040F796BD0000000049454E44AE426082',
'Test_image.jpg', 'image/jpeg', 'ERRAND_ID-1');
INSERT INTO attachment(id, file, file_name, mime_type, errand_id)
VALUES('ATTACHMENT_ID-2', '89504E470D0A1A0A0000000D494844520000001000000010080200000090916836000000017352474200AECE1CE90000000467414D410000B18F0BFC6105000000097048597300000EC300000EC301C76FA8640000001E49444154384F6350DAE843126220493550F1A80662426C349406472801006AC91F1040F796BD0000000049454E44AE426082',
'Test.txt', 'text/plain', 'ERRAND_ID-2');
INSERT INTO attachment(id, file, file_name, mime_type, errand_id)
VALUES('ATTACHMENT_ID-3', '89504E470D0A1A0A0000000D494844520000001000000010080200000090916836000000017352474200AECE1CE90000000467414D410000B18F0BFC6105000000097048597300000EC300000EC301C76FA8640000001E49444154384F6350DAE843126220493550F1A80662426C349406472801006AC91F1040F796BD0000000049454E44AE426082',
'Test2.txt', 'text/plain', 'ERRAND_ID-2');
