
    create table if not exists metadata_label (
        created datetime(6),
        modified datetime(6),
        municipality_id varchar(8) not null,
        namespace varchar(32) not null,
        classification varchar(255),
        display_name varchar(255),
        id varchar(255) not null,
        name varchar(255),
        parent_id varchar(255),
        resource_name varchar(255),
        resource_path varchar(255),
        primary key (id)
    ) engine=InnoDB;
    
    create index idx_namespace_municipality_id 
       on metadata_label (namespace, municipality_id);

    alter table if exists metadata_label 
       add constraint uq_namespace_municipality_id_resource_path unique (namespace, municipality_id, resource_path);

    alter table if exists metadata_label 
       add constraint fk_metadata_label_id 
       foreign key (parent_id) 
       references metadata_label (id);
       