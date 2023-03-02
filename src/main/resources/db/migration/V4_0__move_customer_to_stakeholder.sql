-- Create new tables

create table stakeholder (
   id bigint not null auto_increment,
    address varchar(255),
    care_of varchar(255),
    country varchar(255),
    first_name varchar(255),
    last_name varchar(255),
    external_id varchar(255),
    type varchar(255),
    zip_code varchar(255),
    errand_id varchar(255) not null,
    primary key (id)
) engine=InnoDB;

create table contact_channel (
   stakeholder_id bigint not null,
    type varchar(255),
    value varchar(255)
) engine=InnoDB;

alter table contact_channel
   add constraint fk_stakeholder_contact_channel_stakeholder_id
   foreign key (stakeholder_id)
   references stakeholder (id);

alter table stakeholder
  add constraint fk_errand_stakeholder_errand_id
  foreign key (errand_id)
  references errand (id);

-- Migrate data

insert into stakeholder(errand_id, type, external_id)
select id, customer_type, customer_id from errand;

-- Update errand

alter table errand drop column customer_type;
alter table errand drop column customer_id;
drop index if exists idx_errand_customer_id on errand;