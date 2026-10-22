-- -----------------------------------------------------------------------------------------------
-- A scheduled ADD_LABEL, on top of testdata-it.sql and testdata-process-event.sql, that gives an
-- errand without labels a label naming no process itself, under the label naming the process of
-- NAMESPACE-1 - and is already due.
--
-- Kept apart from testdata-process-event.sql: an active label action in NAMESPACE-1 would act on the
-- errand writes of every other test that loads that script, and the child would show up in the label
-- tree the metadata tests read back.
-- -----------------------------------------------------------------------------------------------

INSERT INTO metadata_label (created, modified, municipality_id, namespace, classification, display_name, id, parent_id,
                            resource_name, resource_path, deprecated)
VALUES ('2025-10-16 09:45:49.815000', NULL, '2281', 'NAMESPACE-1', 'TYPE', 'SOME_TYPE_1',
        'ce000000-0000-0000-0000-0000000000e1', '5940c8c8-d84a-4144-b650-313356ad1333', 'TYPE-1', 'LABEL-1/TYPE-1', false);

INSERT INTO action_config(id, municipality_id, namespace, name, active, display_value, created)
VALUES ('ac-add-child-label', '2281', 'NAMESPACE-1', 'ADD_LABEL', 1, 'The child label is added after an hour', now());

INSERT INTO action_config_parameter(id, action_config_id, parameter_key)
VALUES ('param-child-label', 'ac-add-child-label', 'label'),
       ('param-child-label-duration', 'ac-add-child-label', 'duration');

INSERT INTO action_config_parameter_values(action_config_parameter_id, value, value_order)
VALUES ('param-child-label', 'ce000000-0000-0000-0000-0000000000e1', 0),
       ('param-child-label-duration', 'PT1H', 0);

INSERT INTO errand_action(id, errand_id, execute_after, action_config_id)
VALUES ('ea-add-child-label', 'cc236cf1-c00f-4479-8341-ecf5dd90b5b9', '2020-01-01 00:00:00.000', 'ac-add-child-label');
