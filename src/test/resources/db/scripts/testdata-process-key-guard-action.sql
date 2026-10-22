-- -----------------------------------------------------------------------------------------------
-- Two due runs of a scheduled ADD_LABEL, on top of testdata-it.sql and testdata-process-key-guard.sql, that
-- would give the errand running the application process the label naming the supervision process.
--
-- Kept apart from testdata-process-key-guard.sql: an active label action in PROCESS-NAMESPACE would act on
-- the errand writes of every other test that loads that script.
-- -----------------------------------------------------------------------------------------------

INSERT INTO action_config(id, municipality_id, namespace, name, active, display_value, created)
VALUES ('ac-add-tillsyn-label', '2281', 'PROCESS-NAMESPACE', 'ADD_LABEL', 1, 'The supervision label is added after an hour', now());

INSERT INTO action_config_parameter(id, action_config_id, parameter_key)
VALUES ('param-tillsyn-label', 'ac-add-tillsyn-label', 'label'),
       ('param-tillsyn-label-duration', 'ac-add-tillsyn-label', 'duration');

INSERT INTO action_config_parameter_values(action_config_parameter_id, value, value_order)
VALUES ('param-tillsyn-label', 'bb000000-0000-0000-0000-0000000000b2', 0),
       ('param-tillsyn-label-duration', 'PT1H', 0);

INSERT INTO errand_action(id, errand_id, execute_after, action_config_id)
VALUES ('ea-add-tillsyn-label-1', 'aa000000-0000-0000-0000-0000000000a1', '2020-01-01 00:00:00.000', 'ac-add-tillsyn-label'),
       ('ea-add-tillsyn-label-2', 'aa000000-0000-0000-0000-0000000000a1', '2020-01-01 00:01:00.000', 'ac-add-tillsyn-label');
