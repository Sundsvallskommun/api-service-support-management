START TRANSACTION;
ALTER TABLE IF EXISTS communication ADD COLUMN type_temp VARCHAR(255);
UPDATE communication SET type_temp = type;
ALTER TABLE IF EXISTS communication DROP COLUMN type;
ALTER TABLE IF EXISTS communication CHANGE COLUMN IF EXISTS type_temp type VARCHAR(255) not null;
COMMIT;
