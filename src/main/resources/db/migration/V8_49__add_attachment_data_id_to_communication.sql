alter table if exists communication_attachment
    add column if not exists attachment_data_id integer not null;

alter table if exists communication_attachment
    add constraint fk_communication_attachment_data_attachment
        foreign key if not exists (attachment_data_id)
            references attachment_data (id);

alter table communication_attachment
    modify communication_attachment_data_id bigint null;
