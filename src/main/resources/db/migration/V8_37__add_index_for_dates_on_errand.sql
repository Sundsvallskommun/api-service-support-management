CREATE INDEX IF NOT EXISTS idx_errand_municipality_id_namespace_created USING BTREE ON errand (municipality_id,namespace,created);
CREATE INDEX IF NOT EXISTS idx_errand_suspended_to USING BTREE ON errand (suspended_to);
ALTER TABLE IF EXISTS errand RENAME INDEX idx_errand_number TO idx_errand_errand_number;
