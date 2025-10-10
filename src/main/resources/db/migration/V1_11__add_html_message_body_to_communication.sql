alter table if exists communication
    add column if not exists html_message_body longtext;
