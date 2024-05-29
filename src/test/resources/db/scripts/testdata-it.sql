-------------------------------------
-- Category and Type
-------------------------------------
INSERT INTO category(id, created, display_name, modified, municipality_id, name, namespace)
VALUES (100, '2023-01-01 12:00:00.000', 'CATEGORY-DISPLAY-NAME-1', null, '2281', 'CATEGORY-1',
        'NAMESPACE.1'),
       (101, '2023-01-01 12:00:00.000', 'CATEGORY-DISPLAY-NAME-2', null, '2281', 'CATEGORY-2',
        'NAMESPACE.1'),
       (102, '2023-01-01 12:00:00.000', 'CATEGORY-DISPLAY-NAME-3', null, '2281', 'CATEGORY-3',
        'NAMESPACE.1');

INSERT INTO type(id, created, display_name, escalation_email, modified, name, category_id)
VALUES (100, '2023-01-01 12:00:00.000', 'TYPE-DISPLAY-NAME-1', 'ESCALATION-EMAIL-1', null, 'TYPE-1',
        100),
       (101, '2023-01-01 12:00:00.000', 'TYPE-DISPLAY-NAME-2', 'ESCALATION-EMAIL-2', null, 'TYPE-2',
        100),
       (102, '2023-01-01 12:00:00.000', 'TYPE-DISPLAY-NAME-3', 'ESCALATION-EMAIL-3', null, 'TYPE-3',
        100),
       (103, '2023-01-01 12:00:00.000', 'TYPE-DISPLAY-NAME-1', 'ESCALATION-EMAIL-1', null, 'TYPE-1',
        101),
       (104, '2023-01-01 12:00:00.000', 'TYPE-DISPLAY-NAME-2', 'ESCALATION-EMAIL-2', null, 'TYPE-2',
        101),
       (105, '2023-01-01 12:00:00.000', 'TYPE-DISPLAY-NAME-1', 'ESCALATION-EMAIL-1', null, 'TYPE-1',
        102);

-------------------------------------
-- ExternalIdType
-------------------------------------
INSERT INTO external_id_type(id, created, modified, municipality_id, name, namespace)
VALUES (100, '2023-01-01 12:00:00.000', null, '2281', 'EXTERNAL-ID-TYPE-1', 'NAMESPACE.1'),
       (101, '2023-01-01 12:00:00.000', null, '2281', 'EXTERNAL-ID-TYPE-2', 'NAMESPACE.1'),
       (102, '2023-01-01 12:00:00.000', null, '2281', 'EXTERNAL-ID-TYPE-3', 'NAMESPACE.1'),
       (104, '2023-01-01 12:00:00.000', null, '2281', 'EXTERNAL-ID-TYPE-1', 'NAMESPACE.2'),
       (105, '2023-01-01 12:00:00.000', null, '2305', 'EXTERNAL-ID-TYPE-1', 'NAMESPACE.1'),
       (106, '2023-01-01 12:00:00.000', null, '2305', 'EXTERNAL-ID-TYPE-2', 'NAMESPACE.1');

-------------------------------------
-- Status
-------------------------------------
INSERT INTO status(id, created, modified, municipality_id, name, namespace)
VALUES (100, '2023-01-01 12:00:00.000', null, '2281', 'STATUS-1', 'NAMESPACE.1'),
       (101, '2023-01-01 12:00:00.000', null, '2281', 'STATUS-2', 'NAMESPACE.1'),
       (102, '2023-01-01 12:00:00.000', null, '2281', 'STATUS-3', 'NAMESPACE.1'),
       (104, '2023-01-01 12:00:00.000', null, '2281', 'STATUS-1', 'NAMESPACE.2'),
       (105, '2023-01-01 12:00:00.000', null, '2305', 'STATUS-1', 'NAMESPACE.1'),
       (106, '2023-01-01 12:00:00.000', null, '2305', 'STATUS-2', 'NAMESPACE.1'),
       (107, '2023-01-01 12:00:00.000', null, '2281', 'STATUS-2', 'CONTACTCENTER'),
       (108, '2023-01-01 12:00:00.000', null, '2281', 'STATUS-1', 'NAMESPACE.3'),
       (109, '2023-01-01 12:00:00.000', null, '2281', 'STATUS-2', 'NAMESPACE.3'),
       (110, '2023-01-01 12:00:00.000', null, '2281', 'STATUS-3', 'NAMESPACE.3'),
       (111, '2023-01-01 12:00:00.000', null, '2281', 'STATUS-4', 'NAMESPACE.3');
-------------------------------------
-- Role
-------------------------------------
INSERT INTO role(id, created, modified, municipality_id, name, namespace)
VALUES (100, '2023-01-01 12:00:00.000', null, '2281', 'ROLE-1', 'NAMESPACE.1'),
       (101, '2023-01-01 12:00:00.000', null, '2281', 'ROLE-2', 'NAMESPACE.1'),
       (102, '2023-01-01 12:00:00.000', null, '2281', 'ROLE-3', 'NAMESPACE.1'),
       (104, '2023-01-01 12:00:00.000', null, '2281', 'ROLE-1', 'NAMESPACE.2'),
       (105, '2023-01-01 12:00:00.000', null, '2305', 'ROLE-1', 'NAMESPACE.1'),
       (106, '2023-01-01 12:00:00.000', null, '2305', 'ROLE-2', 'NAMESPACE.1');

