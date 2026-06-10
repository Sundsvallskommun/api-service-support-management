-- -----------------------------------
-- Action Config for SEND_EMAIL action
-- Uses NAMESPACE-1, municipality 2281
-- Uses STATUS-4/STATUS-5 to avoid overlap with ADD_LABEL configs (which use STATUS-1/STATUS-2)
-- -----------------------------------

-- Add statuses for SEND_EMAIL tests
INSERT INTO status(id, created, modified, municipality_id, name, display_name, external_display_name, sort_order, namespace, deprecated)
VALUES ('bb000000-0000-0000-0000-000000000200', '2023-01-01 12:00:00.000', null, '2281', 'STATUS-4', 'Status 4', 'External Status 4', null, 'NAMESPACE-1', false),
       ('bb000000-0000-0000-0000-000000000201', '2023-01-01 12:00:00.000', null, '2281', 'STATUS-5', 'Status 5', 'External Status 5', null, 'NAMESPACE-1', false);

-- Action config: SEND_EMAIL with status condition and duration (action will be scheduled)
INSERT INTO action_config(id, municipality_id, namespace, name, active, display_value, created)
VALUES ('ac-send-email-with-duration', '2281', 'NAMESPACE-1', 'SEND_EMAIL', 1, 'Email will be sent after 24h', now());

INSERT INTO action_config_condition(id, action_config_id, condition_key)
VALUES ('cond-email-status-1', 'ac-send-email-with-duration', 'status');

INSERT INTO action_config_condition_values(action_config_condition_id, value, value_order)
VALUES ('cond-email-status-1', 'STATUS-4', 0);

INSERT INTO action_config_parameter(id, action_config_id, parameter_key)
VALUES ('param-email-recipient-1', 'ac-send-email-with-duration', 'recipient'),
       ('param-email-sender-1', 'ac-send-email-with-duration', 'sender'),
       ('param-email-subject-1', 'ac-send-email-with-duration', 'subject'),
       ('param-email-body-1', 'ac-send-email-with-duration', 'body'),
       ('param-email-addlink-1', 'ac-send-email-with-duration', 'addLinkToErrandInBody'),
       ('param-email-duration-1', 'ac-send-email-with-duration', 'duration');

INSERT INTO action_config_parameter_values(action_config_parameter_id, value, value_order)
VALUES ('param-email-recipient-1', 'recipient@test.com', 0),
       ('param-email-sender-1', 'sender@test.com', 0),
       ('param-email-subject-1', 'Scheduled email subject', 0),
       ('param-email-body-1', 'Scheduled email body', 0),
       ('param-email-addlink-1', 'false', 0),
       ('param-email-duration-1', 'PT24H', 0);

-- Action config: SEND_EMAIL with status condition, without duration (action will be executed immediately)
INSERT INTO action_config(id, municipality_id, namespace, name, active, display_value, created)
VALUES ('ac-send-email-immediate', '2281', 'NAMESPACE-1', 'SEND_EMAIL', 1, 'Email sent immediately', now());

INSERT INTO action_config_condition(id, action_config_id, condition_key)
VALUES ('cond-email-status-2', 'ac-send-email-immediate', 'status');

INSERT INTO action_config_condition_values(action_config_condition_id, value, value_order)
VALUES ('cond-email-status-2', 'STATUS-5', 0);

INSERT INTO action_config_parameter(id, action_config_id, parameter_key)
VALUES ('param-email-recipient-2', 'ac-send-email-immediate', 'recipient'),
       ('param-email-sender-2', 'ac-send-email-immediate', 'sender'),
       ('param-email-subject-2', 'ac-send-email-immediate', 'subject'),
       ('param-email-body-2', 'ac-send-email-immediate', 'body'),
       ('param-email-addlink-2', 'ac-send-email-immediate', 'addLinkToErrandInBody');

INSERT INTO action_config_parameter_values(action_config_parameter_id, value, value_order)
VALUES ('param-email-recipient-2', 'recipient@test.com', 0),
       ('param-email-sender-2', 'sender@test.com', 0),
       ('param-email-subject-2', 'Immediate email subject', 0),
       ('param-email-body-2', 'Immediate email body', 0),
       ('param-email-addlink-2', 'false', 0);