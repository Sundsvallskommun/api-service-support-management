-- -----------------------------------
-- Action Config for ADD_LABEL action
-- Uses NAMESPACE-1, municipality 2281
-- -----------------------------------

-- Action config: ADD_LABEL with status condition and duration (action will be scheduled)
INSERT INTO action_config(id, municipality_id, namespace, name, active, display_value, created)
VALUES ('ac-add-label-with-duration', '2281', 'NAMESPACE-1', 'ADD_LABEL', 1, 'Label will be added after 24h', now());

INSERT INTO action_config_condition(id, action_config_id, condition_key)
VALUES ('cond-status-1', 'ac-add-label-with-duration', 'status');

INSERT INTO action_config_condition_values(action_config_condition_id, value, value_order)
VALUES ('cond-status-1', 'STATUS-1', 0);

INSERT INTO action_config_parameter(id, action_config_id, parameter_key)
VALUES ('param-label-1', 'ac-add-label-with-duration', 'label'),
       ('param-duration-1', 'ac-add-label-with-duration', 'duration');

INSERT INTO action_config_parameter_values(action_config_parameter_id, value, value_order)
VALUES ('param-label-1', 'ffe5f120-6a3b-4404-ace8-8ea87b559907', 0),
       ('param-duration-1', 'PT24H', 0);

-- Action config: ADD_LABEL without duration (action will be executed immediately)
INSERT INTO action_config(id, municipality_id, namespace, name, active, display_value, created)
VALUES ('ac-add-label-immediate', '2281', 'NAMESPACE-1', 'ADD_LABEL', 1, 'Label added immediately', now());

INSERT INTO action_config_condition(id, action_config_id, condition_key)
VALUES ('cond-status-2', 'ac-add-label-immediate', 'status');

INSERT INTO action_config_condition_values(action_config_condition_id, value, value_order)
VALUES ('cond-status-2', 'STATUS-2', 0);

INSERT INTO action_config_parameter(id, action_config_id, parameter_key)
VALUES ('param-label-2', 'ac-add-label-immediate', 'label');

INSERT INTO action_config_parameter_values(action_config_parameter_id, value, value_order)
VALUES ('param-label-2', '0eb1f695-48b1-40fd-af8c-b277c37db2d4', 0);

-- Action config: Inactive ADD_LABEL (should be skipped)
INSERT INTO action_config(id, municipality_id, namespace, name, active, display_value, created)
VALUES ('ac-inactive', '2281', 'NAMESPACE-1', 'ADD_LABEL', 0, 'Inactive action', now());

INSERT INTO action_config_parameter(id, action_config_id, parameter_key)
VALUES ('param-label-3', 'ac-inactive', 'label');

INSERT INTO action_config_parameter_values(action_config_parameter_id, value, value_order)
VALUES ('param-label-3', '926fd3f9-f488-4ba4-93f6-2789dee0c0c3', 0);
