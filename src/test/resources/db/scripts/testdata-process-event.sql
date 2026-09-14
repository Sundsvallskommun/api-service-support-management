-- -----------------------------------------------------------------------------------------------
-- Turns NAMESPACE-1 into a namespace that runs a process, on top of testdata-it.sql. Used by both the
-- WireMock integration test of the intake and the database tests of the publisher.
--
-- Kept apart from the shared test data on purpose: a process consumer on NAMESPACE-1 would make every
-- errand write of every other integration test publish, and would show up in the namespace config the
-- configuration tests read back.
-- -----------------------------------------------------------------------------------------------

-- The namespace delivers to a process engine, and an incoming message is one of the things that wakes it
INSERT INTO namespace_config_value(namespace_config_id, `key`, `value`, `type`)
VALUES (1, 'PROCESS_CONSUMER', 'pw-alkt', 'STRING'),
       (1, 'PROCESS_TRIGGER', 'MESSAGE', 'STRING'),
       (1, 'PROCESS_TRIGGER', 'ERRAND', 'STRING');

-- The label that says which process the errand belongs to, and the errand wearing it
INSERT INTO metadata_label_attribute (metadata_label_id, `key`, `value`)
VALUES ('5940c8c8-d84a-4144-b650-313356ad1333', 'processKey', 'alkt-ansokan');

INSERT INTO errand_labels(errand_id, metadata_label_id)
VALUES ('ec677eb3-604c-4935-bff7-f8f0b500c8f4', '5940c8c8-d84a-4144-b650-313356ad1333');

-- Nothing in the intake but the message itself: no confirmation mail to send and no status to change,
-- so that what the test observes is the publication and not the rest of the worker
UPDATE email_worker_config
SET status_for_new           = NULL,
    inactive_status          = NULL,
    trigger_status_change_on = NULL,
    status_change_to         = NULL
WHERE id = 1;
