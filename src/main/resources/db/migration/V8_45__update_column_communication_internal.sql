UPDATE communication
SET internal = 0
WHERE internal IS NULL;

ALTER TABLE communication
ALTER COLUMN internal SET DEFAULT 0;
