create table if not exists email_dispatch_outbox (
    id            varchar(36) not null,
    subscriber_id varchar(255) not null,
    errand_id     varchar(36) not null,
    errand_number varchar(255),
    created       datetime(3) not null,
    primary key (id),
    index idx_email_dispatch_outbox_subscriber_created (subscriber_id, created),
    constraint fk_email_dispatch_outbox_subscriber_id
        foreign key (subscriber_id) references subscriber (id) on delete cascade
) engine=InnoDB;

create table if not exists email_dispatch_outbox_event (
    outbox_id   varchar(36) not null,
    event_id    varchar(36),
    description varchar(255),
    constraint fk_email_dispatch_outbox_event_outbox_id
        foreign key (outbox_id) references email_dispatch_outbox (id) on delete cascade
) engine=InnoDB;
