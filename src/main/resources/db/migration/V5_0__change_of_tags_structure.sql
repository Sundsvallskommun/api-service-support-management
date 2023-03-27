-- Create new tables
create table category (
    id bigint not null auto_increment,
    created datetime(6),
    display_name varchar(255),
    modified datetime(6),
    municipality_id varchar(255) not null,
    name varchar(255) not null,
    namespace varchar(255) not null,
    primary key (id)
) engine=InnoDB;

create table external_id_type (
    id bigint not null auto_increment,
    created datetime(6),
    modified datetime(6),
    municipality_id varchar(255) not null,
    name varchar(255) not null,
    namespace varchar(255) not null,
    primary key (id)
) engine=InnoDB;

create table status (
    id bigint not null auto_increment,
    created datetime(6),
    modified datetime(6),
    municipality_id varchar(255) not null,
    name varchar(255) not null,
    namespace varchar(255) not null,
    primary key (id)
) engine=InnoDB;

create table validation (
    id bigint not null auto_increment,
    created datetime(6),
    modified datetime(6),
    municipality_id varchar(255) not null,
    namespace varchar(255) not null,
    `type` varchar(255) not null,
    validated bit,
    primary key (id)
) engine=InnoDB;

create table `type` (
    id bigint not null auto_increment,
    created datetime(6),
    display_name varchar(255),
    escalation_email varchar(255),
    modified datetime(6),
    name varchar(255) not null,
    category_id bigint not null,
    primary key (id)
) engine=InnoDB;

create index idx_namespace_municipality_id on category (namespace, municipality_id);
create index idx_namespace_municipality_id on external_id_type (namespace, municipality_id);
create index idx_namespace_municipality_id on status (namespace, municipality_id);
create index idx_namespace_municipality_id_type on validation (namespace, municipality_id, `type`);

alter table category
    add constraint uq_namespace_municipality_id_name unique (namespace, municipality_id, name);
alter table external_id_type
    add constraint uq_namespace_municipality_id_name unique (namespace, municipality_id, name);
alter table status
    add constraint uq_namespace_municipality_id_name unique (namespace, municipality_id, name);
alter table validation 
    add constraint uq_namespace_municipality_id_type unique (namespace, municipality_id, `type`);
alter table `type`
    add constraint uq_category_id_name unique (category_id, name);
alter table `type`
    add constraint fk_category_id foreign key (category_id) references category (id);

-- Remove old table
drop table if exists tag;
