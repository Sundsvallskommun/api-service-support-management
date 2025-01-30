ALTER TABLE if exists parameter
    ADD COLUMN if not exists parameter_order INTEGER NOT NULL DEFAULT 0;
ALTER TABLE if exists parameter
    ADD COLUMN if not exists parameter_group VARCHAR(255);
ALTER TABLE if exists parameter_values
    ADD COLUMN if not exists value_order INTEGER NOT NULL DEFAULT 0;

START TRANSACTION;
-- Create a temporary table to store the calculated parameter order
CREATE TEMPORARY TABLE sorted_parameters AS
SELECT
 parameter.id AS parameter_id,
 ROW_NUMBER() OVER (PARTITION BY parameter.errand_id ORDER BY parameter.id) - 1 AS parameter_order
FROM parameter;

-- Update the parameter table with the calculated parameter order
UPDATE parameter p
JOIN sorted_parameters sp ON p.id = sp.parameter_id
SET p.parameter_order = sp.parameter_order;

-- Clean up the temporary table
DROP TEMPORARY TABLE IF EXISTS sorted_parameters;

-- Create a temporary table to store the calculated value order
CREATE TEMPORARY TABLE sorted_parameter_values AS
SELECT
 parameter_values.parameter_id AS parameter_id,
 parameter_values.value AS value,
 ROW_NUMBER() OVER (PARTITION BY parameter_values.parameter_id ORDER BY parameter_values.value) - 1 AS value_order
FROM parameter_values;

TRUNCATE TABLE parameter_values;

-- Update the parameter table with the calculated parameter order
INSERT INTO parameter_values (parameter_id, value, value_order)
  SELECT sorted_parameter_values.parameter_id, sorted_parameter_values.value, sorted_parameter_values.value_order
  FROM sorted_parameter_values;

-- Clean up the temporary table
DROP TEMPORARY TABLE IF EXISTS sorted_parameter_values;

-- Add primary key to parameter_values table
ALTER TABLE if exists parameter_values
    ADD PRIMARY KEY if not exists (value_order, parameter_id);
COMMIT;