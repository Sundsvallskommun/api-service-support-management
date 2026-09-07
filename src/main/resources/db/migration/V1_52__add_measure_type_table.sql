create table measure_type (
    deprecated bit not null,
    sort_order integer,
    created datetime(6),
    modified datetime(6),
    municipality_id varchar(8) not null,
    namespace varchar(32) not null,
    display_name varchar(255),
    id varchar(255) not null,
    measure_group varchar(255) not null,
    name varchar(255) not null,
    primary key (id)
) engine=InnoDB;

create index idx_measure_type_namespace_municipality_id
   on measure_type (namespace, municipality_id);

alter table if exists measure_type
   add constraint uq_measure_type_namespace_municipality_id_name unique (namespace, municipality_id, name);