-------------------------------------
-- Label
-------------------------------------
INSERT INTO label(id, created, municipality_id, namespace, json_structure)
VALUES (1, '2023-01-01 12:00:00.000', '2281', 'NAMESPACE.1',
        '[{"classification":"CATEGORY","displayName":"CATEGORY-DISPLAY-NAME-1","name":"CATEGORY-1","labels":[{"classification":"TYPE","displayName":"TYPE-DISPLAY-NAME-1","name":"TYPE-1","labels":[{"classification":"SUBTYPE","displayName":"SUBTYPE-DISPLAY-NAME-1","name":"SUBTYPE-1"},{"classification":"SUBTYPE","displayName":"SUBTYPE-DISPLAY-NAME-2","name":"SUBTYPE-2"}]},{"classification":"TYPE","displayName":"TYPE-DISPLAY-NAME-2","name":"TYPE-2","labels":[{"classification":"SUBTYPE","displayName":"SUBTYPE-DISPLAY-NAME-1","name":"SUBTYPE-1"},{"classification":"SUBTYPE","displayName":"SUBTYPE-DISPLAY-NAME-3","name":"SUBTYPE-3","labels":[{"classification":"DEEPSUBTYPE","displayName":"DEEPSUBTYPE-DISPLAY-NAME-1","name":"DEEPSUBTYPE-1"},{"classification":"DEEPSUBTYPE","displayName":"DEEPSUBTYPE-DISPLAY-NAME-2","name":"DEEPSUBTYPE-2"}]}]}]}]'),
       (2, '2023-01-01 12:00:00.000', '2305', 'NAMESPACE.1',
        '[{"classification":"CATEGORY","displayName":"CATEGORY-DISPLAY-NAME-1","name":"CATEGORY-1","labels":[{"classification":"TYPE","displayName":"TYPE-DISPLAY-NAME-1","name":"TYPE-1","labels":[{"classification":"SUBTYPE","displayName":"SUBTYPE-DISPLAY-NAME-1","name":"SUBTYPE-1"}]},{"classification":"TYPE","displayName":"TYPE-DISPLAY-NAME-2","name":"TYPE-2","labels":[{"classification":"SUBTYPE","displayName":"SUBTYPE-DISPLAY-NAME-1","name":"SUBTYPE-1"}]}]}]');

-------------------------------------
-- Validation
-------------------------------------
INSERT INTO validation(id, municipality_id, namespace, `type`, created, modified, validated)
VALUES (100, '2281', 'NAMESPACE.1', 'CATEGORY', '2023-01-01 12:00:00.000', null, true),
       (101, '2281', 'NAMESPACE.1', 'TYPE', '2023-01-01 12:00:00.000', null, false),
       (102, '2281', 'NAMESPACE.2', 'CATEGORY', '2023-01-01 12:00:00.000', null, false),
       (103, '2281', 'NAMESPACE.2', 'STATUS', '2023-01-01 12:00:00.000', null, true);

-------------------------------------
-- Errand
-------------------------------------
INSERT INTO errand(municipality_id, id, assigned_group_id, assigned_user_id, category, namespace,
                   priority, reporter_user_id, status, title, type, created, modified, resolution,
                   description, escalation_email, errand_number, business_related, previous_status)
VALUES ('2281', 'ec677eb3-604c-4935-bff7-f8f0b500c8f4', 'ASSIGNED_GROUP_ID-1', 'ASSIGNED_USER_ID-1',
        'CATEGORY-1', 'NAMESPACE.1', 'LOW', 'REPORTER_USER_ID-1', 'STATUS-1', 'TITLE-1', 'TYPE-1',
        '2022-01-01 12:00:00.000', null, null, null, "ESCALATION_EMAIL_1", 'KC-23020001', false, 'STATUS-2'),
       ('2281', 'cc236cf1-c00f-4479-8341-ecf5dd90b5b9', 'ASSIGNED_GROUP_ID-1', 'ASSIGNED_USER_ID-1',
        'CATEGORY-1', 'NAMESPACE.1', 'LOW', 'REPORTER_USER_ID-1', 'STATUS-1', 'TITLE-1', 'TYPE-1',
        '2022-02-01 12:00:00.000', '2022-04-01 12:00:00.000', null, null, null, 'KC-23020002', false, 'STATUS-2'),
       ('2281', '1be673c0-6ba3-4fb0-af4a-43acf23389f6', 'ASSIGNED_GROUP_ID-3', 'ASSIGNED_USER_ID-3',
        'CATEGORY-3', 'NAMESPACE.1', 'HIGH', 'REPORTER_USER_ID-3', 'STATUS-3', 'TITLE-3', 'TYPE-3',
        '2022-03-01 12:00:00.000', null, "RESOLUTION", "DESCRIPTION", null, 'KC-23020003', false, 'STATUS-2'),
       ('2281', 'f4a7a771-bb75-487b-b7d8-2684a0c3512c', 'ASSIGNED_GROUP_ID-3', 'ASSIGNED_USER_ID-3',
        'CATEGORY-3', 'NAMESPACE.2', 'HIGH', 'REPORTER_USER_ID-3', 'STATUS-3', 'TITLE-3', 'TYPE-3',
        '2022-03-01 12:00:00.000', null, "RESOLUTION", "DESCRIPTION", null, 'KC-23020004', false, 'STATUS-2'),
       ('2305', 'e29906af-3083-4dcf-bb8a-d787ccf2dcc4', 'ASSIGNED_GROUP_ID-3', 'ASSIGNED_USER_ID-3',
        'CATEGORY-3', 'NAMESPACE.1', 'HIGH', 'REPORTER_USER_ID-1', 'STATUS-3', 'TITLE-3', 'TYPE-3',
        '2022-03-01 12:00:00.000', null, null, null, null, 'KC-23020005', false, 'STATUS-2'),
       ('2281', '147d355f-dc94-4fde-a4cb-9ddd16cb1946', 'hardware support', 'jane11dane',
        'CATEGORY-1', 'NAMESPACE.1', 'HIGH', 'joe01doe', 'STATUS-1', 'It is my birthday', 'TYPE-1',
        '2023-04-26 15:48:17.124', '2023-04-26 16:05:08.806', 'FIXED', 'Order cake for everyone',
        'joe.doe@email.com', 'KC-23020006', false, 'STATUS-2');

-------------------------------------
-- Stakeholder
------------------------------------
INSERT INTO stakeholder(id, external_id, external_id_type, errand_id, address, care_of, country,
                        first_name, last_name, zip_code, role)
VALUES ('3001', 'USER_ID', 'EMPLOYEE', 'ec677eb3-604c-4935-bff7-f8f0b500c8f4', 'ADDRESS-1',
        'CARE_OF-1', 'COUNTRY-1', 'FIRST_NAME-1', 'LAST_NAME-1', 'ZIP_CODE-1', 'ROLE-1'),
       ('3002', '01e159b4-724d-40f9-91a4-9a7d021b8563', 'PRIVATE',
        'ec677eb3-604c-4935-bff7-f8f0b500c8f4', 'ADDRESS-2', 'CARE_OF-2', 'COUNTRY-2',
        'FIRST_NAME-1', 'LAST_NAME-1', 'ZIP_CODE-2', 'ROLE-2');

