
    create table attachment (
        attachment_data_id integer not null,
        created datetime(6),
        modified datetime(6),
        errand_id varchar(255) not null,
        file_name varchar(255),
        id varchar(255) not null,
        mime_type varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table attachment_data (
        id integer not null auto_increment,
        file longblob,
        primary key (id)
    ) engine=InnoDB;

    create table category (
        created datetime(6),
        id bigint not null auto_increment,
        modified datetime(6),
        display_name varchar(255),
        municipality_id varchar(255) not null,
        name varchar(255) not null,
        namespace varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table contact_channel (
        stakeholder_id bigint not null,
        type varchar(255),
        value varchar(255)
    ) engine=InnoDB;

    create table errand (
        created datetime(6),
        modified datetime(6),
        assigned_group_id varchar(255),
        assigned_user_id varchar(255),
        category varchar(255),
        escalation_email varchar(255),
        id varchar(255) not null,
        municipality_id varchar(255) not null,
        namespace varchar(255) not null,
        priority varchar(255),
        reporter_user_id varchar(255),
        resolution varchar(255),
        status varchar(255),
        title varchar(255),
        type varchar(255),
        description longtext,
        primary key (id)
    ) engine=InnoDB;

    create table external_id_type (
        created datetime(6),
        id bigint not null auto_increment,
        modified datetime(6),
        municipality_id varchar(255) not null,
        name varchar(255) not null,
        namespace varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table external_tag (
        errand_id varchar(255) not null,
        `key` varchar(255),
        `value` varchar(255)
    ) engine=InnoDB;

    create table label (
        created datetime(6),
        id bigint not null auto_increment,
        modified datetime(6),
        json_structure json not null,
        municipality_id varchar(255) not null,
        namespace varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table revision (
        version integer,
        created datetime(6),
        entity_id varchar(255),
        entity_type varchar(255),
        id varchar(255) not null,
        serialized_snapshot longtext,
        primary key (id)
    ) engine=InnoDB;

    create table role (
        created datetime(6),
        id bigint not null auto_increment,
        modified datetime(6),
        municipality_id varchar(255) not null,
        name varchar(255) not null,
        namespace varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table stakeholder (
        id bigint not null auto_increment,
        address varchar(255),
        care_of varchar(255),
        country varchar(255),
        errand_id varchar(255) not null,
        external_id varchar(255),
        external_id_type varchar(255),
        first_name varchar(255),
        last_name varchar(255),
        role varchar(255),
        zip_code varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table status (
        created datetime(6),
        id bigint not null auto_increment,
        modified datetime(6),
        municipality_id varchar(255) not null,
        name varchar(255) not null,
        namespace varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table `type` (
        category_id bigint not null,
        created datetime(6),
        id bigint not null auto_increment,
        modified datetime(6),
        display_name varchar(255),
        escalation_email varchar(255),
        name varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table validation (
        validated bit,
        created datetime(6),
        id bigint not null auto_increment,
        modified datetime(6),
        municipality_id varchar(255) not null,
        namespace varchar(255) not null,
        `type` enum ('CATEGORY','EXTERNAL_ID_TYPE','ROLE','STATUS','TYPE') not null,
        primary key (id)
    ) engine=InnoDB;

    create index idx_attachment_file_name
       on attachment (file_name);

    alter table if exists attachment
       add constraint UK_scgvw835joke5mqsmk3fhs732 unique (attachment_data_id);

    create index idx_namespace_municipality_id
       on category (namespace, municipality_id);

    alter table if exists category
       add constraint uq_namespace_municipality_id_name unique (namespace, municipality_id, name);

    create index idx_errand_id
       on errand (id);

    create index idx_errand_namespace
       on errand (namespace);

    create index idx_errand_municipality_id
       on errand (municipality_id);

    create index idx_errand_status
       on errand (status);

    create index idx_errand_category
       on errand (category);

    create index idx_errand_type
       on errand (type);

    create index idx_errand_assigned_user_id
       on errand (assigned_user_id);

    create index idx_errand_reporter_user_id
       on errand (reporter_user_id);

    create index idx_namespace_municipality_id
       on external_id_type (namespace, municipality_id);

    alter table if exists external_id_type
       add constraint uq_namespace_municipality_id_name unique (namespace, municipality_id, name);

    create index idx_external_tag_errand_id
       on external_tag (errand_id);

    create index idx_external_tag_key
       on external_tag (`key`);

    alter table if exists external_tag
       add constraint uq_external_tag_errand_id_key unique (errand_id, `key`);

    create index idx_namespace_municipality_id
       on label (namespace, municipality_id);

    alter table if exists label
       add constraint uq_namespace_municipality_id unique (namespace, municipality_id);

    create index revision_entity_id_index
       on revision (entity_id);

    create index revision_entity_type_index
       on revision (entity_type);

    alter table if exists revision
       add constraint uq_entity_id_version unique (entity_id, version);

    create index idx_namespace_municipality_id
       on role (namespace, municipality_id);

    alter table if exists role
       add constraint uq_namespace_municipality_id_name unique (namespace, municipality_id, name);

    create index idx_namespace_municipality_id
       on status (namespace, municipality_id);

    alter table if exists status
       add constraint uq_namespace_municipality_id_name unique (namespace, municipality_id, name);

    alter table if exists `type`
       add constraint uq_category_id_name unique (category_id, name);

    create index idx_namespace_municipality_id_type
       on validation (namespace, municipality_id, `type`);

    alter table if exists validation
       add constraint uq_namespace_municipality_id_type unique (namespace, municipality_id, `type`);

    alter table if exists attachment
       add constraint fk_attachment_data_attachment
       foreign key (attachment_data_id)
       references attachment_data (id);

    alter table if exists attachment
       add constraint fk_errand_attachment_errand_id 
       foreign key (errand_id) 
       references errand (id);

    alter table if exists contact_channel 
       add constraint fk_stakeholder_contact_channel_stakeholder_id 
       foreign key (stakeholder_id) 
       references stakeholder (id);

    alter table if exists external_tag 
       add constraint fk_errand_external_tag_errand_id 
       foreign key (errand_id) 
       references errand (id);

    alter table if exists stakeholder 
       add constraint fk_errand_stakeholder_errand_id 
       foreign key (errand_id) 
       references errand (id);

    alter table if exists `type` 
       add constraint fk_category_id 
       foreign key (category_id) 
       references category (id);
