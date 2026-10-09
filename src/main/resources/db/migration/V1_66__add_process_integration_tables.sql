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

create table if not exists errand_process_signal (
    id                varchar(36)  not null,
    errand_process_id varchar(36)  not null,
    name              varchar(128) character set utf8mb4 collate utf8mb4_nopad_bin not null,
    label             varchar(255),
    sort_order        int          default 0 not null,
    created           datetime(3)  not null,
    primary key (id),
    constraint uq_eps_process_name unique (errand_process_id, name),
    constraint fk_eps_process foreign key (errand_process_id)
        references errand_process (id) on delete cascade
) engine=InnoDB;

alter table if exists errand
    add column if not exists lifecycle varchar(16) default 'ACTIVE' not null;

alter table if exists attachment
    add column if not exists sequence_number integer;

alter table if exists attachment
    add column if not exists received datetime(6);

create table if not exists attachment_sequence (
    errand_id            varchar(255) not null,
    last_sequence_number integer      not null,
    primary key (errand_id),
    constraint fk_attachment_sequence_errand_id foreign key (errand_id)
        references errand (id) on delete cascade
) engine=InnoDB;

update attachment
set received = created
where received is null;

update attachment a
    join (select attachment.id,
                 greatest(coalesce(max(attachment.sequence_number) over (partition by attachment.errand_id), 0),
                          coalesce(attachment_sequence.last_sequence_number, 0))
                     + row_number() over (partition by attachment.errand_id, attachment.sequence_number is null
                                          order by attachment.created, attachment.id) as sequence_number
          from attachment
                   left join attachment_sequence on attachment_sequence.errand_id = attachment.errand_id) numbered
    on numbered.id = a.id
set a.sequence_number = numbered.sequence_number
where a.sequence_number is null;

create unique index if not exists uq_attachment_errand_id_sequence_number
    on attachment (errand_id, sequence_number);

insert into attachment_sequence (errand_id, last_sequence_number)
select errand.id, coalesce(max(attachment.sequence_number), 0)
from errand
         left join attachment on attachment.errand_id = errand.id
group by errand.id
on duplicate key update last_sequence_number = greatest(last_sequence_number, values(last_sequence_number));

create table if not exists decision_parameter (
    id              varchar(255) not null,
    decision_id     varchar(255) not null,
    parameters_key  varchar(255) not null,
    display_name    varchar(255),
    parameter_group varchar(255),
    primary key (id),
    constraint fk_decision_parameter_decision_id
        foreign key (decision_id) references decision (id)
        on delete cascade
) engine=InnoDB;

create index if not exists idx_decision_parameter_decision_id
    on decision_parameter (decision_id);

create table if not exists decision_parameter_values (
    decision_parameter_id varchar(255)  not null,
    value_order           integer       default 0 not null,
    value                 varchar(3000),
    primary key (decision_parameter_id, value_order),
    constraint fk_decision_parameter_values_decision_parameter_id
        foreign key (decision_parameter_id) references decision_parameter (id)
        on delete cascade
) engine=InnoDB;

create table if not exists investigation_parameter (
    id               varchar(255) not null,
    investigation_id varchar(255) not null,
    parameters_key   varchar(255) not null,
    display_name     varchar(255),
    parameter_group  varchar(255),
    primary key (id),
    constraint fk_investigation_parameter_investigation_id
        foreign key (investigation_id) references investigation (id)
        on delete cascade
) engine=InnoDB;

create index if not exists idx_investigation_parameter_investigation_id
    on investigation_parameter (investigation_id);

create table if not exists investigation_parameter_values (
    investigation_parameter_id varchar(255)  not null,
    value_order                integer       default 0 not null,
    value                      varchar(3000),
    primary key (investigation_parameter_id, value_order),
    constraint fk_investigation_parameter_values_investigation_parameter_id
        foreign key (investigation_parameter_id) references investigation_parameter (id)
        on delete cascade
) engine=InnoDB;

alter table if exists stakeholder_parameter_values
    modify column if exists value varchar(3000);