INSERT INTO stakeholder(id, external_id, external_id_type, errand_id)
VALUES ('3003', 'def57969-9b83-4e54-9351-667dc896a19d', 'PRIVATE',
        'cc236cf1-c00f-4479-8341-ecf5dd90b5b9'),
       ('3004', '7fba994b-e99e-4beb-8ab5-63cc74483b66', 'ENTERPRISE',
        '1be673c0-6ba3-4fb0-af4a-43acf23389f6'),
       ('3005', '83793ee4-4b33-467a-ac56-570d6babcc5b', 'ENTERPRISE',
        'f4a7a771-bb75-487b-b7d8-2684a0c3512c'),
       ('3006', '76bf1aa0-6596-4dca-88ee-25d8a1a47e60', 'PRIVATE',
        'e29906af-3083-4dcf-bb8a-d787ccf2dcc4');

INSERT INTO stakeholder(id, address, care_of, country, first_name, last_name, external_id,
                        external_id_type, zip_code, errand_id, `role`)
VALUES ('3008', '155 Country Lane, Cottington', 'Ford Prefect', 'United Kingdom', 'Aurthur', 'Dent',
        'cb20c51f-fcf3-42c0-b613-de563634a8ec', 'PRIVATE', '12345',
        '147d355f-dc94-4fde-a4cb-9ddd16cb1946', 'CAKE-BAKER'),
       ('3009', 'Northern skies', NULL, 'Norway', 'Slartibartfast', 'Magrathea',
        'cb20c51f-fcf3-42c0-b613-de563634a8ec', 'PRIVATE', '23456',
        '147d355f-dc94-4fde-a4cb-9ddd16cb1946', 'CAKE-EATER');

-------------------------------------
-- ContactChannel
------------------------------------

INSERT INTO contact_channel(stakeholder_id, type, value)
VALUES ('3001', 'TYPE-1', 'VALUE-1');

-------------------------------------
-- ExternalTag
-------------------------------------
INSERT INTO external_tag(errand_id, `key`, `value`)
VALUES ('ec677eb3-604c-4935-bff7-f8f0b500c8f4', 'KEY-1', 'VALUE-1'),
       ('ec677eb3-604c-4935-bff7-f8f0b500c8f4', 'KEY-2', 'VALUE-2'),
       ('cc236cf1-c00f-4479-8341-ecf5dd90b5b9', 'KEY-3', 'VALUE-3'),
       ('1be673c0-6ba3-4fb0-af4a-43acf23389f6', 'KEY-4', 'VALUE-4'),
       ('1be673c0-6ba3-4fb0-af4a-43acf23389f6', 'KEY-5', 'VALUE-5');

