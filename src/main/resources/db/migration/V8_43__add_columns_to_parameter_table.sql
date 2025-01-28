ALTER TABLE parameter
    ADD COLUMN parameter_order INTEGER NOT NULL DEFAULT 0;
ALTER TABLE parameter
    ADD COLUMN parameter_group VARCHAR(255);

START TRANSACTION;
-- Create a temporary table to store the calculated parameter order
CREATE TEMPORARY TABLE sorted_parameters AS
SELECT
 parameter.id AS parameter_id,
 ROW_NUMBER() OVER (PARTITION BY parameter.errand_id ORDER BY parameter.id) - 1 AS order_number
FROM parameter;

-- Update the parameter table with the calculated parameter order
UPDATE parameter p
JOIN sorted_parameters sp ON p.id = sp.parameter_id
SET p.parameter_order = sp.order_number;

-- Clean up the temporary table
DROP TEMPORARY TABLE IF EXISTS sorted_parameters;

COMMIT;