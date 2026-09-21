create table if not exists email_dispatch_outbox (
    id               varchar(36)  not null,
    municipality_id  varchar(8)   not null,
    namespace        varchar(32)  not null,
    errand_id        varchar(36)  not null,
    errand_number    varchar(255),
    subscriber_id    varchar(36)  not null,
    recipient_email  varchar(255),
    identifier_type  varchar(16),
    identifier_value varchar(255),
    event_summary    text,
    created          datetime(3)  not null,
    attempts         int          not null default 0,
    last_attempted   datetime(3),
    primary key (id),
    index idx_email_dispatch_outbox_pending (attempts, created)
) engine=InnoDB;