-------------------------------------
-- Attachment Data
-------------------------------------
INSERT INTO attachment_data(id, file)
VALUES ('1',
        0xFFD8FFE000104A46494600010100000100010000FFE201D84943435F50524F46494C45000101000001C800000000043000006D6E74725247422058595A2007E00001000100000000000061637370000000000000000000000000000000000000000000000000000000010000F6D6000100000000D32D0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000964657363000000F0000000247258595A00000114000000146758595A00000128000000146258595A0000013C00000014777470740000015000000014725452430000016400000028675452430000016400000028625452430000016400000028637072740000018C0000003C6D6C756300000000000000010000000C656E5553000000080000001C007300520047004258595A200000000000006FA2000038F50000039058595A2000000000000062990000B785000018DA58595A2000000000000024A000000F840000B6CF58595A20000000000000F6D6000100000000D32D706172610000000000040000000266660000F2A700000D59000013D000000A5B00000000000000006D6C756300000000000000010000000C656E5553000000200000001C0047006F006F0067006C006500200049006E0063002E00200032003000310036FFDB00430017101114110E171412141A18171B223925221F1F224632352939524857555148504E5B66836F5B617C624E50729B737C878B929492586DA0AC9F8EAA838F928DFFDB004301181A1A221E22432525438D5E505E8D8D8D8D8D8D8D8D8D8D8D8D8D8D8D8D8D8D8D8D8D8D8D8D8D8D8D8D8D8D8D8D8D8D8D8D8D8D8D8D8D8D8D8D8D8D8D8D8D8DFFC00011080103010303012200021101031101FFC4001B00000203010101000000000000000000000002010304050607FFC400321000020102040306050501010100000000000102031104122131053251142241537191132334355215334361728142A1FFC400190101000301010000000000000000000000000102030405FFC4002111010100020301010100030100000000000001021103213112133222415161FFDA000C03010002110311003F00F58000000000000000000000000000000000000000000000000000000000000000000000000000000000000000004362B9A5E21376456DC611CD202CCEBA8675D4A3B4D0FC913DA687E488FA89D55D9D750CEBA9476AA17B6741DAA87E687D43557E75D433AEA51DA685AF9908F886122F5A911B86AB5675D433AEA64FD4B07E6C49FD4308FF00922370D56ACEBA8675D4CBDBF09E6443B7E13CC88DC355AB3AEA19D75327EA383F3621FA8E0FCD88D9AAD79D750CEBA98FF52C1F9B11A3C43092DAA458DC355AB3AEA19D7533F6CC37E710ED986FCD0DC355A33AEA19D7528ED5877FFB41DA687E487D43557E75D433AEA54B1145FF00E910F11457FE90FA86AAECEBA8675D4CD2C6E1A3BCD217F51C1F9B11B86AB5E75D433AEA64FD4707E6C43F51C1F9B11B355AF3AEA0A69F8993F51C1F9B12F8CA35219E0EEBC19285D724483BA1C00000000000AEA6C518DFA497A17D4D8A31BF4922B979538FAE192401E73B8ABF71889F765EA32E76247965EA48B64FE57FC38F886F3EE75E5FB4FD0E3625DA469C7DD572E95B76F127E33B5932895EF7B82959FF004747CB2B92FF008B3EA329B7E2539D35A109BB91A3E9745DA779DDA34468C2BC6F4E7AF432A8C9937953778BB322CDF89991AAD29D37690F8793571E15D578E4A9CC3D3A3974296D93556F7C590CD2F1D0B92CAB7160B2EE58D5D195AB4814A4F62D837E2675271762E83CC56A57C64E24CA49ADC4F021EC57753A55560A49A6B439F5B0CE17707A1D294AC5534A4B42F8E5622CDB8D2724F56C333EA74E78155237461AD869527B6874639CACEE362B52775AB3DC70CFB753F43C3C62F323DC70CFB753F435C597278D94F947129F28E68C800000000015D4D8A31BF4922FA9B14637E924572FE6A71F5C30200F35DC55CEC45C92F51973BF4157EDBF5243CBF69FA1C0C555F98D1DEA8ED424FF00A3CF538FC5AEDBEA6FC3FEEB3CFF00E2CA5879CE377E25B1C1CADAEA69859591A69D88CB972FF4D67163A628E06ED68688E062B766ACC8893D0AFE9954CE3C6287422B63162216B9BE4EC64C46C5B0CAED1C98CD39EE4D4AE9EA8E861711F123AEE8E6D5D25A1760A796B25D4DF3C771CB8E5AAEAA9E6F50CEE2C3227AC4257399B2D849498D17DFB2298BB1761DA776C8B132AE724902D5092EF6C327963AB2962C970BF8092A5A685B19A6AE4DD3260A29CF2F76457898A945E859552BE82C5A946CF7250E5CA369D8F67C37EDF4FD0F2988A5695CF57C37EDF4FD0EAE2BB61CBE35D3E51C4A7CA39BB000000000005753633E3BE92468A9B19F1DF4922B97F3538FAE10001E6BB8AB99FA0ABF6DFA92B998BFC4FD4B02BFD2CEDD0E1E1577D9DDABAE1E497438B43B8E575AA36E2FE6B3CBFA8D914D9AA9C5A89CCF8D55F2AB1A30D59C1A75257FE8AE5856D39278DF14BA932466AF34E174EC8C79AAE6D2AE8571C2D4E5969D0A8B433D48E6453DFF0036E5B4EED778BFCFCF68FA9974E5E222E352CC9A0AD5636EA68C653BCD0AE0A9C62FC6E747D6E396E3AAE9C744876D35FD8518AAB4A2D17C69456E725BDB6918E72B053AB960EC6A9D28CAFA18EA52F872FE996965459A6BA13BC35DC79AB9461F434EE6756848E8EC5F6D0A1F35C1D5684292A3B499546769A2672BDDB286F535914B57D66A47A5E1FA6061E8795CDA23D5603E861E86DC534CB92B553E51C4A7CA39BB100000000005753633E3BE92468A9B19F1DF4722B97F3538FAE10001E6BB88B9989FC6FD475CCC44FE5BF52C1AA7ECBF438F157AB24FC4EBD47F25FA1C894B2D746BC5FED4CBD4E4966B6C88747C6ECDD4F24A3A95D6B27644FE97C6BF94F56C611961A317B944B0CD3D168694E3F0A3663C2A26ACD149958B5C6565A583D6EF42F9C553562FF8B18A33569E7968376FA7CC9E32544E75922BC53B6589A2538D37796873B115FE2D5BAD91BE136E7CEC92BBB84EED0491A63B5CC7C32A7C4C3EBBA36A57D8E6CE7F92D8F85653888EC6A8C4A31095EC4254D3D19AE9AD0A2947534C63643D0997528ACF2B3437A368C5523293BB65F188AAE750A653B326A2B0D4A8B96E6B35197748AA6A7B0E1DF410F43CA4F0F95DD23D5F0FFA087A1AF1D97C5339635D3E51C4A7CA39AB200000000005753633E3BE8E468A9B19F1FF004722B979538FAE0A06088679CEE2AE6627F1BF5193EF313F8FFE921AA3F94FD0E3D6FDC4CEB547F25FA1C8ADCC6DC4CF3688C9DB409BEEBB914249C752AAB26E4EDB13AEDA7D6B13C33385EEEC69C3D58E5B3DCC7094D432A4446528BB35AB2D71DAB33B1D09464F60C9955D8985AAEEE331EB4D2465ABBD35FA966DCDE20EED232451AEA539625CA4B9578890C354734B2FFD3AA5D47165DD763874552C3AFECD9195999E953953A295AF604E4D9C97BBB6F3A8D152AAA68C8A6EA4DB63D6945475DC4A7B11A4AFA5A1A6FDDB19E9D93D47CDE240354DF428AAB43466D0CF565A1688ACD9334CBD472BD028C3C5932D1936A243AB49D99E8B06AD848FA1E769ABEA7A3C27D247D0D787DAA72F8BE9F28E253E51CE97380000000002BA9B19F1FF00472F43454D8CD8FF00A3915CBCA9C7D704180AD9E73B8BFF00A656FF006FFE8D9AD262A4DC6C4A754557F25FA1CD693DCE9555F299CD93CA6DC6CF2579DD397F45F4ED28FA99DC5D46453AAE9BB336B3714996AB5592DD8D18C6F7DD995D5CCF71E355416ACA7C569FA45F39A8D64CAB11525566A953D5B28A95734AE7578550A74E3DA6BEFE172D31D7759DCB7E2CAB84861384494B9DEA72F0D8C9D3493D51AB88E2A58AA9953EE78230CE93A52B16EAF5547569F13838DA4AC24F17493D24736C43572BF9C4FDD6DA988537A33452AD0CA966471EEE2C74DF5178E54CC9DA5561F921FB4534B59238799F50727D4AFE49FB76678BA56B290B0B5695D3BA38D999AB038974AA24F6632E3D4E8996EBB09652A9EAC794AEAE8AF76611A53A795247A2C1FD247D0F34E491E9302EF8287A1BF0FAC795A69F28E253E51CE9600000000000AEA6C66C7FD1CBD0D353633710FA397A15CBCA9C7D79FBE851527AE85B3768332D39DA37945DEE79F26DDF8D590A894BBC8D168CE3DD656AA5292B4924249C53F96C6B6D366CD965692BA17B353A9524E6ACAC42A91B3727AA324F1B3CCEC69863633E4AAEB41D394B2981BD59B6A5773D599E953557323A71FF00D72E515662735C6A94250D6DA1524FC0BFAA771A28433CFF00A46E739C96AED15B232E166A29A6B5658A5526F2A894AB45B87A7F16BE9B2271F1B491B70543E15377E6666E270B38B32996F26BAD62C00413F0EA65CD95DBA9BB25726117A0AD49BD465A128305F416E0F60945C8CF24F405B85C21D8C0E23E353CB2DD17D49A8AD0E351AB2A4EF166D86322DA738DCE7CB0EF71ACC96F7E7CA8F57C3D3581827BD8E0C251704E2B467A0C27D2C7D0B7165BB62BCB3A68A7CA3894F9473A1800000000002BA9B19B887D14BD0D353633710FA297A15CBCA99EBCCD7938C6E8AE188596D949C53EE58CD4DA4CE398EE3B665A15AA67968AC57F11C75B96D48AB5D18AACDB7635C71532CB5DA6755C9BD45D4AAF666B9AB61949234B34A4BBF59E52B2B0D85E6655B97E117CC64DF14DEEAFA95A3F09C5C753129E56D6536BA69D4B309C230F0456591366D4E1B56E525B1A70BDFAEDFF00F05524A0F4B1A387D2C979CBC48CAF49C676DAEEA261C666ACD24AECDD37EC6755153AAA6D5D230C6F6D6F8DB80E13495153AAAF27D4E92C2D154DC322B7A19963E87C28CA534AE5F1C42924E3252BF42D6DF59289F09C2CA2FB9B9E7B88E06585AAECBB9E0CF552A8AD6D99E778CE39CEA3A2B951A616ED15C9B005C0D952DF5274165B8041B3244A93DC4B0CF4412DF87E24E36835A23D9E0279F05097547CF61CC8F7FC2BEDB4FD08C7192EE232B6C6DA7CA3894F9472EA00000000002BA9B19B882BE0E4BFA34D4D8A716AF86772B97899EBC7D5939D469AD114B834EE8EDCB074EA49BB59892E1BD19C92FF00C7675A71AA54B41AF130BDCEBE3708A841CDBBB390F737C58E751E274ABB4B85C15B5B98A31A69ACCEE689D6A73A593C1136AB184D184FDC2A718EAD32CC2BB4CB5F09EB4564D49486CADEA8B271CD064508CAA2B456A8C77D2EAACE738C1F89D485351A6A2BC0CB8683755CA4B636DEFA233CEAF84265BAB19B12BBAE3135B57564CCD5B48B4525ED6BE399469FC5AF184E6D46E7A251A584A2A5195D45753CD4EF99D8875EAB864736D743AAE3F4C25D34E2789D79E21CA13697818E7525526E52776C5B017934A5A9448221921640983213250B13076625C1A6424F169347BFE15AF0CA7FE4F9F463AA3E83C27ED94BFC93115B69F28E253E51C95400000000015D4D8CBC4AA2A58294DEC8D553639DC77ED353D08A98C385C4D3ABAC648D53AA94773C4D3C454A4FB9268BDF13C4386572B98FE7A6DF71BB8BE2D4E5F0E2CE4390B39B9CAF27A8A99AC9A6772DA6EEE326C464C49D20C5F86FDC452594B496856F898EAC15EC56DCA856EE78850AAA4AD2D1974A2A4D5F7473DE9B4ED753D2167BBDC149ED1F71335E5645B0B25633AD219D92D0CF560DA6CBD3D7FA12AECDF8159EA5C5A9A4D88D1655FDC621DB3C735F48C51A62A2CAD32219200232B7A32C6249131152A43662A193085B196A8FA0709FB652FF0027CF22F547D0B847DAE97F9056EA7CA3894F9472500000000000AEA6C73F8EFDA6A7A1D0A9B1CEE3BF69ABE807806413E2410904124122413B3021816C7545D4577CA29BD0D38757994ABC6A8C3332EA6A49D9BB8538D899C927A1CF95DB587CB95EBB96465E0519FF00E91F11A657556DB5DD5AC66AF52CAC1F13428AB76F718E3D97262AB2F98C5BE83568DA5A08F4474C634927A82206459500C086040AC621928A424002131DD1F43E11F6BA5FE4F9DADD1F44E0FF006AA5FE4943753E51C4A7CA38000000000015D4D8E771EFB4D5F43A353639FC7166E15512E807CF8097A32084A000090225A04310223A335E179CC9E26EC22566CAE5E2F8B6458C95DEA2C1AF1668A3DE9A715748E6CAE9B48A5C1DF621D3763757945A51CB6653ADB629F4B7CB138B4C57766ECA9EE8AE54FC522F3345C5CDC45EE8CD27A1BB17C9B1859D187718E48432211372EA8640010021803243528A6F52C74E36F03336C22DDD6A4586D67C3D4FA0708FB5D2FF0027845E07BCE15F6CA7FE4988ADB4F947129F28E4AA0000000000AEA6C62E31AF0DA9E86E9ABA2B928548649ABAE8C0F9A4E32CEFBAFD85C92FC5FB1F46EC183F1A50F60EC183F261EC13B7CE324BF17EC1927F8BF63E8FFA7E0FCA87B13D8307E543D821F398D39FE2FD87F873FC5FB1F43EC384F2A3EC1D8B09E547D884EDF3BF853FC5FB1641D482B6591F41EC584F2A3EC4762C27951F61A4EDE0A1F1AA3D9A47670B9A952565A9E9560F0AB6A71F61BB361FF0899E7C7F4BE3C9A79B949B96692BB11D492DA27A7ECF87FC221D9B0FF844A4E15BF579375269DECCAAAD79DB44CF61D970CFF8E24763C2BFE38FB16FCA2BFA3C1D694E49DD3336597E2CFA2761C27951F623B060FCA87B1A49A52E5B7CF72CBF161965F8BF63E87D8307E547D83B060FCA8FB128DBE7B925F8B232CBF167D0FB060FCA8FB0760C1F951F61A36F9E6597E2FD88C92FC5FB1F44EC183F2A3EC1D8307E543D8936F9CB84AFCAC234E599775FB1F45EC183F2A1EC1D8306BF8A1EC0781CB2BAD19EF38569C329FF0091FB0E13CA8FB1725184324159742345AB29F28E241590E4A000000000010D10E09F80C002645D0322E83800991740C8BA0E002645D0322E83800991740C8BA0E002645D0322E83800991740C8BA0E002645D0322E83800991740C8BA0E002645D0322E83800991740C8BA0E002645D0322E83800991740C8BA0E002645D095148600000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000003FFFD9),
       ('2',
        '89504E470D0A1A0A0000000D494844520000001000000010080200000090916836000000017352474200AECE1CE90000000467414D410000B18F0BFC6105000000097048597300000EC300000EC301C76FA8640000001E49444154384F6350DAE843126220493550F1A80662426C349406472801006AC91F1040F796BD0000000049454E44AE426082'),
       ('3',
        '89504E470D0A1A0A0000000D494844520000001000000010080200000090916836000000017352474200AECE1CE90000000467414D410000B18F0BFC6105000000097048597300000EC300000EC301C76FA8640000001E49444154384F6350DAE843126220493550F1A80662426C349406472801006AC91F1040F796BD0000000049454E44AE426082'),
       ('4',
        '89504E470D0A1A0A0000000D494844520000001000000010080200000090916836000000017352474200AECE1CE90000000467414D410000B18F0BFC6105000000097048597300000EC300000EC301C76FA8640000001E49444154384F6350DAE843126220493550F1A80662426C349406472801006AC91F1040F796BD0000000049454E44AE426082'),
       ('5', 'happybirthday');

