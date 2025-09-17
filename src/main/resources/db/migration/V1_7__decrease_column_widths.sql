ALTER TABLE attachment MODIFY COLUMN namespace VARCHAR(32);
ALTER TABLE attachment MODIFY COLUMN municipality_id VARCHAR(8);

ALTER TABLE category MODIFY COLUMN namespace VARCHAR(32);
ALTER TABLE category MODIFY COLUMN municipality_id VARCHAR(8);

ALTER TABLE communication MODIFY COLUMN namespace VARCHAR(32);
ALTER TABLE communication MODIFY COLUMN municipality_id VARCHAR(8);

ALTER TABLE communication_attachment MODIFY COLUMN namespace VARCHAR(32);
ALTER TABLE communication_attachment MODIFY COLUMN municipality_id VARCHAR(8);

ALTER TABLE contact_reason MODIFY COLUMN namespace VARCHAR(32);
ALTER TABLE contact_reason MODIFY COLUMN municipality_id VARCHAR(8);

ALTER TABLE conversation MODIFY COLUMN namespace VARCHAR(32);
ALTER TABLE conversation MODIFY COLUMN municipality_id VARCHAR(8);

ALTER TABLE email_worker_config MODIFY COLUMN namespace VARCHAR(32);
ALTER TABLE email_worker_config MODIFY COLUMN municipality_id VARCHAR(8);

ALTER TABLE errand MODIFY COLUMN namespace VARCHAR(32);
ALTER TABLE errand MODIFY COLUMN municipality_id VARCHAR(8);
ALTER TABLE errand MODIFY COLUMN status VARCHAR(64);
ALTER TABLE errand MODIFY COLUMN type VARCHAR(128);

ALTER TABLE errand_number_sequence MODIFY COLUMN namespace VARCHAR(32);
ALTER TABLE errand_number_sequence MODIFY COLUMN municipality_id VARCHAR(8);

ALTER TABLE external_id_type MODIFY COLUMN namespace VARCHAR(32);
ALTER TABLE external_id_type MODIFY COLUMN municipality_id VARCHAR(8);

ALTER TABLE label MODIFY COLUMN namespace VARCHAR(32);
ALTER TABLE label MODIFY COLUMN municipality_id VARCHAR(8);

ALTER TABLE message_exchange_sync MODIFY COLUMN namespace VARCHAR(32);
ALTER TABLE message_exchange_sync MODIFY COLUMN municipality_id VARCHAR(8);

ALTER TABLE namespace_config MODIFY COLUMN namespace VARCHAR(32);
ALTER TABLE namespace_config MODIFY COLUMN municipality_id VARCHAR(8);

ALTER TABLE notification MODIFY COLUMN namespace VARCHAR(32);
ALTER TABLE notification MODIFY COLUMN municipality_id VARCHAR(8);

ALTER TABLE revision MODIFY COLUMN namespace VARCHAR(32);
ALTER TABLE revision MODIFY COLUMN municipality_id VARCHAR(8);

ALTER TABLE role MODIFY COLUMN namespace VARCHAR(32);
ALTER TABLE role MODIFY COLUMN municipality_id VARCHAR(8);

ALTER TABLE status MODIFY COLUMN namespace VARCHAR(32);
ALTER TABLE status MODIFY COLUMN municipality_id VARCHAR(8);

ALTER TABLE validation MODIFY COLUMN namespace VARCHAR(32);
ALTER TABLE validation MODIFY COLUMN municipality_id VARCHAR(8);

ALTER TABLE web_message_collect MODIFY COLUMN namespace VARCHAR(32);
ALTER TABLE web_message_collect MODIFY COLUMN municipality_id VARCHAR(8);