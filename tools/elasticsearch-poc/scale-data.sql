-- Scale existing data to 700k errands for PoC performance testing
-- Run AFTER loading sm-test-dump.sql (which provides real base data + metadata)
--
-- Strategy:
--   1. Read existing namespace/category/type/status distributions from real data
--   2. Generate synthetic errands following the same distributions
--   3. Generate synthetic jsonParameters for ~10% of new errands with varied schemas
--
-- Usage:
--   docker exec -i <mariadb> mariadb -uroot -proot supportmanagement < tools/elasticsearch-poc/scale-data.sql

-- Store the existing errand count
SET @existing_count = (SELECT COUNT(*) FROM errand);
SET @target_count = 700000;
SET @to_generate = @target_count - @existing_count;

SELECT CONCAT('Existing errands: ', @existing_count, ', generating ', @to_generate, ' synthetic errands') AS status;

-- Collect real distributions into temp tables
DROP TEMPORARY TABLE IF EXISTS tmp_ns_dist;
CREATE TEMPORARY TABLE tmp_ns_dist AS
SELECT namespace, category, type, municipality_id, COUNT(*) as cnt
FROM errand
GROUP BY namespace, category, type, municipality_id;

DROP TEMPORARY TABLE IF EXISTS tmp_status_dist;
CREATE TEMPORARY TABLE tmp_status_dist AS
SELECT status, COUNT(*) as cnt FROM errand GROUP BY status;

-- Get total for proportional sampling
SET @total_ns = (SELECT SUM(cnt) FROM tmp_ns_dist);
SET @total_status = (SELECT SUM(cnt) FROM tmp_status_dist);

-- Collect unique groups and statuses into indexed temp tables for random selection
DROP TEMPORARY TABLE IF EXISTS tmp_ns_indexed;
CREATE TEMPORARY TABLE tmp_ns_indexed (
    idx INT AUTO_INCREMENT PRIMARY KEY,
    namespace VARCHAR(32),
    category VARCHAR(255),
    type VARCHAR(255),
    municipality_id VARCHAR(8),
    weight INT
);
INSERT INTO tmp_ns_indexed (namespace, category, type, municipality_id, weight)
SELECT namespace, category, type, municipality_id, cnt FROM tmp_ns_dist;

SET @ns_count = (SELECT COUNT(*) FROM tmp_ns_indexed);

DROP TEMPORARY TABLE IF EXISTS tmp_status_indexed;
CREATE TEMPORARY TABLE tmp_status_indexed (
    idx INT AUTO_INCREMENT PRIMARY KEY,
    status VARCHAR(255),
    weight INT
);
INSERT INTO tmp_status_indexed (status, weight)
SELECT status, cnt FROM tmp_status_dist;

SET @status_count = (SELECT COUNT(*) FROM tmp_status_indexed);

-- Get distinct assigned groups and users for variety
DROP TEMPORARY TABLE IF EXISTS tmp_groups;
CREATE TEMPORARY TABLE tmp_groups (
    idx INT AUTO_INCREMENT PRIMARY KEY,
    group_id VARCHAR(255)
);
INSERT INTO tmp_groups (group_id)
SELECT DISTINCT assigned_group_id FROM errand WHERE assigned_group_id IS NOT NULL LIMIT 50;
SET @group_count = (SELECT COUNT(*) FROM tmp_groups);

DROP TEMPORARY TABLE IF EXISTS tmp_users;
CREATE TEMPORARY TABLE tmp_users (
    idx INT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255)
);
INSERT INTO tmp_users (user_id)
SELECT DISTINCT assigned_user_id FROM errand WHERE assigned_user_id IS NOT NULL LIMIT 50;
SET @user_count = (SELECT COUNT(*) FROM tmp_users);

-- Sample titles from real data
DROP TEMPORARY TABLE IF EXISTS tmp_titles;
CREATE TEMPORARY TABLE tmp_titles (
    idx INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255)
);
INSERT INTO tmp_titles (title)
SELECT DISTINCT SUBSTRING(title, 1, 255) FROM errand WHERE title IS NOT NULL AND title != '' LIMIT 200;
SET @title_count = (SELECT COUNT(*) FROM tmp_titles);

