-- Every index is declared inside its create table rather than added afterwards. It keeps the whole script behind a
-- single "if not exists", so a rerun is a no-op, and it is also what keeps InnoDB from silently adding an index of its
-- own next to each foreign key: a constraint reuses an index already declared on the same statement.

-- Outbox. Deliberately WITHOUT a foreign key to errand: a DELETE event has to outlive the errand it is about.
create table if not exists process_event_outbox (
    id                varchar(36)  not null,
    municipality_id   varchar(8)   not null,
    namespace         varchar(32)  not null,
    errand_id         varchar(36)  not null,
    -- Where the row is headed, taken from the namespace PROCESS_CONSUMER when it is written. The relay never
    -- reads the configuration again, and groups its work on this column.
    process_service   varchar(64)  not null,
    -- Nullable: required for CREATE and UPDATE, irrelevant for DELETE where the process engine matches on the
    -- business key instead.
    process_key       varchar(128),
    event_type        varchar(64)  not null,
    event_sub_type    varchar(64)  not null,
    -- May the event start a NEW instance? Worked out once, at publication.
    start_allowed     tinyint(1)   not null default 0,
    -- The message name from the BPMN model, set only on rows with the SIGNAL sub type. Without it the process
    -- engine cannot tell WHICH gate the handler pressed.
    signal_name       varchar(128),
    executed_by       varchar(255),
    request_group_id  varchar(36),
    created           datetime(3)  not null,
    -- Soft delete, and the one deliberate departure from notification_dispatch, which removes its rows outright:
    -- the emergency brake counts delivered rows in a time window and needs them around for a while. No retry
    -- count, no next retry, no dead letter - an undelivered row is its own receipt that the work remains.
    delivered_at      datetime(3),
    primary key (id),
    key idx_peo_dispatch (delivered_at, created),
    -- The fetch: undelivered rows for ONE consumer, oldest first.
    key idx_peo_consumer (process_service, delivered_at, created),
    key idx_peo_guard (errand_id, delivered_at, created)
) engine=InnoDB;

-- The process instance, including the state of the lock.
create table if not exists errand_process (
    id                    varchar(36)  not null,
    errand_id             varchar(255) not null,
    municipality_id       varchar(8)   not null,
    namespace             varchar(32)  not null,
    process_service       varchar(64)  not null,
    process_key           varchar(128) not null,
    process_instance_id   varchar(64),
    process_status        varchar(32)  not null,
    current_activity_id   varchar(255),
    current_activity_name varchar(255),
    error_code            varchar(64),
    error_message         varchar(2048),
    started               datetime(3),
    ended                 datetime(3),
    -- 1 while the instance lives, NULL once it is terminal. NULL is distinct in a unique index, so an errand may
    -- carry any number of historical instances but at most one live one.
    active_marker         tinyint      null,
    created               datetime(3)  not null,
    modified              datetime(3),
    primary key (id),
    key idx_ep_errand_id (errand_id),
    constraint uq_ep_process_instance_id   unique (process_instance_id),
    constraint uq_ep_one_active_per_errand unique (errand_id, active_marker),
    constraint fk_ep_errand_id foreign key (errand_id)
        references errand (id) on delete cascade
) engine=InnoDB;

-- Append-only log of facts. Process agnostic: no foreign keys to SM metadata, no validation.
create table if not exists errand_process_activity (
    id                    varchar(36)  not null,
    -- Nullable on purpose: SM writes CONFIG and ERROR entries before any process instance exists, and errand_id
    -- is then the only thing tying the entry to anything.
    errand_process_id     varchar(36)  null,
    errand_id             varchar(255) not null,
    external_task_id      varchar(64),
    activity_type         varchar(64)  not null,
    activity_id           varchar(255),
    activity_name         varchar(255),
    severity              varchar(16)  default 'INFO' not null,
    message               varchar(2048),
    error_code            varchar(64),
    -- occurred_at is the clock of the process, created the clock of SM.
    occurred_at           datetime(3)  not null,
    created               datetime(3)  not null,
    primary key (id),
    key idx_epa_process_occurred (errand_process_id, occurred_at),
    key idx_epa_errand_occurred (errand_id, occurred_at),
    key idx_epa_retention (created),
    constraint uq_epa_idempotency unique (errand_process_id, external_task_id, activity_id),
    constraint fk_epa_process foreign key (errand_process_id)
        references errand_process (id) on delete cascade,
    -- Needed because the instance key is nullable: without it the instanceless entries would outlive the errand.
    constraint fk_epa_errand foreign key (errand_id)
        references errand (id) on delete cascade
) engine=InnoDB;
