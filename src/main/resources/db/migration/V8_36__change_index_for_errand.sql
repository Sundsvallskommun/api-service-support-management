CREATE INDEX IF NOT EXISTS idx_errand_municipality_id_namespace_type USING BTREE ON errand (municipality_id,namespace,`type`);
CREATE INDEX IF NOT EXISTS idx_errand_municipality_id_namespace_status USING BTREE ON errand (municipality_id,namespace,status);
CREATE INDEX IF NOT EXISTS idx_errand_municipality_id_namespace_category USING BTREE ON errand (municipality_id,namespace,category);
CREATE INDEX IF NOT EXISTS idx_errand_municipality_id_namespace_reporter_user_id USING BTREE ON errand (municipality_id,namespace,reporter_user_id);
CREATE INDEX IF NOT EXISTS idx_errand_municipality_id_namespace_assigned_user_id USING BTREE ON errand (municipality_id,namespace,assigned_user_id);

ALTER TABLE IF EXISTS errand DROP INDEX IF EXISTS idx_errand_type;
ALTER TABLE IF EXISTS errand DROP INDEX IF EXISTS idx_errand_status;
ALTER TABLE IF EXISTS errand DROP INDEX IF EXISTS idx_errand_category;
ALTER TABLE IF EXISTS errand DROP INDEX IF EXISTS idx_errand_reporter_user_id;
ALTER TABLE IF EXISTS errand DROP INDEX IF EXISTS idx_errand_assigned_user_id;
