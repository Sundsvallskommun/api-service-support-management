alter table if exists communication
    add column if not exists sender_user_id varchar(255) after sender;