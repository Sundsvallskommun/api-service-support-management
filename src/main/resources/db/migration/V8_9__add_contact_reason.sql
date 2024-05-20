create table contact_reason
(
    id bigint not null primary key,
    namespace varchar(255) not null,
    municipality_id varchar(255) not null,
    reason varchar(255) not null,
    created datetime(6),
    modified datetime(6)
) engine = InnoDB;

alter table if exists errand
    add column business_related bit null;

alter table if exists errand
    add column suspended_to datetime(6);

alter table if exists errand
    add column suspended_from datetime(6);

alter table if exists errand
    add column contact_reason_id bigint;

alter table if exists errand
       add constraint errand_contact_reason_entity_id_fk
       foreign key (contact_reason_id)
       references contact_reason (id);