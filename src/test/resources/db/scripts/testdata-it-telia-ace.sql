-- Minimal errand rows for the two queued work items below.
INSERT INTO errand (id, municipality_id, namespace, errand_number, status)
VALUES ('errand-1', '2281', 'NAMESPACE-1', 'KS-100001', 'ONGOING'),
       ('errand-2', '2281', 'NAMESPACE-1', 'KS-100002', 'ONGOING');

-- errand-2 already has a Telia ACE contact from an earlier delivery, so TeliaAceWorkItemWorker updates it instead
-- of adding a new work item. errand-1 has none yet, so it goes through AddWorkItem.
INSERT INTO telia_ace_contact (errand_id, email_uid, created)
VALUES ('errand-2', '42.CallGuideWorkItem@computer', CURRENT_TIMESTAMP(3));

-- Queued Telia ACE work items for TeliaAceWorkItemSchedulerIT: one without a predefined_agent_name (errand-1,
-- create), one with (errand-2, update).
--
-- created uses CURRENT_TIMESTAMP rather than a fixed date, since TeliaAceWorkItemWorker drops anything older than
-- its max-age without delivering it.
INSERT INTO telia_ace_work_item (id, errand_id, municipality_id, namespace, from_address, subject, content_url, predefined_agent_name, created)
VALUES ('bbbb1111-0000-0000-0000-000000000001', 'errand-1', '2281', 'NAMESPACE-1', 'anna.andersson@example.com',
        'Nytt ärende i Draken', 'https://draken.sundsvall.se/kontaktsundsvall/arende/KS-100001', NULL,
        CURRENT_TIMESTAMP(3)),
       ('bbbb1111-0000-0000-0000-000000000002', 'errand-2', '2281', 'NAMESPACE-1', 'bertil.bengtsson@example.com',
        'Nytt ärende i Draken', 'https://draken.sundsvall.se/kontaktsundsvall/arende/KS-100002', 'jep11jep',
        CURRENT_TIMESTAMP(3));