-------------------------------------
-- Attachment
-------------------------------------
INSERT INTO attachment(id, attachment_data_id, file_name, mime_type, errand_id)
VALUES ('25d266a7-1ff2-4bf4-b6f3-0473b2b86fcd', '1', 'Test_image.jpg', 'image/jpeg',
        'ec677eb3-604c-4935-bff7-f8f0b500c8f4'),
       ('c697642d-4d8d-4b07-8816-025a2734b09a', '2', 'Test.txt', 'text/plain',
        'cc236cf1-c00f-4479-8341-ecf5dd90b5b9'),
       ('c8d88089-5136-4a1a-aa10-5f435cb6e69f', '3', 'Test2.txt', 'text/plain',
        'cc236cf1-c00f-4479-8341-ecf5dd90b5b9'),
       ('99fa4dd0-9308-4d45-bb8e-4bb881a9a536', '4', 'Test3.txt', 'text/plain',
        '1be673c0-6ba3-4fb0-af4a-43acf23389f6'),
       ('95ea267a-28ec-4636-922c-a717d79bd029', '5', 'birthday-card.txt', 'text/plain',
        '147d355f-dc94-4fde-a4cb-9ddd16cb1946');

-- Revision
-------------------------------------
INSERT INTO revision(id, created, entity_id, entity_type, serialized_snapshot, version)
VALUES ('59328e70-4297-4bb5-ba69-cb17f2d15a17', '2022-01-01 12:00:00.000',
        '1be673c0-6ba3-4fb0-af4a-43acf23389f6', 'ErrandEntity',
        '{"id":"1be673c0-6ba3-4fb0-af4a-43acf23389f6"}', 0),
       ('84e0f78f-a857-4325-adff-04d2c0609a64', '2023-04-26 15:48:17.164',
        '147d355f-dc94-4fde-a4cb-9ddd16cb1946', 'ErrandEntity',
        '{"id":"147d355f-dc94-4fde-a4cb-9ddd16cb1946","externalTags":[{"key":"caseid","value":"2222-3333"}],"stakeholders":[{"id":3007,"externalId":"cb20c51f-fcf3-42c0-b613-de563634a8ec","externalIdType":"PRIVATE","role":"ADMINISTRATOR","firstName":"Aurthur","lastName":"Dent","address":"155 Country Lane, Cottington","careOf":"Ford Prefect","zipCode":"12345","country":"United Kingdom","contactChannels":[{"type":"Email","value":"arthur.dent@earth.com"}]}],"municipalityId":"2281","namespace":"NAMESPACE.1","title":"Title for the errand","category":"CATEGORY-1","type":"TYPE-1","status":"STATUS-1","resolution":"FIXED","description":"Order cake for everyone","priority":"LOW","reporterUserId":"joe01doe","assignedUserId":"joe01doe","assignedGroupId":"hardware support","escalationEmail":"joe.doe@email.com","created":"2023-04-26T15:48:17.124+02:00"}',
        0),
       ('b69b0c4a-4a43-4753-ab6d-f5b8eeca1dcd', '2023-04-26 16:05:08.805',
        '147d355f-dc94-4fde-a4cb-9ddd16cb1946', 'ErrandEntity',
        '{"id":"147d355f-dc94-4fde-a4cb-9ddd16cb1946","externalTags":[],"stakeholders":[{"id":3008,"externalId":"cb20c51f-fcf3-42c0-b613-de563634a8ec","externalIdType":"PRIVATE","role":"CAKE-BAKER","firstName":"Aurthur","lastName":"Dent","address":"155 Country Lane, Cottington","careOf":"Ford Prefect","zipCode":"12345","country":"United Kingdom","contactChannels":[{"type":"Email","value":"arthur.dent@earth.com"}]},{"id":3009,"externalId":"cb20c51f-fcf3-42c0-b613-de563634a8ec","externalIdType":"PRIVATE","role":"CAKE-EATER","firstName":"Slartibartfast","lastName":"Magrathea","address":"Northern skies","zipCode":"23456","country":"Norway","contactChannels":[{"type":"Email","value":"slartibartfast@earth.com"}]}],"municipalityId":"2281","namespace":"NAMESPACE.1","title":"It is my birthday","category":"CATEGORY-1","type":"TYPE-1","status":"STATUS-1","resolution":"FIXED","description":"Order cake for everyone","priority":"HIGH","reporterUserId":"joe01doe","assignedUserId":"jane11dane","assignedGroupId":"hardware support","escalationEmail":"joe.doe@email.com","attachments":[],"created":"2023-04-26T15:48:17.124+02:00","modified":"2023-04-26T16:05:08.795+02:00","touched":"2023-04-26T15:48:17.124+02:00"}',
        1),
       ('43a5b3c8-9010-4518-ab1b-d365bd7d6bb1', '2023-04-26 16:07:32.884',
        '147d355f-dc94-4fde-a4cb-9ddd16cb1946', 'ErrandEntity',
        '{"id":"147d355f-dc94-4fde-a4cb-9ddd16cb1946","externalTags":[],"stakeholders":[{"id":3008,"externalId":"cb20c51f-fcf3-42c0-b613-de563634a8ec","externalIdType":"PRIVATE","role":"CAKE-BAKER","firstName":"Aurthur","lastName":"Dent","address":"155 Country Lane, Cottington","careOf":"Ford Prefect","zipCode":"12345","country":"United Kingdom","contactChannels":[{"type":"Email","value":"arthur.dent@earth.com"}]},{"id":3009,"externalId":"cb20c51f-fcf3-42c0-b613-de563634a8ec","externalIdType":"PRIVATE","role":"CAKE-EATER","firstName":"Slartibartfast","lastName":"Magrathea","address":"Northern skies","zipCode":"23456","country":"Norway","contactChannels":[{"type":"Email","value":"slartibartfast@earth.com"}]}],"municipalityId":"2281","namespace":"NAMESPACE.1","title":"It is my birthday","category":"CATEGORY-1","type":"TYPE-1","status":"STATUS-1","resolution":"FIXED","description":"Order cake for everyone","priority":"HIGH","reporterUserId":"joe01doe","assignedUserId":"jane11dane","assignedGroupId":"hardware support","escalationEmail":"joe.doe@email.com","attachments":[{"id":"95ea267a-28ec-4636-922c-a717d79bd029","fileName":"birthday-card.txt","mimeType":"text/plain","file":[104,97,112,112,121,98,105,114,116,104,100,97,121],"created":"2023-04-26T16:07:32.874+02:00"}],"created":"2023-04-26T15:48:17.124+02:00","modified":"2023-04-26T16:05:08.806+02:00","touched":"2023-04-26T16:05:08.806+02:00"}',
        2);

