create table job (
    progress integer,
    processed integer,
    total integer,
    created datetime(6),
    modified datetime(6),
    municipality_id varchar(8) not null,
    namespace varchar(32) not null,
    id varchar(255) not null,
    message text,
    status varchar(255) not null,
    type varchar(255) not null,
    primary key (id)
) engine=InnoDB;

create index idx_job_namespace_municipality_id_status
    on job (namespace, municipality_id, status);
