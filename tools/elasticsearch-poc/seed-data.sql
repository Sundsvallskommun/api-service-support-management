-- Elasticsearch PoC: Seed 50,000 errands with jsonParameters
-- Simulates schema evolution: 8 different key names for "facility ID"
--
-- Usage:
--   docker exec -i <mariadb-container> mariadb -uroot -proot supportmanagement < tools/elasticsearch-poc/seed-data.sql
--   Then call: POST http://localhost:8080/2281/ES-POC/errands/search/_reindex

-- Categories and types needed for FK constraints
INSERT IGNORE INTO category(id, created, display_name, name, municipality_id, namespace)
VALUES (9000, NOW(), 'PoC Category', 'POC-CATEGORY', '2281', 'ES-POC');

INSERT IGNORE INTO type(id, created, display_name, name, category_id)
VALUES (9000, NOW(), 'PoC Type', 'POC-TYPE', 9000);

INSERT IGNORE INTO status(id, created, display_name, name, municipality_id, namespace)
VALUES (9000, NOW(), 'Open', 'OPEN', '2281', 'ES-POC'),
       (9001, NOW(), 'Closed', 'CLOSED', '2281', 'ES-POC'),
       (9002, NOW(), 'In Progress', 'IN_PROGRESS', '2281', 'ES-POC');