-------------------------------------
-- Communication
-------------------------------------
INSERT INTO communication(viewed,sender, sent, id, errand_number, external_case_id,
                          message_body, target, subject, direction, type)
VALUES (0, 'Test Testorsson', '2023-01-01 12:00:00.000', 'cc236cf1-c00f-4479-8341-ecf5dd90b5b9', 'KC-23020001', 'case1',
        'message body 1', '1234567890', 'subject1', 'INBOUND', 'SMS'),
       (1, 'Test Testorsson', '2023-01-02 12:00:00.000', '59328e70-4297-4bb5-ba69-cb17f2d15a17',  'KC-23020001', 'case2',
        'message body 2', '0987654321', 'subject2', 'OUTBOUND', 'EMAIL');

-------------------------------------
-- Communication_attachment_data
-------------------------------------
INSERT INTO communication_attachment_data(id, file)
VALUES (1, UNHEX('48656C6C6F20576F726C6421')), -- 'Hello World!' in hexadecimal
       (2, FROM_BASE64('iVBORw0KGgoAAAANSUhEUgAAAIsAAACPCAMAAAD9VtjbAAAAAXNSR0IArs4c6QAAAARnQU1BAACx jwv8YQUAAAL3UExURXFxcQICAgAAAAEBAS8vLxERERUVFRQUFBMTExYWFgcHBw4ODsLCwurq6ujo 6OPj49ra2tvb2+fn5+np6ebm5tzc3N7e3uXl5eLi4uDg4NnZ2eHh4dbW1t3d3d/f39XV1dfX1+vr 61ZWVhAQEP////f39/b29vX19fn5+f7+/vv7+/T09Pz8/PLy8v39/fHx8fPz8/Dw8Pr6+mFhYdTU 1O/v7+7u7u3t7ezs7Pj4+FxcXF1dXWNjY2RkZGBgYNPT015eXl9fX+Tk5M3Nzb6+vrW1tby8vMHB wbu7u8PDw8vLy9jY2JKSkmVlZUZGRisrKxgYGAoKCgQEBAMDAwYGBiEhIUFBQVdXV25ubo6OjrKy so+Pj0tLSxwcHBoaGkVFRYuLi9HR0aampk5OTltbW8XFxaOjozg4OA0NDXt7e01NTTs7O5ubm7+/ v2dnZw8PD09PTx4eHlNTUzk5OWpqagwMDBcXFyMjIy4uLjc3NzQ0NDAwMCoqKiAgIHh4eGJiYoyM jIiIiK6urtDQ0H19fTIyMgUFBbi4uAsLC729vcjIyBkZGbOzs6qqqpSUlFBQULq6ukRERHNzc6ys rIGBgXZ2dmlpaSQkJFRUVBsbG9nZ2Jycmx8fH6GhoUxMTNLS0jY2NlJSUiYmJp+fn6CgoKSkpFhY WJGRkc7OziIiImhoaEhISFpaWqurq4eHh6ioqMzMzFlZWUZGRa+vr3l5eIqKim1tbXx8fCUlJZyc nImJic/Pz29vbxMTFD4+Ppqami0tLaenp0BAQFFRUYaGhsnJySwsLEdHR6WlpT09PaKioklJSYOD g8DAwAkJCXl5eZmZmX5+fjExMYSEhJ2dnbe3t42NjVtbWikpKYWFhTo6OkJCQn9/f3p6end3d5aW lrCwsLa2ticnJ3JycpeXl6mpqYCAgJ6enggICMbGxsTExENDQ8rKyjMzM2tra5iYmHV1dRISErm5 uZWVlVVVVWZmZpOTkz8/P5CQkEpKSrS0tDw8PAAAAOdW1PAAAAD9dFJOU/////////////////// //////////////////////////////////////////////////////////////////////////// //////////////////////////////////////////////////////////////////////////// //////////////////////////////////////////////////////////////////////////// //////////////////////////////////////////////////////////////////////////// /////////////wD2TzQDAAAACXBIWXMAAA7DAAAOwwHHb6hkAAAM3klEQVR4Xu1by5EjuQ7cmAhd xwPd+jAWyAa5JQN0aDN0kymyQB7oJt0fMvGvIkvduy9i99AcTSEFJEEWCKI+3f3X/dd/pV3+MvBf aD9zGbefuYzbz1zGLeey+7eajV/msrse0PbS5sDlPwArxeFpMyhx2X+gHeRfB0UuTXMg0jUhDaw0 L5tAiYvNxRkOIL8FIANALoAEAzK5g7hceUpkONDOFSwVfgJ7KhIg+g14n9ZbNeu57BBWWiEAVFHA SlEsCkQuNBjMgcvOHayR5IsRFn0cQG6YmmJMWWsoB/mi55gntAYpF2Bp0oUCMJnALJY0VAz3UVK/ BJb+cwqmiVxxIHLRCXMa7iONjAdC/kMqVIsC11DmF1XgIEqFJgWYVD6BSlAGuXvFRM3HJoCcA99Z i04BIDt4U1/wATBZgFHWADIA5ArMZjmsL2hghJfQjIEI919zkcAUWV/WwLmDNRK3YnMGJJHLCYBc A8g3XEhFW/uIJAGaX1AYwIfIABxRQRJlUEKjgPHD9wBB2agvS2oDLhUowvhsafLN/X7lQB3vaTCE AxlONgDkfn89no/arj62Ti+5PnROoYC/EZfYBRpl+eyPp9fjkrdC4mP3eDxfp7NyhyMTuSSY7qPG EqATTIBT5vf96bPOorfd43QF09wEgGOi4nhWX8AxpoHoW8HH4fZpvebtdVSqdRKRWQSZyfO1+gJA GQCWj9vDumy3u7AxcnM3AOP6Yowp4Bld38fE2kO3A3sXL6lQzWAuEhexkWFZwSWHDCA9b/M0WbXd 0XqpO/ix5ho4nu9pacoagziJLzWZTOttMgFWcLhGzoCcgC+vjzWu/Dpn6naf1xdQXHaFgMN3pyI5 I90xMv05gIgBBnNZ3ddhbILQZK/adpcH2+VSHgGzvWzE9BJAz35SX9yaAB8HHyejRru8TkfsLVAg 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'));

