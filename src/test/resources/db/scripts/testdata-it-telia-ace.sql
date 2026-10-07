-- Queued Telia ACE work items for TeliaAceWorkItemSchedulerIT.
--
-- One for a newly created errand (no case worker assigned yet, so no predefined_agent_name), one for an errand
-- updated via email that already has an assigned case worker, whose AD id is carried along for personal-queue
-- routing in ACE.
--
-- created uses CURRENT_TIMESTAMP rather than a fixed date: TeliaAceWorkItemWorker drops anything older than its
-- max-age without delivering it, so a stale hardcoded date would silently stop these rows ever reaching Telia ACE.

INSERT INTO telia_ace_work_item (id, errand_id, municipality_id, namespace, from_address, subject, content_url, predefined_agent_name, created)
VALUES ('bbbb1111-0000-0000-0000-000000000001', 'errand-1', '2281', 'NAMESPACE-1', 'anna.andersson@example.com',
        'Nytt ärende i Draken', 'https://draken.sundsvall.se/kontaktsundsvall/arende/KS-100001', NULL,
        CURRENT_TIMESTAMP(3)),
       ('bbbb1111-0000-0000-0000-000000000002', 'errand-2', '2281', 'NAMESPACE-1', 'bertil.bengtsson@example.com',
        'Uppdaterat ärende i Draken', 'https://draken.sundsvall.se/kontaktsundsvall/arende/KS-100002', 'jep11jep',
        CURRENT_TIMESTAMP(3));
