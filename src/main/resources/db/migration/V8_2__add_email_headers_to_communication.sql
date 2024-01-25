create table communication_email_header
(
    communication_id varchar(255),
    id               varchar(255) not null,
    header_key       enum ('IN_REPLY_TO','REFERENCES','MESSAGE_ID'),
    primary key (id)
) engine = InnoDB;

create table communication_email_header_value
(
    order_index integer      not null,
    value       varchar(2048),
    header_id   varchar(255) not null,
    primary key (order_index, header_id)
) engine = InnoDB;

alter table if exists communication_email_header
    add constraint fk_email_header_email_id
        foreign key (communication_id)
            references communication (id);

alter table if exists communication_email_header_value
    add constraint fk_header_value_header_id
        foreign key (header_id)
            references communication_email_header (id);
