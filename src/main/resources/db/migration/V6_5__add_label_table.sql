    create table label (
        created datetime(6),
        id bigint not null auto_increment,
        modified datetime(6),
        municipality_id varchar(255) not null,
        json_structure json not null,
        namespace varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create index idx_namespace_municipality_id 
       on label (namespace, municipality_id);

    alter table if exists label 
       add constraint uq_namespace_municipality_id_json_structure unique (namespace, municipality_id, json_structure);
