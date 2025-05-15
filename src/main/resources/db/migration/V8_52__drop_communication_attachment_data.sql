alter table if exists communication_attachment
    drop foreign key if exists fk_communication_attachment_data_communication_attachment;

alter table if exists communication_attachment
    drop column if exists communication_attachment_data_id;

drop table if exists communication_attachment_data
