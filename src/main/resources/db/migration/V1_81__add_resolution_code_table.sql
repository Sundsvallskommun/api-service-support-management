-- The resolution codes an errand may be closed with, registered per namespace. The name is the key that
-- errand.resolution holds; a code in use is deprecated rather than removed.
create table if not exists resolution_code (
    id varchar(255) not null,
    name varchar(255) not null,
    display_name varchar(255),
    description varchar(1024),
    sort_order integer,
    deprecated bit not null,
    municipality_id varchar(8) not null,
    namespace varchar(32) not null,
    created datetime(6),
    modified datetime(6),
    primary key (id),
    constraint uq_resolution_code_namespace_municipality_id_name unique (namespace, municipality_id, name)
) engine=InnoDB;

create index if not exists idx_resolution_code_namespace_municipality_id
    on resolution_code (namespace, municipality_id);
