-- -----------------------------------------------------------------------------------------------
-- A scheduled ADD_LABEL, on top of testdata-it.sql and testdata-process-event.sql, that gives an
-- errand without labels the label naming the process of NAMESPACE-1 - and is already due.
--
-- Kept apart from testdata-process-event.sql: an active label action in NAMESPACE-1 would act on the
-- errand writes of every other test that loads that script.
-- -----------------------------------------------------------------------------------------------

INSERT INTO action_config(id, municipality_id, namespace, name, active, display_value, created)
VALUES ('ac-add-process-label', '2281', 'NAMESPACE-1', 'ADD_LABEL', 1, 'The process label is added after an hour', now());

INSERT INTO action_config_parameter(id, action_config_id, parameter_key)
VALUES ('param-process-label', 'ac-add-process-label', 'label'),
       ('param-process-label-duration', 'ac-add-process-label', 'duration');

INSERT INTO action_config_parameter_values(action_config_parameter_id, value, value_order)
VALUES ('param-process-label', '5940c8c8-d84a-4144-b650-313356ad1333', 0),
       ('param-process-label-duration', 'PT1H', 0);

INSERT INTO errand_action(id, errand_id, execute_after, action_config_id)
VALUES ('ea-add-process-label', 'cc236cf1-c00f-4479-8341-ecf5dd90b5b9', '2020-01-01 00:00:00.000', 'ac-add-process-label');
