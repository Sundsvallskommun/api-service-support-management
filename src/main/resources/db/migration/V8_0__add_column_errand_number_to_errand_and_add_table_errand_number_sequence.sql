alter table if exists errand
    add column if not exists errand_number varchar(255) not null;

create index idx_errand_number on errand (errand_number);

alter table if exists errand
    add constraint uq_errand_number unique (errand_number);

create table errand_number_sequence
(
    namespace varchar(255) not null,
    last_sequence_number integer,
    reset_year_month     varchar(6),
    primary key (namespace)
);


START TRANSACTION;

-- Create a variable to store the sequence number
SET @sequence_number := 0;

-- Get the last_sequence_number and reset_year_month values for 'CONTACTCENTER'

-- Get the last_sequence_number and reset_year_month values for 'CONTACTCENTER'
SELECT IFNULL(last_sequence_number, 0)
INTO @last_sequence_number
FROM errand_number_sequence
WHERE namespace = 'CONTACTCENTER';

-- If no row exists for 'CONTACTCENTER', set default values
SET @last_sequence_number = IF(@last_sequence_number IS NULL, 0, @last_sequence_number);

SET @reset_year_month =
        IF(@last_sequence_number IS NULL, DATE_FORMAT(NOW(), '%y%m'), @reset_year_month);

-- Create a temporary table to store the calculated errand numbers
CREATE TEMPORARY TABLE TempErrandNumbers AS
SELECT errand.id AS errand_id,
       IF(errand.NAMESPACE = 'CONTACTCENTER', 'KC', NULL)      AS shortcode, -- As we only have one atm
       DATE_FORMAT(errand.CREATED, '%y%m')                     AS yearMonth, -- Format as YYMM
       (@sequence_number :=
               IF(@prev_year_month = DATE_FORMAT(errand.CREATED, '%y%m'), @sequence_number + 1,
                  1))                                          AS sequence_number,
       @prev_year_month := DATE_FORMAT(errand.CREATED, '%y%m') AS prev_year_month
FROM errand
ORDER BY errand.CREATED;

-- Update the errand table with the calculated errand numbers
UPDATE errand
    JOIN TempErrandNumbers T ON errand.id = T.errand_id
SET errand.errand_number = CONCAT(T.shortcode, '-', T.yearMonth, LPAD(T.sequence_number, 4, '0'))
WHERE T.shortcode IS NOT NULL;

-- Update the errand_number_sequence table with the new values
INSERT INTO errand_number_sequence (namespace, last_sequence_number, reset_year_month)
VALUES ('CONTACTCENTER', @sequence_number, @prev_year_month)
ON DUPLICATE KEY UPDATE last_sequence_number = VALUES(last_sequence_number),
                        reset_year_month     = VALUES(reset_year_month);

-- Clean up the temporary table
DROP TEMPORARY TABLE IF EXISTS TempErrandNumbers;
COMMIT;