-- Generate errands and jsonParameters using a stored procedure
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS seed_es_poc_data()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE errand_uuid VARCHAR(255);
    DECLARE schema_version VARCHAR(50);
    DECLARE facility_key VARCHAR(50);
    DECLARE facility_id_val VARCHAR(20);
    DECLARE address_val VARCHAR(100);
    DECLARE facility_type VARCHAR(30);
    DECLARE status_val VARCHAR(20);
    DECLARE priority_val VARCHAR(10);
    DECLARE inspector_name VARCHAR(50);
    DECLARE env_category VARCHAR(30);

    WHILE i <= 50000 DO
        SET errand_uuid = UUID();

        -- Rotate status
        SET status_val = ELT(1 + (i % 3), 'OPEN', 'CLOSED', 'IN_PROGRESS');
        SET priority_val = ELT(1 + (i % 3), 'LOW', 'MEDIUM', 'HIGH');

        -- Insert errand
        INSERT INTO errand(municipality_id, id, assigned_group_id, assigned_user_id, category, namespace,
                           priority, reporter_user_id, status, title, type, created, errand_number,
                           business_related, previous_status, touched)
        VALUES ('2281', errand_uuid, CONCAT('GROUP-', (i % 10)),
                CONCAT('USER-', (i % 20)), 'POC-CATEGORY', 'ES-POC',
                priority_val, CONCAT('REPORTER-', (i % 15)), status_val,
                CONCAT('Errand #', i, ' - ', ELT(1 + (i % 5), 'Vattenläcka', 'Trasig belysning', 'Potthål i vägen', 'Klotter på fasad', 'Skadad lekplats')),
                'POC-TYPE', DATE_ADD('2024-01-01', INTERVAL (i % 365) DAY),
                CONCAT('KC-POC-', LPAD(i, 5, '0')), false, 'OPEN',
                DATE_ADD('2024-01-01', INTERVAL (i % 365) DAY));

        -- 8 schema versions with different key names for facility ID
        -- Distribution: older schemas more common (simulates real evolution)
        -- v1.0 (30%), v2.0 (25%), v3.0 (15%), v4.0 (10%), v5.0 (8%), v6.0 (5%), v7.0 (4%), v8.0 (3%)
        CASE
            WHEN (i % 100) < 30 THEN
                SET schema_version = '1.0'; SET facility_key = 'facilityId';
            WHEN (i % 100) < 55 THEN
                SET schema_version = '2.0'; SET facility_key = 'anläggningsId';
            WHEN (i % 100) < 70 THEN
                SET schema_version = '3.0'; SET facility_key = 'facility_id';
            WHEN (i % 100) < 80 THEN
                SET schema_version = '4.0'; SET facility_key = 'anlaggning';
            WHEN (i % 100) < 88 THEN
                SET schema_version = '5.0'; SET facility_key = 'anlaggningsNr';
            WHEN (i % 100) < 93 THEN
                SET schema_version = '6.0'; SET facility_key = 'fastighetsId';
            WHEN (i % 100) < 97 THEN
                SET schema_version = '7.0'; SET facility_key = 'propertyId';
            ELSE
                SET schema_version = '8.0'; SET facility_key = 'objektId';
        END CASE;

        -- Facility ID rotates through 500 unique values
        SET facility_id_val = CONCAT('FAC-', LPAD((i % 500) + 1, 4, '0'));

        -- Address varies
        SET address_val = CONCAT(ELT(1 + (i % 8), 'Storgatan', 'Lillgatan', 'Parkvägen', 'Industrivägen', 'Sjögatan', 'Bergsgatan', 'Tallvägen', 'Ekvägen'),
                                 ' ', (i % 50) + 1);
        SET facility_type = ELT(1 + (i % 6), 'building', 'park', 'school', 'road', 'bridge', 'playground');

        -- Insert facility jsonParameter with schema-version-appropriate key
        INSERT INTO json_parameter(id, errand_id, parameter_key, schema_id, value)
        VALUES (UUID(), errand_uuid, 'facility', CONCAT('2281_facility_', schema_version),
                JSON_OBJECT(facility_key, facility_id_val, 'address', address_val, 'type', facility_type));

        -- 30% of errands have an inspection parameter
        IF (i % 3) = 0 THEN
            SET inspector_name = ELT(1 + (i % 10), 'Anna Svensson', 'Erik Johansson', 'Maria Lindberg',
                'Lars Andersson', 'Karin Nilsson', 'Olof Bergström', 'Sara Ekman',
                'Johan Holmgren', 'Eva Sandberg', 'Anders Nyström');
            INSERT INTO json_parameter(id, errand_id, parameter_key, schema_id, value)
            VALUES (UUID(), errand_uuid, 'inspection', '2281_inspection_1.0',
                    JSON_OBJECT('inspectorName', inspector_name,
                                'result', ELT(1 + (i % 3), 'approved', 'rejected', 'pending'),
                                'date', DATE_FORMAT(DATE_ADD('2024-01-01', INTERVAL (i % 365) DAY), '%Y-%m-%d'),
                                'notes', CONCAT('Inspection note for errand ', i)));
        END IF;

        -- 20% of errands have a contact parameter
        IF (i % 5) = 0 THEN
            INSERT INTO json_parameter(id, errand_id, parameter_key, schema_id, value)
            VALUES (UUID(), errand_uuid, 'contact', '2281_contact_1.0',
                    JSON_OBJECT('name', CONCAT('Person ', i),
                                'email', CONCAT('person', i, '@example.com'),
                                'phone', CONCAT('070-', LPAD(i % 10000, 7, '0'))));
        END IF;

        -- 15% of errands have an environment parameter
        IF (i % 7) = 0 THEN
            SET env_category = ELT(1 + (i % 5), 'noise', 'pollution', 'waste', 'water', 'air');
            INSERT INTO json_parameter(id, errand_id, parameter_key, schema_id, value)
            VALUES (UUID(), errand_uuid, 'environment', '2281_environment_1.0',
                    JSON_OBJECT('category', env_category,
                                'severity', ELT(1 + (i % 4), 'low', 'medium', 'high', 'critical'),
                                'location', address_val,
                                'description', CONCAT('Environmental issue: ', env_category, ' at ', address_val)));
        END IF;

        -- 10% of errands have a maintenance parameter
        IF (i % 10) = 0 THEN
            INSERT INTO json_parameter(id, errand_id, parameter_key, schema_id, value)
            VALUES (UUID(), errand_uuid, 'maintenance', '2281_maintenance_1.0',
                    JSON_OBJECT('scheduledDate', DATE_FORMAT(DATE_ADD('2024-06-01', INTERVAL (i % 180) DAY), '%Y-%m-%d'),
                                'contractor', ELT(1 + (i % 4), 'Skanska', 'NCC', 'Peab', 'JM'),
                                'estimatedCost', 10000 + (i % 90000),
                                'workType', ELT(1 + (i % 5), 'repair', 'replacement', 'painting', 'plumbing', 'electrical')));
        END IF;

        SET i = i + 1;
    END WHILE;
END //
DELIMITER ;

CALL seed_es_poc_data();
DROP PROCEDURE IF EXISTS seed_es_poc_data;

SELECT CONCAT('Seeded ', COUNT(*), ' errands') AS result FROM errand WHERE namespace = 'ES-POC';
SELECT CONCAT('Seeded ', COUNT(*), ' jsonParameters') AS result FROM json_parameter jp
    INNER JOIN errand e ON jp.errand_id = e.id WHERE e.namespace = 'ES-POC';
