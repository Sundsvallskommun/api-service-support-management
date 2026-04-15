create table communication_cc_recipients (
    communication_id    varchar(255) not null,
    recipient           varchar(255),
    constraint fk_communication_cc_recipients_communication_id foreign key (communication_id) references communication (id)
);
