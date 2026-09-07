-- Errands for ErrandPurgeIT, in a namespace of their own.
--
-- A purge empties whatever namespace it is pointed at, so it is given errands nothing else asserts on. The namespaces
-- in testdata-it.sql are then what a run has to leave alone, which is the other half of what these tests are about.
--
-- Three of the four were last touched well before the cutoff the tests use and are reached by a run; the fourth was
-- touched after it and is not. The first of the three carries one of everything that hangs off an errand, so that a
-- run has something to remove besides the errand row itself.

INSERT INTO errand(municipality_id, id, assigned_group_id, assigned_user_id, category, namespace, priority,
                   reporter_user_id, status, title, type, created, modified, errand_number, business_related,
                   previous_status, channel, touched)
VALUES ('2281', 'aaaa1111-0000-0000-0000-000000000001', 'ASSIGNED_GROUP_ID-1', 'ASSIGNED_USER_ID-1', 'CATEGORY-1',
        'PURGE-NAMESPACE', 'LOW', 'REPORTER_USER_ID-1', 'STATUS-1', 'Reached, and carrying everything', 'TYPE-1',
        '2020-01-01 12:00:00.000', '2020-01-01 12:00:00.000', 'PU-23020001', false, 'STATUS-2', null,
        '2020-01-01 12:00:00.000'),
       ('2281', 'aaaa1111-0000-0000-0000-000000000002', 'ASSIGNED_GROUP_ID-1', 'ASSIGNED_USER_ID-1', 'CATEGORY-1',
        'PURGE-NAMESPACE', 'LOW', 'REPORTER_USER_ID-1', 'STATUS-1', 'Reached', 'TYPE-1',
        '2020-06-01 12:00:00.000', '2020-06-01 12:00:00.000', 'PU-23020002', false, 'STATUS-2', null,
        '2020-06-01 12:00:00.000'),
       ('2281', 'aaaa1111-0000-0000-0000-000000000003', 'ASSIGNED_GROUP_ID-1', 'ASSIGNED_USER_ID-1', 'CATEGORY-1',
        'PURGE-NAMESPACE', 'LOW', 'REPORTER_USER_ID-1', 'STATUS-1', 'Reached', 'TYPE-1',
        '2021-01-01 12:00:00.000', '2021-01-01 12:00:00.000', 'PU-23020003', false, 'STATUS-2', null,
        '2021-01-01 12:00:00.000'),
       ('2281', 'aaaa1111-0000-0000-0000-000000000004', 'ASSIGNED_GROUP_ID-1', 'ASSIGNED_USER_ID-1', 'CATEGORY-1',
        'PURGE-NAMESPACE', 'LOW', 'REPORTER_USER_ID-1', 'STATUS-1', 'Touched after the cutoff', 'TYPE-1',
        '2025-06-01 12:00:00.000', '2025-06-01 12:00:00.000', 'PU-23020004', false, 'STATUS-2', null,
        '2025-06-01 12:00:00.000');

-- The cutoff itself, and the columns an errand is dated by.
--
-- A run reaches what was last touched *before* the cutoff, so the two errands either side of it are one millisecond
-- apart and only one of them is reached. The three after that carry the fallback: an errand is dated by touched, or by
-- modified where there is no touched, or by created where there is neither - and one that carries none of them cannot
-- be shown to be old enough, so it is left where it is however long it has been there.
INSERT INTO errand(municipality_id, id, namespace, priority, status, title, type, created, modified, errand_number,
                   business_related, touched)
VALUES ('2281', 'aaaa1111-0000-0000-0000-000000000005', 'PURGE-NAMESPACE', 'LOW', 'STATUS-1',
        'One millisecond before the cutoff', 'TYPE-1', '2022-12-31 23:59:59.999', '2022-12-31 23:59:59.999',
        'PU-23020005', false, '2022-12-31 23:59:59.999'),
       ('2281', 'aaaa1111-0000-0000-0000-000000000006', 'PURGE-NAMESPACE', 'LOW', 'STATUS-1',
        'Exactly at the cutoff', 'TYPE-1', '2023-01-01 00:00:00.000', '2023-01-01 00:00:00.000',
        'PU-23020006', false, '2023-01-01 00:00:00.000'),
       ('2281', 'aaaa1111-0000-0000-0000-000000000007', 'PURGE-NAMESPACE', 'LOW', 'STATUS-1',
        'Never touched, dated by modified', 'TYPE-1', '2026-06-01 12:00:00.000', '2020-05-01 12:00:00.000',
        'PU-23020007', false, null),
       ('2281', 'aaaa1111-0000-0000-0000-000000000008', 'PURGE-NAMESPACE', 'LOW', 'STATUS-1',
        'Never touched or modified, dated by created', 'TYPE-1', '2020-05-01 12:00:00.000', null,
        'PU-23020008', false, null),
       ('2281', 'aaaa1111-0000-0000-0000-000000000009', 'PURGE-NAMESPACE', 'LOW', 'STATUS-1',
        'Carrying no date at all', 'TYPE-1', null, null, 'PU-23020009', false, null);

