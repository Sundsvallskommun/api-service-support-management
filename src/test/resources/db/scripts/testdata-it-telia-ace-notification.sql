-- Dedicated, self-contained fixture for EmailReaderTeliaAceNotificationIT: a namespace with the Telia ACE work item
-- feature enabled and one email worker config, so processing its one queued email creates a brand new errand and -
-- for real, against a real database and the real @TransactionalEventListener wiring, not mocks - queues a Telia ACE
-- work item for it only once that errand's own transaction has committed.

INSERT INTO email_worker_config (id, enabled, municipality_id, namespace, status_for_new, errand_channel,
                                 add_sender_as_stakeholder, ignore_auto_reply, ignore_no_reply, created, modified)
VALUES (1, true, '2281', 'NAMESPACE-TA', 'NEW', 'EMAIL', false, false, false, '2026-01-01 00:00:00.000', null);

INSERT INTO namespace_config (id, municipality_id, namespace, created, modified)
VALUES (1, '2281', 'NAMESPACE-TA', '2026-01-01 00:00:00.000', null);

-- DISPLAY_NAME, SHORT_CODE, ACCESS_CONTROL, NOTIFY_REPORTER and NOTIFICATION_TTL_IN_DAYS are read with
-- ConfigPropertyExtractor.getValue, which throws rather than defaulting when a key is missing - NamespaceConfigMapper
-- treats them as mandatory even though nothing in this flow reads most of them back.
INSERT INTO namespace_config_value (namespace_config_id, `key`, `value`, `type`)
VALUES (1, 'DISPLAY_NAME', 'Namespace TA', 'STRING'),
       (1, 'SHORT_CODE', 'TA', 'STRING'),
       (1, 'ACCESS_CONTROL', 'false', 'BOOLEAN'),
       (1, 'NOTIFY_REPORTER', 'false', 'BOOLEAN'),
       (1, 'NOTIFICATION_TTL_IN_DAYS', '40', 'INTEGER'),
       (1, 'TELIA_ACE_WORK_ITEM_ENABLED', 'true', 'BOOLEAN'),
       (1, 'TELIA_ACE_WORK_ITEM_BASE_URL', 'https://draken.sundsvall.se/namespace-ta', 'STRING');
