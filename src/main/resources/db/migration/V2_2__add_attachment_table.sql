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

create index idx_attachment_file_name on attachment (file_name);

alter table attachment
       add constraint fk_errand_attachment_errand_id
       foreign key (errand_id)
       references errand (id);