-- Two blobs, held the two ways the service holds them. 101 belongs to an attachment somebody added to the errand and
-- to nothing else. 102 is shared: a communication arrived carrying it, and the copy of that attachment kept on the
-- errand points at the very same row, which is what CommunicationService#saveAttachment leaves behind. A removal has to
-- account for both, and in an order that does not take a blob out from under something still pointing at it.
INSERT INTO attachment_data(id, file)
VALUES (101, 'attachment added to the errand'),
       (102, 'attachment that arrived with a communication');

INSERT INTO attachment(id, attachment_data_id, file_name, mime_type, errand_id, namespace, municipality_id, file_size,
                       channel)
VALUES ('aaaa2222-0000-0000-0000-000000000001', 101, 'purged.txt', 'text/plain',
        'aaaa1111-0000-0000-0000-000000000001', 'PURGE-NAMESPACE', '2281', 24, null),
       ('aaaa2222-0000-0000-0000-000000000002', 102, 'purged-communication.txt', 'text/plain',
        'aaaa1111-0000-0000-0000-000000000001', 'PURGE-NAMESPACE', '2281', 44, 'EMAIL');

INSERT INTO stakeholder(id, external_id, external_id_type, errand_id, first_name, last_name, role)
VALUES (9001, 'USER_ID', 'EMPLOYEE', 'aaaa1111-0000-0000-0000-000000000001', 'FIRST_NAME-1', 'LAST_NAME-1', 'ROLE-1');

INSERT INTO revision(id, entity_id, entity_type, municipality_id, namespace, serialized_snapshot, version, created)
VALUES ('aaaa3333-0000-0000-0000-000000000001', 'aaaa1111-0000-0000-0000-000000000001',
        'se.sundsvall.supportmanagement.integration.db.model.ErrandEntity', '2281', 'PURGE-NAMESPACE', '{}', 0,
        '2020-01-01 12:00:00.000'),
       ('aaaa3333-0000-0000-0000-000000000002', 'aaaa1111-0000-0000-0000-000000000001',
        'se.sundsvall.supportmanagement.integration.db.model.ErrandEntity', '2281', 'PURGE-NAMESPACE', '{}', 1,
        '2020-01-02 12:00:00.000');

INSERT INTO notification(id, acknowledged, global_acknowledged, created, modified, expires, municipality_id, namespace,
                         content, created_by, description, errand_id, owner_id, type)
VALUES ('aaaa4444-0000-0000-0000-000000000001', false, false, '2020-01-01 12:00:00.000', null,
        '2020-02-01 12:00:00.000', '2281', 'PURGE-NAMESPACE', 'Content', 'TestUser', 'Description',
        'aaaa1111-0000-0000-0000-000000000001', 'OWNER-ID-1', 'UPDATE');

INSERT INTO communication(internal, viewed, sender, sender_user_id, sent, id, errand_number, external_id, message_body,
                          target, subject, direction, type, namespace, municipality_id)
VALUES (0, 0, 'Test Testorsson', 'userId', '2020-01-01 12:00:00.000', 'aaaa5555-0000-0000-0000-000000000001',
        'PU-23020001', 'purge-case-1', 'message body', '1234567890', 'subject', 'INBOUND', 'EMAIL', 'PURGE-NAMESPACE',
        '2281');

INSERT INTO communication_attachment(id, attachment_data_id, communication_id, mime_type, file_name, namespace,
                                     municipality_id, file_size)
VALUES ('aaaa6666-0000-0000-0000-000000000001', 102, 'aaaa5555-0000-0000-0000-000000000001', 'text/plain',
        'purged-communication.txt', 'PURGE-NAMESPACE', '2281', 31);

-- The one thing a purge reaches out of the service for besides notes: the conversation is removed in MessageExchange
-- and the relation it holds in the relation service.
INSERT INTO conversation(municipality_id, latest_synced_sequence_number, namespace, type, id, errand_id,
                         message_exchange_id, topic)
VALUES ('2281', 100, 'PURGE-NAMESPACE', 'INTERNAL', 'aaaa7777-0000-0000-0000-000000000001',
        'aaaa1111-0000-0000-0000-000000000001', 'aaaa8888-0000-0000-0000-000000000001', 'The topic');

INSERT INTO conversation_relation_id(conversation_id, relation_id)
VALUES ('aaaa7777-0000-0000-0000-000000000001', 'PURGE-RELATION-1');

