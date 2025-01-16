alter table if exists notification
    add column if not exists global_acknowledged bit not null after acknowledged;