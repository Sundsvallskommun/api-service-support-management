
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

    create table errand (
       id varchar(255) not null,
        assigned_group_id varchar(255),
        assigned_user_id varchar(255),
        category_tag varchar(255),
        client_id_tag varchar(255),
        created datetime(6),
        customer_id varchar(255),
        customer_type varchar(255),
        description longtext,
        modified datetime(6),
        priority varchar(255),
        reporter_user_id varchar(255),
        resolution varchar(255),
        status_tag varchar(255),
        title varchar(255),
        type_tag varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table external_tag (
       errand_id varchar(255) not null,
        `key` varchar(255),
        `value` varchar(255)
    ) engine=InnoDB;

    create table tag (
       id bigint not null auto_increment,
        created datetime(6),
        name varchar(255),
        type ENUM('CATEGORY', 'STATUS', 'TYPE', 'CLIENT_ID'),
        updated datetime(6),
        primary key (id)
    ) engine=InnoDB;
create index idx_attachment_file_name on attachment (file_name);
create index idx_errand_id on errand (id);
create index idx_errand_customer_id on errand (customer_id);
create index idx_errand_client_id_tag on errand (client_id_tag);
create index idx_external_tag_errand_id on external_tag (errand_id);
create index idx_external_tag_key on external_tag (`key`);

    alter table external_tag
       add constraint uq_external_tag_errand_id_key unique (errand_id, `key`);
create index idx_tag_name on tag (name);
create index idx_tag_type on tag (type);

    alter table tag
       add constraint uq_tag_name unique (name);

    alter table attachment
       add constraint fk_errand_attachment_errand_id
       foreign key (errand_id)
       references errand (id);

    alter table external_tag
       add constraint fk_errand_external_tag_errand_id
       foreign key (errand_id)
       references errand (id);
