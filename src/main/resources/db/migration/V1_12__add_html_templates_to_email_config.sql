alter table if exists email_worker_config
    add column if not exists errand_closed_email_html_template TEXT;

alter table if exists email_worker_config
    add column if not exists errand_new_email_html_template TEXT;