-------------------------------------
-- Communication_attachment
-------------------------------------
INSERT INTO communication_attachment(communication_attachment_data_id, id,
                                     communication_id, content_type, name)
VALUES (1, '896a44d8-724b-11ed-a840-0242ac110002', 'cc236cf1-c00f-4479-8341-ecf5dd90b5b9', 'text/plain', 'attachment1'),
       (2, '05b29c30-4512-46c0-9d82-d0f11cb04bae', '59328e70-4297-4bb5-ba69-cb17f2d15a17', 'image/png', 'attachment2');

-- Insert into communication_email_header
INSERT INTO communication_email_header (communication_id, id, header_key)
VALUES ('cc236cf1-c00f-4479-8341-ecf5dd90b5b9', '81471222-5798-11e9-ae24-57fa13b361e1',
        'REFERENCES');

-- Insert into communication_email_header_value
INSERT INTO communication_email_header_value (header_id, value, order_index)
VALUES ('81471222-5798-11e9-ae24-57fa13b361e1', 'someValue', 0),
       ('81471222-5798-11e9-ae24-57fa13b361e1', 'someOtherValue', 1);

-------------------------------------
-- Notification
-------------------------------------
INSERT INTO notification(acknowledged, created, expires, modified, content, created_by, description, errand_id, id, municipality_id, namespace, owner_full_name, owner_id, type)
VALUES (0, '2023-12-31 23:59:59.999', '2024-12-31 23:59:59.999', '2023-12-31 23:59:59.999', 'content-1', 'created_by-1', 'description-1', 'ec677eb3-604c-4935-bff7-f8f0b500c8f4', '3ec421e9-56d1-4e47-9160-259d8dbe6a50', '2281', 'namespace_1', 'owner_full_name-1', 'owner_id-1', 'type-1'),
       (1, '2023-12-31 23:59:59.999', '2024-12-31 23:59:59.999', '2023-12-31 23:59:59.999', 'content-2', 'created_by-2', 'description-2', 'cc236cf1-c00f-4479-8341-ecf5dd90b5b9', '2', '2281', 'namespace_1', 'owner_full_name-2', 'owner_id-2', 'type-2');

