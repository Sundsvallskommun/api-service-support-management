create table revision (
   id varchar(255) not null,
    created datetime(6),
    entity_id varchar(255),
    entity_type varchar(255),
    serialized_snapshot longtext,
    version integer,
    primary key (id)
) engine=InnoDB;

create index revision_entity_id_index on revision (entity_id);
create index revision_entity_type_index on revision (entity_type);