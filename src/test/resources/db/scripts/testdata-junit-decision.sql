-- Two decisions on the same errand, one concluded and one not, each linked to an attachment and resting on an
-- investigation of its own. The attachment of the other errand is linked to nothing.
INSERT INTO investigation(id, errand_id, municipality_id, namespace, status, created, version)
VALUES ('investigation-completed', 'ERRAND_ID-2', '2281', 'NAMESPACE.1', 'COMPLETED', '2026-01-01 09:00:00.000', 0),
       ('investigation-draft', 'ERRAND_ID-2', '2281', 'NAMESPACE.1', 'ACTIVE', '2026-01-01 09:00:00.000', 0),
       ('investigation-unused', 'ERRAND_ID-2', '2281', 'NAMESPACE.1', 'ACTIVE', '2026-01-01 09:00:00.000', 0);

INSERT INTO decision(id, errand_id, municipality_id, namespace, status, outcome, method, decided_by, decided_at,
                     investigation_id, created, version)
VALUES ('decision-completed', 'ERRAND_ID-2', '2281', 'NAMESPACE.1', 'COMPLETED', 'APPROVAL', 'MANUAL', 'joe01doe',
        '2026-01-01 10:00:00.000', 'investigation-completed', '2026-01-01 10:00:00.000', 0),
       ('decision-draft', 'ERRAND_ID-2', '2281', 'NAMESPACE.1', 'DRAFT', 'APPROVAL', 'MANUAL', 'joe01doe',
        '2026-01-01 10:00:00.000', 'investigation-draft', '2026-01-01 10:00:00.000', 0);

INSERT INTO decision_attachment(decision_id, attachment_id)
VALUES ('decision-completed', 'ATTACHMENT_ID-2'),
       ('decision-draft', 'ATTACHMENT_ID-3');
