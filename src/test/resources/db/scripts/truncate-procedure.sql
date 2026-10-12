CREATE OR REPLACE PROCEDURE truncate_tables()
BEGIN
	DECLARE EXIT HANDLER FOR SQLEXCEPTION
	BEGIN
		SET FOREIGN_KEY_CHECKS = 1;
		RESIGNAL;
	END;
	SET FOREIGN_KEY_CHECKS = 0;
	FOR t IN (SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE' AND table_name NOT IN ('flyway_schema_history', 'shedlock')) DO
		EXECUTE IMMEDIATE CONCAT('TRUNCATE TABLE `', t.table_name, '`');
	END FOR;
	SET FOREIGN_KEY_CHECKS = 1;
END
