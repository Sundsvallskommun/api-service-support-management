alter table if exists communication
    add column if not exists sender_id varchar(255) after sender;