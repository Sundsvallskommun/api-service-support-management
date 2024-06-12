alter table if exists email_worker_config
    add column if not exists add_sender_as_stakeholder bit default 1 not null;
alter table if exists email_worker_config
    add column if not exists stakeholder_role varchar(255);
alter table if exists email_worker_config
    add column if not exists errand_channel varchar(255);
