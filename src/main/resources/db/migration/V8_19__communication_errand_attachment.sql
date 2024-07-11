create table communication_errand_attachment (
    communication_id varchar(255) not null,
    errand_attachment_id varchar(255) not null,
    primary key (communication_id, errand_attachment_id)
) engine=InnoDB;

alter table communication_errand_attachment
    add constraint fk_communication_errand_attachments_communication
        foreign key (communication_id)
            references communication (id);

alter table communication_errand_attachment
    add constraint fk_communication_errand_attachment_attachment_id
        foreign key (errand_attachment_id)
            references attachment (id);