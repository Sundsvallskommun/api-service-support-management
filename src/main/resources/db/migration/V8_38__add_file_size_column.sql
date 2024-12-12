ALTER TABLE if exists attachment
    ADD COLUMN if not exists file_size integer;

ALTER TABLE if exists communication_attachment
    ADD COLUMN if not exists file_size integer;

START TRANSACTION;

UPDATE attachment a
    JOIN attachment_data ad
    ON a.attachment_data_id = ad.id
SET a.file_size = OCTET_LENGTH(ad.file)
WHERE a.file_size IS NULL
  AND ad.file IS NOT NULL;

UPDATE communication_attachment ca
    JOIN communication_attachment_data ad
    ON ca.communication_attachment_data_id = ad.id
SET ca.file_size = OCTET_LENGTH(ad.file)
WHERE ca.file_size IS NULL
  AND ad.file IS NOT NULL;

COMMIT;
