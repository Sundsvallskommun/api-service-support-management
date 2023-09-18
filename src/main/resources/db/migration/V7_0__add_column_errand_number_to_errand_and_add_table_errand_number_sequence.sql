alter table errand
    add column errand_number varchar(255) not null;

create index idx_errand_number on errand (errand_number);

alter table if exists errand
    add constraint uq_errand_number unique (errand_number);

create table errand_number_sequence
(
    id                   bigint not null auto_increment,
    last_sequence_number integer,
    reset_year_month     varchar(6),
    primary key (id)
);


START TRANSACTION;
-- Create a variable to store the sequence number
SET @sequence_number := 0;

-- Create a temporary table to store the calculated errand numbers
CREATE TEMPORARY TABLE TempErrandNumbers AS
SELECT errand.id                                               AS errand_id,
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

-- Clean up the temporary table
DROP TEMPORARY TABLE IF EXISTS TempErrandNumbers;
COMMIT;
