
    create table attachment (
       id varchar(255) not null,
        created datetime(6),
        file longblob,
        file_name varchar(255),
        mime_type varchar(255),
        modified datetime(6),
        errand_id varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

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

    create table contact_channel (
       stakeholder_id bigint not null,
        type varchar(255),
        value varchar(255)
    ) engine=InnoDB;

    create table errand (
       id varchar(255) not null,
        assigned_group_id varchar(255),
        assigned_user_id varchar(255),
        category varchar(255),
        created datetime(6),
        description longtext,
        escalation_email varchar(255),
        modified datetime(6),
        municipality_id varchar(255) not null,
        namespace varchar(255) not null,
        priority varchar(255),
        reporter_user_id varchar(255),
        resolution varchar(255),
        status varchar(255),
        title varchar(255),
        type varchar(255),
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

    create table external_tag (
       errand_id varchar(255) not null,
        `key` varchar(255),
        `value` varchar(255)
    ) engine=InnoDB;

    create table role (
       id bigint not null auto_increment,
        created datetime(6),
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
        external_id varchar(255),
        external_id_type varchar(255),
        first_name varchar(255),
        last_name varchar(255),
        role varchar(255),
        zip_code varchar(255),
        errand_id varchar(255) not null,
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
create index idx_attachment_file_name on attachment (file_name);
create index idx_namespace_municipality_id on category (namespace, municipality_id);

    alter table category
       add constraint uq_namespace_municipality_id_name unique (namespace, municipality_id, name);
create index idx_errand_id on errand (id);
create index idx_errand_namespace on errand (namespace);
create index idx_errand_municipality_id on errand (municipality_id);
create index idx_namespace_municipality_id on external_id_type (namespace, municipality_id);

    alter table external_id_type
       add constraint uq_namespace_municipality_id_name unique (namespace, municipality_id, name);
create index idx_external_tag_errand_id on external_tag (errand_id);
create index idx_external_tag_key on external_tag (`key`);

    alter table external_tag
       add constraint uq_external_tag_errand_id_key unique (errand_id, `key`);
create index idx_namespace_municipality_id on role (namespace, municipality_id);

    alter table role
       add constraint uq_namespace_municipality_id_name unique (namespace, municipality_id, name);
create index idx_namespace_municipality_id on status (namespace, municipality_id);

    alter table status
       add constraint uq_namespace_municipality_id_name unique (namespace, municipality_id, name);

    alter table `type`
       add constraint uq_category_id_name unique (category_id, name);
create index idx_namespace_municipality_id_type on validation (namespace, municipality_id, `type`);

    alter table validation
       add constraint uq_namespace_municipality_id_type unique (namespace, municipality_id, `type`);

    alter table attachment
       add constraint fk_errand_attachment_errand_id
       foreign key (errand_id)
       references errand (id);

    alter table contact_channel
       add constraint fk_stakeholder_contact_channel_stakeholder_id
       foreign key (stakeholder_id)
       references stakeholder (id);

    alter table external_tag
       add constraint fk_errand_external_tag_errand_id
       foreign key (errand_id)
       references errand (id);

    alter table stakeholder
       add constraint fk_errand_stakeholder_errand_id
       foreign key (errand_id)
       references errand (id);

    alter table `type`
       add constraint fk_category_id
       foreign key (category_id)
       references category (id);
       