START TRANSACTION;

INSERT INTO stakeholder_parameter (stakeholder_id, parameters_key)
  SELECT sm.stakeholder_id, sm.metadata_key FROM stakeholder_metadata sm;

INSERT INTO stakeholder_parameter_values (stakeholder_parameter_id, value)
  SELECT sp.id, sm.metadata FROM stakeholder_metadata AS sm, stakeholder_parameter AS sp WHERE sp.stakeholder_id = sm.stakeholder_id AND sm.metadata_key = sp.parameters_key;
 
COMMIT;