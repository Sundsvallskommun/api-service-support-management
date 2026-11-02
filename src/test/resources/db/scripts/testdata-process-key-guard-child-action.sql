-- -----------------------------------------------------------------------------------------------
-- A due run of a scheduled ADD_LABEL, on top of testdata-it.sql and testdata-process-key-guard.sql, that
-- would give the errand running the application process a label naming no process itself, under the
-- label naming the serving process.
--
-- Kept apart from testdata-process-key-guard.sql: an active label action in PROCESS-NAMESPACE would act on
-- the errand writes of every other test that loads that script.
-- -----------------------------------------------------------------------------------------------

INSERT INTO action_config(id, municipality_id, namespace, name, active, display_value, created)
VALUES ('ac-add-stadigvarande-label', '2281', 'PROCESS-NAMESPACE', 'ADD_LABEL', 1, 'The permanent serving label is added after an hour', now());

INSERT INTO action_config_parameter(id, action_config_id, parameter_key)
VALUES ('param-stadigvarande-label', 'ac-add-stadigvarande-label', 'label'),
       ('param-stadigvarande-label-duration', 'ac-add-stadigvarande-label', 'duration');

INSERT INTO action_config_parameter_values(action_config_parameter_id, value, value_order)
VALUES ('param-stadigvarande-label', 'bb000000-0000-0000-0000-0000000000b5', 0),
       ('param-stadigvarande-label-duration', 'PT1H', 0);

INSERT INTO errand_action(id, errand_id, execute_after, action_config_id)
VALUES ('ea-add-stadigvarande-label', 'aa000000-0000-0000-0000-0000000000a1', '2020-01-01 00:00:00.000', 'ac-add-stadigvarande-label');
