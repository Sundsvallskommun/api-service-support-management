ALTER TABLE if exists errand
    ADD COLUMN if not exists touched timestamp null;

CREATE INDEX if not exists idx_errand_municipality_id_namespace_touched
    ON errand (municipality_id, namespace, touched);

-- Update the touched column for all existing rows
UPDATE errand
SET touched = GREATEST(COALESCE(created, '1970-01-01 00:00:00'), COALESCE(modified, '1970-01-01 00:00:00'))
WHERE touched IS NULL;
