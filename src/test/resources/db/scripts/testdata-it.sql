-------------------------------------
-- Tag
-------------------------------------
INSERT INTO tag(id, type, name) VALUES
      (1, 'CATEGORY', 'CATEGORY-1'),
      (2, 'CATEGORY', 'CATEGORY-2'),
      (3, 'CATEGORY', 'CATEGORY-3'),
      (4, 'TYPE', 'TYPE-1'),
      (5, 'TYPE', 'TYPE-2'),
      (6, 'TYPE', 'TYPE-3'),
      (7, 'STATUS', 'STATUS-1'),
      (8, 'STATUS', 'STATUS-2'),
      (9, 'STATUS', 'STATUS-3'),
      (10, 'CLIENT_ID', 'CLIENT_ID-1'),
      (11, 'CLIENT_ID', 'CLIENT_ID-2'),
      (12, 'CLIENT_ID', 'CLIENT_ID-3');

-------------------------------------
-- Errand
-------------------------------------
INSERT INTO errand(id, assigned_group_id, assigned_user_id, category_tag, customer_id, customer_type, client_id_tag, priority, reporter_user_id, status_tag, title, type_tag, created, modified) VALUES
      ('ec677eb3-604c-4935-bff7-f8f0b500c8f4', 'ASSIGNED_GROUP_ID-1', 'ASSIGNED_USER_ID-1', 'CATEGORY-1', 'CUSTOMER_ID-1', 'EMPLOYEE', 'CLIENT_ID-1', 'LOW', 'REPORTER_USER_ID-1', 'STATUS-1', 'TITLE-1', 'TYPE-1', '2022-01-01 12:00:00.000', null),
      ('cc236cf1-c00f-4479-8341-ecf5dd90b5b9', 'ASSIGNED_GROUP_ID-1', 'ASSIGNED_USER_ID-1', 'CATEGORY-1', 'CUSTOMER_ID-2', 'PRIVATE', 'CLIENT_ID-1', 'LOW', 'REPORTER_USER_ID-1', 'STATUS-1', 'TITLE-1', 'TYPE-1', '2022-02-01 12:00:00.000', '2022-04-01 12:00:00.000'),
      ('1be673c0-6ba3-4fb0-af4a-43acf23389f6', 'ASSIGNED_GROUP_ID-3', 'ASSIGNED_USER_ID-3', 'CATEGORY-3', 'CUSTOMER_ID-3', 'ENTERPRISE', 'CLIENT_ID-3', 'HIGH', 'REPORTER_USER_ID-3', 'STATUS-3', 'TITLE-3', 'TYPE-3', '2022-03-01 12:00:00.000', null);

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
      ('99fa4dd0-9308-4d45-bb8e-4bb881a9a536', '89504E470D0A1A0A0000000D494844520000001000000010080200000090916836000000017352474200AECE1CE90000000467414D410000B18F0BFC6105000000097048597300000EC300000EC301C76FA8640000001E49444154384F6350DAE843126220493550F1A80662426C349406472801006AC91F1040F796BD0000000049454E44AE426082', 'Test3.txt', 'text/plain', '1be673c0-6ba3-4fb0-af4a-43acf23389f6');
