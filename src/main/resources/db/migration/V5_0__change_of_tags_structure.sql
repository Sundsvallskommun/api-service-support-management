-- Create new tables
create table category_tag (
    id bigint not null auto_increment,
    created datetime(6),
    display_name varchar(255),
    modified datetime(6),
    municipality_id varchar(255) not null,
    name varchar(255) not null,
    namespace varchar(255) not null,
    primary key (id)
) engine=InnoDB;

create table external_id_type_tag (
    id bigint not null auto_increment,
    created datetime(6),
    modified datetime(6),
    municipality_id varchar(255) not null,
    name varchar(255) not null,
    namespace varchar(255) not null,
    primary key (id)
) engine=InnoDB;

create table status_tag (
    id bigint not null auto_increment,
    created datetime(6),
    modified datetime(6),
    municipality_id varchar(255) not null,
    name varchar(255) not null,
    namespace varchar(255) not null,
    primary key (id)
) engine=InnoDB;

create table tag_validation (
    id bigint not null auto_increment,
    created datetime(6),
    modified datetime(6),
    municipality_id varchar(255) not null,
    namespace varchar(255) not null,
    `type` varchar(255) not null,
    validated bit,
    primary key (id)
) engine=InnoDB;

create table type_tag (
    id bigint not null auto_increment,
    created datetime(6),
    display_name varchar(255),
    escalation_email varchar(255),
    modified datetime(6),
    name varchar(255) not null,
    category_tag_id bigint not null,
    primary key (id)
) engine=InnoDB;

create index idx_namespace_municipality_id on category_tag (namespace, municipality_id);
create index idx_namespace_municipality_id on external_id_type_tag (namespace, municipality_id);
create index idx_namespace_municipality_id on status_tag (namespace, municipality_id);
create index idx_namespace_municipality_id_type on tag_validation (namespace, municipality_id, `type`);

alter table category_tag 
    add constraint uq_namespace_municipality_id_name unique (namespace, municipality_id, name);
alter table external_id_type_tag 
    add constraint uq_namespace_municipality_id_name unique (namespace, municipality_id, name);
alter table status_tag 
    add constraint uq_namespace_municipality_id_name unique (namespace, municipality_id, name);
alter table tag_validation 
    add constraint uq_namespace_municipality_id_type unique (namespace, municipality_id, `type`);
alter table type_tag 
    add constraint uq_category_tag_id_name unique (category_tag_id, name);
alter table type_tag
    add constraint fk_category_tag_id foreign key (category_tag_id) references category_tag (id);

-- Remove old table
drop table if exists tag;
