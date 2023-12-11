create table communication
(
    viewed           bit,
    sent             datetime(6),
    communication_id varchar(255) not null,
    email            varchar(255),
    errand_number    varchar(255),
    external_case_id varchar(255),
    message_body     varchar(255),
    mobile_number    varchar(255),
    subject          varchar(255),
    direction        enum ('INBOUND','OUTBOUND'),
    message_type     enum ('SMS','EMAIL'),
    primary key (communication_id)
) engine = InnoDB;

create table communication_attachment
(
    communication_attachment_data_id integer      not null,
    attachment_id                    varchar(255) not null,
    communication_id                 varchar(255),
    content_type                     varchar(255),
    name                             varchar(255),
    primary key (attachment_id)
) engine = InnoDB;

create table communication_attachment_data
(
    id   integer not null auto_increment,
    file longblob,
    primary key (id)
) engine = InnoDB;


create index idx_errand_number
    on communication (errand_number);

alter table if exists communication
    add constraint uq_errand_number unique (errand_number);

alter table if exists communication_attachment
    add constraint UK_k3pya9ygw4dkeaqoe4sv733jx unique (communication_attachment_data_id);

alter table if exists communication_attachment
    add constraint fk_communication_attachment_data_communication_attachment
        foreign key (communication_attachment_data_id)
            references communication_attachment_data (id);
