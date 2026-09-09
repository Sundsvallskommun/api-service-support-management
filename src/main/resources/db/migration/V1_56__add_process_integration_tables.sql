create table if not exists process_event_outbox (
    id               varchar(36)  not null,
    municipality_id  varchar(8)   not null,
    namespace        varchar(32)  not null,
    errand_id        varchar(36)  not null,
    process_service  varchar(64)  not null,
    process_key      varchar(128),
    event_type       varchar(64)  not null,
    event_sub_type   varchar(64)  not null,
    start_allowed    bit          not null default 0,
    signal_name      varchar(128),
    executed_by      varchar(255),
    request_group_id varchar(36),
    created          datetime(3)  not null,
    delivered_at     datetime(3),
    primary key (id),
    key idx_peo_dispatch (delivered_at, created),
    key idx_peo_consumer (process_service, delivered_at, created),
    key idx_peo_guard (errand_id, delivered_at, created)
) engine=InnoDB;

create table if not exists errand_process (
    id                           varchar(36)  not null,
    errand_id                    varchar(255) not null,
    municipality_id              varchar(8)   not null,
    namespace                    varchar(32)  not null,
    process_service              varchar(64)  not null,
    process_key                  varchar(128) not null,
    process_instance_id          varchar(64),
    process_status               varchar(32)  not null,
    current_activity_id          varchar(255),
    current_activity_name        varchar(255),
    outstanding_external_task_id varchar(64),
    error_code                   varchar(64),
    error_message                varchar(2048),
    started                      datetime(3),
    ended                        datetime(3),
    active_marker                bit          null,
    created                      datetime(3)  not null,
    modified                     datetime(3),
    primary key (id),
    key idx_ep_errand_id (errand_id),
    constraint uq_ep_process_instance_id   unique (process_instance_id),
    constraint uq_ep_one_active_per_errand unique (errand_id, active_marker),
    constraint fk_ep_errand_id foreign key (errand_id)
        references errand (id) on delete cascade
) engine=InnoDB;

create table if not exists errand_process_activity (
    id                varchar(36)  not null,
    errand_process_id varchar(36)  null,
    errand_id         varchar(255) not null,
    external_task_id  varchar(64),
    activity_type     varchar(64)  not null,
    activity_id       varchar(255),
    activity_name     varchar(255),
    severity          varchar(16)  default 'INFO' not null,
    message           varchar(2048),
    error_code        varchar(64),
    occurred_at       datetime(3)  not null,
    created           datetime(3)  not null,
    primary key (id),
    key idx_epa_process_occurred (errand_process_id, occurred_at),
    key idx_epa_errand_occurred (errand_id, occurred_at),
    key idx_epa_retention (created),
    constraint uq_epa_idempotency unique (errand_process_id, external_task_id, activity_id),
    constraint fk_epa_process foreign key (errand_process_id)
        references errand_process (id) on delete cascade,
    constraint fk_epa_errand foreign key (errand_id)
        references errand (id) on delete cascade
) engine=InnoDB;
