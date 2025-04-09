CREATE INDEX IF NOT EXISTS idx_notification_municipality_id_namespace_owner_id USING BTREE ON notification (municipality_id,namespace,owner_id);
