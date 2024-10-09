alter table if exists email_worker_config
    add column if not exists errand_new_email_sender varchar(255);
alter table if exists email_worker_config
    add column if not exists errand_new_email_template varchar(5000);

