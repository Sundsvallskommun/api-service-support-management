alter table communication_attachment
    change if exists name file_name varchar(255) null;

alter table communication_attachment
    change if exists content_type mime_type varchar(255) null;

alter table if exists communication_attachment
    add column if not exists attachment_data_id integer null;

alter table if exists communication_attachment
    add constraint fk_communication_attachment_attachment_data
        foreign key if not exists (attachment_data_id)
            references attachment_data (id);

alter table if exists communication_attachment
    modify if exists communication_attachment_data_id bigint null;
