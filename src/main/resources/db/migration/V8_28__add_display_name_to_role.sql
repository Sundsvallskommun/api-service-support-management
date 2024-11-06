alter table if exists role
    add column if not exists display_name varchar(255);
