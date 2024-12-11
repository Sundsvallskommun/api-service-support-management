CREATE INDEX IF NOT EXISTS idx_errand_municipality_id_namespace_created USING BTREE ON errand (municipality_id,namespace,created);
CREATE INDEX IF NOT EXISTS idx_errand_suspended_to USING BTREE ON errand (suspended_to);
