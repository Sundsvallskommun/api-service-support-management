create table if not exists communication_recipients
(
    communication_id varchar(255) not null,
    recipient        varchar(255)
) engine = InnoDB;


alter table if exists communication_recipients
    add constraint fk_communication_recipients_message_id
        foreign key if not exists (communication_id)
            references communication (id);
