alter table if exists namespace_config
    add column if not exists access_control boolean default false;
