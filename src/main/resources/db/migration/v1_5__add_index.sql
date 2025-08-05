CREATE INDEX IF NOT EXISTS  idx_errand_municipality_id_namespace_status_modified USING BTREE
       ON errand (municipality_id, namespace, status, modified);

CREATE INDEX IF NOT EXISTS idx_external_tag_value USING BTREE
	ON external_tag (value);

