-- Add measure_type_id column
ALTER TABLE measure ADD COLUMN measure_type_id VARCHAR(255);

-- Backfill from existing type name via errand's namespace and municipality
UPDATE measure m
JOIN errand e ON m.errand_id = e.id
JOIN measure_type mt ON mt.name = m.type
    AND mt.namespace = e.namespace
    AND mt.municipality_id = e.municipality_id
SET m.measure_type_id = mt.id;

-- Drop the old type column
ALTER TABLE measure DROP COLUMN type;

-- Add foreign key constraint and index
ALTER TABLE measure ADD CONSTRAINT fk_measure_measure_type_id
    FOREIGN KEY (measure_type_id) REFERENCES measure_type(id);

CREATE INDEX idx_measure_measure_type_id ON measure(measure_type_id);