-- Create new tables

create table stakeholder (
       id bigint not null auto_increment,
        stakeholder_id varchar(255),
        type varchar(255),
        errand_id varchar(255) not null,
        primary key (id)
) engine=InnoDB;

-- create table stakeholder (
--
--   errand_id varchar(255) not null,
--   type varchar(255),
--   id varchar(255),
--   first_name varchar(255),
--   last_name varchar(255),
--   address varchar(255),
--   zip_code varchar(255),
--   county varchar(255),
-- ) engine=InnoDB;
--
-- create table contact_channel (
--   stakeholder_id varchar(255) not null,
--   type varchar(255),
--   ´value´ varchar(255)
-- ) engine=InnoDB;

-- Migrate data

insert into stakeholder(errand_id, type, stakeholder_id)
select id, customer_type, customer_id from errand;

-- Update errand

alter table errand drop column customer_type;
alter table errand drop column customer_id;