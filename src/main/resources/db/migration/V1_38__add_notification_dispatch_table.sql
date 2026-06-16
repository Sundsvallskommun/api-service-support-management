create table if not exists notification_dispatch (
    id                varchar(36)  not null,
    event_id          varchar(36),
    request_group_id  varchar(36),
    errand_id         varchar(36)  not null,
    municipality_id   varchar(8)   not null,
    namespace         varchar(32)  not null,
    event_type        varchar(64)  not null,
    executing_user_id varchar(255),
    created           datetime(3)  not null,
    retry_count       int          default 0 not null,
    next_retry_at     datetime(3),
    dead_letter       bit          default 0 not null,
    primary key (id)
) engine=InnoDB;

create index if not exists idx_dispatch_errand_id
    on notification_dispatch (errand_id);

create index if not exists idx_dispatch_dead_letter_retry
    on notification_dispatch (dead_letter, next_retry_at);

create table if not exists subscriber_notification (
    id               varchar(36)  not null,
    created          datetime(3)  not null,
    modified         datetime(3),
    identifier_type  varchar(16)  not null,
    identifier_value varchar(255) not null,
    municipality_id  varchar(8)   not null,
    namespace        varchar(32)  not null,
    errand_id        varchar(36)  not null,
    errand_number    varchar(255),
    expires          datetime(3),
    acknowledged     datetime(3),
    primary key (id)
) engine=InnoDB;

create index if not exists idx_sub_notif_identifier
    on subscriber_notification (municipality_id, namespace, identifier_type, identifier_value);

create index if not exists idx_sub_notif_errand
    on subscriber_notification (errand_id);

alter table if exists subscriber_notification
    add constraint uq_sub_notif_errand_identifier
    unique (municipality_id, namespace, errand_id, identifier_type, identifier_value);
