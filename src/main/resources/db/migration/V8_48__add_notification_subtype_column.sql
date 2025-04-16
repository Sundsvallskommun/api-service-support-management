alter table if exists notification
    add column if not exists subtype varchar(255) null;
