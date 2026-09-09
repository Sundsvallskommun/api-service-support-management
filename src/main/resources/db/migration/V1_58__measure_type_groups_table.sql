create table measure_type_groups (
    measure_type_id varchar(255) not null,
    measure_group varchar(255),
    constraint fk_measure_type_groups_measure_type_id foreign key (measure_type_id) references measure_type (id)
) engine=InnoDB;

-- Migrate existing data
INSERT INTO measure_type_groups (measure_type_id, measure_group)
SELECT id, measure_group FROM measure_type WHERE measure_group IS NOT NULL;

-- Drop old column
ALTER TABLE measure_type DROP COLUMN measure_group;