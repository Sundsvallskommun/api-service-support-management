START TRANSACTION;
ALTER TABLE communication ADD COLUMN type_temp VARCHAR(255);
UPDATE communication SET type_temp = type;
ALTER TABLE communication DROP COLUMN type;
ALTER TABLE communication CHANGE COLUMN type_temp type VARCHAR(255) not null;
COMMIT;