INSERT INTO parameter(id, name, value, errand_id)
VALUES ('55d266a7-1ff2-4bf4-b6f3-0473b2b86fcd', 'testName', 'testValue', 'ec677eb3-604c-4935-bff7-f8f0b500c8f4'),
('35d266a7-1ff2-4bf4-b6f3-0473b2b86fcd', 'PROPERTY_DESIGNATION', 'KLINGSTA 123', 'ec677eb3-604c-4935-bff7-f8f0b500c8f4'),
('45d266a7-1ff2-4bf4-b6f3-0473b2b86fcd', 'APARTMENT_NUMBER', '1101', 'cc236cf1-c00f-4479-8341-ecf5dd90b5b9');

INSERT INTO contact_reason(id, reason, municipality_id, namespace, created, modified)
VALUES(123, 'reason1', '2281', 'CONTACTCENTER', '2023-12-31 23:59:59.999', '2023-12-31 23:59:59.999'),
(124, 'reason2', '2281', 'CONTACTCENTER', '2023-12-31 23:59:59.999', '2023-12-31 23:59:59.999'),
(127, 'reason3', '2281', 'CONTACTCENTER', '2023-12-31 23:59:59.999', '2023-12-31 23:59:59.999'),
(125, 'reason3', '2281', 'namespace_2', '2023-12-31 23:59:59.999', '2023-12-31 23:59:59.999'),
(126, 'reason4', '2281', 'namespace_2', '2023-12-31 23:59:59.999', '2023-12-31 23:59:59.999');

-------------------------------------
-- Email integration config
-------------------------------------
INSERT INTO email_worker_config (id, enabled, municipality_id, namespace, days_of_inactivity_before_reject, errand_closed_email_sender, errand_closed_email_template,
                                status_for_new, trigger_status_change_on, status_change_to, inactive_status, created, modified)
VALUES (1, true, '2281', 'NAMESPACE.1', 1, 'sender-1', 'template-1', 'STATUS-1', 'STATUS-2', 'STATUS-3', 'STATUS-1', '2021-12-31 23:59:59.999', '2022-12-31 23:59:59.999');

-------------------------------------
-- Time measurement
-------------------------------------
INSERT INTO time_measurement(id, errand_id, start_time,stop_time, status, administrator)
VALUES ('1', 'ec677eb3-604c-4935-bff7-f8f0b500c8f4', '2023-12-31 23:59:59.999', '2024-12-31 23:59:59.999', 'STATUS-1', 'AD01TEST'),
       ('2', 'ec677eb3-604c-4935-bff7-f8f0b500c8f4', '2024-12-31 23:59:59.999', null, 'STATUS-2', 'AD01TEST');
