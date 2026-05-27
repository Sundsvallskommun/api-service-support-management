create table if not exists notification_dispatch (
    id                varchar(36)  not null,
    event_id          varchar(36)  not null,
    request_group_id  varchar(36),
    errand_id         varchar(36)  not null,
    municipality_id   varchar(8)   not null,
    namespace         varchar(32)  not null,
    event_type        varchar(64)  not null,
    executing_user_id varchar(255),
    created           datetime(3)  not null,
    retry_count       int          not null default 0,
    next_retry_at     datetime(3),
    dead_letter       bit          not null default 0,
    primary key (id)
) engine=InnoDB;

create index if not exists idx_dispatch_errand_id
    on notification_dispatch (errand_id);

create index if not exists idx_dispatch_dead_letter_retry
    on notification_dispatch (dead_letter, next_retry_at);