-- Generate synthetic errands in batches
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS scale_errands()
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE batch_size INT DEFAULT 5000;
    DECLARE errand_uuid VARCHAR(255);
    DECLARE ns_idx INT;
    DECLARE status_idx INT;
    DECLARE group_idx INT;
    DECLARE user_idx INT;
    DECLARE title_idx INT;
    DECLARE v_namespace VARCHAR(32);
    DECLARE v_category VARCHAR(255);
    DECLARE v_type VARCHAR(255);
    DECLARE v_municipality VARCHAR(8);
    DECLARE v_status VARCHAR(255);
    DECLARE v_group VARCHAR(255);
    DECLARE v_user VARCHAR(255);
    DECLARE v_title VARCHAR(255);
    DECLARE v_priority VARCHAR(20);
    DECLARE v_created DATETIME(6);
    DECLARE v_errand_number VARCHAR(255);
    DECLARE jp_schema_version VARCHAR(10);
    DECLARE jp_facility_key VARCHAR(50);
    DECLARE jp_facility_id VARCHAR(20);
    DECLARE jp_address VARCHAR(100);

    WHILE i < @to_generate DO
        SET errand_uuid = UUID();

        -- Weighted random namespace/category/type selection
        SET ns_idx = 1 + FLOOR(RAND() * @ns_count);
        SELECT namespace, category, type, municipality_id
        INTO v_namespace, v_category, v_type, v_municipality
        FROM tmp_ns_indexed WHERE idx = ns_idx;

        -- Weighted random status
        SET status_idx = 1 + FLOOR(RAND() * @status_count);
        SELECT status INTO v_status FROM tmp_status_indexed WHERE idx = status_idx;

        -- Random group/user
        IF @group_count > 0 THEN
            SET group_idx = 1 + FLOOR(RAND() * @group_count);
            SELECT group_id INTO v_group FROM tmp_groups WHERE idx = group_idx;
        ELSE
            SET v_group = NULL;
        END IF;

        IF @user_count > 0 THEN
            SET user_idx = 1 + FLOOR(RAND() * @user_count);
            SELECT user_id INTO v_user FROM tmp_users WHERE idx = user_idx;
        ELSE
            SET v_user = NULL;
        END IF;

        -- Random title from real data
        IF @title_count > 0 THEN
            SET title_idx = 1 + FLOOR(RAND() * @title_count);
            SELECT title INTO v_title FROM tmp_titles WHERE idx = title_idx;
        ELSE
            SET v_title = CONCAT('Synthetic errand #', i);
        END IF;

        -- Random priority
        SET v_priority = ELT(1 + FLOOR(RAND() * 3), 'LOW', 'MEDIUM', 'HIGH');

        -- Random date in last 2 years
        SET v_created = DATE_ADD('2024-01-01', INTERVAL FLOOR(RAND() * 730) DAY);

        -- Unique errand number
        SET v_errand_number = CONCAT('KC-SYN-', LPAD(i + 1, 7, '0'));

        INSERT INTO errand(municipality_id, id, assigned_group_id, assigned_user_id, category, namespace,
                           priority, reporter_user_id, status, title, type, created, errand_number,
                           business_related, previous_status, touched)
        VALUES (v_municipality, errand_uuid, v_group, v_user, v_category, v_namespace,
                v_priority, CONCAT('REPORTER-', FLOOR(RAND() * 100)), v_status,
                v_title, v_type, v_created, v_errand_number, false, 'NEW', v_created);

        -- ~10% of errands get jsonParameters with varied schemas
        -- This simulates real usage where some namespaces have json schemas
        IF FLOOR(RAND() * 10) = 0 THEN
            -- Rotate through 8 schema versions with different key names (the "16 keys" scenario)
            CASE FLOOR(RAND() * 8)
                WHEN 0 THEN
                    SET jp_schema_version = '1.0'; SET jp_facility_key = 'facilityId';
                WHEN 1 THEN
                    SET jp_schema_version = '1.1'; SET jp_facility_key = 'anläggningsId';
                WHEN 2 THEN
                    SET jp_schema_version = '2.0'; SET jp_facility_key = 'facility_id';
                WHEN 3 THEN
                    SET jp_schema_version = '2.1'; SET jp_facility_key = 'anlaggning';
                WHEN 4 THEN
                    SET jp_schema_version = '3.0'; SET jp_facility_key = 'anlaggningsNr';
                WHEN 5 THEN
                    SET jp_schema_version = '3.1'; SET jp_facility_key = 'fastighetsId';
                WHEN 6 THEN
                    SET jp_schema_version = '4.0'; SET jp_facility_key = 'propertyId';
                ELSE
                    SET jp_schema_version = '4.1'; SET jp_facility_key = 'objektId';
            END CASE;

            SET jp_facility_id = CONCAT('FAC-', LPAD(FLOOR(RAND() * 2000) + 1, 4, '0'));
            SET jp_address = CONCAT(
                ELT(1 + FLOOR(RAND() * 10), 'Storgatan', 'Lillgatan', 'Parkvägen', 'Industrivägen',
                    'Sjögatan', 'Bergsgatan', 'Tallvägen', 'Ekvägen', 'Norrlandsgatan', 'Kyrkogatan'),
                ' ', FLOOR(RAND() * 100) + 1);

            INSERT INTO json_parameter(id, errand_id, parameter_key, schema_id, value)
            VALUES (UUID(), errand_uuid, 'facility',
                    CONCAT(v_municipality, '_facility_', jp_schema_version),
                    JSON_OBJECT(jp_facility_key, jp_facility_id,
                                'address', jp_address,
                                'type', ELT(1 + FLOOR(RAND() * 8), 'building', 'park', 'school', 'road', 'bridge', 'playground', 'office', 'warehouse')));

            -- 30% of those also get an inspection parameter
            IF FLOOR(RAND() * 3) = 0 THEN
                INSERT INTO json_parameter(id, errand_id, parameter_key, schema_id, value)
                VALUES (UUID(), errand_uuid, 'inspection',
                        CONCAT(v_municipality, '_inspection_1.0'),
                        JSON_OBJECT('inspectorName',
                            ELT(1 + FLOOR(RAND() * 10), 'Anna Svensson', 'Erik Johansson', 'Maria Lindberg',
                                'Lars Andersson', 'Karin Nilsson', 'Olof Bergström', 'Sara Ekman',
                                'Johan Holmgren', 'Eva Sandberg', 'Anders Nyström'),
                            'result', ELT(1 + FLOOR(RAND() * 3), 'approved', 'rejected', 'pending'),
                            'date', DATE_FORMAT(DATE_ADD('2024-01-01', INTERVAL FLOOR(RAND() * 730) DAY), '%Y-%m-%d')));
            END IF;

            -- 20% get a healthcare deviation parameter (matching real schema patterns)
            IF FLOOR(RAND() * 5) = 0 THEN
                INSERT INTO json_parameter(id, errand_id, parameter_key, schema_id, value)
                VALUES (UUID(), errand_uuid, 'avvikelse-plats-handelse',
                        CONCAT(v_municipality, '_avvikelse-plats-handelse_1.2'),
                        JSON_OBJECT('facilityInfo', JSON_OBJECT(
                            'orgId', FLOOR(RAND() * 10000),
                            'orgName', ELT(1 + FLOOR(RAND() * 6),
                                'VOF HOS Korttidsboende 1', 'VOF HOS Hemtjänst Centrum',
                                'KSK AVD Digitalisering IT stab', 'SBK GA Drift Gata',
                                'BOU Skola Norra', 'IAF Handläggning')
                        ),
                        'eventDate', DATE_FORMAT(DATE_ADD('2024-01-01', INTERVAL FLOOR(RAND() * 730) DAY), '%Y-%m-%d'),
                        'eventDescription', CONCAT('Händelsebeskrivning för ärende ', i)));
            END IF;
        END IF;

        SET i = i + 1;

        -- Progress logging every 50000
        IF i % 50000 = 0 THEN
            SELECT CONCAT('Generated ', i, ' / ', @to_generate, ' errands') AS progress;
        END IF;

    END WHILE;
END //
DELIMITER ;

CALL scale_errands();
DROP PROCEDURE IF EXISTS scale_errands;

-- Report
SELECT CONCAT('Total errands: ', COUNT(*)) AS result FROM errand;
SELECT CONCAT('Total jsonParameters: ', COUNT(*)) AS result FROM json_parameter;
SELECT CONCAT('Synthetic jsonParameters: ', COUNT(*) - 18) AS result FROM json_parameter;

-- Cleanup temp tables
DROP TEMPORARY TABLE IF EXISTS tmp_ns_dist;
DROP TEMPORARY TABLE IF EXISTS tmp_status_dist;
DROP TEMPORARY TABLE IF EXISTS tmp_ns_indexed;
DROP TEMPORARY TABLE IF EXISTS tmp_status_indexed;
DROP TEMPORARY TABLE IF EXISTS tmp_groups;
DROP TEMPORARY TABLE IF EXISTS tmp_users;
DROP TEMPORARY TABLE IF EXISTS tmp_titles;
