create table if not exists messaging_outbox (
    id              varchar(36)  not null,
    municipality_id varchar(8)   not null,
    message_type    varchar(16)  not null,
    payload         text         not null,
    created         datetime(3)  not null,
    retry_count     int          default 0 not null,
    next_retry_at   datetime(3),
    dead_letter     bit          default 0 not null,
    primary key (id)
) engine=InnoDB;

create index if not exists idx_outbox_dead_letter_retry
    on messaging_outbox (dead_letter, next_retry_at);
