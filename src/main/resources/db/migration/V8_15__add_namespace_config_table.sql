create table namespace_config (
    id bigint not null auto_increment,
    municipality_id varchar(255) not null,
    namespace varchar(255) not null,
    short_code varchar(255) not null,
    created datetime(6),
    modified datetime(6),
    primary key (id)
) engine=InnoDB;

create index idx_namespace_municipality_id
   on namespace_config (namespace, municipality_id);

alter table if exists namespace_config
   add constraint uq_namespace_municipality_id unique (namespace, municipality_id);